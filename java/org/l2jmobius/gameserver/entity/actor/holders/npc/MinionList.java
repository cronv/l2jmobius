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
package org.l2jmobius.gameserver.entity.actor.holders.npc;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.geoengine.GeoEngine;

/**
 * Manages the spawning, tracking, and lifecycle of minions for monster masters.<br>
 * This class handles automatic respawning, combat assistance, and position synchronization during teleports.
 * @author luisantonioa, DS, Mobius
 */
public class MinionList
{
	// Constants.
	private static final int SPAWN_OFFSET_RADIUS = 200;
	private static final int MINIMUM_SPAWN_RADIUS = 30;
	private static final int BASE_AGGRO_AMOUNT = 1;
	private static final int MASTER_AGGRO_AMOUNT = 10;
	private static final int RAID_AGGRO_MULTIPLIER = 10;
	private static final int INVALID_RESPAWN_TIME = -1;
	private static final int NO_RESPAWN = 0;
	
	// Master Monster Management.
	protected final Monster _master;
	
	// Minion Tracking.
	private final Set<Monster> _spawnedMinions = ConcurrentHashMap.newKeySet();
	private final Set<ScheduledFuture<?>> _respawnTasks = ConcurrentHashMap.newKeySet();
	
	/**
	 * Creates a new {@code MinionList} for a specific master.<br>
	 * This constructor initializes the list with the provided {@code Monster}.
	 * @param master The {@code Monster} that will own these minions.
	 */
	public MinionList(Monster master)
	{
		if (master == null)
		{
			throw new NullPointerException("MinionList: Master monster cannot be null!");
		}
		
		_master = master;
	}
	
	/**
	 * Retrieves all currently active minions.<br>
	 * This method returns the collection of {@code Monster} objects that have been spawned.
	 * @return a {@code Collection} of {@code Monster} objects representing the spawned minions.
	 */
	public Collection<Monster> getSpawnedMinions()
	{
		return _spawnedMinions;
	}
	
	/**
	 * Spawns the required number of minions for a master.<br>
	 * This method checks if the master is alive before spawning.<br>
	 * It compares the desired count with currently spawned minions.<br>
	 * Only missing minions are created based on their unique IDs.
	 * @param minions The collection of {@code MinionHolder} objects to process.
	 */
	public void spawnMinions(Collection<MinionHolder> minions)
	{
		if (_master.isAlikeDead() || (minions == null))
		{
			return;
		}
		
		int minionCount;
		int minionId;
		int minionsToSpawn;
		for (MinionHolder minion : minions)
		{
			minionCount = minion.getCount();
			minionId = minion.getId();
			minionsToSpawn = minionCount - countSpawnedMinionsById(minionId);
			if (minionsToSpawn > 0)
			{
				for (int i = 0; i < minionsToSpawn; i++)
				{
					spawnMinion(minionId);
				}
			}
		}
	}
	
	/**
	 * This method is called when a new minion is spawned.<br>
	 * It adds the {@code minion} to the internal list of active minions.
	 * @param minion The {@code Monster} instance that was just spawned.
	 */
	public void onMinionSpawn(Monster minion)
	{
		_spawnedMinions.add(minion);
	}
	
	/**
	 * Handles the logic when the master monster dies.<br>
	 * It removes all active minions and cancels their respawn tasks.<br>
	 * This action occurs if the master is a raid boss, if {@code force} is {@code true}, or if global config allows it.
	 * @param force Set to {@code true} to manually trigger the removal of all minions regardless of other conditions.
	 */
	public void onMasterDie(boolean force)
	{
		if (_master.isRaid() || force || NpcConfig.FORCE_DELETE_MINIONS)
		{
			if (!_spawnedMinions.isEmpty())
			{
				for (Monster minion : _spawnedMinions)
				{
					if (minion != null)
					{
						minion.setLeader(null);
						minion.deleteMe();
					}
				}
				
				_spawnedMinions.clear();
			}
			
			if (!_respawnTasks.isEmpty())
			{
				for (ScheduledFuture<?> task : _respawnTasks)
				{
					if ((task != null) && !task.isCancelled() && !task.isDone())
					{
						task.cancel(true);
					}
				}
				
				_respawnTasks.clear();
			}
		}
	}
	
	/**
	 * Handles the logic when a minion dies.<br>
	 * Removes the dead minion from the active list.<br>
	 * Schedules a respawn task if the time is valid.
	 * @param minion The {@code Monster} that has died.
	 * @param respawnTime The time in milliseconds until the minion should respawn.
	 */
	public void onMinionDie(Monster minion, int respawnTime)
	{
		// Prevent memory leaks.
		if (respawnTime == NO_RESPAWN)
		{
			minion.setLeader(null);
		}
		
		_spawnedMinions.remove(minion);
		
		final int actualRespawnTime = respawnTime <= INVALID_RESPAWN_TIME ? _master.isRaid() ? (int) NpcConfig.RAID_MINION_RESPAWN_TIMER : NO_RESPAWN : respawnTime;
		if ((actualRespawnTime > NO_RESPAWN) && !_master.isAlikeDead())
		{
			_respawnTasks.add(ThreadPool.schedule(new MinionRespawnTask(minion), actualRespawnTime));
		}
	}
	
	/**
	 * Handles the logic for when a creature assists in an attack.<br>
	 * This method updates the hate levels of the master and its minions based on the attacker.
	 * @param caller The {@code Creature} that initiated the action or call.
	 * @param attacker The {@code Creature} being attacked.
	 */
	public void onAssist(Creature caller, Creature attacker)
	{
		if (attacker == null)
		{
			return;
		}
		
		if (!_master.isAlikeDead() && !_master.isInCombat())
		{
			_master.addDamageHate(attacker, 0, BASE_AGGRO_AMOUNT);
		}
		
		final boolean callerIsMaster = caller == _master;
		int aggroAmount = callerIsMaster ? MASTER_AGGRO_AMOUNT : BASE_AGGRO_AMOUNT;
		if (_master.isRaid())
		{
			aggroAmount *= RAID_AGGRO_MULTIPLIER;
		}
		
		for (Monster minion : _spawnedMinions)
		{
			if ((minion != null) && !minion.isDead() && (callerIsMaster || !minion.isInCombat()))
			{
				minion.addDamageHate(attacker, 0, aggroAmount);
			}
		}
	}
	
	/**
	 * This method is called when the master monster teleports.<br>
	 * It updates the positions of all active minions to maintain their formation.<br>
	 * Each minion is moved to a new random location relative to the master's position.
	 */
	public void onMasterTeleported()
	{
		final int spawnRadius = SPAWN_OFFSET_RADIUS;
		final int minimumRadius = (int) _master.getCollisionRadius() + MINIMUM_SPAWN_RADIUS;
		for (Monster minion : _spawnedMinions)
		{
			if ((minion != null) && !minion.isDead() && !minion.isMovementDisabled())
			{
				int newX = Rnd.get(minimumRadius * 2, spawnRadius * 2); // x
				int newY = Rnd.get(newX, spawnRadius * 2); // distance
				newY = (int) Math.sqrt((newY * newY) - (newX * newX)); // y
				final int masterX = _master.getX();
				if (newX > (spawnRadius + minimumRadius))
				{
					newX = (masterX + newX) - spawnRadius;
				}
				else
				{
					newX = (masterX - newX) + minimumRadius;
				}
				
				final int masterY = _master.getY();
				if (newY > (spawnRadius + minimumRadius))
				{
					newY = (masterY + newY) - spawnRadius;
				}
				else
				{
					newY = (masterY - newY) + minimumRadius;
				}
				
				// The drawn point may hang over a wall or a hole of a cave with several floors, the minion stays on the floor of the master.
				minion.teleToLocation(_master.isFlying() ? new Location(newX, newY, _master.getZ()) : GeoEngine.getInstance().getValidLocation(masterX, masterY, _master.getZ(), newX, newY, _master.getZ(), _master.getInstanceWorld()));
			}
		}
	}
	
	/**
	 * Spawns a single minion based on the provided template ID.<br>
	 * This method checks if the {@code minionTemplateId} is valid before spawning.<br>
	 * It uses the internal master reference to handle the creation process.
	 * @param minionTemplateId The unique identifier for the minion type to spawn.
	 */
	private void spawnMinion(int minionTemplateId)
	{
		if (minionTemplateId == 0)
		{
			return;
		}
		
		spawnMinion(_master, minionTemplateId);
	}
	
	/**
	 * Task for respawning dead minions after a configured delay.<br>
	 * Restores the minion's state and inherits master's current aggro list.
	 */
	private class MinionRespawnTask implements Runnable
	{
		private final Monster _minion;
		
		/**
		 * Creates a respawn task for the specified minion.
		 * @param minion the minion to respawn
		 */
		public MinionRespawnTask(Monster minion)
		{
			_minion = minion;
		}
		
		@Override
		public void run()
		{
			// Minion can be already spawned or deleted.
			if (!_master.isAlikeDead() && _master.isSpawned() && !_minion.isSpawned())
			{
				// _minion.refreshId();
				initializeNpc(_master, _minion);
				
				// Assist master with existing aggro targets.
				final Map<Creature, AggroInfo> masterAggro = _master.getAggroList();
				if (!masterAggro.isEmpty())
				{
					final Map<Creature, AggroInfo> minionAggro = _minion.getAggroList();
					minionAggro.putAll(masterAggro);
					_minion.getAI().setIntentionAttack(minionAggro.keySet().iterator().next());
				}
			}
		}
	}
	
	/**
	 * Creates and initializes a new minion for a specific master.<br>
	 * This method retrieves the template from {@code NpcData} based on the provided ID.<br>
	 * It returns the newly created {@code Monster} or {@code null} if the template is invalid.
	 * @param master The {@code Monster} that will act as the owner of the minion.
	 * @param minionTemplateId The unique identifier for the minion type to spawn.
	 * @return The spawned {@code Monster} instance, or {@code null} if it failed to load.
	 */
	public static Monster spawnMinion(Monster master, int minionTemplateId)
	{
		// Get the template of the Minion to spawn.
		final NpcTemplate minionTemplate = NpcData.getInstance().getTemplate(minionTemplateId);
		if (minionTemplate == null)
		{
			return null;
		}
		
		return initializeNpc(master, new Monster(minionTemplate));
	}
	
	/**
	 * Sets up the initial state for a new minion.<br>
	 * This method resets the health and mana of the {@code minion}.<br>
	 * It links the {@code minion} to its {@code master} leader.<br>
	 * It also calculates a random spawn position near the {@code master}.
	 * @param master The monster that acts as the owner of the minion.
	 * @param minion The monster being initialized as a minion.
	 * @return The fully initialized {@code Monster} object.
	 */
	protected static Monster initializeNpc(Monster master, Monster minion)
	{
		minion.stopAllEffects();
		minion.setDead(false);
		minion.setDecayed(false);
		
		// Set the Minion HP, MP and Heading.
		minion.fullRestore();
		minion.setHeading(master.getHeading());
		
		// Set the Minion leader to this RaidBoss.
		minion.setLeader(master);
		
		// Move monster to masters instance.
		minion.setInstance(master.getInstanceWorld());
		
		// Set custom Npc server side name and title.
		final NpcTemplate template = minion.getTemplate();
		if (template.isUsingServerSideName())
		{
			minion.setName(template.getName());
		}
		
		if (template.isUsingServerSideTitle())
		{
			minion.setTitle(template.getTitle());
		}
		
		// Initialize the position of the Minion and add it in the world as a visible object.
		final int spawnRadius = SPAWN_OFFSET_RADIUS;
		final int minimumRadius = (int) master.getCollisionRadius() + MINIMUM_SPAWN_RADIUS;
		int newX = Rnd.get(minimumRadius * 2, spawnRadius * 2); // x
		int newY = Rnd.get(newX, spawnRadius * 2); // distance
		newY = (int) Math.sqrt((newY * newY) - (newX * newX)); // y
		final int masterX = master.getX();
		if (newX > (spawnRadius + minimumRadius))
		{
			newX = (masterX + newX) - spawnRadius;
		}
		else
		{
			newX = (masterX - newX) + minimumRadius;
		}
		
		final int masterY = master.getY();
		if (newY > (spawnRadius + minimumRadius))
		{
			newY = (masterY + newY) - spawnRadius;
		}
		else
		{
			newY = (masterY - newY) + minimumRadius;
		}
		
		// The drawn point may hang over a wall or a hole of a cave with several floors, the minion is born on the floor of the master.
		final Location location = master.isFlying() ? new Location(newX, newY, master.getZ()) : GeoEngine.getInstance().getValidLocation(masterX, masterY, master.getZ(), newX, newY, master.getZ(), master.getInstanceWorld());
		minion.spawnMe(location.getX(), location.getY(), location.getZ());
		
		// Make sure info is broadcasted in instances.
		if (minion.getInstanceId() > 0)
		{
			minion.broadcastInfo();
		}
		
		return minion;
	}
	
	/**
	 * Counts how many active minions match a specific template ID.<br>
	 * It iterates through the list of spawned minions and checks their IDs.
	 * @param minionTemplateId The unique identifier for the minion type to count.
	 * @return The total number of spawned minions that match the given {@code minionTemplateId}.
	 */
	private int countSpawnedMinionsById(int minionTemplateId)
	{
		int count = 0;
		for (Monster minion : _spawnedMinions)
		{
			if ((minion != null) && (minion.getId() == minionTemplateId))
			{
				count++;
			}
		}
		
		return count;
	}
	
	/**
	 * Returns the total number of minions currently spawned.<br>
	 * This count is based on the size of the internal minion collection.
	 * @return The current number of active minions as an {@code int}.
	 */
	public int getSpawnedMinionCount()
	{
		return _spawnedMinions.size();
	}
}
