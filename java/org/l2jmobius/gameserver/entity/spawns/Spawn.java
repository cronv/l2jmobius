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
package org.l2jmobius.gameserver.entity.spawns;

import java.lang.reflect.Constructor;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.time.SchedulingPattern;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.custom.RandomSpawnsConfig;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.holders.npc.MinionList;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.zone.type.WaterZone;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.taskmanagers.RespawnTaskManager;
import org.l2jmobius.gameserver.taskmanagers.WalkingTaskManager;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * This class manages the spawning and respawning of groups of {@code Npc} entities within a specific area.<br>
 * It handles both exact coordinate placement and random distribution within defined locations.<br>
 * The heading can be set to an exact value or randomized if it is set to {@code -1}.
 * @author Nightmare, Mobius
 */
public class Spawn extends Location
{
	protected static final Logger LOGGER = Logger.getLogger(Spawn.class.getName());
	
	/** String identifier of this spawn */
	private String _name;
	/** The link on the NpcTemplate object containing generic and static properties of this spawn (ex : RewardExp, RewardSP, AggroRange...) */
	private NpcTemplate _template;
	/** The maximum number of Npc that can manage this Spawn */
	private int _maximumCount;
	/** The current number of Npc managed by this Spawn */
	private int _currentCount;
	/** The current number of SpawnTask in progress or stand by of this Spawn */
	public int _scheduledCount;
	/** The identifier of the location area where Npc can be spawned */
	private int _locationId;
	/** The spawn instance id */
	private int _instanceId = 0;
	/** Minimum respawn delay */
	private int _respawnMinDelay;
	/** Maximum respawn delay */
	private int _respawnMaxDelay;
	/** Respawn Pattern **/
	private SchedulingPattern _respawnPattern;
	/** The generic constructor of Npc managed by this Spawn */
	private Constructor<? extends Npc> _constructor;
	/** If True an Npc is respawned each time that another is killed */
	private boolean _doRespawn = true;
	private final Deque<Npc> _spawnedNpcs = new ConcurrentLinkedDeque<>();
	private boolean _randomWalk = false; // Is no random walk
	private NpcSpawnTemplate _spawnTemplate;
	
	/**
	 * Creates a new {@code Spawn} instance using the provided {@code NpcTemplate}.<br>
	 * This constructor initializes the internal template and prepares the reflection data for spawning.
	 * @param template The {@code NpcTemplate} used to define the NPC properties.
	 * @throws ClassNotFoundException If the NPC class cannot be found.
	 * @throws NoSuchMethodException If the required constructor is missing.
	 * @throws ClassCastException If the loaded class is not a subclass of {@code Npc}.
	 */
	public Spawn(NpcTemplate template) throws ClassNotFoundException, NoSuchMethodException, ClassCastException
	{
		super(0, 0, -10000);
		
		// Set the _template of the Spawn.
		_template = template;
		
		if (_template == null)
		{
			return;
		}
		
		final String className = "org.l2jmobius.gameserver.entity.actor.instance." + _template.getType();
		
		// Create the generic constructor of Npc managed by this Spawn.
		_constructor = Class.forName(className).asSubclass(Npc.class).getConstructor(NpcTemplate.class);
	}
	
	/**
	 * Creates a new {@code Spawn} instance using a specific NPC ID.<br>
	 * This constructor retrieves the {@code NpcTemplate} from the data manager.<br>
	 * It also initializes the reflection constructor for the NPC class.
	 * @param npcId The unique identifier of the NPC to spawn.
	 * @throws ClassNotFoundException If the NPC class cannot be found.
	 * @throws NoSuchMethodException If the required constructor is missing.
	 * @throws ClassCastException If the loaded class is not a subclass of {@code Npc}.
	 */
	public Spawn(int npcId) throws ClassNotFoundException, NoSuchMethodException, ClassCastException
	{
		super(0, 0, -10000);
		_template = Objects.requireNonNull(NpcData.getInstance().getTemplate(npcId), "NpcTemplate not found for NPC ID: " + npcId);
		
		final String className = "org.l2jmobius.gameserver.entity.actor.instance." + _template.getType();
		
		// Create the generic constructor of Npc managed by this Spawn.
		_constructor = Class.forName(className).asSubclass(Npc.class).getConstructor(NpcTemplate.class);
	}
	
	/**
	 * Retrieves the maximum number of NPCs allowed for this spawn.
	 * @return The current {@code _maximumCount}.
	 */
	public int getAmount()
	{
		return _maximumCount;
	}
	
	/**
	 * Retrieves the name of this spawn group.
	 * @return The {@code String} name of the spawn.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Sets the display name for this spawn group.<br>
	 * The value is stored in the {@code _name} field.
	 * @param name The new name to assign to the spawn group.
	 */
	public void setName(String name)
	{
		_name = name;
	}
	
	/**
	 * Retrieves the unique identifier for the spawn location.<br>
	 * This ID identifies where a group of {@code Npc} entities will appear.
	 * @return The {@code int} value representing the location ID.
	 */
	public int getLocationId()
	{
		return _locationId;
	}
	
	/**
	 * Retrieves the unique identifier for this spawn.<br>
	 * This value is obtained from the underlying {@code NpcTemplate}.
	 * @return The {@code int} ID of the template.
	 */
	public int getId()
	{
		return _template.getId();
	}
	
	/**
	 * Gets the minimum time delay for an NPC to respawn.<br>
	 * This value is used as the lower bound for the random respawn interval.
	 * @return The minimum respawn delay in milliseconds.
	 */
	public int getRespawnMinDelay()
	{
		return _respawnMinDelay;
	}
	
	/**
	 * Retrieves the maximum time delay for an NPC to respawn.<br>
	 * This value is used as the upper limit for random respawn intervals.
	 * @return The maximum respawn delay in milliseconds.
	 */
	public int getRespawnMaxDelay()
	{
		return _respawnMaxDelay;
	}
	
	/**
	 * Retrieves the current respawn pattern for this spawn group.<br>
	 * This pattern determines how and when NPCs are rescheduled to appear.
	 * @return the {@code SchedulingPattern} used for respawning.
	 */
	public SchedulingPattern getRespawnPattern()
	{
		return _respawnPattern;
	}
	
	/**
	 * Sets the maximum number of NPCs for this spawn group.<br>
	 * This updates the {@code _maximumCount} field.
	 * @param amount The new maximum count to set.
	 */
	public void setAmount(int amount)
	{
		_maximumCount = amount;
	}
	
	/**
	 * Sets the unique identifier for the location.<br>
	 * This value determines where the {@code Npc} group will spawn.
	 * @param id The new {@code int} ID for the location.
	 */
	public void setLocationId(int id)
	{
		_locationId = id;
	}
	
	/**
	 * Sets the minimum time delay for an NPC to respawn.<br>
	 * This value is used by the {@code RespawnTaskManager}.
	 * @param date The new minimum delay in milliseconds.
	 */
	public void setRespawnMinDelay(int date)
	{
		_respawnMinDelay = date;
	}
	
	/**
	 * Sets the maximum time delay for an NPC to respawn.<br>
	 * This value determines the upper limit of the random respawn interval.
	 * @param date The maximum delay in milliseconds.
	 */
	public void setRespawnMaxDelay(int date)
	{
		_respawnMaxDelay = date;
	}
	
	/**
	 * Reduces the current count of active NPCs for this spawn.<br>
	 * Removes the specified NPC from the internal list of spawned entities.<br>
	 * Schedules a new respawn if the total count is below the maximum limit.
	 * @param oldNpc The {@code Npc} instance that was removed or destroyed.
	 */
	public void decreaseCount(Npc oldNpc)
	{
		// sanity check
		if (_currentCount <= 0)
		{
			return;
		}
		
		// Decrease the current number of Npc of this Spawn.
		_currentCount--;
		
		// Remove this NPC from list of spawned.
		_spawnedNpcs.remove(oldNpc);
		
		// Check if respawn is possible to prevent multiple respawning caused by lag.
		if (_doRespawn && ((_scheduledCount + _currentCount) < _maximumCount))
		{
			// Update the current number of SpawnTask in progress or stand by of this Spawn.
			_scheduledCount++;
			
			// Schedule the next respawn.
			RespawnTaskManager.getInstance().add(oldNpc, System.currentTimeMillis() + (hasRespawnRandom() ? Rnd.get(_respawnMinDelay, _respawnMaxDelay) : _respawnMinDelay));
		}
		
		// Notify the spawn group, it rolls the replacement itself and the dead unit leaves the tables.
		if (_spawnTemplate != null)
		{
			_spawnTemplate.releaseDeadUnit(oldNpc);
			_spawnTemplate.getGroup().onUnitDeath(_spawnTemplate, oldNpc.getInstanceWorld());
		}
	}
	
	/**
	 * Initializes the spawn group by creating NPCs until the maximum count is reached.<br>
	 * Sets whether respawning is enabled based on the minimum delay.
	 * @return The total number of NPCs currently spawned.
	 */
	public int init()
	{
		while (_currentCount < _maximumCount)
		{
			doSpawn();
		}
		
		_doRespawn = _respawnMinDelay > 0;
		
		return _currentCount;
	}
	
	/**
	 * Checks if the respawn system is currently active.<br>
	 * Returns {@code true} if respawning is enabled.<br>
	 * Returns {@code false} if respawning is disabled.
	 * @return The current status of the respawn system.
	 */
	public boolean isRespawnEnabled()
	{
		return _doRespawn;
	}
	
	/**
	 * Stops the automatic respawning of NPCs for this spawn group.<br>
	 * Sets the internal {@code _doRespawn} flag to {@code false}.
	 */
	public void stopRespawn()
	{
		_doRespawn = false;
	}
	
	/**
	 * Starts the respawn process for this spawn group.<br>
	 * Sets the internal {@code _doRespawn} flag to {@code true}.
	 */
	public void startRespawn()
	{
		_doRespawn = true;
	}
	
	/**
	 * Spawns a new {@code Npc} instance if respawning is enabled.<br>
	 * This method checks the {@code _doRespawn} flag before proceeding.
	 * @return The spawned {@code Npc} object or {@code null} if respawning is disabled.
	 */
	public Npc doSpawn()
	{
		return _doRespawn ? doSpawn(false) : null;
	}
	
	/**
	 * Spawns a new {@code Npc} based on the current template.<br>
	 * This method handles special logic for summon animations if requested.<br>
	 * It returns {@code null} if the template is a Pet, Decoy, or Trap type.
	 * @param isSummonSpawn Determines if the NPC should show a summon animation.
	 * @return The initialized {@code Npc} object or {@code null} if spawning fails.
	 */
	public Npc doSpawn(boolean isSummonSpawn)
	{
		try
		{
			// Check if the spawn is not a Pet, Decoy or Trap spawn.
			if (_template.isType("Pet") || _template.isType("Decoy") || _template.isType("Trap"))
			{
				_currentCount++;
				return null;
			}
			
			// Call the constructor of the Npc.
			final Npc npc = _constructor.newInstance(_template);
			npc.setInstanceById(_instanceId); // Must be done before object is spawned into visible world.
			if (isSummonSpawn)
			{
				npc.setShowSummonAnimation(isSummonSpawn);
			}
			
			return initializeNpc(npc);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error while spawning " + _template.getId(), e);
		}
		
		return null;
	}
	
	/**
	 * Prepares and spawns an {@code Npc} instance.<br>
	 * This method sets the location, heading, and name of the {@code Npc}.<br>
	 * It also handles random spawning logic and updates the spawn count.
	 * @param npc The {@code Npc} object to initialize.
	 * @return The initialized {@code Npc} instance.
	 */
	private Npc initializeNpc(Npc npc)
	{
		// Reset some variables
		npc.onRespawn();
		
		int newlocx = 0;
		int newlocy = 0;
		int newlocz = -10000;
		
		// If Locx and Locy are not defined, the Npc must be spawned in an area defined by location or spawn territory.
		if (_spawnTemplate != null)
		{
			final Location loc = _spawnTemplate.getSpawnLocation();
			newlocx = loc.getX();
			newlocy = loc.getY();
			newlocz = loc.getZ();
			setLocation(loc);
		}
		else if ((getX() == 0) && (getY() == 0))
		{
			LOGGER.warning("NPC " + npc + " doesn't have spawn location!");
			return null;
		}
		else
		{
			// The Npc is spawned at the exact position (Lox, Locy, Locz).
			newlocx = getX();
			newlocy = getY();
			newlocz = getZ();
		}
		
		// Check if npc is in water.
		final WaterZone water = ZoneManager.getInstance().getZone(newlocx, newlocy, newlocz, WaterZone.class);
		
		// If random spawn system is enabled.
		final boolean npcIsMonster = npc.isMonster();
		final boolean npcIsFlying = npc.isFlying();
		if (RandomSpawnsConfig.ENABLE_RANDOM_MONSTER_SPAWNS && (getHeading() != -1) && npcIsMonster && !npc.isQuestMonster() && !WalkingTaskManager.getInstance().isTargeted(npc) && (getInstanceId() == 0) && !getTemplate().isUndying() && !npc.isRaid() && !npc.isRaidMinion() && !npcIsFlying && (water == null) && !RandomSpawnsConfig.MOBS_LIST_NOT_RANDOM.contains(npc.getId()))
		{
			final int randX = newlocx + Rnd.get(RandomSpawnsConfig.MOB_MIN_SPAWN_RANGE, RandomSpawnsConfig.MOB_MAX_SPAWN_RANGE);
			final int randY = newlocy + Rnd.get(RandomSpawnsConfig.MOB_MIN_SPAWN_RANGE, RandomSpawnsConfig.MOB_MAX_SPAWN_RANGE);
			final Instance npcInstanceWorld = npc.getInstanceWorld();
			if (GeoEngine.getInstance().canMoveToTarget(newlocx, newlocy, newlocz, randX, randY, newlocz, npcInstanceWorld) //
				&& GeoEngine.getInstance().canSeeTarget(newlocx, newlocy, newlocz, randX, randY, newlocz, npcInstanceWorld))
			{
				newlocx = randX;
				newlocy = randY;
				setXYZ(newlocx, newlocy, newlocz);
				setHeading(-1);
			}
		}
		
		// Correct Z of monsters.
		if (npcIsMonster && !npcIsFlying && (water == null))
		{
			// Do not correct Z distances greater than 300.
			final int geoZ = GeoEngine.getInstance().getHeight(newlocx, newlocy, newlocz);
			if (LocationUtil.calculateDistance(newlocx, newlocy, newlocz, newlocx, newlocy, geoZ, true, true) < 300)
			{
				newlocz = geoZ;
			}
			
			// Prevent new z exceeding spawn territory high z.
			if (_spawnTemplate != null)
			{
				final int highZ = _spawnTemplate.getHighZ();
				if ((highZ != Integer.MAX_VALUE) && (highZ < newlocz))
				{
					newlocz = highZ;
				}
			}
		}
		
		// Set is not random walk default value.
		npc.setRandomWalking(_randomWalk);
		
		// Set the heading of the Npc (random heading if not defined).
		final int currentHeading = getHeading();
		if (currentHeading == -1)
		{
			npc.setHeading(Rnd.get(61794));
		}
		else
		{
			npc.setHeading(currentHeading);
		}
		
		// Set custom Npc server side name and title.
		final NpcTemplate npcTemplate = npc.getTemplate();
		if (npcTemplate.isUsingServerSideName())
		{
			npc.setName(npcTemplate.getName());
		}
		
		if (npcTemplate.isUsingServerSideTitle())
		{
			npc.setTitle(npcTemplate.getTitle());
		}
		
		// Link the Npc to this Spawn.
		npc.setSpawn(this);
		
		// Spawn NPC
		npc.spawnMe(newlocx, newlocy, newlocz);
		
		// Make sure info is broadcasted in instances.
		if (npc.getInstanceId() > 0)
		{
			npc.broadcastInfo();
		}
		
		if (_spawnTemplate != null)
		{
			_spawnTemplate.notifySpawnNpc(npc);
		}
		
		_spawnedNpcs.add(npc);
		
		// Increase the current number of Npcs managed by this Spawn.
		_currentCount++;
		
		// Minions
		if (npcIsMonster)
		{
			final MinionList minionList = npc.asMonster().getMinionList();
			if (NpcData.getMasterMonsterIDs().contains(npc.getId()))
			{
				minionList.spawnMinions(npc.getParameters().getMinionList("Privates"));
			}
			
			// The privates declared by the spawn entry itself come back with the leader as well.
			if (_spawnTemplate != null)
			{
				minionList.spawnMinions(_spawnTemplate.getMinions());
			}
		}
		
		return npc;
	}
	
	/**
	 * Sets the respawn delay for this spawn group.<br>
	 * This method calculates a minimum and maximum delay based on a base value and an interval.<br>
	 * If {@code delay} is {@code 0}, both min and max delays are set to {@code 0}.
	 * @param delay The base time in milliseconds for the respawn delay.
	 * @param randomInterval The amount to add or subtract from the base delay to create a range.
	 */
	public void setRespawnDelay(int delay, int randomInterval)
	{
		if (delay != 0)
		{
			if (delay < 0)
			{
				LOGGER.warning("respawn delay is negative for spawn:" + this);
			}
			
			final int minDelay = delay - randomInterval;
			final int maxDelay = delay + randomInterval;
			
			_respawnMinDelay = Math.max(1, minDelay) * 1000;
			_respawnMaxDelay = Math.max(1, maxDelay) * 1000;
		}
		else
		{
			_respawnMinDelay = 0;
			_respawnMaxDelay = 0;
		}
	}
	
	/**
	 * Sets the scheduling pattern for this spawn group.<br>
	 * This determines how often and when NPCs will reappear.
	 * @param respawnPattern The {@code SchedulingPattern} to apply.
	 */
	public void setRespawnPattern(SchedulingPattern respawnPattern)
	{
		_respawnPattern = respawnPattern;
	}
	
	/**
	 * Sets the time delay for an NPC to respawn.<br>
	 * This method uses a fixed delay without any random variation.
	 * @param delay The amount of time to wait before the NPC respawns.
	 */
	public void setRespawnDelay(int delay)
	{
		setRespawnDelay(delay, 0);
	}
	
	/**
	 * Calculates the average respawn delay.<br>
	 * This value is derived from {@code getRespawnMinDelay()} and {@code getRespawnMaxDelay()}.
	 * @return The calculated average delay as an {@code int}.
	 */
	public int getRespawnDelay()
	{
		return (_respawnMinDelay + _respawnMaxDelay) / 2;
	}
	
	/**
	 * Checks if the respawn delay is randomized.<br>
	 * This returns {@code true} if the minimum and maximum delays are different.<br>
	 * It returns {@code false} if both values are equal.
	 * @return {@code true} if the respawn delay varies, {@code false} otherwise.
	 */
	public boolean hasRespawnRandom()
	{
		return _respawnMinDelay != _respawnMaxDelay;
	}
	
	/**
	 * Retrieves the chase range for this spawn.<br>
	 * This value is taken from the associated {@code NpcTemplate}.<br>
	 * If no template exists, it returns {@code 0}.
	 * @return The chase range as an {@code int}.
	 */
	public int getChaseRange()
	{
		if (_spawnTemplate == null)
		{
			return 0;
		}
		
		return _spawnTemplate.getChaseRange();
	}
	
	/**
	 * Retrieves the most recently spawned {@code Npc}.<br>
	 * This method looks at the end of the internal spawn list.<br>
	 * It returns {@code null} if no NPCs have been spawned yet.
	 * @return The last {@code Npc} object spawned, or {@code null} if empty.
	 */
	public Npc getLastSpawn()
	{
		if (!_spawnedNpcs.isEmpty())
		{
			return _spawnedNpcs.peekLast();
		}
		
		return null;
	}
	
	/**
	 * Removes the most recently spawned {@code Npc} from the current spawn group.<br>
	 * This method checks if any NPCs are currently active before attempting deletion.
	 * @return {@code true} if an NPC was successfully deleted, or {@code false} otherwise.
	 */
	public boolean deleteLastNpc()
	{
		return !_spawnedNpcs.isEmpty() && _spawnedNpcs.getLast().deleteMe();
	}
	
	/**
	 * Retrieves the collection of {@code Npc} entities that have been spawned.
	 * @return a {@code Deque} containing all currently spawned {@code Npc} objects.
	 */
	public Deque<Npc> getSpawnedNpcs()
	{
		return _spawnedNpcs;
	}
	
	/**
	 * Respawns an existing {@code Npc} if respawning is enabled.<br>
	 * This method initializes the NPC and adds it back to its instance world.
	 * @param oldNpc The {@code Npc} object to be respawned.
	 */
	public void respawnNpc(Npc oldNpc)
	{
		if (_doRespawn)
		{
			// oldNpc.refreshID();
			initializeNpc(oldNpc);
			
			// Register NPC back to instance world.
			final Instance instance = oldNpc.getInstanceWorld();
			if (instance != null)
			{
				instance.addNpc(oldNpc);
			}
		}
	}
	
	/**
	 * Retrieves the {@code NpcTemplate} associated with this spawn.
	 * @return the {@code NpcTemplate} object.
	 */
	public NpcTemplate getTemplate()
	{
		return _template;
	}
	
	/**
	 * Retrieves the unique identifier for this instance.<br>
	 * This ID distinguishes this specific spawn group from others.
	 * @return The {@code int} value of the instance ID.
	 */
	public int getInstanceId()
	{
		return _instanceId;
	}
	
	/**
	 * Sets the unique identifier for this specific spawn instance.<br>
	 * This value is used to distinguish between different instances of the same spawn type.
	 * @param instanceId The unique {@code int} ID to assign to this instance.
	 */
	public void setInstanceId(int instanceId)
	{
		_instanceId = instanceId;
	}
	
	/**
	 * Checks if the entity is currently performing a random walk.
	 * @return {@code true} if the entity is walking randomly, {@code false} otherwise.
	 */
	public boolean getRandomWalking()
	{
		return _randomWalk;
	}
	
	/**
	 * Enables or disables the random walking behavior for this spawn.<br>
	 * Set to {@code true} to allow NPCs to move randomly.<br>
	 * Set to {@code false} to keep them stationary.
	 * @param value The boolean flag to enable or disable random walking.
	 */
	public void setRandomWalking(boolean value)
	{
		_randomWalk = value;
	}
	
	/**
	 * Sets the template for the NPC spawn.<br>
	 * This method updates the {@code _spawnTemplate} field with the provided data.
	 * @param npcSpawnTemplate The {@code NpcSpawnTemplate} to use for spawning.
	 */
	public void setSpawnTemplate(NpcSpawnTemplate npcSpawnTemplate)
	{
		_spawnTemplate = npcSpawnTemplate;
	}
	
	/**
	 * Retrieves the template used for spawning NPCs.<br>
	 * This returns the {@code NpcSpawnTemplate} associated with this spawn group.
	 * @return the {@code NpcSpawnTemplate} object.
	 */
	public NpcSpawnTemplate getNpcSpawnTemplate()
	{
		return _spawnTemplate;
	}
	
	/**
	 * @return {@code true} when this spawn is bound to a territory instead of a fixed point
	 */
	public boolean hasSpawnTerritory()
	{
		return (_spawnTemplate != null) && _spawnTemplate.hasTerritory();
	}
	
	/**
	 * @param x
	 * @param y
	 * @return {@code true} if the point lies inside the territory of this spawn
	 */
	public boolean isInsideSpawnTerritory(int x, int y)
	{
		return (_spawnTemplate != null) && _spawnTemplate.isInsideTerritory(x, y);
	}
	
	/**
	 * Returns a string representation of the spawn data.<br>
	 * This includes the {@code NpcTemplate} ID, coordinates, and heading.
	 * @return A formatted string containing the spawn details.
	 */
	@Override
	public String toString()
	{
		return "Spawn ID: " + _template.getId() + " X: " + getX() + " Y: " + getY() + " Z: " + getZ() + " Heading: " + getHeading();
	}
}
