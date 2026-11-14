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
package org.l2jmobius.gameserver.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.actor.instance.Fence;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.taskmanagers.RandomAnimationTaskManager;

/**
 * Represents a specific geographical area within the game world.<br>
 * It tracks the visible objects, doors and fences within a localized region of the map.<br>
 * Regions activate and deactivate based on nearby player activity to optimize performance.
 */
public class WorldRegion
{
	/** Set containing visible objects in this world region. */
	private final Set<WorldObject> _visibleObjects = ConcurrentHashMap.newKeySet();
	/** Set containing doors in this world region. Allocated on first use, most regions hold none. */
	private volatile Set<Door> _doors = null;
	/** Set containing fences in this world region. Allocated on first use, most regions hold none. */
	private volatile Set<Fence> _fences = null;
	/** Array containing nearby regions forming this world region's effective area. */
	private volatile WorldRegion[] _surroundingRegions;
	private final int _regionX;
	private final int _regionY;
	private final int _regionZ;
	private boolean _active = GeneralConfig.GRIDS_ALWAYS_ON;
	private ScheduledFuture<?> _neighborsTask = null;
	private final AtomicInteger _activeNeighbors = new AtomicInteger();
	
	/**
	 * Creates a new {@code WorldRegion} instance.<br>
	 * This constructor initializes the region with specific coordinates.
	 * @param regionX The X coordinate of the region.
	 * @param regionY The Y coordinate of the region.
	 * @param regionZ The Z coordinate of the region.
	 */
	public WorldRegion(int regionX, int regionY, int regionZ)
	{
		_regionX = regionX;
		_regionY = regionY;
		_regionZ = regionZ;
	}
	
	/**
	 * Toggles the AI state for all visible objects in this region.<br>
	 * If {@code isOn} is {@code false}, it stops actions and resets mobs.<br>
	 * If {@code isOn} is {@code true}, it resumes regeneration and animations.
	 * @param isOn The state to set for the AI systems.
	 */
	private void switchAI(boolean isOn)
	{
		if (_visibleObjects.isEmpty())
		{
			return;
		}
		
		if (!isOn)
		{
			for (WorldObject wo : _visibleObjects)
			{
				if (wo.isAttackable())
				{
					final Attackable mob = wo.asAttackable();
					
					// Set target to null and cancel attack or cast.
					mob.setTarget(null);
					
					// Stop movement.
					mob.stopMove(null);
					
					// Stop all active skills effects in progress on the Creature.
					mob.stopAllEffects();
					
					mob.clearAggroList();
					mob.getAttackByList().clear();
					
					// Teleport to spawn when too far away.
					final Spawn spawn = mob.getSpawn();
					if ((spawn != null) && (mob.calculateDistance2D(spawn) > NpcConfig.MAX_DRIFT_RANGE))
					{
						mob.teleToLocation(spawn);
					}
					
					// Stop the AI tasks.
					if (mob.hasAI())
					{
						final CreatureAI ai = mob.getAI();
						ai.setIntentionIdle();
						ai.stopAITask();
					}
					
					// Stop attack task.
					mob.abortAttack();
					
					RandomAnimationTaskManager.getInstance().remove(mob);
				}
				else if (wo.isNpc())
				{
					RandomAnimationTaskManager.getInstance().remove(wo.asNpc());
				}
			}
		}
		else
		{
			for (WorldObject wo : _visibleObjects)
			{
				if (wo.isAttackable())
				{
					// Start HP/MP/CP regeneration task.
					wo.asAttackable().getStatus().startHpMpRegeneration();
					RandomAnimationTaskManager.getInstance().add(wo.asNpc());
				}
				else if (wo.isNpc())
				{
					RandomAnimationTaskManager.getInstance().add(wo.asNpc());
				}
			}
		}
	}
	
	/**
	 * Checks if this world region is currently active.
	 * @return {@code true} if the region is active, {@code false} otherwise.
	 */
	public boolean isActive()
	{
		return _active;
	}
	
	/**
	 * Increments the count of active neighboring regions.
	 */
	private void incrementActiveNeighbors()
	{
		_activeNeighbors.incrementAndGet();
	}
	
	/**
	 * Decrements the count of active neighboring regions.
	 */
	private void decrementActiveNeighbors()
	{
		_activeNeighbors.decrementAndGet();
	}
	
	/**
	 * Checks if the surrounding regions are active.<br>
	 * This method returns {@code true} if the global configuration is set to always on.<br>
	 * It also returns {@code true} if at least one neighboring region is currently active.
	 * @return {@code true} if neighbors are active or config is enabled, {@code false} otherwise.
	 */
	public boolean areNeighborsActive()
	{
		return GeneralConfig.GRIDS_ALWAYS_ON || (_activeNeighbors.get() > 0);
	}
	
	/**
	 * Checks whether the active surrounding regions contain no playable objects.
	 * @return {@code true} if no playable object is found in any active surrounding region, {@code false} otherwise.
	 */
	public boolean areNeighborsEmpty()
	{
		final WorldRegion[] surroundingRegions = getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final WorldRegion worldRegion = surroundingRegions[i];
			if (worldRegion.isActive())
			{
				final Collection<WorldObject> regionObjects = worldRegion.getVisibleObjects();
				if (regionObjects.isEmpty())
				{
					continue;
				}
				
				for (WorldObject wo : regionObjects)
				{
					if ((wo != null) && wo.isPlayable())
					{
						return false;
					}
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Sets whether this world region is currently active.<br>
	 * This method updates the internal state and notifies neighboring regions.<br>
	 * It also toggles the AI status based on the new value.
	 * @param value The {@code boolean} value to set for activation.
	 */
	public synchronized void setActive(boolean value)
	{
		if (_active == value)
		{
			return;
		}
		
		_active = value;
		
		final WorldRegion[] surroundingRegions = getSurroundingRegions();
		if (value)
		{
			for (int i = 0; i < surroundingRegions.length; i++)
			{
				surroundingRegions[i].incrementActiveNeighbors();
			}
		}
		else
		{
			for (int i = 0; i < surroundingRegions.length; i++)
			{
				surroundingRegions[i].decrementActiveNeighbors();
			}
		}
		
		// Turn the AI on or off to match the region's activation.
		switchAI(value);
	}
	
	/**
	 * Activates this region and schedules the activation of its surrounding regions.<br>
	 * Any pending neighbor task is cancelled first.<br>
	 * The surrounding regions are activated after {@code GeneralConfig.GRID_NEIGHBOR_TURNON_TIME} seconds.
	 */
	private void startActivation()
	{
		// First set self to active and do self-tasks...
		setActive(true);
		
		// If the timer to deactivate neighbors is running, cancel it.
		synchronized (this)
		{
			if (_neighborsTask != null)
			{
				_neighborsTask.cancel(true);
				_neighborsTask = null;
			}
			
			// Then, set a timer to activate the neighbors.
			_neighborsTask = ThreadPool.schedule(() ->
			{
				final WorldRegion[] surroundingRegions = getSurroundingRegions();
				for (int i = 0; i < surroundingRegions.length; i++)
				{
					surroundingRegions[i].setActive(true);
				}
			}, 1000 * GeneralConfig.GRID_NEIGHBOR_TURNON_TIME);
		}
	}
	
	/**
	 * Deactivates this region and schedules the deactivation of its surrounding regions.<br>
	 * Any pending neighbor task is cancelled first.<br>
	 * After {@code GeneralConfig.GRID_NEIGHBOR_TURNOFF_TIME} seconds, each surrounding region with no players is deactivated.
	 */
	private void startDeactivation()
	{
		// If the timer to activate neighbors is running, cancel it.
		synchronized (this)
		{
			if (_neighborsTask != null)
			{
				_neighborsTask.cancel(true);
				_neighborsTask = null;
			}
			
			// Start a timer to "suggest" a deactivate to self and neighbors.
			// Suggest means: first check if a neighbor has Players in it. If not, deactivate.
			_neighborsTask = ThreadPool.schedule(() ->
			{
				final WorldRegion[] surroundingRegions = getSurroundingRegions();
				for (int i = 0; i < surroundingRegions.length; i++)
				{
					final WorldRegion worldRegion = surroundingRegions[i];
					if (worldRegion.areNeighborsEmpty())
					{
						worldRegion.setActive(false);
					}
				}
			}, 1000 * GeneralConfig.GRID_NEIGHBOR_TURNOFF_TIME);
		}
	}
	
	/**
	 * Adds a {@code WorldObject} to this region's visible objects.<br>
	 * Doors and fences are also registered with the surrounding regions.<br>
	 * Adding a playable object activates this region and its neighbors when grids are not always on.
	 * @param object The {@code WorldObject} to add.
	 */
	public synchronized void addVisibleObject(WorldObject object)
	{
		if (object == null)
		{
			return;
		}
		
		_visibleObjects.add(object);
		
		if (object.isDoor())
		{
			final WorldRegion[] surroundingRegions = getSurroundingRegions();
			for (int i = 0; i < surroundingRegions.length; i++)
			{
				surroundingRegions[i].addDoor(object.asDoor());
			}
		}
		else if (object.isFence())
		{
			final WorldRegion[] surroundingRegions = getSurroundingRegions();
			for (int i = 0; i < surroundingRegions.length; i++)
			{
				surroundingRegions[i].addFence((Fence) object);
			}
		}
		
		// If this is the first player to enter the region, activate self and neighbors.
		if (object.isPlayable() && !_active && !GeneralConfig.GRIDS_ALWAYS_ON)
		{
			startActivation();
		}
	}
	
	/**
	 * Removes a {@code WorldObject} from this region's visible objects.<br>
	 * Doors and fences are also removed from the surrounding regions.<br>
	 * Removing a playable object deactivates this region and its neighbors once they are empty and grids are not always on.
	 * @param object The {@code WorldObject} to remove.
	 */
	public synchronized void removeVisibleObject(WorldObject object)
	{
		if (object == null)
		{
			return;
		}
		
		if (_visibleObjects.isEmpty())
		{
			return;
		}
		
		_visibleObjects.remove(object);
		
		if (object.isDoor())
		{
			final WorldRegion[] surroundingRegions = getSurroundingRegions();
			for (int i = 0; i < surroundingRegions.length; i++)
			{
				surroundingRegions[i].removeDoor(object.asDoor());
			}
		}
		else if (object.isFence())
		{
			final WorldRegion[] surroundingRegions = getSurroundingRegions();
			for (int i = 0; i < surroundingRegions.length; i++)
			{
				surroundingRegions[i].removeFence((Fence) object);
			}
		}
		
		if (object.isPlayable() && areNeighborsEmpty() && !GeneralConfig.GRIDS_ALWAYS_ON)
		{
			startDeactivation();
		}
	}
	
	/**
	 * Retrieves all objects currently visible in this world region.
	 * @return a {@code Collection} of {@code WorldObject} instances.
	 */
	public Collection<WorldObject> getVisibleObjects()
	{
		return _visibleObjects;
	}
	
	/**
	 * Registers a {@code Door} with this region.
	 * @param door The {@code Door} to add.
	 */
	public synchronized void addDoor(Door door)
	{
		if (_doors == null)
		{
			_doors = ConcurrentHashMap.newKeySet();
		}
		
		_doors.add(door);
	}
	
	/**
	 * Removes a {@code Door} from this region.
	 * @param door The {@code Door} to remove.
	 */
	private void removeDoor(Door door)
	{
		final Set<Door> doors = _doors;
		if (doors != null)
		{
			doors.remove(door);
		}
	}
	
	/**
	 * Retrieves all doors located in this world region.
	 * @return A {@code Collection} of {@code Door} objects, empty when none are registered.
	 */
	public Collection<Door> getDoors()
	{
		final Set<Door> doors = _doors;
		return doors == null ? Collections.emptySet() : doors;
	}
	
	/**
	 * Registers a {@code Fence} with this region.
	 * @param fence The {@code Fence} to add.
	 */
	public synchronized void addFence(Fence fence)
	{
		if (_fences == null)
		{
			_fences = ConcurrentHashMap.newKeySet();
		}
		
		_fences.add(fence);
	}
	
	/**
	 * Removes a {@code Fence} from this region.
	 * @param fence The {@code Fence} to remove.
	 */
	private void removeFence(Fence fence)
	{
		final Set<Fence> fences = _fences;
		if (fences != null)
		{
			fences.remove(fence);
		}
	}
	
	/**
	 * Retrieves all fences located in this world region.
	 * @return a {@code Collection} of {@code Fence} objects, empty when none are registered.
	 */
	public Collection<Fence> getFences()
	{
		final Set<Fence> fences = _fences;
		return fences == null ? Collections.emptySet() : fences;
	}
	
	/**
	 * Retrieves the regions that are adjacent to this one.<br>
	 * These regions form the effective area of the current {@code WorldRegion}.<br>
	 * The array is built once on first access, creating every adjacent region that does not exist yet.<br>
	 * Adjacent regions are created without linking their own surroundings, to avoid cascading creation across the grid.
	 * @return an array of {@code WorldRegion} objects representing the surrounding areas.
	 */
	public WorldRegion[] getSurroundingRegions()
	{
		WorldRegion[] surroundingRegions = _surroundingRegions;
		if (surroundingRegions == null)
		{
			synchronized (this)
			{
				surroundingRegions = _surroundingRegions;
				if (surroundingRegions == null)
				{
					// This region is always first, to improve bulk operations when this region should be updated first.
					final List<WorldRegion> regionList = new ArrayList<>();
					regionList.add(this);
					for (int sx = _regionX - 1; sx <= (_regionX + 1); sx++)
					{
						for (int sy = _regionY - 1; sy <= (_regionY + 1); sy++)
						{
							for (int sz = _regionZ - 1; sz <= (_regionZ + 1); sz++)
							{
								if (((sx == _regionX) && (sy == _regionY) && (sz == _regionZ)) || (sx < 0) || (sx >= World.REGIONS_X) || (sy < 0) || (sy >= World.REGIONS_Y) || (sz < 0) || (sz >= World.REGIONS_Z))
								{
									continue;
								}
								
								final int index = ((((sx * (World.REGIONS_Y + 1)) + sy) * World.REGIONS_Z) + sz);
								WorldRegion region = World.getWorldRegions().get(index);
								if (region == null)
								{
									final WorldRegion newRegion = new WorldRegion(sx, sy, sz);
									final WorldRegion existingRegion = World.getWorldRegions().compareAndExchange(index, null, newRegion);
									region = existingRegion == null ? newRegion : existingRegion;
								}
								
								regionList.add(region);
							}
						}
					}
					
					surroundingRegions = regionList.toArray(new WorldRegion[regionList.size()]);
					_surroundingRegions = surroundingRegions;
				}
			}
		}
		
		return surroundingRegions;
	}
	
	/**
	 * Checks whether the given {@code WorldRegion} is adjacent to this one.<br>
	 * Two regions are adjacent when their coordinates differ by at most one on each axis.
	 * @param region The {@code WorldRegion} to check.
	 * @return {@code true} if the region is adjacent to this one, {@code false} if it is not or is {@code null}.
	 */
	public boolean isSurroundingRegion(WorldRegion region)
	{
		if (region == null)
		{
			return false;
		}
		
		return (Math.abs(_regionX - region._regionX) <= 1) && (Math.abs(_regionY - region._regionY) <= 1) && (Math.abs(_regionZ - region._regionZ) <= 1);
	}
	
	/**
	 * Retrieves the X coordinate of this region in the world grid.
	 * @return The {@code int} X coordinate.
	 */
	public int getRegionX()
	{
		return _regionX;
	}
	
	/**
	 * Retrieves the Y coordinate of this region in the world grid.
	 * @return The {@code int} Y coordinate.
	 */
	public int getRegionY()
	{
		return _regionY;
	}
	
	/**
	 * Retrieves the Z coordinate of this region in the world grid.
	 * @return The {@code int} Z coordinate.
	 */
	public int getRegionZ()
	{
		return _regionZ;
	}
	
	/**
	 * Returns this region's coordinates in {@code (x, y, z)} form.
	 * @return A string representation of this region's coordinates.
	 */
	@Override
	public String toString()
	{
		return "(" + _regionX + ", " + _regionY + ", " + _regionZ + ")";
	}
}
