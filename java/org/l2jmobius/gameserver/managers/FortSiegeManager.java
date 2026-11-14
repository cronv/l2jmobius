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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.siege.CombatFlag;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.siege.FortSiege;
import org.l2jmobius.gameserver.mechanics.siege.FortSpawnHolder;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Manages the logic and lifecycle of fort sieges within the game world.<br>
 * This class handles siege mechanics, including fort spawning and combat rules.<br>
 * It coordinates interactions between {@code Clan} members and {@code FortSiege} instances.
 */
public class FortSiegeManager
{
	private static final Logger LOGGER = Logger.getLogger(FortSiegeManager.class.getName());
	
	private static final String FORTSIEGE_CONFIG_FILE = "./config/FortSiege.ini";
	
	private int _attackerMaxClans = 500; // Max number of clans.
	
	// Fort Siege settings
	private Map<Integer, List<FortSpawnHolder>> _commanderSpawnList;
	private Map<Integer, List<CombatFlag>> _flagList;
	private boolean _justToTerritory = true; // Changeable in fortsiege.properties
	private int _flagMaxCount = 1; // Changeable in fortsiege.properties
	private int _siegeClanMinLevel = 4; // Changeable in fortsiege.properties
	private int _siegeLength = 60; // Time in minute. Changeable in fortsiege.properties.
	private int _countDownLength = 10; // Time in minute. Changeable in fortsiege.properties.
	private int _suspiciousMerchantRespawnDelay = 180; // Time in minute. Changeable in fortsiege.properties.
	private final Map<Integer, FortSiege> _sieges = new ConcurrentHashMap<>();
	
	/**
	 * Initializes a new instance of the {@code FortSiegeManager}.<br>
	 * This constructor calls the internal {@code load()} method to prepare data.
	 */
	protected FortSiegeManager()
	{
		load();
	}
	
	/**
	 * Grants specific siege skills to a player.<br>
	 * This method adds the {@code SEAL_OF_RULER} and {@code BUILD_HEADQUARTERS} skills.<br>
	 * The skills are added with the passive flag set to {@code false}.
	 * @param character The {@code Player} instance to receive the skills.
	 */
	public void addSiegeSkills(Player character)
	{
		character.addSkill(CommonSkill.SEAL_OF_RULER.getSkill(), false);
		character.addSkill(CommonSkill.BUILD_HEADQUARTERS.getSkill(), false);
	}
	
	/**
	 * Checks if a specific clan is registered for a fort siege.<br>
	 * This method queries the database to verify the registration status.
	 * @param clan The {@code Clan} object to check.
	 * @param fortid The unique identifier of the fort.
	 * @return {@code true} if the clan is registered, {@code false} otherwise.
	 */
	public boolean checkIsRegistered(Clan clan, int fortid)
	{
		if (clan == null)
		{
			return false;
		}
		
		boolean register = false;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT clan_id FROM fortsiege_clans where clan_id=? and fort_id=?"))
		{
			ps.setInt(1, clan.getId());
			ps.setInt(2, fortid);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					register = true;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: checkIsRegistered(): " + e.getMessage(), e);
		}
		
		return register;
	}
	
	/**
	 * Removes specific siege skills from a player.<br>
	 * This method clears the {@code SEAL_OF_RULER} and {@code BUILD_HEADQUARTERS} skills.
	 * @param character The {@code Player} object to modify.
	 */
	public void removeSiegeSkills(Player character)
	{
		character.removeSkill(CommonSkill.SEAL_OF_RULER.getSkill());
		character.removeSkill(CommonSkill.BUILD_HEADQUARTERS.getSkill());
	}
	
	/**
	 * Loads the configuration settings from a file.<br>
	 * This method populates siege parameters and spawn locations.<br>
	 * It parses values for flags, commanders, and timing limits.
	 */
	private void load()
	{
		final Properties siegeSettings = new Properties();
		final File file = new File(FORTSIEGE_CONFIG_FILE);
		try (InputStream is = new FileInputStream(file))
		{
			siegeSettings.load(is);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error while loading Fort Siege Manager settings!", e);
		}
		
		// Siege setting
		_justToTerritory = Boolean.parseBoolean(siegeSettings.getProperty("JustToTerritory", "true"));
		_attackerMaxClans = Integer.decode(siegeSettings.getProperty("AttackerMaxClans", "500"));
		_flagMaxCount = Integer.decode(siegeSettings.getProperty("MaxFlags", "1"));
		_siegeClanMinLevel = Integer.decode(siegeSettings.getProperty("SiegeClanMinLevel", "4"));
		_siegeLength = Integer.decode(siegeSettings.getProperty("SiegeLength", "60"));
		_countDownLength = Integer.decode(siegeSettings.getProperty("CountDownLength", "10"));
		_suspiciousMerchantRespawnDelay = Integer.decode(siegeSettings.getProperty("SuspiciousMerchantRespawnDelay", "180"));
		
		// Siege spawns settings
		_commanderSpawnList = new ConcurrentHashMap<>();
		_flagList = new ConcurrentHashMap<>();
		for (Fort fort : FortManager.getInstance().getForts())
		{
			final List<FortSpawnHolder> commanderSpawns = new CopyOnWriteArrayList<>();
			final List<CombatFlag> flagSpawns = new CopyOnWriteArrayList<>();
			final String fortName = fort.getName().replace(" ", "");
			for (int i = 1; i < 5; i++)
			{
				final String _spawnParams = siegeSettings.getProperty(fortName + "Commander" + i, "");
				if (_spawnParams.isEmpty())
				{
					break;
				}
				
				final StringTokenizer st = new StringTokenizer(_spawnParams.trim(), ",");
				
				try
				{
					final int x = Integer.parseInt(st.nextToken());
					final int y = Integer.parseInt(st.nextToken());
					final int z = Integer.parseInt(st.nextToken());
					final int heading = Integer.parseInt(st.nextToken());
					final int npc_id = Integer.parseInt(st.nextToken());
					commanderSpawns.add(new FortSpawnHolder(fort.getResidenceId(), x, y, z, heading, npc_id, i));
				}
				catch (Exception e)
				{
					LOGGER.warning("Error while loading commander(s) for " + fort.getName() + " fort.");
				}
			}
			
			final int residenceId = fort.getResidenceId();
			_commanderSpawnList.put(residenceId, commanderSpawns);
			
			for (int i = 1; i < 4; i++)
			{
				final String _spawnParams = siegeSettings.getProperty(fortName + "Flag" + i, "");
				if (_spawnParams.isEmpty())
				{
					break;
				}
				
				final StringTokenizer st = new StringTokenizer(_spawnParams.trim(), ",");
				
				try
				{
					final int x = Integer.parseInt(st.nextToken());
					final int y = Integer.parseInt(st.nextToken());
					final int z = Integer.parseInt(st.nextToken());
					final int flag_id = Integer.parseInt(st.nextToken());
					flagSpawns.add(new CombatFlag(residenceId, x, y, z, 0, flag_id));
				}
				catch (Exception e)
				{
					LOGGER.warning("Error while loading flag(s) for " + fort.getName() + " fort.");
				}
			}
			
			_flagList.put(residenceId, flagSpawns);
		}
	}
	
	/**
	 * Retrieves the list of commander spawn locations for a specific fort.<br>
	 * This method looks up the data using the provided {@code fortId}.
	 * @param fortId The unique identifier of the fort.
	 * @return A {@code List} of {@code FortSpawnHolder} objects associated with the fort.
	 */
	public List<FortSpawnHolder> getCommanderSpawnList(int fortId)
	{
		return _commanderSpawnList.get(fortId);
	}
	
	/**
	 * Retrieves the list of combat flags for a specific fort.<br>
	 * This method looks up the flags associated with the provided {@code fortId}.
	 * @param fortId The unique identifier of the fort.
	 * @return A {@code List} of {@code CombatFlag} objects, or {@code null} if no flags exist.
	 */
	public List<CombatFlag> getFlagList(int fortId)
	{
		return _flagList.get(fortId);
	}
	
	/**
	 * Retrieves the maximum number of clans allowed to attack.<br>
	 * This value is used to limit participation in a siege.
	 * @return the maximum number of attacking clans as an {@code int}.
	 */
	public int getAttackerMaxClans()
	{
		return _attackerMaxClans;
	}
	
	/**
	 * Retrieves the maximum number of flags allowed.<br>
	 * This value is used to limit flag counts during a siege.
	 * @return the maximum count of flags as an {@code int}.
	 */
	public int getFlagMaxCount()
	{
		return _flagMaxCount;
	}
	
	/**
	 * Checks if the system allows registration for territory only.<br>
	 * This value is retrieved from the configuration settings.
	 * @return {@code true} if just territory registration is enabled, {@code false} otherwise.
	 */
	public boolean canRegisterJustTerritory()
	{
		return _justToTerritory;
	}
	
	/**
	 * Retrieves the respawn delay for suspicious merchants.<br>
	 * This value is used to determine how long it takes for a merchant to reappear.
	 * @return the current respawn delay as an {@code int}.
	 */
	public int getSuspiciousMerchantRespawnDelay()
	{
		return _suspiciousMerchantRespawnDelay;
	}
	
	/**
	 * Retrieves the {@code FortSiege} object at the location of a specific world object.<br>
	 * This method uses the coordinates of the provided {@code WorldObject}.
	 * @param activeObject The {@code WorldObject} used to determine the current position.
	 * @return The {@code FortSiege} associated with that location, or {@code null} if none exists.
	 */
	public FortSiege getSiege(WorldObject activeObject)
	{
		return getSiege(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}
	
	/**
	 * Retrieves the {@code FortSiege} object for a specific location.<br>
	 * This method checks all forts to see if the coordinates are within their zone.
	 * @param x The x-coordinate of the location.
	 * @param y The y-coordinate of the location.
	 * @param z The z-coordinate of the location.
	 * @return The {@code FortSiege} object if found, or {@code null} if no siege exists at these coordinates.
	 */
	public FortSiege getSiege(int x, int y, int z)
	{
		for (Fort fort : FortManager.getInstance().getForts())
		{
			final FortSiege siege = fort.getSiege();
			if (siege.checkIfInZone(x, y, z))
			{
				return siege;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves the minimum level required for a clan to participate in a siege.
	 * @return the minimum level as an {@code int}.
	 */
	public int getSiegeClanMinLevel()
	{
		return _siegeClanMinLevel;
	}
	
	/**
	 * Retrieves the total duration of the siege.
	 * @return The length of the siege as an {@code int}.
	 */
	public int getSiegeLength()
	{
		return _siegeLength;
	}
	
	/**
	 * Retrieves the duration of the countdown timer.<br>
	 * This value is used to determine how long players must wait before a siege begins.
	 * @return The length of the countdown in seconds.
	 */
	public int getCountDownLength()
	{
		return _countDownLength;
	}
	
	/**
	 * Retrieves all active sieges currently managed by the system.
	 * @return A {@code Collection} of {@code FortSiege} objects.
	 */
	public Collection<FortSiege> getSieges()
	{
		return _sieges.values();
	}
	
	/**
	 * Retrieves a {@code FortSiege} object based on the provided ID.<br>
	 * This method looks up the siege data from the internal collection.
	 * @param fortId The unique identifier for the fort.
	 * @return The {@code FortSiege} associated with the given ID, or {@code null} if not found.
	 */
	public FortSiege getSiege(int fortId)
	{
		return _sieges.get(fortId);
	}
	
	/**
	 * Adds a new {@code FortSiege} to the active sieges list.<br>
	 * This method registers the siege using its unique residence ID.
	 * @param fortSiege The {@code FortSiege} object to be added.
	 */
	public void addSiege(FortSiege fortSiege)
	{
		_sieges.put(fortSiege.getFort().getResidenceId(), fortSiege);
	}
	
	/**
	 * Checks if the given item ID represents a combat flag.<br>
	 * This method returns {@code true} only for the specific ID {@code 9819}.
	 * @param itemId The unique identifier of the item to check.
	 * @return {@code true} if the item is a combat flag, otherwise {@code false}.
	 */
	public boolean isCombat(int itemId)
	{
		return (itemId == 9819);
	}
	
	/**
	 * Activates a specific combat flag for a player.<br>
	 * This method checks if the {@code Player} is allowed to pick up the {@code Item}.<br>
	 * It then finds the matching {@code CombatFlag} and activates it.
	 * @param player The {@code Player} attempting to activate the flag.
	 * @param item The {@code Item} being used as a combat flag.
	 * @return {@code true} if the flag was successfully activated, otherwise {@code false}.
	 */
	public boolean activateCombatFlag(Player player, Item item)
	{
		if (!checkIfCanPickup(player))
		{
			return false;
		}
		
		final Fort fort = FortManager.getInstance().getFort(player);
		final List<CombatFlag> fcf = _flagList.get(fort.getResidenceId());
		for (CombatFlag cf : fcf)
		{
			if (cf.getCombatFlagInstance() == item)
			{
				cf.activate(player, item);
			}
		}
		
		return true;
	}
	
	/**
	 * Checks if a {@code Player} is allowed to pick up a combat flag.<br>
	 * The player must be a clan leader and not already have a flag equipped.<br>
	 * It also verifies that the fort siege is currently in progress.
	 * @param player The {@code Player} attempting to pick up the flag.
	 * @return {@code true} if the player can pick up the flag, {@code false} otherwise.
	 */
	public boolean checkIfCanPickup(Player player)
	{
		if (player.isCombatFlagEquipped())
		{
			return false;
		}
		
		if (!player.isClanLeader())
		{
			player.sendMessage("The flag can only be picked up by a clan leader.");
			return false;
		}
		
		final Fort fort = FortManager.getInstance().getFort(player);
		if ((fort == null) || (fort.getResidenceId() <= 0) || (fort.getSiege().getAttackerClan(player.getClan()) == null))
		{
			return false;
		}
		
		if (!fort.getSiege().isInProgress())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.THE_FORTRESS_BATTLE_OF_S1_HAS_FINISHED).addItemName(9819));
			return false;
		}
		
		return true;
	}
	
	/**
	 * Removes a {@code CombatFlag} from the specified {@code Player}.<br>
	 * If the siege is currently in progress, the flag will be dropped on the ground.
	 * @param player The {@code Player} who is dropping the flag.
	 * @param fortId The unique identifier of the {@code Fort} associated with the flag.
	 */
	public void dropCombatFlag(Player player, int fortId)
	{
		final Fort fort = FortManager.getInstance().getFortById(fortId);
		final List<CombatFlag> fcf = _flagList.get(fort.getResidenceId());
		for (CombatFlag cf : fcf)
		{
			if (cf.getPlayerObjectId() == player.getObjectId())
			{
				cf.dropIt();
				if (fort.getSiege().isInProgress())
				{
					cf.spawnMe();
				}
			}
		}
	}
	
	/**
	 * Retrieves the singleton instance of the {@code FortSiegeManager}.<br>
	 * This method provides a global access point to the manager.
	 * @return The active {@code FortSiegeManager} instance.
	 */
	public static FortSiegeManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final FortSiegeManager INSTANCE = new FortSiegeManager();
	}
}
