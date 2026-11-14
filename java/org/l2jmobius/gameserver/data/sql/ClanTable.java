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
package org.l2jmobius.gameserver.data.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.communitybbs.Manager.ForumsBBSManager;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.ClanHallData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.entity.clan.ClanPrivileges;
import org.l2jmobius.gameserver.entity.clan.ClanWar;
import org.l2jmobius.gameserver.entity.clan.enums.ClanWarState;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.residences.ClanHall;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.ClanEntryManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.FortSiegeManager;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerClanCreate;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerClanDestroy;
import org.l2jmobius.gameserver.mechanics.events.holders.clan.OnClanWarFinish;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.siege.FortSiege;
import org.l2jmobius.gameserver.mechanics.siege.Siege;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.UserInfoType;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListAll;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This class handles the loading and management of all clan-related data from the database.<br>
 * It provides access to persistent information for clans, members, and associated structures.
 */
public class ClanTable
{
	private static final Logger LOGGER = Logger.getLogger(ClanTable.class.getName());
	
	private final Map<Integer, Clan> _clans = new ConcurrentHashMap<>();
	private final Map<String, Clan> _clansByName = new ConcurrentHashMap<>();
	
	/**
	 * Initializes the {@code ClanTable} by loading data from the database.<br>
	 * It populates the clan list and sets up background tasks for rank updates.<br>
	 * This constructor also initializes community board data if enabled in configuration.
	 */
	protected ClanTable()
	{
		// Forums has to be loaded before clan data, because of last forum id used should have also memo included.
		if (GeneralConfig.ENABLE_COMMUNITY_BOARD)
		{
			ForumsBBSManager.getInstance().initRoot();
		}
		
		// Get all clan ids.
		final List<Integer> cids = new ArrayList<>();
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT clan_id FROM clan_data"))
		{
			while (rs.next())
			{
				cids.add(rs.getInt("clan_id"));
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error restoring ClanTable.", e);
		}
		
		// Initialize clans.
		for (int cid : cids)
		{
			final Clan clan = new Clan(cid);
			_clans.put(cid, clan);
			
			final String name = clan.getName();
			if (name != null)
			{
				_clansByName.putIfAbsent(name.toLowerCase(), clan);
			}
			
			if (clan.getDissolvingExpiryTime() != 0)
			{
				scheduleRemoveClan(clan.getId());
			}
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Restored " + cids.size() + " clans from the database.");
		allianceCheck();
		restoreClanWars();
		
		ThreadPool.scheduleAtFixedRate(this::updateClanRanks, 1000, 1200000); // 20 minutes.
	}
	
	/**
	 * Retrieves all clans currently stored in the system.
	 * @return a {@code Collection} of {@code Clan} objects.
	 */
	public Collection<Clan> getClans()
	{
		return _clans.values();
	}
	
	/**
	 * Retrieves the total number of clans currently in the system.<br>
	 * This method returns the size of the internal clan collection.
	 * @return The total count of clans as an {@code int}.
	 */
	public int getClanCount()
	{
		return _clans.size();
	}
	
	/**
	 * Retrieves a {@code Clan} object based on its unique identifier.<br>
	 * This method looks up the clan in the internal data map.
	 * @param clanId The unique ID of the clan to retrieve.
	 * @return The {@code Clan} associated with the provided ID, or {@code null} if not found.
	 */
	public Clan getClan(int clanId)
	{
		return _clans.get(clanId);
	}
	
	/**
	 * Retrieves a {@code Clan} object based on its name.<br>
	 * The search is case-insensitive.
	 * @param clanName The name of the clan to find.
	 * @return The matching {@code Clan} object, or {@code null} if not found.
	 */
	public Clan getClanByName(String clanName)
	{
		if (clanName == null)
		{
			return null;
		}
		
		return _clansByName.get(clanName.toLowerCase());
	}
	
	/**
	 * Creates a new {@code Clan} for the specified player.<br>
	 * This method validates the player level, existing membership, and name requirements.<br>
	 * It also checks if the clan name is unique before saving to the database.
	 * @param player The {@code Player} attempting to create the clan.
	 * @param clanName The desired name for the new clan.
	 * @return The created {@code Clan} object, or {@code null} if creation fails.
	 */
	public Clan createClan(Player player, String clanName)
	{
		if (player == null)
		{
			return null;
		}
		
		if (player.getLevel() < 10)
		{
			player.sendPacket(SystemMessageId.CURRENTLY_YOU_CANNOT_CREATE_A_CLAN);
			return null;
		}
		
		if (player.getClanId() != 0)
		{
			player.sendPacket(SystemMessageId.FAILED_TO_CREATE_A_CLAN);
			return null;
		}
		
		if (System.currentTimeMillis() < player.getClanCreateExpiryTime())
		{
			player.sendPacket(SystemMessageId.YOU_MUST_WAIT_10_DAYS_BEFORE_CREATING_A_NEW_CLAN);
			return null;
		}
		
		final int nameLength = clanName.length();
		if (!StringUtil.isAlphaNumeric(clanName) || (nameLength < 2))
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_IS_INVALID);
			return null;
		}
		
		if (nameLength > 16)
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_S_LENGTH_IS_INCORRECT);
			return null;
		}
		
		final Clan clan = new Clan(IdManager.getInstance().getNextId(), clanName);
		if (_clansByName.putIfAbsent(clanName.toLowerCase(), clan) != null)
		{
			// Clan name is already taken (atomic check + claim).
			IdManager.getInstance().releaseId(clan.getId());
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_ALREADY_EXISTS);
			sm.addString(clanName);
			player.sendPacket(sm);
			return null;
		}
		
		final ClanMember leader = new ClanMember(clan, player);
		clan.setLeader(leader);
		leader.setPlayer(player);
		clan.store();
		player.setClan(clan);
		player.setPledgeClass(ClanMember.calculatePledgeClass(player));
		
		final ClanPrivileges privileges = new ClanPrivileges();
		privileges.enableAll();
		player.setClanPrivileges(privileges);
		
		_clans.put(clan.getId(), clan);
		
		// Should be update packet only.
		player.sendPacket(new PledgeShowInfoUpdate(clan));
		PledgeShowMemberListAll.sendAllTo(player);
		player.sendPacket(new PledgeShowMemberListUpdate(player));
		player.sendPacket(SystemMessageId.YOUR_CLAN_HAS_BEEN_CREATED);
		player.broadcastUserInfo(UserInfoType.RELATION, UserInfoType.CLAN);
		
		// Notify to scripts.
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_CLAN_CREATE))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanCreate(player, clan));
		}
		
		return clan;
	}
	
	/**
	 * Removes a clan from the game and deletes its data.<br>
	 * This method handles all cleanup tasks including members, items, and database records.<br>
	 * It also notifies online members of the dispersal.
	 * @param clanId The unique identifier of the clan to be destroyed.
	 */
	public synchronized void destroyClan(int clanId)
	{
		final Clan clan = getClan(clanId);
		if (clan == null)
		{
			return;
		}
		
		clan.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.THE_CLAN_IS_DISBANDED));
		
		// The wars are deleted from the database below; forget them in memory as well.
		for (Clan other : getClans())
		{
			other.deleteWar(clan.getId());
		}
		
		ClanEntryManager.getInstance().removeFromClanList(clan.getId());
		
		final int castleId = clan.getCastleId();
		if (castleId == 0)
		{
			for (Siege siege : SiegeManager.getInstance().getSieges())
			{
				siege.removeSiegeClan(clan);
			}
		}
		else
		{
			// A disbanded clan must not keep its castle (removeOwner also clears circlets and residential skills).
			final Castle castle = CastleManager.getInstance().getCastleById(castleId);
			if ((castle != null) && (castle.getOwnerId() == clanId))
			{
				castle.removeOwner(clan);
			}
		}
		
		final int fortId = clan.getFortId();
		if (fortId == 0)
		{
			for (FortSiege siege : FortSiegeManager.getInstance().getSieges())
			{
				siege.removeAttacker(clan);
			}
		}
		
		final ClanHall hall = ClanHallData.getInstance().getClanHallByClan(clan);
		if (hall != null)
		{
			hall.setOwner(null);
		}
		
		final ClanMember leaderMember = clan.getLeader();
		if (leaderMember == null)
		{
			clan.getWarehouse().destroyAllItems(ItemProcessType.DESTROY, null, null);
		}
		else
		{
			clan.getWarehouse().destroyAllItems(ItemProcessType.DESTROY, leaderMember.getPlayer(), null);
		}
		
		for (ClanMember member : clan.getMembers())
		{
			clan.removeClanMember(member.getObjectId(), 0);
		}
		
		_clans.remove(clanId);
		final String name = clan.getName();
		if (name != null)
		{
			_clansByName.remove(name.toLowerCase(), clan);
		}
		
		IdManager.getInstance().releaseId(clanId);
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM clan_data WHERE clan_id=?"))
			{
				ps.setInt(1, clanId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM clan_privs WHERE clan_id=?"))
			{
				ps.setInt(1, clanId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM clan_skills WHERE clan_id=?"))
			{
				ps.setInt(1, clanId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM clan_subpledges WHERE clan_id=?"))
			{
				ps.setInt(1, clanId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM clan_wars WHERE clan1=? OR clan2=?"))
			{
				ps.setInt(1, clanId);
				ps.setInt(2, clanId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM clan_notices WHERE clan_id=?"))
			{
				ps.setInt(1, clanId);
				ps.execute();
			}
			
			if (fortId != 0)
			{
				final Fort fort = FortManager.getInstance().getFortById(fortId);
				if (fort != null)
				{
					final Clan owner = fort.getOwnerClan();
					if (clan == owner)
					{
						fort.removeOwner(true);
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Error removing clan from DB.", e);
		}
		
		// Notify to scripts.
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_CLAN_DESTROY))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanDestroy(leaderMember, clan));
		}
	}
	
	/**
	 * Schedules the removal of a clan after a specific delay.<br>
	 * This method checks if the clan exists and calculates the remaining time until it should be destroyed.<br>
	 * It uses a {@code ThreadPool} to execute the {@code destroyClan} action automatically.
	 * @param clanId The unique identifier of the clan to be removed.
	 */
	public void scheduleRemoveClan(int clanId)
	{
		final Clan clan = getClan(clanId);
		if (clan == null)
		{
			return;
		}
		
		final long delay = Math.max(clan.getDissolvingExpiryTime() - System.currentTimeMillis(), 300000);
		ThreadPool.schedule(() ->
		{
			final Clan c = getClan(clanId);
			if ((c != null) && (c.getDissolvingExpiryTime() != 0))
			{
				destroyClan(clanId);
			}
		}, delay);
	}
	
	/**
	 * Checks if any clan has a specific ally name assigned.<br>
	 * This method iterates through all clans to find a match.
	 * @param allyName The name of the ally to search for.
	 * @return {@code true} if a matching ally name is found, otherwise {@code false}.
	 */
	public boolean isAllyExists(String allyName)
	{
		for (Clan clan : _clans.values())
		{
			final String clanAllyName = clan.getAllyName();
			if ((clanAllyName != null) && clanAllyName.equalsIgnoreCase(allyName))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Saves the details of a {@code ClanWar} to the database.<br>
	 * This method uses a {@code REPLACE INTO} statement to update or insert the war record.<br>
	 * It handles errors by logging them to the server log.
	 * @param war The {@code ClanWar} object containing the data to be stored.
	 */
	public void storeClanWars(ClanWar war)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("REPLACE INTO clan_wars (clan1, clan2, clan1Kill, clan2Kill, winnerClan, startTime, endTime, state) VALUES(?,?,?,?,?,?,?,?)"))
		{
			ps.setInt(1, war.getAttackerClanId());
			ps.setInt(2, war.getAttackedClanId());
			ps.setInt(3, war.getAttackerKillCount());
			ps.setInt(4, war.getAttackedKillCount());
			ps.setInt(5, war.getWinnerClanId());
			ps.setLong(6, war.getStartTime());
			ps.setLong(7, war.getEndTime());
			ps.setInt(8, war.getState().ordinal());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.severe("Error storing clan wars data: " + e);
		}
	}
	
	/**
	 * Removes the war record between two specific clans.<br>
	 * This method updates both {@code Clan} objects and deletes the entry from the database.<br>
	 * It also triggers a broadcast to notify players of the status change.
	 * @param clanId1 The unique identifier for the first clan.
	 * @param clanId2 The unique identifier for the second clan.
	 */
	public void deleteClanWars(int clanId1, int clanId2)
	{
		final Clan clan1 = getClan(clanId1);
		final Clan clan2 = getClan(clanId2);
		
		if ((clan1 != null) && (clan2 != null))
		{
			if (EventDispatcher.getInstance().hasListener(EventType.ON_CLAN_WAR_FINISH))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnClanWarFinish(clan1, clan2));
			}
			
			clan1.deleteWar(clan2.getId());
			clan2.deleteWar(clan1.getId());
			clan1.broadcastClanStatus();
			clan2.broadcastClanStatus();
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM clan_wars WHERE (clan1=? AND clan2=?) OR (clan2=? AND clan1=?)"))
		{
			ps.setInt(1, clanId1);
			ps.setInt(2, clanId2);
			ps.setInt(3, clanId1);
			ps.setInt(4, clanId2);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Error removing clan wars data.", e);
		}
	}
	
	/**
	 * Loads all historical clan war records from the database.<br>
	 * It populates the {@code ClanWar} objects for each clan.<br>
	 * This method ensures that previous war data is available in memory.
	 */
	private void restoreClanWars()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement statement = con.createStatement();
			ResultSet rset = statement.executeQuery("SELECT clan1, clan2, clan1Kill, clan2Kill, winnerClan, startTime, endTime, state FROM clan_wars"))
		{
			while (rset.next())
			{
				final Clan attacker = getClan(rset.getInt("clan1"));
				final Clan attacked = getClan(rset.getInt("clan2"));
				if ((attacker != null) && (attacked != null))
				{
					final ClanWarState state = ClanWarState.values()[rset.getInt("state")];
					final ClanWar clanWar = new ClanWar(attacker, attacked, rset.getInt("clan1Kill"), rset.getInt("clan2Kill"), rset.getInt("winnerClan"), rset.getLong("startTime"), rset.getLong("endTime"), state);
					attacker.addWar(attacked.getId(), clanWar);
					attacked.addWar(attacker.getId(), clanWar);
				}
				else
				{
					LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Restorewars one of clans is null attacker:" + attacker + " attacked:" + attacked);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Error restoring clan wars data.", e);
		}
	}
	
	/**
	 * Validates that every clan has a valid alliance association.<br>
	 * It checks if the ally ID belongs to an existing clan in the system.<br>
	 * If the ally does not exist, it resets the clan's alliance data and updates the database.
	 */
	private void allianceCheck()
	{
		for (Clan clan : _clans.values())
		{
			final int allyId = clan.getAllyId();
			if ((allyId != 0) && (clan.getId() != allyId) && !_clans.containsKey(allyId))
			{
				clan.setAllyId(0);
				clan.setAllyName(null);
				clan.changeAllyCrest(0, true);
				clan.updateClanInDB();
				LOGGER.info(getClass().getSimpleName() + ": Removed alliance from clan: " + clan);
			}
		}
	}
	
	/**
	 * Retrieves a list of all clans belonging to a specific alliance.<br>
	 * This method filters the existing clans based on the provided {@code allianceId}.<br>
	 * If the {@code allianceId} is 0, it returns an empty list.
	 * @param allianceId The unique identifier for the alliance.
	 * @return A {@code List} of {@code Clan} objects that are part of the specified alliance.
	 */
	public List<Clan> getClanAllies(int allianceId)
	{
		final List<Clan> clanAllies = new ArrayList<>();
		if (allianceId != 0)
		{
			for (Clan clan : _clans.values())
			{
				if ((clan != null) && (clan.getAllyId() == allianceId))
				{
					clanAllies.add(clan);
				}
			}
		}
		
		return clanAllies;
	}
	
	/**
	 * Shuts down the current database operations.<br>
	 * This method saves all active {@code Clan} data to the database.<br>
	 * It ensures that clan variables and war records are persisted.
	 */
	public void shutdown()
	{
		for (Clan clan : _clans.values())
		{
			clan.updateClanInDB();
			clan.getVariables().saveNow();
			for (ClanWar war : clan.getWarList().values())
			{
				storeClanWars(war);
			}
		}
	}
	
	/**
	 * Updates the rank of every {@code Clan} in the system.<br>
	 * Clans with a level lower than {@code 3} are assigned a rank of {@code 0}.<br>
	 * Other clans are ranked based on their level and reputation score.<br>
	 * Tied clans share the highest rank within their group.
	 */
	private void updateClanRanks()
	{
		// O(n log n) bulk rank pass; preserves tie semantics of getClanRank
		// (clans with equal level + equal reputation share the highest position in their tie group).
		final List<Clan> qualified = new ArrayList<>(_clans.size());
		for (Clan clan : _clans.values())
		{
			if (clan.getLevel() < 3)
			{
				clan.setRank(0);
			}
			else
			{
				qualified.add(clan);
			}
		}
		
		qualified.sort((a, b) ->
		{
			final int levelDiff = b.getLevel() - a.getLevel();
			if (levelDiff != 0)
			{
				return levelDiff;
			}
			
			return b.getReputationScore() - a.getReputationScore();
		});
		
		int i = 0;
		final int n = qualified.size();
		while (i < n)
		{
			final Clan currentClan = qualified.get(i);
			final int groupLevel = currentClan.getLevel();
			final int groupRep = currentClan.getReputationScore();
			int j = i + 1;
			while ((j < n) && (qualified.get(j).getLevel() == groupLevel) && (qualified.get(j).getReputationScore() == groupRep))
			{
				j++;
			}
			
			// Ties share the highest rank in the group (= j, the 1-based position of the last tied member).
			for (int k = i; k < j; k++)
			{
				qualified.get(k).setRank(j);
			}
			
			i = j;
		}
	}
	
	/**
	 * Retrieves the numerical rank of a specific {@code Clan}.<br>
	 * The rank is determined by comparing levels and reputation scores.<br>
	 * Clans with a level lower than 3 are assigned a rank of 0.
	 * @param clan The {@code Clan} object to check.
	 * @return The calculated rank as an {@code int}.
	 */
	public int getClanRank(Clan clan)
	{
		if (clan.getLevel() < 3)
		{
			return 0;
		}
		
		final int clanLevel = clan.getLevel();
		final int clanReputationScore = clan.getReputationScore();
		int rank = 1;
		for (Clan c : _clans.values())
		{
			if ((clan != c) && ((clanLevel < c.getLevel()) || ((clanLevel == c.getLevel()) && (clanReputationScore <= c.getReputationScore()))))
			{
				rank++;
			}
		}
		
		return rank;
	}
	
	/**
	 * Provides the singleton instance of the {@code ClanTable} class.<br>
	 * Use this method to access the global clan data manager.
	 * @return The singleton instance of {@code ClanTable}.
	 */
	public static ClanTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ClanTable INSTANCE = new ClanTable();
	}
}
