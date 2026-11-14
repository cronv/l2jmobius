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
package org.l2jmobius.gameserver.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.custom.ChampionMonstersConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.WorldRegion;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.npc.AISkillScope;
import org.l2jmobius.gameserver.entity.actor.enums.npc.AIType;
import org.l2jmobius.gameserver.entity.actor.holders.creature.EffectList;
import org.l2jmobius.gameserver.entity.actor.holders.npc.AggroInfo;
import org.l2jmobius.gameserver.entity.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.entity.actor.instance.Guard;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.actor.instance.RaidBoss;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.interfaces.IPositionable;
import org.l2jmobius.gameserver.managers.ItemsOnGroundManager;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnAttackableFactionCall;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnAttackableHate;
import org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.taskmanagers.AttackableThinkTaskManager;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;
import org.l2jmobius.gameserver.util.LocationUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Manages the AI of {@code Attackable} entities, handling target selection, aggro, movement and combat.
 */
public class AttackableAI extends CreatureAI
{
	private static final int RANDOM_WALK_RATE = 30; // confirmed
	private static final int MAX_ATTACK_TIMEOUT = 1200; // int ticks, i.e. 2min
	private static final int WANDER_ATTEMPTS = 3;
	private static final int SKILL_PROBABILITY_SCALE = 10000;
	private static final int PERCENT_SCALE = 100; // The combat monster classes roll their "Probability" spelling on this scale.
	private static final int DEFAULT_SKILL_PROBABILITY = 3333; // The warrior classes roll one in three when the template says nothing.
	private static final int DEFAULT_SKILL_HIGH_HP = 100;
	private static final int DEFAULT_SKILL_DIST_MAX = 2000;
	private static final int MAX_PARAMETERIZED_SKILL_SLOTS = 6;
	
	/** The delay after which the attacked is stopped. */
	private int _attackTimeout;
	/** The Attackable aggro counter. */
	private int _globalAggro;
	/** The flag used to indicate that a thinking action is in progress, to prevent recursive thinking. */
	private boolean _thinking;
	
	private int _chaosTime = 0;
	private long _nextChaosTime = 0;
	
	/**
	 * Creates an AI for the given {@code Attackable}, setting the attack timeout and a post-spawn aggro delay.
	 * @param attackable The {@code Attackable} this AI controls.
	 */
	public AttackableAI(Attackable attackable)
	{
		super(attackable);
		_attackTimeout = Integer.MAX_VALUE;
		rollGlobalAggro();
	}
	
	/**
	 * Checks whether this NPC is aggressive toward the target, considering invulnerability, peace zones, fake-death, auto-attackability and line of sight.
	 * @param target The {@code Creature} to evaluate.
	 * @return {@code true} if aggressive toward the target, {@code false} otherwise.
	 */
	private boolean isAggressiveTowards(Creature target)
	{
		if ((target == null) || (getActiveChar() == null))
		{
			return false;
		}
		
		// Check if the target isn't invulnerable.
		if (target.isInvul())
		{
			return false;
		}
		
		// Check if the target isn't a Folk or a Door.
		if (target.isDoor())
		{
			return false;
		}
		
		// Check if the target isn't dead, is in the Aggro range and is at the same height.
		if (target.isAlikeDead())
		{
			return false;
		}
		
		// Check if the target is a Playable and if the AI isn't a Raid Boss, can See Silent Moving players and the target isn't in silent move mode.
		final Attackable me = getActiveChar();
		if (target.isPlayable() && !(me.isRaid()) && !(me.canSeeThroughSilentMove()) && target.asPlayable().isSilentMovingAffected())
		{
			return false;
		}
		
		// Retail gates acquisition on where the monster stands, so one dragged away from its territory stops picking up new targets.
		if (NpcConfig.AGGRO_TERRITORY_CHECK && me.isMonster() && !me.isRaid() && !me.isWalker() && (me.getInstanceId() == 0))
		{
			final Spawn spawn = me.getSpawn();
			if ((spawn != null) && spawn.hasSpawnTerritory() && !spawn.isInsideSpawnTerritory(me.getX(), me.getY()))
			{
				return false;
			}
		}
		
		// Gets the player if there is any.
		final Player player = target.asPlayer();
		if (player != null)
		{
			// Don't take the aggro if the GM has the access level below or equal to GM_DONT_TAKE_AGGRO.
			if (!player.getAccessLevel().canTakeAggro())
			{
				return false;
			}
			
			// Check if the target is within the grace period for JUST getting up from fake death.
			if (player.isRecentFakeDeath())
			{
				return false;
			}
			
			if (me instanceof Guard)
			{
				World.forFirstVisibleObjectInRange(me, Guard.class, 500, guard -> guard.isAttackingNow() && (guard.getTarget() == player), guard ->
				{
					me.getAI().startFollow(player);
					me.addDamageHate(player, 0, 10);
				});
				
				if (player.getReputation() < 0)
				{
					return true;
				}
			}
		}
		else if (me.isMonster())
		{
			// depending on config, do not allow mobs to attack _new_ players in peacezones,
			// unless they are already following those players from outside the peacezone.
			if (!NpcConfig.ALT_MOB_AGRO_IN_PEACEZONE && target.isInsideZone(ZoneId.PEACE) && target.isInsideZone(ZoneId.NO_PVP))
			{
				return false;
			}
			
			if (!me.isAggressive())
			{
				return false;
			}
		}
		
		if (me.isChampion() && ChampionMonstersConfig.CHAMPION_PASSIVE)
		{
			return false;
		}
		
		return target.isAutoAttackable(me) && GeoEngine.getInstance().canSeeTarget(me, target);
	}
	
	/**
	 * Registers this character with the {@code AttackableThinkTaskManager} to begin AI processing.
	 */
	public void startAITask()
	{
		AttackableThinkTaskManager.getInstance().add(getActiveChar());
	}
	
	/**
	 * Removes this character from the {@code AttackableThinkTaskManager} and delegates to the superclass.
	 */
	@Override
	public void stopAITask()
	{
		AttackableThinkTaskManager.getInstance().remove(getActiveChar());
		super.stopAITask();
	}
	
	/**
	 * Sets the intention to idle, reverting to active when players are nearby or the NPC drifted from spawn, otherwise stopping the AI task.
	 */
	@Override
	public synchronized void setIntentionIdle()
	{
		final Attackable npc = getActiveChar();
		if (!npc.isAlikeDead())
		{
			// If its _knownPlayer isn't empty set the Intention to ACTIVE.
			if ((World.getFirstVisibleObject(npc, Player.class) != null))
			{
				setIntentionActive();
				return;
			}
			
			final Spawn npcSpawn = npc.getSpawn();
			if ((npcSpawn != null) && (npcSpawn.hasSpawnTerritory() ? !npcSpawn.isInsideSpawnTerritory(npc.getX(), npc.getY()) : !npc.isInsideRadius3D(npcSpawn, NpcConfig.MAX_DRIFT_RANGE + NpcConfig.MAX_DRIFT_RANGE)))
			{
				setIntentionActive();
				return;
			}
		}
		
		// Set the Intention of this AttackableAI to IDLE.
		super.setIntentionIdle();
		
		stopAITask();
		
		// Cancel the AI
		_actor.detachAI();
	}
	
	/**
	 * Sets the intention to attack, refreshing the attack timeout and starting the AI task.
	 * @param target The {@code WorldObject} to attack.
	 */
	@Override
	public synchronized void setIntentionAttack(WorldObject target)
	{
		// Calculate the attack timeout.
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeTaskManager.getInstance().getGameTicks();
		
		// Manage the Attack Intention : Stop current Attack (if necessary), Start a new Attack and Launch Think Action.
		super.setIntentionAttack(target);
		
		// Schedule notifyActionThink repeatedly.
		startAITask();
	}
	
	@Override
	public void notifyActionReadyToAct()
	{
		// A landed attack keeps the fight alive, the timeout is only for targets that cannot be reached.
		if (getIntention() == Intention.ATTACK)
		{
			_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeTaskManager.getInstance().getGameTicks();
		}
		
		super.notifyActionReadyToAct();
	}
	
	@Override
	public void notifyActionFinishCasting()
	{
		// A finished cast keeps the fight alive, the timeout is only for targets that cannot be reached.
		if (getIntention() == Intention.ATTACK)
		{
			_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeTaskManager.getInstance().getGameTicks();
		}
		
		super.notifyActionFinishCasting();
	}
	
	/**
	 * Sets the intention to cast a skill and starts the AI task.
	 * @param skill The {@code Skill} to cast.
	 * @param target The {@code WorldObject} targeted.
	 * @param item The {@code Item} required, if any.
	 * @param forceUse {@code true} to force skill use.
	 * @param dontMove {@code true} to prevent movement during the cast.
	 */
	@Override
	public synchronized void setIntentionCast(Skill skill, WorldObject target, Item item, boolean forceUse, boolean dontMove)
	{
		// Set the AI cast target.
		setTarget(target);
		
		super.setIntentionCast(skill, target, item, forceUse, dontMove);
		
		// Schedule notifyActionThink repeatedly.
		startAITask();
	}
	
	/**
	 * Sets the intention to move to the given location and starts the AI task.
	 * @param destination The {@code ILocational} movement target.
	 */
	@Override
	public synchronized void setIntentionMoveTo(ILocational destination)
	{
		super.setIntentionMoveTo(destination);
		
		// Schedule notifyActionThink repeatedly.
		startAITask();
	}
	
	/**
	 * Sets the intention to follow the given target and starts the AI task.
	 * @param target The {@code WorldObject} to follow.
	 */
	@Override
	public synchronized void setIntentionFollow(WorldObject target)
	{
		super.setIntentionFollow(target);
		
		// Schedule notifyActionThink repeatedly.
		startAITask();
	}
	
	/**
	 * Sets the intention to pick up the given object and starts the AI task.
	 * @param item The {@code WorldObject} to pick up.
	 */
	@Override
	public synchronized void setIntentionPickUp(WorldObject item)
	{
		super.setIntentionPickUp(item);
		
		// Schedule notifyActionThink repeatedly.
		startAITask();
	}
	
	/**
	 * Sets the intention to interact with the given object and starts the AI task.
	 * @param object The {@code WorldObject} to interact with.
	 */
	@Override
	public synchronized void setIntentionInteract(WorldObject object)
	{
		super.setIntentionInteract(object);
		
		// Schedule notifyActionThink repeatedly.
		startAITask();
	}
	
	/**
	 * Resolves the skill target, moves into range if needed, then sets active and casts the skill.
	 */
	protected void thinkCast()
	{
		final WorldObject target = _skill.getTarget(_actor, getTarget(), _forceUse, _dontMove, false);
		if (checkTargetLost(target))
		{
			setCastTarget(null);
			return;
		}
		
		if (maybeMoveToPawn(target, _actor.getMagicalAttackRange(_skill)))
		{
			return;
		}
		
		setIntentionActive();
		_actor.doCast(_skill, _item, _forceUse, _dontMove);
	}
	
	/**
	 * Runs active-state logic: updates aggro, scans for targets, attacks the most hated, returns home, follows a leader or random walks.
	 */
	protected void thinkActive()
	{
		// Check if region and its neighbors are active.
		final WorldRegion region = _actor.getWorldRegion();
		if ((region == null) || !region.areNeighborsActive())
		{
			return;
		}
		
		final Attackable npc = getActiveChar();
		WorldObject target = getTarget();
		
		// Update every 1s the _globalAggro counter to come close to 0.
		if (_globalAggro != 0)
		{
			if (_globalAggro < 0)
			{
				_globalAggro++;
			}
			else
			{
				_globalAggro--;
			}
		}
		
		// Add all autoAttackable Creature in Attackable Aggro Range to its _aggroList with 0 damage and 1 hate.
		// An Attackable isn't aggressive during 10s after its spawn because _globalAggro is set to -10.
		if (_globalAggro >= 0)
		{
			if (npc.isFakePlayer() && npc.isAggressive())
			{
				final List<Item> droppedItems = npc.getFakePlayerDrops();
				if (droppedItems.isEmpty())
				{
					final Creature nearestTarget = World.getNearestVisibleObjectInRange(npc, Creature.class, npc.getAggroRange(), t -> (t != _actor) && !t.isDead() && ((FakePlayersConfig.FAKE_PLAYER_AGGRO_FPC && t.isFakePlayer()) || (FakePlayersConfig.FAKE_PLAYER_AGGRO_MONSTERS && t.isMonster() && !t.isFakePlayer()) || (FakePlayersConfig.FAKE_PLAYER_AGGRO_PLAYERS && t.isPlayer())) && (npc.getHating(t) == 0));
					if (nearestTarget != null)
					{
						npc.addDamageHate(nearestTarget, 0, 1);
					}
				}
				else if (!npc.isInCombat()) // Must pickup items.
				{
					final int itemIndex = droppedItems.size() - 1; // Last item dropped - can also use 0 for first item dropped.
					final Item droppedItem = droppedItems.get(itemIndex);
					if ((droppedItem != null) && droppedItem.isSpawned())
					{
						if (npc.calculateDistance2D(droppedItem) > 50)
						{
							moveTo(droppedItem);
						}
						else
						{
							droppedItems.remove(itemIndex);
							droppedItem.pickupMe(npc);
							if (GeneralConfig.SAVE_DROPPED_ITEM)
							{
								ItemsOnGroundManager.getInstance().removeObject(droppedItem);
							}
							
							final ItemTemplate droppedItemTemplate = droppedItem.getTemplate();
							if (droppedItemTemplate.hasExImmediateEffect())
							{
								for (SkillHolder skillHolder : droppedItemTemplate.getAllSkills())
								{
									SkillCaster.triggerCast(npc, null, skillHolder.getSkill(), null, false);
								}
								
								npc.broadcastInfo(); // TODO: Check if this is necessary.
							}
						}
					}
					else
					{
						npc.getFakePlayerDrops().remove(itemIndex);
					}
					
					npc.setRunning();
				}
			}
			else if (npc.isAggressive() || (npc instanceof Guard))
			{
				// Guards without a template aggro range keep the legacy default.
				final int aggroRange = npc.getAggroRange();
				final int range = (aggroRange <= 0) && (npc instanceof Guard) ? 500 : aggroRange;
				World.forEachVisibleObjectInRange(npc, Creature.class, range, t ->
				{
					// For each Creature check if the target is autoattackable.
					if (isAggressiveTowards(t)) // Check aggression.
					{
						if (t.isFakePlayer())
						{
							if (!npc.isFakePlayer() || (npc.isFakePlayer() && FakePlayersConfig.FAKE_PLAYER_AGGRO_FPC))
							{
								final long hating = npc.getHating(t);
								if (hating == 0)
								{
									npc.addDamageHate(t, 0, 0);
								}
							}
						}
						else if (t.isPlayable() || (t.isMonster() && NpcConfig.GUARD_ATTACK_AGGRO_MOB && t.isAutoAttackable(npc)))
						{
							final EventDispatcher eventDispatcher = EventDispatcher.getInstance();
							if (eventDispatcher.hasListener(EventType.ON_NPC_HATE, getActiveChar()))
							{
								final TerminateReturn term = eventDispatcher.notifyEvent(new OnAttackableHate(getActiveChar(), t.asPlayer(), t.isSummon()), getActiveChar(), TerminateReturn.class);
								if ((term != null) && term.terminate())
								{
									return;
								}
							}
							
							// Get the hate level of the Attackable against this Creature target contained in _aggroList.
							final long hating = npc.getHating(t);
							
							// Add the attacker to the Attackable _aggroList with 0 damage and 1 hate.
							if (hating == 0)
							{
								npc.addDamageHate(t, 0, 0);
							}
							
							if (npc instanceof Guard)
							{
								World.forEachVisibleObjectInRange(npc, Guard.class, 500, guard -> guard.addDamageHate(t, 0, 10));
							}
						}
					}
				});
			}
			
			// Chose a target from its aggroList.
			Creature hated;
			if (npc.isConfused() && (target != null) && target.isCreature())
			{
				hated = target.asCreature(); // Effect handles selection.
			}
			else
			{
				hated = npc.getMostHated();
			}
			
			// Order to the Attackable to attack the target.
			if ((hated != null) && !npc.isCoreAIDisabled())
			{
				// Get the hate level of the Attackable against this Creature target contained in _aggroList.
				final long aggro = npc.getHating(hated);
				if ((aggro + _globalAggro) > 0)
				{
					// Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others Player.
					if (!npc.isRunning())
					{
						npc.setRunning();
					}
					
					// Set the AI Intention to ATTACK.
					setIntentionAttack(hated);
				}
				
				return;
			}
		}
		
		// Chance to forget attackers after some time.
		if ((npc.getCurrentHp() == npc.getMaxHp()) && (npc.getCurrentMp() == npc.getMaxMp()))
		{
			final Set<Creature> attackByList = npc.getAttackByList();
			if (!attackByList.isEmpty() && (Rnd.get(500) == 0))
			{
				npc.clearAggroList();
				attackByList.clear();
			}
		}
		
		// Check if the mob should not return to spawn point.
		if (!npc.canReturnToSpawnPoint()
		/* || npc.isReturningToSpawnPoint() */ ) // Commented because sometimes it stops movement.
		{
			return;
		}
		
		// Order this attackable to return to its spawn because there's no target to attack.
		final Spawn npcSpawn = npc.getSpawn();
		if (!npc.isWalker() && (npcSpawn != null) && (npcSpawn.hasSpawnTerritory() ? !npcSpawn.isInsideSpawnTerritory(npc.getX(), npc.getY()) : (npc.calculateDistance2D(npcSpawn) > NpcConfig.MAX_DRIFT_RANGE)) && ((target == null) || target.isInvisible() || (target.isPlayer() && !NpcConfig.ATTACKABLES_CAMP_PLAYER_CORPSES && target.asPlayer().isAlikeDead())))
		{
			npc.setWalking();
			npc.returnHome();
			return;
		}
		
		// Do not leave dead player.
		if ((target != null) && target.isPlayer() && target.asPlayer().isAlikeDead())
		{
			return;
		}
		
		// Minions following leader.
		final Creature leader = npc.getLeader();
		if ((leader != null) && !leader.isAlikeDead())
		{
			final int offset;
			final int minRadius = 30;
			if (npc.isRaidMinion())
			{
				offset = 500; // For Raids.
			}
			else
			{
				offset = 200; // For normal minions.
			}
			
			if (leader.isRunning())
			{
				npc.setRunning();
			}
			else
			{
				npc.setWalking();
			}
			
			if (npc.calculateDistance2D(leader) > offset)
			{
				int x1 = Rnd.get(minRadius * 2, offset * 2); // x
				int y1 = Rnd.get(x1, offset * 2); // distance
				y1 = (int) Math.sqrt((y1 * y1) - (x1 * x1)); // y
				final int leaderX = leader.getX();
				final int leaderY = leader.getY();
				if (x1 > (offset + minRadius))
				{
					x1 = (leaderX + x1) - offset;
				}
				else
				{
					x1 = (leaderX - x1) + minRadius;
				}
				
				if (y1 > (offset + minRadius))
				{
					y1 = (leaderY + y1) - offset;
				}
				else
				{
					y1 = (leaderY - y1) + minRadius;
				}
				
				// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation (broadcast).
				moveTo(x1, y1, leader.getZ());
			}
			else if (Rnd.get(RANDOM_WALK_RATE) == 0)
			{
				for (Skill sk : npc.getTemplate().getAISkills(AISkillScope.BUFF))
				{
					target = skillTargetReconsider(sk, true);
					if (target != null)
					{
						setTarget(target);
						npc.doCast(sk);
					}
				}
			}
		}
		// Order to the Monster to random walk (1/100).
		else if ((npcSpawn != null) && (Rnd.get(RANDOM_WALK_RATE) == 0) && npc.isRandomWalkingEnabled())
		{
			for (Skill sk : npc.getTemplate().getAISkills(AISkillScope.BUFF))
			{
				target = skillTargetReconsider(sk, true);
				if (target != null)
				{
					setTarget(target);
					npc.doCast(sk);
					return;
				}
			}
			
			// A territory spawn has no fixed home, the Spawn location holds the point rolled for the last NPC of the group.
			if (npcSpawn.hasSpawnTerritory())
			{
				// Near a territory border most offsets fall outside, so give the roll a few attempts before giving up.
				for (int i = 0; i < WANDER_ATTEMPTS; i++)
				{
					final int deltaX = Rnd.get(NpcConfig.MAX_DRIFT_RANGE * 2); // x
					int deltaY = Rnd.get(deltaX, NpcConfig.MAX_DRIFT_RANGE * 2); // distance
					deltaY = (int) Math.sqrt((deltaY * deltaY) - (deltaX * deltaX)); // y
					final int x2 = (deltaX + npc.getX()) - NpcConfig.MAX_DRIFT_RANGE;
					final int y2 = (deltaY + npc.getY()) - NpcConfig.MAX_DRIFT_RANGE;
					if (!npcSpawn.isInsideSpawnTerritory(x2, y2))
					{
						continue;
					}
					
					// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation (broadcast).
					final Location moveLoc = _actor.isFlying() ? new Location(x2, y2, npc.getZ()) : GeoEngine.getInstance().getValidLocation(npc.getX(), npc.getY(), npc.getZ(), x2, y2, npc.getZ(), npc.getInstanceWorld());
					if (npcSpawn.isInsideSpawnTerritory(moveLoc.getX(), moveLoc.getY()))
					{
						moveTo(moveLoc.getX(), moveLoc.getY(), moveLoc.getZ());
						break;
					}
				}
				
				return;
			}
			
			int x1 = npcSpawn.getX();
			int y1 = npcSpawn.getY();
			int z1 = npcSpawn.getZ();
			if (npc.isInsideRadius2D(x1, y1, 0, NpcConfig.MAX_DRIFT_RANGE))
			{
				final int deltaX = Rnd.get(NpcConfig.MAX_DRIFT_RANGE * 2); // x
				int deltaY = Rnd.get(deltaX, NpcConfig.MAX_DRIFT_RANGE * 2); // distance
				deltaY = (int) Math.sqrt((deltaY * deltaY) - (deltaX * deltaX)); // y
				x1 = (deltaX + x1) - NpcConfig.MAX_DRIFT_RANGE;
				y1 = (deltaY + y1) - NpcConfig.MAX_DRIFT_RANGE;
				z1 = npc.getZ();
			}
			
			// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation (broadcast).
			final Location moveLoc = _actor.isFlying() ? new Location(x1, y1, z1) : GeoEngine.getInstance().getValidLocation(npc.getX(), npc.getY(), npc.getZ(), x1, y1, z1, npc.getInstanceWorld());
			if (LocationUtil.calculateDistance(npcSpawn, moveLoc, false, false) <= NpcConfig.MAX_DRIFT_RANGE)
			{
				moveTo(moveLoc.getX(), moveLoc.getY(), moveLoc.getZ());
			}
		}
	}
	
	/**
	 * Runs attack-state logic: enforces return-to-spawn, selects the most hated target, casts AI skills and performs the auto attack.
	 */
	protected void thinkAttack()
	{
		final Attackable npc = getActiveChar();
		if ((npc == null) || npc.isCastingNow())
		{
			return;
		}
		
		final boolean isRaid = npc.isRaid();
		if ((npc.isMonster() && !npc.isWalker() && !(npc instanceof GrandBoss)) && (isRaid ? NpcConfig.AGGRO_DISTANCE_CHECK_RAIDS : NpcConfig.AGGRO_DISTANCE_CHECK_ENABLED))
		{
			final Spawn spawn = npc.getSpawn();
			if (spawn != null)
			{
				final IPositionable spawnLocation = spawn.getLocation();
				if (npc.calculateDistance2D(spawnLocation) > (spawn.getChaseRange() > 0 ? Math.max(NpcConfig.MAX_DRIFT_RANGE, spawn.getChaseRange()) : isRaid ? NpcConfig.AGGRO_DISTANCE_CHECK_RAID_RANGE : NpcConfig.AGGRO_DISTANCE_CHECK_RANGE))
				{
					if (NpcConfig.AGGRO_DISTANCE_CHECK_INSTANCES || !npc.isInInstance())
					{
						if (NpcConfig.AGGRO_DISTANCE_CHECK_RESTORE_LIFE)
						{
							npc.setCurrentHp(npc.getMaxHp());
							npc.setCurrentMp(npc.getMaxMp());
						}
						
						npc.abortAttack();
						npc.clearAggroList();
						npc.getAttackByList().clear();
						if (npc.hasAI())
						{
							npc.getAI().setIntentionMoveTo(spawnLocation);
						}
						else
						{
							npc.teleToLocation(spawnLocation, true);
						}
						
						// Minions should return as well.
						if (_actor.asMonster().hasMinions())
						{
							for (Monster minion : _actor.asMonster().getMinionList().getSpawnedMinions())
							{
								if (NpcConfig.AGGRO_DISTANCE_CHECK_RESTORE_LIFE)
								{
									minion.setCurrentHp(minion.getMaxHp());
									minion.setCurrentMp(minion.getMaxMp());
								}
								
								minion.abortAttack();
								minion.clearAggroList();
								minion.getAttackByList().clear();
								if (minion.hasAI())
								{
									minion.getAI().setIntentionMoveTo(spawnLocation);
								}
								else
								{
									minion.teleToLocation(spawnLocation, true);
								}
							}
						}
						return;
					}
				}
			}
		}
		
		if (npc.isCoreAIDisabled())
		{
			return;
		}
		
		Creature target = npc.getMostHated();
		if (target == null)
		{
			setIntentionActive();
			return;
		}
		
		if (getTarget() != target)
		{
			setTarget(target);
		}
		
		// Check if target is dead or if timeout is expired to stop this attack.
		if (target.isAlikeDead() && !(target.isPlayer() && target.asPlayer().isFakeDeathSeenBy(npc)))
		{
			// Stop hating this target after the attack timeout or if target is dead.
			npc.stopHating(target);
			return;
		}
		
		if (_attackTimeout < GameTimeTaskManager.getInstance().getGameTicks())
		{
			// Remember the combat state, the intention change below clears it.
			final boolean wasInCombat = npc.isInCombat();
			
			// Set the AI Intention to ACTIVE.
			setIntentionActive();
			
			// Lose hate, otherwise the next think would attack the same target again.
			npc.clearAggroList();
			npc.getAttackByList().clear();
			
			if (!_actor.isFakePlayer())
			{
				npc.setWalking();
			}
			else
			{
				npc.remount(); // Get back on the mount after combat.
			}
			
			// Monster teleport to spawn.
			final Spawn npcSpawn = npc.getSpawn();
			if (npc.isMonster() && (npcSpawn != null) && !npc.isInInstance() && (wasInCombat || (World.getFirstVisibleObject(npc, Player.class) == null)))
			{
				npc.teleToLocation(npcSpawn, false);
			}
			return;
		}
		
		// Actor should be able to see target.
		if (!GeoEngine.getInstance().canSeeTarget(_actor, target))
		{
			if (_actor.calculateDistance3D(target) < 6000)
			{
				moveTo(target);
			}
			return;
		}
		
		// Initialize data.
		final NpcTemplate template = npc.getTemplate();
		final int collision = template.getCollisionRadius();
		final List<Skill> aiSuicideSkills = template.getAISkills(AISkillScope.SUICIDE);
		if (!aiSuicideSkills.isEmpty() && ((int) ((npc.getCurrentHp() / npc.getMaxHp()) * 100) < 30) && npc.hasSkillChance())
		{
			final Skill skill = aiSuicideSkills.get(Rnd.get(aiSuicideSkills.size()));
			if (SkillCaster.checkUseConditions(npc, skill) && checkSkillTarget(skill, target) && allowsParameterizedCast(npc, skill, target))
			{
				npc.doCast(skill);
				return;
			}
		}
		
		// ------------------------------------------------------
		// In case many mobs are trying to hit from same place, move a bit, circling around the target.
		// Note from Gnacik:
		// On l2js because of that sometimes mobs don't attack player only running around player without any sense, so decrease chance for now.
		final int combinedCollision = collision + target.getTemplate().getCollisionRadius();
		if (!npc.isMovementDisabled() && (Rnd.get(100) <= 3))
		{
			final Creature currentTarget = target;
			if (World.getFirstVisibleObject(npc, Attackable.class, nearby -> npc.isInsideRadius2D(nearby, collision) && (nearby != currentTarget)) != null)
			{
				int newX = combinedCollision + Rnd.get(40);
				if (Rnd.nextBoolean())
				{
					newX += target.getX();
				}
				else
				{
					newX = target.getX() - newX;
				}
				
				int newY = combinedCollision + Rnd.get(40);
				if (Rnd.nextBoolean())
				{
					newY += target.getY();
				}
				else
				{
					newY = target.getY() - newY;
				}
				
				if (!npc.isInsideRadius2D(newX, newY, 0, collision))
				{
					final int npcZ = npc.getZ();
					final int newZ = npcZ + 30;
					
					// Mobius: Verify destination. Prevents wall collision issues and fixes monsters not avoiding obstacles.
					moveTo(GeoEngine.getInstance().getValidLocation(npc.getX(), npc.getY(), npcZ, newX, newY, newZ, npc.getInstanceWorld()));
				}
				return;
			}
		}
		
		// Calculate Archer movement.
		if ((!npc.isMovementDisabled()) && (npc.getAiType() == AIType.ARCHER) && (Rnd.get(100) < 15))
		{
			final double distance = npc.calculateDistance2D(target);
			if (distance <= (60 + combinedCollision))
			{
				int posX = npc.getX();
				int posY = npc.getY();
				final int posZ = npc.getZ() + 30;
				if (target.getX() < posX)
				{
					posX += 300;
				}
				else
				{
					posX -= 300;
				}
				
				if (target.getY() < posY)
				{
					posY += 300;
				}
				else
				{
					posY -= 300;
				}
				
				if (GeoEngine.getInstance().canMoveToTarget(npc.getX(), npc.getY(), npc.getZ(), posX, posY, posZ, npc.getInstanceWorld()))
				{
					setIntentionMoveTo(new Location(posX, posY, posZ, 0));
				}
				return;
			}
		}
		
		// ------------------------------------------------------------------------------
		// BOSS/Raid Minion Target Reconsider.
		// Retail raid bosses pick a random target once in five checks, raid minions once in three.
		if (npc.isRaid() || npc.isRaidMinion())
		{
			_chaosTime++;
			boolean changeTarget = false;
			if ((npc instanceof GrandBoss) && (_chaosTime > NpcConfig.GRAND_CHAOS_TIME))
			{
				final double chaosRate = 100 - ((npc.getCurrentHp() * 300) / npc.getMaxHp());
				changeTarget = ((chaosRate <= 10) && (Rnd.get(100) <= 10)) || ((chaosRate > 10) && (Rnd.get(100) <= chaosRate));
			}
			else if ((npc instanceof RaidBoss) || npc.isRaidMinion())
			{
				final long currentTime = System.currentTimeMillis();
				if (currentTime >= _nextChaosTime)
				{
					final boolean isMinion = npc.isRaidMinion();
					changeTarget = (_nextChaosTime > 0) && (Rnd.get(isMinion ? 3 : 5) == 0);
					_nextChaosTime = currentTime + ((isMinion ? NpcConfig.MINION_CHAOS_TIME : NpcConfig.RAID_CHAOS_TIME) * 1000L);
				}
			}
			
			if (changeTarget)
			{
				target = targetReconsider(true);
				if (target != null)
				{
					setTarget(target);
					_chaosTime = 0;
					return;
				}
			}
		}
		
		if (target == null)
		{
			target = targetReconsider(false);
			if (target == null)
			{
				return;
			}
			
			setTarget(target);
		}
		
		// Cast skills.
		if ((!npc.isMoving() && npc.hasSkillChance()) || (npc.getAiType() == AIType.MAGE))
		{
			// First use the most important skill - heal. Even reconsider target.
			if (!template.getAISkills(AISkillScope.HEAL).isEmpty())
			{
				final Skill healSkill = template.getAISkills(AISkillScope.HEAL).get(Rnd.get(template.getAISkills(AISkillScope.HEAL).size()));
				if (SkillCaster.checkUseConditions(npc, healSkill))
				{
					final Creature healTarget = skillTargetReconsider(healSkill, false);
					if (healTarget != null)
					{
						final double healChance = (100 - healTarget.getCurrentHpPercent()) * 1.5; // Ensure heal chance is always 100% if HP is below 33%.
						if ((Rnd.get(100) < healChance) && checkSkillTarget(healSkill, healTarget))
						{
							setTarget(healTarget);
							npc.doCast(healSkill);
							// LOGGER.finer(this + " used heal skill " + healSkill + " with target " + getTarget());
							return;
						}
					}
				}
			}
			
			// Then use the second most important skill - buff. Even reconsider target.
			if (!template.getAISkills(AISkillScope.BUFF).isEmpty())
			{
				final Skill buffSkill = template.getAISkills(AISkillScope.BUFF).get(Rnd.get(template.getAISkills(AISkillScope.BUFF).size()));
				if (SkillCaster.checkUseConditions(npc, buffSkill))
				{
					final Creature buffTarget = skillTargetReconsider(buffSkill, true);
					if (checkSkillTarget(buffSkill, buffTarget))
					{
						setTarget(buffTarget);
						npc.doCast(buffSkill);
						// LOGGER.finer(this + " used buff skill " + buffSkill + " with target " + getTarget());
						return;
					}
				}
			}
			
			// Then try to immobolize target if moving.
			if (target.isMoving() && !template.getAISkills(AISkillScope.IMMOBILIZE).isEmpty())
			{
				final Skill immobolizeSkill = template.getAISkills(AISkillScope.IMMOBILIZE).get(Rnd.get(template.getAISkills(AISkillScope.IMMOBILIZE).size()));
				if (SkillCaster.checkUseConditions(npc, immobolizeSkill) && checkSkillTarget(immobolizeSkill, target) && allowsParameterizedCast(npc, immobolizeSkill, target))
				{
					npc.doCast(immobolizeSkill);
					// LOGGER.finer(this + " used immobolize skill " + immobolizeSkill + " with target " + getTarget());
					return;
				}
			}
			
			// Then try to mute target if he is casting.
			if (target.isCastingNow() && !template.getAISkills(AISkillScope.COT).isEmpty())
			{
				final Skill muteSkill = template.getAISkills(AISkillScope.COT).get(Rnd.get(template.getAISkills(AISkillScope.COT).size()));
				if (SkillCaster.checkUseConditions(npc, muteSkill) && checkSkillTarget(muteSkill, target) && allowsParameterizedCast(npc, muteSkill, target))
				{
					npc.doCast(muteSkill);
					// LOGGER.finer(this + " used mute skill " + muteSkill + " with target " + getTarget());
					return;
				}
			}
			
			// Try cast short range skill.
			final List<Skill> shortRangeSkills = npc.getShortRangeSkills();
			if (!shortRangeSkills.isEmpty() && (npc.calculateDistance2D(target) <= 150))
			{
				final Skill shortRangeSkill = shortRangeSkills.get(Rnd.get(shortRangeSkills.size()));
				if (SkillCaster.checkUseConditions(npc, shortRangeSkill) && checkSkillTarget(shortRangeSkill, target) && allowsParameterizedCast(npc, shortRangeSkill, target))
				{
					npc.doCast(shortRangeSkill);
					// LOGGER.finer(this + " used short range skill " + shortRangeSkill + " with target " + getTarget());
					return;
				}
			}
			
			// Try cast long range skill.
			final List<Skill> longRangeSkills = npc.getLongRangeSkills();
			if (!longRangeSkills.isEmpty())
			{
				final Skill longRangeSkill = longRangeSkills.get(Rnd.get(longRangeSkills.size()));
				if (SkillCaster.checkUseConditions(npc, longRangeSkill) && checkSkillTarget(longRangeSkill, target) && allowsParameterizedCast(npc, longRangeSkill, target))
				{
					npc.doCast(longRangeSkill);
					// LOGGER.finer(this + " used long range skill " + longRangeSkill + " with target " + getTarget());
					return;
				}
			}
			
			// Finally, if none succeed, try to cast any skill.
			if (!template.getAISkills(AISkillScope.GENERAL).isEmpty())
			{
				final Skill generalSkill = template.getAISkills(AISkillScope.GENERAL).get(Rnd.get(template.getAISkills(AISkillScope.GENERAL).size()));
				if (SkillCaster.checkUseConditions(npc, generalSkill) && checkSkillTarget(generalSkill, target) && allowsParameterizedCast(npc, generalSkill, target))
				{
					npc.doCast(generalSkill);
					// LOGGER.finer(this + " used general skill " + generalSkill + " with target " + getTarget());
					return;
				}
			}
		}
		
		// Check if target is within range or move.
		int range = npc.getPhysicalAttackRange() + combinedCollision;
		if (npc.isMoving())
		{
			range *= 2;
		}
		
		if (npc.getAiType() == AIType.ARCHER)
		{
			range = 850 + combinedCollision; // Base bow range for NPCs.
		}
		
		if (npc.calculateDistance2D(target) > range)
		{
			if (checkTarget(target))
			{
				moveToPawn(target, range);
				return;
			}
			
			target = targetReconsider(false);
			if (target == null)
			{
				return;
			}
			
			setTarget(target);
		}
		
		// Attacks target.
		_actor.doAutoAttack(target);
	}
	
	/**
	 * Checks whether the target is valid for the skill, considering cast range, effect reapplication, dispel and heal applicability.
	 * @param skill The {@code Skill} being evaluated.
	 * @param target The intended {@code WorldObject} target.
	 * @return {@code true} if the target is valid, {@code false} otherwise.
	 */
	private boolean checkSkillTarget(Skill skill, WorldObject target)
	{
		if (target == null)
		{
			return false;
		}
		
		// Check if target is valid and within cast range.
		final Attackable attackable = getActiveChar();
		if (skill.getTarget(attackable, target, false, attackable.isMovementDisabled(), false) == null)
		{
			return false;
		}
		
		if (!LocationUtil.checkIfInRange(skill.getCastRange(), attackable, target, true))
		{
			return false;
		}
		
		if (target.isCreature())
		{
			final Creature creature = target.asCreature();
			
			// Skip if target is already affected by such skill.
			if (skill.isContinuous())
			{
				if (creature.getEffectList().hasAbnormalType(skill.getAbnormalType(), i -> (i.getSkill().getAbnormalLevel() >= skill.getAbnormalLevel())))
				{
					return false;
				}
				
				// There are cases where negative effect skills (negative effect points) are actually buffs and NPCs cast them on players, but they shouldn't.
				if ((!skill.isDebuff() || !skill.hasNegativeEffect()) && target.isAutoAttackable(attackable))
				{
					return false;
				}
			}
			
			// Check if target had buffs if skill is bad cancel, or debuffs if skill is good cancel.
			if (skill.hasEffectType(EffectType.DISPEL, EffectType.DISPEL_BY_SLOT))
			{
				final EffectList effectList = creature.getEffectList();
				if (skill.hasNegativeEffect())
				{
					if (effectList.getBuffCount() == 0)
					{
						return false;
					}
				}
				else if (effectList.getDebuffCount() == 0)
				{
					return false;
				}
			}
			
			// Check for damaged targets if using healing skill.
			if ((creature.getCurrentHp() == creature.getMaxHp()) && skill.hasEffectType(EffectType.HEAL))
			{
				return false;
			}
			
			// Monsters only heal themselves and the monsters of their clan.
			if (attackable.isMonster() && skill.hasEffectType(EffectType.HEAL) && (creature != attackable) && (!creature.isNpc() || !attackable.isInMyClan(creature.asNpc())))
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Checks whether the target is a valid attack target, considering death, range, visibility and auto-attackability.
	 * @param target The {@code WorldObject} to check.
	 * @return {@code true} if the target is valid, {@code false} otherwise.
	 */
	private boolean checkTarget(WorldObject target)
	{
		if (target == null)
		{
			return false;
		}
		
		final Attackable npc = getActiveChar();
		if (target.isCreature())
		{
			if (target.asCreature().isDead())
			{
				return false;
			}
			
			if (npc.isMovementDisabled())
			{
				if (!npc.isInsideRadius2D(target, npc.getPhysicalAttackRange() + npc.getTemplate().getCollisionRadius() + target.asCreature().getTemplate().getCollisionRadius()))
				{
					return false;
				}
				
				if (!GeoEngine.getInstance().canSeeTarget(npc, target))
				{
					return false;
				}
			}
			
			if (!target.isAutoAttackable(npc))
			{
				return false;
			}
		}
		
		// Fixes monsters not avoiding obstacles.
		return true; // GeoEngine.getInstance().canMoveToTarget(npc.getX(), npc.getY(), npc.getZ(), target.getX(), target.getY(), target.getZ(), npc.getInstanceWorld());
	}
	
	/**
	 * Selects a valid target for the skill, preferring the lowest-HP creature for heals.
	 * @param skill The {@code Skill} to evaluate.
	 * @param insideCastRange {@code true} to restrict the search to cast range.
	 * @return The chosen {@code Creature}, or {@code null} if none.
	 */
	private Creature skillTargetReconsider(Skill skill, boolean insideCastRange)
	{
		// Check if skill can be casted.
		final Attackable npc = getActiveChar();
		if (!SkillCaster.checkUseConditions(npc, skill))
		{
			return null;
		}
		
		// There are cases where negative effect skills (negative effect points) are actually buffs and NPCs cast them on players, but they shouldn't.
		final boolean hasNegativeEffect = skill.isContinuous() ? skill.isDebuff() : skill.hasNegativeEffect();
		
		// Check current target first.
		final int range = insideCastRange ? skill.getCastRange() + getActiveChar().getTemplate().getCollisionRadius() : 2000; // TODO need some forget range
		
		final List<Creature> result = new ArrayList<>();
		if (hasNegativeEffect)
		{
			for (AggroInfo aggro : npc.getAggroList().values())
			{
				final Creature attacker = aggro.getAttacker();
				if (checkSkillTarget(skill, attacker))
				{
					result.add(attacker);
				}
			}
		}
		else
		{
			World.forEachVisibleObjectInRange(npc, Creature.class, range, creature ->
			{
				if (checkSkillTarget(skill, creature))
				{
					result.add(creature);
				}
			});
			
			// Maybe add self to the list of targets since getVisibleObjects doesn't return yourself.
			if (checkSkillTarget(skill, npc))
			{
				result.add(npc);
			}
			
			// For heal skills sort by hp missing.
			if (skill.hasEffectType(EffectType.HEAL))
			{
				int searchValue = Integer.MAX_VALUE;
				Creature creature = null;
				
				for (Creature c : result)
				{
					final int hpPer = c.getCurrentHpPercent();
					if (hpPer < searchValue)
					{
						searchValue = hpPer;
						creature = c;
					}
				}
				
				if (creature != null)
				{
					return creature;
				}
			}
		}
		
		// Return any target.
		if (!result.isEmpty())
		{
			return result.get(Rnd.get(result.size()));
		}
		
		return null;
	}
	
	/**
	 * Selects a new target from the aggro list and range, randomly or by highest hate.
	 * @param randomTarget {@code true} to pick a random target, {@code false} for highest hate.
	 * @return The chosen {@code Creature}, or {@code null} if none.
	 */
	private Creature targetReconsider(boolean randomTarget)
	{
		final Attackable npc = getActiveChar();
		if (randomTarget)
		{
			final List<Creature> result = new ArrayList<>();
			for (AggroInfo aggro : npc.getAggroList().values())
			{
				final Creature attacker = aggro.getAttacker();
				if (checkTarget(attacker))
				{
					result.add(attacker);
				}
			}
			
			// If npc is aggressive, add characters within aggro range too.
			if (npc.isAggressive())
			{
				World.forEachVisibleObjectInRange(npc, Creature.class, npc.getAggroRange(), creature ->
				{
					if (checkTarget(creature))
					{
						result.add(creature);
					}
				});
			}
			
			if (!result.isEmpty())
			{
				return result.get(Rnd.get(result.size()));
			}
		}
		
		long searchValue = Long.MIN_VALUE;
		Creature creature = null;
		for (AggroInfo aggro : npc.getAggroList().values())
		{
			final Creature attacker = aggro.getAttacker();
			final long hate = aggro.getHate();
			if (checkTarget(attacker) && (hate > searchValue))
			{
				searchValue = hate;
				creature = attacker;
			}
		}
		
		if ((creature == null) && npc.isAggressive())
		{
			final Creature _match = World.getFirstVisibleObjectInRange(npc, Creature.class, npc.getAggroRange(), nearby -> checkTarget(nearby));
			if (_match != null)
			{
				return _match;
			}
		}
		
		return null;
	}
	
	/**
	 * Dispatches to the think method for the current intention, guarding against recursion, inactive regions and disabled skills.
	 */
	@Override
	public void notifyActionThink()
	{
		// Check if a thinking action is already in progress.
		if (_thinking)
		{
			return;
		}
		
		// Check if region and its neighbors are active.
		final WorldRegion region = _actor.getWorldRegion();
		if ((region == null) || !region.areNeighborsActive())
		{
			return;
		}
		
		// Check if the actor is all skills disabled.
		final Attackable activeChar = getActiveChar();
		if (activeChar.isAllSkillsDisabled())
		{
			return;
		}
		
		// Start thinking action.
		_thinking = true;
		
		try
		{
			// Manage AI thinks of a Attackable.
			switch (getIntention())
			{
				case ACTIVE:
				{
					thinkActive();
					break;
				}
				case ATTACK:
				{
					thinkAttack();
					break;
				}
				case MOVE_TO:
				{
					// A walker keeps scanning for aggro while it moves along its route.
					if (activeChar.isWalker())
					{
						thinkActive();
					}
					break;
				}
				case CAST:
				{
					thinkCast();
					break;
				}
			}
		}
		catch (Exception e)
		{
			// LOGGER.warning(getClass().getSimpleName() + ": " + getActor().getName() + " - notifyActionThink() failed!");
		}
		finally
		{
			// Stop thinking action.
			_thinking = false;
		}
	}
	
	/**
	 * Handles being attacked by a source that carries no damage, such as a debuff.
	 * @param attackerObj The {@code WorldObject} that attacked.
	 */
	@Override
	public void notifyActionAttacked(WorldObject attackerObj)
	{
		notifyActionAttacked(attackerObj, 0);
	}
	
	/**
	 * Handles being attacked: refreshes aggro, switches to attack, alerts minions and calls faction members, then delegates to the superclass.
	 * @param attackerObj The {@code WorldObject} that attacked.
	 * @param damage The damage of the hit, shared with the faction members that answer the call.
	 */
	public void notifyActionAttacked(WorldObject attackerObj, int damage)
	{
		if (attackerObj == null)
		{
			return;
		}
		
		final Creature attacker = attackerObj.asCreature();
		if (attacker == null)
		{
			return;
		}
		
		final Attackable me = getActiveChar();
		
		// Fake players dismount when attacked, then fight back.
		if (me.isFakePlayer())
		{
			me.dismount();
		}
		
		final WorldObject target = getTarget();
		
		// Calculate the attack timeout.
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeTaskManager.getInstance().getGameTicks();
		
		// Set the _globalAggro to 0 to permit attack even just after spawn.
		if (_globalAggro < 0)
		{
			_globalAggro = 0;
		}
		
		// Add the attacker to the _aggroList of the actor if not present.
		if (!me.isInAggroList(attacker))
		{
			me.addDamageHate(attacker, 0, 1);
		}
		
		// Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others Player.
		if (!me.isRunning())
		{
			me.setRunning();
		}
		
		if (!me.isCoreAIDisabled())
		{
			// Set the Intention to ATTACK.
			if (getIntention() != Intention.ATTACK)
			{
				setIntentionAttack(attacker);
			}
			else
			{
				// The hate list already moved on, so follow it instead of turning on whoever just hit.
				final Creature mostHated = me.getMostHated();
				if ((mostHated != null) && (mostHated != target))
				{
					setIntentionAttack(mostHated);
				}
			}
		}
		
		if (me.isMonster())
		{
			Monster master = me.asMonster();
			if (master.hasMinions())
			{
				master.getMinionList().onAssist(me, attacker);
			}
			
			master = master.getLeader();
			if ((master != null) && master.hasMinions())
			{
				master.getMinionList().onAssist(me, attacker);
			}
		}
		
		// Handle all WorldObject of its Faction inside the Faction Range.
		final NpcTemplate template = me.getTemplate();
		final Set<Integer> clans = template.getClans();
		if ((clans != null) && !clans.isEmpty())
		{
			final int collision = template.getCollisionRadius();
			final int factionRange = template.getClanHelpRange() + collision;
			
			// Go through all WorldObject that belong to its faction.
			try
			{
				// Call friendly npcs for help only if this NPC was attacked by the target creature.
				final Creature finalTarget = attacker;
				if (me.getAttackByList().contains(finalTarget))
				{
					World.forEachVisibleObjectInRange(me, Attackable.class, factionRange, nearby ->
					{
						// Don't call dead npcs, npcs without ai or npcs which are too far away.
						if (nearby.isDead() || !nearby.hasAI() || (Math.abs(finalTarget.getZ() - nearby.getZ()) > 600))
						{
							return;
						}
						
						// Don't call npcs spawned within the last 7 seconds.
						if ((System.currentTimeMillis() - nearby.getSpawnTime()) < 7000)
						{
							return;
						}
						
						final CreatureAI nearbyAI = nearby.getAI();
						
						// Don't call npcs who are already doing some action (e.g. attacking, casting).
						if ((nearbyAI._intention != Intention.IDLE) && (nearbyAI._intention != Intention.ACTIVE))
						{
							return;
						}
						
						// Don't call npcs who aren't in the same clan.
						final NpcTemplate nearbytemplate = nearby.getTemplate();
						if (!template.isClan(nearbytemplate.getClans()) || (nearbytemplate.hasIgnoreClanNpcIds() && nearbytemplate.getIgnoreClanNpcIds().contains(me.getId())))
						{
							return;
						}
						
						if (finalTarget.isPlayable())
						{
							// The helper takes the hate of the hit that called it, on its own level and ratio, at thirty percent of the weight the victim gets.
							if (GeoEngine.getInstance().canSeeTarget(nearby, finalTarget))
							{
								final long hateValue = (long) ((((long) damage * 30) / (nearby.getLevel() + 7)) * nearby.getHateRatio(finalTarget));
								nearbyAI.notifyActionAggression(finalTarget, (int) Math.max(1, hateValue));
							}
							
							if (EventDispatcher.getInstance().hasListener(EventType.ON_ATTACKABLE_FACTION_CALL, nearby))
							{
								EventDispatcher.getInstance().notifyEventAsync(new OnAttackableFactionCall(nearby, me, finalTarget.asPlayer(), finalTarget.isSummon()), nearby);
							}
						}
						else if (nearbyAI._intention != Intention.ATTACK)
						{
							if (GeoEngine.getInstance().canSeeTarget(nearby, finalTarget))
							{
								nearby.addDamageHate(finalTarget, 0, me.getHating(finalTarget));
								nearbyAI.setIntentionAttack(finalTarget);
							}
						}
					});
				}
			}
			catch (NullPointerException e)
			{
				// LOGGER.warning(getClass().getSimpleName() + ": There has been a problem trying to think the attack!", e);
			}
		}
		
		super.notifyActionAttacked(attacker);
	}
	
	/**
	 * Adds hate for the target, sets the attack intention if not already attacking, and alerts minions.
	 * @param targetObj The {@code WorldObject} to target.
	 * @param aggro The amount of hate to add.
	 */
	@Override
	public void notifyActionAggression(WorldObject targetObj, int aggro)
	{
		if (targetObj == null)
		{
			return;
		}
		
		final Creature target = targetObj.asCreature();
		if (target == null)
		{
			return;
		}
		
		final Attackable me = getActiveChar();
		if (me.isDead())
		{
			return;
		}
		
		// Add the target to the actor _aggroList or update hate if already present.
		me.addDamageHate(target, 0, aggro);
		
		// Set the actor AI Intention to ATTACK.
		if (getIntention() != Intention.ATTACK)
		{
			// Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others Player.
			if (!me.isRunning())
			{
				me.setRunning();
			}
			
			setIntentionAttack(target);
		}
		
		if (me.isMonster())
		{
			Monster master = me.asMonster();
			if (master.hasMinions())
			{
				master.getMinionList().onAssist(me, target);
			}
			
			master = master.getLeader();
			if ((master != null) && master.hasMinions())
			{
				master.getMinionList().onAssist(me, target);
			}
		}
	}
	
	/**
	 * Sets the intention to active, clearing the attack timeout and starting the AI task.
	 */
	@Override
	public synchronized void setIntentionActive()
	{
		// Cancel attack timeout
		_attackTimeout = Integer.MAX_VALUE;
		super.setIntentionActive();
		
		// Schedule notifyActionThink repeatedly.
		startAITask();
	}
	
	/**
	 * Sets the global aggro counter.
	 * @param value The new global aggro value.
	 */
	/**
	 * Rolls the timeout of ATTACK that follows a spawn, three to seven seconds by default.<br>
	 * SetAggressiveTime overrides it per npc: zero acquires on sight, a positive value gates for that many seconds.
	 */
	public void rollGlobalAggro()
	{
		final int aggressiveTime = getActiveChar().getTemplate().getParameters().getInt("SetAggressiveTime", -1);
		if (aggressiveTime == 0)
		{
			_globalAggro = 0;
		}
		else if (aggressiveTime > 0)
		{
			// The counter is tested on the tick that raises it, so it is set one past the wanted delay.
			_globalAggro = -(aggressiveTime + 1);
		}
		else
		{
			_globalAggro = -(Rnd.get(5) + 4);
		}
	}
	
	public void setGlobalAggro(int value)
	{
		_globalAggro = value;
	}
	
	/**
	 * Sets the actor target, shared with the AI target.
	 * @param target The {@code WorldObject} to target.
	 */
	@Override
	public void setTarget(WorldObject target)
	{
		// NPCs share their regular target with AI target.
		_actor.setTarget(target);
	}
	
	/**
	 * Retrieves the actor target, shared with the AI target.
	 * @return The current {@code WorldObject} target, or {@code null} if none.
	 */
	@Override
	public WorldObject getTarget()
	{
		// NPCs share their regular target with AI target.
		return _actor.getTarget();
	}
	
	/**
	 * Retrieves the actor as an {@code Attackable}.
	 * @return The {@code Attackable} controlled by this AI.
	 */
	public Attackable getActiveChar()
	{
		return _actor.asAttackable();
	}
	
	/**
	 * Checks whether a template-parameterized skill may be cast, deferring to its configured gates.
	 * @param caster The {@code Attackable} casting.
	 * @param skill The {@code Skill} attempted.
	 * @param target The {@code Creature} targeted.
	 * @return {@code true} if the cast is permitted, {@code false} otherwise.
	 */
	private boolean allowsParameterizedCast(Attackable caster, Skill skill, Creature target)
	{
		if ((caster == null) || (skill == null))
		{
			return true;
		}
		
		final StatSet params = caster.getTemplate() != null ? caster.getTemplate().getParameters() : null;
		if ((params == null) || params.isEmpty())
		{
			return true;
		}
		
		// The retail classes roll every slot that declares the skill, the wizard ones keep the same skill in two slots split by distance.
		boolean declared = false;
		for (int slot = 1; slot <= MAX_PARAMETERIZED_SKILL_SLOTS; slot++)
		{
			final SkillHolder holder = params.getObject("Skill0" + slot + "_ID", SkillHolder.class);
			if ((holder == null) || (holder.getSkillId() != skill.getId()))
			{
				continue;
			}
			
			declared = true;
			if (passesParameterizedGates(slot, params, caster, target))
			{
				return true;
			}
		}
		
		return !declared;
	}
	
	/**
	 * Checks whether the parameterized skill slot passes its probability, HP window and distance gates.
	 * @param slot The skill slot to check.
	 * @param params The {@code StatSet} of skill parameters.
	 * @param caster The {@code Attackable} casting.
	 * @param target The {@code Creature} targeted.
	 * @return {@code true} if all gates pass, {@code false} otherwise.
	 */
	private boolean passesParameterizedGates(int slot, StatSet params, Attackable caster, Creature target)
	{
		final String prefix = "Skill0" + slot + "_";
		
		// Retail AI classes spell this parameter three different ways, all present in npcdata, and roll the "Probability" one on a 100 scale. An explicit 0 never casts.
		final int probability = params.contains(prefix + "Probability") ? (params.getInt(prefix + "Probability") * (SKILL_PROBABILITY_SCALE / PERCENT_SCALE)) : params.getInt(prefix + "Probablity", params.getInt(prefix + "Prob", DEFAULT_SKILL_PROBABILITY));
		final boolean checksDistance = params.getInt(prefix + "Check_Dist", 0) == 1;
		if (Rnd.get(SKILL_PROBABILITY_SCALE) >= probability)
		{
			return false;
		}
		
		// The skill fires only while the HP of its subject, the caster or the target when HPTarget is 1, stays between LowHP and HighHP.
		final Creature hpSource = (params.getInt(prefix + "HPTarget", 0) == 1) ? target : caster;
		if (hpSource == null)
		{
			return false;
		}
		
		final double hpPercent = (hpSource.getCurrentHp() * 100.0) / hpSource.getMaxHp();
		if ((hpPercent > params.getInt(prefix + "HighHP", DEFAULT_SKILL_HIGH_HP)) || (hpPercent < params.getInt(prefix + "LowHP", 0)))
		{
			return false;
		}
		
		if (checksDistance && (target != null))
		{
			final int distMin = params.getInt(prefix + "Dist_Min", 0);
			final int distMax = params.getInt(prefix + "Dist_Max", DEFAULT_SKILL_DIST_MAX);
			final int distance = (int) caster.calculateDistance2D(target);
			if ((distance < distMin) || (distance >= distMax))
			{
				return false;
			}
		}
		
		return true;
	}
}
