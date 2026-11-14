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
package org.l2jmobius.gameserver.entity.clan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.communitybbs.BB.Forum;
import org.l2jmobius.gameserver.communitybbs.Manager.ForumsBBSManager;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.holders.ClanMasteryHolder;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.sql.CrestTable;
import org.l2jmobius.gameserver.data.sql.PlayerInfoTable;
import org.l2jmobius.gameserver.data.xml.CategoryData;
import org.l2jmobius.gameserver.data.xml.ClanLevelData;
import org.l2jmobius.gameserver.data.xml.ClanMasteryData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.BlockList;
import org.l2jmobius.gameserver.entity.actor.holders.player.SubClassHolder;
import org.l2jmobius.gameserver.entity.itemcontainer.ClanWarehouse;
import org.l2jmobius.gameserver.entity.itemcontainer.ItemContainer;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerClanJoin;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerClanLeaderChange;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerClanLeft;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerClanLvlUp;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.siege.Siege;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.mechanics.variables.ClanVariables;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.ExSubPledgeSkillAdd;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListAll;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListDeleteAll;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PledgeSkillList;
import org.l2jmobius.gameserver.network.serverpackets.PledgeSkillList.SubPledgeSkill;
import org.l2jmobius.gameserver.network.serverpackets.PledgeSkillListAdd;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.pledgeV2.ExPledgeMissionInfo;
import org.l2jmobius.gameserver.network.serverpackets.pledgeV2.ExPledgeMissionRewardCount;
import org.l2jmobius.gameserver.network.serverpackets.pledgeV2.ExPledgeShowInfoUpdate;

/**
 * Represents a clan entity within the game world.<br>
 * This class manages clan-related data, members, and shared mechanics such as warehouses and skills.<br>
 * It handles all logic for clan progression, leadership, and interactions between players.
 */
public class Clan
{
	private static final Logger LOGGER = Logger.getLogger(Clan.class.getName());
	
	// Community board forums of the clan.
	public static final String ANNOUNCEMENT_BOARD = "Announcement";
	public static final String FREE_BOARD = "Free Board";
	
	// SQL queries
	private static final String INSERT_CLAN_DATA = "INSERT INTO clan_data (clan_id,clan_name,clan_level,hasCastle,blood_alliance_count,blood_oath_count,ally_id,ally_name,leader_id,crest_id,crest_large_id,ally_crest_id,new_leader_id) values (?,?,?,?,?,?,?,?,?,?,?,?,?)";
	private static final String SELECT_CLAN_DATA = "SELECT * FROM clan_data where clan_id=?";
	
	// Ally Penalty Types
	/** Clan leaved ally */
	public static final int PENALTY_TYPE_CLAN_LEAVED = 1;
	/** Clan was dismissed from ally */
	public static final int PENALTY_TYPE_CLAN_DISMISSED = 2;
	/** Leader clan dismiss clan from ally */
	public static final int PENALTY_TYPE_DISMISS_CLAN = 3;
	/** Leader clan dissolve ally */
	public static final int PENALTY_TYPE_DISSOLVE_ALLY = 4;
	
	// Clan types
	public static final int PLEDGE_CLASS_COMMON = 0;
	public static final int PLEDGE_CLASS_ELITE = 100;
	
	private String _name;
	private int _clanId;
	private ClanMember _leader;
	private final Map<Integer, ClanMember> _members = new ConcurrentHashMap<>();
	
	private String _allyName;
	private int _allyId = 0;
	private int _level;
	private int _castleId;
	private int _fortId;
	private int _hideoutId;
	private int _hiredGuards;
	private int _crestId;
	private int _crestLargeId;
	private int _allyCrestId;
	private int _auctionBiddedAt = 0;
	private long _allyPenaltyExpiryTime;
	private int _allyPenaltyType;
	private long _charPenaltyExpiryTime;
	private long _dissolvingExpiryTime;
	private int _bloodAllianceCount;
	private int _bloodOathCount;
	
	private final ItemContainer _warehouse = new ClanWarehouse(this);
	private final ConcurrentHashMap<Integer, ClanWar> _atWarWith = new ConcurrentHashMap<>();
	
	private Forum _forum;
	
	private final Map<Integer, Skill> _skills = new ConcurrentSkipListMap<>();
	private final Map<Integer, RankPrivs> _privs = new ConcurrentSkipListMap<>();
	private final Map<Integer, SubPledge> _subPledges = new ConcurrentSkipListMap<>();
	private final Map<Integer, Skill> _subPledgeSkills = new ConcurrentSkipListMap<>();
	
	private int _reputationScore = 0;
	private int _rank = 0;
	
	private String _notice;
	private boolean _noticeEnabled = false;
	private static final int MAX_NOTICE_LENGTH = 8192;
	private int _newLeaderId;
	
	private final AtomicInteger _siegeKills = new AtomicInteger();
	private final AtomicInteger _siegeDeaths = new AtomicInteger();
	
	private final Collection<ScheduledFuture<?>> masterySkillTasks = ConcurrentHashMap.newKeySet();
	
	private ClanVariables _vars;
	
	/**
	 * Creates a new {@code Clan} instance using the provided unique identifier.<br>
	 * This constructor initializes the clan's privileges and restores its data from the database.<br>
	 * It also sets up initial reward bonuses for online members and hunting monsters.
	 * @param clanId The unique identification number of the clan.
	 */
	public Clan(int clanId)
	{
		_clanId = clanId;
		initializePrivs();
		restore();
		_warehouse.restore();
		
		final int masteryTime19538 = getMasterySkillRemainingTime(19538);
		if (masteryTime19538 > 0)
		{
			final ScheduledFuture<?> task = ThreadPool.schedule(() -> removeMasterySkill(19538), masteryTime19538);
			masterySkillTasks.add(task);
		}
		
		final int masteryTime19539 = getMasterySkillRemainingTime(19539);
		if (masteryTime19539 > 0)
		{
			final ScheduledFuture<?> task = ThreadPool.schedule(() -> removeMasterySkill(19539), masteryTime19539);
			masterySkillTasks.add(task);
		}
		
		final int masteryTime19540 = getMasterySkillRemainingTime(19540);
		if (masteryTime19540 > 0)
		{
			final ScheduledFuture<?> task = ThreadPool.schedule(() -> removeMasterySkill(19540), masteryTime19540);
			masterySkillTasks.add(task);
		}
		
		final int masteryTime19541 = getMasterySkillRemainingTime(19541);
		if (masteryTime19541 > 0)
		{
			final ScheduledFuture<?> task = ThreadPool.schedule(() -> removeMasterySkill(19541), masteryTime19541);
			masterySkillTasks.add(task);
		}
		
		final int masteryTime19542 = getMasterySkillRemainingTime(19542);
		if (masteryTime19542 > 0)
		{
			final ScheduledFuture<?> task = ThreadPool.schedule(() -> removeMasterySkill(19542), masteryTime19542);
			masterySkillTasks.add(task);
		}
	}
	
	/**
	 * Creates a new {@code Clan} instance with a specific ID and name.<br>
	 * This constructor also initializes the clan privileges.
	 * @param clanId The unique identifier for the clan.
	 * @param clanName The display name of the clan.
	 */
	public Clan(int clanId, String clanName)
	{
		_clanId = clanId;
		_name = clanName;
		initializePrivs();
	}
	
	/**
	 * Retrieves the unique identifier for this clan.
	 * @return the {@code int} ID of the clan.
	 */
	public int getId()
	{
		return _clanId;
	}
	
	/**
	 * Sets the unique identifier for this clan.<br>
	 * This updates the internal {@code _clanId} field.
	 * @param clanId The new unique ID to assign to the clan.
	 */
	public void setClanId(int clanId)
	{
		_clanId = clanId;
	}
	
	/**
	 * Retrieves the unique identifier of the clan leader.<br>
	 * Returns {@code 0} if there is no leader assigned.
	 * @return The {@code int} object ID of the leader.
	 */
	public int getLeaderId()
	{
		return _leader != null ? _leader.getObjectId() : 0;
	}
	
	/**
	 * Retrieves the current leader of the clan.
	 * @return the {@code ClanMember} object representing the leader.
	 */
	public ClanMember getLeader()
	{
		return _leader;
	}
	
	/**
	 * Sets the current leader of the clan.<br>
	 * This method updates the {@code _leader} field.<br>
	 * It also adds the new leader to the internal members collection.
	 * @param leader The {@code ClanMember} object to be set as the new leader.
	 */
	public void setLeader(ClanMember leader)
	{
		_leader = leader;
		_members.put(leader.getObjectId(), leader);
	}
	
	/**
	 * Sets a new leader for the clan.<br>
	 * This method handles all privilege transfers and updates for both old and new leaders.<br>
	 * It also notifies listeners and broadcasts the change to online members.
	 * @param member The {@code ClanMember} who will become the new leader.
	 */
	public void setNewLeader(ClanMember member)
	{
		final Player newLeader = member.getPlayer();
		final ClanMember exMember = _leader;
		final Player exLeader = exMember.getPlayer();
		
		// Notify to scripts.
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_CLAN_LEADER_CHANGE))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanLeaderChange(exMember, member, this));
		}
		
		if (exLeader != null)
		{
			if (exLeader.isFlying())
			{
				exLeader.dismount();
			}
			
			if (getLevel() >= SiegeManager.getInstance().getSiegeClanMinLevel())
			{
				SiegeManager.getInstance().removeSiegeSkills(exLeader);
			}
			
			exLeader.getClanPrivileges().disableAll();
			exLeader.broadcastUserInfo();
		}
		else
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("UPDATE characters SET clan_privs = ? WHERE charId = ?"))
			{
				ps.setInt(1, 0);
				ps.setInt(2, getLeaderId());
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Couldn't update clan privs for old clan leader", e);
			}
		}
		
		setLeader(member);
		if (_newLeaderId != 0)
		{
			setNewLeaderId(0, true);
		}
		
		updateClanInDB();
		
		if (exLeader != null)
		{
			exLeader.setPledgeClass(PLEDGE_CLASS_COMMON);
			exLeader.broadcastUserInfo();
			exLeader.checkItemRestriction();
		}
		
		if (newLeader != null)
		{
			newLeader.setPledgeClass(PLEDGE_CLASS_COMMON);
			newLeader.getClanPrivileges().enableAll();
			
			if (getLevel() >= SiegeManager.getInstance().getSiegeClanMinLevel())
			{
				SiegeManager.getInstance().addSiegeSkills(newLeader);
			}
			
			newLeader.broadcastUserInfo();
		}
		else
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("UPDATE characters SET clan_privs = ? WHERE charId = ?"))
			{
				ps.setInt(1, ClanPrivileges.getCompleteMask());
				ps.setInt(2, getLeaderId());
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Couldn't update clan privs for new clan leader", e);
			}
		}
		
		broadcastClanStatus();
		broadcastToOnlineMembers(new SystemMessage(SystemMessageId.CLAN_LEADER_PRIVILEGES_HAVE_BEEN_TRANSFERRED_TO_C1).addString(member.getName()));
		
		LOGGER.info("Leader of Clan: " + getName() + " changed to: " + member.getName() + " ex leader: " + exMember.getName());
	}
	
	/**
	 * Retrieves the name of the current clan leader.<br>
	 * If no leader exists, it returns an empty {@code String}.
	 * @return The name of the leader as a {@code String}.
	 */
	public String getLeaderName()
	{
		if (_leader == null)
		{
			LOGGER.warning("Clan " + _name + " without clan leader!");
			return "";
		}
		
		return _leader.getName();
	}
	
	/**
	 * Retrieves the name of the clan.
	 * @return The {@code String} name of the clan.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Sets the name of the clan.<br>
	 * This updates the internal {@code _name} field with the provided value.
	 * @param name The new name to assign to the clan.
	 */
	public void setName(String name)
	{
		_name = name;
	}
	
	/**
	 * Adds a new {@code ClanMember} to the internal collection.<br>
	 * This method updates the list of members for this clan.
	 * @param member The {@code ClanMember} object to be added.
	 */
	private void addClanMember(ClanMember member)
	{
		_members.put(member.getObjectId(), member);
	}
	
	/**
	 * Adds a new {@code Player} to the clan.<br>
	 * This method creates a {@code ClanMember} object for the player.<br>
	 * It updates the player's status and sends necessary network packets.<br>
	 * It also notifies any active listeners of the join event.
	 * @param player The {@code Player} to be added to the clan.
	 */
	public void addClanMember(Player player)
	{
		final ClanMember member = new ClanMember(this, player);
		
		// Store in memory.
		addClanMember(member);
		member.setPlayer(player);
		player.setClan(this);
		player.setPledgeClass(PLEDGE_CLASS_COMMON);
		player.sendPacket(new PledgeShowMemberListUpdate(player));
		player.sendPacket(new PledgeSkillList(this));
		player.sendPacket(new ExPledgeMissionRewardCount(player));
		player.sendPacket(new ExPledgeMissionInfo(player));
		addSkillEffects(player);
		
		// Notify to scripts.
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_CLAN_JOIN))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanJoin(member, this));
		}
	}
	
	/**
	 * Updates the clan membership information for a specific {@code Player}.<br>
	 * This method creates a new {@code ClanMember} object.<br>
	 * It also sets the leader if the player is currently the leader.<br>
	 * Finally, it adds the member to the internal collection.
	 * @param player The {@code Player} whose clan membership needs updating.
	 */
	public void updateClanMember(Player player)
	{
		final ClanMember member = new ClanMember(player.getClan(), player);
		if (player.isClanLeader())
		{
			setLeader(member);
		}
		
		addClanMember(member);
	}
	
	/**
	 * Finds a {@code ClanMember} by their name.<br>
	 * It searches through all members of the clan.
	 * @param name The name of the member to find.
	 * @return The matching {@code ClanMember} object, or {@code null} if no match is found.
	 */
	public ClanMember getClanMember(String name)
	{
		for (ClanMember temp : _members.values())
		{
			if (temp.getName().equals(name))
			{
				return temp;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves a {@code ClanMember} based on their unique object ID.
	 * @param objectId The unique identifier of the clan member to find.
	 * @return The {@code ClanMember} associated with the provided {@code objectId}, or {@code null} if not found.
	 */
	public ClanMember getClanMember(int objectId)
	{
		return _members.get(objectId);
	}
	
	/**
	 * Removes a member from the clan based on their unique object ID.<br>
	 * This method handles all necessary cleanup for apprentices, sponsors, and sub-pledge leaders.<br>
	 * It also updates player skills, titles, and database records if the member is online or offline.
	 * @param objectId The unique identifier of the clan member to remove.
	 * @param clanJoinExpiryTime The timestamp used to set the new join expiry time for the player.
	 */
	public void removeClanMember(int objectId, long clanJoinExpiryTime)
	{
		final ClanMember exMember = _members.remove(objectId);
		if (exMember == null)
		{
			LOGGER.warning("Member Object ID: " + objectId + " not found in clan while trying to remove");
			return;
		}
		
		final int leadssubpledge = getLeaderSubPledge(objectId);
		if (leadssubpledge != 0)
		{
			// The sub-unit leader has withdrawn, leaving the position vacant; the leader should appoint a new one via an NPC.
			getSubPledge(leadssubpledge).setLeaderId(0);
			updateSubPledgeInDB(leadssubpledge);
		}
		
		if (exMember.getApprentice() != 0)
		{
			final ClanMember apprentice = getClanMember(exMember.getApprentice());
			if (apprentice != null)
			{
				if (apprentice.getPlayer() != null)
				{
					apprentice.getPlayer().setSponsor(0);
				}
				else
				{
					apprentice.setApprenticeAndSponsor(0, 0);
				}
				
				apprentice.saveApprenticeAndSponsor(0, 0);
			}
		}
		
		if (exMember.getSponsor() != 0)
		{
			final ClanMember sponsor = getClanMember(exMember.getSponsor());
			if (sponsor != null)
			{
				if (sponsor.getPlayer() != null)
				{
					sponsor.getPlayer().setApprentice(0);
				}
				else
				{
					sponsor.setApprenticeAndSponsor(0, 0);
				}
				
				sponsor.saveApprenticeAndSponsor(0, 0);
			}
		}
		
		exMember.saveApprenticeAndSponsor(0, 0);
		
		if (PlayerConfig.REMOVE_CASTLE_CIRCLETS)
		{
			CastleManager.getInstance().removeCirclet(exMember, getCastleId());
		}
		
		if (exMember.isOnline())
		{
			final Player player = exMember.getPlayer();
			if (player.getNobleLevel() == 0)
			{
				player.setTitle("");
			}
			
			player.setApprentice(0);
			player.setSponsor(0);
			
			player.getVariables().remove(PlayerVariables.CLAN_CONTRIBUTION);
			player.getVariables().remove(PlayerVariables.CLAN_CONTRIBUTION_TOTAL);
			
			if (player.isClanLeader())
			{
				SiegeManager.getInstance().removeSiegeSkills(player);
				player.setClanCreateExpiryTime(System.currentTimeMillis() + (PlayerConfig.ALT_CLAN_CREATE_DAYS * 86400000)); // 24*60*60*1000 = 86400000
			}
			
			// Remove Clan skills from Player.
			removeSkillEffects(player);
			player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, 19009);
			
			// Remove Residential skills.
			if (getCastleId() > 0)
			{
				final Castle castle = CastleManager.getInstance().getCastleByOwner(this);
				if (castle != null)
				{
					castle.removeResidentialSkills(player);
				}
			}
			
			if (getFortId() > 0)
			{
				final Fort fort = FortManager.getInstance().getFortByOwner(this);
				if (fort != null)
				{
					fort.removeResidentialSkills(player);
				}
			}
			
			player.sendSkillList();
			player.setClan(null);
			
			// Players leaving from clan academy have no penalty.
			if (exMember.getPledgeType() != -1)
			{
				player.setClanJoinExpiryTime(clanJoinExpiryTime);
			}
			
			player.broadcastUserInfo();
			
			// Disable clan tab.
			player.sendPacket(PledgeShowMemberListDeleteAll.STATIC_PACKET);
		}
		else
		{
			removeMemberInDatabase(exMember, clanJoinExpiryTime, getLeaderId() == objectId ? System.currentTimeMillis() + (PlayerConfig.ALT_CLAN_CREATE_DAYS * 86400000) : 0);
		}
		
		// Notify to scripts.
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_CLAN_LEFT))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanLeft(exMember, this));
		}
	}
	
	/**
	 * Retrieves all members belonging to this clan.
	 * @return a {@code Collection} of {@code ClanMember} objects.
	 */
	public Collection<ClanMember> getMembers()
	{
		return _members.values();
	}
	
	/**
	 * Retrieves the total number of members in this clan.<br>
	 * This method returns the size of the internal member collection.
	 * @return The count of all members currently in the clan as an {@code int}.
	 */
	public int getMembersCount()
	{
		return _members.size();
	}
	
	/**
	 * Calculates the number of members belonging to a specific pledge type.<br>
	 * It iterates through all current clan members and counts those that match the provided ID.
	 * @param subpl The pledge type identifier to filter by.
	 * @return The total count of members with the matching pledge type.
	 */
	public int getSubPledgeMembersCount(int subpl)
	{
		int result = 0;
		for (ClanMember temp : _members.values())
		{
			if (temp.getPledgeType() == subpl)
			{
				result++;
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves a list of all online members belonging to this clan.<br>
	 * It filters out the member with the specified object ID.
	 * @param exclude The {@code int} object ID of the member to ignore.
	 * @return A {@code List} of {@code Player} objects who are currently online.
	 */
	public List<Player> getOnlineMembers(int exclude)
	{
		final List<Player> result = new ArrayList<>();
		for (ClanMember member : _members.values())
		{
			if ((member.getObjectId() != exclude) && member.isOnline())
			{
				final Player player = member.getPlayer();
				if (player != null)
				{
					result.add(player);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves the total number of members who are currently online.<br>
	 * This method iterates through all clan members and checks their status.
	 * @return The count of online members as an {@code int}.
	 */
	public int getOnlineMembersCount()
	{
		int count = 0;
		for (ClanMember member : _members.values())
		{
			if (member.isOnline())
			{
				count++;
			}
		}
		
		return count;
	}
	
	/**
	 * Retrieves the unique identifier of the allied clan.
	 * @return the {@code int} ID of the ally.
	 */
	public int getAllyId()
	{
		return _allyId;
	}
	
	/**
	 * Retrieves the name of the clan's ally.
	 * @return The {@code String} name of the ally.
	 */
	public String getAllyName()
	{
		return _allyName;
	}
	
	/**
	 * Sets the unique identifier for the alliance crest.<br>
	 * This value is used to identify which crest belongs to the allied clan.
	 * @param allyCrestId The {@code int} ID of the alliance crest.
	 */
	public void setAllyCrestId(int allyCrestId)
	{
		_allyCrestId = allyCrestId;
	}
	
	/**
	 * Retrieves the unique identifier for the clan's ally crest.
	 * @return the {@code int} value of the ally crest ID.
	 */
	public int getAllyCrestId()
	{
		return _allyCrestId;
	}
	
	/**
	 * Retrieves the current level of the clan.
	 * @return the current level as an {@code int}
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Updates the clan level and initializes the community board.<br>
	 * This method checks if the level is at least {@code 2}.<br>
	 * It creates a new forum if it does not already exist.
	 * @param level The new level to set for the clan.
	 */
	private void setLevel(int level)
	{
		_level = level;
		if ((_level >= 2) && (_forum == null) && GeneralConfig.ENABLE_COMMUNITY_BOARD)
		{
			final Forum forum = ForumsBBSManager.getInstance().getForumByName("ClanRoot");
			if (forum != null)
			{
				_forum = forum.getChildByName(_name);
				if (_forum == null)
				{
					_forum = ForumsBBSManager.getInstance().createNewForum(_name, ForumsBBSManager.getInstance().getForumByName("ClanRoot"), Forum.CLAN, Forum.CLANMEMBERONLY, getId());
				}
			}
		}
	}
	
	/**
	 * Retrieves a community board forum of this clan, creating it on first use.
	 * @param name the board name, {@code ANNOUNCEMENT_BOARD} or {@code FREE_BOARD}.
	 * @return the board {@code Forum}, or {@code null} while the clan has no forum.
	 */
	public Forum getBoard(String name)
	{
		if (_forum == null)
		{
			return null;
		}
		
		final Forum board = _forum.getChildByName(name);
		return (board != null) ? board : ForumsBBSManager.getInstance().createNewForum(name, _forum, Forum.CLAN, Forum.CLANMEMBERONLY, getId());
	}
	
	/**
	 * Retrieves the unique identifier for the castle.
	 * @return the {@code int} value of the castle ID.
	 */
	public int getCastleId()
	{
		return _castleId;
	}
	
	/**
	 * Retrieves the unique identifier for the clan fort.
	 * @return the {@code int} value of the fort ID.
	 */
	public int getFortId()
	{
		return _fortId;
	}
	
	/**
	 * Retrieves the unique identifier for the clan hideout.
	 * @return the {@code int} ID of the hideout.
	 */
	public int getHideoutId()
	{
		return _hideoutId;
	}
	
	/**
	 * Sets the unique identifier for the clan's crest.<br>
	 * This value is stored in the {@code _crestId} field.
	 * @param crestId The new ID to assign to the crest.
	 */
	public void setCrestId(int crestId)
	{
		_crestId = crestId;
	}
	
	/**
	 * Retrieves the unique identifier for the clan crest.
	 * @return the {@code int} value of the crest ID.
	 */
	public int getCrestId()
	{
		return _crestId;
	}
	
	/**
	 * Sets the unique identifier for the large crest.<br>
	 * This value is used to identify which large crest belongs to the clan.
	 * @param crestLargeId The {@code int} ID of the large crest.
	 */
	public void setCrestLargeId(int crestLargeId)
	{
		_crestLargeId = crestLargeId;
	}
	
	/**
	 * Retrieves the unique identifier for the large crest.
	 * @return the {@code int} value of the large crest ID.
	 */
	public int getCrestLargeId()
	{
		return _crestLargeId;
	}
	
	/**
	 * Sets the unique identifier for the clan's ally.<br>
	 * This value is stored in the {@code _allyId} field.
	 * @param allyId The unique ID of the allied clan.
	 */
	public void setAllyId(int allyId)
	{
		_allyId = allyId;
	}
	
	/**
	 * Sets the name of the ally for this clan.<br>
	 * This updates the {@code _allyName} field with the provided value.
	 * @param allyName The new name to assign to the ally.
	 */
	public void setAllyName(String allyName)
	{
		_allyName = allyName;
	}
	
	/**
	 * Sets the unique identifier for the clan's castle.<br>
	 * This updates the {@code _castleId} field within the {@code Clan} object.
	 * @param castleId The new ID to assign to the castle.
	 */
	public void setCastleId(int castleId)
	{
		_castleId = castleId;
	}
	
	/**
	 * Sets the unique identifier for the clan's fort.<br>
	 * This updates the internal {@code _fortId} field.
	 * @param fortId The new ID to assign to the fort.
	 */
	public void setFortId(int fortId)
	{
		_fortId = fortId;
	}
	
	/**
	 * Sets the unique identifier for the clan hideout.<br>
	 * This value is used to associate a specific location with the clan.
	 * @param hideoutId The {@code int} ID of the hideout.
	 */
	public void setHideoutId(int hideoutId)
	{
		_hideoutId = hideoutId;
	}
	
	/**
	 * Checks if a member exists in the clan based on their unique identifier.<br>
	 * The method returns {@code true} if the {@code id} is not {@code 0} and is present in the members list.<br>
	 * Otherwise, it returns {@code false}.
	 * @param id The unique identifier of the member to check.
	 * @return {@code true} if the member exists, {@code false} otherwise.
	 */
	public boolean isMember(int id)
	{
		return ((id != 0) && _members.containsKey(id));
	}
	
	/**
	 * Retrieves the current number of blood alliances.
	 * @return The total count of blood alliances.
	 */
	public int getBloodAllianceCount()
	{
		return _bloodAllianceCount;
	}
	
	/**
	 * Increases the blood alliance count for this clan.<br>
	 * This method retrieves the reward from {@code SiegeManager}.<br>
	 * It then updates the value in the database.
	 */
	public void increaseBloodAllianceCount()
	{
		_bloodAllianceCount += SiegeManager.getInstance().getBloodAllianceReward();
		updateBloodAllianceCountInDB();
	}
	
	/**
	 * Resets the blood alliance count to {@code 0}.<br>
	 * Updates the database with the new value.
	 */
	public void resetBloodAllianceCount()
	{
		_bloodAllianceCount = 0;
		updateBloodAllianceCountInDB();
	}
	
	/**
	 * Updates the blood alliance count for the current clan in the database.<br>
	 * This method uses a {@code PreparedStatement} to execute an {@code UPDATE} query.<br>
	 * It logs a warning if any exception occurs during the process.
	 */
	public void updateBloodAllianceCountInDB()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE clan_data SET blood_alliance_count=? WHERE clan_id=?"))
		{
			ps.setInt(1, _bloodAllianceCount);
			ps.setInt(2, _clanId);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on updateBloodAllianceCountInDB(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieves the current number of blood oaths.<br>
	 * This value represents the total count for the clan.
	 * @return The total number of blood oaths as an {@code int}.
	 */
	public int getBloodOathCount()
	{
		return _bloodOathCount;
	}
	
	/**
	 * Increases the current blood oath count.<br>
	 * This method adds a value from {@code FeatureConfig}.<br>
	 * It also updates the count in the database.
	 */
	public void increaseBloodOathCount()
	{
		_bloodOathCount += FeatureConfig.FS_BLOOD_OATH_COUNT;
		updateBloodOathCountInDB();
	}
	
	/**
	 * Resets the blood oath count to {@code 0}.<br>
	 * This method also updates the value in the database.
	 */
	public void resetBloodOathCount()
	{
		_bloodOathCount = 0;
		updateBloodOathCountInDB();
	}
	
	/**
	 * Updates the blood oath count for the current clan in the database.<br>
	 * This method saves the value of {@code _bloodOathCount} to the {@code clan_data} table.<br>
	 * It uses the unique {@code _clanId} to identify which record to update.
	 */
	public void updateBloodOathCountInDB()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE clan_data SET blood_oath_count=? WHERE clan_id=?"))
		{
			ps.setInt(1, _bloodOathCount);
			ps.setInt(2, _clanId);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on updateBloodAllianceCountInDB(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Saves the current state of the clan to the database.<br>
	 * This method updates all relevant fields in the {@code clan_data} table.<br>
	 * It handles the connection and execution using a {@code PreparedStatement}.<br>
	 * Any errors during the update process are logged as severe errors.
	 */
	public void updateClanInDB()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE clan_data SET leader_id=?,ally_id=?,ally_name=?,reputation_score=?,ally_penalty_expiry_time=?,ally_penalty_type=?,char_penalty_expiry_time=?,dissolving_expiry_time=?,new_leader_id=? WHERE clan_id=?"))
		{
			ps.setInt(1, getLeaderId());
			ps.setInt(2, _allyId);
			ps.setString(3, _allyName);
			ps.setInt(4, _reputationScore);
			ps.setLong(5, _allyPenaltyExpiryTime);
			ps.setInt(6, _allyPenaltyType);
			ps.setLong(7, _charPenaltyExpiryTime);
			ps.setLong(8, _dissolvingExpiryTime);
			ps.setInt(9, _newLeaderId);
			ps.setInt(10, _clanId);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error saving clan: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Saves the current clan data to the database.<br>
	 * This method inserts a new record into the clan table.<br>
	 * It handles all necessary fields including name, level, and leader information.
	 */
	public void store()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(INSERT_CLAN_DATA))
		{
			ps.setInt(1, _clanId);
			ps.setString(2, _name);
			ps.setInt(3, _level);
			ps.setInt(4, _castleId);
			ps.setInt(5, _bloodAllianceCount);
			ps.setInt(6, _bloodOathCount);
			ps.setInt(7, _allyId);
			ps.setString(8, _allyName);
			ps.setInt(9, getLeaderId());
			ps.setInt(10, _crestId);
			ps.setInt(11, _crestLargeId);
			ps.setInt(12, _allyCrestId);
			ps.setInt(13, _newLeaderId);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error saving new clan: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Removes a {@code ClanMember} from the database.<br>
	 * This method resets the character's clan status and related flags.<br>
	 * It also clears any apprentice or sponsor statuses for the member.
	 * @param member The {@code ClanMember} to be removed.
	 * @param clanJoinExpiryTime The expiry time of the join date.
	 * @param clanCreateExpiryTime The expiry time of the creation date.
	 */
	private void removeMemberInDatabase(ClanMember member, long clanJoinExpiryTime, long clanCreateExpiryTime)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps1 = con.prepareStatement("UPDATE characters SET clanid=0, title=?, clan_join_expiry_time=?, clan_create_expiry_time=?, clan_privs=0, wantspeace=0, subpledge=0, lvl_joined_academy=0, apprentice=0, sponsor=0 WHERE charId=?");
			PreparedStatement ps2 = con.prepareStatement("UPDATE characters SET apprentice=0 WHERE apprentice=?");
			PreparedStatement ps3 = con.prepareStatement("UPDATE characters SET sponsor=0 WHERE sponsor=?");
			PreparedStatement ps4 = con.prepareStatement("DELETE FROM character_variables WHERE var=?");
			PreparedStatement ps5 = con.prepareStatement("DELETE FROM character_variables WHERE var=?"))
		{
			final int objectId = member.getObjectId();
			ps1.setString(1, "");
			ps1.setLong(2, clanJoinExpiryTime);
			ps1.setLong(3, clanCreateExpiryTime);
			ps1.setInt(4, objectId);
			ps1.execute();
			
			// Remove apprentice.
			ps2.setInt(1, objectId);
			ps2.execute();
			
			// Remove sponsor.
			ps3.setInt(1, objectId);
			ps3.execute();
			
			// Clan contribution.
			ps4.setString(1, PlayerVariables.CLAN_CONTRIBUTION);
			ps4.execute();
			ps5.setString(1, PlayerVariables.CLAN_CONTRIBUTION_TOTAL);
			ps5.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error removing clan member: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Loads the clan data from the database.<br>
	 * This method populates all fields including names, levels, and crests.<br>
	 * It also restores sub-pledges, ranks, skills, and notices.
	 */
	private void restore()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_CLAN_DATA))
		{
			ps.setInt(1, _clanId);
			try (ResultSet clanData = ps.executeQuery())
			{
				if (clanData.next())
				{
					setName(clanData.getString("clan_name"));
					setLevel(clanData.getInt("clan_level"));
					setCastleId(clanData.getInt("hasCastle"));
					_bloodAllianceCount = clanData.getInt("blood_alliance_count");
					_bloodOathCount = clanData.getInt("blood_oath_count");
					setAllyId(clanData.getInt("ally_id"));
					setAllyName(clanData.getString("ally_name"));
					setAllyPenaltyExpiryTime(clanData.getLong("ally_penalty_expiry_time"), clanData.getInt("ally_penalty_type"));
					if (_allyPenaltyExpiryTime < System.currentTimeMillis())
					{
						setAllyPenaltyExpiryTime(0, 0);
					}
					
					setCharPenaltyExpiryTime(clanData.getLong("char_penalty_expiry_time"));
					if ((_charPenaltyExpiryTime + (PlayerConfig.ALT_CLAN_JOIN_DAYS * 86400000)) < System.currentTimeMillis()) // 24*60*60*1000 = 86400000
					{
						setCharPenaltyExpiryTime(0);
					}
					
					setDissolvingExpiryTime(clanData.getLong("dissolving_expiry_time"));
					
					setCrestId(clanData.getInt("crest_id"));
					setCrestLargeId(clanData.getInt("crest_large_id"));
					setAllyCrestId(clanData.getInt("ally_crest_id"));
					
					setReputationScore(clanData.getInt("reputation_score"));
					setAuctionBiddedAt(clanData.getInt("auction_bid_at"), false);
					setNewLeaderId(clanData.getInt("new_leader_id"), false);
					
					final int leaderId = (clanData.getInt("leader_id"));
					ps.clearParameters();
					
					try (PreparedStatement select = con.prepareStatement("SELECT char_name,level,classid,charId,title,power_grade,subpledge,apprentice,sponsor,sex,race FROM characters WHERE clanid=?"))
					{
						select.setInt(1, _clanId);
						try (ResultSet clanMember = select.executeQuery())
						{
							ClanMember member = null;
							while (clanMember.next())
							{
								member = new ClanMember(this, clanMember);
								if (member.getObjectId() == leaderId)
								{
									setLeader(member);
								}
								else
								{
									addClanMember(member);
								}
							}
						}
					}
				}
			}
			
			restoreSubPledges();
			restoreRankPrivs();
			restoreSkills();
			restoreNotice();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error restoring clan data: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Loads the clan notice data from the database.<br>
	 * It updates the {@code _noticeEnabled} and {@code _notice} fields.<br>
	 * Any errors during the database operation are logged as severe.
	 */
	private void restoreNotice()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT enabled,notice FROM clan_notices WHERE clan_id=?"))
		{
			ps.setInt(1, _clanId);
			try (ResultSet noticeData = ps.executeQuery())
			{
				while (noticeData.next())
				{
					_noticeEnabled = Boolean.parseBoolean(noticeData.getString("enabled"));
					_notice = noticeData.getString("notice");
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error restoring clan notice: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Saves the clan notice to the database.<br>
	 * This method handles null values and enforces a maximum length.<br>
	 * It updates the record if it already exists.
	 * @param noticeValue The text content of the notice to store.
	 * @param enabled Whether the notice should be active.
	 */
	private void storeNotice(String noticeValue, boolean enabled)
	{
		String notice = noticeValue;
		if (notice == null)
		{
			notice = "";
		}
		
		if (notice.length() > MAX_NOTICE_LENGTH)
		{
			notice = notice.substring(0, MAX_NOTICE_LENGTH - 1);
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO clan_notices (clan_id,notice,enabled) values (?,?,?) ON DUPLICATE KEY UPDATE notice=?,enabled=?"))
		{
			ps.setInt(1, _clanId);
			ps.setString(2, notice);
			if (enabled)
			{
				ps.setString(3, "true");
			}
			else
			{
				ps.setString(3, "false");
			}
			
			ps.setString(4, notice);
			if (enabled)
			{
				ps.setString(5, "true");
			}
			else
			{
				ps.setString(5, "false");
			}
			
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error could not store clan notice: " + e.getMessage(), e);
		}
		
		_notice = notice;
		_noticeEnabled = enabled;
	}
	
	/**
	 * Enables or disables the clan notice feature.<br>
	 * This method updates the status of notices for the clan.
	 * @param enabled The boolean value to set for enabling or disabling notices.
	 */
	public void setNoticeEnabled(boolean enabled)
	{
		storeNotice(getNotice(), enabled);
	}
	
	/**
	 * Updates the clan notice message.<br>
	 * This method stores the provided {@code String} in the database.
	 * @param notice The new notice text to display.
	 */
	public void setNotice(String notice)
	{
		storeNotice(notice, _noticeEnabled);
	}
	
	/**
	 * Checks if the clan notice feature is currently active.
	 * @return {@code true} if notices are enabled, {@code false} otherwise.
	 */
	public boolean isNoticeEnabled()
	{
		return _noticeEnabled;
	}
	
	/**
	 * Retrieves the clan notice message.<br>
	 * This method removes all HTML-style tags from the text.<br>
	 * It returns an empty {@code String} if the notice is null or contains specific exploit keywords.
	 * @return The cleaned notice text as a {@code String}.
	 */
	public String getNotice()
	{
		if (_notice == null)
		{
			return "";
		}
		
		// Bypass exploit check.
		final String text = _notice.toLowerCase();
		if (text.contains("action") && text.contains("bypass"))
		{
			return "";
		}
		
		// Returns text without tags.
		return _notice.replaceAll("<.*?>", "");
	}
	
	/**
	 * Loads all clan skills from the database.<br>
	 * It populates the {@code _skills}, {@code _subPledgeSkills}, and subpledge skill collections.<br>
	 * This method handles different types of skills based on their {@code sub_pledge_id}.
	 */
	private void restoreSkills()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT skill_id,skill_level,sub_pledge_id FROM clan_skills WHERE clan_id=?"))
		{
			// Retrieve all skills of this Player from the database.
			ps.setInt(1, _clanId);
			try (ResultSet rset = ps.executeQuery())
			{
				// Go though the recordset of this SQL query.
				while (rset.next())
				{
					final int id = rset.getInt("skill_id");
					final int level = rset.getInt("skill_level");
					
					// Create a Skill object for each record.
					final Skill skill = SkillData.getInstance().getSkill(id, level);
					
					// Add the Skill object to the Clan _skills.
					final int subType = rset.getInt("sub_pledge_id");
					if (subType == -2)
					{
						_skills.put(skill.getId(), skill);
					}
					else if (subType == 0)
					{
						_subPledgeSkills.put(skill.getId(), skill);
					}
					else
					{
						final SubPledge subunit = _subPledges.get(subType);
						if (subunit != null)
						{
							subunit.addNewSkill(skill);
						}
						else
						{
							LOGGER.info("Missing subpledge " + subType + " for clan " + this + ", skill skipped.");
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error restoring clan skills: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieves all skills associated with the clan.
	 * @return a {@code Collection} of {@code Skill} objects.
	 */
	public Collection<Skill> getAllSkills()
	{
		return _skills.values();
	}
	
	/**
	 * Retrieves the collection of skills associated with this clan.
	 * @return a {@code Map} where the key is an {@code Integer} and the value is a {@code Skill} object.
	 */
	public Map<Integer, Skill> getSkills()
	{
		return _skills;
	}
	
	/**
	 * Adds a new {@code Skill} to the collection.<br>
	 * If a skill with the same ID already exists, it will be replaced.
	 * @param newSkill The {@code Skill} object to add.
	 * @return The {@code Skill} that was replaced, or {@code null} if no replacement occurred.
	 */
	public Skill addSkill(Skill newSkill)
	{
		Skill oldSkill = null;
		if (newSkill != null)
		{
			// Replace oldSkill by newSkill or Add the newSkill.
			oldSkill = _skills.put(newSkill.getId(), newSkill);
		}
		
		return oldSkill;
	}
	
	/**
	 * Adds a new {@code Skill} to the clan.<br>
	 * This method uses a default value for the internal parameter.
	 * @param newSkill The {@code Skill} object to be added.
	 * @return The newly added {@code Skill} object.
	 */
	public Skill addNewSkill(Skill newSkill)
	{
		return addNewSkill(newSkill, -2);
	}
	
	/**
	 * Adds a new skill to the clan and updates the database.<br>
	 * This method handles skills for general, sub-pledge, or specific subunit types.<br>
	 * It also notifies all eligible online members about the new skill.
	 * @param newSkill The {@code Skill} object to be added.
	 * @param subType The type of pledge associated with the skill.
	 * @return The {@code Skill} that was previously in the map, or {@code null} if it was a new entry.
	 */
	public Skill addNewSkill(Skill newSkill, int subType)
	{
		Skill oldSkill = null;
		if (newSkill != null)
		{
			final int newSkillId = newSkill.getId();
			if (subType == -2)
			{
				oldSkill = _skills.put(newSkillId, newSkill);
			}
			else if (subType == 0)
			{
				oldSkill = _subPledgeSkills.put(newSkillId, newSkill);
			}
			else
			{
				final SubPledge subunit = getSubPledge(subType);
				if (subunit != null)
				{
					oldSkill = subunit.addNewSkill(newSkill);
				}
				else
				{
					LOGGER.warning("Subpledge " + subType + " does not exist for clan " + this);
					return oldSkill;
				}
			}
			
			try (Connection con = DatabaseFactory.getConnection())
			{
				if (oldSkill != null)
				{
					try (PreparedStatement ps = con.prepareStatement("UPDATE clan_skills SET skill_level=? WHERE skill_id=? AND clan_id=?"))
					{
						ps.setInt(1, newSkill.getLevel());
						ps.setInt(2, oldSkill.getId());
						ps.setInt(3, _clanId);
						ps.execute();
					}
				}
				else
				{
					try (PreparedStatement ps = con.prepareStatement("INSERT INTO clan_skills (clan_id,skill_id,skill_level,skill_name,sub_pledge_id) VALUES (?,?,?,?,?)"))
					{
						ps.setInt(1, _clanId);
						ps.setInt(2, newSkillId);
						ps.setInt(3, newSkill.getLevel());
						ps.setString(4, newSkill.getName());
						ps.setInt(5, subType);
						ps.execute();
					}
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Error could not store clan skills: " + e.getMessage(), e);
			}
			
			final int newSkillLevel = newSkill.getLevel();
			final SystemMessage sm = new SystemMessage(SystemMessageId.THE_CLAN_SKILL_S1_HAS_BEEN_ADDED);
			sm.addSkillName(newSkillId);
			
			for (ClanMember temp : _members.values())
			{
				if ((temp != null) && temp.isOnline())
				{
					final Player tempPlayer = temp.getPlayer();
					if (tempPlayer != null)
					{
						if (subType == -2)
						{
							if (hasRankFor(tempPlayer, newSkill))
							{
								tempPlayer.addSkill(newSkill, false); // Skill is not saved to player DB.
								tempPlayer.sendPacket(new PledgeSkillListAdd(newSkillId, newSkillLevel));
								tempPlayer.sendPacket(sm);
								tempPlayer.sendSkillList();
							}
						}
						else if (temp.getPledgeType() == subType)
						{
							tempPlayer.addSkill(newSkill, false); // Skill is not saved to player DB.
							tempPlayer.sendPacket(new ExSubPledgeSkillAdd(subType, newSkillId, newSkillLevel));
							tempPlayer.sendPacket(sm);
							tempPlayer.sendSkillList();
						}
					}
				}
			}
		}
		
		return oldSkill;
	}
	
	/**
	 * Checks if a member's rank allows a clan skill, following the pledge skill tree social class.
	 * @param player The clan member.
	 * @param skill The clan skill.
	 * @return {@code true} if the member may use the skill.
	 */
	private static boolean hasRankFor(Player player, Skill skill)
	{
		final SkillLearn skillLearn = SkillTreeData.getInstance().getPledgeSkill(skill.getId(), skill.getLevel());
		return (skillLearn == null) || (skillLearn.getSocialClass() == null) || ((player.getPledgeClass() + 1) >= skillLearn.getSocialClass().ordinal());
	}
	
	/**
	 * Applies active clan skills to all online members.<br>
	 * This method checks if a member's pledge class meets the required minimum for each skill.<br>
	 * If the requirement is met, the skill is added to the player without saving it to the database.
	 */
	public void addSkillEffects()
	{
		for (Skill skill : _skills.values())
		{
			for (ClanMember temp : _members.values())
			{
				try
				{
					if ((temp != null) && temp.isOnline() && hasRankFor(temp.getPlayer(), skill))
					{
						temp.getPlayer().addSkill(skill, false); // Skill is not saved to player DB.
					}
				}
				catch (NullPointerException e)
				{
					LOGGER.log(Level.WARNING, e.getMessage(), e);
				}
			}
		}
	}
	
	/**
	 * Applies clan-related skills and effects to a {@code Player}.<br>
	 * This method checks the player's pledge class and social status.<br>
	 * It grants temporary skills that are not saved to the database.<br>
	 * If the reputation score is negative, it triggers special skill statuses.
	 * @param player The {@code Player} object to receive the effects.
	 */
	public void addSkillEffects(Player player)
	{
		// Clan skills stay off for the whole Olympiad match, they are given back when it ends.
		if ((player == null) || player.isInOlympiadMode())
		{
			return;
		}
		
		final int playerSocialClass = player.getPledgeClass() + 1;
		for (Skill skill : _skills.values())
		{
			if (hasRankFor(player, skill))
			{
				player.addSkill(skill, false); // Skill is not saved to player DB.
			}
		}
		
		final int playerPledgeType = player.getPledgeType();
		if (playerPledgeType == 0)
		{
			for (Skill skill : _subPledgeSkills.values())
			{
				final SkillLearn skillLearn = SkillTreeData.getInstance().getSubPledgeSkill(skill.getId(), skill.getLevel());
				if ((skillLearn == null) || (skillLearn.getSocialClass() == null) || (playerSocialClass >= skillLearn.getSocialClass().ordinal()))
				{
					player.addSkill(skill, false); // Skill is not saved to player DB.
				}
			}
		}
		else
		{
			final SubPledge subunit = getSubPledge(playerPledgeType);
			if (subunit == null)
			{
				return;
			}
			
			for (Skill skill : subunit.getSkills())
			{
				player.addSkill(skill, false); // Skill is not saved to player DB.
			}
		}
		
		if (_reputationScore <= 0)
		{
			skillsStatus(player, true);
		}
	}
	
	/**
	 * Removes all active skills from the specified {@code Player}.<br>
	 * This includes standard clan skills and sub-pledge skills.<br>
	 * The removal process does not save changes to the database.
	 * @param player The {@code Player} object whose skills will be removed.
	 */
	public void removeSkillEffects(Player player)
	{
		if (player == null)
		{
			return;
		}
		
		for (Skill skill : _skills.values())
		{
			player.removeSkill(skill, false); // Skill is not saved to player DB.
		}
		
		final int playerPledgeType = player.getPledgeType();
		if (playerPledgeType == 0)
		{
			for (Skill skill : _subPledgeSkills.values())
			{
				player.removeSkill(skill, false); // Skill is not saved to player DB.
			}
		}
		else
		{
			final SubPledge subunit = getSubPledge(playerPledgeType);
			if (subunit == null)
			{
				return;
			}
			
			for (Skill skill : subunit.getSkills())
			{
				player.removeSkill(skill, false); // Skill is not saved to player DB.
			}
		}
	}
	
	/**
	 * Updates the skill status for a specific {@code Player}.<br>
	 * This method enables or disables skills based on the provided boolean.<br>
	 * It handles both standard clan skills and sub-pledge skills.
	 * @param player The {@code Player} whose skills will be updated.
	 * @param disable Set to {@code true} to disable skills, or {@code false} to enable them.
	 */
	public void skillsStatus(Player player, boolean disable)
	{
		if (player == null)
		{
			return;
		}
		
		for (Skill skill : _skills.values())
		{
			if (disable)
			{
				player.disableSkill(skill, -1);
			}
			else
			{
				player.enableSkill(skill, false);
			}
		}
		
		final int playerPledgeType = player.getPledgeType();
		if (playerPledgeType == 0)
		{
			for (Skill skill : _subPledgeSkills.values())
			{
				if (disable)
				{
					player.disableSkill(skill, -1);
				}
				else
				{
					player.enableSkill(skill, false);
				}
			}
		}
		else
		{
			final SubPledge subunit = getSubPledge(playerPledgeType);
			if (subunit != null)
			{
				for (Skill skill : subunit.getSkills())
				{
					if (disable)
					{
						player.disableSkill(skill, -1);
					}
					else
					{
						player.enableSkill(skill, false);
					}
				}
			}
		}
	}
	
	/**
	 * Removes a specific skill from the clan.<br>
	 * This method updates the internal list and the database.<br>
	 * It also removes the skill from all online members of the clan.
	 * @param skill The {@code Skill} object to be removed.
	 */
	public void removeSkill(Skill skill)
	{
		_skills.remove(skill.getId());
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM clan_skills WHERE clan_id=? AND skill_id=? AND skill_level=?"))
		{
			ps.setInt(1, _clanId);
			ps.setInt(2, skill.getId());
			ps.setInt(3, skill.getLevel());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error removing clan masteries: " + e.getMessage(), e);
		}
		
		for (ClanMember member : _members.values())
		{
			if ((member != null) && member.isOnline())
			{
				final Player player = member.getPlayer();
				if (player != null)
				{
					player.removeSkill(skill, false);
					player.sendSkillList();
				}
			}
		}
	}
	
	/**
	 * Sends a packet to all online members of allied clans.<br>
	 * This method iterates through all allies based on the current clan's ally ID.<br>
	 * It calls {@code broadcastToOnlineMembers} for each ally found in {@code ClanTable}.
	 * @param packet The {@code ServerPacket} to be sent to the members.
	 */
	public void broadcastToOnlineAllyMembers(ServerPacket packet)
	{
		for (Clan clan : ClanTable.getInstance().getClanAllies(getAllyId()))
		{
			clan.broadcastToOnlineMembers(packet);
		}
	}
	
	/**
	 * Sends a specific packet to all members of the clan who are currently online.<br>
	 * This method iterates through all registered members and checks their online status.<br>
	 * If a member is online, the {@code ServerPacket} is delivered to them.
	 * @param packet The {@code ServerPacket} to be sent to each online member.
	 */
	public void broadcastToOnlineMembers(ServerPacket packet)
	{
		for (ClanMember member : _members.values())
		{
			if ((member != null) && member.isOnline())
			{
				final Player player = member.getPlayer();
				if (player != null)
				{
					player.sendPacket(packet);
				}
			}
		}
	}
	
	/**
	 * Sends a chat packet to all online members of the clan.<br>
	 * It checks if the member is currently online.<br>
	 * It also ensures the recipient is not blocked by the broadcaster.
	 * @param packet The {@code CreatureSay} packet to be sent.
	 * @param broadcaster The {@code Player} who is sending the message.
	 */
	public void broadcastCSToOnlineMembers(CreatureSay packet, Player broadcaster)
	{
		for (ClanMember member : _members.values())
		{
			if ((member != null) && member.isOnline())
			{
				final Player player = member.getPlayer();
				if ((player != null) && !BlockList.isBlocked(player, broadcaster))
				{
					player.sendPacket(packet);
				}
			}
		}
	}
	
	/**
	 * Sends a specific packet to all online members of the clan.<br>
	 * This method excludes the original {@code Player} who triggered the action.<br>
	 * It iterates through all registered {@code ClanMember} objects.
	 * @param packet The {@code ServerPacket} to be sent to other members.
	 * @param player The {@code Player} who initiated the broadcast.
	 */
	public void broadcastToOtherOnlineMembers(ServerPacket packet, Player player)
	{
		for (ClanMember member : _members.values())
		{
			if ((member != null) && member.isOnline())
			{
				final Player memberPlayer = member.getPlayer();
				if ((memberPlayer != null) && (memberPlayer != player))
				{
					memberPlayer.sendPacket(packet);
				}
			}
		}
	}
	
	/**
	 * Returns a string representation of the {@code Clan}.<br>
	 * The format includes the clan name and the unique ID.
	 * @return A string containing the name and ID.
	 */
	@Override
	public String toString()
	{
		return _name + "[" + _clanId + "]";
	}
	
	/**
	 * Retrieves the warehouse associated with this clan.
	 * @return the {@code ItemContainer} object representing the warehouse.
	 */
	public ItemContainer getWarehouse()
	{
		return _warehouse;
	}
	
	/**
	 * Checks if this clan is currently at war with another clan.<br>
	 * It looks up the provided {@code clanId} in the internal war list.
	 * @param clanId The unique identifier of the clan to check.
	 * @return {@code true} if the clan is at war, otherwise {@code false}.
	 */
	public boolean isAtWarWith(int clanId)
	{
		return _atWarWith.containsKey(clanId);
	}
	
	/**
	 * Checks if this clan is currently at war with another clan.<br>
	 * It returns {@code true} if the target clan exists in the war list.<br>
	 * It returns {@code false} if the clan is not at war or if the input is {@code null}.
	 * @param clan The {@code Clan} to check against.
	 * @return {@code true} if a state of war exists, otherwise {@code false}.
	 */
	public boolean isAtWarWith(Clan clan)
	{
		if (clan == null)
		{
			return false;
		}
		
		return _atWarWith.containsKey(clan.getId());
	}
	
	/**
	 * Retrieves the total number of hired guards.
	 * @return the count of hired guards as an {@code int}.
	 */
	public int getHiredGuards()
	{
		return _hiredGuards;
	}
	
	/**
	 * Increases the count of hired guards for this clan.<br>
	 * This method updates the {@code _hiredGuards} counter.
	 */
	public void incrementHiredGuards()
	{
		_hiredGuards++;
	}
	
	/**
	 * Checks if the clan is currently in a state of war.<br>
	 * It returns {@code true} if there are any enemies listed.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if the clan is at war, {@code false} otherwise.
	 */
	public boolean isAtWar()
	{
		return !_atWarWith.isEmpty();
	}
	
	/**
	 * Retrieves the list of current clan wars.<br>
	 * The map uses an {@code Integer} ID as the key for each {@code ClanWar}.
	 * @return A {@code Map} containing all active {@code ClanWar} objects.
	 */
	public Map<Integer, ClanWar> getWarList()
	{
		return _atWarWith;
	}
	
	/**
	 * Sends the current clan member list to all online members.<br>
	 * This method refreshes the UI for every player in the clan.<br>
	 * It clears existing lists and sends a new update packet.
	 */
	public void broadcastClanStatus()
	{
		for (Player member : getOnlineMembers(0))
		{
			member.sendPacket(PledgeShowMemberListDeleteAll.STATIC_PACKET);
			PledgeShowMemberListAll.sendAllTo(member);
		}
	}
	
	public static class SubPledge
	{
		private final int _id;
		private String _subPledgeName;
		private int _leaderId;
		private final Map<Integer, Skill> _subPledgeSkills = new ConcurrentSkipListMap<>();
		
		public SubPledge(int id, String name, int leaderId)
		{
			_id = id;
			_subPledgeName = name;
			_leaderId = leaderId;
		}
		
		public int getId()
		{
			return _id;
		}
		
		public String getName()
		{
			return _subPledgeName;
		}
		
		public void setName(String name)
		{
			_subPledgeName = name;
		}
		
		public int getLeaderId()
		{
			return _leaderId;
		}
		
		public void setLeaderId(int leaderId)
		{
			_leaderId = leaderId;
		}
		
		public Skill addNewSkill(Skill skill)
		{
			return _subPledgeSkills.put(skill.getId(), skill);
		}
		
		public Collection<Skill> getSkills()
		{
			return _subPledgeSkills.values();
		}
		
		public Skill getSkill(int id)
		{
			return _subPledgeSkills.get(id);
		}
	}
	
	public static class RankPrivs
	{
		private final int _rankId;
		private final int _party; // TODO: Find out what this stuff means and implement it.
		private final ClanPrivileges _rankPrivs;
		
		public RankPrivs(int rank, int party, int privs)
		{
			_rankId = rank;
			_party = party;
			_rankPrivs = new ClanPrivileges(privs);
		}
		
		public RankPrivs(int rank, int party, ClanPrivileges rankPrivs)
		{
			_rankId = rank;
			_party = party;
			_rankPrivs = rankPrivs;
		}
		
		public int getRank()
		{
			return _rankId;
		}
		
		public int getParty()
		{
			return _party;
		}
		
		public ClanPrivileges getPrivs()
		{
			return _rankPrivs;
		}
		
		public void setPrivs(int privs)
		{
			_rankPrivs.setMask(privs);
		}
	}
	
	/**
	 * Loads all subpledges for this clan from the database.<br>
	 * It populates the internal {@code _subPledges} map with data.<br>
	 * Any errors during the loading process are logged as warnings.
	 */
	private void restoreSubPledges()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT sub_pledge_id,name,leader_id FROM clan_subpledges WHERE clan_id=?"))
		{
			// Retrieve all subpledges of this clan from the database.
			ps.setInt(1, _clanId);
			try (ResultSet rset = ps.executeQuery())
			{
				while (rset.next())
				{
					final int id = rset.getInt("sub_pledge_id");
					final String name = rset.getString("name");
					final int leaderId = rset.getInt("leader_id");
					
					// Create a SubPledge object for each record.
					final SubPledge pledge = new SubPledge(id, name, leaderId);
					_subPledges.put(id, pledge);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore clan sub-units: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieves a specific sub-pledge based on its type.<br>
	 * This method looks up the pledge in the internal collection.
	 * @param pledgeType The unique identifier for the pledge type.
	 * @return The {@code SubPledge} object, or {@code null} if it does not exist.
	 */
	public SubPledge getSubPledge(int pledgeType)
	{
		return _subPledges.get(pledgeType);
	}
	
	/**
	 * Retrieves a {@code SubPledge} object based on its name.<br>
	 * This method searches through all available sub-pledges.<br>
	 * It performs a case-insensitive comparison of the provided name.
	 * @param pledgeName The name of the sub-pledge to find.
	 * @return The matching {@code SubPledge} object, or {@code null} if no match is found.
	 */
	public SubPledge getSubPledge(String pledgeName)
	{
		for (SubPledge sp : _subPledges.values())
		{
			if (sp.getName().equalsIgnoreCase(pledgeName))
			{
				return sp;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves all sub-pledges associated with this clan.<br>
	 * Returns an empty list if no sub-pledges exist.
	 * @return A {@code Collection} of {@code SubPledge} objects.
	 */
	public Collection<SubPledge> getAllSubPledges()
	{
		return _subPledges.values();
	}
	
	/**
	 * Creates a new {@code SubPledge} for the clan.<br>
	 * This method saves the new pledge to the database and adds it to the internal map.<br>
	 * It returns {@code null} if the pledge type already exists or is common.
	 * @param pledgeType The unique identifier for the sub-pledge type.
	 * @return The newly created {@code SubPledge} object, or {@code null} if creation failed.
	 */
	public SubPledge createSubPledge(int pledgeType)
	{
		if ((_subPledges.get(pledgeType) != null) || (pledgeType == PLEDGE_CLASS_COMMON))
		{
			return null;
		}
		
		final String subPledgeName = "ELITE"; // For now only elite exists.
		SubPledge subPledge = null;
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO clan_subpledges (clan_id,sub_pledge_id,name,leader_id) values (?,?,?,?)"))
		{
			ps.setInt(1, _clanId);
			ps.setInt(2, pledgeType);
			ps.setString(3, subPledgeName);
			final int objectId = _leader.getObjectId();
			ps.setInt(4, objectId);
			ps.execute();
			
			subPledge = new SubPledge(pledgeType, subPledgeName, objectId);
			_subPledges.put(pledgeType, subPledge);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error saving sub clan data: " + e.getMessage(), e);
		}
		
		// broadcastToOnlineMembers(new PledgeShowInfoUpdate(_leader.getClan()));
		// broadcastToOnlineMembers(new PledgeReceiveSubPledgeCreated(subPledge, _leader.getClan()));
		return subPledge;
	}
	
	/**
	 * Updates the leader information for a specific sub-pledge in the database.<br>
	 * This method synchronizes the {@code leader_id} and name with the current data.
	 * @param pledgeType The unique identifier for the sub-pledge to update.
	 */
	public void updateSubPledgeInDB(int pledgeType)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE clan_subpledges SET leader_id=?, name=? WHERE clan_id=? AND sub_pledge_id=?"))
		{
			ps.setInt(1, getSubPledge(pledgeType).getLeaderId());
			ps.setString(2, getSubPledge(pledgeType).getName());
			ps.setInt(3, _clanId);
			ps.setInt(4, pledgeType);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error updating subpledge: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Restores the privileges for each rank from the database.<br>
	 * This method fetches data from the {@code clan_privs} table.<br>
	 * It updates the {@code _privs} map with values for all valid ranks.
	 */
	private void restoreRankPrivs()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT privs,`rank`,party FROM clan_privs WHERE clan_id=?"))
		{
			// Retrieve all skills of this Player from the database.
			ps.setInt(1, _clanId);
			// LOGGER.warning("clanPrivs restore for ClanId : "+getClanId());
			try (ResultSet rset = ps.executeQuery())
			{
				// Go though the recordset of this SQL query.
				while (rset.next())
				{
					final int rank = rset.getInt("rank");
					
					// Create a SubPledge object for each record.
					if (rank == -1)
					{
						continue;
					}
					
					// int party = rset.getInt("party");
					final int privileges = rset.getInt("privs");
					_privs.get(rank).setPrivs(privileges);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error restoring clan privs by rank: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Initializes the default privileges for clan ranks.<br>
	 * This method populates the {@code _privs} map with {@code RankPrivs} objects.<br>
	 * It creates entries for rank IDs from 1 to 9.
	 */
	public void initializePrivs()
	{
		for (int i = 1; i < 10; i++)
		{
			_privs.put(i, new RankPrivs(i, 0, new ClanPrivileges()));
		}
	}
	
	/**
	 * Retrieves the privileges associated with a specific rank.<br>
	 * If no privileges are found for the given {@code rank}, it returns an empty {@code ClanPrivileges} object.
	 * @param rank The numerical rank to look up.
	 * @return The {@code ClanPrivileges} object for the specified rank or a new instance if none exists.
	 */
	public ClanPrivileges getRankPrivs(int rank)
	{
		return _privs.get(rank) != null ? _privs.get(rank).getPrivs() : new ClanPrivileges();
	}
	
	/**
	 * Updates the privileges associated with a specific rank.<br>
	 * This method saves the new privileges to the database.<br>
	 * It also updates online members who hold this rank and broadcasts the change.
	 * @param rank The unique identifier for the clan rank.
	 * @param privs The bitmask representing the permissions for this rank.
	 */
	public void setRankPrivs(int rank, int privs)
	{
		final RankPrivs currentPrivs = _privs.get(rank);
		if (currentPrivs != null)
		{
			currentPrivs.setPrivs(privs);
			
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("REPLACE INTO clan_privs (clan_id,`rank`,party,privs) VALUES (?,?,?,?)"))
			{
				// Retrieve all skills of this Player from the database.
				ps.setInt(1, _clanId);
				ps.setInt(2, rank);
				ps.setInt(3, 0);
				ps.setInt(4, privs);
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Could not store clan privs for rank: " + e.getMessage(), e);
			}
			
			for (ClanMember cm : _members.values())
			{
				if (cm.isOnline() && (cm.getPowerGrade() == rank))
				{
					final Player player = cm.getPlayer();
					if (player != null)
					{
						player.getClanPrivileges().setMask(privs);
						player.updateUserInfo();
					}
				}
			}
			
			broadcastClanStatus();
		}
		else
		{
			_privs.put(rank, new RankPrivs(rank, 0, privs));
			
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("REPLACE INTO clan_privs (clan_id,`rank`,party,privs) VALUES (?,?,?,?)"))
			{
				// Retrieve all skills of this Player from the database.
				ps.setInt(1, _clanId);
				ps.setInt(2, rank);
				ps.setInt(3, 0);
				ps.setInt(4, privs);
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Could not create new rank and store clan privs for rank: " + e.getMessage(), e);
			}
		}
	}
	
	/**
	 * Retrieves all privileges associated with the clan rank.<br>
	 * This method returns a collection of {@code RankPrivs}.<br>
	 * If no privileges exist, it returns an empty list.
	 * @return A {@code Collection} of {@code RankPrivs} objects.
	 */
	public Collection<RankPrivs> getAllRankPrivs()
	{
		return _privs.values();
	}
	
	/**
	 * Retrieves the unique identifier for a sub-pledge owned by a specific leader.<br>
	 * It searches through all active sub-pledges to find a match.<br>
	 * If no matching sub-pledge is found, it returns 0.
	 * @param leaderId The unique ID of the clan leader to search for.
	 * @return The {@code int} ID of the sub-pledge or 0 if not found.
	 */
	public int getLeaderSubPledge(int leaderId)
	{
		int id = 0;
		for (SubPledge sp : _subPledges.values())
		{
			final int spLeaderId = sp.getLeaderId();
			if (spLeaderId == 0)
			{
				continue;
			}
			
			if (spLeaderId == leaderId)
			{
				id = sp.getId();
			}
		}
		
		return id;
	}
	
	/**
	 * Updates the current reputation score of the clan.<br>
	 * This method adds the specified amount to the existing score.
	 * @param value The amount to add to the reputation score.
	 */
	public synchronized void addReputationScore(int value)
	{
		setReputationScore(_reputationScore + value);
	}
	
	/**
	 * Reduces the current reputation score of the clan.<br>
	 * This method subtracts the specified amount from the existing score.
	 * @param value The amount to subtract from the reputation score.
	 */
	public synchronized void takeReputationScore(int value)
	{
		setReputationScore(_reputationScore - value);
	}
	
	/**
	 * Updates the reputation score of the clan.<br>
	 * This method handles the activation or deactivation of clan skills based on whether the score is positive or negative.<br>
	 * It also clamps the final value between -100000000 and 100000000.
	 * @param value The new reputation score to set.
	 */
	private void setReputationScore(int value)
	{
		if ((_reputationScore > 0) && (value <= 0))
		{
			broadcastToOnlineMembers(new SystemMessage(SystemMessageId.SINCE_THE_CLAN_REPUTATION_HAS_DROPPED_BELOW_0_YOUR_CLAN_SKILL_S_WILL_BE_DE_ACTIVATED));
			for (ClanMember member : _members.values())
			{
				if (member.isOnline())
				{
					final Player memberPlayer = member.getPlayer();
					if (memberPlayer != null)
					{
						skillsStatus(memberPlayer, true);
					}
				}
			}
		}
		else if ((_reputationScore <= 0) && (value > 0))
		{
			broadcastToOnlineMembers(new SystemMessage(SystemMessageId.CLAN_SKILLS_WILL_NOW_BE_ACTIVATED_SINCE_THE_CLAN_REPUTATION_IS_1_OR_HIGHER));
			for (ClanMember member : _members.values())
			{
				if (member.isOnline())
				{
					final Player memberPlayer = member.getPlayer();
					if (memberPlayer != null)
					{
						skillsStatus(memberPlayer, false);
					}
				}
			}
		}
		
		_reputationScore = value;
		if (_reputationScore > 100000000)
		{
			_reputationScore = 100000000;
		}
		
		if (_reputationScore < -100000000)
		{
			_reputationScore = -100000000;
		}
		
		broadcastToOnlineMembers(new PledgeShowInfoUpdate(this));
	}
	
	/**
	 * Retrieves the current reputation score of the clan.<br>
	 * This value represents the overall standing of the group.
	 * @return The current {@code int} reputation score.
	 */
	public int getReputationScore()
	{
		return _reputationScore;
	}
	
	/**
	 * Updates the current rank of a clan member.<br>
	 * This method is synchronized to ensure thread safety during updates.
	 * @param rank The new {@code int} value for the rank.
	 */
	public synchronized void setRank(int rank)
	{
		_rank = rank;
	}
	
	/**
	 * Retrieves the current rank of the clan member.
	 * @return The integer value representing the rank.
	 */
	public int getRank()
	{
		return _rank;
	}
	
	/**
	 * Retrieves the timestamp of when the auction was last bid on.
	 * @return The time value representing the last bid.
	 */
	public int getAuctionBiddedAt()
	{
		return _auctionBiddedAt;
	}
	
	/**
	 * Sets the timestamp of when an auction was bid.<br>
	 * This method updates the internal field and optionally saves it to the database.
	 * @param id The new auction bid timestamp.
	 * @param storeInDb Set to {@code true} to persist the change in the database.
	 */
	public void setAuctionBiddedAt(int id, boolean storeInDb)
	{
		_auctionBiddedAt = id;
		if (storeInDb)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("UPDATE clan_data SET auction_bid_at=? WHERE clan_id=?"))
			{
				ps.setInt(1, id);
				ps.setInt(2, _clanId);
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Could not store auction for clan: " + e.getMessage(), e);
			}
		}
	}
	
	/**
	 * Checks the Clan Academy join rule: the main class and every subclass must be unawakened and level 84 or below.
	 * @param target The {@code Player} who wants to join the academy.
	 * @return {@code true} if the player can join the academy, otherwise {@code false}.
	 */
	private static boolean canJoinAcademy(Player target)
	{
		final CategoryData categoryData = CategoryData.getInstance();
		if ((target.getLevel() > 84) || target.isAwakenedClass() || categoryData.isInCategory(CategoryType.SIXTH_CLASS_GROUP, target.getBaseClass()))
		{
			return false;
		}
		
		for (SubClassHolder subClass : target.getSubClasses().values())
		{
			if ((subClass.getLevel() > 84) || categoryData.isInCategory(CategoryType.SIXTH_CLASS_GROUP, subClass.getId()))
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Checks if a player can join the clan based on various requirements.<br>
	 * It verifies permissions, target validity, and cooldown periods.<br>
	 * It also checks for existing memberships and pledge capacity limits.
	 * @param player The {@code Player} attempting to perform the action.
	 * @param target The {@code Player} who wants to join the clan.
	 * @param pledgeType The type of pledge being joined.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	public boolean checkClanJoinCondition(Player player, Player target, int pledgeType)
	{
		if (player == null)
		{
			return false;
		}
		
		if (!player.hasAccess(ClanAccess.INVITE_MEMBER))
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return false;
		}
		
		if (target == null)
		{
			player.sendPacket(SystemMessageId.THE_TARGET_CANNOT_BE_INVITED);
			return false;
		}
		
		if (player.getObjectId() == target.getObjectId())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_ASK_YOURSELF_TO_APPLY_TO_A_CLAN);
			return false;
		}
		
		if (_charPenaltyExpiryTime > System.currentTimeMillis())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_ACCEPT_A_NEW_CLAN_MEMBER_FOR_24_H_AFTER_DISMISSING_SOMEONE);
			return false;
		}
		
		if (target.getClanId() != 0)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_ALREADY_A_MEMBER_OF_ANOTHER_CLAN);
			sm.addString(target.getName());
			player.sendPacket(sm);
			return false;
		}
		
		if (target.getClanJoinExpiryTime() > System.currentTimeMillis())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_WILL_BE_ABLE_TO_JOIN_YOUR_CLAN_IN_24_H_AFTER_LEAVING_THE_PREVIOUS_ONE);
			sm.addString(target.getName());
			player.sendPacket(sm);
			return false;
		}
		
		if ((pledgeType == -1) && !canJoinAcademy(target))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_DOES_NOT_MEET_THE_REQUIREMENTS_TO_JOIN_A_CLAN_ACADEMY);
			sm.addString(target.getName());
			player.sendPacket(sm);
			player.sendPacket(SystemMessageId.IN_ORDER_TO_JOIN_THE_CLAN_ACADEMY_YOU_MUST_BE_UNAFFILIATED_WITH_A_CLAN_AND_BE_AN_UNAWAKENED_CHARACTER_LV_84_OR_BELOW_FOR_BOTH_MAIN_AND_SUBCLASS);
			return false;
		}
		
		if (getSubPledgeMembersCount(pledgeType) >= (pledgeType == 0 ? ClanLevelData.getInstance().getCommonMemberLimit(_level) : ClanLevelData.getInstance().getEliteMemberLimit(_level)))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_FULL_AND_CANNOT_ACCEPT_ADDITIONAL_CLAN_MEMBERS_AT_THIS_TIME);
			sm.addString(_name);
			player.sendPacket(sm);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Checks if a player can join an alliance with another clan.<br>
	 * This method validates several conditions including leadership status, penalty timers, and war states.<br>
	 * It also ensures the target is eligible and that the alliance limit has not been exceeded.
	 * @param player The {@code Player} attempting to initiate the alliance join.
	 * @param target The {@code Player} who is the leader of the clan being joined.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	public boolean checkAllyJoinCondition(Player player, Player target)
	{
		if (player == null)
		{
			return false;
		}
		
		final int playerAllyId = player.getAllyId();
		if ((playerAllyId == 0) || !player.isClanLeader() || (player.getClanId() != playerAllyId))
		{
			player.sendPacket(SystemMessageId.THIS_FEATURE_IS_ONLY_AVAILABLE_TO_ALLIANCE_LEADERS);
			return false;
		}
		
		final Clan leaderClan = player.getClan();
		if ((leaderClan.getAllyPenaltyExpiryTime() > System.currentTimeMillis()) && (leaderClan.getAllyPenaltyType() == PENALTY_TYPE_DISMISS_CLAN))
		{
			player.sendPacket(SystemMessageId.YOU_CAN_ACCEPT_A_NEW_CLAN_IN_THE_ALLIANCE_IN_24_H_AFTER_DISMISSING_ANOTHER_ONE);
			return false;
		}
		
		if (target == null)
		{
			player.sendPacket(SystemMessageId.THE_TARGET_CANNOT_BE_INVITED);
			return false;
		}
		
		if (player.getObjectId() == target.getObjectId())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_ASK_YOURSELF_TO_APPLY_TO_A_CLAN);
			return false;
		}
		
		if (target.getClan() == null)
		{
			player.sendPacket(SystemMessageId.THE_TARGET_MUST_BE_A_CLAN_MEMBER);
			return false;
		}
		
		if (!target.isClanLeader())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_NOT_A_CLAN_LEADER);
			sm.addString(target.getName());
			player.sendPacket(sm);
			return false;
		}
		
		final Clan targetClan = target.getClan();
		if (target.getAllyId() != 0)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CLAN_IS_ALREADY_A_MEMBER_OF_S2_ALLIANCE);
			sm.addString(targetClan.getName());
			sm.addString(targetClan.getAllyName());
			player.sendPacket(sm);
			return false;
		}
		
		if (targetClan.getAllyPenaltyExpiryTime() > System.currentTimeMillis())
		{
			final int penaltyType = targetClan.getAllyPenaltyType();
			if (penaltyType == PENALTY_TYPE_CLAN_LEAVED)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CLAN_CANNOT_JOIN_THE_ALLIANCE_BECAUSE_ONE_DAY_HAS_NOT_YET_PASSED_SINCE_THEY_LEFT_ANOTHER_ALLIANCE);
				sm.addString(targetClan.getName());
				sm.addString(targetClan.getAllyName());
				player.sendPacket(sm);
				return false;
			}
			
			if (penaltyType == PENALTY_TYPE_CLAN_DISMISSED)
			{
				player.sendPacket(SystemMessageId.A_CLAN_CAN_JOIN_ANOTHER_ALLIANCE_IN_24_H_AFTER_LEAVING_THE_PREVIOUS_ONE);
				return false;
			}
		}
		
		if (player.isInsideZone(ZoneId.SIEGE) && target.isInsideZone(ZoneId.SIEGE))
		{
			player.sendPacket(SystemMessageId.THE_OPPOSING_CLAN_IS_PARTICIPATING_IN_A_SIEGE_BATTLE);
			return false;
		}
		
		for (Clan allyClan : ClanTable.getInstance().getClanAllies(playerAllyId))
		{
			if (allyClan.isSiegeEnemyOf(targetClan))
			{
				player.sendPacket(SystemMessageId.AS_THERE_IS_A_CONFLICT_IN_THE_SIEGE_RELATIONSHIP_WITH_A_CLAN_IN_THE_ALLIANCE_YOU_CANNOT_INVITE_THAT_CLAN_TO_THE_ALLIANCE);
				return false;
			}
		}
		
		if (!PlayerConfig.ALT_ALLY_WITH_CLANS_AT_WAR && leaderClan.isAtWarWith(targetClan.getId()))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_MAKE_AN_ALLIANCE_WITH_A_CLAN_YOU_ARE_IN_WAR_WITH);
			return false;
		}
		
		if (ClanTable.getInstance().getClanAllies(playerAllyId).size() >= PlayerConfig.ALT_MAX_NUM_OF_CLANS_IN_ALLY)
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_LIMIT);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Retrieves the expiration time for the ally penalty.<br>
	 * This value is returned as a {@code long}.
	 * @return The expiration time of the ally penalty.
	 */
	public long getAllyPenaltyExpiryTime()
	{
		return _allyPenaltyExpiryTime;
	}
	
	/**
	 * Retrieves the current penalty type for allies.<br>
	 * This value determines how penalties are applied to allied entities.
	 * @return the {@code int} representing the ally penalty type.
	 */
	public int getAllyPenaltyType()
	{
		return _allyPenaltyType;
	}
	
	/**
	 * Sets the expiration time for an ally penalty.<br>
	 * This method updates both the duration and the type of the penalty.
	 * @param expiryTime The time when the penalty should expire.
	 * @param penaltyType The specific type of penalty to apply.
	 */
	public void setAllyPenaltyExpiryTime(long expiryTime, int penaltyType)
	{
		_allyPenaltyExpiryTime = expiryTime;
		_allyPenaltyType = penaltyType;
	}
	
	/**
	 * Retrieves the expiration time for the character penalty.<br>
	 * This value is returned as a {@code long}.
	 * @return The timestamp when the character penalty expires.
	 */
	public long getCharPenaltyExpiryTime()
	{
		return _charPenaltyExpiryTime;
	}
	
	/**
	 * Sets the expiration time for character penalties.<br>
	 * This value determines when a penalty will no longer apply to a character.
	 * @param time The new expiry time in milliseconds.
	 */
	public void setCharPenaltyExpiryTime(long time)
	{
		_charPenaltyExpiryTime = time;
	}
	
	/**
	 * Retrieves the expiration time for the clan dissolution.<br>
	 * This value is used to determine when a clan will be fully removed.
	 * @return The {@code long} timestamp of the dissolution expiry.
	 */
	public long getDissolvingExpiryTime()
	{
		return _dissolvingExpiryTime;
	}
	
	/**
	 * Sets the expiration time for the clan dissolution process.<br>
	 * This value determines when a dissolving clan will be fully removed.
	 * @param time The expiration time in milliseconds.
	 */
	public void setDissolvingExpiryTime(long time)
	{
		_dissolvingExpiryTime = time;
	}
	
	/**
	 * Creates a new alliance for the clan led by the specified player.<br>
	 * This method checks if the leader meets all requirements such as level and status.<br>
	 * It also validates that the provided name is unique and follows naming rules.
	 * @param player The {@code Player} who must be the current clan leader.
	 * @param allyName The desired name for the new alliance.
	 */
	public void createAlly(Player player, String allyName)
	{
		if (player == null)
		{
			return;
		}
		
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.ONLY_CLAN_LEADERS_MAY_CREATE_ALLIANCES);
			return;
		}
		
		if (_allyId != 0)
		{
			player.sendPacket(SystemMessageId.YOU_ALREADY_BELONG_TO_ANOTHER_ALLIANCE);
			return;
		}
		
		if (_level < 5)
		{
			player.sendPacket(SystemMessageId.TO_CREATE_AN_ALLIANCE_YOUR_CLAN_MUST_BE_LV_5_OR_HIGHER);
			return;
		}
		
		if (isInSiegeBattle())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_CREATE_AN_ALLIANCE_WHILE_PARTICIPATING_IN_A_SIEGE);
			return;
		}
		
		if ((_allyPenaltyExpiryTime > System.currentTimeMillis()) && (_allyPenaltyType == PENALTY_TYPE_DISSOLVE_ALLY))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_CREATE_A_NEW_ALLIANCE_WITHIN_1_DAY_OF_DISSOLUTION);
			return;
		}
		
		if (_dissolvingExpiryTime > System.currentTimeMillis())
		{
			player.sendPacket(SystemMessageId.AS_YOU_ARE_CURRENTLY_SCHEDULE_FOR_CLAN_DISSOLUTION_NO_ALLIANCE_CAN_BE_CREATED);
			return;
		}
		
		if (!StringUtil.isAlphaNumeric(allyName))
		{
			player.sendPacket(SystemMessageId.INCORRECT_ALLIANCE_NAME_PLEASE_TRY_AGAIN);
			return;
		}
		
		final int nameLength = allyName.length();
		if ((nameLength > 16) || (nameLength < 2))
		{
			player.sendPacket(SystemMessageId.INCORRECT_LENGTH_FOR_AN_ALLIANCE_NAME);
			return;
		}
		
		if (ClanTable.getInstance().isAllyExists(allyName))
		{
			player.sendPacket(SystemMessageId.THAT_ALLIANCE_NAME_ALREADY_EXISTS);
			return;
		}
		
		setAllyId(_clanId);
		setAllyName(allyName.trim());
		setAllyPenaltyExpiryTime(0, 0);
		updateClanInDB();
		
		player.updateUserInfo();
		
		// TODO: Need correct message id
		player.sendMessage("Alliance " + allyName + " has been created.");
	}
	
	/**
	 * Dissolves the current alliance for the player's clan.<br>
	 * This method checks if the player is a leader and not in a siege zone.<br>
	 * It removes all alliance links from other clans and updates the database.
	 * @param player The {@code Player} who is attempting to dissolve the alliance.
	 */
	public void dissolveAlly(Player player)
	{
		if (_allyId == 0)
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_IN_AN_ALLIANCE);
			return;
		}
		
		if (!player.isClanLeader() || (_clanId != _allyId))
		{
			player.sendPacket(SystemMessageId.THIS_FEATURE_IS_ONLY_AVAILABLE_TO_ALLIANCE_LEADERS);
			return;
		}
		
		if (player.isInsideZone(ZoneId.SIEGE))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_DISSOLVE_AN_ALLIANCE_WHILE_AN_AFFILIATED_CLAN_IS_PARTICIPATING_IN_A_SIEGE_BATTLE);
			return;
		}
		
		for (Clan allyClan : ClanTable.getInstance().getClanAllies(_allyId))
		{
			if (allyClan.isInSiegeBattle())
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_DISSOLVE_AN_ALLIANCE_WHILE_AN_AFFILIATED_CLAN_IS_PARTICIPATING_IN_A_SIEGE_BATTLE);
				return;
			}
		}
		
		broadcastToOnlineAllyMembers(new SystemMessage(SystemMessageId.THE_ALLIANCE_IS_DISBANDED));
		
		final long currentTime = System.currentTimeMillis();
		for (Clan clan : ClanTable.getInstance().getClanAllies(getAllyId()))
		{
			if (clan.getId() != getId())
			{
				clan.setAllyId(0);
				clan.setAllyName(null);
				clan.setAllyPenaltyExpiryTime(0, 0);
				clan.updateClanInDB();
			}
		}
		
		setAllyId(0);
		setAllyName(null);
		changeAllyCrest(0, false);
		setAllyPenaltyExpiryTime(currentTime + (PlayerConfig.ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED * 86400000), PENALTY_TYPE_DISSOLVE_ALLY); // 24*60*60*1000 = 86400000
		updateClanInDB();
	}
	
	/**
	 * Checks if this clan fights as attacker or defender in a castle siege that is in progress.
	 * @return {@code true} if the clan takes part in a running siege, {@code false} otherwise.
	 */
	public boolean isInSiegeBattle()
	{
		for (Siege siege : SiegeManager.getInstance().getSieges())
		{
			if (siege.isInProgress() && (siege.checkIsAttacker(this) || siege.checkIsDefender(this)))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if this clan and the given clan are registered on opposite sides of a castle siege.
	 * @param clan The {@code Clan} to compare with.
	 * @return {@code true} if one clan attacks where the other defends, {@code false} otherwise.
	 */
	public boolean isSiegeEnemyOf(Clan clan)
	{
		for (Siege siege : SiegeManager.getInstance().getSieges())
		{
			if ((siege.checkIsAttacker(this) && siege.checkIsDefender(clan)) || (siege.checkIsDefender(this) && siege.checkIsAttacker(clan)))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Attempts to increase the current clan level.<br>
	 * Checks if the {@code Player} is the leader and meets all required costs.<br>
	 * Deducts necessary resources like SP, Adena, items, or reputation points.<br>
	 * Updates the clan level and notifies relevant systems on success.
	 * @param player The {@code Player} attempting to level up the clan.
	 * @return {@code true} if the level up was successful, {@code false} otherwise.
	 */
	public boolean levelUpClan(Player player)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return false;
		}
		
		if (System.currentTimeMillis() < _dissolvingExpiryTime)
		{
			player.sendPacket(SystemMessageId.AS_YOU_ARE_CURRENTLY_SCHEDULE_FOR_CLAN_DISSOLUTION_YOUR_CLAN_LEVEL_CANNOT_BE_INCREASED);
			return false;
		}
		
		boolean increasedLevel = false;
		if (_level < 15)
		{
			final int requiredReputation = ClanLevelData.getInstance().getLevelExp(_level);
			if (requiredReputation <= _reputationScore)
			{
				setReputationScore(_reputationScore - requiredReputation);
				final SystemMessage cr = new SystemMessage(SystemMessageId.CLAN_REPUTATION_POINTS_S1_2);
				cr.addInt(requiredReputation);
				player.sendPacket(cr);
				
				changeLevel(_level + 1);
				player.sendPacket(new ExPledgeShowInfoUpdate(player));
				increasedLevel = true;
				
				// Notify to scripts.
				if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_CLAN_LEVELUP))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanLvlUp(player, this));
				}
			}
		}
		
		if (!increasedLevel)
		{
			player.sendPacket(SystemMessageId.THE_CONDITIONS_NECESSARY_TO_INCREASE_THE_CLAN_S_LEVEL_HAVE_NOT_BEEN_MET);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Updates the clan level in the database and memory.<br>
	 * This method also updates the leader's siege skills based on the new level.<br>
	 * It broadcasts a notification to all online members about the change.
	 * @param level The new level to set for the clan.
	 */
	public void changeLevel(int level)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE clan_data SET clan_level = ? WHERE clan_id = ?"))
		{
			ps.setInt(1, level);
			ps.setInt(2, _clanId);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "could not increase clan level:" + e.getMessage(), e);
		}
		
		setLevel(level);
		setRank(ClanTable.getInstance().getClanRank(this));
		
		if (_leader.isOnline())
		{
			final Player leader = _leader.getPlayer();
			if (level >= SiegeManager.getInstance().getSiegeClanMinLevel())
			{
				SiegeManager.getInstance().addSiegeSkills(leader);
			}
			else
			{
				SiegeManager.getInstance().removeSiegeSkills(leader);
			}
			
			if (level > 4)
			{
				leader.sendPacket(SystemMessageId.NOW_THAT_YOUR_CLAN_LEVEL_IS_ABOVE_LEVEL_5_IT_CAN_ACCUMULATE_CLAN_REPUTATION);
			}
		}
		
		// The pledge class of every member depends on the clan level.
		for (Player member : getOnlineMembers(0))
		{
			member.setPledgeClass(ClanMember.calculatePledgeClass(member));
			member.broadcastUserInfo();
		}
		
		// Notify all the members about it.
		broadcastToOnlineMembers(new SystemMessage(SystemMessageId.THE_CLAN_S_LEVEL_HAS_INCREASED));
		broadcastToOnlineMembers(new PledgeShowInfoUpdate(this));
	}
	
	/**
	 * Updates the clan crest to a new identifier.<br>
	 * This method removes the old crest if one exists.<br>
	 * It updates the database and broadcasts the change to all online members.
	 * @param crestId The new unique identifier for the clan crest.
	 */
	public void changeClanCrest(int crestId)
	{
		if (_crestId != 0)
		{
			CrestTable.getInstance().removeCrest(getCrestId());
		}
		
		setCrestId(crestId);
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE clan_data SET crest_id = ? WHERE clan_id = ?"))
		{
			ps.setInt(1, crestId);
			ps.setInt(2, _clanId);
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "Could not update crest for clan " + _name + " [" + _clanId + "] : " + e.getMessage(), e);
		}
		
		for (Player member : getOnlineMembers(0))
		{
			member.broadcastUserInfo();
		}
	}
	
	/**
	 * Updates the ally crest for a specific clan or its allies.<br>
	 * This method modifies the database and broadcasts changes to online players.
	 * @param crestId The unique identifier of the new crest to apply.
	 * @param onlyThisClan If {@code true}, only updates the current clan. If {@code false}, it updates all clans associated with the ally.
	 */
	public void changeAllyCrest(int crestId, boolean onlyThisClan)
	{
		String sqlStatement = "UPDATE clan_data SET ally_crest_id = ? WHERE clan_id = ?";
		int allyId = _clanId;
		if (!onlyThisClan)
		{
			if (_allyCrestId != 0)
			{
				CrestTable.getInstance().removeCrest(getAllyCrestId());
			}
			
			sqlStatement = "UPDATE clan_data SET ally_crest_id = ? WHERE ally_id = ?";
			allyId = _allyId;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(sqlStatement))
		{
			ps.setInt(1, crestId);
			ps.setInt(2, allyId);
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "Could not update ally crest for ally/clan id " + allyId + " : " + e.getMessage(), e);
		}
		
		if (onlyThisClan)
		{
			setAllyCrestId(crestId);
			for (Player member : getOnlineMembers(0))
			{
				member.broadcastUserInfo();
			}
		}
		else
		{
			for (Clan clan : ClanTable.getInstance().getClanAllies(getAllyId()))
			{
				clan.setAllyCrestId(crestId);
				for (Player member : clan.getOnlineMembers(0))
				{
					member.broadcastUserInfo();
				}
			}
		}
	}
	
	/**
	 * Updates the large crest associated with this clan.<br>
	 * This method removes the old crest if it exists and saves the new one to the database.<br>
	 * It also broadcasts the updated information to all online members.
	 * @param crestId The unique identifier for the new large crest.
	 */
	public void changeLargeCrest(int crestId)
	{
		if (_crestLargeId != 0)
		{
			CrestTable.getInstance().removeCrest(getCrestLargeId());
		}
		
		setCrestLargeId(crestId);
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE clan_data SET crest_large_id = ? WHERE clan_id = ?"))
		{
			ps.setInt(1, crestId);
			ps.setInt(2, _clanId);
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "Could not update large crest for clan " + _name + " [" + _clanId + "] : " + e.getMessage(), e);
		}
		
		for (Player member : getOnlineMembers(0))
		{
			member.broadcastUserInfo();
		}
	}
	
	/**
	 * Checks if a specific sub-skill can be learned.<br>
	 * It verifies if the requested level is the next available level for any active sub-pledge.
	 * @param skillId The unique identifier of the skill to check.
	 * @param skillLevel The target level of the skill to learn.
	 * @return {@code true} if the skill can be learned, otherwise {@code false}.
	 */
	public boolean isLearnableSubSkill(int skillId, int skillLevel)
	{
		Skill current = _subPledgeSkills.get(skillId);
		
		// is next level?
		if ((current != null) && ((current.getLevel() + 1) == skillLevel))
		{
			return true;
		}
		
		// is first level?
		if ((current == null) && (skillLevel == 1))
		{
			return true;
		}
		
		// other sub-pledges
		for (SubPledge subunit : _subPledges.values())
		{
			// disable academy
			if (subunit.getId() == -1)
			{
				continue;
			}
			
			current = subunit.getSkill(skillId);
			
			// is next level?
			if ((current != null) && ((current.getLevel() + 1) == skillLevel))
			{
				return true;
			}
			
			// is first level?
			if ((current == null) && (skillLevel == 1))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if a specific skill can be learned as part of a sub-pledge.<br>
	 * It verifies if the skill is the next logical step in the progression tree.
	 * @param skill The {@code Skill} object to check.
	 * @param subType The type of sub-pledge associated with the skill.
	 * @return {@code true} if the skill is learnable, otherwise {@code false}.
	 */
	public boolean isLearnableSubPledgeSkill(Skill skill, int subType)
	{
		// academy
		if (subType == -1)
		{
			return false;
		}
		
		final int id = skill.getId();
		Skill current;
		if (subType == 0)
		{
			current = _subPledgeSkills.get(id);
		}
		else
		{
			current = _subPledges.get(subType).getSkill(id);
		}
		
		// is next level?
		if ((current != null) && ((current.getLevel() + 1) == skill.getLevel()))
		{
			return true;
		}
		
		// is first level?
		if ((current == null) && (skill.getLevel() == 1))
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * Retrieves all sub-pledge skills associated with the clan.<br>
	 * This method collects skills from both the main pledge and all subunits.
	 * @return A {@code Collection} of {@code SubPledgeSkill} objects.
	 */
	public Collection<SubPledgeSkill> getAllSubSkills()
	{
		final List<SubPledgeSkill> list = new ArrayList<>();
		for (Skill skill : _subPledgeSkills.values())
		{
			list.add(new SubPledgeSkill(0, skill.getId(), skill.getLevel()));
		}
		
		for (SubPledge subunit : _subPledges.values())
		{
			for (Skill skill : subunit.getSkills())
			{
				list.add(new SubPledgeSkill(subunit.getId(), skill.getId(), skill.getLevel()));
			}
		}
		
		return list;
	}
	
	/**
	 * Sets the ID of the new leader for the clan.<br>
	 * If {@code storeInDb} is {@code true}, it updates the database immediately.
	 * @param objectId The unique identifier of the new leader.
	 * @param storeInDb Whether to save this change to the database.
	 */
	public void setNewLeaderId(int objectId, boolean storeInDb)
	{
		_newLeaderId = objectId;
		if (storeInDb)
		{
			updateClanInDB();
		}
	}
	
	/**
	 * Retrieves the ID of the new clan leader.<br>
	 * This value is used during the process of changing leadership.
	 * @return the {@code int} ID of the new leader.
	 */
	public int getNewLeaderId()
	{
		return _newLeaderId;
	}
	
	/**
	 * Retrieves the {@code Player} object for the new leader.<br>
	 * This method uses the internal ID to fetch the player from the world.
	 * @return the {@code Player} instance of the new leader.
	 */
	public Player getNewLeader()
	{
		return World.getPlayer(_newLeaderId);
	}
	
	/**
	 * Retrieves the name of the new clan leader.<br>
	 * This method looks up the name using the {@code _newLeaderId}.
	 * @return The name of the new leader as a {@code String}.
	 */
	public String getNewLeaderName()
	{
		return PlayerInfoTable.getInstance().getNameById(_newLeaderId);
	}
	
	/**
	 * Retrieves the total number of siege kills for this clan.
	 * @return The current count of siege kills as an {@code int}.
	 */
	public int getSiegeKills()
	{
		return _siegeKills.get();
	}
	
	/**
	 * Retrieves the total number of deaths during a siege.
	 * @return The current count of siege deaths as an {@code int}.
	 */
	public int getSiegeDeaths()
	{
		return _siegeDeaths.get();
	}
	
	/**
	 * Increments the total number of siege kills for the clan.<br>
	 * This method updates the internal counter and returns the new value.
	 * @return The updated count of siege kills as an {@code int}.
	 */
	public int addSiegeKill()
	{
		return _siegeKills.incrementAndGet();
	}
	
	/**
	 * Increments the total number of siege deaths.<br>
	 * This method updates the internal counter for siege fatalities.
	 * @return The new total count of siege deaths as an {@code int}.
	 */
	public int addSiegeDeath()
	{
		return _siegeDeaths.incrementAndGet();
	}
	
	/**
	 * Resets the siege kills counter to {@code 0}.<br>
	 * This method clears the current progress for siege kills.
	 */
	public void clearSiegeKills()
	{
		_siegeKills.set(0);
	}
	
	/**
	 * Resets the siege death counter to {@code 0}.<br>
	 * This method clears the current count of deaths recorded during a siege.
	 */
	public void clearSiegeDeaths()
	{
		_siegeDeaths.set(0);
	}
	
	/**
	 * Retrieves the total number of clans currently at war.<br>
	 * This method returns the size of the {@code _atWarWith} collection.
	 * @return The count of clans in a state of war.
	 */
	public int getWarCount()
	{
		return _atWarWith.size();
	}
	
	/**
	 * Adds a {@code ClanWar} object to the list of active wars for a specific clan.<br>
	 * This method maps the {@code clanId} to its corresponding {@code ClanWar} instance.
	 * @param clanId The unique identifier of the clan.
	 * @param war The {@code ClanWar} object to associate with the clan.
	 */
	public void addWar(int clanId, ClanWar war)
	{
		_atWarWith.put(clanId, war);
	}
	
	/**
	 * Removes a clan from the list of clans currently at war.<br>
	 * This method updates the internal state to end the conflict with the specified ID.
	 * @param clanId The unique identifier of the clan to remove from the war status.
	 */
	public void deleteWar(int clanId)
	{
		_atWarWith.remove(clanId);
	}
	
	/**
	 * Retrieves the {@code ClanWar} object associated with a specific clan.<br>
	 * This method looks up the war status using the provided unique identifier.
	 * @param clanId The unique ID of the clan to check.
	 * @return The {@code ClanWar} object, or {@code null} if no war is found.
	 */
	public ClanWar getWarWith(int clanId)
	{
		return _atWarWith.get(clanId);
	}
	
	/**
	 * Adds a specific mastery to the clan.<br>
	 * This method sets the mastery status for the given {@code id} to {@code true}.
	 * @param id The unique identifier of the mastery to add.
	 */
	public void addMastery(int id)
	{
		getVariables().set(ClanVariables.CLAN_MASTERY + id, true);
	}
	
	/**
	 * Checks if a specific mastery is unlocked for the clan.<br>
	 * This method retrieves the boolean value associated with the given {@code id}.<br>
	 * It returns {@code false} if the mastery has not been set.
	 * @param id The unique identifier of the mastery to check.
	 * @return {@code true} if the mastery is unlocked, {@code false} otherwise.
	 */
	public boolean hasMastery(int id)
	{
		return getVariables().getBoolean(ClanVariables.CLAN_MASTERY + id, false);
	}
	
	/**
	 * Removes all masteries associated with the clan.<br>
	 * This method clears clan variables and removes all related skills.<br>
	 * It also cancels any active mastery skill tasks and clears the task list.
	 */
	public void removeAllMasteries()
	{
		for (ClanMasteryHolder mastery : ClanMasteryData.getInstance().getMasteries())
		{
			getVariables().remove(ClanVariables.CLAN_MASTERY + mastery.getId());
			for (Skill skill : mastery.getSkills())
			{
				removeSkill(skill);
			}
		}
		
		for (ScheduledFuture<?> task : masterySkillTasks)
		{
			if ((task != null) && !task.isDone())
			{
				task.cancel(true);
			}
		}
		
		masterySkillTasks.clear();
		removeMasterySkill(19538);
		removeMasterySkill(19539);
		removeMasterySkill(19540);
		removeMasterySkill(19541);
		removeMasterySkill(19542);
	}
	
	/**
	 * Adds a new mastery skill to the clan.<br>
	 * This method sets the expiration time for the skill.<br>
	 * It schedules a task to remove the skill after 15 days.<br>
	 * The skill is then added to the clan's active skills.
	 * @param id The unique identifier of the skill to add.
	 */
	public void addMasterySkill(int id)
	{
		getVariables().set(ClanVariables.CLAN_MASTERY_SKILL_TIME + id, System.currentTimeMillis() + 1296000000);
		final ScheduledFuture<?> task = ThreadPool.schedule(() -> removeMasterySkill(id), 1296000000); // 1296000000 = 15 days
		masterySkillTasks.add(task);
		addNewSkill(SkillData.getInstance().getSkill(id, 1));
	}
	
	/**
	 * Removes a specific mastery skill from the clan.<br>
	 * This method deletes the skill variable and removes the skill data.
	 * @param id The unique identifier of the skill to remove.
	 */
	public void removeMasterySkill(int id)
	{
		getVariables().remove(ClanVariables.CLAN_MASTERY_SKILL_TIME + id);
		removeSkill(SkillData.getInstance().getSkill(id, 1));
	}
	
	/**
	 * Calculates the remaining time for a specific mastery skill.<br>
	 * It retrieves the end timestamp from the clan variables.<br>
	 * If no end time is set, it returns -1.
	 * @param id The unique identifier of the mastery skill.
	 * @return The remaining time in milliseconds, or -1 if not active.
	 */
	public int getMasterySkillRemainingTime(int id)
	{
		final long endTime = getVariables().getLong(ClanVariables.CLAN_MASTERY_SKILL_TIME + id, 0);
		if (endTime == 0)
		{
			return -1;
		}
		
		return (int) (endTime - System.currentTimeMillis());
	}
	
	/**
	 * Sets the number of development points for the clan.<br>
	 * This method updates the {@code ClanVariables.CLAN_DEVELOPMENT_POINTS} value.<br>
	 * It also triggers a save operation to persist the change.
	 * @param count The new amount of development points to set.
	 */
	public void setDevelopmentPoints(int count)
	{
		final ClanVariables variables = getVariables();
		variables.set(ClanVariables.CLAN_DEVELOPMENT_POINTS, count);
		variables.storeMe();
	}
	
	/**
	 * Retrieves the current amount of development points used by the clan.<br>
	 * This value is fetched from the {@code ClanVariables} storage.
	 * @return The number of development points currently in use.
	 */
	public int getUsedDevelopmentPoints()
	{
		return getVariables().getInt(ClanVariables.CLAN_DEVELOPMENT_POINTS, 0);
	}
	
	/**
	 * Calculates the total development points available for the clan.<br>
	 * This value is based on the current level of the {@code Clan}.<br>
	 * It returns a minimum value of 0.
	 * @return The calculated number of development points as an {@code int}.
	 */
	public int getTotalDevelopmentPoints()
	{
		return Math.max(0, _level - 4);
	}
	
	/**
	 * Retrieves the variables associated with this clan.<br>
	 * This method initializes the {@code ClanVariables} object if it does not exist.<br>
	 * It also starts a background task to store these variables periodically based on the configuration.
	 * @return the {@code ClanVariables} instance for this clan.
	 */
	public ClanVariables getVariables()
	{
		if (_vars == null)
		{
			synchronized (this)
			{
				if (_vars == null)
				{
					_vars = new ClanVariables(_clanId);
					if (GeneralConfig.CLAN_VARIABLES_STORE_INTERVAL > 0)
					{
						ThreadPool.scheduleAtFixedRate(this::storeVariables, GeneralConfig.CLAN_VARIABLES_STORE_INTERVAL, GeneralConfig.CLAN_VARIABLES_STORE_INTERVAL);
					}
				}
			}
		}
		
		return _vars;
	}
	
	/**
	 * Checks if the clan has any variables assigned.
	 * @return {@code true} if {@code _vars} is not {@code null}, {@code false} otherwise.
	 */
	public boolean hasVariables()
	{
		return _vars != null;
	}
	
	/**
	 * Saves the current clan variables to the database.<br>
	 * This method checks if {@code _vars} is not {@code null}.<br>
	 * If it exists, it calls the {@code storeMe()} method on the {@code ClanVariables} object.
	 */
	private void storeVariables()
	{
		final ClanVariables vars = _vars;
		if (vars != null)
		{
			vars.storeMe();
		}
	}
}
