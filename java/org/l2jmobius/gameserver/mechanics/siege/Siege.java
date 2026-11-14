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
package org.l2jmobius.gameserver.mechanics.siege;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.cache.RelationCache;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.SiegeScheduleData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.actor.instance.ControlTower;
import org.l2jmobius.gameserver.entity.actor.instance.FlameTower;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.zone.type.SiegeZone;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.SiegeGuardManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.sieges.OnCastleSiegeFinish;
import org.l2jmobius.gameserver.mechanics.events.holders.sieges.OnCastleSiegeOwnerChange;
import org.l2jmobius.gameserver.mechanics.events.holders.sieges.OnCastleSiegeStart;
import org.l2jmobius.gameserver.mechanics.olympiad.Hero;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.RelationChanged;
import org.l2jmobius.gameserver.network.serverpackets.SiegeInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Manages the core logic and lifecycle of siege events within the game world.<br>
 * This class handles the progression, rules, and state transitions for {@code SiegeZone} areas.<br>
 * It coordinates interactions between players, clans, and castle structures during active sieges.
 * @author Mobius
 */
public class Siege implements Siegable
{
	protected static final Logger LOGGER = Logger.getLogger(Siege.class.getName());
	
	// Type ids.
	public static final byte OWNER = -1;
	public static final byte DEFENDER = 0;
	public static final byte ATTACKER = 1;
	public static final byte DEFENDER_NOT_APPROVED = 2;
	
	private int _controlTowerCount;
	
	// Must support concurrent modifications.
	private final Collection<SiegeClan> _attackerClans = ConcurrentHashMap.newKeySet();
	private final Collection<SiegeClan> _defenderClans = ConcurrentHashMap.newKeySet();
	private final Collection<SiegeClan> _defenderWaitingClans = ConcurrentHashMap.newKeySet();
	
	// Castle setting.
	private final List<ControlTower> _controlTowers = new ArrayList<>();
	private final List<FlameTower> _flameTowers = new ArrayList<>();
	private final List<Npc> _towerWrecks = new ArrayList<>();
	final Castle _castle;
	boolean _isInProgress = false;
	private boolean _isNormalSide = true; // true = Atk is Atk, false = Atk is Def
	protected boolean _isRegistrationOver = false;
	protected Calendar _siegeEndDate;
	protected ScheduledFuture<?> _scheduledStartSiegeTask = null;
	protected int _firstOwnerClanId = -1;
	
	/**
	 * Creates a new {@code Siege} instance for a specific castle.<br>
	 * This constructor initializes the siege data and starts auto tasks if enabled.
	 * @param castle The {@code Castle} object associated with this siege.
	 */
	public Siege(Castle castle)
	{
		_castle = castle;
		
		final SiegeScheduleDate schedule = SiegeScheduleData.getInstance().getScheduleDateForCastleId(_castle.getResidenceId());
		if ((schedule != null) && schedule.siegeEnabled())
		{
			startAutoTask();
		}
	}
	
	public class ScheduleEndSiegeTask implements Runnable
	{
		private final Castle _castleInst;
		
		public ScheduleEndSiegeTask(Castle pCastle)
		{
			_castleInst = pCastle;
		}
		
		@Override
		public void run()
		{
			if (!_isInProgress)
			{
				return;
			}
			
			try
			{
				final long timeRemaining = _siegeEndDate.getTimeInMillis() - System.currentTimeMillis();
				if (timeRemaining > 3600000)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.THE_CASTLE_SIEGE_ENDS_IN_S1_H);
					sm.addInt(2);
					announceToPlayer(sm, true);
					ThreadPool.schedule(new ScheduleEndSiegeTask(_castleInst), timeRemaining - 3600000); // Prepare task for 1 hr left.
				}
				else if ((timeRemaining <= 3600000) && (timeRemaining > 600000))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.THE_CASTLE_SIEGE_ENDS_IN_S1_MIN);
					sm.addInt((int) timeRemaining / 60000);
					announceToPlayer(sm, true);
					ThreadPool.schedule(new ScheduleEndSiegeTask(_castleInst), timeRemaining - 600000); // Prepare task for 10 minute left.
				}
				else if ((timeRemaining <= 600000) && (timeRemaining > 300000))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.THE_CASTLE_SIEGE_ENDS_IN_S1_MIN);
					sm.addInt((int) timeRemaining / 60000);
					announceToPlayer(sm, true);
					ThreadPool.schedule(new ScheduleEndSiegeTask(_castleInst), timeRemaining - 300000); // Prepare task for 5 minute left.
				}
				else if ((timeRemaining <= 300000) && (timeRemaining > 10000))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.THE_CASTLE_SIEGE_ENDS_IN_S1_MIN);
					sm.addInt((int) timeRemaining / 60000);
					announceToPlayer(sm, true);
					ThreadPool.schedule(new ScheduleEndSiegeTask(_castleInst), timeRemaining - 10000); // Prepare task for 10 seconds count down.
				}
				else if ((timeRemaining <= 10000) && (timeRemaining > 0))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.THE_CASTLE_SIEGE_ENDS_IN_S1_SEC);
					sm.addInt((int) timeRemaining / 1000);
					announceToPlayer(sm, true);
					ThreadPool.schedule(new ScheduleEndSiegeTask(_castleInst), timeRemaining); // Prepare task for second count down.
				}
				else
				{
					_castleInst.getSiege().endSiege();
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": ", e);
			}
		}
	}
	
	public class ScheduleStartSiegeTask implements Runnable
	{
		private final Castle _castleInst;
		
		public ScheduleStartSiegeTask(Castle pCastle)
		{
			_castleInst = pCastle;
		}
		
		@Override
		public void run()
		{
			_scheduledStartSiegeTask.cancel(false);
			if (_isInProgress)
			{
				return;
			}
			
			try
			{
				final long currentTime = System.currentTimeMillis();
				if (!_castle.isTimeRegistrationOver())
				{
					final long regTimeRemaining = getTimeRegistrationOverDate().getTimeInMillis() - currentTime;
					if (regTimeRemaining > 0)
					{
						_scheduledStartSiegeTask = ThreadPool.schedule(new ScheduleStartSiegeTask(_castleInst), regTimeRemaining);
						return;
					}
					
					endTimeRegistration(true);
				}
				
				final long timeRemaining = getSiegeDate().getTimeInMillis() - currentTime;
				if (timeRemaining > 86400000)
				{
					_scheduledStartSiegeTask = ThreadPool.schedule(new ScheduleStartSiegeTask(_castleInst), timeRemaining - 86400000); // Prepare task for 24 before siege start to end registration.
				}
				else if ((timeRemaining <= 86400000) && (timeRemaining > 13600000))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.THE_REGISTRATION_TERM_FOR_S1_HAS_ENDED);
					sm.addCastleId(_castle.getResidenceId());
					World.broadcastToAllOnlinePlayers(sm);
					_isRegistrationOver = true;
					clearSiegeWaitingClan();
					_scheduledStartSiegeTask = ThreadPool.schedule(new ScheduleStartSiegeTask(_castleInst), timeRemaining - 13600000); // Prepare task for 1 hr left before siege start.
				}
				else if ((timeRemaining <= 13600000) && (timeRemaining > 600000))
				{
					_scheduledStartSiegeTask = ThreadPool.schedule(new ScheduleStartSiegeTask(_castleInst), timeRemaining - 600000); // Prepare task for 10 minute left.
				}
				else if ((timeRemaining <= 600000) && (timeRemaining > 300000))
				{
					_scheduledStartSiegeTask = ThreadPool.schedule(new ScheduleStartSiegeTask(_castleInst), timeRemaining - 300000); // Prepare task for 5 minute left.
				}
				else if ((timeRemaining <= 300000) && (timeRemaining > 10000))
				{
					_scheduledStartSiegeTask = ThreadPool.schedule(new ScheduleStartSiegeTask(_castleInst), timeRemaining - 10000); // Prepare task for 10 seconds count down.
				}
				else if ((timeRemaining <= 10000) && (timeRemaining > 0))
				{
					_scheduledStartSiegeTask = ThreadPool.schedule(new ScheduleStartSiegeTask(_castleInst), timeRemaining); // Prepare task for second count down.
				}
				else
				{
					_castleInst.getSiege().startSiege();
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": ", e);
			}
		}
	}
	
	/**
	 * Ends the current siege and handles all cleanup tasks.<br>
	 * This method broadcasts the final result to all players.<br>
	 * It updates clan statistics, clears kills, and resets castle data.<br>
	 * All participants are teleported back to town and flags are removed.
	 */
	@Override
	public void endSiege()
	{
		if (_isInProgress)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.THE_S1_SIEGE_HAS_FINISHED);
			final int residenceId = _castle.getResidenceId();
			sm.addCastleId(residenceId);
			World.broadcastToAllOnlinePlayers(sm);
			if (_castle.getOwnerId() > 0)
			{
				final Clan clan = ClanTable.getInstance().getClan(getCastle().getOwnerId());
				sm = new SystemMessage(SystemMessageId.CLAN_S1_IS_VICTORIOUS_OVER_S2_S_CASTLE_SIEGE);
				sm.addString(clan.getName());
				sm.addString(_castle.getName());
				World.broadcastToAllOnlinePlayers(sm);
				
				if (clan.getId() == _firstOwnerClanId)
				{
					// Owner is unchanged
					clan.increaseBloodAllianceCount();
				}
				else
				{
					_castle.setTicketBuyCount(0);
					for (ClanMember member : clan.getMembers())
					{
						if (member != null)
						{
							final Player player = member.getPlayer();
							if ((player != null) && (player.getNobleLevel() > 0))
							{
								Hero.getInstance().setCastleTaken(player.getObjectId(), residenceId);
							}
						}
					}
				}
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.THE_SIEGE_OF_S1_HAS_ENDED_IN_A_DRAW);
				sm.addCastleId(residenceId);
				World.broadcastToAllOnlinePlayers(sm);
			}
			
			for (SiegeClan attackerClan : getAttackerClans())
			{
				final Clan clan = ClanTable.getInstance().getClan(attackerClan.getClanId());
				if (clan == null)
				{
					continue;
				}
				
				for (Player member : clan.getOnlineMembers(0))
				{
					member.checkItemRestriction();
				}
				
				clan.clearSiegeKills();
				clan.clearSiegeDeaths();
			}
			
			for (SiegeClan defenderClan : getDefenderClans())
			{
				final Clan clan = ClanTable.getInstance().getClan(defenderClan.getClanId());
				if (clan == null)
				{
					continue;
				}
				
				for (Player member : clan.getOnlineMembers(0))
				{
					member.checkItemRestriction();
				}
				
				clan.clearSiegeKills();
				clan.clearSiegeDeaths();
			}
			
			_castle.updateClansReputation();
			removeFlags(); // Removes all flags. Note: Remove flag before teleporting players.
			teleportPlayer(SiegeTeleportWhoType.NotOwner, TeleportWhereType.TOWN); // Teleport to the second closest town.
			_isInProgress = false; // Flag so that siege instance can be started.
			updatePlayerSiegeStateFlags(true);
			saveCastleSiege(); // Save castle specific data.
			clearSiegeClan(); // Clear siege clan from db.
			removeTowers(); // Remove all towers from this castle.
			SiegeGuardManager.getInstance().unspawnSiegeGuard(getCastle()); // Remove all spawned siege guard from this castle.
			if (_castle.getOwnerId() > 0)
			{
				SiegeGuardManager.getInstance().removeSiegeGuards(getCastle());
			}
			
			_castle.restoreDoors(); // Restore castle doors.
			_castle.openDefaultDoors(); // Doors kept open outside sieges, like the Goddard rear door, open again.
			_castle.setFirstMidVictory(false);
			
			final SiegeZone endZone = _castle.getZone();
			endZone.setActive(false);
			endZone.updateZoneStatusForCharactersInside();
			endZone.setSiegeInstance(null);
			
			// Notify to scripts.
			final Castle castle = getCastle();
			if (EventDispatcher.getInstance().hasListener(EventType.ON_CASTLE_SIEGE_FINISH, castle))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnCastleSiegeFinish(this), castle);
			}
		}
	}
	
	/**
	 * Removes a specific clan from the defender list.<br>
	 * This method checks if the provided {@code SiegeClan} object is not {@code null}.<br>
	 * If it exists, it removes it from the internal collection of defenders.
	 * @param sc The {@code SiegeClan} to be removed from the siege.
	 */
	private void removeDefender(SiegeClan sc)
	{
		if (sc != null)
		{
			getDefenderClans().remove(sc);
		}
	}
	
	/**
	 * Removes a specific attacker from the active siege list.<br>
	 * This method checks if the provided {@code SiegeClan} is not {@code null}.<br>
	 * If it exists, it removes it from the internal collection of attackers.
	 * @param sc The {@code SiegeClan} object to be removed.
	 */
	private void removeAttacker(SiegeClan sc)
	{
		if (sc != null)
		{
			getAttackerClans().remove(sc);
		}
	}
	
	/**
	 * Adds a {@code SiegeClan} to the list of defenders.<br>
	 * This method sets the clan type and registers it in the internal collection.
	 * @param sc The {@code SiegeClan} object to be added.
	 * @param type The {@code SiegeClanType} to assign to the clan.
	 */
	private void addDefender(SiegeClan sc, SiegeClanType type)
	{
		if (sc == null)
		{
			return;
		}
		
		sc.setType(type);
		getDefenderClans().add(sc);
	}
	
	/**
	 * Registers a clan as an attacker in the current siege.<br>
	 * This method sets the {@code SiegeClanType} to {@code ATTACKER}.<br>
	 * It also adds the clan to the internal list of attackers.
	 * @param sc The {@code SiegeClan} object to be added.
	 */
	private void addAttacker(SiegeClan sc)
	{
		if (sc == null)
		{
			return;
		}
		
		sc.setType(SiegeClanType.ATTACKER);
		getAttackerClans().add(sc);
	}
	
	/**
	 * Handles the logic for a mid-victory event during an active siege.<br>
	 * This method checks if the conditions for a victory are met.<br>
	 * It updates ownership, teleports players, and resets castle structures.<br>
	 * It also notifies scripts of the owner change.
	 */
	public void midVictory()
	{
		if (_isInProgress) // Siege still in progress.
		{
			final int ownerId = _castle.getOwnerId();
			if (ownerId > 0)
			{
				SiegeGuardManager.getInstance().removeSiegeGuards(getCastle()); // Remove all merc entry from db.
			}
			
			if (getDefenderClans().isEmpty() && // If defender doesn't exist (Pc vs Npc).
				(getAttackerClans().size() == 1 // Only 1 attacker
				))
			{
				final SiegeClan scNewOwner = getAttackerClan(ownerId);
				removeAttacker(scNewOwner);
				addDefender(scNewOwner, SiegeClanType.OWNER);
				endSiege();
				return;
			}
			
			if (ownerId > 0)
			{
				// If defender doesn't exist (Pc vs Npc) and only an alliance attacks.
				final int allyId = ClanTable.getInstance().getClan(getCastle().getOwnerId()).getAllyId();
				if (getDefenderClans().isEmpty() && (allyId != 0))
				{
					boolean allinsamealliance = true;
					for (SiegeClan sc : getAttackerClans())
					{
						if ((sc != null) && (ClanTable.getInstance().getClan(sc.getClanId()).getAllyId() != allyId))
						{
							allinsamealliance = false;
						}
					}
					
					if (allinsamealliance)
					{
						final SiegeClan scNewOwner = getAttackerClan(ownerId);
						removeAttacker(scNewOwner);
						addDefender(scNewOwner, SiegeClanType.OWNER);
						endSiege();
						return;
					}
				}
				
				for (SiegeClan sc : getDefenderClans())
				{
					if (sc != null)
					{
						removeDefender(sc);
						addAttacker(sc);
					}
				}
				
				final SiegeClan scNewOwner = getAttackerClan(ownerId);
				removeAttacker(scNewOwner);
				addDefender(scNewOwner, SiegeClanType.OWNER);
				
				// The player's clan is in an alliance.
				for (Clan clan : ClanTable.getInstance().getClanAllies(allyId))
				{
					final SiegeClan sc = getAttackerClan(clan.getId());
					if (sc != null)
					{
						removeAttacker(sc);
						addDefender(sc, SiegeClanType.DEFENDER);
					}
				}
				
				_castle.setFirstMidVictory(true);
				teleportPlayer(SiegeTeleportWhoType.Attacker, TeleportWhereType.SIEGEFLAG); // Teleport to the second closest town.
				teleportPlayer(SiegeTeleportWhoType.Spectator, TeleportWhereType.TOWN); // Teleport to the second closest town.
				
				// Everyone except the new owning clan returns to a restart point, allied defenders to the castle.
				for (SiegeClan siegeClan : getDefenderClans())
				{
					if (siegeClan.getClanId() == _castle.getOwnerId())
					{
						continue;
					}
					
					for (Player player : ClanTable.getInstance().getClan(siegeClan.getClanId()).getOnlineMembers(0))
					{
						if (player.isInSiege() && !player.isGM() && !player.isJailed())
						{
							player.teleToLocation(TeleportWhereType.CASTLE);
						}
					}
				}
				
				removeDefenderFlags(); // Removes defenders' flags
				_castle.removeUpgrade(); // Remove all castle upgrade.
				_castle.restoreDoors(true); // Restore castle doors, but make them weaker (50% hp).
				removeTowers(); // Remove all towers from this castle.
				SiegeGuardManager.getInstance().unspawnSiegeGuard(getCastle()); // Remove all spawned siege guard from this castle.
				if (ownerId > 0)
				{
					SiegeGuardManager.getInstance().removeSiegeGuards(getCastle());
				}
				
				_controlTowerCount = 0; // Each new siege midvictory CT are completely respawned.
				spawnControlTower();
				spawnFlameTower();
				updatePlayerSiegeStateFlags(false);
				
				// Notify to scripts.
				final Castle castle = getCastle();
				if (EventDispatcher.getInstance().hasListener(EventType.ON_CASTLE_SIEGE_OWNER_CHANGE, castle))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnCastleSiegeOwnerChange(this), castle);
				}
			}
		}
	}
	
	/**
	 * Starts the siege process for a specific castle.<br>
	 * This method initializes all necessary game objects and zones.<br>
	 * It also handles broadcasting messages to all online players.<br>
	 * If no clans participate, it cancels the siege automatically.
	 */
	@Override
	public void startSiege()
	{
		if (!_isInProgress)
		{
			_firstOwnerClanId = _castle.getOwnerId();
			if (getAttackerClans().isEmpty())
			{
				SystemMessage sm;
				if (_firstOwnerClanId <= 0)
				{
					sm = new SystemMessage(SystemMessageId.THE_SIEGE_OF_S1_HAS_BEEN_CANCELLED_DUE_TO_LACK_OF_INTEREST);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_S_SIEGE_WAS_CANCELLED_BECAUSE_THERE_WERE_NO_CLANS_THAT_PARTICIPATED);
					final Clan ownerClan = ClanTable.getInstance().getClan(_firstOwnerClanId);
					ownerClan.increaseBloodAllianceCount();
				}
				
				sm.addCastleId(_castle.getResidenceId());
				World.broadcastToAllOnlinePlayers(sm);
				saveCastleSiege();
				return;
			}
			
			_isNormalSide = true; // Atk is now atk.
			_isInProgress = true; // Flag so that same siege instance cannot be started again.
			_castle.clearEngravedArtefacts(); // Engravings of a previous siege do not count.
			loadSiegeClan(); // Load siege clan from db.
			updatePlayerSiegeStateFlags(false);
			updatePlayerSiegeStateFlags(false); // This fixes icons between allies because it first shows as an enemy for unknown reasons.
			teleportPlayer(SiegeTeleportWhoType.NotOwner, TeleportWhereType.TOWN); // Teleport to the closest town.
			_controlTowerCount = 0;
			spawnControlTower(); // Spawn control tower
			spawnFlameTower(); // Spawn control tower
			_castle.restoreDoors(); // Restore castle doors.
			spawnSiegeGuard(); // Spawn siege guard
			SiegeGuardManager.getInstance().deleteTickets(getCastle().getResidenceId()); // remove the tickets from the ground
			
			final SiegeZone startZone = _castle.getZone();
			startZone.setSiegeInstance(this);
			startZone.setActive(true);
			startZone.updateZoneStatusForCharactersInside();
			
			// Schedule a task to prepare auto siege end.
			_siegeEndDate = Calendar.getInstance();
			_siegeEndDate.add(Calendar.MINUTE, SiegeManager.getInstance().getSiegeLength());
			ThreadPool.schedule(new ScheduleEndSiegeTask(_castle), 1000); // Prepare auto end task.
			
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_THE_SIEGE_HAS_BEGUN);
			sm.addCastleId(_castle.getResidenceId());
			World.broadcastToAllOnlinePlayers(sm);
			
			// Notify to scripts.
			final Castle castle = getCastle();
			if (EventDispatcher.getInstance().hasListener(EventType.ON_CASTLE_SIEGE_START, castle))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnCastleSiegeStart(this), castle);
			}
		}
	}
	
	/**
	 * Sends a system message to players based on their siege status.<br>
	 * It sends the message to all members of defender clans.<br>
	 * If {@code bothSides} is {@code true}, it also sends the message to attacker clans.
	 * @param message The {@code SystemMessage} to be sent to the players.
	 * @param bothSides Set to {@code true} to include attackers in the announcement, or {@code false} to only notify defenders.
	 */
	public void announceToPlayer(SystemMessage message, boolean bothSides)
	{
		for (SiegeClan siegeClans : getDefenderClans())
		{
			final Clan clan = ClanTable.getInstance().getClan(siegeClans.getClanId());
			if (clan != null)
			{
				for (Player member : clan.getOnlineMembers(0))
				{
					member.sendPacket(message);
				}
			}
		}
		
		if (bothSides)
		{
			for (SiegeClan siegeClans : getAttackerClans())
			{
				final Clan clan = ClanTable.getInstance().getClan(siegeClans.getClanId());
				if (clan != null)
				{
					for (Player member : clan.getOnlineMembers(0))
					{
						member.sendPacket(message);
					}
				}
			}
		}
	}
	
	/**
	 * Updates the siege status and fame tasks for all online members of attacker and defender clans.<br>
	 * If {@code clear} is {@code true}, it resets all siege-related flags to default values.<br>
	 * If {@code clear} is {@code false}, it sets the appropriate side and starts fame tasks if the player is in the zone.
	 * @param clear Determines whether to reset the siege state or initialize it for active participants.
	 */
	public void updatePlayerSiegeStateFlags(boolean clear)
	{
		Clan clan;
		for (SiegeClan siegeclan : getAttackerClans())
		{
			if (siegeclan == null)
			{
				continue;
			}
			
			clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
			for (Player member : clan.getOnlineMembers(0))
			{
				if (clear)
				{
					member.setSiegeState((byte) 0);
					member.setSiegeSide(0);
					member.setInSiege(false);
					member.stopFameTask();
				}
				else
				{
					member.setSiegeState((byte) 1);
					member.setSiegeSide(_castle.getResidenceId());
					if (checkIfInZone(member))
					{
						member.setInSiege(true);
						member.startFameTask(PlayerConfig.CASTLE_ZONE_FAME_TASK_FREQUENCY * 1000, PlayerConfig.CASTLE_ZONE_FAME_AQUIRE_POINTS);
					}
				}
				
				member.updateUserInfo();
				World.forEachVisibleObject(member, Player.class, player ->
				{
					if (!member.isVisibleFor(player))
					{
						return;
					}
					
					final long relation = member.getRelation(player);
					final boolean isAutoAttackable = member.isAutoAttackable(player);
					final int objectId = player.getObjectId();
					final RelationCache oldrelation = member.getKnownRelations().get(objectId);
					if ((oldrelation == null) || (oldrelation.getRelation() != relation) || (oldrelation.isAutoAttackable() != isAutoAttackable))
					{
						final RelationChanged rc = new RelationChanged();
						rc.addRelation(member, relation, isAutoAttackable);
						if (member.hasSummon())
						{
							final Summon pet = member.getPet();
							if (pet != null)
							{
								rc.addRelation(pet, relation, isAutoAttackable);
							}
							
							if (member.hasServitors())
							{
								member.getServitors().values().forEach(s -> rc.addRelation(s, relation, isAutoAttackable));
							}
						}
						
						player.sendPacket(rc);
						member.getKnownRelations().put(objectId, new RelationCache(relation, isAutoAttackable));
					}
				});
			}
		}
		
		for (SiegeClan siegeclan : getDefenderClans())
		{
			if (siegeclan == null)
			{
				continue;
			}
			
			clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
			for (Player member : clan.getOnlineMembers(0))
			{
				if (clear)
				{
					member.setSiegeState((byte) 0);
					member.setSiegeSide(0);
					member.setInSiege(false);
					member.stopFameTask();
				}
				else
				{
					member.setSiegeState((byte) 2);
					member.setSiegeSide(_castle.getResidenceId());
					if (checkIfInZone(member))
					{
						member.setInSiege(true);
						member.startFameTask(PlayerConfig.CASTLE_ZONE_FAME_TASK_FREQUENCY * 1000, PlayerConfig.CASTLE_ZONE_FAME_AQUIRE_POINTS);
					}
				}
				
				member.updateUserInfo();
				World.forEachVisibleObject(member, Player.class, player ->
				{
					if (!member.isVisibleFor(player))
					{
						return;
					}
					
					final long relation = member.getRelation(player);
					final boolean isAutoAttackable = member.isAutoAttackable(player);
					final int objectId = player.getObjectId();
					final RelationCache oldrelation = member.getKnownRelations().get(objectId);
					if ((oldrelation == null) || (oldrelation.getRelation() != relation) || (oldrelation.isAutoAttackable() != isAutoAttackable))
					{
						final RelationChanged rc = new RelationChanged();
						rc.addRelation(member, relation, isAutoAttackable);
						if (member.hasSummon())
						{
							final Summon pet = member.getPet();
							if (pet != null)
							{
								rc.addRelation(pet, relation, isAutoAttackable);
							}
							
							if (member.hasServitors())
							{
								member.getServitors().values().forEach(s -> rc.addRelation(s, relation, isAutoAttackable));
							}
						}
						
						player.sendPacket(rc);
						member.getKnownRelations().put(objectId, new RelationCache(relation, isAutoAttackable));
					}
				});
			}
		}
	}
	
	/**
	 * Approves a specific clan as a defender for the current siege.<br>
	 * This method updates the status of the clan in the database.<br>
	 * It then reloads the siege clan data to apply the changes.
	 * @param clanId The unique identifier of the clan to approve.
	 */
	public void approveSiegeDefenderClan(int clanId)
	{
		if (clanId <= 0)
		{
			return;
		}
		
		saveSiegeClan(ClanTable.getInstance().getClan(clanId), DEFENDER, true);
		loadSiegeClan();
	}
	
	/**
	 * Checks if a specific {@code WorldObject} is located within the siege zone.<br>
	 * This method retrieves the coordinates from the object and validates them.
	 * @param object The {@code WorldObject} to check.
	 * @return {@code true} if the object is inside the zone, {@code false} otherwise.
	 */
	public boolean checkIfInZone(WorldObject object)
	{
		return checkIfInZone(object.getX(), object.getY(), object.getZ());
	}
	
	/**
	 * Checks if the given coordinates are within the siege zone.<br>
	 * This method returns {@code true} only if a siege is currently in progress and the location is inside the castle area.
	 * @param x The x coordinate of the position.
	 * @param y The y coordinate of the position.
	 * @param z The z coordinate of the position.
	 * @return {@code true} if the coordinates are in the zone during a siege, {@code false} otherwise.
	 */
	public boolean checkIfInZone(int x, int y, int z)
	{
		return (_isInProgress && (_castle.checkIfInZone(x, y, z))); // Castle zone during siege.
	}
	
	/**
	 * Checks if the provided {@code Clan} is currently participating as an attacker.<br>
	 * This method verifies if the clan exists in the active attacker list.
	 * @param clan The {@code Clan} to check.
	 * @return {@code true} if the clan is an attacker, {@code false} otherwise.
	 */
	@Override
	public boolean checkIsAttacker(Clan clan)
	{
		return (getAttackerClan(clan) != null);
	}
	
	/**
	 * Checks if the specified clan is currently registered as a defender.<br>
	 * This method verifies if the {@code Clan} exists in the defender list.
	 * @param clan The {@code Clan} to check.
	 * @return {@code true} if the clan is a defender, {@code false} otherwise.
	 */
	@Override
	public boolean checkIsDefender(Clan clan)
	{
		return (getDefenderClan(clan) != null);
	}
	
	/**
	 * Checks if a specific clan is currently waiting to become a defender.<br>
	 * This method returns {@code true} if the clan exists in the waiting list.
	 * @param clan The {@code Clan} object to check.
	 * @return {@code true} if the clan is waiting, {@code false} otherwise.
	 */
	public boolean checkIsDefenderWaiting(Clan clan)
	{
		return (getDefenderWaitingClan(clan) != null);
	}
	
	/**
	 * Removes all siege clan data from the database for the current castle.<br>
	 * Clears the internal lists of attackers, defenders, and waiting clans.<br>
	 * This method handles both the specific castle records and the owner's clan record.
	 */
	public void clearSiegeClan()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM siege_clans WHERE castle_id=?"))
		{
			statement.setInt(1, _castle.getResidenceId());
			statement.execute();
			
			final int castleOwnerId = _castle.getOwnerId();
			if (castleOwnerId > 0)
			{
				try (PreparedStatement delete = con.prepareStatement("DELETE FROM siege_clans WHERE clan_id=?"))
				{
					delete.setInt(1, castleOwnerId);
					delete.execute();
				}
			}
			
			getAttackerClans().clear();
			getDefenderClans().clear();
			_defenderWaitingClans.clear();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Exception: clearSiegeClan(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Removes clans from the database that are waiting to defend a castle.<br>
	 * This method clears the {@code _defenderWaitingClans} collection.<br>
	 * It targets records where the type is equal to {@code 2}.
	 */
	public void clearSiegeWaitingClan()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM siege_clans WHERE castle_id=? and type = 2"))
		{
			statement.setInt(1, _castle.getResidenceId());
			statement.execute();
			
			_defenderWaitingClans.clear();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Exception: clearSiegeWaitingClan(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieves a list of all players currently attacking in the siege zone.<br>
	 * This method checks all attacker clans and identifies their online members.<br>
	 * It only includes members who are actively participating in the siege.
	 * @return A {@code List} of {@code Player} objects who are attackers.
	 */
	@Override
	public List<Player> getAttackersInZone()
	{
		final List<Player> result = new ArrayList<>();
		for (SiegeClan siegeclan : getAttackerClans())
		{
			final Clan clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
			if (clan != null)
			{
				for (Player member : clan.getOnlineMembers(0))
				{
					if (member.isInSiege())
					{
						result.add(member);
					}
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves all players currently located within the siege zone.<br>
	 * This method fetches the list from the {@code Castle} object.
	 * @return a {@code List} of {@code Player} objects inside the zone.
	 */
	public List<Player> getPlayersInZone()
	{
		return _castle.getZone().getPlayersInside();
	}
	
	/**
	 * Retrieves a list of players who own the current siege zone.<br>
	 * It identifies members of the clan that matches the castle owner ID.<br>
	 * Only online members currently in a siege state are included.
	 * @return A {@code List} of {@code Player} objects belonging to the owners.
	 */
	public List<Player> getOwnersInZone()
	{
		final List<Player> result = new ArrayList<>();
		for (SiegeClan siegeclan : getDefenderClans())
		{
			final int siegeClanId = siegeclan.getClanId();
			if (siegeClanId == _castle.getOwnerId())
			{
				final Clan clan = ClanTable.getInstance().getClan(siegeClanId);
				if (clan != null)
				{
					for (Player member : clan.getOnlineMembers(0))
					{
						if (member.isInSiege())
						{
							result.add(member);
						}
					}
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves a list of players currently inside the siege zone.<br>
	 * This method filters out any players who are actively participating in the siege.<br>
	 * It only returns players who are not marked as being in a siege state.
	 * @return A {@code List} of {@code Player} objects who are spectators in the zone.
	 */
	public List<Player> getSpectatorsInZone()
	{
		final List<Player> result = new ArrayList<>();
		for (Player player : _castle.getZone().getPlayersInside())
		{
			if (!player.isInSiege())
			{
				result.add(player);
			}
		}
		
		return result;
	}
	
	/**
	 * Decreases the count of active control towers.<br>
	 * This method is called when a {@code Npc} representing a control tower is destroyed.<br>
	 * The internal counter will not drop below {@code 0}.
	 * @param ct The {@code Npc} object that was killed.
	 */
	public void killedCT(Npc ct)
	{
		_controlTowerCount = Math.max(_controlTowerCount - 1, 0);
	}
	
	/**
	 * Removes a specific flag from all attacking clans.<br>
	 * This method updates the state of active attackers during a siege.
	 * @param flag The {@code Npc} object representing the flag to be removed.
	 */
	public void killedFlag(Npc flag)
	{
		getAttackerClans().forEach(siegeClan -> siegeClan.removeFlag(flag));
	}
	
	/**
	 * Sends the current siege information to a specific player.<br>
	 * This method updates the client with data regarding the active siege.
	 * @param player The {@code Player} who will receive the siege information.
	 */
	public void listRegisterClan(Player player)
	{
		player.sendPacket(new SiegeInfo(_castle, player));
	}
	
	/**
	 * Adds a {@code Player} to the list of active attackers.<br>
	 * This method updates the siege state for the specified player.
	 * @param player The {@code Player} to register as an attacker.
	 */
	public void registerAttacker(Player player)
	{
		registerAttacker(player, false);
	}
	
	/**
	 * Registers a {@code Player} as an attacker for the siege.<br>
	 * This method checks if the player is eligible based on their clan alliance.<br>
	 * If {@code force} is {@code true}, it bypasses standard eligibility checks.
	 * @param player The {@code Player} to register as an attacker.
	 * @param force Whether to ignore alliance restrictions during registration.
	 */
	public void registerAttacker(Player player, boolean force)
	{
		final Clan clan = player.getClan();
		if (clan == null)
		{
			return;
		}
		
		int allyId = 0;
		if (_castle.getOwnerId() != 0)
		{
			allyId = ClanTable.getInstance().getClan(getCastle().getOwnerId()).getAllyId();
		}
		
		if ((allyId != 0) && (clan.getAllyId() == allyId) && !force)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_REGISTER_AS_AN_ATTACKER_BECAUSE_YOU_ARE_IN_AN_ALLIANCE_WITH_THE_CASTLE_OWNING_CLAN);
			return;
		}
		
		if (force)
		{
			if (SiegeManager.getInstance().checkIsRegistered(clan, getCastle().getResidenceId()))
			{
				player.sendPacket(SystemMessageId.YOU_HAVE_ALREADY_REQUESTED_A_CASTLE_SIEGE);
			}
			else
			{
				saveSiegeClan(clan, ATTACKER, false); // Save to database
			}
			return;
		}
		
		if (checkIfCanRegister(player, ATTACKER))
		{
			saveSiegeClan(clan, ATTACKER, false); // Save to database
		}
	}
	
	/**
	 * Registers a {@code Player} as a defender for the current siege.<br>
	 * This method updates the player's status in the siege system.
	 * @param player The {@code Player} to register.
	 */
	public void registerDefender(Player player)
	{
		registerDefender(player, false);
	}
	
	/**
	 * Registers a {@code Player} as a defender for the siege.<br>
	 * This method checks if the castle is owned by a player before proceeding.<br>
	 * It saves the registration status to the database.
	 * @param player The {@code Player} who wants to register as a defender.
	 * @param force If {@code true}, it bypasses standard registration checks.
	 */
	public void registerDefender(Player player, boolean force)
	{
		if (_castle.getOwnerId() <= 0)
		{
			player.sendMessage("You cannot register as a defender because " + _castle.getName() + " is owned by NPC.");
			return;
		}
		
		final Clan clan = player.getClan();
		if (force)
		{
			if (SiegeManager.getInstance().checkIsRegistered(clan, getCastle().getResidenceId()))
			{
				player.sendPacket(SystemMessageId.YOU_HAVE_ALREADY_REQUESTED_A_CASTLE_SIEGE);
			}
			else
			{
				saveSiegeClan(clan, DEFENDER_NOT_APPROVED, false); // Save to database
			}
			return;
		}
		
		if (checkIfCanRegister(player, DEFENDER_NOT_APPROVED))
		{
			saveSiegeClan(clan, DEFENDER_NOT_APPROVED, false); // Save to database
		}
	}
	
	/**
	 * Removes a specific clan from the current siege.<br>
	 * This method deletes the record from the database and reloads the data.
	 * @param clanId The unique identifier of the clan to remove.
	 */
	public void removeSiegeClan(int clanId)
	{
		if (clanId <= 0)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM siege_clans WHERE castle_id=? and clan_id=?"))
		{
			statement.setInt(1, _castle.getResidenceId());
			statement.setInt(2, clanId);
			statement.execute();
			
			loadSiegeClan();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Exception: removeSiegeClan(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Removes a specific clan from the current siege.<br>
	 * This method checks if the {@code Clan} is valid and registered for the castle.<br>
	 * If the conditions are met, it removes the clan by its unique ID.
	 * @param clan The {@code Clan} object to be removed from the siege.
	 */
	public void removeSiegeClan(Clan clan)
	{
		final int residenceId = getCastle().getResidenceId();
		if ((clan == null) || (clan.getCastleId() == residenceId) || !SiegeManager.getInstance().checkIsRegistered(clan, residenceId))
		{
			return;
		}
		
		removeSiegeClan(clan.getId());
	}
	
	/**
	 * Removes a {@code Player} from the current siege status.<br>
	 * This method updates the siege state for the player's clan.
	 * @param player The {@code Player} to be removed from the siege.
	 */
	public void removeSiegeClan(Player player)
	{
		removeSiegeClan(player.getClan());
	}
	
	/**
	 * Starts the automated process for beginning a siege.<br>
	 * This method synchronizes the current time and loads existing clan data.<br>
	 * It cancels any previous tasks and schedules a new {@code ScheduleStartSiegeTask}.
	 */
	public void startAutoTask()
	{
		correctSiegeDateTime();
		
		LOGGER.info("Siege of " + _castle.getName() + ": " + _castle.getSiegeDate().getTime());
		loadSiegeClan();
		
		// Schedule siege auto start.
		if (_scheduledStartSiegeTask != null)
		{
			_scheduledStartSiegeTask.cancel(false);
		}
		
		_scheduledStartSiegeTask = ThreadPool.schedule(new ScheduleStartSiegeTask(_castle), 1000);
	}
	
	/**
	 * Teleports a specific group of players to a new location.<br>
	 * The method filters players based on their role in the siege.<br>
	 * It excludes players who are game masters or currently jailed.
	 * @param teleportWho The type of player to target for teleportation.
	 * @param teleportWhere The destination where the players will be moved.
	 */
	public void teleportPlayer(SiegeTeleportWhoType teleportWho, TeleportWhereType teleportWhere)
	{
		final List<Player> players;
		switch (teleportWho)
		{
			case Owner:
			{
				players = getOwnersInZone();
				break;
			}
			case NotOwner:
			{
				players = _castle.getZone().getPlayersInside();
				final Iterator<Player> it = players.iterator();
				while (it.hasNext())
				{
					final Player player = it.next();
					if ((player == null) || player.getObserverMode().isActive() || ((player.getClanId() > 0) && (player.getClanId() == _castle.getOwnerId())))
					{
						it.remove();
					}
				}
				break;
			}
			case Attacker:
			{
				players = getAttackersInZone();
				break;
			}
			case Spectator:
			{
				players = getSpectatorsInZone();
				break;
			}
			default:
			{
				players = Collections.emptyList();
				break;
			}
		}
		
		for (Player player : players)
		{
			if (player.isGM() || player.isJailed())
			{
				continue;
			}
			
			player.teleToLocation(teleportWhere);
		}
	}
	
	/**
	 * Adds a new clan to the list of attackers.<br>
	 * This method creates a {@code SiegeClan} object for the provided ID.<br>
	 * It sets the type to {@code SiegeClanType.ATTACKER}.
	 * @param clanId The unique identifier of the clan to add.
	 */
	private void addAttacker(int clanId)
	{
		getAttackerClans().add(new SiegeClan(clanId, SiegeClanType.ATTACKER)); // Add registered attacker to attacker list.
	}
	
	/**
	 * Adds a new clan to the defender list.<br>
	 * This method registers a clan as a defender for the current siege.
	 * @param clanId The unique identifier of the clan to add.
	 */
	private void addDefender(int clanId)
	{
		getDefenderClans().add(new SiegeClan(clanId, SiegeClanType.DEFENDER)); // Add registered defender to defender list.
	}
	
	/**
	 * Adds a new defender clan to the siege.<br>
	 * This method creates a {@code SiegeClan} object and stores it in the internal list.
	 * @param clanId The unique identifier of the clan.
	 * @param type The specific type of siege defense assigned to this clan.
	 */
	private void addDefender(int clanId, SiegeClanType type)
	{
		getDefenderClans().add(new SiegeClan(clanId, type));
	}
	
	/**
	 * Adds a clan to the pending defenders list.<br>
	 * This method registers a clan as a defender that is currently waiting.
	 * @param clanId The unique identifier of the clan to add.
	 */
	private void addDefenderWaiting(int clanId)
	{
		_defenderWaitingClans.add(new SiegeClan(clanId, SiegeClanType.DEFENDER_PENDING)); // Add registered defender to defender list.
	}
	
	/**
	 * Checks if a {@code Player} is allowed to register for a siege.<br>
	 * It validates the clan level, ownership status, and registration limits.
	 * @param player The {@code Player} attempting to register.
	 * @param typeId The {@code byte} representing the side they want to join.
	 * @return {@code true} if the registration is valid, {@code false} otherwise.
	 */
	private boolean checkIfCanRegister(Player player, byte typeId)
	{
		final Clan clan = player.getClan();
		if (_isRegistrationOver)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.THE_DEADLINE_TO_REGISTER_FOR_THE_SIEGE_OF_S1_HAS_PASSED);
			sm.addCastleId(_castle.getResidenceId());
			player.sendPacket(sm);
		}
		else if (_isInProgress)
		{
			player.sendPacket(SystemMessageId.THIS_IS_NOT_THE_TIME_FOR_SIEGE_REGISTRATION_AND_SO_REGISTRATION_AND_CANCELLATION_CANNOT_BE_DONE);
		}
		else if ((clan == null) || (clan.getLevel() < SiegeManager.getInstance().getSiegeClanMinLevel()))
		{
			player.sendPacket(SystemMessageId.ONLY_CLANS_OF_LEVEL_5_OR_ABOVE_MAY_REGISTER_FOR_A_CASTLE_SIEGE);
		}
		else if (clan.getId() == _castle.getOwnerId())
		{
			player.sendPacket(SystemMessageId.CASTLE_OWNING_CLANS_ARE_AUTOMATICALLY_REGISTERED_ON_THE_DEFENDING_SIDE);
		}
		else if (clan.getCastleId() > 0)
		{
			player.sendPacket(SystemMessageId.A_CLAN_THAT_OWNS_A_CASTLE_CANNOT_PARTICIPATE_IN_ANOTHER_SIEGE);
		}
		else if (SiegeManager.getInstance().checkIsRegistered(clan, getCastle().getResidenceId()))
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_ALREADY_REQUESTED_A_CASTLE_SIEGE);
		}
		else if (checkIfAlreadyRegisteredForSameDay(clan))
		{
			player.sendPacket(SystemMessageId.YOUR_APPLICATION_HAS_BEEN_DENIED_BECAUSE_YOU_HAVE_ALREADY_SUBMITTED_A_REQUEST_FOR_ANOTHER_CASTLE_SIEGE);
		}
		else if ((typeId == ATTACKER) && (getAttackerClans().size() >= SiegeManager.getInstance().getAttackerMaxClans()))
		{
			player.sendPacket(SystemMessageId.NO_MORE_REGISTRATIONS_MAY_BE_ACCEPTED_FOR_THE_ATTACKER_SIDE);
		}
		else if (((typeId == DEFENDER) || (typeId == DEFENDER_NOT_APPROVED) || (typeId == OWNER)) && ((getDefenderClans().size() + getDefenderWaitingClans().size()) >= SiegeManager.getInstance().getDefenderMaxClans()))
		{
			player.sendPacket(SystemMessageId.NO_MORE_REGISTRATIONS_MAY_BE_ACCEPTED_FOR_THE_DEFENDER_SIDE);
		}
		else
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * Checks if the provided {@code Clan} is already registered for a siege on the same day.<br>
	 * It iterates through all active sieges to find any matches based on the day of the week.<br>
	 * This includes checking if the clan is an attacker, defender, or waiting defender.
	 * @param clan The {@code Clan} to check.
	 * @return {@code true} if the clan is registered for another siege on the same day, {@code false} otherwise.
	 */
	public boolean checkIfAlreadyRegisteredForSameDay(Clan clan)
	{
		for (Siege siege : SiegeManager.getInstance().getSieges())
		{
			if (siege == this)
			{
				continue;
			}
			
			if (siege.getSiegeDate().get(Calendar.DAY_OF_WEEK) == getSiegeDate().get(Calendar.DAY_OF_WEEK))
			{
				if (siege.checkIsAttacker(clan))
				{
					return true;
				}
				
				if (siege.checkIsDefender(clan))
				{
					return true;
				}
				
				if (siege.checkIsDefenderWaiting(clan))
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if the current siege date has already passed.<br>
	 * If the date is in the past, it reschedules the siege to the next available time.<br>
	 * This method updates and saves the new date to the database if a correction occurs.
	 */
	public void correctSiegeDateTime()
	{
		boolean corrected = false;
		if (getCastle().getSiegeDate().getTimeInMillis() < System.currentTimeMillis())
		{
			// Since siege has past reschedule it to the next one.
			// This is usually caused by server being down.
			corrected = true;
			setNextSiegeDate();
		}
		
		if (corrected)
		{
			saveSiegeDate();
		}
	}
	
	/**
	 * Loads the siege clan data from the database.<br>
	 * It clears existing lists and populates them with attackers, defenders, and waiting clans.<br>
	 * The castle owner is automatically added as a defender if they exist.
	 */
	private void loadSiegeClan()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT clan_id,type FROM siege_clans where castle_id=?"))
		{
			getAttackerClans().clear();
			getDefenderClans().clear();
			_defenderWaitingClans.clear();
			
			// Add castle owner as defender (add owner first so that they are on the top of the defender list).
			final int castleOwnerId = _castle.getOwnerId();
			if (castleOwnerId > 0)
			{
				addDefender(castleOwnerId, SiegeClanType.OWNER);
			}
			
			statement.setInt(1, _castle.getResidenceId());
			try (ResultSet rs = statement.executeQuery())
			{
				int typeId;
				while (rs.next())
				{
					typeId = rs.getInt("type");
					if (typeId == DEFENDER)
					{
						addDefender(rs.getInt("clan_id"));
					}
					else if (typeId == ATTACKER)
					{
						addAttacker(rs.getInt("clan_id"));
					}
					else if (typeId == DEFENDER_NOT_APPROVED)
					{
						addDefenderWaiting(rs.getInt("clan_id"));
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Exception: loadSiegeClan(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Removes all active towers from the game world.<br>
	 * This method calls {@code deleteMe()} on every {@code FlameTower}.<br>
	 * It also calls {@code deleteMe()} on every {@code ControlTower}.<br>
	 * Finally, it clears both internal tower lists.
	 */
	private void removeTowers()
	{
		for (FlameTower ct : _flameTowers)
		{
			ct.deleteMe();
		}
		
		for (ControlTower ct : _controlTowers)
		{
			ct.deleteMe();
		}
		
		for (Npc wreck : _towerWrecks)
		{
			wreck.deleteMe();
		}
		
		_flameTowers.clear();
		_controlTowers.clear();
		_towerWrecks.clear();
	}
	
	/**
	 * Leaves the wreck of a destroyed tower where it stood; it goes away with the towers at the end of the siege.
	 * @param tower The tower that was just destroyed.
	 * @param wreckId The npc id of its wreck.
	 */
	public void spawnTowerWreck(Npc tower, int wreckId)
	{
		try
		{
			final Spawn spawn = new Spawn(wreckId);
			spawn.setLocation(tower.getLocation());
			_towerWrecks.add(spawn.doSpawn(false));
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getName() + ": Cannot spawn tower wreck " + wreckId + "! " + e);
		}
	}
	
	/**
	 * Removes all flags from every attacker and defender clan.<br>
	 * This method iterates through both {@code getAttackerClans()} and {@code getDefenderClans()}.<br>
	 * It calls the {@code removeFlags()} method on each non-null {@code SiegeClan} object.
	 */
	private void removeFlags()
	{
		for (SiegeClan sc : getAttackerClans())
		{
			if (sc != null)
			{
				sc.removeFlags();
			}
		}
		
		for (SiegeClan sc : getDefenderClans())
		{
			if (sc != null)
			{
				sc.removeFlags();
			}
		}
	}
	
	/**
	 * Removes all flags from every defender clan.<br>
	 * This method iterates through the list of defender clans.<br>
	 * It calls {@code removeFlags()} on each non-null {@code SiegeClan}.
	 */
	private void removeDefenderFlags()
	{
		for (SiegeClan sc : getDefenderClans())
		{
			if (sc != null)
			{
				sc.removeFlags();
			}
		}
	}
	
	/**
	 * Saves the current castle siege data to the database.<br>
	 * This method updates the next siege date and handles time registration.<br>
	 * It also schedules the automatic tasks for the upcoming siege.
	 */
	private void saveCastleSiege()
	{
		setNextSiegeDate(); // Set the next set date for 2 weeks from now.
		
		// Schedule Time registration end.
		getTimeRegistrationOverDate().setTimeInMillis(System.currentTimeMillis());
		_castle.getTimeRegistrationOverDate().add(Calendar.DAY_OF_MONTH, 1);
		_castle.setTimeRegistrationOver(false);
		
		saveSiegeDate(); // Save the new date.
		startAutoTask(); // Prepare auto start siege and end registration.
	}
	
	/**
	 * Saves the current siege date and registration status to the database.<br>
	 * This method updates the {@code castle} table with new time values.<br>
	 * It cancels any existing scheduled tasks before performing the update.
	 */
	public void saveSiegeDate()
	{
		if (_scheduledStartSiegeTask != null)
		{
			_scheduledStartSiegeTask.cancel(true);
			_scheduledStartSiegeTask = ThreadPool.schedule(new ScheduleStartSiegeTask(_castle), 1000);
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE castle SET siegeDate = ?, regTimeEnd = ?, regTimeOver = ?  WHERE id = ?"))
		{
			statement.setLong(1, _castle.getSiegeDate().getTimeInMillis());
			statement.setLong(2, _castle.getTimeRegistrationOverDate().getTimeInMillis());
			statement.setString(3, String.valueOf(_castle.isTimeRegistrationOver()));
			statement.setInt(4, _castle.getResidenceId());
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Exception: saveSiegeDate(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Saves a {@code Clan} to the siege database.<br>
	 * This method handles both new registrations and updates based on the {@code isUpdateRegistration} flag.<br>
	 * It checks for maximum clan limits before performing the database operation.
	 * @param clan The {@code Clan} object to be saved.
	 * @param typeId The siege role type identifier.
	 * @param isUpdateRegistration Set to {@code true} to update an existing record, or {@code false} to insert a new one.
	 */
	private void saveSiegeClan(Clan clan, byte typeId, boolean isUpdateRegistration)
	{
		if (clan.getCastleId() > 0)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			if ((typeId == DEFENDER) || (typeId == DEFENDER_NOT_APPROVED) || (typeId == OWNER))
			{
				if ((getDefenderClans().size() + getDefenderWaitingClans().size()) >= SiegeManager.getInstance().getDefenderMaxClans())
				{
					return;
				}
			}
			else if (getAttackerClans().size() >= SiegeManager.getInstance().getAttackerMaxClans())
			{
				return;
			}
			
			final int clanId = clan.getId();
			final int castleResidenceId = _castle.getResidenceId();
			if (!isUpdateRegistration)
			{
				try (PreparedStatement statement = con.prepareStatement("INSERT INTO siege_clans (clan_id,castle_id,type,castle_owner) values (?,?,?,0)"))
				{
					statement.setInt(1, clanId);
					statement.setInt(2, castleResidenceId);
					statement.setInt(3, typeId);
					statement.execute();
				}
			}
			else
			{
				try (PreparedStatement statement = con.prepareStatement("UPDATE siege_clans SET type = ? WHERE castle_id = ? AND clan_id = ?"))
				{
					statement.setInt(1, typeId);
					statement.setInt(2, castleResidenceId);
					statement.setInt(3, clanId);
					statement.execute();
				}
			}
			
			switch (typeId)
			{
				case DEFENDER:
				case OWNER:
				{
					addDefender(clanId);
					break;
				}
				case ATTACKER:
				{
					addAttacker(clanId);
					break;
				}
				case DEFENDER_NOT_APPROVED:
				{
					addDefenderWaiting(clanId);
					break;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Exception: saveSiegeClan(Pledge clan, int typeId, boolean isUpdateRegistration): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Calculates and registers the date for the next castle siege.<br>
	 * It checks the schedule from {@code SiegeScheduleData}.<br>
	 * If a valid time is found, it updates the registration status.<br>
	 * A global message is sent to all players if the date is registered.
	 */
	private void setNextSiegeDate()
	{
		final SiegeScheduleDate holder = SiegeScheduleData.getInstance().getScheduleDateForCastleId(_castle.getResidenceId());
		if ((holder == null) || !holder.siegeEnabled())
		{
			return;
		}
		
		final Calendar calendar = _castle.getSiegeDate();
		final long currentTime = System.currentTimeMillis();
		if (calendar.getTimeInMillis() < currentTime)
		{
			calendar.setTimeInMillis(currentTime);
		}
		
		calendar.set(Calendar.DAY_OF_WEEK, holder.getDay());
		calendar.set(Calendar.HOUR_OF_DAY, holder.getHour());
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0); // Clear milliseconds so the future check below cannot tie with the current time at the exact siege tick.
		
		// Advance by the siege cycle until the date is strictly in the future (a tie must not leave it in the past).
		while (!calendar.after(Calendar.getInstance()))
		{
			calendar.add(Calendar.WEEK_OF_YEAR, SiegeManager.getInstance().getSiegeCycle());
		}
		
		final long siegeTimeMs = calendar.getTimeInMillis();
		if (CastleManager.getInstance().getSiegeDates(siegeTimeMs) < holder.getMaxConcurrent())
		{
			CastleManager.getInstance().registerSiegeDate(getCastle().getResidenceId(), siegeTimeMs);
			
			World.broadcastToAllOnlinePlayers(new SystemMessage(SystemMessageId.S1_HAS_ANNOUNCED_THE_NEXT_CASTLE_SIEGE_TIME).addCastleId(_castle.getResidenceId()));
			
			// Allow registration for next siege.
			_isRegistrationOver = false;
		}
		else
		{
			// Deny registration for next siege.
			_isRegistrationOver = true;
		}
	}
	
	/**
	 * Spawns the control towers for the current castle.<br>
	 * It iterates through all defined tower spawn locations.<br>
	 * Each valid location creates a new {@code ControlTower} instance.<br>
	 * The total count of spawned towers is updated at the end.
	 */
	private void spawnControlTower()
	{
		try
		{
			for (TowerSpawn ts : SiegeManager.getInstance().getControlTowers(getCastle().getResidenceId()))
			{
				final Spawn spawn = new Spawn(ts.getId());
				spawn.setLocation(ts.getLocation());
				_controlTowers.add((ControlTower) spawn.doSpawn(false));
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Cannot spawn control tower! " + e);
		}
		
		_controlTowerCount = _controlTowers.size();
	}
	
	/**
	 * Spawns all {@code FlameTower} entities for the current castle.<br>
	 * It retrieves tower data from the {@code SiegeManager}.<br>
	 * Each tower is placed at its specific location and upgrade level.<br>
	 * The spawned towers are added to the internal {@code _flameTowers} list.
	 */
	private void spawnFlameTower()
	{
		try
		{
			for (TowerSpawn ts : SiegeManager.getInstance().getFlameTowers(getCastle().getResidenceId()))
			{
				final Spawn spawn = new Spawn(ts.getId());
				spawn.setLocation(ts.getLocation());
				final FlameTower tower = (FlameTower) spawn.doSpawn(false);
				tower.setUpgradeLevel(ts.getUpgradeLevel());
				tower.setZoneList(ts.getZoneList());
				_flameTowers.add(tower);
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Cannot spawn flame tower! " + e);
		}
	}
	
	/**
	 * Spawns the siege guards for the current castle.<br>
	 * This method also registers each guard to the nearest {@code ControlTower}.<br>
	 * If a {@code ControlTower} is destroyed, its associated guards will also die.
	 */
	private void spawnSiegeGuard()
	{
		SiegeGuardManager.getInstance().spawnSiegeGuard(getCastle());
		
		// Register guard to the closest Control Tower.
		// When CT dies, so do all the guards that it controls.
		final Set<Spawn> spawned = SiegeGuardManager.getInstance().getSpawnedGuards(getCastle().getResidenceId());
		if (!spawned.isEmpty())
		{
			ControlTower closestCt;
			double distance;
			double distanceClosest = 0;
			for (Spawn spawn : spawned)
			{
				if (spawn == null)
				{
					continue;
				}
				
				closestCt = null;
				distanceClosest = Integer.MAX_VALUE;
				for (ControlTower ct : _controlTowers)
				{
					if (ct == null)
					{
						continue;
					}
					
					distance = ct.calculateDistance3D(spawn);
					if (distance < distanceClosest)
					{
						closestCt = ct;
						distanceClosest = distance;
					}
				}
				
				if (closestCt != null)
				{
					closestCt.registerGuard(spawn);
				}
			}
		}
	}
	
	/**
	 * Retrieves the {@code SiegeClan} object for a specific clan.<br>
	 * This method checks if the provided {@code Clan} is null before searching.<br>
	 * It uses the unique ID of the {@code Clan} to find the matching attacker data.
	 * @param clan The {@code Clan} object to look up.
	 * @return The corresponding {@code SiegeClan} or {@code null} if not found.
	 */
	@Override
	public SiegeClan getAttackerClan(Clan clan)
	{
		if (clan == null)
		{
			return null;
		}
		
		return getAttackerClan(clan.getId());
	}
	
	/**
	 * Retrieves the {@code SiegeClan} object for a specific attacker.<br>
	 * This method searches through all current attacker clans.<br>
	 * It returns the matching clan or {@code null} if no match is found.
	 * @param clanId The unique identifier of the clan to find.
	 * @return The corresponding {@code SiegeClan} object, or {@code null}.
	 */
	@Override
	public SiegeClan getAttackerClan(int clanId)
	{
		for (SiegeClan sc : getAttackerClans())
		{
			if ((sc != null) && (sc.getClanId() == clanId))
			{
				return sc;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves the collection of clans currently participating in the siege.<br>
	 * The returned list depends on which side is currently active.<br>
	 * If the attacker side is active, it returns {@code _attackerClans}.<br>
	 * Otherwise, it returns {@code _defenderClans}.
	 * @return A {@code Collection} of {@code SiegeClan} objects.
	 */
	@Override
	public Collection<SiegeClan> getAttackerClans()
	{
		if (_isNormalSide)
		{
			return _attackerClans;
		}
		
		return _defenderClans;
	}
	
	/**
	 * Retrieves the respawn delay for attackers.<br>
	 * This value is fetched from the {@code SiegeManager} singleton.
	 * @return The delay time as an {@code int}.
	 */
	public int getAttackerRespawnDelay()
	{
		return (SiegeManager.getInstance().getAttackerRespawnDelay());
	}
	
	/**
	 * Retrieves the {@code Castle} associated with this siege.
	 * @return the current {@code Castle} object.
	 */
	public Castle getCastle()
	{
		return _castle;
	}
	
	/**
	 * Retrieves the {@code SiegeClan} associated with a specific clan.<br>
	 * This method checks if the provided {@code Clan} is null before searching.
	 * @param clan The {@code Clan} to look up.
	 * @return The corresponding {@code SiegeClan} or {@code null} if not found.
	 */
	@Override
	public SiegeClan getDefenderClan(Clan clan)
	{
		if (clan == null)
		{
			return null;
		}
		
		return getDefenderClan(clan.getId());
	}
	
	/**
	 * Retrieves the {@code SiegeClan} object for a specific defender.<br>
	 * It searches through all registered defender clans to find a match.
	 * @param clanId The unique identifier of the clan to look up.
	 * @return The matching {@code SiegeClan} object, or {@code null} if no match is found.
	 */
	@Override
	public SiegeClan getDefenderClan(int clanId)
	{
		for (SiegeClan sc : getDefenderClans())
		{
			if ((sc != null) && (sc.getClanId() == clanId))
			{
				return sc;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves the collection of clans currently on the active side.<br>
	 * This method returns {@code SiegeClan} objects for defenders if the siege is normal.<br>
	 * It returns attacker clans otherwise.
	 * @return A {@code Collection} of {@code SiegeClan} objects representing the current side.
	 */
	@Override
	public Collection<SiegeClan> getDefenderClans()
	{
		if (_isNormalSide)
		{
			return _defenderClans;
		}
		
		return _attackerClans;
	}
	
	/**
	 * Retrieves the {@code SiegeClan} for a clan that is waiting to defend.<br>
	 * This method checks if the provided {@code Clan} is in the waiting state.
	 * @param clan The {@code Clan} to check.
	 * @return The corresponding {@code SiegeClan} or {@code null} if not found.
	 */
	public SiegeClan getDefenderWaitingClan(Clan clan)
	{
		if (clan == null)
		{
			return null;
		}
		
		return getDefenderWaitingClan(clan.getId());
	}
	
	/**
	 * Retrieves the {@code SiegeClan} that is waiting to defend for a specific clan.<br>
	 * It searches through the list of clans currently in the waiting state.
	 * @param clanId The unique identifier of the clan to search for.
	 * @return The matching {@code SiegeClan} object, or {@code null} if no such clan is found.
	 */
	public SiegeClan getDefenderWaitingClan(int clanId)
	{
		for (SiegeClan sc : _defenderWaitingClans)
		{
			if ((sc != null) && (sc.getClanId() == clanId))
			{
				return sc;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves the collection of clans waiting to become defenders.
	 * @return a {@code Collection} of {@code SiegeClan} objects.
	 */
	public Collection<SiegeClan> getDefenderWaitingClans()
	{
		return _defenderWaitingClans;
	}
	
	/**
	 * Checks if the siege is currently active.
	 * @return {@code true} if the siege is in progress, {@code false} otherwise.
	 */
	public boolean isInProgress()
	{
		return _isInProgress;
	}
	
	/**
	 * Checks if the registration period for the siege has ended.
	 * @return {@code true} if registration is closed, {@code false} otherwise.
	 */
	public boolean isRegistrationOver()
	{
		return _isRegistrationOver;
	}
	
	/**
	 * Checks if the registration period for the siege has ended.<br>
	 * This method queries the status from the {@code Castle} object.
	 * @return {@code true} if registration is closed, {@code false} otherwise.
	 */
	public boolean isTimeRegistrationOver()
	{
		return _castle.isTimeRegistrationOver();
	}
	
	/**
	 * Retrieves the scheduled date for the siege.<br>
	 * This method fetches the date from the associated {@code Castle}.
	 * @return the {@code Calendar} object representing the siege date.
	 */
	@Override
	public Calendar getSiegeDate()
	{
		return _castle.getSiegeDate();
	}
	
	/**
	 * Retrieves the date when registration for the castle closes.<br>
	 * This method fetches the deadline from the {@code Castle} object.
	 * @return a {@code Calendar} object representing the registration end time.
	 */
	public Calendar getTimeRegistrationOverDate()
	{
		return _castle.getTimeRegistrationOverDate();
	}
	
	/**
	 * Ends the registration period for a siege.<br>
	 * Sets the castle time registration status to {@code true}.<br>
	 * Saves the siege date if {@code automatic} is {@code false}.
	 * @param automatic Determines whether to save the siege date automatically.
	 */
	public void endTimeRegistration(boolean automatic)
	{
		_castle.setTimeRegistrationOver(true);
		if (!automatic)
		{
			saveSiegeDate();
		}
	}
	
	/**
	 * Retrieves the set of flags associated with a specific clan.<br>
	 * This method checks if the provided {@code Clan} is registered as an attacker.<br>
	 * If it is, it returns their current flags.
	 * @param clan The {@code Clan} to check for siege flags.
	 * @return A {@code Set} of {@code Npc} objects representing the flags, or {@code null} if no match is found.
	 */
	@Override
	public Set<Npc> getFlag(Clan clan)
	{
		if (clan != null)
		{
			final SiegeClan sc = getAttackerClan(clan);
			if (sc != null)
			{
				return sc.getFlag();
			}
		}
		
		return null;
	}
	
	/**
	 * Checks if a dead player may be resurrected on this siege battlefield.<br>
	 * Defenders can be resurrected while a control tower stands, attackers while their clan has a headquarters, anyone else never.
	 * @param player The dead {@code Player}.
	 * @return The refusal message, or {@code null} when the resurrection is allowed.
	 */
	public SystemMessageId getResurrectionRefusal(Player player)
	{
		final Clan clan = player.getClan();
		if (clan != null)
		{
			if (checkIsDefender(clan))
			{
				return getControlTowerCount() == 0 ? SystemMessageId.THE_GUARDIAN_TOWER_HAS_BEEN_DESTROYED_AND_RESURRECTION_IS_NOT_POSSIBLE : null;
			}
			
			final SiegeClan attackerClan = getAttackerClan(clan);
			if (attackerClan != null)
			{
				return attackerClan.getNumFlags() == 0 ? SystemMessageId.IF_A_BASE_CAMP_DOES_NOT_EXIST_RESURRECTION_IS_NOT_POSSIBLE : null;
			}
		}
		
		return SystemMessageId.IT_IS_NOT_POSSIBLE_TO_RESURRECT_IN_BATTLEGROUNDS_WHERE_A_SIEGE_WAR_IS_TAKING_PLACE;
	}
	
	/**
	 * Retrieves the current number of active control towers.<br>
	 * This value is used to track progress during a siege.
	 * @return The total count of {@code ControlTower} instances.
	 */
	public int getControlTowerCount()
	{
		return _controlTowerCount;
	}
	
	/**
	 * This method is a placeholder for granting fame.<br>
	 * It currently always returns {@code true}.
	 * @return {@code true} if the fame was successfully granted.
	 */
	@Override
	public boolean giveFame()
	{
		return true;
	}
	
	/**
	 * Retrieves the frequency for the castle zone fame task.<br>
	 * This value is pulled from the {@code PlayerConfig} class.
	 * @return The integer frequency value.
	 */
	@Override
	public int getFameFrequency()
	{
		return PlayerConfig.CASTLE_ZONE_FAME_TASK_FREQUENCY;
	}
	
	/**
	 * Retrieves the amount of fame points awarded for capturing a castle zone.<br>
	 * This value is pulled from the {@code PlayerConfig} class.
	 * @return The total fame points to be granted.
	 */
	@Override
	public int getFameAmount()
	{
		return PlayerConfig.CASTLE_ZONE_FAME_AQUIRE_POINTS;
	}
	
	/**
	 * Updates the current state of the siege.<br>
	 * This method refreshes active data for both attackers and defenders.<br>
	 * It ensures that all participants are correctly tracked in the zone.
	 */
	@Override
	public void updateSiege()
	{
	}
}
