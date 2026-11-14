/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.entity.actor.instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.holders.SummonEffect;
import org.l2jmobius.gameserver.data.sql.SummonEffectTable;
import org.l2jmobius.gameserver.data.sql.SummonInfoTable;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.EffectScope;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.network.serverpackets.SetSummonRemainTime;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Represents a specialized type of {@code Summon} that functions as an instance-based entity.<br>
 * This class handles the logic for servitors created by players within the game world.
 * @author UnAfraid, Mobius
 */
public class Servitor extends Summon implements Runnable
{
	protected static final Logger log = Logger.getLogger(Servitor.class.getName());
	
	private static final String ADD_SKILL_SAVE = "REPLACE INTO character_summon_skills_save (ownerId,ownerClassIndex,summonSkillId,skill_id,skill_level,skill_sub_level,remaining_time,buff_index) VALUES (?,?,?,?,?,?,?,?)";
	private static final String RESTORE_SKILL_SAVE = "SELECT skill_id,skill_level,skill_sub_level,remaining_time,buff_index FROM character_summon_skills_save WHERE ownerId=? AND ownerClassIndex=? AND summonSkillId=? ORDER BY buff_index ASC";
	private static final String DELETE_SKILL_SAVE = "DELETE FROM character_summon_skills_save WHERE ownerId=? AND ownerClassIndex=? AND summonSkillId=?";
	private static final int SIEGE_MODE_SWITCH_TIME = 30000;
	
	private float _expMultiplier = 0;
	private ItemHolder _itemConsume;
	private int _lifeTime;
	private int _lifeTimeRemaining;
	private boolean _siegeMode = false;
	private long _siegeModeSwitchEnd = 0;
	private int _consumeItemInterval;
	private int _consumeItemIntervalRemaining;
	protected Future<?> _summonLifeTask;
	
	private int _referenceSkill;
	
	/**
	 * Creates a new {@code Servitor} instance.<br>
	 * This constructor initializes the servitor using a specific template and owner.<br>
	 * It sets the instance type to {@code InstanceType.Servitor}.<br>
	 * It enables the summon animation for this entity.
	 * @param template The {@code NpcTemplate} used to define the servitor's properties.
	 * @param owner The {@code Player} who owns this servitor.
	 */
	public Servitor(NpcTemplate template, Player owner)
	{
		super(template, owner);
		setInstanceType(InstanceType.Servitor);
		setShowSummonAnimation(true);
	}
	
	/**
	 * This method is called when the summon is first created in the world.<br>
	 * It initializes the life task for the summon.<br>
	 * The task runs every {@code 5000} milliseconds to manage its remaining time.
	 */
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		if (_summonLifeTask == null)
		{
			_summonLifeTask = ThreadPool.scheduleAtFixedRate(this, 0, 5000);
		}
	}
	
	/**
	 * Retrieves the level of this summon.<br>
	 * It returns the level from the {@code NpcTemplate}.<br>
	 * If no template exists, it returns {@code 0}.
	 * @return The current level as an {@code int}.
	 */
	@Override
	public int getLevel()
	{
		return getTemplate() != null ? getTemplate().getLevel() : 0;
	}
	
	/**
	 * Retrieves the type of the summon.<br>
	 * This method currently returns a constant value of {@code 1}.
	 * @return The integer representation of the summon type.
	 */
	@Override
	public int getSummonType()
	{
		return 1;
	}
	
	/**
	 * Sets the experience point multiplier for this summon.<br>
	 * This value determines how much experience is gained from it.
	 * @param expMultiplier The {@code float} value to use as a multiplier.
	 */
	public void setExpMultiplier(float expMultiplier)
	{
		_expMultiplier = expMultiplier;
	}
	
	/**
	 * Retrieves the experience point multiplier for this summon.<br>
	 * This value determines how much experience is gained from it.
	 * @return the current {@code float} multiplier.
	 */
	public float getExpMultiplier()
	{
		return _expMultiplier;
	}
	
	/**
	 * Sets the item that is consumed by this summon.<br>
	 * This method updates the {@code _itemConsume} field with the provided {@code ItemHolder}.
	 * @param item The {@code ItemHolder} to be used for consumption.
	 */
	public void setItemConsume(ItemHolder item)
	{
		_itemConsume = item;
	}
	
	/**
	 * Retrieves the item consumed by this summon.
	 * @return the {@code ItemHolder} associated with the consumption.
	 */
	public ItemHolder getItemConsume()
	{
		return _itemConsume;
	}
	
	/**
	 * Sets the time interval for item consumption.<br>
	 * This value determines how often an item is consumed by this entity.
	 * @param interval The number of ticks between each item consumption.
	 */
	public void setItemConsumeInterval(int interval)
	{
		_consumeItemInterval = interval;
		_consumeItemIntervalRemaining = interval;
	}
	
	/**
	 * Gets the interval for item consumption.<br>
	 * This value determines how often an item is consumed.
	 * @return The current consumption interval as an {@code int}.
	 */
	public int getItemConsumeInterval()
	{
		return _consumeItemInterval;
	}
	
	/**
	 * Sets the total duration for which this summon remains active.<br>
	 * This updates both the initial {@code _lifeTime} and the current {@code _lifeTimeRemaining}.
	 * @param lifeTime The total time in seconds for the summon's existence.
	 */
	public void setLifeTime(int lifeTime)
	{
		_lifeTime = lifeTime;
		_lifeTimeRemaining = lifeTime;
	}
	
	/**
	 * Retrieves the total life time of the summon.<br>
	 * This value represents the duration for which the summon exists.
	 * @return The total life time as an {@code int}.
	 */
	public int getLifeTime()
	{
		return _lifeTime;
	}
	
	/**
	 * Sets the amount of time remaining before the summon expires.<br>
	 * This updates the internal {@code _lifeTimeRemaining} variable.
	 * @param time The number of ticks or seconds remaining for the life cycle.
	 */
	public void setLifeTimeRemaining(int time)
	{
		_lifeTimeRemaining = time;
	}
	
	/**
	 * Gets the amount of time remaining before the summon expires.<br>
	 * This value is updated as the life time decreases over time.
	 * @return The current remaining life time as an {@code int}.
	 */
	public int getLifeTimeRemaining()
	{
		return _lifeTimeRemaining;
	}
	
	/**
	 * Sets the unique identifier for the reference skill.<br>
	 * This value is used to identify which skill this summon relates to.
	 * @param skillId The {@code int} ID of the skill.
	 */
	public void setReferenceSkill(int skillId)
	{
		_referenceSkill = skillId;
	}
	
	/**
	 * Retrieves the ID of the skill associated with this summon.<br>
	 * This value is used to identify which specific skill created the entity.
	 * @return The {@code int} identifier for the reference skill.
	 */
	public int getReferenceSkill()
	{
		return _referenceSkill;
	}
	
	/**
	 * Handles the logic for when this creature dies.<br>
	 * It calls the parent class death logic and cancels any active life tasks.
	 * @param killer The {@code Creature} that caused the death.
	 * @return {@code true} if the death was successful, {@code false} otherwise.
	 */
	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		if (_summonLifeTask != null)
		{
			_summonLifeTask.cancel(false);
		}
		
		return true;
	}
	
	/**
	 * Servitors' skills automatically change their level based on the servitor's level.<br>
	 * Until level 70, the servitor gets 1 lv of skill per 10 levels.<br>
	 * After that, it is 1 skill level per 5 servitor levels.<br>
	 * If the resulting skill level doesn't exist use the max that does exist!
	 */
	@Override
	public void doCast(Skill skill)
	{
		final int petLevel = getLevel();
		int skillLevel = petLevel / 10;
		if (petLevel >= 70)
		{
			skillLevel += (petLevel - 65) / 10;
		}
		
		// Adjust the level for servitors less than level 1.
		if (skillLevel < 1)
		{
			skillLevel = 1;
		}
		
		if (isServitor())
		{
			skillLevel = skill.getLevel();
		}
		
		final Skill skillToCast = SkillData.getInstance().getSkill(skill.getId(), skillLevel);
		if (skillToCast != null)
		{
			super.doCast(skillToCast);
		}
		else
		{
			super.doCast(skill);
		}
	}
	
	/**
	 * Sets whether the summon should be restored.<br>
	 * This determines if the summon is treated as a restorable entity.
	 * @param value The boolean state to set for {@code _restoreSummon}.
	 */
	@Override
	public void setRestoreSummon(boolean value)
	{
		_restoreSummon = value;
	}
	
	/**
	 * Stops the visual or mechanical effects of a specific skill.<br>
	 * This method removes the effect from the current summon based on the provided type and ID.
	 * @param type The {@code SkillFinishType} that determines how the skill ends.
	 * @param skillId The unique identifier for the skill to stop.
	 */
	@Override
	public void stopSkillEffects(SkillFinishType type, int skillId)
	{
		super.stopSkillEffects(type, skillId);
		final Player owner = getOwner();
		if (owner == null)
		{
			return;
		}
		
		final Collection<SummonEffect> effects = SummonEffectTable.getInstance().getServitorEffectsForSkill(owner.getObjectId(), owner.getClassIndex(), _referenceSkill);
		if ((effects == null) || effects.isEmpty())
		{
			return;
		}
		
		for (SummonEffect effect : effects)
		{
			final Skill skill = effect.getSkill();
			if ((skill != null) && (skill.getId() == skillId))
			{
				effects.remove(effect);
			}
		}
	}
	
	/**
	 * Saves the current servitor data to the database.<br>
	 * This method checks if the servitor has a reference skill and is eligible for restoration.<br>
	 * It uses {@code SummonInfoTable} to persist the summon state based on the server configuration.
	 */
	@Override
	public void storeMe()
	{
		if (_referenceSkill == 0)
		{
			return;
		}
		
		if (PlayerConfig.RESTORE_SERVITOR_ON_RECONNECT)
		{
			if (isDead())
			{
				SummonInfoTable.getInstance().removeServitor(getOwner(), getObjectId());
			}
			else
			{
				SummonInfoTable.getInstance().saveSummon(this);
			}
		}
	}
	
	/**
	 * Saves the current skill effects of a summon to the database.<br>
	 * This method checks if the owner is in Olympiad mode before proceeding.<br>
	 * It clears existing records to prevent duplicate data.<br>
	 * If {@code storeEffects} is {@code true}, it saves valid skills and their remaining times.
	 * @param storeEffects Determines whether the skill effects should be saved to the database.
	 */
	@Override
	public void storeEffect(boolean storeEffects)
	{
		if (!PlayerConfig.SUMMON_STORE_SKILL_COOLTIME)
		{
			return;
		}
		
		final Player owner = getOwner();
		if ((owner == null) || owner.isInOlympiadMode())
		{
			return;
		}
		
		final int ownerObjectId = owner.getObjectId();
		final int classIndex = owner.getClassIndex();
		final int referenceSkill = _referenceSkill;
		final SummonEffectTable summonEffectTable = SummonEffectTable.getInstance();
		
		// Clear cached effect list so the upcoming inserts do not duplicate state from a prior save.
		summonEffectTable.clearServitorEffectsForSkill(ownerObjectId, classIndex, referenceSkill);
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_SKILL_SAVE))
		{
			// Delete all current stored effects for summon to avoid dupe.
			statement.setInt(1, ownerObjectId);
			statement.setInt(2, classIndex);
			statement.setInt(3, referenceSkill);
			statement.execute();
			
			int buffIndex = 0;
			
			final Collection<Long> storedSkills = new HashSet<>();
			
			// Store all effect data along with calculated remaining.
			if (storeEffects)
			{
				try (PreparedStatement ps2 = con.prepareStatement(ADD_SKILL_SAVE))
				{
					for (BuffInfo info : getEffectList().getEffects())
					{
						if (info == null)
						{
							continue;
						}
						
						final Skill skill = info.getSkill();
						
						// Do not store those effects.
						if (skill.isDeleteAbnormalOnLeave() || !skill.isSharedWithSummon())
						{
							continue;
						}
						
						// Do not save heals.
						if (skill.getAbnormalType() == AbnormalType.LIFE_FORCE_OTHERS)
						{
							continue;
						}
						
						// Toggles are skipped, unless they are necessary to be always on.
						if (skill.isToggle() && !skill.isNecessaryToggle())
						{
							continue;
						}
						
						// Dances and songs are not kept in retail.
						if (skill.isDance() && !PlayerConfig.ALT_STORE_DANCES)
						{
							continue;
						}
						
						if (!storedSkills.add(skill.getReuseHashCode()))
						{
							continue;
						}
						
						ps2.setInt(1, ownerObjectId);
						ps2.setInt(2, classIndex);
						ps2.setInt(3, referenceSkill);
						ps2.setInt(4, skill.getId());
						ps2.setInt(5, skill.getLevel());
						ps2.setInt(6, skill.getSubLevel());
						final int infoTime = info.getTime();
						ps2.setInt(7, infoTime);
						ps2.setInt(8, ++buffIndex);
						ps2.addBatch();
						
						summonEffectTable.addServitorEffect(ownerObjectId, classIndex, referenceSkill, new SummonEffect(skill, infoTime));
					}
					
					ps2.executeBatch();
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not store summon effect data: ", e);
		}
	}
	
	/**
	 * Restores active effects for the summon from the database.<br>
	 * This method checks if any saved effects exist for the owner and skill.<br>
	 * It applies these effects to the current instance of the summon.<br>
	 * The saved data is cleared after the restoration process completes.
	 */
	@Override
	public void restoreEffects()
	{
		final Player owner = getOwner();
		if ((owner == null) || owner.isInOlympiadMode())
		{
			return;
		}
		
		final int ownerObjectId = owner.getObjectId();
		final int classIndex = owner.getClassIndex();
		final int referenceSkill = _referenceSkill;
		final SummonEffectTable summonEffectTable = SummonEffectTable.getInstance();
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			// Only hit the database when the cache has nothing for this owner/class/skill.
			if (!summonEffectTable.hasServitorEffectsForSkill(ownerObjectId, classIndex, referenceSkill))
			{
				try (PreparedStatement statement = con.prepareStatement(RESTORE_SKILL_SAVE))
				{
					statement.setInt(1, ownerObjectId);
					statement.setInt(2, classIndex);
					statement.setInt(3, referenceSkill);
					try (ResultSet rset = statement.executeQuery())
					{
						while (rset.next())
						{
							final Skill skill = SkillData.getInstance().getSkill(rset.getInt("skill_id"), rset.getInt("skill_level"), rset.getInt("skill_sub_level"));
							if (skill == null)
							{
								continue;
							}
							
							if (skill.hasEffects(EffectScope.GENERAL))
							{
								final int effectCurrentTime = rset.getInt("remaining_time");
								summonEffectTable.addServitorEffect(ownerObjectId, classIndex, referenceSkill, new SummonEffect(skill, effectCurrentTime));
							}
						}
					}
				}
			}
			
			try (PreparedStatement statement = con.prepareStatement(DELETE_SKILL_SAVE))
			{
				statement.setInt(1, ownerObjectId);
				statement.setInt(2, classIndex);
				statement.setInt(3, referenceSkill);
				statement.executeUpdate();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore " + this + " active effect data: " + e.getMessage(), e);
		}
		finally
		{
			final Collection<SummonEffect> effects = summonEffectTable.getServitorEffectsForSkill(ownerObjectId, classIndex, referenceSkill);
			if (effects != null)
			{
				for (SummonEffect se : effects)
				{
					if (se != null)
					{
						se.getSkill().applyEffects(this, this, false, se.getEffectCurrentTime());
					}
				}
			}
		}
	}
	
	/**
	 * Removes the summon from the player.<br>
	 * This method cancels any active life tasks for the summon.<br>
	 * It also removes the summon record if it is not set to be restored.
	 * @param owner The {@code Player} who owns this summon.
	 */
	@Override
	public void unSummon(Player owner)
	{
		if (_summonLifeTask != null)
		{
			_summonLifeTask.cancel(false);
		}
		
		super.unSummon(owner);
		
		if (!_restoreSummon)
		{
			SummonInfoTable.getInstance().removeServitor(owner, getObjectId());
		}
		
		owner.setRecallCreature(null);
	}
	
	/**
	 * Destroys an item from the owner's inventory based on the specified process.<br>
	 * This method delegates the destruction logic to the {@code Player} owner.
	 * @param process The type of item process being performed.
	 * @param objectId The unique identifier of the item to destroy.
	 * @param count The amount of the item to remove.
	 * @param reference The {@code WorldObject} associated with the item.
	 * @param sendMessage Whether to send a confirmation message to the player.
	 * @return {@code true} if the item was successfully destroyed, {@code false} otherwise.
	 */
	@Override
	public boolean destroyItem(ItemProcessType process, int objectId, long count, WorldObject reference, boolean sendMessage)
	{
		return getOwner().destroyItem(process, objectId, count, reference, sendMessage);
	}
	
	/**
	 * Destroys an item based on its unique ID and the specified process type.<br>
	 * This method delegates the destruction logic to the owner of this object.
	 * @param process The {@code ItemProcessType} used for the destruction.
	 * @param itemId The unique identifier of the item to be destroyed.
	 * @param count The amount of items to remove.
	 * @param reference The {@code WorldObject} associated with the item.
	 * @param sendMessage Whether to send a confirmation message to the player.
	 * @return {@code true} if the destruction was successful, {@code false} otherwise.
	 */
	@Override
	public boolean destroyItemByItemId(ItemProcessType process, int itemId, long count, WorldObject reference, boolean sendMessage)
	{
		return getOwner().destroyItemByItemId(process, itemId, count, reference, sendMessage);
	}
	
	/**
	 * Retrieves the attack element of this entity.<br>
	 * If an owner exists, it returns the owner's attack element.<br>
	 * Otherwise, it returns the default attack element from the parent class.
	 * @return the {@code AttributeType} of the attack element.
	 */
	@Override
	public AttributeType getAttackElement()
	{
		final Player owner = getOwner();
		if (owner != null)
		{
			return owner.getAttackElement();
		}
		
		return super.getAttackElement();
	}
	
	/**
	 * Retrieves the attack element value for a specific attribute.<br>
	 * This method checks if the owner exists first.<br>
	 * If an owner is present, it returns the owner's value.<br>
	 * Otherwise, it returns the base value from the parent class.
	 * @param attackAttribute The type of attack attribute to check.
	 * @return The integer value associated with the specified attribute.
	 */
	@Override
	public int getAttackElementValue(AttributeType attackAttribute)
	{
		if (getOwner() != null)
		{
			return (getOwner().getAttackElementValue(attackAttribute));
		}
		
		return super.getAttackElementValue(attackAttribute);
	}
	
	/**
	 * Retrieves the defense value for a specific element type.<br>
	 * This method checks the owner's defense first if one exists.<br>
	 * If no owner is present, it returns the default value from the parent class.
	 * @param defenseAttribute The {@code AttributeType} to check for.
	 * @return The integer defense value for the specified attribute.
	 */
	@Override
	public int getDefenseElementValue(AttributeType defenseAttribute)
	{
		if (getOwner() != null)
		{
			return (getOwner().getDefenseElementValue(defenseAttribute));
		}
		
		return super.getDefenseElementValue(defenseAttribute);
	}
	
	/**
	 * Checks if the current entity is a servitor.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the entity is a servitor.
	 */
	@Override
	public boolean isServitor()
	{
		return true;
	}
	
	/**
	 * Returns the current instance of this {@code Servitor}.<br>
	 * This method is used for fluent API chaining.
	 * @return the current {@code Servitor} object.
	 */
	@Override
	public Servitor asServitor()
	{
		return this;
	}
	
	@Override
	public void run()
	{
		// Out of combat a servitor uses up only a quarter of its life time, siege weapons always pay per minute.
		final int usedtime = (isInCombat() || (getTemplate().getRace() == Race.SIEGE_WEAPON)) ? 5000 : 1250;
		final boolean hasLifetime = _lifeTime > 0;
		if (hasLifetime)
		{
			_lifeTimeRemaining -= usedtime;
		}
		
		if (isDead() || !isSpawned())
		{
			if (_summonLifeTask != null)
			{
				_summonLifeTask.cancel(false);
			}
			return;
		}
		
		// Check if the summon's lifetime has ran out.
		if (hasLifetime && (_lifeTimeRemaining < 0))
		{
			sendPacket(SystemMessageId.YOUR_SERVITOR_PASSED_AWAY);
			unSummon(getOwner());
			return;
		}
		
		if (_consumeItemInterval > 0)
		{
			_consumeItemIntervalRemaining -= usedtime;
			
			// Check if it is time to consume another item.
			final long itemCount = _itemConsume.getCount();
			final int itemConsumeId = _itemConsume.getId();
			if ((_consumeItemIntervalRemaining <= 0) && (itemCount > 0) && (itemConsumeId > 0) && !isDead())
			{
				// Siege weapons warn their surroundings when the owner can afford only one more payment.
				if (getTemplate().getRace() == Race.SIEGE_WEAPON)
				{
					final long consumeCount = _itemConsume.getCount();
					final long ownedCount = getOwner().getInventory().getInventoryItemCount(itemConsumeId, -1);
					if ((ownedCount >= consumeCount) && (ownedCount < (consumeCount * 2)))
					{
						broadcastPacket(new NpcSay(getObjectId(), ChatType.GENERAL, getTemplate().getId(), "You come to lack in power."));
					}
				}
				
				if (destroyItemByItemId(null, itemConsumeId, itemCount, this, false))
				{
					final SystemMessage msg = new SystemMessage(SystemMessageId.A_SUMMONED_MONSTER_USES_S1);
					msg.addItemName(itemConsumeId);
					sendPacket(msg);
					
					// Reset
					_consumeItemIntervalRemaining = _consumeItemInterval;
				}
				else
				{
					sendPacket(SystemMessageId.SINCE_YOU_DO_NOT_HAVE_ENOUGH_ITEMS_TO_MAINTAIN_THE_SERVITOR_S_STAY_THE_SERVITOR_HAS_DISAPPEARED);
					unSummon(getOwner());
				}
			}
		}
		
		if (hasLifetime)
		{
			sendPacket(new SetSummonRemainTime(_lifeTime, _lifeTimeRemaining));
		}
		
		// Using same task to check if owner is in visible range.
		final WorldObject owner = getOwner();
		if (calculateDistance3D(owner) > 2000)
		{
			getAI().setIntentionFollow(owner);
		}
		else
		{
			// Return to owner when target is dead.
			final WorldObject target = getTarget();
			if ((target != null) && target.isCreature() && target.asCreature().isAlikeDead())
			{
				getAI().setIntentionFollow(owner);
			}
		}
	}
	
	/**
	 * Picks up a specific item from the world.<br>
	 * This method handles the logic for moving an item to the player's inventory.
	 * @param object The {@code WorldObject} representing the item to be picked up.
	 */
	@Override
	public void doPickupItem(WorldObject object)
	{
	}
	
	/**
	 * Switches a siege weapon between move mode and siege mode.<br>
	 * The switch takes 30 seconds, the siege weapon cannot move meanwhile nor while in siege mode.
	 */
	public void switchSiegeMode()
	{
		if (isSwitchingSiegeMode())
		{
			saySwitchState();
			return;
		}
		
		stopMove(null);
		_siegeMode = !_siegeMode;
		_siegeModeSwitchEnd = System.currentTimeMillis() + SIEGE_MODE_SWITCH_TIME;
		setImmobilized(true);
		ThreadPool.schedule(() -> setImmobilized(_siegeMode), SIEGE_MODE_SWITCH_TIME);
	}
	
	/**
	 * Checks whether the siege weapon can fire its cannon, which needs a finished switch to siege mode.<br>
	 * Otherwise the siege weapon says why it cannot fire.
	 * @return {@code true} if the cannon can fire, otherwise {@code false}.
	 */
	public boolean canFireCannon()
	{
		if (isSwitchingSiegeMode())
		{
			saySwitchState();
			return false;
		}
		
		if (!_siegeMode)
		{
			broadcastPacket(new CreatureSay(this, ChatType.NPC_GENERAL, getName(), "You can use skills even while sitting."));
			return false;
		}
		
		return true;
	}
	
	/**
	 * Checks whether the siege weapon is switching between move mode and siege mode.
	 * @return {@code true} while the 30 second switch runs, otherwise {@code false}.
	 */
	private boolean isSwitchingSiegeMode()
	{
		return System.currentTimeMillis() < _siegeModeSwitchEnd;
	}
	
	/**
	 * Says whether the siege weapon is sitting down into siege mode or standing up into move mode.
	 */
	private void saySwitchState()
	{
		broadcastPacket(new CreatureSay(this, ChatType.NPC_GENERAL, getName(), _siegeMode ? "is sitting down." : "is standing up."));
	}
}
