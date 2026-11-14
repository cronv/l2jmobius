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

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcInfo;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.StopMove;

// While a tamed beast behaves like a pet and has an owner in-game, it acts like a mob in all other respects.
// In addition, it can be fed in order to increase its duration.
// This class handles the running tasks, AI, and feed of the mob.
// The (mostly optional) AI on feeding the spawn is handled by the datapack ai script.
/**
 * Represents a beast that has been tamed by a {@code Player} to act as a pet.<br>
 * This class handles the behavior, AI, and feeding mechanics of the creature.<br>
 * It functions like a standard mob but maintains an owner relationship.
 */
public class TamedBeast extends FeedableBeast
{
	private int _foodSkillId;
	private static final int MAX_DISTANCE_FROM_HOME = 30000;
	private static final int MAX_DISTANCE_FROM_OWNER = 2000;
	private static final int MAX_DURATION = 1200000; // 20 minutes
	private static final int DURATION_CHECK_INTERVAL = 60000; // 1 minute
	private static final int DURATION_INCREASE_INTERVAL = 20000; // 20 secs (gained upon feeding)
	private static final int BUFF_INTERVAL = 5000; // 5 seconds
	private int _remainingTime = MAX_DURATION;
	private int _homeX;
	private int _homeY;
	private int _homeZ;
	protected Player _owner;
	private Future<?> _buffTask = null;
	private Future<?> _durationCheckTask = null;
	protected boolean _isFreyaBeast;
	private Collection<Skill> _beastSkills = null;
	
	/**
	 * Creates a new {@code TamedBeast} instance based on a template.<br>
	 * This constructor initializes the beast with its default properties.<br>
	 * It sets the instance type to {@code InstanceType.TamedBeast}.<br>
	 * The home location is automatically set to the current position.
	 * @param npcTemplateId The unique identifier for the NPC template.
	 */
	public TamedBeast(int npcTemplateId)
	{
		super(NpcData.getInstance().getTemplate(npcTemplateId));
		setInstanceType(InstanceType.TamedBeast);
		setHome(this);
	}
	
	/**
	 * Creates a new {@code TamedBeast} instance with specific owner and location data.<br>
	 * This constructor initializes the beast as a pet that can be fed to extend its duration.
	 * @param npcTemplateId The unique identifier for the NPC template.
	 * @param owner The {@code Player} who owns this beast.
	 * @param foodSkillId The skill ID associated with the food used to feed the beast.
	 * @param x The X coordinate for spawning.
	 * @param y The Y coordinate for spawning.
	 * @param z The Z coordinate for spawning.
	 */
	public TamedBeast(int npcTemplateId, Player owner, int foodSkillId, int x, int y, int z)
	{
		super(NpcData.getInstance().getTemplate(npcTemplateId));
		_isFreyaBeast = false;
		setInstanceType(InstanceType.TamedBeast);
		fullRestore();
		setOwner(owner);
		setFoodType(foodSkillId);
		setHome(x, y, z);
		spawnMe(x, y, z);
	}
	
	/**
	 * Creates a new {@code TamedBeast} instance with specific owner and location data.<br>
	 * This constructor initializes the beast's stats, home position, and food type.<br>
	 * It also sets up special behavior if the beast is a Freya beast.
	 * @param npcTemplateId The ID of the NPC template to use for this beast.
	 * @param owner The {@code Player} who owns this tamed beast.
	 * @param food The item ID used as food for this beast.
	 * @param x The X coordinate for spawning.
	 * @param y The Y coordinate for spawning.
	 * @param z The Z coordinate for spawning.
	 * @param isFreyaBeast Set to {@code true} if the beast should follow its owner automatically.
	 */
	public TamedBeast(int npcTemplateId, Player owner, int food, int x, int y, int z, boolean isFreyaBeast)
	{
		super(NpcData.getInstance().getTemplate(npcTemplateId));
		_isFreyaBeast = isFreyaBeast;
		setInstanceType(InstanceType.TamedBeast);
		fullRestore();
		setFoodType(food);
		setHome(x, y, z);
		spawnMe(x, y, z);
		setOwner(owner);
		if (isFreyaBeast)
		{
			getAI().setIntentionFollow(_owner);
		}
	}
	
	/**
	 * Handles the logic when the beast receives food.<br>
	 * This method increases the remaining duration of the beast.<br>
	 * The duration is capped at a maximum value.
	 */
	public void onReceiveFood()
	{
		// Eating food extends the duration by 20secs, to a max of 20minutes.
		_remainingTime += DURATION_INCREASE_INTERVAL;
		if (_remainingTime > MAX_DURATION)
		{
			_remainingTime = MAX_DURATION;
		}
	}
	
	/**
	 * Retrieves the home location of this beast.<br>
	 * It returns a new {@code Location} object based on the stored coordinates.
	 * @return the current home {@code Location}.
	 */
	public Location getHome()
	{
		return new Location(_homeX, _homeY, _homeZ);
	}
	
	/**
	 * Sets the home coordinates for this beast.<br>
	 * This updates the internal location values used to determine where the beast returns.
	 * @param x The new X coordinate.
	 * @param y The new Y coordinate.
	 * @param z The new Z coordinate.
	 */
	public void setHome(int x, int y, int z)
	{
		_homeX = x;
		_homeY = y;
		_homeZ = z;
	}
	
	/**
	 * Sets the home location of this beast to the current position of a {@code Creature}.<br>
	 * This method updates the internal coordinates based on the provided entity.
	 * @param c The {@code Creature} used to determine the new home location.
	 */
	public void setHome(Creature c)
	{
		setHome(c.getX(), c.getY(), c.getZ());
	}
	
	/**
	 * Gets the time remaining for the tamed beast.<br>
	 * This value represents how much longer the beast will remain active.
	 * @return The amount of time left as an {@code int}.
	 */
	public int getRemainingTime()
	{
		return _remainingTime;
	}
	
	/**
	 * Sets the remaining time for the tamed beast.<br>
	 * This value determines how long the beast stays active.
	 * @param duration The new time duration to set.
	 */
	public void setRemainingTime(int duration)
	{
		_remainingTime = duration;
	}
	
	/**
	 * Retrieves the ID of the food skill associated with this beast.<br>
	 * This value is used to determine which skill is triggered when feeding.
	 * @return the {@code int} ID of the food skill.
	 */
	public int getFoodType()
	{
		return _foodSkillId;
	}
	
	/**
	 * Sets the specific food item used to feed this beast.<br>
	 * This method updates the {@code _foodSkillId} and restarts the duration check task.<br>
	 * The update only occurs if the provided ID is greater than {@code 0}.
	 * @param foodItemId The unique identifier for the food item.
	 */
	public void setFoodType(int foodItemId)
	{
		if (foodItemId > 0)
		{
			_foodSkillId = foodItemId;
			
			// Start the duration checks.
			// Start the buff tasks.
			if (_durationCheckTask != null)
			{
				_durationCheckTask.cancel(true);
			}
			
			_durationCheckTask = ThreadPool.scheduleAtFixedRate(new CheckDuration(this), DURATION_CHECK_INTERVAL, DURATION_CHECK_INTERVAL);
		}
	}
	
	/**
	 * Handles the logic for when this beast dies.<br>
	 * It stops following behaviors and cancels active tasks.<br>
	 * It also cleans up references to the owner and other variables.
	 * @param killer The {@code Creature} that killed this beast.
	 * @return {@code true} if the death was successful, otherwise {@code false}.
	 */
	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		getAI().stopFollow();
		if (_buffTask != null)
		{
			_buffTask.cancel(true);
		}
		
		if (_durationCheckTask != null)
		{
			_durationCheckTask.cancel(true);
		}
		
		// Clean up variables.
		if (_owner != null)
		{
			_owner.getTrainedBeasts().remove(this);
		}
		
		_buffTask = null;
		_durationCheckTask = null;
		_owner = null;
		_foodSkillId = 0;
		_remainingTime = 0;
		return true;
	}
	
	/**
	 * Checks if this beast can be attacked automatically.<br>
	 * This method returns {@code false} if the beast is a Freya beast.<br>
	 * Otherwise, it returns {@code true}.
	 * @param attacker The {@code Creature} attempting to attack.
	 * @return {@code true} if auto-attackable, {@code false} otherwise.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return !_isFreyaBeast;
	}
	
	/**
	 * Checks if this beast is a Freya beast.<br>
	 * This property determines specific behavior for the tamed creature.
	 * @return {@code true} if it is a Freya beast, {@code false} otherwise.
	 */
	public boolean isFreyaBeast()
	{
		return _isFreyaBeast;
	}
	
	/**
	 * Adds a new skill to the beast's list of available skills.<br>
	 * This method initializes the internal collection if it is {@code null}.
	 * @param skill The {@code Skill} object to be added.
	 */
	public void addBeastSkill(Skill skill)
	{
		if (_beastSkills == null)
		{
			_beastSkills = ConcurrentHashMap.newKeySet();
		}
		
		_beastSkills.add(skill);
	}
	
	/**
	 * This method triggers the skills of a tamed beast.<br>
	 * It checks if the beast has an owner and active skills.<br>
	 * Skills are scheduled to be cast with specific delays based on their hit time.
	 */
	public void castBeastSkills()
	{
		if ((_owner == null) || (_beastSkills == null))
		{
			return;
		}
		
		int delay = 100;
		for (Skill skill : _beastSkills)
		{
			ThreadPool.schedule(new buffCast(skill), delay);
			delay += (100 + skill.getHitTime());
		}
		
		ThreadPool.schedule(new buffCast(null), delay);
	}
	
	private class buffCast implements Runnable
	{
		private final Skill _skill;
		
		public buffCast(Skill skill)
		{
			_skill = skill;
		}
		
		@Override
		public void run()
		{
			if (_skill == null)
			{
				getAI().setIntentionFollow(_owner);
			}
			else
			{
				sitCastAndFollow(_skill, _owner);
			}
		}
	}
	
	/**
	 * Retrieves the {@code Player} who owns this beast.
	 * @return the {@code Player} owner of this beast.
	 */
	public Player getOwner()
	{
		return _owner;
	}
	
	/**
	 * Sets the {@code Player} who owns this tamed beast.<br>
	 * If the provided {@code owner} is {@code null}, this beast will be deleted.<br>
	 * This method updates the title, starts following behavior, and manages buff tasks.
	 * @param owner The {@code Player} that becomes the new owner of this beast.
	 */
	public void setOwner(Player owner)
	{
		if (owner != null)
		{
			_owner = owner;
			setTitle(owner.getName());
			
			// Broadcast the new title.
			setShowSummonAnimation(true);
			broadcastPacket(new NpcInfo(this));
			owner.addTrainedBeast(this);
			
			// Always and automatically follow the owner.
			getAI().startFollow(_owner, 100);
			if (!_isFreyaBeast)
			{
				// Instead of calculating this value each time, let's get this now and pass it on.
				int totalBuffsAvailable = 0;
				for (Skill skill : getTemplate().getSkills().values())
				{
					// If the skill is a buff, check if the owner has it already [ owner.getEffect(Skill skill) ].
					if (skill.isContinuous() && !skill.isDebuff())
					{
						totalBuffsAvailable++;
					}
				}
				
				// Start the buff tasks.
				if (_buffTask != null)
				{
					_buffTask.cancel(true);
				}
				
				_buffTask = ThreadPool.scheduleAtFixedRate(new CheckOwnerBuffs(this, totalBuffsAvailable), BUFF_INTERVAL, BUFF_INTERVAL);
			}
		}
		else
		{
			deleteMe(); // despawn if no owner
		}
	}
	
	/**
	 * Checks if the beast is outside its allowed home radius.<br>
	 * It compares the current position to the home coordinates.<br>
	 * Returns {@code true} if the distance exceeds the maximum limit.<br>
	 * Returns {@code false} if the beast is within the safe zone.
	 * @return {@code true} if too far from home, {@code false} otherwise.
	 */
	public boolean isTooFarFromHome()
	{
		return !isInsideRadius3D(_homeX, _homeY, _homeZ, MAX_DISTANCE_FROM_HOME);
	}
	
	/**
	 * Removes this beast from the game world.<br>
	 * This method cleans up all active tasks and references to the owner.<br>
	 * It resets internal variables like duration and target before deletion.
	 * @return {@code true} if the beast was successfully deleted, otherwise {@code false}.
	 */
	@Override
	public boolean deleteMe()
	{
		if (_buffTask != null)
		{
			_buffTask.cancel(true);
		}
		
		_durationCheckTask.cancel(true);
		stopHpMpRegeneration();
		
		// Clean up variables.
		if (_owner != null)
		{
			_owner.getTrainedBeasts().remove(this);
		}
		
		setTarget(null);
		_buffTask = null;
		_durationCheckTask = null;
		_owner = null;
		_foodSkillId = 0;
		_remainingTime = 0;
		
		// Remove the spawn.
		return super.deleteMe();
	}
	
	// Notification triggered by the owner when the owner is attacked.
	// Tamed mobs will heal/recharge or debuff the enemy according to their skills.
	/**
	 * This method is called when the owner of the beast is attacked by a {@code Creature}.<br>
	 * It handles the beast's reaction based on the owner's current health.<br>
	 * If the owner is far away, the beast will start following them.<br>
	 * The beast may cast debuffs on the attacker or healing skills on the owner depending on their HP ratio.
	 * @param attacker The {@code Creature} that attacked the owner.
	 */
	public void onOwnerGotAttacked(Creature attacker)
	{
		// Check if the owner is no longer around...if so, despawn.
		if ((_owner == null) || !_owner.isOnline())
		{
			deleteMe();
			return;
		}
		
		// If the owner is too far away, stop anything else and immediately run towards the owner.
		if (!_owner.isInsideRadius3D(this, MAX_DISTANCE_FROM_OWNER))
		{
			getAI().startFollow(_owner);
			return;
		}
		
		// If the owner is dead, do nothing...
		if (_owner.isDead() || _isFreyaBeast)
		{
			return;
		}
		
		// If the tamed beast is currently in the middle of casting, let it complete its skill...
		if (isCastingNow(SkillCaster::isAnyNormalType))
		{
			return;
		}
		
		final float HPRatio = ((float) _owner.getCurrentHp()) / _owner.getMaxHp();
		
		// If the owner has a lot of HP, then debuff the enemy with a random debuff among the available skills.
		// Use of more than one debuff at this moment is acceptable.
		if (HPRatio >= 0.8)
		{
			for (Skill skill : getTemplate().getSkills().values())
			{
				// if the skill is a debuff, check if the attacker has it already [ attacker.getEffect(Skill skill) ]
				if (skill.isDebuff() && (Rnd.get(3) < 1) && ((attacker != null) && attacker.isAffectedBySkill(skill.getId())))
				{
					sitCastAndFollow(skill, attacker);
				}
			}
		}
		
		// for HP levels between 80% and 50%, do not react to attack events (so that MP can regenerate a bit)
		// for lower HP ranges, heal or recharge the owner with 1 skill use per attack.
		else if (HPRatio < 0.5)
		{
			int chance = 1;
			if (HPRatio < 0.25)
			{
				chance = 2;
			}
			
			// If the owner has a lot of HP, then debuff the enemy with a random debuff among the available skills.
			for (Skill skill : getTemplate().getSkills().values())
			{
				// If the skill is a buff, check if the owner has it already [ owner.getEffect(Skill skill) ].
				if ((Rnd.get(5) < chance) && skill.hasEffectType(EffectType.CPHEAL, EffectType.HEAL, EffectType.MANAHEAL_BY_LEVEL, EffectType.MANAHEAL_PERCENT))
				{
					sitCastAndFollow(skill, _owner);
				}
			}
		}
	}
	
	/**
	 * Stops the current movement and sets the AI to an idle state.<br>
	 * Sets the target for the specified {@code Skill}.<br>
	 * Executes the skill cast on the {@code Creature}.<br>
	 * Changes the AI intention to follow the owner.
	 * @param skill The {@code Skill} to be cast by the beast.
	 * @param target The {@code Creature} that will be targeted by the skill.
	 */
	protected void sitCastAndFollow(Skill skill, Creature target)
	{
		stopMove(null);
		broadcastPacket(new StopMove(this));
		getAI().setIntentionIdle();
		
		setTarget(target);
		doCast(skill);
		getAI().setIntentionFollow(_owner);
	}
	
	private static class CheckDuration implements Runnable
	{
		private final TamedBeast _tamedBeast;
		
		CheckDuration(TamedBeast tamedBeast)
		{
			_tamedBeast = tamedBeast;
		}
		
		@Override
		public void run()
		{
			final int foodTypeSkillId = _tamedBeast.getFoodType();
			final Player owner = _tamedBeast.getOwner();
			Item item = null;
			if (_tamedBeast._isFreyaBeast)
			{
				item = owner.getInventory().getItemByItemId(foodTypeSkillId);
				if ((item != null) && (item.getCount() >= 1))
				{
					owner.destroyItem(ItemProcessType.DESTROY, item, 1, _tamedBeast, true);
					_tamedBeast.broadcastPacket(new SocialAction(_tamedBeast.getObjectId(), 3));
				}
				else
				{
					_tamedBeast.deleteMe();
				}
			}
			else
			{
				_tamedBeast.setRemainingTime(_tamedBeast.getRemainingTime() - DURATION_CHECK_INTERVAL);
				
				// I tried to avoid this as much as possible...but it seems I can't avoid hardcoding.
				// ids further, except by carrying an additional variable just for these two lines...
				// Find which food item needs to be consumed.
				if (foodTypeSkillId == 2188)
				{
					item = owner.getInventory().getItemByItemId(6643);
				}
				else if (foodTypeSkillId == 2189)
				{
					item = owner.getInventory().getItemByItemId(6644);
				}
				
				// If the owner has enough food, call the item handler (use the food and triffer all necessary actions).
				if ((item != null) && (item.getCount() >= 1))
				{
					final WorldObject oldTarget = owner.getTarget();
					owner.setTarget(_tamedBeast);
					
					// Emulate a call to the owner using food, but bypass all checks for range, etc.
					// This also causes a call to the AI tasks handling feeding, which may call onReceiveFood as required.
					SkillCaster.triggerCast(owner, _tamedBeast, SkillData.getInstance().getSkill(foodTypeSkillId, 1));
					owner.setTarget(oldTarget);
				}
				else
				{
					// if the owner has no food, the beast immediately despawns, except when it was only
					// newly spawned. Newly spawned beasts can last up to 5 minutes
					if (_tamedBeast.getRemainingTime() < (MAX_DURATION - 300000))
					{
						_tamedBeast.setRemainingTime(-1);
					}
				}
				
				// There are too many conflicting reports about whether distance from home should be taken into consideration. Disabled for now.
				// if (_tamedBeast.isTooFarFromHome())
				// _tamedBeast.setRemainingTime(-1);
				if (_tamedBeast.getRemainingTime() <= 0)
				{
					_tamedBeast.deleteMe();
				}
			}
		}
	}
	
	private class CheckOwnerBuffs implements Runnable
	{
		private final TamedBeast _tamedBeast;
		private final int _numBuffs;
		
		CheckOwnerBuffs(TamedBeast tamedBeast, int numBuffs)
		{
			_tamedBeast = tamedBeast;
			_numBuffs = numBuffs;
		}
		
		@Override
		public void run()
		{
			final Player owner = _tamedBeast.getOwner();
			
			// Check if the owner is no longer around...if so, despawn.
			if ((owner == null) || !owner.isOnline())
			{
				deleteMe();
				return;
			}
			
			// If the owner is too far away, stop anything else and immediately run towards the owner.
			if (!isInsideRadius3D(owner, MAX_DISTANCE_FROM_OWNER))
			{
				getAI().startFollow(owner);
				return;
			}
			
			// If the owner is dead, do nothing...
			if (owner.isDead())
			{
				return;
			}
			
			// If the tamed beast is currently casting a spell, do not interfere (do not attempt to cast anything new yet).
			if (isCastingNow(SkillCaster::isAnyNormalType))
			{
				return;
			}
			
			int totalBuffsOnOwner = 0;
			int i = 0;
			final int rand = Rnd.get(_numBuffs);
			Skill buffToGive = null;
			
			// Get this npc's skills: getSkills().
			for (Skill skill : _tamedBeast.getTemplate().getSkills().values())
			{
				// If the skill is a buff, check if the owner has it already [ owner.getEffect(Skill skill) ].
				if (skill.isContinuous() && !skill.isDebuff())
				{
					if (i++ == rand)
					{
						buffToGive = skill;
					}
					
					if (owner.isAffectedBySkill(skill.getId()))
					{
						totalBuffsOnOwner++;
					}
				}
			}
			
			// If the owner has less than 60% of this beast's available buff, cast a random buff.
			if (((_numBuffs * 2) / 3) > totalBuffsOnOwner)
			{
				_tamedBeast.sitCastAndFollow(buffToGive, owner);
			}
			
			getAI().setIntentionFollow(_tamedBeast.getOwner());
		}
	}
	
	/**
	 * Handles actions performed by a {@code Player} on this beast.<br>
	 * It updates the player's target if they are not already targeting it.<br>
	 * If the player interacts, it may trigger an attack intention based on distance and reachability.
	 * @param player The {@code Player} performing the action.
	 * @param interact Whether the player is currently interacting with the beast.
	 */
	@Override
	public void onAction(Player player, boolean interact)
	{
		if ((player == null) || !canTarget(player))
		{
			return;
		}
		
		// Check if the Player already target the Npc.
		if (this != player.getTarget())
		{
			// Set the target of the Player player.
			player.setTarget(this);
		}
		else if (interact)
		{
			if (isAutoAttackable(player) && (Math.abs(player.getZ() - getZ()) < 100))
			{
				player.getAI().setIntentionAttack(this);
			}
			else
			{
				// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet.
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
		}
	}
}
