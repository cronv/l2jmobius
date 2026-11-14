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
package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.time.SchedulingPattern;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.DevelopmentConfig;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.SpawnData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.enums.npc.RaidBossStatus;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.spawns.NpcSpawnTemplate;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Manages the loading and handling of NPC spawns from the database.<br>
 * This class coordinates how {@code Spawn} data is retrieved and processed into the game world.
 * @author godson, UnAfraid
 */
public class DatabaseSpawnManager
{
	private static final Logger LOGGER = Logger.getLogger(DatabaseSpawnManager.class.getName());
	
	protected final Map<Integer, Npc> _npcs = new ConcurrentHashMap<>();
	protected final Map<Integer, Spawn> _spawns = new ConcurrentHashMap<>();
	protected final Map<Integer, StatSet> _storedInfo = new ConcurrentHashMap<>();
	protected final Map<Integer, ScheduledFuture<?>> _schedules = new ConcurrentHashMap<>();
	
	/**
	 * Initializes a new instance of the {@code DatabaseSpawnManager}.<br>
	 * This constructor automatically calls the {@code load()} method.
	 */
	protected DatabaseSpawnManager()
	{
		load();
	}
	
	/**
	 * Loads all NPC spawn data from the database.<br>
	 * This method clears existing data and populates the manager with records from the {@code npc_respawns} table.<br>
	 * It validates templates and initializes respawn schedules for each entry.
	 */
	public void load()
	{
		if (DevelopmentConfig.NO_SPAWNS)
		{
			return;
		}
		
		if (!_spawns.isEmpty())
		{
			for (Spawn spawn : _spawns.values())
			{
				deleteSpawn(spawn, false);
			}
		}
		
		_npcs.clear();
		_spawns.clear();
		_storedInfo.clear();
		_schedules.clear();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM npc_respawns");
			ResultSet rset = statement.executeQuery())
		{
			while (rset.next())
			{
				final int id = rset.getInt("id");
				final NpcTemplate template = getValidTemplate(id);
				if (template != null)
				{
					final Spawn spawn = new Spawn(template);
					spawn.setXYZ(rset.getInt("x"), rset.getInt("y"), rset.getInt("z"));
					spawn.setAmount(1);
					spawn.setHeading(rset.getInt("heading"));
					
					final List<NpcSpawnTemplate> spawns = SpawnData.getInstance().getNpcSpawns(npc -> (npc.getId() == template.getId()) && npc.hasDBSave());
					if (spawns.isEmpty())
					{
						LOGGER.warning(getClass().getSimpleName() + ": Couldn't find spawn declaration for npc: " + template.getId() + " - " + template.getName());
						deleteSpawn(spawn, true);
						continue;
					}
					else if (spawns.size() > 1)
					{
						LOGGER.warning(getClass().getSimpleName() + ": Found multiple database spawns for npc: " + template.getId() + " - " + template.getName() + " " + spawns);
						continue;
					}
					
					final NpcSpawnTemplate spawnTemplate = spawns.get(0);
					spawn.setSpawnTemplate(spawnTemplate);
					
					int respawn = 0;
					int respawnRandom = 0;
					SchedulingPattern respawnPattern = null;
					if (spawnTemplate.getRespawnTime() != null)
					{
						respawn = (int) spawnTemplate.getRespawnTime().getSeconds();
					}
					
					if (spawnTemplate.getRespawnTimeRandom() != null)
					{
						respawnRandom = (int) spawnTemplate.getRespawnTimeRandom().getSeconds();
					}
					
					if (spawnTemplate.getRespawnPattern() != null)
					{
						respawnPattern = spawnTemplate.getRespawnPattern();
					}
					
					if ((respawn > 0) || (respawnPattern != null))
					{
						spawn.setRespawnDelay(respawn, respawnRandom);
						spawn.setRespawnPattern(respawnPattern);
						spawn.startRespawn();
					}
					else
					{
						spawn.stopRespawn();
						LOGGER.warning(getClass().getSimpleName() + ": Found database spawns without respawn for npc: " + template.getId() + " - " + template.getName() + " " + spawnTemplate);
						continue;
					}
					
					addNewSpawn(spawn, rset.getLong("respawnTime"), rset.getDouble("currentHp"), rset.getDouble("currentMp"), false);
				}
				else
				{
					LOGGER.warning(getClass().getSimpleName() + ": Could not load npc #" + rset.getInt("id") + " from DB");
					
					// Remove non existent NPC respawn.
					try (Connection con2 = DatabaseFactory.getConnection();
						PreparedStatement statement2 = con2.prepareStatement("DELETE FROM npc_respawns WHERE id=?"))
					{
						statement2.setInt(1, id);
						statement2.execute();
					}
					catch (Exception e)
					{
						LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not remove npc #" + id + " from DB: ", e);
					}
				}
			}
			
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _npcs.size() + " instances.");
			LOGGER.info(getClass().getSimpleName() + ": Scheduled " + _schedules.size() + " instances.");
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not load npc_respawns table", e);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error while initializing DatabaseSpawnManager: ", e);
		}
	}
	
	/**
	 * Handles the logic for spawning an NPC into the game world.<br>
	 * It initializes the NPC status and stores its initial stats in the database records.<br>
	 * This method also removes the pending schedule for the given ID.
	 * @param npcId The unique identifier of the NPC to spawn.
	 */
	private void scheduleSpawn(int npcId)
	{
		final Npc npc = _spawns.get(npcId).doSpawn();
		if (npc != null)
		{
			npc.setDBStatus(RaidBossStatus.ALIVE);
			
			final StatSet info = new StatSet();
			info.set("currentHP", npc.getCurrentHp());
			info.set("currentMP", npc.getCurrentMp());
			info.set("respawnTime", 0);
			_storedInfo.put(npcId, info);
			_npcs.put(npcId, npc);
			LOGGER.info(getClass().getSimpleName() + ": Spawning NPC " + npc.getName());
		}
		
		_schedules.remove(npcId);
	}
	
	/**
	 * Updates the status and respawn data for a specific {@code Npc}.<br>
	 * If {@code isNpcDead} is {@code true}, it calculates the next respawn time.<br>
	 * If {@code isNpcDead} is {@code false}, it updates the current health and mana.
	 * @param npc The {@code Npc} object to update.
	 * @param isNpcDead A boolean indicating if the NPC has died.
	 */
	public void updateStatus(Npc npc, boolean isNpcDead)
	{
		final Integer npcId = npc.getId();
		final StatSet info = _storedInfo.get(npcId);
		if (info == null)
		{
			return;
		}
		
		if (isNpcDead)
		{
			npc.setDBStatus(RaidBossStatus.DEAD);
			
			final Spawn spawn = npc.getSpawn();
			final SchedulingPattern respawnPattern = spawn.getRespawnPattern();
			final int respawnMinDelay;
			final int respawnMaxDelay;
			final int respawnDelay;
			final long respawnTime;
			if (respawnPattern != null)
			{
				respawnTime = respawnPattern.next(System.currentTimeMillis());
				respawnMinDelay = (int) (respawnTime - System.currentTimeMillis());
				respawnMaxDelay = respawnMinDelay;
				respawnDelay = respawnMinDelay;
			}
			else
			{
				respawnMinDelay = (int) (spawn.getRespawnMinDelay() * NpcConfig.RAID_MIN_RESPAWN_MULTIPLIER);
				respawnMaxDelay = (int) (spawn.getRespawnMaxDelay() * NpcConfig.RAID_MAX_RESPAWN_MULTIPLIER);
				respawnDelay = Rnd.get(respawnMinDelay, respawnMaxDelay);
				respawnTime = System.currentTimeMillis() + respawnDelay;
			}
			
			info.set("currentHP", npc.getMaxHp());
			info.set("currentMP", npc.getMaxMp());
			info.set("respawnTime", respawnTime);
			if ((!_schedules.containsKey(npcId) && ((respawnMinDelay > 0) || (respawnMaxDelay > 0))) || (respawnPattern != null))
			{
				LOGGER.info(getClass().getSimpleName() + ": Updated " + npc.getName() + " respawn time to " + TimeUtil.getDateTimeString(respawnTime));
				_schedules.put(npcId, ThreadPool.schedule(() -> scheduleSpawn(npcId), respawnDelay));
				updateDb();
			}
		}
		else
		{
			npc.setDBStatus(RaidBossStatus.ALIVE);
			
			info.set("currentHP", npc.getCurrentHp());
			info.set("currentMP", npc.getCurrentMp());
			info.set("respawnTime", 0);
		}
		
		_storedInfo.put(npcId, info);
	}
	
	/**
	 * Adds a new spawn to the manager and handles its initial state.<br>
	 * It checks if the NPC is already active before proceeding.<br>
	 * If the respawn time has passed, it spawns the {@code Npc} immediately.<br>
	 * Otherwise, it schedules the spawn for a future time.
	 * @param spawn The {@code Spawn} object containing the location and ID data.
	 * @param respawnTime The timestamp when the NPC should be allowed to spawn.
	 * @param currentHP The health value to set for the NPC.
	 * @param currentMP The mana value to set for the NPC.
	 * @param storeInDb Set to {@code true} to save the spawn data into the database.
	 */
	public void addNewSpawn(Spawn spawn, long respawnTime, double currentHP, double currentMP, boolean storeInDb)
	{
		if (spawn == null)
		{
			return;
		}
		
		final int npcId = spawn.getId();
		if (_spawns.containsKey(npcId))
		{
			return;
		}
		
		final long time = System.currentTimeMillis();
		SpawnTable.getInstance().addSpawn(spawn);
		if ((respawnTime == 0) || (time > respawnTime))
		{
			final Npc npc = spawn.doSpawn();
			if (npc != null)
			{
				// Fully heal NPC.
				ThreadPool.schedule(() ->
				{
					npc.setCurrentHp(currentHP);
					npc.setCurrentMp(currentMP, false);
				}, 100 /* Compensate for recalculateStats task delay. */);
				
				npc.setDBStatus(RaidBossStatus.ALIVE);
				_npcs.put(npcId, npc);
				
				final StatSet info = new StatSet();
				info.set("currentHP", currentHP);
				info.set("currentMP", currentMP);
				info.set("respawnTime", 0);
				_storedInfo.put(npcId, info);
			}
		}
		else
		{
			final long spawnTime = respawnTime - System.currentTimeMillis();
			_schedules.put(npcId, ThreadPool.schedule(() -> scheduleSpawn(npcId), spawnTime));
		}
		
		_spawns.put(npcId, spawn);
		
		if (storeInDb)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement statement = con.prepareStatement("INSERT INTO npc_respawns (id, x, y, z, heading, respawnTime, currentHp, currentMp) VALUES(?, ?, ?, ?, ?, ?, ?, ?)"))
			{
				statement.setInt(1, npcId);
				statement.setInt(2, spawn.getX());
				statement.setInt(3, spawn.getY());
				statement.setInt(4, spawn.getZ());
				statement.setInt(5, spawn.getHeading());
				statement.setLong(6, respawnTime);
				statement.setDouble(7, currentHP);
				statement.setDouble(8, currentMP);
				statement.execute();
			}
			catch (Exception e)
			{
				// Problem with storing spawn.
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not store npc #" + npcId + " in the DB: ", e);
			}
		}
	}
	
	/**
	 * Adds a new spawn to the manager and creates the corresponding {@code Npc}.<br>
	 * This method registers the spawn data and initializes its status.<br>
	 * If {@code storeInDb} is {@code true}, it saves the initial stats to the database.
	 * @param spawn The {@code Spawn} object containing the location and ID of the new NPC.
	 * @param storeInDb A boolean indicating whether to save the new NPC data to the database.
	 * @return The newly created {@code Npc} object, or {@code null} if the provided spawn is {@code null}.
	 */
	public Npc addNewSpawn(Spawn spawn, boolean storeInDb)
	{
		if (spawn == null)
		{
			return null;
		}
		
		final int npcId = spawn.getId();
		final Spawn existingSpawn = _spawns.get(npcId);
		if (existingSpawn != null)
		{
			return existingSpawn.getLastSpawn();
		}
		
		SpawnTable.getInstance().addSpawn(spawn);
		
		final Npc npc = spawn.doSpawn();
		if (npc == null)
		{
			throw new NullPointerException();
		}
		
		npc.setDBStatus(RaidBossStatus.ALIVE);
		
		final StatSet info = new StatSet();
		final long maxHp = npc.getMaxHp();
		final int maxMp = npc.getMaxMp();
		info.set("currentHP", maxHp);
		info.set("currentMP", maxMp);
		info.set("respawnTime", 0);
		_npcs.put(npcId, npc);
		_storedInfo.put(npcId, info);
		
		_spawns.put(npcId, spawn);
		
		if (storeInDb)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement statement = con.prepareStatement("INSERT INTO npc_respawns (id, x, y, z, heading, respawnTime, currentHp, currentMp) VALUES(?, ?, ?, ?, ?, ?, ?, ?)"))
			{
				statement.setInt(1, npcId);
				statement.setInt(2, spawn.getX());
				statement.setInt(3, spawn.getY());
				statement.setInt(4, spawn.getZ());
				statement.setInt(5, spawn.getHeading());
				statement.setLong(6, 0);
				statement.setDouble(7, maxHp);
				statement.setDouble(8, maxMp);
				statement.execute();
			}
			catch (Exception e)
			{
				// Problem with storing spawn.
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not store npc #" + npcId + " in the DB: ", e);
			}
		}
		
		return npc;
	}
	
	/**
	 * Removes a specific spawn from the manager and memory.<br>
	 * This method clears all associated data for the given {@code Spawn}.<br>
	 * It also cancels any active scheduled tasks for this ID.
	 * @param spawn The {@code Spawn} object to be removed.
	 * @param updateDb Set to {@code true} to remove the entry from the database as well.
	 */
	public void deleteSpawn(Spawn spawn, boolean updateDb)
	{
		if (spawn == null)
		{
			return;
		}
		
		final int npcId = spawn.getId();
		_spawns.remove(npcId);
		_npcs.remove(npcId);
		_storedInfo.remove(npcId);
		
		final ScheduledFuture<?> task = _schedules.remove(npcId);
		if (task != null)
		{
			task.cancel(true);
		}
		
		if (updateDb)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("DELETE FROM npc_respawns WHERE id = ?"))
			{
				ps.setInt(1, npcId);
				ps.execute();
			}
			catch (Exception e)
			{
				// Problem with deleting spawn.
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not remove npc #" + npcId + " from DB: ", e);
			}
		}
		
		SpawnTable.getInstance().removeSpawn(spawn);
	}
	
	/**
	 * Updates the current status of all NPCs in the database.<br>
	 * This method synchronizes the {@code _storedInfo} and {@code _npcs} maps with the {@code npc_respawns} table.<br>
	 * It handles updating health, mana, and respawn times for each NPC.
	 */
	private void updateDb()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE npc_respawns SET respawnTime = ?, currentHP = ?, currentMP = ? WHERE id = ?"))
		{
			for (Entry<Integer, StatSet> entry : _storedInfo.entrySet())
			{
				final Integer npcId = entry.getKey();
				if (npcId == null)
				{
					continue;
				}
				
				final Npc npc = _npcs.get(npcId);
				if (npc == null)
				{
					continue;
				}
				
				if (npc.getDBStatus() == RaidBossStatus.ALIVE)
				{
					updateStatus(npc, false);
				}
				
				final StatSet info = entry.getValue();
				if (info == null)
				{
					continue;
				}
				
				try
				{
					final boolean npcDead = npc.isDead();
					statement.setLong(1, info.getLong("respawnTime"));
					statement.setDouble(2, npcDead ? npc.getMaxHp() : info.getDouble("currentHP"));
					statement.setDouble(3, npcDead ? npc.getMaxMp() : info.getDouble("currentMP"));
					statement.setInt(4, npcId);
					statement.executeUpdate();
					statement.clearParameters();
				}
				catch (SQLException e)
				{
					LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not update npc_respawns table ", e);
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": SQL error while updating database spawn to database: ", e);
		}
	}
	
	/**
	 * Retrieves the status of all currently loaded NPCs.<br>
	 * Each entry shows the NPC name followed by its database status.<br>
	 * Returns a list of strings or "None" if no NPCs exist.
	 * @return An array of {@code String} containing NPC statuses.
	 */
	public String[] getAllNpcsStatus()
	{
		final String[] msg = new String[(_npcs == null) ? 0 : _npcs.size()];
		if (_npcs == null)
		{
			msg[0] = "None";
			return msg;
		}
		
		int index = 0;
		for (Npc npc : _npcs.values())
		{
			msg[index++] = npc.getName() + ": " + npc.getDBStatus().name();
		}
		
		return msg;
	}
	
	/**
	 * Retrieves the current status of a specific NPC.<br>
	 * It returns a formatted string containing the name and database status.<br>
	 * If no NPCs are loaded, it returns a default message.
	 * @param npcId The unique identifier for the NPC to check.
	 * @return A {@code String} describing the NPC's current state.
	 */
	public String getNpcsStatus(int npcId)
	{
		String msg = "NPC Status..." + System.lineSeparator();
		if (_npcs == null)
		{
			msg += "None";
			return msg;
		}
		
		final Npc npc = _npcs.get(npcId);
		if (npc != null)
		{
			msg += npc.getName() + ": " + npc.getDBStatus().name();
		}
		
		return msg;
	}
	
	/**
	 * Retrieves the current status of a raid boss based on its unique ID.<br>
	 * It checks if the NPC is currently active, scheduled to spawn, or undefined.
	 * @param npcId The unique identifier for the NPC.
	 * @return The {@code RaidBossStatus} associated with the given {@code npcId}.
	 */
	public RaidBossStatus getStatus(int npcId)
	{
		final Npc npc = _npcs.get(npcId);
		if (npc != null)
		{
			return npc.getDBStatus();
		}
		else if (_schedules.containsKey(npcId))
		{
			return RaidBossStatus.DEAD;
		}
		else
		{
			return RaidBossStatus.UNDEFINED;
		}
	}
	
	/**
	 * Retrieves the {@code NpcTemplate} for a specific NPC ID.<br>
	 * This method fetches data from the global {@code NpcData} instance.
	 * @param npcId The unique identifier of the NPC to look up.
	 * @return The {@code NpcTemplate} associated with the provided ID.
	 */
	public NpcTemplate getValidTemplate(int npcId)
	{
		return NpcData.getInstance().getTemplate(npcId);
	}
	
	/**
	 * Notifies the system that a new night NPC has spawned.<br>
	 * This method initializes the {@code StatSet} for the NPC.<br>
	 * It sets the status to {@code RaidBossStatus.ALIVE}.<br>
	 * The NPC is then added to the internal tracking maps.
	 * @param npc The {@code Npc} instance that was spawned.
	 */
	public void notifySpawnNightNpc(Npc npc)
	{
		final StatSet info = new StatSet();
		info.set("currentHP", npc.getCurrentHp());
		info.set("currentMP", npc.getCurrentMp());
		info.set("respawnTime", 0);
		npc.setDBStatus(RaidBossStatus.ALIVE);
		
		final int npcId = npc.getId();
		_storedInfo.put(npcId, info);
		_npcs.put(npcId, npc);
	}
	
	/**
	 * Checks if a specific NPC exists in the spawn map.<br>
	 * This method verifies if the provided {@code npcId} is currently registered.
	 * @param npcId The unique identifier of the NPC to check.
	 * @return {@code true} if the NPC is defined, {@code false} otherwise.
	 */
	public boolean isDefined(int npcId)
	{
		return _spawns.containsKey(npcId);
	}
	
	/**
	 * Retrieves an {@code Npc} object based on its unique identifier.<br>
	 * This method looks up the NPC in the internal map.
	 * @param id The unique ID of the NPC to find.
	 * @return The {@code Npc} associated with the given ID, or {@code null} if not found.
	 */
	public Npc getNpc(int id)
	{
		return _npcs.get(id);
	}
	
	/**
	 * Retrieves all currently active {@code Npc} objects.<br>
	 * The map uses the unique NPC ID as the key.
	 * @return A {@code Map} containing all loaded {@code Npc} instances.
	 */
	public Map<Integer, Npc> getNpcs()
	{
		return _npcs;
	}
	
	/**
	 * Retrieves all active spawns from the manager.
	 * @return A {@code Map} where the key is the NPC ID and the value is the {@code Spawn} object.
	 */
	public Map<Integer, Spawn> getSpawns()
	{
		return _spawns;
	}
	
	/**
	 * Retrieves the map of stored information.<br>
	 * This map contains {@code StatSet} objects indexed by their unique IDs.
	 * @return a {@code Map<Integer, StatSet>} containing all stored data.
	 */
	public Map<Integer, StatSet> getStoredInfo()
	{
		return _storedInfo;
	}
	
	/**
	 * Performs a full cleanup of the manager's internal data.<br>
	 * This method updates the database and clears all active maps.<br>
	 * It also cancels all pending scheduled tasks.
	 */
	public void cleanUp()
	{
		updateDb();
		
		_npcs.clear();
		
		for (ScheduledFuture<?> shedule : _schedules.values())
		{
			shedule.cancel(true);
		}
		
		_schedules.clear();
		
		_storedInfo.clear();
		_spawns.clear();
	}
	
	/**
	 * Retrieves the singleton instance of the {@code DatabaseSpawnManager}.<br>
	 * This ensures that only one manager handles database spawns throughout the application.
	 * @return The active {@code DatabaseSpawnManager} instance.
	 */
	public static DatabaseSpawnManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final DatabaseSpawnManager INSTANCE = new DatabaseSpawnManager();
	}
}
