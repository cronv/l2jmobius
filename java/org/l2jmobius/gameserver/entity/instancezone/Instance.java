/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.entity.instancezone;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.actor.templates.DoorTemplate;
import org.l2jmobius.gameserver.entity.spawns.NpcSpawnTemplate;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.spawns.SpawnGroup;
import org.l2jmobius.gameserver.entity.spawns.SpawnTemplate;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.instance.OnInstanceCreated;
import org.l2jmobius.gameserver.mechanics.events.holders.instance.OnInstanceDestroy;
import org.l2jmobius.gameserver.mechanics.events.holders.instance.OnInstanceEnter;
import org.l2jmobius.gameserver.mechanics.events.holders.instance.OnInstanceLeave;
import org.l2jmobius.gameserver.mechanics.events.holders.instance.OnInstanceStatusChange;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.ArrayUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * A private instance world holding its players, NPCs, doors, spawns and lifecycle timers.
 * @author malyelfik
 */
public class Instance
{
	private static final Logger LOGGER = Logger.getLogger(Instance.class.getName());
	
	// Basic instance parameters
	private final int _id;
	private final InstanceTemplate _template;
	private final long _startTime;
	private long _endTime;
	
	// Advanced instance parameters
	private final Set<Integer> _allowed = ConcurrentHashMap.newKeySet(); // Player ids which can enter to instance.
	private final Set<Player> _players = ConcurrentHashMap.newKeySet(); // Players inside instance
	private final Set<Npc> _npcs = ConcurrentHashMap.newKeySet(); // Spawned NPCs inside instance
	private final Map<Integer, Door> _doors = new HashMap<>(); // Spawned doors inside instance
	private final StatSet _parameters = new StatSet();
	
	// Timers
	private final Map<Integer, ScheduledFuture<?>> _ejectDeadTasks = new ConcurrentHashMap<>();
	private ScheduledFuture<?> _cleanUpTask = null;
	private ScheduledFuture<?> _emptyDestroyTask = null;
	private final List<SpawnTemplate> _spawns;
	
	/**
	 * Creates an instance from a template, registers it with the {@code InstanceManager}, spawns doors and default entities, and notifies scripts.
	 * @param id the unique identifier for this instance
	 * @param template the {@code InstanceTemplate} used to configure the instance
	 * @param player the {@code Player} who triggered the creation
	 */
	public Instance(int id, InstanceTemplate template, Player player)
	{
		// Set basic instance info.
		_id = id;
		_template = template;
		_startTime = System.currentTimeMillis();
		
		final List<SpawnTemplate> templateSpawns = template.getSpawns();
		_spawns = new ArrayList<>(templateSpawns.size());
		
		// Clone and add the spawn templates.
		for (SpawnTemplate spawn : templateSpawns)
		{
			_spawns.add(spawn.clone());
		}
		
		// Register world to instance manager.
		InstanceManager.getInstance().register(this);
		
		// Set duration, spawns, status, etc..
		setDuration(_template.getDuration());
		setStatus(0);
		spawnDoors();
		
		// Initialize instance spawns.
		for (SpawnTemplate spawnTemplate : _spawns)
		{
			if (spawnTemplate.isSpawningByDefault())
			{
				spawnTemplate.spawnAll(this);
			}
		}
		
		// Notify DP scripts
		if (!isDynamic() && EventDispatcher.getInstance().hasListener(EventType.ON_INSTANCE_CREATED, _template))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnInstanceCreated(this, player), _template);
		}
	}
	
	/**
	 * Retrieves the unique id of this instance.
	 * @return the {@code int} id of the instance
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the display name of this instance from its template.
	 * @return the name as a {@code String}
	 */
	public String getName()
	{
		return _template.getName();
	}
	
	/**
	 * Indicates whether this instance is dynamic (template id is {@code -1}).
	 * @return {@code true} if dynamic, {@code false} otherwise
	 */
	public boolean isDynamic()
	{
		return _template.getId() == -1;
	}
	
	/**
	 * Sets or removes a parameter, removing the key when {@code value} is {@code null}.
	 * @param key the parameter name
	 * @param value the data to store, or {@code null} to delete the entry
	 */
	public void setParameter(String key, Object value)
	{
		if (value == null)
		{
			_parameters.remove(key);
		}
		else
		{
			_parameters.set(key, value);
		}
	}
	
	/**
	 * Sets a {@code boolean} parameter for this instance.
	 * @param key the parameter name
	 * @param value the {@code boolean} value to assign
	 */
	public void setParameter(String key, boolean value)
	{
		_parameters.set(key, Boolean.valueOf(value));
	}
	
	/**
	 * Retrieves the parameters of this instance.
	 * @return a {@code StatSet} of all parameters
	 */
	public StatSet getParameters()
	{
		return _parameters;
	}
	
	/**
	 * Retrieves the current status, returning {@code 0} when the status key is absent.
	 * @return the status as an {@code int}
	 */
	public int getStatus()
	{
		return _parameters.getInt("INSTANCE_STATUS", 0);
	}
	
	/**
	 * Checks whether the current status matches the given value.
	 * @param status the status to check against
	 * @return {@code true} if the status matches, {@code false} otherwise
	 */
	public boolean isStatus(int status)
	{
		return getStatus() == status;
	}
	
	/**
	 * Sets the status parameter and notifies status-change listeners.
	 * @param value the new status value
	 */
	public void setStatus(int value)
	{
		_parameters.set("INSTANCE_STATUS", value);
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_INSTANCE_STATUS_CHANGE, _template))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnInstanceStatusChange(this, value), _template);
		}
	}
	
	/**
	 * Increments the current status by {@code 1} and applies it.
	 * @return the updated status as an {@code int}
	 */
	public int incStatus()
	{
		final int status = getStatus() + 1;
		setStatus(status);
		return status;
	}
	
	/**
	 * Adds a player to the allowed list if not already present.
	 * @param player the {@code Player} to add
	 */
	public void addAllowed(Player player)
	{
		final Integer playerId = player.getObjectId();
		if (!_allowed.contains(playerId))
		{
			_allowed.add(playerId);
		}
	}
	
	/**
	 * Checks whether a player is allowed in this instance.
	 * @param player the {@code Player} to check
	 * @return {@code true} if allowed, {@code false} otherwise
	 */
	public boolean isAllowed(Player player)
	{
		return _allowed.contains(player.getObjectId());
	}
	
	/**
	 * Retrieves all allowed players still active in the world.
	 * @return a {@code List} of valid {@code Player} instances
	 */
	public List<Player> getAllowed()
	{
		final List<Player> allowed = new ArrayList<>(_allowed.size());
		for (int playerId : _allowed)
		{
			final Player player = World.getPlayer(playerId);
			if (player != null)
			{
				allowed.add(player);
			}
		}
		
		return allowed;
	}
	
	/**
	 * Adds a player to this instance and cancels any pending empty-destroy task.
	 * @param player the {@code Player} to add
	 */
	public void addPlayer(Player player)
	{
		_players.add(player);
		if (_emptyDestroyTask != null)
		{
			_emptyDestroyTask.cancel(false);
			_emptyDestroyTask = null;
		}
	}
	
	/**
	 * Removes a player and, when none remain, destroys or schedules destruction of the instance.
	 * @param player the {@code Player} to remove
	 */
	public void removePlayer(Player player)
	{
		_players.remove(player);
		if (_players.isEmpty())
		{
			final long emptyTime = _template.getEmptyDestroyTime();
			if ((_template.getDuration() == 0) || (emptyTime == 0))
			{
				destroy();
			}
			else if ((emptyTime >= 0) && (_emptyDestroyTask == null))
			{
				_emptyDestroyTask = ThreadPool.schedule(this::destroy, emptyTime);
			}
		}
	}
	
	/**
	 * Checks whether a player is currently in this instance.
	 * @param player the {@code Player} to check
	 * @return {@code true} if present, {@code false} otherwise
	 */
	public boolean containsPlayer(Player player)
	{
		return _players.contains(player);
	}
	
	/**
	 * Retrieves all players currently in this instance.
	 * @return a {@code Set} of {@code Player}
	 */
	public Set<Player> getPlayers()
	{
		return _players;
	}
	
	/**
	 * Retrieves the number of players currently in this instance.
	 * @return the player count as an {@code int}
	 */
	public int getPlayersCount()
	{
		return _players.size();
	}
	
	/**
	 * Retrieves the first player in this instance, or {@code null} if none.
	 * @return the first {@code Player}, or {@code null}
	 */
	public Player getFirstPlayer()
	{
		for (Player player : _players)
		{
			return player;
		}
		
		return null;
	}
	
	/**
	 * Finds a player in this instance by object id.
	 * @param id the player object id
	 * @return the matching {@code Player}, or {@code null} if not found
	 */
	public Player getPlayerById(int id)
	{
		for (Player player : _players)
		{
			if (player.getObjectId() == id)
			{
				return player;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves all players within a 3D radius of the given center.
	 * @param object the {@code ILocational} center
	 * @param radius the distance from the center
	 * @return a {@code List} of {@code Player} within the radius
	 */
	public List<Player> getPlayersInsideRadius(ILocational object, int radius)
	{
		final List<Player> result = new ArrayList<>();
		for (Player player : _players)
		{
			if (player.isInsideRadius3D(object, radius))
			{
				result.add(player);
			}
		}
		
		return result;
	}
	
	/**
	 * Spawns every door defined in the template and registers it.
	 */
	private void spawnDoors()
	{
		for (DoorTemplate template : _template.getDoors().values())
		{
			// Create new door instance.
			_doors.put(template.getId(), DoorData.getInstance().spawnDoor(template, this));
		}
	}
	
	/**
	 * Retrieves all doors of this instance.
	 * @return a {@code Collection} of {@code Door}
	 */
	public Collection<Door> getDoors()
	{
		return _doors.values();
	}
	
	/**
	 * Retrieves a door by its id.
	 * @param id the door id
	 * @return the matching {@code Door}, or {@code null} if not found
	 */
	public Door getDoor(int id)
	{
		return _doors.get(id);
	}
	
	/**
	 * Opens or closes the door with the given id, doing nothing when it is not found.
	 * @param id the door id
	 * @param open {@code true} to open, {@code false} to close
	 */
	public void openCloseDoor(int id, boolean open)
	{
		final Door door = _doors.get(id);
		if (door != null)
		{
			if (open)
			{
				if (!door.isOpen())
				{
					door.openMe();
				}
			}
			else if (door.isOpen())
			{
				door.closeMe();
			}
		}
	}
	
	/**
	 * Checks whether a spawn group with the given name exists, case-insensitively.
	 * @param name the spawn group name
	 * @return {@code true} if found, {@code false} otherwise
	 */
	public boolean isSpawnGroupExist(String name)
	{
		for (SpawnTemplate spawnTemplate : _spawns)
		{
			for (SpawnGroup group : spawnTemplate.getGroups())
			{
				if (name.equalsIgnoreCase(group.getName()))
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Retrieves all spawn groups matching the given name.
	 * @param name the spawn group name
	 * @return a {@code List} of matching {@code SpawnGroup}, or empty if none
	 */
	public List<SpawnGroup> getSpawnGroup(String name)
	{
		final List<SpawnGroup> spawns = new ArrayList<>();
		for (SpawnTemplate spawnTemplate : _spawns)
		{
			spawns.addAll(spawnTemplate.getGroupsByName(name));
		}
		
		return spawns;
	}
	
	/**
	 * Retrieves all NPCs of the named spawn group.
	 * @param name the spawn group name
	 * @return a {@code List} of {@code Npc} in that group
	 */
	public List<Npc> getNpcsOfGroup(String name)
	{
		return getNpcsOfGroup(name, null);
	}
	
	/**
	 * Retrieves the NPCs of the named spawn group matching the filter.
	 * @param groupName the spawn group name
	 * @param filterValue the {@code Predicate} filter; when {@code null}, all non-null NPCs are returned
	 * @return a {@code List} of matching {@code Npc}
	 */
	public List<Npc> getNpcsOfGroup(String groupName, Predicate<Npc> filterValue)
	{
		Predicate<Npc> filter = filterValue;
		if (filter == null)
		{
			filter = Objects::nonNull;
		}
		
		final List<Npc> npcs = new ArrayList<>();
		for (SpawnTemplate spawnTemplate : _spawns)
		{
			for (SpawnGroup group : spawnTemplate.getGroupsByName(groupName))
			{
				for (NpcSpawnTemplate npcTemplate : group.getSpawns())
				{
					for (Npc npc : npcTemplate.getSpawnedNpcs())
					{
						if (filter.test(npc))
						{
							npcs.add(npc);
						}
					}
				}
			}
		}
		
		return npcs;
	}
	
	/**
	 * Retrieves the first NPC of the named spawn group matching the filter.
	 * @param groupName the spawn group name
	 * @param filterValue the {@code Predicate} filter; when {@code null}, defaults to a non-null check
	 * @return the first matching {@code Npc}, or {@code null} if none
	 */
	public Npc getNpcOfGroup(String groupName, Predicate<Npc> filterValue)
	{
		Predicate<Npc> filter = filterValue;
		if (filter == null)
		{
			filter = Objects::nonNull;
		}
		
		for (SpawnTemplate spawnTemplate : _spawns)
		{
			for (SpawnGroup group : spawnTemplate.getGroupsByName(groupName))
			{
				for (NpcSpawnTemplate npcTemplate : group.getSpawns())
				{
					for (Npc npc : npcTemplate.getSpawnedNpcs())
					{
						if (filter.test(npc))
						{
							return npc;
						}
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Spawns every NPC of the named spawn group, returning empty when the group is absent.
	 * @param name the spawn group name
	 * @return a {@code List} of spawned {@code Npc}
	 */
	public List<Npc> spawnGroup(String name)
	{
		final List<SpawnGroup> spawns = getSpawnGroup(name);
		if (spawns == null)
		{
			LOGGER.warning("Spawn group " + name + " doesn't exist for instance " + _template.getName() + " (" + _id + ")!");
			return Collections.emptyList();
		}
		
		final List<Npc> npcs = new ArrayList<>();
		try
		{
			for (SpawnGroup holder : spawns)
			{
				holder.spawnAll(this);
				holder.getSpawns().forEach(spawn -> npcs.addAll(spawn.getSpawnedNpcs()));
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("Unable to spawn group " + name + " inside instance " + _template.getName() + " (" + _id + ")");
		}
		
		return npcs;
	}
	
	/**
	 * Despawns every entity of the named spawn group, logging a warning when it does not exist.
	 * @param name the spawn group name
	 */
	public void despawnGroup(String name)
	{
		final List<SpawnGroup> spawns = getSpawnGroup(name);
		if (spawns == null)
		{
			LOGGER.warning("Spawn group " + name + " doesn't exist for instance " + _template.getName() + " (" + _id + ")!");
			return;
		}
		
		try
		{
			spawns.forEach(SpawnGroup::despawnAll);
		}
		catch (Exception e)
		{
			LOGGER.warning("Unable to spawn group " + name + " inside instance " + _template.getName() + " (" + _id + ")");
		}
	}
	
	/**
	 * Retrieves all NPCs currently in this instance.
	 * @return a {@code Set} of {@code Npc}
	 */
	public Set<Npc> getNpcs()
	{
		return _npcs;
	}
	
	/**
	 * Retrieves all NPCs whose id matches one of the given ids.
	 * @param id the ids to search for
	 * @return a {@code List} of matching {@code Npc}
	 */
	public List<Npc> getNpcs(int... id)
	{
		final List<Npc> result = new ArrayList<>();
		for (Npc npc : _npcs)
		{
			if (ArrayUtil.contains(id, npc.getId()))
			{
				result.add(npc);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves all NPCs of the given type, optionally filtered by id.
	 * @param <T> the {@code Creature} subtype
	 * @param clazz the class to filter by
	 * @param ids the ids to retrieve, or none for all of the type
	 * @return a {@code List} of matching NPCs
	 */
	@SafeVarargs
	@SuppressWarnings("unchecked")
	public final <T extends Creature> List<T> getNpcs(Class<T> clazz, int... ids)
	{
		final List<T> result = new ArrayList<>();
		for (Npc npc : _npcs)
		{
			if (((ids.length == 0) || ArrayUtil.contains(ids, npc.getId())) && clazz.isInstance(npc))
			{
				result.add((T) npc);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves all NPCs whose current HP is above {@code 0}.
	 * @return a {@code List} of living {@code Npc}
	 */
	public List<Npc> getAliveNpcs()
	{
		final List<Npc> result = new ArrayList<>();
		for (Npc npc : _npcs)
		{
			if (npc.getCurrentHp() > 0)
			{
				result.add(npc);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves all living NPCs whose id matches one of the given ids.
	 * @param id the ids to filter by
	 * @return a {@code List} of matching living {@code Npc}
	 */
	public List<Npc> getAliveNpcs(int... id)
	{
		final List<Npc> result = new ArrayList<>();
		for (Npc npc : _npcs)
		{
			if ((npc.getCurrentHp() > 0) && ArrayUtil.contains(id, npc.getId()))
			{
				result.add(npc);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves all living NPCs of the given type, optionally filtered by id.
	 * @param <T> the {@code Creature} subtype
	 * @param clazz the class to filter by
	 * @param ids optional ids to include
	 * @return a {@code List} of matching living NPCs
	 */
	@SafeVarargs
	@SuppressWarnings("unchecked")
	public final <T extends Creature> List<T> getAliveNpcs(Class<T> clazz, int... ids)
	{
		final List<T> result = new ArrayList<>();
		for (Npc npc : _npcs)
		{
			if ((((ids.length == 0) || ArrayUtil.contains(ids, npc.getId())) && (npc.getCurrentHp() > 0)) && clazz.isInstance(npc))
			{
				result.add((T) npc);
			}
		}
		
		return result;
	}
	
	/**
	 * Counts the NPCs whose current HP is above {@code 0}.
	 * @return the count of living NPCs
	 */
	public int getAliveNpcCount()
	{
		int count = 0;
		for (Npc npc : _npcs)
		{
			if (npc.getCurrentHp() > 0)
			{
				count++;
			}
		}
		
		return count;
	}
	
	/**
	 * Counts the living NPCs whose id matches one of the given ids.
	 * @param id the ids to check
	 * @return the count of matching living NPCs
	 */
	public int getAliveNpcCount(int... id)
	{
		int count = 0;
		for (Npc npc : _npcs)
		{
			if ((npc.getCurrentHp() > 0) && ArrayUtil.contains(id, npc.getId()))
			{
				count++;
			}
		}
		
		return count;
	}
	
	/**
	 * Retrieves an NPC in this instance by its id.
	 * @param id the NPC object id
	 * @return the matching {@code Npc}, or {@code null} if not found
	 */
	public Npc getNpc(int id)
	{
		for (Npc npc : _npcs)
		{
			if (npc.getId() == id)
			{
				return npc;
			}
		}
		
		return null;
	}
	
	/**
	 * Adds an NPC to this instance.
	 * @param npc the {@code Npc} to add
	 */
	public void addNpc(Npc npc)
	{
		_npcs.add(npc);
	}
	
	/**
	 * Removes an NPC from this instance.
	 * @param npc the {@code Npc} to remove
	 */
	public void removeNpc(Npc npc)
	{
		_npcs.remove(npc);
	}
	
	/**
	 * Ejects every player and clears the player collection.
	 */
	private void removePlayers()
	{
		_players.forEach(this::ejectPlayer);
		_players.clear();
	}
	
	/**
	 * Decays every door and clears the door collection.
	 */
	private void removeDoors()
	{
		for (Door door : _doors.values())
		{
			if (door != null)
			{
				door.decayMe();
			}
		}
		
		_doors.clear();
	}
	
	/**
	 * Despawns all spawn templates, stops every NPC respawn, deletes the NPCs and clears the collection.
	 */
	public void removeNpcs()
	{
		_spawns.forEach(SpawnTemplate::despawnAll);
		for (Npc npc : _npcs)
		{
			// A script built spawn is not one of the templates, so deleting the npc would only have it come back.
			final Spawn spawn = npc.getSpawn();
			if (spawn != null)
			{
				spawn.stopRespawn();
			}
			
			npc.deleteMe();
		}
		
		_npcs.clear();
	}
	
	/**
	 * Sets the remaining lifetime, scheduling cleanup or destroying immediately; a negative value never ends.
	 * @param minutes the minutes before the instance is destroyed
	 */
	public void setDuration(int minutes)
	{
		// Instance never ends
		if (minutes < 0)
		{
			_endTime = -1;
			return;
		}
		
		// Stop running tasks
		final long millis = TimeUnit.MINUTES.toMillis(minutes);
		if (_cleanUpTask != null)
		{
			_cleanUpTask.cancel(true);
			_cleanUpTask = null;
		}
		
		if ((_emptyDestroyTask != null) && (millis < _emptyDestroyTask.getDelay(TimeUnit.MILLISECONDS)))
		{
			_emptyDestroyTask.cancel(true);
			_emptyDestroyTask = null;
		}
		
		// Set new cleanup task.
		_endTime = System.currentTimeMillis() + millis;
		if (minutes < 1) // Destroy instance
		{
			destroy();
		}
		else
		{
			sendWorldDestroyMessage(minutes);
			if (minutes <= 5) // Message 1 minute before destroy
			{
				_cleanUpTask = ThreadPool.schedule(this::cleanUp, millis - 60000);
			}
			else // Message 5 minutes before destroy
			{
				_cleanUpTask = ThreadPool.schedule(this::cleanUp, millis - (5 * 60000));
			}
		}
	}
	
	/**
	 * Shuts down the instance, cancelling tasks, removing players, doors and NPCs, and unregistering it.
	 */
	public synchronized void destroy()
	{
		if (_cleanUpTask != null)
		{
			_cleanUpTask.cancel(false);
			_cleanUpTask = null;
		}
		
		if (_emptyDestroyTask != null)
		{
			_emptyDestroyTask.cancel(false);
			_emptyDestroyTask = null;
		}
		
		_ejectDeadTasks.values().forEach(t -> t.cancel(true));
		_ejectDeadTasks.clear();
		
		// Notify DP scripts
		if (!isDynamic() && EventDispatcher.getInstance().hasListener(EventType.ON_INSTANCE_DESTROY, _template))
		{
			EventDispatcher.getInstance().notifyEvent(new OnInstanceDestroy(this), _template);
		}
		
		// Set reenter time when empty time is positive and reenter type is ON_FINISH.
		if ((_template.getEmptyDestroyTime() > 0) && (_template.getReenterType() == InstanceReenterType.ON_FINISH))
		{
			setReenterTime();
		}
		
		removePlayers();
		removeDoors();
		removeNpcs();
		
		InstanceManager.getInstance().unregister(getId());
	}
	
	/**
	 * Ejects a player to the template exit location, or to a town when none is defined.
	 * @param player the {@code Player} to remove
	 */
	public void ejectPlayer(Player player)
	{
		final Instance world = player.getInstanceWorld();
		if ((world != null) && world.equals(this))
		{
			final Location loc = _template.getExitLocation(player);
			if (loc != null)
			{
				player.teleToLocation(loc, null);
			}
			else
			{
				player.teleToLocation(TeleportWhereType.TOWN, null);
			}
		}
	}
	
	/**
	 * Sends the given packets to every player in this instance.
	 * @param packets the {@code ServerPacket} objects to send
	 */
	public void broadcastPacket(ServerPacket... packets)
	{
		for (Player player : _players)
		{
			for (ServerPacket packet : packets)
			{
				player.sendPacket(packet);
			}
		}
	}
	
	/**
	 * Retrieves the creation time of this instance.
	 * @return the start time as a {@code long}
	 */
	public long getStartTime()
	{
		return _startTime;
	}
	
	/**
	 * Retrieves the time elapsed since this instance was created.
	 * @return the elapsed time in milliseconds
	 */
	public long getElapsedTime()
	{
		return System.currentTimeMillis() - _startTime;
	}
	
	/**
	 * Retrieves the time remaining until expiry, or {@code -1} when it has no end time.
	 * @return the remaining time in milliseconds, or {@code -1}
	 */
	public long getRemainingTime()
	{
		return (_endTime == -1) ? -1 : (_endTime - System.currentTimeMillis());
	}
	
	/**
	 * Retrieves the end time of this instance.
	 * @return the end time as a {@code long}
	 */
	public long getEndTime()
	{
		return _endTime;
	}
	
	/**
	 * Sets the re-entry time using the value from the template.
	 */
	public void setReenterTime()
	{
		setReenterTime(_template.calculateReenterTime());
	}
	
	/**
	 * Persists and applies the re-entry restriction and notifies allowed players.
	 * @param time the timestamp when re-entry is allowed
	 */
	public void setReenterTime(long time)
	{
		// Cannot store reenter data for instance without template id.
		final int templateId = _template.getId();
		if ((templateId == -1) && (time > 0))
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT IGNORE INTO character_instance_time (charId,instanceId,time) VALUES (?,?,?)"))
		{
			// Save to database
			for (Integer playerId : _allowed)
			{
				ps.setInt(1, playerId);
				ps.setInt(2, templateId);
				ps.setLong(3, time);
				ps.addBatch();
			}
			
			ps.executeBatch();
			
			// Save to memory and send message to player.
			final SystemMessage msg = new SystemMessage(SystemMessageId.INSTANCE_ZONE_S1_S_ENTRY_HAS_BEEN_RESTRICTED_YOU_CAN_CHECK_THE_NEXT_POSSIBLE_ENTRY_TIME_WITH_INSTANCEZONE);
			final int templateIdLocal = getTemplateId();
			if (InstanceManager.getInstance().getInstanceName(templateIdLocal) != null)
			{
				msg.addInstanceName(templateId);
			}
			else
			{
				msg.addString(_template.getName());
			}
			
			_allowed.forEach(playerId ->
			{
				InstanceManager.getInstance().setReenterPenalty(playerId, templateIdLocal, time);
				final Player player = World.getPlayer(playerId);
				if ((player != null) && player.isOnline())
				{
					player.sendPacket(msg);
				}
			});
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not insert character instance reenter data: ", e);
		}
	}
	
	/**
	 * Finishes the instance after the configured delay.
	 */
	public void finishInstance()
	{
		finishInstance(GeneralConfig.INSTANCE_FINISH_TIME);
	}
	
	/**
	 * Finishes the instance, updating re-entry time when required and setting the new duration.
	 * @param delay the new duration to set
	 */
	public void finishInstance(int delay)
	{
		// Set re-enter for players.
		if (_template.getReenterType() == InstanceReenterType.ON_FINISH)
		{
			setReenterTime();
		}
		
		// Change instance duration
		setDuration(delay);
	}
	
	// ---------------------------------------------
	// Listeners
	// ---------------------------------------------
	/**
	 * Handles a player death, scheduling an ejection task when configured and the player is not on an event.
	 * @param player the {@code Player} who died
	 */
	public void onDeath(Player player)
	{
		if (player.isOnEvent())
		{
			return;
		}
		
		final int ejectTime = _template.getEjectTime();
		if (ejectTime > 0)
		{
			// Send message
			final SystemMessage sm = new SystemMessage(SystemMessageId.IF_YOU_ARE_NOT_RESURRECTED_IN_S1_MIN_YOU_WILL_BE_TELEPORTED_OUT_OF_THE_INSTANCE_ZONE);
			sm.addInt(ejectTime);
			player.sendPacket(sm);
			
			// Start eject task
			final ScheduledFuture<?> oldTAsk = _ejectDeadTasks.put(player.getObjectId(), ThreadPool.schedule(() ->
			{
				if (player.isDead())
				{
					ejectPlayer(player.asPlayer());
				}
			}, ejectTime * 60 * 1000)); // Minutes to milliseconds.
			if (oldTAsk != null)
			{
				oldTAsk.cancel(true);
			}
		}
	}
	
	/**
	 * Cancels and removes the scheduled ejection task for the given player.
	 * @param player the {@code Player} whose ejection task is stopped
	 */
	public void doRevive(Player player)
	{
		final ScheduledFuture<?> task = _ejectDeadTasks.remove(player.getObjectId());
		if (task != null)
		{
			task.cancel(true);
		}
	}
	
	/**
	 * Handles a world object entering or leaving, managing registration, buffs, return location, NPC spawning and events.
	 * @param object the {@code WorldObject} changing state
	 * @param enter {@code true} if entering, {@code false} if leaving
	 */
	public void onInstanceChange(WorldObject object, boolean enter)
	{
		if (object.isPlayer())
		{
			final Player player = object.asPlayer();
			if (enter)
			{
				addPlayer(player);
				
				// Cancel _emptyDestroyTask when remaining time is bigger than empty time.
				final long emptyTime = _template.getEmptyDestroyTime();
				if ((_emptyDestroyTask != null) && (emptyTime > 0) && (getRemainingTime() > emptyTime))
				{
					_emptyDestroyTask.cancel(false);
					_emptyDestroyTask = null;
				}
				
				// Set origin return location if enabled.
				if (_template.getExitLocationType() == InstanceTeleportType.ORIGIN)
				{
					player.getVariables().set(PlayerVariables.INSTANCE_ORIGIN, player.getX() + ";" + player.getY() + ";" + player.getZ());
				}
				
				// Remove player buffs
				if (_template.isRemoveBuffEnabled())
				{
					_template.removePlayerBuff(player);
				}
				
				// Notify DP scripts
				if (!isDynamic() && EventDispatcher.getInstance().hasListener(EventType.ON_INSTANCE_ENTER, _template))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnInstanceEnter(player, this), _template);
				}
			}
			else
			{
				removePlayer(player);
				
				// Notify DP scripts
				if (!isDynamic() && EventDispatcher.getInstance().hasListener(EventType.ON_INSTANCE_LEAVE, _template))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnInstanceLeave(player, this), _template);
				}
			}
		}
		else if (object.isNpc())
		{
			final Npc npc = object.asNpc();
			if (enter)
			{
				addNpc(npc);
			}
			else
			{
				final Spawn npcSpawn = npc.getSpawn();
				if (npcSpawn != null)
				{
					npcSpawn.stopRespawn();
				}
				
				removeNpc(npc);
			}
		}
	}
	
	/**
	 * Handles a player logout, removing them and marking for restore or moving them and any pet to the exit location.
	 * @param player the {@code Player} logging out
	 */
	public void onPlayerLogout(Player player)
	{
		removePlayer(player);
		if (GeneralConfig.RESTORE_PLAYER_INSTANCE)
		{
			player.getVariables().set(PlayerVariables.INSTANCE_RESTORE, _id);
		}
		else
		{
			final Location loc = getExitLocation(player);
			if (loc != null)
			{
				player.setLocationInvisible(loc);
				
				// If player has death pet, put him out of instance world.
				final Summon pet = player.getPet();
				if (pet != null)
				{
					pet.teleToLocation(loc, true);
				}
			}
		}
	}
	
	// ----------------------------------------------
	// Template methods
	// ----------------------------------------------
	/**
	 * Retrieves the parameters defined in the template.
	 * @return the template {@code StatSet}
	 */
	public StatSet getTemplateParameters()
	{
		return _template.getParameters();
	}
	
	/**
	 * Retrieves the id of the template for this instance.
	 * @return the template id as an {@code int}
	 */
	public int getTemplateId()
	{
		return _template.getId();
	}
	
	/**
	 * Retrieves the re-entry type from the template.
	 * @return the {@code InstanceReenterType}
	 */
	public InstanceReenterType getReenterType()
	{
		return _template.getReenterType();
	}
	
	/**
	 * Indicates whether PvP is enabled in this instance.
	 * @return {@code true} if PvP is allowed, {@code false} otherwise
	 */
	public boolean isPvP()
	{
		return _template.isPvP();
	}
	
	/**
	 * Indicates whether players may use summons in this instance.
	 * @return {@code true} if summoning is permitted, {@code false} otherwise
	 */
	public boolean isPlayerSummonAllowed()
	{
		return _template.isPlayerSummonAllowed();
	}
	
	/**
	 * Retrieves the entry location from the template.
	 * @return the entry {@code Location}
	 */
	public Location getEnterLocation()
	{
		return _template.getEnterLocation();
	}
	
	/**
	 * Retrieves the entry points from the template.
	 * @return a {@code List} of entry {@code Location}
	 */
	public List<Location> getEnterLocations()
	{
		return _template.getEnterLocations();
	}
	
	/**
	 * Retrieves the exit location for the given player.
	 * @param player the {@code Player} to resolve for
	 * @return the exit {@code Location}
	 */
	public Location getExitLocation(Player player)
	{
		return _template.getExitLocation(player);
	}
	
	/**
	 * Retrieves the experience rate from the template.
	 * @return the experience rate as a {@code float}
	 */
	public float getExpRate()
	{
		return _template.getExpRate();
	}
	
	/**
	 * Retrieves the SP rate from the template.
	 * @return the SP rate as a {@code float}
	 */
	public float getSPRate()
	{
		return _template.getSPRate();
	}
	
	/**
	 * Retrieves the party experience rate from the template.
	 * @return the party experience rate as a {@code float}
	 */
	public float getExpPartyRate()
	{
		return _template.getExpPartyRate();
	}
	
	/**
	 * Retrieves the party SP rate from the template.
	 * @return the party SP rate as a {@code float}
	 */
	public float getSPPartyRate()
	{
		return _template.getSPPartyRate();
	}
	
	// ----------------------------------------------
	// Tasks
	// ----------------------------------------------
	/**
	 * Reschedules cleanup, scheduling destruction when one minute or less remains, otherwise re-running in five minutes.
	 */
	private void cleanUp()
	{
		if (_cleanUpTask != null)
		{
			_cleanUpTask.cancel(true);
		}
		
		if (getRemainingTime() <= TimeUnit.MINUTES.toMillis(1))
		{
			sendWorldDestroyMessage(1);
			_cleanUpTask = ThreadPool.schedule(this::destroy, 60 * 1000); // 1 minute
		}
		else
		{
			sendWorldDestroyMessage(5);
			_cleanUpTask = ThreadPool.schedule(this::cleanUp, 5 * 60 * 1000); // 5 minutes
		}
	}
	
	/**
	 * Broadcasts a destruction warning when the delay is {@code 5} minutes or less.
	 * @param delay the minutes until destruction
	 */
	private void sendWorldDestroyMessage(int delay)
	{
		// Dimensional wrap does not show timer after 5 minutes.
		if (delay > 5)
		{
			return;
		}
		
		final SystemMessage sm = new SystemMessage(SystemMessageId.THE_INSTANCE_ZONE_EXPIRES_IN_S1_MIN_AFTER_THAT_YOU_WILL_BE_TELEPORTED_OUTSIDE_2);
		sm.addInt(delay);
		broadcastPacket(sm);
	}
	
	/**
	 * Compares by instance id.
	 * @param obj the object to compare
	 * @return {@code true} if equal, {@code false} otherwise
	 */
	@Override
	public boolean equals(Object obj)
	{
		return (obj instanceof Instance) && (((Instance) obj).getId() == getId());
	}
	
	/**
	 * Returns a string of the form {@code name(id)}.
	 * @return a string with the name and id
	 */
	@Override
	public String toString()
	{
		return _template.getName() + "(" + _id + ")";
	}
}
