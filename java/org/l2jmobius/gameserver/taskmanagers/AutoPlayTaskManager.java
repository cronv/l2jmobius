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
package org.l2jmobius.gameserver.taskmanagers;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.holders.player.AutoPlaySettingsHolder;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.autoplay.ExAutoPlayDoMacro;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Manages the execution of automated actions for players and their summons.<br>
 * This class handles background tasks to simulate gameplay behavior based on {@code AutoPlaySettingsHolder} configurations.
 * @author Mobius
 */
public class AutoPlayTaskManager
{
	private static final Set<Set<Player>> POOLS = ConcurrentHashMap.newKeySet();
	private static final Set<Player> THINKING_PLAYERS = ConcurrentHashMap.newKeySet();
	private static final Map<Player, Integer> IDLE_COUNT = new ConcurrentHashMap<>();
	private static final int POOL_SIZE = 200;
	private static final int TASK_DELAY = 700;
	
	// Warg skills.
	private static final int MOON_JUMP = 31460;
	private static final int INSTINCT_DASH = 31476;
	
	/**
	 * Initializes a new instance of the {@code AutoPlayTaskManager}.<br>
	 * This constructor is protected to restrict instantiation.
	 */
	protected AutoPlayTaskManager()
	{
	}
	
	private class AutoPlay implements Runnable
	{
		private final Set<Player> _players;
		
		public AutoPlay(Set<Player> players)
		{
			_players = players;
		}
		
		@Override
		public void run()
		{
			if (_players.isEmpty())
			{
				return;
			}
			
			for (Player player : _players)
			{
				// A cast completion may think for the player on its own thread, so a pool pass never overlaps it.
				if (THINKING_PLAYERS.add(player))
				{
					try
					{
						think(player);
					}
					finally
					{
						THINKING_PLAYERS.remove(player);
					}
				}
			}
		}
		
		private void think(Player player)
		{
			if (!player.isOnline() || (player.getOfflineMode().isEnabled() && !player.getOfflineMode().isPlayRunning()) || !GeneralConfig.ENABLE_AUTO_PLAY)
			{
				stopAutoPlay(player);
				return;
			}
			
			if (player.isSitting() || player.isCastingNow() || (player.getQueuedSkill() != null))
			{
				return;
			}
			
			// Next target mode.
			final int targetMode = player.getAutoPlaySettings().getNextTargetMode();
			
			// Skip thinking.
			final WorldObject target = player.getTarget();
			if ((target != null) && target.isCreature())
			{
				final Creature creature = target.asCreature();
				if (creature.isAlikeDead() || !isTargetModeValid(targetMode, player, creature))
				{
					player.setTarget(null);
				}
				else
				{
					// GeoEngine can see target check.
					if (!GeoEngine.getInstance().canSeeTarget(player, creature))
					{
						player.setTarget(null);
						return;
					}
					
					// Summons are ordered on the current target, no matter what that target is busy with.
					if (player.getOfflineMode().isPlayRunning() && player.hasSummon())
					{
						for (Summon summon : player.getServitors().values())
						{
							final CreatureAI summonAI = summon.getAI();
							final boolean alreadyEngaged = (summonAI.getIntention() == Intention.ATTACK) && (summonAI.getTarget() == creature);
							if (summon.hasAI() && !summon.isMoving() && !summon.isDisabled() && !alreadyEngaged && (summonAI.getIntention() != Intention.CAST) && creature.isAutoAttackable(player))
							{
								summonAI.setIntentionAttack(creature);
							}
						}
					}
					
					if ((creature.getTarget() == player) || (creature.getTarget() == null))
					{
						// Manage Warg skill.
						if (player.isWarg())
						{
							castWargSkill(player, creature);
						}
						
						// We take granted that mage classes do not auto hit.
						if (isMageCaster(player))
						{
							// A caster out of range would otherwise stand still casting at a target it never approaches, so walk it into skill range.
							// Skip this when a pet or servitor is doing the fighting, otherwise the owner runs across the map to every far mob and up to its corpse.
							if (!player.hasServitors() && !player.hasPet() && (player.calculateDistance2D(creature) > 900) && !player.isMoving() && player.hasAI() && !player.isDisabled())
							{
								player.getAI().setIntentionMoveTo(creature);
							}
							
							return;
						}
						
						// Check if actually attacking.
						if (player.hasAI() && !player.isAttackingNow() && !player.isCastingNow() && !player.isMoving() && !player.isDisabled())
						{
							final CreatureAI playerAI = player.getAI();
							if (playerAI.getIntention() != Intention.ATTACK)
							{
								if (creature.isAutoAttackable(player))
								{
									// GeoEngine can see target check.
									if (!GeoEngine.getInstance().canSeeTarget(player, creature))
									{
										player.setTarget(null);
										return;
									}
									
									playerAI.setIntentionAttack(creature);
								}
							}
							else
							{
								// Re-submitting the same attack revalidates a move to target that stalled.
								player.getAI().setIntentionAttack(creature);
								if (player.isAttackingNow() || player.isCastingNow() || player.isMoving())
								{
									IDLE_COUNT.remove(player);
									return;
								}
								
								final Weapon weapon = player.getActiveWeaponItem();
								if (weapon != null)
								{
									final int idleCount = IDLE_COUNT.getOrDefault(player, 0);
									if (idleCount > 10)
									{
										final boolean ranged = weapon.getItemType().isRanged();
										final double angle = LocationUtil.calculateHeadingFrom(player, creature);
										final double radian = Math.toRadians(angle);
										final double course = Math.toRadians(180);
										final double distance = (ranged ? player.getCollisionRadius() : player.getCollisionRadius() + creature.getCollisionRadius()) * 2;
										final int x1 = (int) (Math.cos(Math.PI + radian + course) * distance);
										final int y1 = (int) (Math.sin(Math.PI + radian + course) * distance);
										final Location location;
										if (ranged)
										{
											location = new Location(player.getX() + x1, player.getY() + y1, player.getZ());
										}
										else
										{
											location = new Location(creature.getX() + x1, creature.getY() + y1, player.getZ());
										}
										
										player.getAI().setIntentionMoveTo(location);
										IDLE_COUNT.remove(player);
									}
									else
									{
										IDLE_COUNT.put(player, idleCount + 1);
									}
								}
							}
						}
						
						return;
					}
				}
			}
			
			// Reset idle count.
			IDLE_COUNT.remove(player);
			
			// Pickup.
			if (player.getAutoPlaySettings().doPickup())
			{
				// A nearly full bag still takes stackable items that merge into an existing stack.
				final boolean inventoryUnder90 = player.isInventoryUnder90(false);
				
				// First item we can act on: either reachable far (move to it) or reachable close + pickup-eligible.
				final Item droppedItem = World.getFirstVisibleObjectInRange(player, Item.class, 200, item ->
				{
					if (!item.isSpawned() || GeneralConfig.IGNORED_AUTO_PICK_ITEMS.contains(item.getId()) || !GeoEngine.getInstance().canMoveToTarget(player.getX(), player.getY(), player.getZ(), item.getX(), item.getY(), item.getZ(), player.getInstanceWorld()))
					{
						return false;
					}
					
					if (!inventoryUnder90 && (!item.isStackable() || (player.getInventory().getItemByItemId(item.getId()) == null)))
					{
						return false;
					}
					
					// Far item: we can move to it.
					if (player.calculateDistance2D(item) > 70)
					{
						return true;
					}
					
					// Close item: must be unprotected or owned by player.
					return !item.isProtected() || (item.getOwnerId() == player.getObjectId());
				});
				
				if (droppedItem != null)
				{
					if (player.calculateDistance2D(droppedItem) > 70)
					{
						if (!player.isMoving())
						{
							player.getAI().setIntentionMoveTo(droppedItem);
						}
						
						return;
					}
					
					// Item is close and pickup-eligible.
					player.doPickupItem(droppedItem);
					return; // Avoid pickup being skipped.
				}
			}
			
			// Find target.
			Creature creature = null;
			final Party party = player.getParty();
			final Player leader = party == null ? null : party.getLeader();
			if (GeneralConfig.ENABLE_AUTO_ASSIST && (party != null) && (leader != null) && (leader != player) && !leader.isDead())
			{
				if (leader.calculateDistance3D(player) < (PlayerConfig.ALT_PARTY_RANGE * 2 /* 2? */))
				{
					final WorldObject leaderTarget = leader.getTarget();
					if ((leaderTarget != null) && (leaderTarget.isAttackable() || (leaderTarget.isPlayable() && !party.containsPlayer(leaderTarget.asPlayer()))))
					{
						creature = leaderTarget.asCreature();
					}
					else if (!player.isDisabled())
					{
						final CreatureAI playerAI = player.getAI();
						if (playerAI.getIntention() != Intention.FOLLOW)
						{
							playerAI.setIntentionFollow(leader);
						}
					}
				}
			}
			else
			{
				final AutoPlaySettingsHolder playerAutoPlaySettings = player.getAutoPlaySettings();
				creature = World.getNearestVisibleObjectInRange(player, Creature.class, playerAutoPlaySettings.isShortRange() && (targetMode != 2 /* Characters */) ? 600 : 1400, nearby ->
				{
					// Skip unavailable creatures.
					if (nearby.isAlikeDead())
					{
						return false;
					}
					
					// Check creature target.
					// Respectful hunting should only skip mobs that are engaged with another real player, not mobs holding a non-player target (monster-vs-monster aggro or a stale post-combat target).
					final WorldObject nearbyTarget = nearby.getTarget();
					if (playerAutoPlaySettings.isRespectfulHunting() && !nearby.isPlayable() && (nearbyTarget != null) && nearbyTarget.isPlayer() && (nearbyTarget != player) && !player.getServitors().containsKey(nearbyTarget.getObjectId()))
					{
						return false;
					}
					
					// Check next target mode.
					if (!isTargetModeValid(targetMode, player, nearby))
					{
						return false;
					}
					
					// Check if creature is reachable.
					return (Math.abs(player.getZ() - nearby.getZ()) < 800) && GeoEngine.getInstance().canSeeTarget(player, nearby) && GeoEngine.getInstance().canMoveToTarget(player.getX(), player.getY(), player.getZ(), nearby.getX(), nearby.getY(), nearby.getZ(), player.getInstanceWorld());
				});
			}
			
			// New target was assigned.
			if (creature != null)
			{
				player.setTarget(creature);
				
				// We take granted that mage classes do not auto hit.
				if (isMageCaster(player))
				{
					// A caster out of range would otherwise stand still casting at a target it never approaches, so walk it into skill range.
					// Skip this when a pet or servitor is doing the fighting, otherwise the owner runs across the map to every far mob and up to its corpse.
					if (!player.hasServitors() && !player.hasPet() && (player.calculateDistance2D(creature) > 900) && !player.isMoving() && player.hasAI() && !player.isDisabled())
					{
						player.getAI().setIntentionMoveTo(creature);
					}
					
					return;
				}
				
				player.sendPacket(ExAutoPlayDoMacro.STATIC_PACKET);
			}
		}
		
		private boolean isMageCaster(Player player)
		{
			// Iss classes considered fighters.
			final int classId = player.getActiveClass();
			if ((classId >= PlayerClass.ISS_HIEROPHANT.getId()) && (classId <= PlayerClass.ISS_DOOMCRYER.getId()))
			{
				return false;
			}
			
			return player.isMageClass() && (player.getRace() != Race.ORC);
		}
		
		private boolean isTargetModeValid(int mode, Player player, Creature creature)
		{
			if (!creature.isTargetable() || (creature.isNpc() && (creature.isInvul() || !creature.asNpc().isShowName())))
			{
				return false;
			}
			
			switch (mode)
			{
				case 1: // Monster
				{
					return creature.isMonster() && !creature.isRaid() && creature.isAutoAttackable(player);
				}
				case 2: // Characters
				{
					return creature.isPlayable() && creature.isAutoAttackable(player);
				}
				case 3: // NPC
				{
					return creature.isNpc() && !creature.isMonster() && !creature.isInsideZone(ZoneId.PEACE);
				}
				default: // Any Target
				{
					return (creature.isNpc() && !creature.isInsideZone(ZoneId.PEACE)) || (creature.isPlayable() && creature.isAutoAttackable(player));
				}
			}
		}
	}
	
	/**
	 * Executes the Instinct Dash skill for a player against a specific target.<br>
	 * This method checks all requirements such as distance, mana, and visibility.<br>
	 * It ensures the player is in auto-play mode before casting.
	 * @param player The {@code Player} who will cast the skill.
	 * @param target The {@code Creature} that will be targeted by the skill.
	 */
	private void castWargSkill(Player player, Creature target)
	{
		if ((player == null) || (target == null))
		{
			return;
		}
		
		if (!player.isAutoPlaying() || player.isCastingNow() || player.isAllSkillsDisabled() || player.isAlikeDead() || !player.isAffectedBySkill(MOON_JUMP))
		{
			return;
		}
		
		final Skill skill = SkillData.getInstance().getSkill(INSTINCT_DASH, player.getSkillLevel(INSTINCT_DASH));
		if (skill == null)
		{
			return;
		}
		
		if (player.calculateDistance2D(target) > skill.getCastRange())
		{
			return;
		}
		
		// Skill should not work if distance is lower than 80.
		if (player.calculateDistance2D(target) < 80)
		{
			return;
		}
		
		if (player.isSkillDisabled(skill) || player.isMuted() || (player.getCurrentMp() < skill.getMpConsume()))
		{
			return;
		}
		
		if (!target.isTargetable() || target.isAlikeDead() || !target.isAutoAttackable(player))
		{
			return;
		}
		
		if (!GeoEngine.getInstance().canSeeTarget(player, target))
		{
			return;
		}
		
		player.setTarget(target); // Ensure target set.
		player.useMagic(skill, null, false, false);
	}
	
	/**
	 * Runs a target pass for one player right away, instead of waiting for the next pool tick after an auto-skill finished off the target.
	 * @param player the auto-playing player without a live target
	 */
	public void pickNextTarget(Player player)
	{
		ThreadPool.execute(() ->
		{
			// The player may have stopped auto-play while the pass was pending.
			if (player.isAutoPlaying())
			{
				new AutoPlay(Set.of(player)).run();
			}
		});
	}
	
	/**
	 * Starts the auto-play system for a specific player.<br>
	 * This method checks if the {@code Player} is already in an active pool.<br>
	 * It assigns the {@code Player} to an existing pool or creates a new one if needed.
	 * @param player The {@code Player} who wants to start auto-playing.
	 */
	public synchronized void startAutoPlay(Player player)
	{
		for (Set<Player> pool : POOLS)
		{
			if (pool.contains(player))
			{
				return;
			}
		}
		
		player.setAutoPlaying(true);
		
		for (Set<Player> pool : POOLS)
		{
			if (pool.size() < POOL_SIZE)
			{
				player.onActionRequest();
				pool.add(player);
				return;
			}
		}
		
		final Set<Player> pool = ConcurrentHashMap.newKeySet(POOL_SIZE);
		player.onActionRequest();
		pool.add(player);
		ThreadPool.schedulePriorityTaskAtFixedRate(new AutoPlay(pool), TASK_DELAY, TASK_DELAY);
		POOLS.add(pool);
	}
	
	/**
	 * Stops the auto-play mode for a specific player.<br>
	 * This method removes the {@code Player} from all active pools.<br>
	 * It also sets the player's auto-playing status to {@code false}.<br>
	 * Any pets or summons are commanded to follow their owner.
	 * @param player The {@code Player} whose auto-play should be stopped.
	 */
	public void stopAutoPlay(Player player)
	{
		for (Set<Player> pool : POOLS)
		{
			if (pool.remove(player))
			{
				player.setAutoPlaying(false);
				
				// Pets must follow their owner.
				if (player.hasServitors())
				{
					for (Summon summon : player.getServitors().values())
					{
						summon.followOwner();
					}
				}
				
				if (player.hasPet())
				{
					player.getPet().followOwner();
				}
				
				IDLE_COUNT.remove(player);
				return;
			}
		}
	}
	
	/**
	 * Retrieves the singleton instance of the {@code AutoPlayTaskManager}.<br>
	 * Use this method to access the global manager for auto-play tasks.
	 * @return The single instance of {@code AutoPlayTaskManager}.
	 */
	public static AutoPlayTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AutoPlayTaskManager INSTANCE = new AutoPlayTaskManager();
	}
}
