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
package org.l2jmobius.gameserver.entity.actor;

import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.SummonAI;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.data.sql.SummonInfoTable;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Team;
import org.l2jmobius.gameserver.entity.actor.holders.npc.AggroInfo;
import org.l2jmobius.gameserver.entity.actor.instance.SiegeFlag;
import org.l2jmobius.gameserver.entity.actor.stat.SummonStat;
import org.l2jmobius.gameserver.entity.actor.status.SummonStatus;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.EtcItem;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.ActionType;
import org.l2jmobius.gameserver.entity.itemcontainer.PetInventory;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneRegion;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerSummonSpawn;
import org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGameManager;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.NpcInfoType;
import org.l2jmobius.gameserver.network.serverpackets.AbstractComponentPacket;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExDamagePopUp;
import org.l2jmobius.gameserver.network.serverpackets.ExMagicAttackInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExPartyPetWindowAdd;
import org.l2jmobius.gameserver.network.serverpackets.ExPartyPetWindowDelete;
import org.l2jmobius.gameserver.network.serverpackets.ExPartyPetWindowUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ExPetInfo;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PetDelete;
import org.l2jmobius.gameserver.network.serverpackets.PetItemList;
import org.l2jmobius.gameserver.network.serverpackets.PetStatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PetSummonInfo;
import org.l2jmobius.gameserver.network.serverpackets.RelationChanged;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SummonInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.TeleportToLocation;
import org.l2jmobius.gameserver.taskmanagers.DecayTaskManager;
import org.l2jmobius.gameserver.util.ArrayUtil;

/**
 * Represents a summonable entity that can be controlled or commanded by a player.<br>
 * This class serves as the base for all types of summons in the game world.<br>
 * It handles core behaviors such as stats, inventory, and AI logic.
 */
public abstract class Summon extends Playable
{
	private Player _owner;
	private int _attackRange = 36; // Melee range
	private boolean _follow = true;
	private boolean _previousFollowStatus = true;
	protected boolean _restoreSummon = true;
	private int _summonPoints = 0;
	private ScheduledFuture<?> _abnormalEffectTask;
	
	// @formatter:off
	private static final int[] PASSIVE_SUMMONS =
	{
		12564, 12621, 14702, 14703, 14704, 14705, 14706, 14707, 14708, 14709, 14710, 14711,
		14712, 14713, 14714, 14715, 14716, 14717, 14718, 14719, 14720, 14721, 14722, 14723,
		14724, 14725, 14726, 14727, 14728, 14729, 14730, 14731, 14732, 14733, 14734, 14735, 14736
	};
	// @formatter:on
	
	/**
	 * Creates a new {@code Summon} instance based on a template.<br>
	 * This constructor initializes the summon with its owner and sets up its AI.<br>
	 * It also finds a valid spawn location near the owner to avoid walls.
	 * @param template The {@code NpcTemplate} used to define the summon's properties.
	 * @param owner The {@code Player} who owns this summon.
	 */
	public Summon(NpcTemplate template, Player owner)
	{
		super(template);
		setInstanceType(InstanceType.Summon);
		setInstance(owner.getInstanceWorld()); // Set instance to same as owner.
		setShowSummonAnimation(true);
		_owner = owner;
		getAI();
		
		// Make sure summon does not spawn in a wall.
		final int x = owner.getX();
		final int y = owner.getY();
		final int z = owner.getZ();
		final Location location = GeoEngine.getInstance().getValidLocation(x, y, z, x + Rnd.get(-100, 100), y + Rnd.get(-100, 100), z, getInstanceWorld());
		setXYZInvisible(location.getX(), location.getY(), location.getZ());
	}
	
	/**
	 * This method is called when the summon is first created in the world.<br>
	 * It initializes the summon status and follows behavior.<br>
	 * It also handles network packets to notify nearby players of the new summon.<br>
	 * Finally, it triggers any relevant spawn events for scripts.
	 */
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		
		if (PlayerConfig.SUMMON_STORE_SKILL_COOLTIME && !isTeleporting())
		{
			restoreEffects();
		}
		
		setFollowStatus(true);
		updateAndBroadcastStatus(0);
		sendPacket(new RelationChanged(this, _owner.getRelation(_owner), false));
		World.forEachVisibleObject(getOwner(), Player.class, player ->
		{
			if (!isVisibleFor(player))
			{
				return;
			}
			
			player.sendPacket(new RelationChanged(this, _owner.getRelation(player), isAutoAttackable(player)));
		});
		final Party party = _owner.getParty();
		if (party != null)
		{
			party.broadcastToPartyMembers(_owner, new ExPartyPetWindowAdd(this));
		}
		
		setShowSummonAnimation(false); // AddVisibleObject created the info packets with summon animation.
		
		// If someone comes into range now, the animation shouldn't show any more.
		_restoreSummon = false;
		rechargeShots(true, true, false);
		
		// Notify to scripts.
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_SUMMON_SPAWN, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerSummonSpawn(this), this);
		}
	}
	
	/**
	 * Restores the summon's current health and mana.<br>
	 * This method schedules a task to set these values to their maximum amounts.<br>
	 * It provides a small delay to allow for stat recalculations.
	 */
	@Override
	public void fullRestore()
	{
		ThreadPool.schedule(() ->
		{
			setCurrentHp(getMaxHp());
			setCurrentMp(getMaxMp());
		}, 100 /* Compensate for recalculateStats task delay. */);
	}
	
	/**
	 * Retrieves the current statistics for this summon.<br>
	 * This method returns a {@code SummonStat} object containing all relevant data.
	 * @return the {@code SummonStat} of the summon.
	 */
	@Override
	public SummonStat getStat()
	{
		return (SummonStat) super.getStat();
	}
	
	/**
	 * Initializes the {@code SummonStat} for this entity.<br>
	 * This method creates and sets a new instance of {@code SummonStat}.
	 */
	@Override
	public void initCharStat()
	{
		setStat(new SummonStat(this));
	}
	
	/**
	 * Retrieves the current status of the summon.<br>
	 * This method returns the {@code SummonStatus} associated with this entity.
	 * @return the current {@code SummonStatus}
	 */
	@Override
	public SummonStatus getStatus()
	{
		return (SummonStatus) super.getStatus();
	}
	
	/**
	 * Initializes the status of this summon.<br>
	 * This method creates a new {@code SummonStatus} object for the current entity.
	 */
	@Override
	public void initCharStatus()
	{
		setStatus(new SummonStatus(this));
	}
	
	/**
	 * Initializes the artificial intelligence for this summon.<br>
	 * This method creates and returns a new {@code SummonAI} instance.
	 * @return The initialized {@code CreatureAI} object.
	 */
	@Override
	protected CreatureAI initAI()
	{
		return new SummonAI(this);
	}
	
	/**
	 * Retrieves the {@code NpcTemplate} associated with this entity.<br>
	 * This method returns the base data used to initialize the NPC.
	 * @return The {@code NpcTemplate} for this actor.
	 */
	@Override
	public NpcTemplate getTemplate()
	{
		return (NpcTemplate) super.getTemplate();
	}
	
	// This defines the action buttons, 1 for Summon, 2 for Pets.
	public abstract int getSummonType();
	
	/**
	 * Stops all active effects on this entity.<br>
	 * This method calls the superclass implementation to clear base effects.<br>
	 * It then updates and broadcasts the current status of the actor.
	 */
	@Override
	public void stopAllEffects()
	{
		super.stopAllEffects();
		updateAndBroadcastStatus(1);
	}
	
	/**
	 * Stops all active effects on the entity.<br>
	 * Effects that are designed to persist through death will remain active.<br>
	 * This method ensures that only temporary status changes are cleared.
	 */
	@Override
	public void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		super.stopAllEffectsExceptThoseThatLastThroughDeath();
		updateAndBroadcastStatus(1);
	}
	
	/**
	 * Updates the visual effects for abnormal statuses.<br>
	 * This method schedules a task to send packets to nearby players.<br>
	 * It ensures that visible objects receive updated {@code ABNORMALS} information.
	 */
	@Override
	public void updateAbnormalVisualEffects()
	{
		if (_abnormalEffectTask == null)
		{
			_abnormalEffectTask = ThreadPool.schedule(() ->
			{
				if (isSpawned())
				{
					World.forEachVisibleObject(this, Player.class, player ->
					{
						if (!isVisibleFor(player))
						{
							return;
						}
						
						if (player == _owner)
						{
							player.sendPacket(new PetSummonInfo(this, 1));
							return;
						}
						
						final AbstractComponentPacket<NpcInfoType> packet;
						if (isPet())
						{
							packet = new ExPetInfo(this, player, 1);
						}
						else
						{
							packet = new SummonInfo(this, player, 1);
						}
						
						packet.addComponentType(NpcInfoType.ABNORMALS);
						player.sendPacket(packet);
					});
				}
				
				_abnormalEffectTask = null;
			}, 50);
		}
	}
	
	/**
	 * Checks if this summon can be used as a mount.<br>
	 * This method currently always returns {@code false}.
	 * @return {@code true} if the summon is mountable, {@code false} otherwise.
	 */
	public boolean isMountable()
	{
		return false;
	}
	
	/**
	 * Retrieves the experience required to reach the current level.<br>
	 * This method checks if the entity has reached the maximum pet level.<br>
	 * If it is at or above the max level, it returns 0.<br>
	 * Otherwise, it fetches the value from {@code ExperienceData}.
	 * @return The experience amount required for the current level.
	 */
	public long getExpForThisLevel()
	{
		final int level = getLevel();
		if (level >= ExperienceData.getInstance().getMaxPetLevel())
		{
			return 0;
		}
		
		return ExperienceData.getInstance().getExpForLevel(level);
	}
	
	/**
	 * Calculates the experience required to reach the next level.<br>
	 * It checks if the current level is at the maximum allowed limit.<br>
	 * If it is at the max level, it returns {@code 0}.<br>
	 * Otherwise, it retrieves the value from {@code ExperienceData}.
	 * @return The experience amount needed for the next level.
	 */
	public long getExpForNextLevel()
	{
		final int level = getLevel();
		if (level >= (ExperienceData.getInstance().getMaxPetLevel() - 1))
		{
			return 0;
		}
		
		return ExperienceData.getInstance().getExpForLevel(level + 1);
	}
	
	/**
	 * Retrieves the reputation of the owner.<br>
	 * If the owner is {@code null}, it returns {@code 0}.
	 * @return The reputation value as an {@code int}.
	 */
	@Override
	public int getReputation()
	{
		return _owner != null ? _owner.getReputation() : 0;
	}
	
	/**
	 * Retrieves the PvP flag from the owner.<br>
	 * If the owner is {@code null}, it returns {@code 0}.
	 * @return The PvP flag of the owner as a {@code byte}.
	 */
	@Override
	public byte getPvpFlag()
	{
		return _owner != null ? _owner.getPvpFlag() : 0;
	}
	
	/**
	 * Retrieves the team associated with the owner of this summon.<br>
	 * If the owner is {@code null}, it returns {@code Team.NONE}.
	 * @return The {@code Team} of the owner or {@code Team.NONE}.
	 */
	@Override
	public Team getTeam()
	{
		return _owner != null ? _owner.getTeam() : Team.NONE;
	}
	
	/**
	 * Retrieves the {@code Player} that owns this summon.<br>
	 * This method returns the owner assigned during initialization.
	 * @return the {@code Player} who owns this entity.
	 */
	public Player getOwner()
	{
		return _owner;
	}
	
	/**
	 * Retrieves the unique identifier for this summon.<br>
	 * This value is obtained from the associated {@code NpcTemplate}.
	 * @return The {@code int} ID of the template.
	 */
	@Override
	public int getId()
	{
		return getTemplate().getId();
	}
	
	/**
	 * Retrieves the number of soul shots fired per hit.<br>
	 * This value is taken from the {@code NpcTemplate}.<br>
	 * If no specific value is set, it returns {@code 1}.
	 * @return The number of soul shots per hit as a {@code short}.
	 */
	public short getSoulShotsPerHit()
	{
		final int soulShot = getTemplate().getSoulShot();
		if (soulShot > 0)
		{
			return (short) soulShot;
		}
		
		return 1;
	}
	
	/**
	 * Retrieves the number of spirit shots fired per hit.<br>
	 * This value is taken from the {@code NpcTemplate}.<br>
	 * If no specific value is set, it defaults to {@code 1}.
	 * @return The number of spirit shots per hit as a {@code short}.
	 */
	public short getSpiritShotsPerHit()
	{
		final int spiritShot = getTemplate().getSpiritShot();
		if (spiritShot > 0)
		{
			return (short) spiritShot;
		}
		
		return 1;
	}
	
	/**
	 * Sets the summon to follow its owner.<br>
	 * This method updates the {@code SummonStatus} to enable following behavior.
	 */
	public void followOwner()
	{
		setFollowStatus(true);
	}
	
	/**
	 * Handles the logic for when this creature dies.<br>
	 * It checks for specific blessings and manages owner-related effects.<br>
	 * This method also triggers decay tasks and updates nearby targets.
	 * @param killer The {@code Creature} that caused the death.
	 * @return {@code true} if the death was successful, {@code false} otherwise.
	 */
	@Override
	public boolean doDie(Creature killer)
	{
		if (isNoblesseBlessedAffected())
		{
			stopEffects(EffectFlag.NOBLESS_BLESSING);
			storeEffect(true);
		}
		else
		{
			storeEffect(false);
		}
		
		if (!super.doDie(killer))
		{
			return false;
		}
		
		if (_owner != null)
		{
			World.forEachVisibleObject(this, Attackable.class, target ->
			{
				if (target.isDead())
				{
					return;
				}
				
				final AggroInfo info = target.getAggroList().get(this);
				if (info != null)
				{
					target.addDamageHate(_owner, info.getDamage(), info.getHate());
				}
			});
		}
		
		addKillerHate(killer);
		
		DecayTaskManager.getInstance().add(this);
		return true;
	}
	
	/**
	 * Handles the logic for when a creature dies.<br>
	 * It checks if the death is successful before proceeding.<br>
	 * If the creature did not decay, it adds itself to the decay task manager.
	 * @param killer The {@code Creature} that caused the death.
	 * @param decayed Whether the creature has already decayed.
	 * @return {@code true} if the death was processed successfully, {@code false} otherwise.
	 */
	public boolean doDie(Creature killer, boolean decayed)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		addKillerHate(killer);
		
		if (!decayed)
		{
			DecayTaskManager.getInstance().add(this);
		}
		
		return true;
	}
	
	/**
	 * Retail monster AIs add 500 hate against the owner to the monster that finishes a summon or pet.
	 * @param killer the creature that killed this summon
	 */
	private void addKillerHate(Creature killer)
	{
		if ((_owner != null) && (killer != null) && killer.isAttackable() && !killer.isDead())
		{
			killer.asAttackable().addDamageHate(_owner, 0, 500);
		}
	}
	
	/**
	 * Stops the decay process for this entity.<br>
	 * This method cancels the scheduled task in {@code DecayTaskManager}.
	 */
	public void stopDecay()
	{
		DecayTaskManager.getInstance().cancel(this);
	}
	
	/**
	 * Handles the logic when a summon decays.<br>
	 * This method unsummons and deletes the entity from the world.
	 */
	@Override
	public void onDecay()
	{
		unSummon(_owner);
		deleteMe(_owner);
	}
	
	/**
	 * Sends a status update to all nearby players.<br>
	 * This method updates the current status and broadcasts it.
	 * @param caster The {@code Creature} that triggered the update.
	 */
	@Override
	public void broadcastStatusUpdate(Creature caster)
	{
		super.broadcastStatusUpdate(caster);
		updateAndBroadcastStatus(1);
	}
	
	/**
	 * Removes the summon from the game world.<br>
	 * This method handles cleanup for both pets and servitors.<br>
	 * It notifies the owner and any party members of the removal.<br>
	 * All items in the summon inventory are destroyed during this process.
	 * @param owner The {@code Player} who owns this summon.
	 */
	public void deleteMe(Player owner)
	{
		super.deleteMe();
		
		final int objectId = getObjectId();
		if (owner != null)
		{
			owner.sendPacket(new PetDelete(getSummonType(), objectId));
			final Party party = owner.getParty();
			if (party != null)
			{
				party.broadcastToPartyMembers(owner, new ExPartyPetWindowDelete(this));
			}
			
			if (isPet())
			{
				owner.setPet(null);
			}
			else
			{
				owner.removeServitor(objectId);
			}
		}
		
		// Pet will be deleted along with all his items.
		if (getInventory() != null)
		{
			getInventory().destroyAllItems(ItemProcessType.DESTROY, _owner, this);
		}
		
		decayMe();
		
		if (!isPet())
		{
			SummonInfoTable.getInstance().removeServitor(_owner, objectId);
		}
	}
	
	/**
	 * Removes the summon from the game world.<br>
	 * This method stops all AI tasks and effects for the summon.<br>
	 * It also updates the owner's pet status and clears active targets.
	 * @param owner The {@code Player} who owns this summon.
	 */
	public void unSummon(Player owner)
	{
		if (isSpawned())
		{
			if (isDead())
			{
				stopDecay();
			}
			
			// Prevent adding effects while unsummoning.
			setInvul(true);
			
			abortAttack();
			abortCast();
			storeMe();
			storeEffect(true);
			
			// Stop AI tasks.
			if (hasAI())
			{
				getAI().stopAITask(); // Calls stopFollow as well.
			}
			
			// Cancel running skill casters.
			abortAllSkillCasters();
			
			stopAllEffects();
			stopHpMpRegeneration();
			
			if (owner != null)
			{
				if (isPet())
				{
					owner.setPet(null);
				}
				else
				{
					owner.removeServitor(getObjectId());
				}
				
				owner.sendPacket(new PetDelete(getSummonType(), getObjectId()));
				final Party party = owner.getParty();
				if (party != null)
				{
					party.broadcastToPartyMembers(owner, new ExPartyPetWindowDelete(this));
				}
				
				final PetInventory inventory = getInventory();
				if ((inventory != null) && (inventory.getSize() > 0))
				{
					_owner.setPetInvItems(true);
					sendPacket(SystemMessageId.THERE_ARE_ITEMS_IN_YOUR_PET_INVENTORY_RENDERING_YOU_UNABLE_TO_SELL_TRADE_DROP_PET_SUMMONING_ITEMS_PLEASE_EMPTY_YOUR_PET_INVENTORY);
				}
				else
				{
					_owner.setPetInvItems(false);
				}
			}
			
			final ZoneRegion oldRegion = ZoneManager.getInstance().getRegion(this);
			decayMe();
			oldRegion.removeFromZones(this);
			
			setTarget(null);
			if (owner != null)
			{
				for (int itemId : owner.getAutoSoulShot())
				{
					final String handler = ((EtcItem) ItemData.getInstance().getTemplate(itemId)).getHandlerName();
					if ((handler != null) && handler.contains("Beast"))
					{
						owner.disableAutoShot(itemId);
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves the current attack range of the summon.<br>
	 * This value determines how far away an enemy can be for the summon to strike it.
	 * @return The attack range as an {@code int}.
	 */
	public int getAttackRange()
	{
		return _attackRange;
	}
	
	/**
	 * Sets the maximum distance at which this entity can attack.<br>
	 * The value is clamped to a minimum of {@code 36}.
	 * @param range The desired attack range.
	 */
	public void setAttackRange(int range)
	{
		_attackRange = (range < 36) ? 36 : range;
	}
	
	/**
	 * Updates the follow status of this summon.<br>
	 * If {@code true}, the summon will begin following its owner.<br>
	 * If {@code false}, the summon will return to an idle state.
	 * @param value The new follow status to set.
	 */
	public void setFollowStatus(boolean value)
	{
		_follow = value;
		if (_follow)
		{
			getAI().setIntentionFollow(_owner);
		}
		else
		{
			getAI().setIntentionIdle();
		}
	}
	
	/**
	 * Checks if the summon is currently following its owner.
	 * @return {@code true} if the summon is following, {@code false} otherwise.
	 */
	public boolean getFollowStatus()
	{
		return _follow;
	}
	
	/**
	 * Checks if the owner of this summon can be attacked by a specific creature.<br>
	 * This method returns {@code true} only if the owner exists and is attackable.
	 * @param attacker The {@code Creature} attempting to perform an attack.
	 * @return {@code true} if the owner is attackable, {@code false} otherwise.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return (_owner != null) && _owner.isAutoAttackable(attacker);
	}
	
	/**
	 * Retrieves the unique identifier for the control object.<br>
	 * This method currently returns a default value of {@code 0}.
	 * @return The integer ID of the control object.
	 */
	public int getControlObjectId()
	{
		return 0;
	}
	
	/**
	 * Retrieves the weapon currently being used by the actor.<br>
	 * This method returns {@code null} if no weapon is equipped.
	 * @return the current {@code Weapon} object or {@code null}
	 */
	public Weapon getActiveWeapon()
	{
		return null;
	}
	
	/**
	 * Retrieves the inventory of the summon.<br>
	 * This method currently returns {@code null}.
	 * @return the {@code PetInventory} object or {@code null}.
	 */
	@Override
	public PetInventory getInventory()
	{
		return null;
	}
	
	/**
	 * Sets whether the summon should be restored.<br>
	 * This determines if a summoned creature reappears after being destroyed.
	 * @param value The boolean state to set for restoration.
	 */
	public void setRestoreSummon(boolean value)
	{
	}
	
	/**
	 * Retrieves the current weapon instance being used by the actor.<br>
	 * This method returns {@code null} if no weapon is currently active.
	 * @return The active {@code Item} instance or {@code null}.
	 */
	@Override
	public Item getActiveWeaponInstance()
	{
		return null;
	}
	
	/**
	 * Retrieves the weapon currently held by the actor.<br>
	 * This method returns {@code null} if no weapon is equipped.
	 * @return the current {@code Weapon} object or {@code null}.
	 */
	@Override
	public Weapon getActiveWeaponItem()
	{
		return null;
	}
	
	/**
	 * Retrieves the secondary weapon instance for this entity.<br>
	 * This method currently returns {@code null}.
	 * @return the {@code Item} representing the secondary weapon, or {@code null} if none exists.
	 */
	@Override
	public Item getSecondaryWeaponInstance()
	{
		return null;
	}
	
	/**
	 * Retrieves the secondary weapon of the actor.<br>
	 * This method currently returns {@code null}.
	 * @return the {@code Weapon} object if it exists, or {@code null} otherwise.
	 */
	@Override
	public Weapon getSecondaryWeaponItem()
	{
		return null;
	}
	
	/**
	 * Checks if the entity is currently invincible.<br>
	 * This returns {@code true} if the entity itself is invulnerable or if its owner has spawn protection.
	 * @return {@code true} if the entity cannot be damaged, {@code false} otherwise.
	 */
	@Override
	public boolean isInvul()
	{
		return super.isInvul() || _owner.isSpawnProtected();
	}
	
	/**
	 * Retrieves the {@code Party} associated with the owner of this summon.<br>
	 * This method returns {@code null} if the owner is not set.
	 * @return the {@code Party} object, or {@code null} if no owner exists.
	 */
	@Override
	public Party getParty()
	{
		if (_owner == null)
		{
			return null;
		}
		
		return _owner.getParty();
	}
	
	/**
	 * Checks if the owner of this summon is currently in a party.<br>
	 * This method returns {@code true} only if the owner exists and belongs to a party.
	 * @return {@code true} if the owner is in a party, {@code false} otherwise.
	 */
	@Override
	public boolean isInParty()
	{
		return (_owner != null) && _owner.isInParty();
	}
	
	/**
	 * Attempts to cast a magic skill using a specific item.<br>
	 * This method validates requirements like MP, HP, and cooldowns.<br>
	 * It also determines the correct target based on the provided flags.
	 * @param skill The {@code Skill} to be used.
	 * @param item The {@code Item} required for the cast.
	 * @param forceUse Whether to ignore certain target restrictions.
	 * @param dontMove Whether the summon should remain stationary while casting.
	 * @return {@code true} if the skill was successfully initiated, {@code false} otherwise.
	 */
	@Override
	public boolean useMagic(Skill skill, Item item, boolean forceUse, boolean dontMove)
	{
		// Null skill, dead summon or null owner are reasons to prevent casting.
		if ((skill == null) || isDead() || (_owner == null))
		{
			return false;
		}
		
		// Check if the skill is active.
		if (skill.isPassive())
		{
			// Just ignore the passive skill request. Why does the client send it anyway??
			return false;
		}
		
		// If a skill is currently being used.
		if (isCastingNow(SkillCaster::isAnyNormalType))
		{
			return false;
		}
		
		// Get the target for the skill.
		WorldObject target;
		if (skill.getTargetType() == TargetType.OWNER_PET)
		{
			target = _owner;
		}
		else
		{
			final WorldObject currentTarget = _owner.getTarget();
			if (currentTarget != null)
			{
				target = skill.getTarget(this, forceUse && (!currentTarget.isPlayable() || !currentTarget.isInsideZone(ZoneId.PEACE) || !currentTarget.isInsideZone(ZoneId.NO_PVP)), dontMove, false);
				final Player currentTargetPlayer = currentTarget.asPlayer();
				if (!forceUse && (currentTargetPlayer != null) && !currentTargetPlayer.isAutoAttackable(_owner))
				{
					sendPacket(SystemMessageId.INVALID_TARGET);
					return false;
				}
			}
			else
			{
				target = skill.getTarget(this, forceUse, dontMove, false);
			}
		}
		
		// Check the validity of the target.
		if (target == null)
		{
			if (!isMovementDisabled())
			{
				setTarget(_owner.getTarget());
				target = skill.getTarget(this, forceUse, dontMove, false);
			}
			
			if (target == null)
			{
				sendPacket(SystemMessageId.YOUR_TARGET_CANNOT_BE_FOUND);
				return false;
			}
		}
		
		// Siege weapons only attack castle gates, walls and headquarters.
		if (skill.hasNegativeEffect() && !canSiegeWeaponTarget(target))
		{
			sendPacket(SystemMessageId.INVALID_TARGET);
			return false;
		}
		
		// The Wild Hog Cannon fires its cannon only in siege mode.
		if ((skill.getId() == 4230) && isServitor() && !asServitor().canFireCannon())
		{
			return false;
		}
		
		// Check if this skill is enabled (e.g. reuse time).
		if (isSkillDisabled(skill))
		{
			sendPacket(SystemMessageId.THAT_PET_SERVITOR_SKILL_CANNOT_BE_USED_BECAUSE_IT_IS_RECHARGING);
			return false;
		}
		
		// Check if the summon has enough MP.
		if (getCurrentMp() < (getStat().getMpConsume(skill) + getStat().getMpInitialConsume(skill)))
		{
			// Send a System Message to the caster.
			sendPacket(SystemMessageId.NOT_ENOUGH_MP);
			return false;
		}
		
		// Check if the summon has enough HP.
		if (getCurrentHp() <= skill.getHpConsume())
		{
			// Send a System Message to the caster.
			sendPacket(SystemMessageId.NOT_ENOUGH_HP);
			return false;
		}
		
		// Check if all casting conditions are completed.
		if (!skill.checkCondition(this, target, true))
		{
			// Send a Server->Client packet ActionFailed to the Player.
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if this is bad magic skill and if Player is in Olympiad and the match isn't already start, send a Server->Client packet ActionFailed.
		if (skill.hasNegativeEffect() && _owner.isInOlympiadMode() && !_owner.isOlympiadStart())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Notify the AI with CAST and target.
		getAI().setIntentionCast(skill, target);
		return true;
	}
	
	/**
	 * Sets whether the actor is currently immobilized.<br>
	 * If {@code true}, it disables the follow status if it was active.<br>
	 * If {@code false}, it restores the previous follow status.
	 * @param value The immobilization state to set.
	 */
	@Override
	public void setImmobilized(boolean value)
	{
		super.setImmobilized(value);
		
		if (value)
		{
			_previousFollowStatus = _follow;
			
			// If immobilized temporarily disable follow mode.
			if (_previousFollowStatus)
			{
				setFollowStatus(false);
			}
		}
		else
		{
			// If not more immobilized restore previous follow mode.
			setFollowStatus(_previousFollowStatus);
		}
	}
	
	/**
	 * Sets the owner of this summon.<br>
	 * This method updates the {@code _owner} field to a new {@code Player}.
	 * @param newOwner The new {@code Player} who will own this summon.
	 */
	public void setOwner(Player newOwner)
	{
		_owner = newOwner;
	}
	
	/**
	 * Sends a damage message to the target creature.<br>
	 * This method handles critical hits, blocked attacks, and olympiad notifications.<br>
	 * It prevents sending messages if the attack misses or the owner is null.<br>
	 * Messages are not sent if the target is the owner of this summon.
	 * @param target The {@code Creature} receiving the damage.
	 * @param skill The {@code Skill} used to perform the attack.
	 * @param damage The amount of damage dealt.
	 * @param crit Set to {@code true} if the hit is a critical strike.
	 * @param miss Set to {@code true} if the attack failed to hit.
	 */
	@Override
	public void sendDamageMessage(Creature target, Skill skill, int damage, boolean crit, boolean miss)
	{
		if (miss || (_owner == null))
		{
			sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), 0, ExDamagePopUp.DODGE, skill));
			
			// target.sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), 0, ExDamagePopUp.DODGE));
			return;
		}
		
		// Prevents the double spam of system messages, if the target is the owning player.
		if (target.getObjectId() != _owner.getObjectId())
		{
			if (crit)
			{
				if (isServitor())
				{
					if (skill != null)
					{
						sendPacket(SystemMessageId.M_CRITICAL);
						sendPacket(new ExMagicAttackInfo(getObjectId(), target.getObjectId(), ExMagicAttackInfo.M_CRITICAL));
					}
					else
					{
						sendPacket(SystemMessageId.SUMMONED_MONSTER_S_CRITICAL_HIT);
					}
				}
				else
				{
					sendPacket(SystemMessageId.PET_S_CRITICAL_HIT);
				}
			}
			
			if (_owner.isInOlympiadMode() && target.isPlayer() && target.asPlayer().isInOlympiadMode() && (target.asPlayer().getOlympiadGameId() == _owner.getOlympiadGameId()))
			{
				OlympiadGameManager.getInstance().notifyCompetitorDamage(getOwner(), damage);
			}
			
			final SystemMessage sm;
			if ((target.isHpBlocked() && !target.isNpc()) || (target.isPlayer() && target.isAffected(EffectFlag.DUELIST_FURY) && !_owner.isAffected(EffectFlag.FACEOFF)))
			{
				final int objectId = getObjectId();
				final int targetObjectId = target.getObjectId();
				if (skill == null)
				{
					sendPacket(new ExDamagePopUp(objectId, targetObjectId, 0, ExDamagePopUp.IMMUNE, skill));
					// target.sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), 0, ExDamagePopUp.IMMUNE));
				}
				else
				{
					sendPacket(new ExMagicAttackInfo(objectId, targetObjectId, ExMagicAttackInfo.RESISTED));
					// target.sendPacket(new ExMagicAttackInfo(getObjectId(), target.getObjectId(), ExMagicAttackInfo.RESISTED));
				}
				
				sm = new SystemMessage(SystemMessageId.THE_ATTACK_HAS_BEEN_BLOCKED);
			}
			else
			{
				final int objectId = getObjectId();
				final int targetObjectId = target.getObjectId();
				if (crit)
				{
					sendPacket(new ExDamagePopUp(objectId, targetObjectId, damage, ExDamagePopUp.CRITICAL, skill));
					target.sendPacket(new ExDamagePopUp(objectId, targetObjectId, damage, ExDamagePopUp.CRITICAL, skill));
				}
				else if (skill != null)
				{
					sendPacket(new ExDamagePopUp(objectId, targetObjectId, damage, ExDamagePopUp.SKILL_HIT, skill));
					target.sendPacket(new ExDamagePopUp(objectId, targetObjectId, damage, ExDamagePopUp.SKILL_HIT, skill));
				}
				else
				{
					sendPacket(new ExDamagePopUp(objectId, targetObjectId, damage, ExDamagePopUp.NORMAL_ATTACK, skill));
					target.sendPacket(new ExDamagePopUp(objectId, targetObjectId, damage, ExDamagePopUp.NORMAL_ATTACK, skill));
				}
				
				sm = new SystemMessage(SystemMessageId.C1_HAS_DEALT_S3_DAMAGE_TO_C2);
				sm.addNpcName(this);
				sm.addString(target.getName());
				sm.addInt(damage);
				sm.addPopup(targetObjectId, objectId, (damage * -1));
			}
			
			sendPacket(sm);
		}
	}
	
	/**
	 * Reduces the current health of this creature based on incoming damage.<br>
	 * It also handles specific logic for displaying damage messages to the owner.<br>
	 * This method ensures that certain effects like duels or face-offs are respected.
	 * @param damage The amount of damage to subtract from the health.
	 * @param attacker The {@code Creature} that initiated the attack.
	 * @param skill The {@code Skill} used to perform the attack.
	 */
	@Override
	public void reduceCurrentHp(double damage, Creature attacker, Skill skill)
	{
		super.reduceCurrentHp(damage, attacker, skill);
		
		if (!isDead() && !isHpBlocked() && (_owner != null) && (attacker != null) && (!_owner.isAffected(EffectFlag.DUELIST_FURY) || attacker.isAffected(EffectFlag.FACEOFF)))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_RECEIVED_S3_DAMAGE_FROM_C2);
			sm.addNpcName(this);
			sm.addString(attacker.getName());
			sm.addInt((int) damage);
			sm.addPopup(getObjectId(), attacker.getObjectId(), (int) -damage);
			sendPacket(sm);
		}
	}
	
	/**
	 * Executes the casting logic for a specific {@code Skill}.<br>
	 * This method validates if the target is valid before proceeding.<br>
	 * If the target is invalid, it sends an error message to the owner.<br>
	 * Otherwise, it calls the superclass implementation of {@code doCast}.
	 * @param skill The {@code Skill} to be cast.
	 */
	@Override
	public void doCast(Skill skill)
	{
		if ((skill.getTarget(this, false, false, false) == null) && !_owner.getAccessLevel().allowPeaceAttack())
		{
			// Send a System Message to the Player.
			_owner.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
			
			// Send a Server->Client packet ActionFailed to the Player.
			_owner.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		super.doCast(skill);
	}
	
	/**
	 * Checks if the owner of this summon is currently in combat.<br>
	 * This method returns {@code true} only if the owner exists and is fighting.
	 * @return {@code true} if the owner is in combat, {@code false} otherwise.
	 */
	@Override
	public boolean isInCombat()
	{
		return (_owner != null) && _owner.isInCombat();
	}
	
	/**
	 * Retrieves the {@code Player} that owns this summon.
	 * @return the {@code Player} owner of this entity.
	 */
	@Override
	public Player asPlayer()
	{
		return _owner;
	}
	
	/**
	 * Updates the summon status and sends a notification to all relevant clients.<br>
	 * This method checks if the owner exists and if the summon is currently spawned.<br>
	 * It sends packets to the owner and broadcasts information to nearby players.<br>
	 * If the owner is in a party, it also updates the party members.
	 * @param value The status value to be updated and broadcast.
	 */
	public void updateAndBroadcastStatus(int value)
	{
		if (_owner == null)
		{
			return;
		}
		
		if (isSpawned())
		{
			sendPacket(new PetSummonInfo(this, value));
			sendPacket(new PetStatusUpdate(this));
			broadcastNpcInfo(value);
			
			final Party party = _owner.getParty();
			if (party != null)
			{
				party.broadcastToPartyMembers(_owner, new ExPartyPetWindowUpdate(this));
			}
		}
	}
	
	/**
	 * Sends information about this entity to all visible players.<br>
	 * It distinguishes between pets and other summons when sending packets.<br>
	 * The packet sent depends on whether the entity is a pet or not.
	 * @param value The specific value to include in the broadcast packet.
	 */
	public void broadcastNpcInfo(int value)
	{
		World.forEachVisibleObject(this, Player.class, player ->
		{
			if (!isVisibleFor(player))
			{
				return;
			}
			
			if (player == _owner)
			{
				return;
			}
			
			if (isPet())
			{
				player.sendPacket(new ExPetInfo(this, player, value));
			}
			else
			{
				player.sendPacket(new SummonInfo(this, player, value));
			}
		});
	}
	
	/**
	 * Checks if the current entity is hungry.<br>
	 * This method currently always returns {@code false}.
	 * @return {@code true} if the entity is hungry, otherwise {@code false}.
	 */
	public boolean isHungry()
	{
		return false;
	}
	
	/**
	 * Retrieves the current weapon of the summon.<br>
	 * This method returns a default value of {@code 0}.
	 * @return The unique identifier for the weapon.
	 */
	public int getWeapon()
	{
		return 0;
	}
	
	/**
	 * Retrieves the current armor value of the summon.<br>
	 * This method returns a default value of {@code 0}.
	 * @return The integer value representing the armor.
	 */
	public int getArmor()
	{
		return 0;
	}
	
	/**
	 * Sends the relevant summon information to a specific {@code Player}.<br>
	 * It determines which packet to send based on whether the player owns the summon.<br>
	 * If the player is the owner, it sends pet-specific details like inventory items.<br>
	 * Otherwise, it sends general summon or external pet information.
	 * @param player The {@code Player} who will receive the information packet.
	 */
	@Override
	public void sendInfo(Player player)
	{
		// Check if the Player is the owner of the Pet.
		if (player == _owner)
		{
			player.sendPacket(new PetSummonInfo(this, isDead() ? 0 : 1));
			if (isPet())
			{
				player.sendPacket(new PetItemList(getInventory().getItems()));
			}
		}
		else
		{
			if (isPet())
			{
				player.sendPacket(new ExPetInfo(this, player, 0));
			}
			else
			{
				player.sendPacket(new SummonInfo(this, player, 0));
			}
		}
	}
	
	/**
	 * This method is called when the entity is teleported to a new location.<br>
	 * It calls the superclass {@code onTeleported()} method.<br>
	 * It sends a {@code TeleportToLocation} packet to update the client position.
	 */
	@Override
	public void onTeleported()
	{
		super.onTeleported();
		sendPacket(new TeleportToLocation(this, getX(), getY(), getZ(), getHeading()));
	}
	
	/**
	 * Checks if the entity belongs to the undead race.<br>
	 * This method retrieves the race from the {@code NpcTemplate}.
	 * @return {@code true} if the entity is an undead, {@code false} otherwise.
	 */
	@Override
	public boolean isUndead()
	{
		return getTemplate().getRace() == Race.UNDEAD;
	}
	
	/**
	 * This method is a placeholder for switching modes.<br>
	 * It currently performs no actions.
	 */
	public void switchMode()
	{
		// Do nothing.
	}
	
	/**
	 * Cancels the current action of the summon.<br>
	 * This method enables the AI intention if movement is not disabled.
	 */
	public void cancelAction()
	{
		if (!isMovementDisabled())
		{
			getAI().setIntentionActive();
		}
	}
	
	/**
	 * Initiates an attack against a specific target.<br>
	 * This method sets the new target and updates the AI intention.<br>
	 * It also handles PvP status updates if the target is a fake player.
	 * @param target The {@code WorldObject} to be attacked.
	 */
	public void doAttack(WorldObject target)
	{
		// Siege weapons only attack castle gates, walls and headquarters.
		if (!canSiegeWeaponTarget(target))
		{
			sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}
		
		if ((_owner != null) && (target != null))
		{
			setTarget(target);
			getAI().setIntentionAttack(target);
			if (target.isFakePlayer() && !FakePlayersConfig.FAKE_PLAYER_AUTO_ATTACKABLE)
			{
				_owner.updatePvPStatus();
			}
		}
	}
	
	/**
	 * Stops a siege weapon from auto attacking anything other than castle gates, walls and headquarters.<br>
	 * Stops Big Boom from auto attacking at all, its only attack is Boom Attack.
	 * @param target The {@code Creature} to attack.
	 */
	@Override
	public void doAutoAttack(Creature target)
	{
		// Big Boom only walks up to the target, the servitor knows Boom Attack (4139) and nothing else attacks.
		if (!canSiegeWeaponTarget(target) || (isServitor() && (getKnownSkill(4139) != null)))
		{
			getAI().setIntentionIdle();
			return;
		}
		
		super.doAutoAttack(target);
	}
	
	/**
	 * Checks whether this summon may attack the target.<br>
	 * Siege weapons only attack castle gates, walls and headquarters, other summons attack anything.
	 * @param target The {@code WorldObject} to attack.
	 * @return {@code true} if the target may be attacked, otherwise {@code false}.
	 */
	private boolean canSiegeWeaponTarget(WorldObject target)
	{
		return (getTemplate().getRace() != Race.SIEGE_WEAPON) || (target == null) || target.isDoor() || (target instanceof SiegeFlag);
	}
	
	/**
	 * Checks if the summon is allowed to attack a specific target.<br>
	 * This method validates various conditions such as ownership, level differences, and zone restrictions.<br>
	 * It also handles special cases for passive summons and siege mechanics.
	 * @param target The {@code WorldObject} that the summon is attempting to attack.
	 * @param ctrlPressed A boolean indicating if the control key is being held down.
	 * @return {@code true} if the attack is permitted, otherwise {@code false}.
	 */
	public boolean canAttack(WorldObject target, boolean ctrlPressed)
	{
		if (_owner == null)
		{
			return false;
		}
		
		if ((target == null) || (this == target) || (_owner == target))
		{
			return false;
		}
		
		// Sin eater, Big Boom, Wyvern can't attack with attack button.
		final int npcId = getId();
		if (ArrayUtil.contains(PASSIVE_SUMMONS, npcId))
		{
			_owner.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (isBetrayed())
		{
			sendPacket(SystemMessageId.YOUR_PET_SERVITOR_IS_UNRESPONSIVE_AND_WILL_NOT_OBEY_ANY_ORDERS);
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (isPet() && ((getLevel() - _owner.getLevel()) > 20))
		{
			sendPacket(SystemMessageId.YOUR_PET_S_LEVEL_IS_TOO_HIGH_YOU_CANNOT_CONTROL_IT);
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (_owner.isInOlympiadMode() && !_owner.isOlympiadStart())
		{
			// If owner is in Olympiad and the match isn't already start, send a Server->Client packet ActionFailed.
			_owner.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (_owner.isSiegeFriend(target))
		{
			sendPacket(SystemMessageId.FORCE_ATTACK_IS_IMPOSSIBLE_AGAINST_A_TEMPORARY_ALLIED_MEMBER_DURING_A_SIEGE);
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (!_owner.getAccessLevel().allowPeaceAttack() && _owner.isInsidePeaceZone(this, target))
		{
			sendPacket(SystemMessageId.YOU_CANNOT_ATTACK_THIS_TARGET_IN_A_PEACEFUL_ZONE);
			return false;
		}
		
		if (isLockedTarget())
		{
			sendPacket(SystemMessageId.FAILED_TO_CHANGE_ENMITY);
			return false;
		}
		
		if (!target.isAutoAttackable(_owner) && !ctrlPressed)
		{
			setFollowStatus(false);
			getAI().setIntentionFollow(target);
			sendPacket(SystemMessageId.INVALID_TARGET);
			return false;
		}
		
		// Siege golems AI doesn't support attacking other than doors/walls at the moment.
		if (target.isDoor() && (getTemplate().getRace() != Race.SIEGE_WEAPON))
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Sends a {@code ServerPacket} to the owner of this summon.<br>
	 * This method checks if the owner is not {@code null} before sending.
	 * @param packet The {@code ServerPacket} to be sent.
	 */
	@Override
	public void sendPacket(ServerPacket packet)
	{
		if (_owner != null)
		{
			_owner.sendPacket(packet);
		}
	}
	
	/**
	 * Sends a specific packet to the owner of this summon.<br>
	 * This method checks if the {@code _owner} is not {@code null} before sending.
	 * @param id The {@code SystemMessageId} of the packet to be sent.
	 */
	@Override
	public void sendPacket(SystemMessageId id)
	{
		if (_owner != null)
		{
			_owner.sendPacket(id);
		}
	}
	
	/**
	 * Checks if the current entity is a summon.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the entity is a summon.
	 */
	@Override
	public boolean isSummon()
	{
		return true;
	}
	
	/**
	 * Returns the current {@code Summon} instance.<br>
	 * This method is used for fluent API chaining.
	 * @return the current {@code Summon} object.
	 */
	@Override
	public Summon asSummon()
	{
		return this;
	}
	
	/**
	 * This method recharges shots for the summon based on the owner's auto soul shot configuration.<br>
	 * It checks the owner's inventory for valid items to use.<br>
	 * If an item is missing, it is removed from the auto soul shot list.
	 * @param physical Determines if physical soul shots should be processed.
	 * @param magic Determines if magic spirit shots should be processed.
	 * @param fish Determines if fishing related actions should be processed.
	 */
	@Override
	public void rechargeShots(boolean physical, boolean magic, boolean fish)
	{
		if ((_owner.getAutoSoulShot() == null) || _owner.getAutoSoulShot().isEmpty())
		{
			return;
		}
		
		Item item;
		IItemHandler handler;
		for (int itemId : _owner.getAutoSoulShot())
		{
			item = _owner.getInventory().getItemByItemId(itemId);
			if (item != null)
			{
				final ActionType defaultAction = item.getTemplate().getDefaultAction();
				if (magic && ((defaultAction == ActionType.SPIRITSHOT) || /* Old BeastSpiritShot item support. */ (defaultAction == ActionType.SUMMON_SPIRITSHOT)))
				{
					handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
					if (handler != null)
					{
						handler.onItemUse(_owner, item, false);
					}
				}
				
				if (physical && ((defaultAction == ActionType.SOULSHOT) || /* Old BeastSoulShot item support. */ (defaultAction == ActionType.SUMMON_SOULSHOT)))
				{
					handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
					if (handler != null)
					{
						handler.onItemUse(_owner, item, false);
					}
				}
			}
			else
			{
				_owner.removeAutoSoulShot(itemId);
			}
		}
	}
	
	/**
	 * Retrieves the clan identifier for the owner of this summon.<br>
	 * If the owner is {@code null}, it returns {@code 0}.
	 * @return The unique ID of the owner's clan.
	 */
	@Override
	public int getClanId()
	{
		return (_owner != null) ? _owner.getClanId() : 0;
	}
	
	/**
	 * Retrieves the unique identifier of the owner's ally.<br>
	 * If the owner is {@code null}, it returns {@code 0}.
	 * @return The ally ID of the owner or {@code 0} if no owner exists.
	 */
	@Override
	public int getAllyId()
	{
		return (_owner != null) ? _owner.getAllyId() : 0;
	}
	
	/**
	 * Sets the current amount of summon points for this entity.<br>
	 * This value is used to determine if a player can perform summoning actions.
	 * @param summonPoints The new amount of summon points to set.
	 */
	public void setSummonPoints(int summonPoints)
	{
		_summonPoints = summonPoints;
	}
	
	/**
	 * Retrieves the current amount of summon points.
	 * @return The total number of {@code summonPoints}.
	 */
	public int getSummonPoints()
	{
		return _summonPoints;
	}
	
	/**
	 * Sends an inventory update to the owner of this summon.<br>
	 * This method checks if the {@code _owner} is not {@code null}.<br>
	 * If the owner exists, it calls their {@code sendInventoryUpdate} method.
	 * @param iu The {@code InventoryUpdate} object containing the data to be sent.
	 */
	public void sendInventoryUpdate(InventoryUpdate iu)
	{
		final Player owner = _owner;
		if (owner != null)
		{
			owner.sendInventoryUpdate(iu);
		}
	}
	
	/**
	 * Checks if the entity is currently unable to move.<br>
	 * This returns {@code true} if movement is disabled by a status or if the template prevents it.
	 * @return {@code true} if movement is restricted, {@code false} otherwise.
	 */
	@Override
	public boolean isMovementDisabled()
	{
		return super.isMovementDisabled() || !getTemplate().canMove();
	}
	
	/**
	 * Checks if this entity can be targeted by other actors.<br>
	 * It verifies both the base class status and the specific template settings.
	 * @return {@code true} if the entity is targetable, {@code false} otherwise.
	 */
	@Override
	public boolean isTargetable()
	{
		return super.isTargetable() && getTemplate().isTargetable();
	}
	
	/**
	 * Checks if the owner of this summon is currently participating in an event.<br>
	 * This method returns {@code true} only if the owner exists and their {@code isOnEvent()} status is {@code true}.
	 * @return {@code true} if the owner is on an event, {@code false} otherwise.
	 */
	@Override
	public boolean isOnEvent()
	{
		return (_owner != null) && _owner.isOnEvent();
	}
	
	/**
	 * Returns a string representation of the {@code Summon} object.<br>
	 * This includes the base class information and the owner details.
	 * @return A formatted string containing the ID and Owner information.
	 */
	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append('(');
		sb.append(getId());
		sb.append(") Owner: ");
		sb.append(_owner);
		return sb.toString();
	}
}
