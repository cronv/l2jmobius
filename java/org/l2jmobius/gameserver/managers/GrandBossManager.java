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
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Manages the lifecycle and state of {@code GrandBoss} entities.<br>
 * This class handles the spawning, tracking, and data management for all grand bosses in the game world.
 * @author DaRkRaGe, Emperorc
 */
public class GrandBossManager
{
	// SQL queries
	private static final String UPDATE_GRAND_BOSS_DATA = "UPDATE grandboss_data set loc_x = ?, loc_y = ?, loc_z = ?, heading = ?, respawn_time = ?, currentHP = ?, currentMP = ?, status = ? where boss_id = ?";
	private static final String UPDATE_GRAND_BOSS_DATA2 = "UPDATE grandboss_data set status = ? where boss_id = ?";
	
	protected static final Logger LOGGER = Logger.getLogger(GrandBossManager.class.getName());
	
	protected static Map<Integer, GrandBoss> _bosses = new ConcurrentHashMap<>();
	
	protected static Map<Integer, StatSet> _storedInfo = new HashMap<>();
	
	private final Map<Integer, Integer> _bossStatus = new HashMap<>();
	
	/**
	 * Initializes the {@code GrandBossManager}.<br>
	 * This constructor calls the internal {@code init()} method.<br>
	 * It prepares the manager for use by loading necessary data.
	 */
	protected GrandBossManager()
	{
		init();
	}
	
	/**
	 * Initializes the manager by loading data from the database.<br>
	 * It populates the internal maps with boss information for faster access.<br>
	 * This method also schedules a periodic task to save data every 5 minutes.
	 */
	private void init()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT * from grandboss_data ORDER BY boss_id"))
		{
			// Read all info from DB, and store it for AI to read and decide what to do faster than accessing DB in real time.
			while (rs.next())
			{
				final int bossId = rs.getInt("boss_id");
				final NpcTemplate template = NpcData.getInstance().getTemplate(bossId);
				if (template != null)
				{
					final StatSet info = new StatSet();
					info.set("loc_x", rs.getInt("loc_x"));
					info.set("loc_y", rs.getInt("loc_y"));
					info.set("loc_z", rs.getInt("loc_z"));
					info.set("heading", rs.getInt("heading"));
					info.set("respawn_time", rs.getLong("respawn_time"));
					info.set("currentHP", rs.getDouble("currentHP"));
					info.set("currentMP", rs.getDouble("currentMP"));
					final int status = rs.getInt("status");
					_bossStatus.put(bossId, status);
					_storedInfo.put(bossId, info);
					final String name = template.getName();
					LOGGER.info(getClass().getSimpleName() + ": " + name + "(" + bossId + ") status is " + status);
					if (status > 0)
					{
						LOGGER.info(getClass().getSimpleName() + ": Next spawn date of " + name + " is " + new Date(info.getLong("respawn_time")));
					}
				}
				else
				{
					LOGGER.warning(getClass().getSimpleName() + ": Could not find GrandBoss NPC template for " + bossId);
				}
			}
			
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _storedInfo.size() + " instances.");
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not load grandboss_data table: " + e.getMessage(), e);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error while initializing GrandBossManager: " + e.getMessage(), e);
		}
		
		ThreadPool.scheduleAtFixedRate(this::storeMe, 5 * 60 * 1000, 5 * 60 * 1000);
	}
	
	/**
	 * Retrieves the current status of a specific boss.<br>
	 * This method looks up the value in the internal status map.
	 * @param bossId The unique identifier for the boss.
	 * @return The integer status associated with the given {@code bossId}, or -1 if it does not exist.
	 */
	public int getStatus(int bossId)
	{
		if (!_bossStatus.containsKey(bossId))
		{
			return -1;
		}
		
		return _bossStatus.get(bossId);
	}
	
	/**
	 * Updates the current status of a specific grand boss.<br>
	 * This method saves the new value to the internal map and the database.
	 * @param bossId The unique identifier for the boss.
	 * @param status The new status value to assign.
	 */
	public void setStatus(int bossId, int status)
	{
		_bossStatus.put(bossId, status);
		LOGGER.info(getClass().getSimpleName() + ": Updated " + NpcData.getInstance().getTemplate(bossId).getName() + "(" + bossId + ") status to " + status + ".");
		updateDb(bossId, true);
	}
	
	/**
	 * Adds a new {@code GrandBoss} to the manager.<br>
	 * This method updates the internal map of active bosses.<br>
	 * It checks if the provided {@code boss} is not {@code null}.
	 * @param boss The {@code GrandBoss} instance to add.
	 */
	public void addBoss(GrandBoss boss)
	{
		if (boss != null)
		{
			_bosses.put(boss.getId(), boss);
		}
	}
	
	/**
	 * Retrieves a {@code GrandBoss} object based on its unique ID.<br>
	 * This method looks up the boss in the internal manager map.<br>
	 * It returns {@code null} if no boss is found for the given ID.
	 * @param bossId The unique identifier of the boss to retrieve.
	 * @return The {@code GrandBoss} instance, or {@code null} if not found.
	 */
	public GrandBoss getBoss(int bossId)
	{
		return _bosses.get(bossId);
	}
	
	/**
	 * Retrieves the {@code StatSet} for a specific boss.<br>
	 * This method looks up the data using the provided {@code bossId}.<br>
	 * It returns {@code null} if no stats are found.
	 * @param bossId The unique identifier of the boss.
	 * @return The {@code StatSet} associated with the boss, or {@code null}.
	 */
	public StatSet getStatSet(int bossId)
	{
		return _storedInfo.get(bossId);
	}
	
	/**
	 * Updates the statistics for a specific boss.<br>
	 * This method saves the {@code StatSet} data to the internal map.<br>
	 * It also triggers a database update for the provided {@code bossId}.
	 * @param bossId The unique identifier of the boss.
	 * @param info The new {@code StatSet} information to apply.
	 */
	public void setStatSet(int bossId, StatSet info)
	{
		_storedInfo.put(bossId, info);
		updateDb(bossId, false);
	}
	
	/**
	 * Saves the current state of all grand bosses to the database.<br>
	 * This method iterates through {@code _storedInfo} and updates their coordinates, health, and status.<br>
	 * It returns {@code true} if the operation succeeds.<br>
	 * It returns {@code false} if a {@code SQLException} occurs during the process.
	 * @return {@code true} if the data was saved successfully; {@code false} otherwise.
	 */
	public boolean storeMe()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			for (Entry<Integer, StatSet> e : _storedInfo.entrySet())
			{
				final GrandBoss boss = _bosses.get(e.getKey());
				final StatSet info = e.getValue();
				if ((boss == null) || (info == null))
				{
					try (PreparedStatement update = con.prepareStatement(UPDATE_GRAND_BOSS_DATA2))
					{
						update.setInt(1, _bossStatus.get(e.getKey()));
						update.setInt(2, e.getKey());
						update.executeUpdate();
						update.clearParameters();
					}
				}
				else
				{
					try (PreparedStatement update = con.prepareStatement(UPDATE_GRAND_BOSS_DATA))
					{
						update.setInt(1, boss.getX());
						update.setInt(2, boss.getY());
						update.setInt(3, boss.getZ());
						update.setInt(4, boss.getHeading());
						update.setLong(5, info.getLong("respawn_time"));
						double hp = boss.getCurrentHp();
						double mp = boss.getCurrentMp();
						if (boss.isDead())
						{
							hp = boss.getMaxHp();
							mp = boss.getMaxMp();
						}
						
						update.setDouble(6, hp);
						update.setDouble(7, mp);
						update.setInt(8, _bossStatus.get(e.getKey()));
						update.setInt(9, e.getKey());
						update.executeUpdate();
						update.clearParameters();
					}
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "Couldn't store grandbosses to database: " + e.getMessage(), e);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Updates the boss information in the database.<br>
	 * It saves coordinates and stats unless only the status is requested.
	 * @param bossId The unique identifier of the boss to update.
	 * @param statusOnly If {@code true}, only the status field will be updated.
	 */
	private void updateDb(int bossId, boolean statusOnly)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			final GrandBoss boss = _bosses.get(bossId);
			final StatSet info = _storedInfo.get(bossId);
			if (statusOnly || (boss == null) || (info == null))
			{
				try (PreparedStatement ps = con.prepareStatement(UPDATE_GRAND_BOSS_DATA2))
				{
					ps.setInt(1, _bossStatus.get(bossId));
					ps.setInt(2, bossId);
					ps.executeUpdate();
				}
			}
			else
			{
				try (PreparedStatement ps = con.prepareStatement(UPDATE_GRAND_BOSS_DATA))
				{
					ps.setInt(1, boss.getX());
					ps.setInt(2, boss.getY());
					ps.setInt(3, boss.getZ());
					ps.setInt(4, boss.getHeading());
					ps.setLong(5, info.getLong("respawn_time"));
					double hp = boss.getCurrentHp();
					double mp = boss.getCurrentMp();
					if (boss.isDead())
					{
						hp = boss.getMaxHp();
						mp = boss.getMaxMp();
					}
					
					ps.setDouble(6, hp);
					ps.setDouble(7, mp);
					ps.setInt(8, _bossStatus.get(bossId));
					ps.setInt(9, bossId);
					ps.executeUpdate();
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "Couldn't update grandbosses to database:" + e.getMessage(), e);
		}
	}
	
	/**
	 * Performs a cleanup of the manager's internal data.<br>
	 * This method calls {@code storeMe()} to save current progress.<br>
	 * It then clears all entries from the {@code _bosses}, {@code _storedInfo}, and {@code _bossStatus} maps.
	 */
	public void cleanUp()
	{
		storeMe();
		
		_bosses.clear();
		_storedInfo.clear();
		_bossStatus.clear();
	}
	
	/**
	 * Retrieves the singleton instance of the {@code GrandBossManager}.<br>
	 * Use this method to access the global manager for all grand boss logic.
	 * @return The active {@code GrandBossManager} instance.
	 */
	public static GrandBossManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final GrandBossManager INSTANCE = new GrandBossManager();
	}
}
