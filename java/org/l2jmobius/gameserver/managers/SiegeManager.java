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
package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Siege;
import org.l2jmobius.gameserver.mechanics.siege.TowerSpawn;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * Manages the logic and state for siege mechanics within the game world.<br>
 * This class handles {@code Castle} ownership, {@code TowerSpawn} management, and related siege events.
 */
public class SiegeManager
{
	private static final Logger LOGGER = Logger.getLogger(SiegeManager.class.getName());
	
	private static final String SIEGE_CONFIG_FILE = "./config/Siege.ini";
	
	private final Map<Integer, List<TowerSpawn>> _controlTowers = new HashMap<>();
	private final Map<Integer, List<TowerSpawn>> _flameTowers = new HashMap<>();
	
	private int _siegeCycle = 2; // 2 weeks by default.
	private int _attackerMaxClans = 500; // Max number of clans.
	private int _attackerRespawnDelay = 0; // Time in ms. Changeable in siege.config.
	private int _defenderMaxClans = 500; // Max number of clans.
	private int _flagMaxCount = 1; // Changeable in siege.config.
	private int _siegeClanMinLevel = 5; // Changeable in siege.config.
	private int _siegeLength = 120; // Time in minute. Changeable in siege.config.
	private int _bloodAllianceReward = 0; // Number of Blood Alliance items reward for successful castle defending.
	
	/**
	 * Initializes a new instance of the {@code SiegeManager}.<br>
	 * This constructor calls the internal {@code load()} method to populate data.
	 */
	protected SiegeManager()
	{
		load();
	}
	
	/**
	 * Grants siege skills to a specific player.<br>
	 * The skills are determined by the character's noble status.<br>
	 * It uses {@code SkillData} to fetch and add the correct skills.
	 * @param character The {@code Player} who will receive the new skills.
	 */
	public void addSiegeSkills(Player character)
	{
		for (Skill sk : SkillData.getInstance().getSiegeSkills(character.getNobleLevel() > 0))
		{
			character.addSkill(sk, false);
		}
	}
	
	/**
	 * Checks if a specific clan is registered for a siege.<br>
	 * This method verifies the registration status in the database.<br>
	 * It returns {@code true} if the clan owns the castle or is registered for it.
	 * @param clan The {@code Clan} object to check.
	 * @param castleid The unique identifier of the castle.
	 * @return {@code true} if the clan is registered, {@code false} otherwise.
	 */
	public boolean checkIsRegistered(Clan clan, int castleid)
	{
		if (clan == null)
		{
			return false;
		}
		
		if (clan.getCastleId() > 0)
		{
			return true;
		}
		
		boolean register = false;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT clan_id FROM siege_clans where clan_id=? and castle_id=?"))
		{
			statement.setInt(1, clan.getId());
			statement.setInt(2, castleid);
			try (ResultSet rs = statement.executeQuery())
			{
				if (rs.next())
				{
					register = true;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Exception: checkIsRegistered(): " + e.getMessage(), e);
		}
		
		return register;
	}
	
	/**
	 * Removes all siege skills from a specific player.<br>
	 * This method checks if the character is a noble to determine which skills to remove.<br>
	 * It iterates through the relevant skills and calls {@code removeSkill} on the {@code Player}.
	 * @param character The {@code Player} whose skills will be removed.
	 */
	public void removeSiegeSkills(Player character)
	{
		for (Skill sk : SkillData.getInstance().getSiegeSkills(character.getNobleLevel() > 0))
		{
			character.removeSkill(sk);
		}
	}
	
	/**
	 * Loads the siege configuration from the {@code SIEGE_CONFIG_FILE}.<br>
	 * It populates various siege settings and tower data.<br>
	 * This method also initializes control towers and flame towers for each castle.
	 */
	private void load()
	{
		final ConfigReader siegeConfig = new ConfigReader(SIEGE_CONFIG_FILE);
		
		// Siege configurations.
		_siegeCycle = siegeConfig.getInt("SiegeCycle", 2);
		_attackerMaxClans = siegeConfig.getInt("AttackerMaxClans", 500);
		_attackerRespawnDelay = siegeConfig.getInt("AttackerRespawn", 0);
		_defenderMaxClans = siegeConfig.getInt("DefenderMaxClans", 500);
		_flagMaxCount = siegeConfig.getInt("MaxFlags", 1);
		_siegeClanMinLevel = siegeConfig.getInt("SiegeClanMinLevel", 5);
		_siegeLength = siegeConfig.getInt("SiegeLength", 120);
		_bloodAllianceReward = siegeConfig.getInt("BloodAllianceReward", 1);
		
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			final List<TowerSpawn> controlTowers = new ArrayList<>();
			final String name = castle.getName();
			for (int i = 1; i < 0xFF; i++)
			{
				final String configKey = name + "ControlTower" + i;
				if (!siegeConfig.containsKey(configKey))
				{
					break;
				}
				
				final StringTokenizer st = new StringTokenizer(siegeConfig.getString(configKey, ""), ",");
				try
				{
					final int x = Integer.parseInt(st.nextToken());
					final int y = Integer.parseInt(st.nextToken());
					final int z = Integer.parseInt(st.nextToken());
					final int npcId = Integer.parseInt(st.nextToken());
					
					controlTowers.add(new TowerSpawn(npcId, new Location(x, y, z)));
				}
				catch (Exception e)
				{
					LOGGER.warning(getClass().getSimpleName() + ": Error while loading control tower(s) for " + name + " castle.");
				}
			}
			
			final List<TowerSpawn> flameTowers = new ArrayList<>();
			for (int i = 1; i < 0xFF; i++)
			{
				final String configKey = name + "FlameTower" + i;
				if (!siegeConfig.containsKey(configKey))
				{
					break;
				}
				
				final StringTokenizer st = new StringTokenizer(siegeConfig.getString(configKey, ""), ",");
				try
				{
					final int x = Integer.parseInt(st.nextToken());
					final int y = Integer.parseInt(st.nextToken());
					final int z = Integer.parseInt(st.nextToken());
					final int npcId = Integer.parseInt(st.nextToken());
					final List<Integer> zoneList = new ArrayList<>();
					
					while (st.hasMoreTokens())
					{
						zoneList.add(Integer.parseInt(st.nextToken()));
					}
					
					flameTowers.add(new TowerSpawn(npcId, new Location(x, y, z), zoneList));
				}
				catch (Exception e)
				{
					LOGGER.warning(getClass().getSimpleName() + ": Error while loading flame tower(s) for " + name + " castle.");
				}
			}
			
			final int residenceId = castle.getResidenceId();
			_controlTowers.put(residenceId, controlTowers);
			_flameTowers.put(residenceId, flameTowers);
			
			if (castle.getOwnerId() != 0)
			{
				loadTrapUpgrade(residenceId);
			}
		}
	}
	
	/**
	 * Retrieves the list of control towers for a specific castle.<br>
	 * This method looks up the towers based on the provided ID.
	 * @param castleId The unique identifier of the castle.
	 * @return A {@code List} of {@code TowerSpawn} objects or {@code null} if none exist.
	 */
	public List<TowerSpawn> getControlTowers(int castleId)
	{
		return _controlTowers.get(castleId);
	}
	
	/**
	 * Retrieves the list of flame towers for a specific castle.<br>
	 * This method looks up the data based on the provided {@code castleId}.
	 * @param castleId The unique identifier of the castle.
	 * @return A {@code List} of {@code TowerSpawn} objects, or {@code null} if none exist.
	 */
	public List<TowerSpawn> getFlameTowers(int castleId)
	{
		return _flameTowers.get(castleId);
	}
	
	/**
	 * Retrieves the current siege cycle value.<br>
	 * This value is used to determine the progression of the siege event.
	 * @return The current {@code int} value of the siege cycle.
	 */
	public int getSiegeCycle()
	{
		return _siegeCycle;
	}
	
	/**
	 * Retrieves the maximum number of clans allowed to attack.<br>
	 * This value is loaded from the configuration settings.
	 * @return The maximum number of attacking clans as an {@code int}.
	 */
	public int getAttackerMaxClans()
	{
		return _attackerMaxClans;
	}
	
	/**
	 * Retrieves the time delay for attackers to respawn.<br>
	 * This value is used during siege mechanics.
	 * @return The respawn delay as an {@code int}.
	 */
	public int getAttackerRespawnDelay()
	{
		return _attackerRespawnDelay;
	}
	
	/**
	 * Retrieves the maximum number of clans allowed to defend a siege.<br>
	 * This value is loaded from the configuration settings.
	 * @return The maximum number of defender clans as an {@code int}.
	 */
	public int getDefenderMaxClans()
	{
		return _defenderMaxClans;
	}
	
	/**
	 * Retrieves the maximum number of flags allowed.<br>
	 * This value is used to limit flag counts during a siege.
	 * @return The maximum count of flags as an {@code int}.
	 */
	public int getFlagMaxCount()
	{
		return _flagMaxCount;
	}
	
	/**
	 * Retrieves the {@code Siege} object at a specific location.<br>
	 * This method uses the coordinates from the provided {@code ILocational} object.
	 * @param loc The location to check for an active siege.
	 * @return The {@code Siege} instance at the location, or {@code null} if none exists.
	 */
	public Siege getSiege(ILocational loc)
	{
		return getSiege(loc.getX(), loc.getY(), loc.getZ());
	}
	
	/**
	 * Retrieves the {@code Siege} object at the location of a specific world object.<br>
	 * This method uses the coordinates of the provided {@code WorldObject}.
	 * @param activeObject The {@code WorldObject} used to determine the current location.
	 * @return The {@code Siege} instance located at the object's position, or {@code null} if none exists.
	 */
	public Siege getSiege(WorldObject activeObject)
	{
		return getSiege(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}
	
	/**
	 * Retrieves the {@code Siege} object for a specific set of coordinates.<br>
	 * This method checks all castles to see if the location is within their zone.<br>
	 * It returns {@code null} if no siege is found at the given position.
	 * @param x The x-coordinate of the location.
	 * @param y The y-coordinate of the location.
	 * @param z The z-coordinate of the location.
	 * @return The {@code Siege} object for the area, or {@code null} if none exists.
	 */
	public Siege getSiege(int x, int y, int z)
	{
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			final Siege siege = castle.getSiege();
			if (siege.checkIfInZone(x, y, z))
			{
				return siege;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves the minimum level required for a clan to participate in a siege.<br>
	 * This value is loaded from the configuration settings.
	 * @return The minimum level as an {@code int}.
	 */
	public int getSiegeClanMinLevel()
	{
		return _siegeClanMinLevel;
	}
	
	/**
	 * Retrieves the total duration of a siege.<br>
	 * This value is used to determine how long a siege event lasts.
	 * @return The length of the siege as an {@code int}.
	 */
	public int getSiegeLength()
	{
		return _siegeLength;
	}
	
	/**
	 * Retrieves the reward amount for a Blood Alliance.<br>
	 * This value is loaded from the configuration files.
	 * @return The integer reward value.
	 */
	public int getBloodAllianceReward()
	{
		return _bloodAllianceReward;
	}
	
	/**
	 * Retrieves all active siege objects from the server.<br>
	 * This method iterates through every {@code Castle} and collects their associated {@code Siege}.
	 * @return A {@code Collection} of all {@code Siege} instances.
	 */
	public Collection<Siege> getSieges()
	{
		final List<Siege> sieges = new ArrayList<>();
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			sieges.add(castle.getSiege());
		}
		
		return sieges;
	}
	
	/**
	 * Retrieves the {@code Siege} object for a specific castle.<br>
	 * It searches through all castles to find one matching the provided ID.
	 * @param castleId The unique identifier of the castle.
	 * @return The {@code Siege} associated with the castle, or {@code null} if not found.
	 */
	public Siege getSiege(int castleId)
	{
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			if (castle.getResidenceId() == castleId)
			{
				return castle.getSiege();
			}
		}
		
		return null;
	}
	
	/**
	 * Loads the trap upgrade levels from the database for a specific castle.<br>
	 * It updates the level of each tower in the {@code _flameTowers} map.
	 * @param castleId The unique identifier of the castle to load.
	 */
	private void loadTrapUpgrade(int castleId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM castle_trapupgrade WHERE castleId=?"))
		{
			ps.setInt(1, castleId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					_flameTowers.get(castleId).get(rs.getInt("towerIndex")).setUpgradeLevel(rs.getInt("level"));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: loadTrapUpgrade(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Provides the global instance of the {@code SiegeManager}.<br>
	 * This method follows the singleton pattern.
	 * @return The active {@code SiegeManager} instance.
	 */
	public static SiegeManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SiegeManager INSTANCE = new SiegeManager();
	}
}
