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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.commons.time.SchedulingPattern;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.holders.npc.ChanceLocation;
import org.l2jmobius.gameserver.entity.actor.holders.npc.MinionHolder;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.zone.type.BannedSpawnTerritory;
import org.l2jmobius.gameserver.entity.zone.type.SpawnTerritory;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.interfaces.IParameterized;
import org.l2jmobius.gameserver.managers.DatabaseSpawnManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Represents a template for spawning {@code Npc} entities in the game world.<br>
 * It contains configuration data such as location, frequency, and spawn logic.<br>
 * This class is used by the server to manage how and where NPCs appear.
 * @author UnAfraid, Mobius
 */
public class NpcSpawnTemplate implements Cloneable, IParameterized<StatSet>
{
	private static final Logger LOGGER = Logger.getLogger(NpcSpawnTemplate.class.getName());
	
	private final int _id;
	private final int _count;
	private final Duration _respawnTime;
	private final Duration _respawnTimeRandom;
	private final SchedulingPattern _respawnPattern;
	private final int _chaseRange;
	private List<ChanceLocation> _locations;
	private SpawnTerritory _zone;
	private StatSet _parameters;
	private final boolean _spawnAnimation;
	private final boolean _saveInDB;
	private final String _dbName;
	private List<MinionHolder> _minions;
	private final SpawnTemplate _spawnTemplate;
	private final SpawnGroup _group;
	private final Set<Npc> _spawnedNpcs = ConcurrentHashMap.newKeySet();
	private int _aliveOrReserved = 0;
	
	/**
	 * Creates a new {@code NpcSpawnTemplate} by copying data from an existing one.<br>
	 * This constructor performs a deep copy of all internal fields.
	 * @param template The source {@code NpcSpawnTemplate} to copy from.
	 * @param group The group the copy belongs to.
	 */
	private NpcSpawnTemplate(NpcSpawnTemplate template, SpawnGroup group)
	{
		_spawnTemplate = template._spawnTemplate;
		_group = group;
		_id = template._id;
		_count = template._count;
		_respawnTime = template._respawnTime;
		_respawnTimeRandom = template._respawnTimeRandom;
		_respawnPattern = template._respawnPattern;
		_chaseRange = template._chaseRange;
		_spawnAnimation = template._spawnAnimation;
		_saveInDB = template._saveInDB;
		_dbName = template._dbName;
		_locations = template._locations;
		_zone = template._zone;
		_parameters = template._parameters;
		_minions = template._minions;
	}
	
	/**
	 * Creates a new {@code NpcSpawnTemplate} using the provided template, group, and statistics.<br>
	 * This constructor initializes all spawn properties such as respawn times, locations, and animations.<br>
	 * It merges parameters from both the {@code SpawnTemplate} and {@code SpawnGroup}.
	 * @param spawnTemplate The base template for the NPC spawn.
	 * @param group The group associated with this spawn.
	 * @param set The set of statistics containing specific configuration values.
	 */
	public NpcSpawnTemplate(SpawnTemplate spawnTemplate, SpawnGroup group, StatSet set)
	{
		_spawnTemplate = spawnTemplate;
		_group = group;
		_id = set.getInt("id");
		_count = set.getInt("count", 1);
		_respawnTime = set.getDuration("respawnTime", null);
		_respawnTimeRandom = set.getDuration("respawnRandom", null);
		final String pattern = set.getString("respawnPattern", null);
		_respawnPattern = (pattern == null) || pattern.isEmpty() ? null : new SchedulingPattern(pattern);
		_chaseRange = set.getInt("chaseRange", 0);
		_spawnAnimation = set.getBoolean("spawnAnimation", false);
		_saveInDB = set.getBoolean("dbSave", false);
		_dbName = set.getString("dbName", null);
		_parameters = mergeParameters(spawnTemplate, group);
		
		final int x = set.getInt("x", Integer.MAX_VALUE);
		final int y = set.getInt("y", Integer.MAX_VALUE);
		final int z = set.getInt("z", Integer.MAX_VALUE);
		final boolean xDefined = x != Integer.MAX_VALUE;
		final boolean yDefined = y != Integer.MAX_VALUE;
		final boolean zDefined = z != Integer.MAX_VALUE;
		if (xDefined && yDefined && zDefined)
		{
			_locations = new ArrayList<>();
			_locations.add(new ChanceLocation(x, y, z, set.getInt("heading", 0), 100));
		}
		else
		{
			if (xDefined || yDefined || zDefined)
			{
				throw new IllegalStateException(String.format("Spawn with partially declared and x: %s y: %s z: %s!", processParam(x), processParam(y), processParam(z)));
			}
			
			final String zoneName = set.getString("zone", null);
			if (zoneName != null)
			{
				final SpawnTerritory zone = ZoneManager.getInstance().getSpawnTerritory(zoneName);
				if (zone == null)
				{
					throw new NullPointerException("Spawn with non existing zone requested " + zoneName);
				}
				
				_zone = zone;
			}
		}
		
		mergeParameters(spawnTemplate, group);
	}
	
	/**
	 * Combines parameters from a template, a group, and the current instance.<br>
	 * It merges values into a new {@code StatSet}.<br>
	 * If all sources are {@code null}, it returns {@code null}.
	 * @param spawnTemplate The base template containing initial parameters.
	 * @param group The group providing additional shared parameters.
	 * @return A merged {@code StatSet} containing all combined values.
	 */
	private StatSet mergeParameters(SpawnTemplate spawnTemplate, SpawnGroup group)
	{
		final StatSet templateParams = spawnTemplate.getParameters();
		final StatSet groupParams = group.getParameters();
		if ((_parameters == null) && (templateParams == null) && (groupParams == null))
		{
			return null;
		}
		
		final StatSet set = new StatSet();
		if (templateParams != null)
		{
			set.merge(templateParams);
		}
		
		if (groupParams != null)
		{
			set.merge(groupParams);
		}
		
		if (_parameters != null)
		{
			set.merge(_parameters);
		}
		
		return set;
	}
	
	/**
	 * Adds a new location to the list of possible spawn points.<br>
	 * This method initializes the internal list if it is currently {@code null}.
	 * @param loc The {@code ChanceLocation} to be added.
	 */
	public void addSpawnLocation(ChanceLocation loc)
	{
		if (_locations == null)
		{
			_locations = new ArrayList<>();
		}
		
		_locations.add(loc);
	}
	
	/**
	 * Retrieves the {@code SpawnTemplate} associated with this object.
	 * @return The {@code SpawnTemplate} instance.
	 */
	public SpawnTemplate getSpawnTemplate()
	{
		return _spawnTemplate;
	}
	
	/**
	 * Retrieves the {@code SpawnGroup} associated with this template.
	 * @return the {@code SpawnGroup} object.
	 */
	public SpawnGroup getGroup()
	{
		return _group;
	}
	
	/**
	 * Converts an integer value into a string representation.<br>
	 * If the input is {@code Integer.MAX_VALUE}, it returns {@code "undefined"}.<br>
	 * Otherwise, it returns the string version of the number.
	 * @param value The integer to be processed.
	 * @return A string representation of the input value.
	 */
	private String processParam(int value)
	{
		return value != Integer.MAX_VALUE ? Integer.toString(value) : "undefined";
	}
	
	/**
	 * Retrieves the unique identifier for this {@code NpcSpawnTemplate}.
	 * @return The {@code int} ID of the template.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the total count of NPCs for this template.
	 * @return The current count as an {@code int}.
	 */
	public int getCount()
	{
		return _count;
	}
	
	/**
	 * Retrieves the time required for an NPC to respawn.<br>
	 * This value is returned as a {@code Duration}.
	 * @return The respawn duration.
	 */
	public Duration getRespawnTime()
	{
		return _respawnTime;
	}
	
	/**
	 * Retrieves the random respawn time for this template.<br>
	 * This value is used to determine how long a creature stays dead before reappearing.
	 * @return the {@code Duration} of the random respawn time.
	 */
	public Duration getRespawnTimeRandom()
	{
		return _respawnTimeRandom;
	}
	
	/**
	 * Retrieves the respawn pattern for this template.<br>
	 * This determines how often and in what sequence NPCs reappear.
	 * @return the {@code SchedulingPattern} used for respawning.
	 */
	public SchedulingPattern getRespawnPattern()
	{
		return _respawnPattern;
	}
	
	/**
	 * Gets the maximum distance an NPC can chase a target.
	 * @return The chase range as an {@code int}.
	 */
	public int getChaseRange()
	{
		return _chaseRange;
	}
	
	/**
	 * Retrieves the list of possible locations for this spawn.
	 * @return a {@code List} of {@code ChanceLocation} objects.
	 */
	public List<ChanceLocation> getLocation()
	{
		return _locations;
	}
	
	/**
	 * Retrieves the territory where this NPC template is allowed to spawn.
	 * @return the {@code SpawnTerritory} associated with this template.
	 */
	public SpawnTerritory getZone()
	{
		return _zone;
	}
	
	/**
	 * @return {@code true} when this entry spawns inside a territory instead of at fixed locations
	 */
	public boolean hasTerritory()
	{
		return (_zone != null) || !_group.getTerritories().isEmpty() || !_spawnTemplate.getTerritories().isEmpty();
	}
	
	/**
	 * Tests whether a point lies inside the territory this entry spawns in, following the precedence of {@link #getSpawnLocation()}.
	 * @param x
	 * @param y
	 * @return {@code true} if a unit of this entry could have spawned there
	 */
	public boolean isInsideTerritory(int x, int y)
	{
		if (_zone != null)
		{
			return _zone.isInsideZone(x, y);
		}
		
		if (!_group.getTerritories().isEmpty())
		{
			return isInsideAny(_group.getTerritories(), _group.getBannedTerritories(), x, y);
		}
		
		if (!_spawnTemplate.getTerritories().isEmpty())
		{
			return isInsideAny(_spawnTemplate.getTerritories(), _spawnTemplate.getBannedTerritories(), x, y);
		}
		
		return false;
	}
	
	/**
	 * @param territories the territories a unit can spawn in
	 * @param banned the holes cut out of them
	 * @param x
	 * @param y
	 * @return {@code true} if the point lies in one of the territories and in none of the holes
	 */
	private boolean isInsideAny(List<SpawnTerritory> territories, List<BannedSpawnTerritory> banned, int x, int y)
	{
		boolean inside = false;
		for (SpawnTerritory territory : territories)
		{
			if (territory.isInsideZone(x, y))
			{
				inside = true;
				break;
			}
		}
		
		if (!inside)
		{
			return false;
		}
		
		for (BannedSpawnTerritory territory : banned)
		{
			if (territory.isInsideZone(x, y))
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Retrieves the current set of parameters for this template.
	 * @return the {@code StatSet} containing the configuration data.
	 */
	@Override
	public StatSet getParameters()
	{
		return _parameters;
	}
	
	/**
	 * Updates the spawn parameters for this template.<br>
	 * If the current parameters are {@code null}, they are replaced by the new set.<br>
	 * Otherwise, the new values are merged into the existing set.
	 * @param parameters The {@code StatSet} containing the new parameters to apply.
	 */
	@Override
	public void setParameters(StatSet parameters)
	{
		if (_parameters == null)
		{
			_parameters = parameters;
		}
		else
		{
			_parameters.merge(parameters);
		}
	}
	
	/**
	 * Checks if the NPC has a specific animation when it spawns.
	 * @return {@code true} if an animation is enabled, {@code false} otherwise.
	 */
	public boolean hasSpawnAnimation()
	{
		return _spawnAnimation;
	}
	
	/**
	 * Checks if this template should be saved in the database.<br>
	 * Returns {@code true} if it is a persistent spawn.<br>
	 * Returns {@code false} otherwise.
	 * @return the save status of the template.
	 */
	public boolean hasDBSave()
	{
		return _saveInDB;
	}
	
	/**
	 * Retrieves the name of the database associated with this template.<br>
	 * This is used to identify which table contains the data.
	 * @return The {@code String} name of the database.
	 */
	public String getDBName()
	{
		return _dbName;
	}
	
	/**
	 * Retrieves the list of minions associated with this template.<br>
	 * Returns an empty {@code List} if no minions are defined.
	 * @return a {@code List} of {@code MinionHolder} objects.
	 */
	public List<MinionHolder> getMinions()
	{
		return _minions != null ? _minions : Collections.emptyList();
	}
	
	/**
	 * Adds a new minion to the list of minions.<br>
	 * This method initializes the internal list if it is {@code null}.
	 * @param minion The {@code MinionHolder} to be added.
	 */
	public void addMinion(MinionHolder minion)
	{
		if (_minions == null)
		{
			_minions = new ArrayList<>();
		}
		
		_minions.add(minion);
	}
	
	/**
	 * Retrieves all {@code Npc} entities that have been spawned from this template.
	 * @return A {@code Set} of currently active {@code Npc} objects.
	 */
	public Set<Npc> getSpawnedNpcs()
	{
		return _spawnedNpcs;
	}
	
	/**
	 * Forgets a dead unit of a rolling group.<br>
	 * The group replaces it with a spawn of its own, so the spawn and the npc of the dead unit would otherwise stay in the tables until the template is despawned.
	 * @param npc the unit that died
	 */
	public void releaseDeadUnit(Npc npc)
	{
		if (_group.getSelection() == SpawnSelection.ALL)
		{
			return;
		}
		
		_spawnedNpcs.remove(npc);
		SpawnTable.getInstance().removeSpawn(npc.getSpawn());
	}
	
	/**
	 * @return the amount of units of this entry that are alive or reserved by a pending replacement
	 */
	public int getAliveOrReserved()
	{
		return _aliveOrReserved;
	}
	
	public void setAliveOrReserved(int count)
	{
		_aliveOrReserved = count;
	}
	
	/**
	 * Tells whether the ground under a point lies outside the Z band of a territory, that is on another floor.
	 * @param x The X of the point.
	 * @param y The Y of the point.
	 * @param z The Z the point was drawn at.
	 * @param territory The {@code SpawnTerritory} that owns the point.
	 * @return {@code true} when the geodata height is below the low Z or above the high Z of the territory.
	 */
	private static boolean isOnAnotherFloor(int x, int y, int z, SpawnTerritory territory)
	{
		final int height = GeoEngine.getInstance().getHeight(x, y, z);
		return (height < territory.getLowZ()) || (height > territory.getHighZ());
	}
	
	/**
	 * Calculates and returns a random spawn location for the NPC.<br>
	 * It checks specific locations, zones, groups, or templates to find a valid point.<br>
	 * The method ensures the point is not in a banned territory.
	 * @return the calculated {@code Location} or {@code null} if no location is found.
	 */
	public Location getSpawnLocation()
	{
		if (_locations != null)
		{
			final double locRandom = (100 * Rnd.nextDouble());
			float cumulativeChance = 0;
			for (ChanceLocation loc : _locations)
			{
				if (locRandom <= (cumulativeChance += loc.getChance()))
				{
					return loc;
				}
			}
			
			LOGGER.warning("Couldn't match location by chance turning first...");
			return null;
		}
		else if (_zone != null)
		{
			int count = 0; // Prevent infinite loop.
			Location location;
			int randomX;
			int randomY;
			int randomZ;
			
			while (count++ < 100)
			{
				location = _zone.getRandomPoint();
				randomX = location.getX();
				randomY = location.getY();
				randomZ = location.getZ();
				
				if (isOnAnotherFloor(randomX, randomY, randomZ, _zone))
				{
					continue;
				}
				
				location.setHeading(-1);
				return location;
			}
			
			location = _zone.getRandomPoint();
			location.setHeading(-1);
			return location;
		}
		else if (!_group.getTerritories().isEmpty())
		{
			final List<SpawnTerritory> groupTerritories = _group.getTerritories();
			final SpawnTerritory territory = groupTerritories.get(Rnd.get(groupTerritories.size()));
			int count = 0; // Prevent infinite loop.
			Location location;
			int randomX;
			int randomY;
			int randomZ;
			
			final List<BannedSpawnTerritory> bannedTerritories = _group.getBannedTerritories();
			if (!bannedTerritories.isEmpty())
			{
				SEARCH: while (count++ < 100)
				{
					location = territory.getRandomPoint();
					randomX = location.getX();
					randomY = location.getY();
					randomZ = location.getZ();
					
					for (BannedSpawnTerritory banned : bannedTerritories)
					{
						if (banned.isInsideZone(randomX, randomY, randomZ))
						{
							continue SEARCH;
						}
					}
					
					if (isOnAnotherFloor(randomX, randomY, randomZ, territory))
					{
						continue;
					}
					
					location.setHeading(-1);
					return location;
				}
				
				count = 0;
				SEARCH_NO_GEO: while (count++ < 100)
				{
					location = territory.getRandomPoint();
					randomX = location.getX();
					randomY = location.getY();
					randomZ = location.getZ();
					
					for (BannedSpawnTerritory banned : bannedTerritories)
					{
						if (banned.isInsideZone(randomX, randomY, randomZ))
						{
							continue SEARCH_NO_GEO;
						}
					}
					
					location.setHeading(-1);
					return location;
				}
			}
			
			count = 0;
			while (count++ < 100)
			{
				location = territory.getRandomPoint();
				randomX = location.getX();
				randomY = location.getY();
				randomZ = location.getZ();
				
				if (isOnAnotherFloor(randomX, randomY, randomZ, territory))
				{
					continue;
				}
				
				location.setHeading(-1);
				return location;
			}
			
			location = territory.getRandomPoint();
			location.setHeading(-1);
			return location;
		}
		else if (!_spawnTemplate.getTerritories().isEmpty())
		{
			final List<SpawnTerritory> spawnTemplateTerritories = _spawnTemplate.getTerritories();
			final SpawnTerritory territory = spawnTemplateTerritories.get(Rnd.get(spawnTemplateTerritories.size()));
			int count = 0; // Prevent infinite loop.
			Location location;
			int randomX;
			int randomY;
			int randomZ;
			
			final List<BannedSpawnTerritory> bannedTerritories = _spawnTemplate.getBannedTerritories();
			if (!bannedTerritories.isEmpty())
			{
				SEARCH: while (count++ < 100)
				{
					location = territory.getRandomPoint();
					randomX = location.getX();
					randomY = location.getY();
					randomZ = location.getZ();
					
					for (BannedSpawnTerritory banned : bannedTerritories)
					{
						if (banned.isInsideZone(randomX, randomY, randomZ))
						{
							continue SEARCH;
						}
					}
					
					if (isOnAnotherFloor(randomX, randomY, randomZ, territory))
					{
						continue;
					}
					
					location.setHeading(-1);
					return location;
				}
				
				count = 0;
				SEARCH_NO_GEO: while (count++ < 100)
				{
					location = territory.getRandomPoint();
					randomX = location.getX();
					randomY = location.getY();
					randomZ = location.getZ();
					
					for (BannedSpawnTerritory banned : bannedTerritories)
					{
						if (banned.isInsideZone(randomX, randomY, randomZ))
						{
							continue SEARCH_NO_GEO;
						}
					}
					
					location.setHeading(-1);
					return location;
				}
			}
			
			count = 0;
			while (count++ < 100)
			{
				location = territory.getRandomPoint();
				randomX = location.getX();
				randomY = location.getY();
				randomZ = location.getZ();
				
				if (isOnAnotherFloor(randomX, randomY, randomZ, territory))
				{
					continue;
				}
				
				location.setHeading(-1);
				return location;
			}
			
			location = territory.getRandomPoint();
			location.setHeading(-1);
			return location;
		}
		
		return null;
	}
	
	/**
	 * Spawns the NPC into the world.<br>
	 * This method calls {@code spawn(null)} to initiate the process.
	 */
	public void spawn()
	{
		spawn(null);
	}
	
	/**
	 * Spawns the NPCs defined by this template into a specific instance.<br>
	 * It validates that the NPC template exists and is not of type Defender.<br>
	 * The method spawns the specified number of NPCs based on the count.
	 * @param instance The {@code Instance} where the NPCs will be spawned.
	 */
	public void spawn(Instance instance)
	{
		try
		{
			final NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(_id);
			if (npcTemplate == null)
			{
				LOGGER.warning("Attempting to spawn unexisting npc id: " + _id + " file: " + _spawnTemplate.getFile().getName() + " spawn: " + _spawnTemplate.getName() + " group: " + _group.getName());
				return;
			}
			
			if (npcTemplate.isType("Defender"))
			{
				LOGGER.warning("Attempting to spawn npc id: " + _id + " type: " + npcTemplate.getType() + " file: " + _spawnTemplate.getFile().getName() + " spawn: " + _spawnTemplate.getName() + " group: " + _group.getName());
				return;
			}
			
			for (int i = 0; i < _count; i++)
			{
				spawnNpc(npcTemplate, instance);
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("Couldn't spawn npc " + _id + e);
		}
	}
	
	/**
	 * Creates and initializes a new NPC based on the provided template.<br>
	 * This method handles location setup, respawn logic, and database registration.<br>
	 * It also manages spawning minions if they are defined in the template.
	 * @param npcTemplate The {@code NpcTemplate} used to define the NPC properties.
	 * @param instance The {@code Instance} where the NPC will be spawned, or {@code null}.
	 * @return {@code true} when the NPC was created.
	 * @throws ClassNotFoundException If the NPC class cannot be loaded.
	 * @throws NoSuchMethodException If the required initialization method is missing.
	 * @throws ClassCastException If the spawned object is of an incorrect type.
	 */
	private boolean spawnNpc(NpcTemplate npcTemplate, Instance instance) throws ClassNotFoundException, NoSuchMethodException, ClassCastException
	{
		final Spawn spawn = new Spawn(npcTemplate);
		final Location loc = getSpawnLocation();
		if (loc == null)
		{
			LOGGER.warning("Couldn't initialize new spawn, no location found!");
			return false;
		}
		
		spawn.setInstanceId(instance != null ? instance.getId() : 0);
		spawn.setAmount(1);
		spawn.setXYZ(loc);
		spawn.setHeading(loc.getHeading());
		spawn.setLocation(loc);
		int respawn = 0;
		int respawnRandom = 0;
		SchedulingPattern respawnPattern = null;
		if (_respawnTime != null)
		{
			respawn = (int) _respawnTime.getSeconds();
		}
		
		if (_respawnTimeRandom != null)
		{
			respawnRandom = (int) _respawnTimeRandom.getSeconds();
		}
		
		if (_respawnPattern != null)
		{
			respawnPattern = _respawnPattern;
		}
		
		if (_group.getSelection() != SpawnSelection.ALL)
		{
			// A rolling group replaces its own units, the slot must not bring its own species back.
			spawn.setRespawnDelay(respawn, respawnRandom);
			spawn.stopRespawn();
		}
		else if ((respawn > 0) || (respawnPattern != null))
		{
			spawn.setRespawnDelay(respawn, respawnRandom);
			spawn.setRespawnPattern(respawnPattern);
			spawn.startRespawn();
		}
		else
		{
			spawn.stopRespawn();
		}
		
		spawn.setSpawnTemplate(this);
		
		if (_saveInDB)
		{
			if (!DatabaseSpawnManager.getInstance().isDefined(_id))
			{
				final Npc spawnedNpc = DatabaseSpawnManager.getInstance().addNewSpawn(spawn, true);
				if ((spawnedNpc != null) && spawnedNpc.isMonster() && (_minions != null))
				{
					spawnedNpc.asMonster().getMinionList().spawnMinions(_minions);
				}
				
				_spawnedNpcs.add(spawnedNpc);
			}
		}
		else
		{
			final Npc npc = spawn.doSpawn(_spawnAnimation);
			if (npc.isMonster() && (_minions != null))
			{
				npc.asMonster().getMinionList().spawnMinions(_minions);
			}
			
			_spawnedNpcs.add(npc);
			
			SpawnTable.getInstance().addSpawn(spawn);
		}
		
		return true;
	}
	
	/**
	 * Spawns a single unit of this entry, used by the entry selection of the owning group.
	 * @param instance the instance to spawn in, {@code null} in the open world
	 * @return {@code true} when the unit was spawned
	 */
	public boolean spawnOne(Instance instance)
	{
		try
		{
			final NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(_id);
			if (npcTemplate == null)
			{
				LOGGER.warning("Attempting to spawn unexisting npc id: " + _id + " file: " + _spawnTemplate.getFile().getName() + " spawn: " + _spawnTemplate.getName() + " group: " + _group.getName());
				return false;
			}
			
			if (npcTemplate.isType("Defender"))
			{
				LOGGER.warning("Attempting to spawn npc id: " + _id + " type: " + npcTemplate.getType() + " file: " + _spawnTemplate.getFile().getName() + " spawn: " + _spawnTemplate.getName() + " group: " + _group.getName());
				return false;
			}
			
			return spawnNpc(npcTemplate, instance);
		}
		catch (Exception e)
		{
			LOGGER.warning("Couldn't spawn npc " + _id + e);
			return false;
		}
	}
	
	/**
	 * Removes all NPCs spawned from this template.<br>
	 * This method stops the respawn cycle for each NPC.<br>
	 * It also removes them from the {@code SpawnTable} and deletes them from the world.
	 */
	public void despawn()
	{
		_aliveOrReserved = 0;
		_spawnedNpcs.forEach(npc ->
		{
			final Spawn npcSpawn = npc.getSpawn();
			npcSpawn.stopRespawn();
			SpawnTable.getInstance().removeSpawn(npcSpawn);
			npc.deleteMe();
		});
		_spawnedNpcs.clear();
	}
	
	/**
	 * Notifies the system that a new {@code Npc} has been spawned.<br>
	 * This method triggers events related to the spawning of an entity.
	 * @param npc The {@code Npc} instance that was just created.
	 */
	public void notifySpawnNpc(Npc npc)
	{
		_spawnTemplate.notifyEvent(event -> event.onSpawnNpc(_spawnTemplate, _group, npc));
	}
	
	/**
	 * Notifies the system that an {@code Npc} has despawned.<br>
	 * This method triggers events related to the removal of a spawned entity.
	 * @param npc The {@code Npc} instance that is being removed.
	 */
	public void notifyDespawnNpc(Npc npc)
	{
		_spawnTemplate.notifyEvent(event -> event.onSpawnDespawnNpc(_spawnTemplate, _group, npc));
	}
	
	/**
	 * Notifies the system that an {@code Npc} has died.<br>
	 * This method triggers events related to the death of a spawned NPC.<br>
	 * It records which {@code Creature} was responsible for the kill.
	 * @param npc The {@code Npc} instance that died.
	 * @param killer The {@code Creature} that killed the NPC.
	 */
	public void notifyNpcDeath(Npc npc, Creature killer)
	{
		_spawnTemplate.notifyEvent(event -> event.onSpawnNpcDeath(_spawnTemplate, _group, npc, killer));
	}
	
	/**
	 * Retrieves the maximum height of the current zone.<br>
	 * If no zone is assigned, it returns {@code Integer.MAX_VALUE}.
	 * @return The high Z coordinate as an {@code int}.
	 */
	public int getHighZ()
	{
		return _zone != null ? _zone.getHighZ() : Integer.MAX_VALUE;
	}
	
	/**
	 * Creates a copy of the current {@code NpcSpawnTemplate}.<br>
	 * This method returns a new instance with the same properties.
	 * @return A new {@code NpcSpawnTemplate} object.
	 */
	@Override
	public NpcSpawnTemplate clone()
	{
		return new NpcSpawnTemplate(this, _group);
	}
	
	/**
	 * Clones this entry onto another group, used when a group is cloned for an instance.
	 * @param group the group the clone belongs to
	 * @return the cloned entry
	 */
	public NpcSpawnTemplate clone(SpawnGroup group)
	{
		return new NpcSpawnTemplate(this, group);
	}
}
