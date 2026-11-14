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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.clan.entry.PledgeApplicantInfo;
import org.l2jmobius.gameserver.entity.clan.entry.PledgeRecruitInfo;
import org.l2jmobius.gameserver.entity.clan.entry.PledgeWaitingInfo;

/**
 * Manages the lifecycle and data of clan entries within the game server.<br>
 * This class handles operations related to pledge applicants, recruits, and waiting lists.<br>
 * It interacts with {@code ClanTable} to persist and retrieve entry information.
 * @author Sdw
 */
public class ClanEntryManager
{
	protected static final Logger LOGGER = Logger.getLogger(ClanEntryManager.class.getName());
	
	private static final Map<Integer, PledgeWaitingInfo> _waitingList = new ConcurrentHashMap<>();
	private static final Map<Integer, PledgeRecruitInfo> _clanList = new ConcurrentHashMap<>();
	private static final Map<Integer, Map<Integer, PledgeApplicantInfo>> _applicantList = new ConcurrentHashMap<>();
	
	private static final Map<Integer, ScheduledFuture<?>> _clanLocked = new ConcurrentHashMap<>();
	private static final Map<Integer, ScheduledFuture<?>> _playerLocked = new ConcurrentHashMap<>();
	
	private static final String INSERT_APPLICANT = "REPLACE INTO pledge_applicant VALUES (?, ?, ?, ?)";
	private static final String DELETE_APPLICANT = "DELETE FROM pledge_applicant WHERE charId = ? AND clanId = ?";
	
	private static final String INSERT_WAITING_LIST = "INSERT INTO pledge_waiting_list VALUES (?, ?)";
	private static final String DELETE_WAITING_LIST = "DELETE FROM pledge_waiting_list WHERE char_id = ?";
	
	private static final String INSERT_CLAN_RECRUIT = "INSERT INTO pledge_recruit VALUES (?, ?, ?, ?, ?, ?)";
	private static final String UPDATE_CLAN_RECRUIT = "UPDATE pledge_recruit SET karma = ?, information = ?, detailed_information = ?, application_type = ?, recruit_type = ? WHERE clan_id = ?";
	private static final String DELETE_CLAN_RECRUIT = "DELETE FROM pledge_recruit WHERE clan_id = ?";
	
	// @formatter:off
	private static final List<Comparator<PledgeWaitingInfo>> PLAYER_COMPARATOR = Arrays.asList(
		null,
		Comparator.comparing(PledgeWaitingInfo::getPlayerName), 
		Comparator.comparingInt(PledgeWaitingInfo::getKarma), 
		Comparator.comparingInt(PledgeWaitingInfo::getPlayerLvl), 
		Comparator.comparingInt(PledgeWaitingInfo::getPlayerClassId));
	// @formatter:on
	
	// @formatter:off
	private static final List<Comparator<PledgeRecruitInfo>> CLAN_COMPARATOR = Arrays.asList(
		null,
		Comparator.comparing(PledgeRecruitInfo::getClanName),
		Comparator.comparing(PledgeRecruitInfo::getClanLeaderName),
		Comparator.comparingInt(PledgeRecruitInfo::getClanLevel),
		Comparator.comparingInt(PledgeRecruitInfo::getKarma));
	// @formatter:on
	
	private static final long LOCK_TIME = TimeUnit.MINUTES.toMillis(5);
	
	/**
	 * Initializes a new instance of the {@code ClanEntryManager}.<br>
	 * This constructor calls the {@code load()} method to populate data.
	 */
	protected ClanEntryManager()
	{
		load();
	}
	
	/**
	 * Loads clan recruitment data from the database.<br>
	 * This method populates the internal maps for clans, waiting lists, and applicants.<br>
	 * It handles errors by logging a warning if any query fails.
	 */
	private void load()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT * FROM pledge_recruit"))
		{
			while (rs.next())
			{
				final int clanId = rs.getInt("clan_id");
				final int karma = rs.getInt("karma");
				final String information = rs.getString("information");
				_clanList.put(clanId, new PledgeRecruitInfo(clanId, karma, information, rs.getString("detailed_information"), rs.getInt("application_type"), rs.getInt("recruit_type")));
				
				// Remove non existing clan data.
				if (ClanTable.getInstance().getClan(clanId) == null)
				{
					removeFromClanList(clanId);
				}
			}
			
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _clanList.size() + " clan entries.");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed to load: ", e);
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT a.char_id, a.karma, b.base_class, b.level, b.char_name FROM pledge_waiting_list as a LEFT JOIN characters as b ON a.char_id = b.charId"))
		{
			while (rs.next())
			{
				final int charId = rs.getInt("char_id");
				final int level = rs.getInt("level");
				final int karma = rs.getInt("karma");
				_waitingList.put(charId, new PledgeWaitingInfo(charId, level, karma, rs.getInt("base_class"), rs.getString("char_name")));
			}
			
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _waitingList.size() + " players in waiting list.");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed to load: ", e);
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT a.charId, a.clanId, a.karma, a.message, b.base_class, b.level, b.char_name FROM pledge_applicant as a LEFT JOIN characters as b ON a.charId = b.charId"))
		{
			while (rs.next())
			{
				final int clanId = rs.getInt("clanId");
				final int charId = rs.getInt("charId");
				final int level = rs.getInt("level");
				final int karma = rs.getInt("karma");
				final String message = rs.getString("message");
				_applicantList.computeIfAbsent(clanId, _ -> new ConcurrentHashMap<>()).put(charId, new PledgeApplicantInfo(charId, rs.getString("char_name"), level, karma, clanId, message));
			}
			
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _applicantList.size() + " player applications.");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed to load: ", e);
		}
	}
	
	/**
	 * Retrieves the current list of players waiting to join a clan.<br>
	 * The map uses the player ID as the key.
	 * @return A {@code Map} containing all {@code PledgeWaitingInfo} objects.
	 */
	public Map<Integer, PledgeWaitingInfo> getWaitingList()
	{
		return _waitingList;
	}
	
	/**
	 * Retrieves the current list of clans.<br>
	 * The map uses the clan ID as the key.
	 * @return a {@code Map} containing all {@code PledgeRecruitInfo} objects.
	 */
	public Map<Integer, PledgeRecruitInfo> getClanList()
	{
		return _clanList;
	}
	
	/**
	 * Retrieves the full list of pledge applicants.<br>
	 * The data is organized by clan ID and then by player ID.
	 * @return A map containing all {@code PledgeApplicantInfo} grouped by clan and player.
	 */
	public Map<Integer, Map<Integer, PledgeApplicantInfo>> getApplicantList()
	{
		return _applicantList;
	}
	
	/**
	 * Retrieves the list of pledge applicants for a specific clan.<br>
	 * It returns an empty map if no applicants are found or the clan does not exist.
	 * @param clanId The unique identifier of the clan.
	 * @return A map where the key is the player ID and the value is the {@code PledgeApplicantInfo}.
	 */
	public Map<Integer, PledgeApplicantInfo> getApplicantListForClan(int clanId)
	{
		return _applicantList.getOrDefault(clanId, Collections.emptyMap());
	}
	
	/**
	 * Retrieves the application information for a specific player in a clan.<br>
	 * This method looks up the data using both the {@code clanId} and {@code playerId}.<br>
	 * It returns {@code null} if no application is found.
	 * @param clanId The unique identifier of the clan.
	 * @param playerId The unique identifier of the player.
	 * @return The {@code PledgeApplicantInfo} object or {@code null} if not found.
	 */
	public PledgeApplicantInfo getPlayerApplication(int clanId, int playerId)
	{
		return _applicantList.getOrDefault(clanId, Collections.emptyMap()).get(playerId);
	}
	
	/**
	 * Removes a player's application from a specific clan.<br>
	 * This method deletes the record from the database and updates the internal list.
	 * @param clanId The unique identifier of the clan.
	 * @param playerId The unique identifier of the player.
	 * @return {@code true} if the application was successfully removed, {@code false} otherwise.
	 */
	public boolean removePlayerApplication(int clanId, int playerId)
	{
		final Map<Integer, PledgeApplicantInfo> clanApplicantList = _applicantList.get(clanId);
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_APPLICANT))
		{
			statement.setInt(1, playerId);
			statement.setInt(2, clanId);
			statement.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		return (clanApplicantList != null) && (clanApplicantList.remove(playerId) != null);
	}
	
	/**
	 * Adds a new player application to a specific clan.<br>
	 * This method updates the internal applicant list and the database.<br>
	 * It returns {@code false} if the player is already locked.
	 * @param clanId The unique identifier of the clan.
	 * @param info The data containing the player's application details.
	 * @return {@code true} if the application was added successfully, or {@code false} otherwise.
	 */
	public boolean addPlayerApplicationToClan(int clanId, PledgeApplicantInfo info)
	{
		if (!_playerLocked.containsKey(info.getPlayerId()))
		{
			final Integer playerId = info.getPlayerId();
			_applicantList.computeIfAbsent(clanId, _ -> new ConcurrentHashMap<>()).put(playerId, info);
			
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement statement = con.prepareStatement(INSERT_APPLICANT))
			{
				statement.setInt(1, playerId);
				statement.setInt(2, info.getRequestClanId());
				statement.setInt(3, info.getKarma());
				statement.setString(4, info.getMessage());
				statement.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, e.getMessage(), e);
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Finds the clan ID associated with a player's application.<br>
	 * It searches through all active applications in the system.<br>
	 * If no application is found, it returns {@code 0}.
	 * @param playerId The unique identifier of the player.
	 * @return The {@code Integer} clan ID or {@code 0} if not found.
	 */
	public Integer getClanIdForPlayerApplication(int playerId)
	{
		for (Entry<Integer, Map<Integer, PledgeApplicantInfo>> entry : _applicantList.entrySet())
		{
			if (entry.getValue().containsKey(playerId))
			{
				return entry.getKey();
			}
		}
		
		return 0;
	}
	
	/**
	 * Adds a player to the waiting list.<br>
	 * This method saves the information to the database and updates the internal map.<br>
	 * It returns {@code false} if the player is already locked.
	 * @param playerId The unique ID of the player.
	 * @param info The data containing the player's pledge waiting details.
	 * @return {@code true} if the player was successfully added, or {@code false} otherwise.
	 */
	public boolean addToWaitingList(int playerId, PledgeWaitingInfo info)
	{
		if (!_playerLocked.containsKey(playerId))
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement statement = con.prepareStatement(INSERT_WAITING_LIST))
			{
				statement.setInt(1, info.getPlayerId());
				statement.setInt(2, info.getKarma());
				statement.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, e.getMessage(), e);
			}
			
			_waitingList.put(playerId, info);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Removes a player from the waiting list.<br>
	 * This method deletes the entry from the database and the internal map.<br>
	 * It also locks the player to prevent concurrent modifications.
	 * @param playerId The unique ID of the player to remove.
	 * @return {@code true} if the player was found and removed, otherwise {@code false}.
	 */
	public boolean removeFromWaitingList(int playerId)
	{
		if (_waitingList.containsKey(playerId))
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement statement = con.prepareStatement(DELETE_WAITING_LIST))
			{
				statement.setInt(1, playerId);
				statement.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, e.getMessage(), e);
			}
			
			_waitingList.remove(playerId);
			lockPlayer(playerId);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Adds a new clan recruitment entry to the internal list.<br>
	 * This method saves the data to the database and updates the cache.<br>
	 * It returns {@code false} if the clan already exists or is currently locked.
	 * @param clanId The unique identifier for the clan.
	 * @param info The {@code PledgeRecruitInfo} object containing recruitment details.
	 * @return {@code true} if the entry was added successfully, otherwise {@code false}.
	 */
	public boolean addToClanList(int clanId, PledgeRecruitInfo info)
	{
		if (!_clanList.containsKey(clanId) && !_clanLocked.containsKey(clanId))
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement statement = con.prepareStatement(INSERT_CLAN_RECRUIT))
			{
				statement.setInt(1, info.getClanId());
				statement.setInt(2, info.getKarma());
				statement.setString(3, info.getInformation());
				statement.setString(4, info.getDetailedInformation());
				statement.setInt(5, info.getApplicationType());
				statement.setInt(6, info.getRecruitType());
				statement.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, e.getMessage(), e);
			}
			
			_clanList.put(clanId, info);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Updates the recruitment information for a specific clan.<br>
	 * This method modifies the database and the internal cache if the clan exists.<br>
	 * It returns {@code true} if the update was successful.
	 * @param clanId The unique identifier of the clan to update.
	 * @param info The new {@code PledgeRecruitInfo} data for the clan.
	 * @return {@code true} if the clan was successfully updated, otherwise {@code false}.
	 */
	public boolean updateClanList(int clanId, PledgeRecruitInfo info)
	{
		if (_clanList.containsKey(clanId) && !_clanLocked.containsKey(clanId))
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement statement = con.prepareStatement(UPDATE_CLAN_RECRUIT))
			{
				statement.setInt(1, info.getKarma());
				statement.setString(2, info.getInformation());
				statement.setString(3, info.getDetailedInformation());
				statement.setInt(4, info.getApplicationType());
				statement.setInt(5, info.getRecruitType());
				statement.setInt(6, info.getClanId());
				statement.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, e.getMessage(), e);
			}
			
			return _clanList.replace(clanId, info) != null;
		}
		
		return false;
	}
	
	/**
	 * Removes a clan from the internal recruitment list.<br>
	 * This method deletes the entry from the database and clears it from memory.<br>
	 * It also triggers a lock for the specified clan.
	 * @param clanId The unique identifier of the clan to remove.
	 * @return {@code true} if the clan was successfully removed, or {@code false} if it did not exist.
	 */
	public boolean removeFromClanList(int clanId)
	{
		if (_clanList.containsKey(clanId))
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement statement = con.prepareStatement(DELETE_CLAN_RECRUIT))
			{
				statement.setInt(1, clanId);
				statement.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, e.getMessage(), e);
			}
			
			_clanList.remove(clanId);
			lockClan(clanId);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Retrieves a list of players waiting to join a clan based on specific filters.<br>
	 * The results are filtered by level range and sorted according to the provided criteria.
	 * @param levelMin The minimum character level required to be included in the list.
	 * @param levelMax The maximum character level allowed to be included in the list.
	 * @param role The role of the player to filter by.
	 * @param sortByValue The index used to determine which attribute to sort the list by.
	 * @param descending Set to {@code true} to sort in descending order, or {@code false} for ascending order.
	 * @return A {@code List} of {@code PledgeWaitingInfo} objects matching the criteria.
	 */
	public List<PledgeWaitingInfo> getSortedWaitingList(int levelMin, int levelMax, int role, int sortByValue, boolean descending)
	{
		final int sortBy = Math.clamp(sortByValue, 1, PLAYER_COMPARATOR.size() - 1);
		final List<PledgeWaitingInfo> result = new ArrayList<>();
		for (PledgeWaitingInfo p : _waitingList.values())
		{
			// TODO: Handle Role.
			final int playerLvl = p.getPlayerLvl();
			if ((playerLvl >= levelMin) && (playerLvl <= levelMax))
			{
				result.add(p);
			}
		}
		
		result.sort(descending ? PLAYER_COMPARATOR.get(sortBy).reversed() : PLAYER_COMPARATOR.get(sortBy));
		return result;
	}
	
	/**
	 * Retrieves a list of players from the waiting list based on their name.<br>
	 * The search is case-insensitive and matches any part of the name.
	 * @param name The string to search for within player names.
	 * @return A {@code List} of {@code PledgeWaitingInfo} objects that match the criteria.
	 */
	public List<PledgeWaitingInfo> queryWaitingListByName(String name)
	{
		final List<PledgeWaitingInfo> result = new ArrayList<>();
		for (PledgeWaitingInfo p : _waitingList.values())
		{
			if (p.getPlayerName().toLowerCase().contains(name))
			{
				result.add(p);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves a list of clans filtered by a search query.<br>
	 * The results are filtered based on the provided type.
	 * @param query The string to search for in names.
	 * @param type The filter type where 1 is clan name and any other value is leader name.
	 * @return A {@code List} of {@code PledgeRecruitInfo} matching the criteria.
	 */
	public List<PledgeRecruitInfo> getSortedClanListByName(String query, int type)
	{
		final List<PledgeRecruitInfo> result = new ArrayList<>();
		if (type == 1)
		{
			for (PledgeRecruitInfo p : _clanList.values())
			{
				if (p.getClanName().toLowerCase().contains(query))
				{
					result.add(p);
				}
			}
		}
		else
		{
			for (PledgeRecruitInfo p : _clanList.values())
			{
				if (p.getClanLeaderName().toLowerCase().contains(query))
				{
					result.add(p);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves the recruitment information for a specific clan.<br>
	 * This method looks up the data using the provided unique identifier.
	 * @param clanId The unique ID of the clan to search for.
	 * @return The {@code PledgeRecruitInfo} object associated with the ID, or {@code null} if not found.
	 */
	public PledgeRecruitInfo getClanById(int clanId)
	{
		return _clanList.get(clanId);
	}
	
	/**
	 * Checks if a specific clan is currently registered in the system.<br>
	 * It looks up the {@code clanId} in the internal clan list.
	 * @param clanId The unique identifier of the clan to check.
	 * @return {@code true} if the clan exists, otherwise {@code false}.
	 */
	public boolean isClanRegistred(int clanId)
	{
		return _clanList.get(clanId) != null;
	}
	
	/**
	 * Checks if a player is currently on the waiting list.<br>
	 * This method looks up the {@code playerId} in the internal waiting list map.
	 * @param playerId The unique identifier of the player to check.
	 * @return {@code true} if the player is found in the waiting list, {@code false} otherwise.
	 */
	public boolean isPlayerRegistred(int playerId)
	{
		return _waitingList.get(playerId) != null;
	}
	
	/**
	 * Retrieves all clan recruitment information from the internal list.<br>
	 * The returned list is not sorted in any specific order.
	 * @return a {@code List} of {@code PledgeRecruitInfo} objects.
	 */
	public List<PledgeRecruitInfo> getUnSortedClanList()
	{
		return new ArrayList<>(_clanList.values());
	}
	
	/**
	 * Retrieves a list of clans filtered by level and karma.<br>
	 * The results are sorted based on the provided criteria.
	 * @param clanLevel The minimum required level. Use -1 to ignore this filter.
	 * @param karma The minimum required karma. Use -1 to ignore this filter.
	 * @param sortByValue The index of the comparator to use for sorting.
	 * @param descending True to sort in descending order, false for ascending.
	 * @return A list of {@code PledgeRecruitInfo} objects matching the filters.
	 */
	public List<PledgeRecruitInfo> getSortedClanList(int clanLevel, int karma, int sortByValue, boolean descending)
	{
		final int sortBy = Math.clamp(sortByValue, 1, CLAN_COMPARATOR.size() - 1);
		final List<PledgeRecruitInfo> sortedList = new ArrayList<>(_clanList.values());
		for (int i = 0; i < sortedList.size(); i++)
		{
			final PledgeRecruitInfo currentInfo = sortedList.get(i);
			if (((clanLevel < 0) && (karma >= 0) && (karma != currentInfo.getKarma())) || ((clanLevel >= 0) && (karma < 0) && (clanLevel != (currentInfo.getClan() != null ? currentInfo.getClanLevel() : 0))) || ((clanLevel >= 0) && (karma >= 0) && ((clanLevel != (currentInfo.getClan() != null ? currentInfo.getClanLevel() : 0)) || (karma != currentInfo.getKarma()))))
			{
				sortedList.remove(i--);
			}
		}
		
		sortedList.sort(descending ? CLAN_COMPARATOR.get(sortBy).reversed() : CLAN_COMPARATOR.get(sortBy));
		return sortedList;
	}
	
	/**
	 * Retrieves the remaining lock time for a specific player.<br>
	 * This method checks if the player is currently locked in the {@code _playerLocked} map.<br>
	 * If no lock exists, it returns 0.
	 * @param playerId The unique identifier of the player to check.
	 * @return The remaining lock time in minutes, or 0 if the player is not locked.
	 */
	public long getPlayerLockTime(int playerId)
	{
		return _playerLocked.get(playerId) == null ? 0 : _playerLocked.get(playerId).getDelay(TimeUnit.MINUTES);
	}
	
	/**
	 * Retrieves the remaining lock time for a player's clan.<br>
	 * It returns the delay in minutes if a lock exists.<br>
	 * If no lock is active, it returns {@code 0}.
	 * @param playerId The unique identifier of the player.
	 * @return The remaining lock time in minutes as a {@code long}.
	 */
	public long getClanLockTime(int playerId)
	{
		return _clanLocked.get(playerId) == null ? 0 : _clanLocked.get(playerId).getDelay(TimeUnit.MINUTES);
	}
	
	/**
	 * Locks a specific player to prevent concurrent modifications.<br>
	 * This method schedules a task to remove the lock after {@code LOCK_TIME}.
	 * @param playerId The unique identifier of the player to lock.
	 */
	private void lockPlayer(int playerId)
	{
		_playerLocked.put(playerId, ThreadPool.schedule(() -> _playerLocked.remove(playerId), LOCK_TIME));
	}
	
	/**
	 * This method locks a specific clan for a set period of time.<br>
	 * It uses the {@code ThreadPool} to schedule an automatic unlock.<br>
	 * The lock is stored in the {@code _clanLocked} map.
	 * @param clanId The unique identifier of the clan to be locked.
	 */
	private void lockClan(int clanId)
	{
		_clanLocked.put(clanId, ThreadPool.schedule(() -> _clanLocked.remove(clanId), LOCK_TIME));
	}
	
	/**
	 * Retrieves the singleton instance of the {@code ClanEntryManager}.<br>
	 * This provides a global access point to manage clan entries.
	 * @return The active {@code ClanEntryManager} instance.
	 */
	public static ClanEntryManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ClanEntryManager INSTANCE = new ClanEntryManager();
	}
}
