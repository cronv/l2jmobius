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
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.actor.instance.FortCommander;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.zone.type.SiegeZone;
import org.l2jmobius.gameserver.interfaces.IPositionable;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.FortSiegeManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.sieges.OnFortSiegeFinish;
import org.l2jmobius.gameserver.mechanics.events.holders.sieges.OnFortSiegeStart;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Manages the logic and state for a fort siege event.<br>
 * This class handles the progression of the siege, including objective tracking and victory conditions.<br>
 * It interacts with {@code FortSiegeManager} to coordinate active siege instances.
 */
public class FortSiege implements Siegable
{
	protected static final Logger LOGGER = Logger.getLogger(FortSiege.class.getName());
	
	// SQL
	private static final String DELETE_FORT_SIEGECLANS_BY_CLAN_ID = "DELETE FROM fortsiege_clans WHERE fort_id = ? AND clan_id = ?";
	private static final String DELETE_FORT_SIEGECLANS = "DELETE FROM fortsiege_clans WHERE fort_id = ?";
	
	public class ScheduleEndSiegeTask implements Runnable
	{
		@Override
		public void run()
		{
			if (!_isInProgress)
			{
				return;
			}
			
			try
			{
				_siegeEnd = null;
				endSiege();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Exception: ScheduleEndSiegeTask() for Fort: " + _fort.getName() + " " + e.getMessage(), e);
			}
		}
	}
	
	public class ScheduleStartSiegeTask implements Runnable
	{
		private final Fort _fortInst;
		private final int _time;
		
		public ScheduleStartSiegeTask(int time)
		{
			_fortInst = _fort;
			_time = time;
		}
		
		@Override
		public void run()
		{
			if (_isInProgress)
			{
				return;
			}
			
			try
			{
				final SystemMessage sm;
				if (_time == 3600) // 1hr remains
				{
					ThreadPool.schedule(new ScheduleStartSiegeTask(600), 3000000); // Prepare task for 10 minutes left.
				}
				else if (_time == 600) // 10min remains
				{
					_fort.despawnSuspiciousMerchant();
					sm = new SystemMessage(SystemMessageId.THE_FORTRESS_BATTLE_STARTS_IN_S1_MIN);
					sm.addInt(10);
					announceToPlayer(sm);
					ThreadPool.schedule(new ScheduleStartSiegeTask(300), 300000); // Prepare task for 5 minutes left.
				}
				else if (_time == 300) // 5min remains
				{
					sm = new SystemMessage(SystemMessageId.THE_FORTRESS_BATTLE_STARTS_IN_S1_MIN);
					sm.addInt(5);
					announceToPlayer(sm);
					ThreadPool.schedule(new ScheduleStartSiegeTask(60), 240000); // Prepare task for 1 minute left.
				}
				else if (_time == 60) // 1min remains
				{
					sm = new SystemMessage(SystemMessageId.THE_FORTRESS_BATTLE_STARTS_IN_S1_MIN);
					sm.addInt(1);
					announceToPlayer(sm);
					ThreadPool.schedule(new ScheduleStartSiegeTask(30), 30000); // Prepare task for 30 seconds left.
				}
				else if (_time == 30) // 30seconds remains
				{
					sm = new SystemMessage(SystemMessageId.THE_FORTRESS_BATTLE_STARTS_IN_S1_SEC);
					sm.addInt(30);
					announceToPlayer(sm);
					ThreadPool.schedule(new ScheduleStartSiegeTask(10), 20000); // Prepare task for 10 seconds left.
				}
				else if (_time == 10) // 10seconds remains
				{
					sm = new SystemMessage(SystemMessageId.THE_FORTRESS_BATTLE_STARTS_IN_S1_SEC);
					sm.addInt(10);
					announceToPlayer(sm);
					ThreadPool.schedule(new ScheduleStartSiegeTask(5), 5000); // Prepare task for 5 seconds left.
				}
				else if (_time == 5) // 5seconds remains
				{
					sm = new SystemMessage(SystemMessageId.THE_FORTRESS_BATTLE_STARTS_IN_S1_SEC);
					sm.addInt(5);
					announceToPlayer(sm);
					ThreadPool.schedule(new ScheduleStartSiegeTask(1), 4000); // Prepare task for 1 seconds left.
				}
				else if (_time == 1) // 1seconds remains
				{
					sm = new SystemMessage(SystemMessageId.THE_FORTRESS_BATTLE_STARTS_IN_S1_SEC);
					sm.addInt(1);
					announceToPlayer(sm);
					ThreadPool.schedule(new ScheduleStartSiegeTask(0), 1000); // Prepare task start siege.
				}
				else if (_time == 0)// start siege
				{
					_fortInst.getSiege().startSiege();
				}
				else
				{
					LOGGER.warning(getClass().getSimpleName() + ": Exception: ScheduleStartSiegeTask(): unknown siege time: " + _time);
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Exception: ScheduleStartSiegeTask() for Fort: " + _fortInst.getName() + " " + e.getMessage(), e);
			}
		}
	}
	
	public class ScheduleSuspiciousMerchantSpawn implements Runnable
	{
		@Override
		public void run()
		{
			if (_isInProgress)
			{
				return;
			}
			
			try
			{
				_fort.spawnSuspiciousMerchant();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Exception: ScheduleSuspicoiusMerchantSpawn() for Fort: " + _fort.getName() + " " + e.getMessage(), e);
			}
		}
	}
	
	public class ScheduleSiegeRestore implements Runnable
	{
		@Override
		public void run()
		{
			if (!_isInProgress)
			{
				return;
			}
			
			try
			{
				_siegeRestore = null;
				resetSiege();
				announceToPlayer(new SystemMessage(SystemMessageId.THE_BARRACKS_FUNCTION_HAS_BEEN_RESTORED));
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Exception: ScheduleSiegeRestore() for Fort: " + _fort.getName() + " " + e.getMessage(), e);
			}
		}
	}
	
	private final Set<SiegeClan> _attackerClans = ConcurrentHashMap.newKeySet();
	
	// Fort setting
	protected Set<Spawn> _commanders = ConcurrentHashMap.newKeySet();
	protected final Fort _fort;
	boolean _isInProgress = false;
	private final Collection<Spawn> _siegeGuards = new ArrayList<>();
	ScheduledFuture<?> _siegeEnd = null;
	ScheduledFuture<?> _siegeRestore = null;
	ScheduledFuture<?> _siegeStartTask = null;
	
	/**
	 * Creates a new {@code FortSiege} instance for a specific fort.<br>
	 * This constructor initializes the siege data and registers it with the manager.<br>
	 * It also triggers an initial check for any automated tasks.
	 * @param fort The {@code Fort} object associated with this siege.
	 */
	public FortSiege(Fort fort)
	{
		_fort = fort;
		checkAutoTask();
		FortSiegeManager.getInstance().addSiege(this);
	}
	
	/**
	 * Ends the current siege for a fort.<br>
	 * This method resets all siege flags and clears active tasks.<br>
	 * It also restores the fort to its default state and notifies listeners.
	 */
	@Override
	public void endSiege()
	{
		if (_isInProgress)
		{
			_isInProgress = false; // Flag so that siege instance can be started.
			removeFlags(); // Removes all flags. Note: Remove flag before teleporting players.
			unSpawnFlags();
			
			updatePlayerSiegeStateFlags(true);
			
			int ownerId = -1;
			final Clan ownerClan = _fort.getOwnerClan();
			if (ownerClan != null)
			{
				ownerId = ownerClan.getId();
			}
			
			final SiegeZone fortZone = _fort.getZone();
			fortZone.banishForeigners(ownerId);
			fortZone.setActive(false);
			fortZone.updateZoneStatusForCharactersInside();
			fortZone.setSiegeInstance(null);
			
			saveFortSiege(); // Save fort specific data.
			clearSiegeClan(); // Clear siege clan from db.
			removeCommanders(); // Remove commander from this fort.
			_fort.spawnNpcCommanders(); // Spawn NPC commanders
			unspawnSiegeGuard(); // Remove all spawned siege guard from this fort.
			_fort.resetDoors(); // Respawn door to fort
			ThreadPool.schedule(new ScheduleSuspiciousMerchantSpawn(), FortSiegeManager.getInstance().getSuspiciousMerchantRespawnDelay() * 60 * 1000); // Prepare 3hr task for suspicious merchant respawn.
			setSiegeDateTime(true); // Store suspicious merchant spawn in DB.
			if (_siegeEnd != null)
			{
				_siegeEnd.cancel(true);
				_siegeEnd = null;
			}
			
			if (_siegeRestore != null)
			{
				_siegeRestore.cancel(true);
				_siegeRestore = null;
			}
			
			if ((_fort.getOwnerClan() != null) && (_fort.getFlagPole().getMeshIndex() == 0))
			{
				_fort.setVisibleFlag(true);
			}
			
			LOGGER.info(getClass().getSimpleName() + ": Siege of " + _fort.getName() + " fort finished.");
			
			// Notify to scripts.
			final Fort fort = getFort();
			if (EventDispatcher.getInstance().hasListener(EventType.ON_FORT_SIEGE_FINISH, fort))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnFortSiegeFinish(this), fort);
			}
		}
	}
	
	/**
	 * Starts the siege process for a specific fort.<br>
	 * This method initializes all necessary game objects and states.<br>
	 * It handles NPC spawning, player teleportation, and zone activation.<br>
	 * A scheduled task is created to automatically end the siege after the set duration.
	 */
	@Override
	public void startSiege()
	{
		if (!_isInProgress)
		{
			if (_siegeStartTask != null) // used admin command "admin_startfortsiege"
			{
				_siegeStartTask.cancel(true);
				_fort.despawnSuspiciousMerchant();
			}
			
			_siegeStartTask = null;
			if (_attackerClans.isEmpty())
			{
				return;
			}
			
			_isInProgress = true; // Flag so that same siege instance cannot be started again.
			loadSiegeClan(); // Load siege clan from db.
			updatePlayerSiegeStateFlags(false);
			teleportPlayer(FortTeleportWhoType.Attacker, TeleportWhereType.TOWN); // Teleport to the closest town.
			_fort.despawnNpcCommanders(); // Despawn NPC commanders
			spawnCommanders(); // Spawn commanders
			_fort.resetDoors(); // Spawn door
			spawnSiegeGuard(); // Spawn siege guard
			_fort.setVisibleFlag(false);
			final SiegeZone fortZone = _fort.getZone();
			fortZone.setSiegeInstance(this);
			fortZone.setActive(true);
			fortZone.updateZoneStatusForCharactersInside();
			
			// Schedule a task to prepare auto siege end.
			_siegeEnd = ThreadPool.schedule(new ScheduleEndSiegeTask(), FortSiegeManager.getInstance().getSiegeLength() * 60 * 1000); // Prepare auto end task.
			
			announceToPlayer(new SystemMessage(SystemMessageId.THE_FORTRESS_BATTLE_HAS_BEGUN));
			saveFortSiege();
			
			LOGGER.info(getClass().getSimpleName() + ": Siege of " + _fort.getName() + " fort started.");
			
			// Notify to scripts.
			final Fort fort = getFort();
			if (EventDispatcher.getInstance().hasListener(EventType.ON_FORT_SIEGE_START, fort))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnFortSiegeStart(this), fort);
			}
		}
	}
	
	/**
	 * Sends a system message to all participants in the siege.<br>
	 * This includes members of attacking clans and the owner clan.<br>
	 * The method ensures that only online players receive the {@code SystemMessage}.
	 * @param sm The {@code SystemMessage} to be sent to the players.
	 */
	public void announceToPlayer(SystemMessage sm)
	{
		// Announce messages only for participants.
		Clan clan;
		for (SiegeClan siegeclan : _attackerClans)
		{
			clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
			for (Player member : clan.getOnlineMembers(0))
			{
				if (member != null)
				{
					member.sendPacket(sm);
				}
			}
		}
		
		if (_fort.getOwnerClan() != null)
		{
			clan = ClanTable.getInstance().getClan(getFort().getOwnerClan().getId());
			for (Player member : clan.getOnlineMembers(0))
			{
				if (member != null)
				{
					member.sendPacket(sm);
				}
			}
		}
	}
	
	/**
	 * Sends a system message to the player with an additional string appended.<br>
	 * This method modifies the {@code SystemMessage} object before sending it.
	 * @param sm The {@code SystemMessage} to be sent to the player.
	 * @param s The extra text to append to the message.
	 */
	public void announceToPlayer(SystemMessage sm, String s)
	{
		sm.addString(s);
		announceToPlayer(sm);
	}
	
	/**
	 * Updates the siege status and fame tasks for all online members of attacking and owning clans.<br>
	 * If {@code clear} is {@code true}, it resets all siege-related flags for these players.<br>
	 * If {@code clear} is {@code false}, it sets the correct siege side and starts fame tasks if they are in the zone.
	 * @param clear Determines whether to reset the siege state or apply new siege values.
	 */
	public void updatePlayerSiegeStateFlags(boolean clear)
	{
		Clan clan;
		for (SiegeClan siegeclan : _attackerClans)
		{
			clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
			for (Player member : clan.getOnlineMembers(0))
			{
				if (member == null)
				{
					continue;
				}
				
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
					member.setSiegeSide(_fort.getResidenceId());
					if (checkIfInZone(member))
					{
						member.setInSiege(true);
						member.startFameTask(PlayerConfig.FORTRESS_ZONE_FAME_TASK_FREQUENCY * 1000, PlayerConfig.FORTRESS_ZONE_FAME_AQUIRE_POINTS);
					}
				}
				
				member.broadcastUserInfo();
			}
		}
		
		if (_fort.getOwnerClan() != null)
		{
			clan = ClanTable.getInstance().getClan(_fort.getOwnerClan().getId());
			for (Player member : clan.getOnlineMembers(0))
			{
				if (member == null)
				{
					continue;
				}
				
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
					member.setSiegeSide(_fort.getResidenceId());
					if (checkIfInZone(member))
					{
						member.setInSiege(true);
						member.startFameTask(PlayerConfig.FORTRESS_ZONE_FAME_TASK_FREQUENCY * 1000, PlayerConfig.FORTRESS_ZONE_FAME_AQUIRE_POINTS);
					}
				}
				
				member.broadcastUserInfo();
			}
		}
	}
	
	/**
	 * Checks if a specific {@code WorldObject} is located within the siege zone.<br>
	 * This method retrieves the coordinates from the provided object and validates them.
	 * @param object The {@code WorldObject} to check.
	 * @return {@code true} if the object is inside the zone, {@code false} otherwise.
	 */
	public boolean checkIfInZone(WorldObject object)
	{
		return checkIfInZone(object.getX(), object.getY(), object.getZ());
	}
	
	/**
	 * Checks if the specified coordinates are within the active siege zone.<br>
	 * This method verifies both the current siege status and the spatial boundaries.
	 * @param x The x coordinate to check.
	 * @param y The y coordinate to check.
	 * @param z The z coordinate to check.
	 * @return {@code true} if the coordinates are inside the zone during a siege, {@code false} otherwise.
	 */
	public boolean checkIfInZone(int x, int y, int z)
	{
		return (_isInProgress && (_fort.checkIfInZone(x, y, z))); // Fort zone during siege
	}
	
	/**
	 * Checks if the specified clan is currently attacking.<br>
	 * This method returns {@code true} if the clan is registered as an attacker.<br>
	 * It returns {@code false} if the clan is not found in the attacker list.
	 * @param clan The {@code Clan} object to check.
	 * @return {@code true} if the clan is an attacker, otherwise {@code false}.
	 */
	@Override
	public boolean checkIsAttacker(Clan clan)
	{
		return (getAttackerClan(clan) != null);
	}
	
	/**
	 * Checks if the provided {@code Clan} is the current owner of the fort.<br>
	 * This method returns {@code true} if the clan matches the fort's owner.<br>
	 * It returns {@code false} if the clan is {@code null} or does not match.
	 * @param clan The {@code Clan} to check.
	 * @return {@code true} if the clan is the defender, {@code false} otherwise.
	 */
	@Override
	public boolean checkIsDefender(Clan clan)
	{
		return (clan != null) && (_fort.getOwnerClan() == clan);
	}
	
	/**
	 * Clears all clan data associated with the current fort siege.<br>
	 * This method removes records from the database for both the fort and its owner.<br>
	 * It also clears the internal list of attacker clans.<br>
	 * If a siege is active, it calls {@code endSiege()} to stop it.<br>
	 * Any pending tasks to start a siege are cancelled.
	 */
	public void clearSiegeClan()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM fortsiege_clans WHERE fort_id=?"))
		{
			ps.setInt(1, _fort.getResidenceId());
			ps.execute();
			
			final Clan clearSiegeOwnerClan = _fort.getOwnerClan();
			if (clearSiegeOwnerClan != null)
			{
				try (PreparedStatement delete = con.prepareStatement("DELETE FROM fortsiege_clans WHERE clan_id=?"))
				{
					delete.setInt(1, clearSiegeOwnerClan.getId());
					delete.execute();
				}
			}
			
			_attackerClans.clear();
			
			// if siege is in progress, end siege
			if (_isInProgress)
			{
				endSiege();
			}
			
			// if siege isn't in progress (1hr waiting time till siege starts), cancel waiting time
			if (_siegeStartTask != null)
			{
				_siegeStartTask.cancel(true);
				_siegeStartTask = null;
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Exception: clearSiegeClan(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Resets the siege date to its default value.<br>
	 * This method sets the time in milliseconds for the {@code _fort} siege date to {@code 0}.
	 */
	private void clearSiegeDate()
	{
		_fort.getSiegeDate().setTimeInMillis(0);
	}
	
	/**
	 * Retrieves a list of all players who are currently attacking the siege.<br>
	 * This method filters online members from attacker clans who are also in a siege state.
	 * @return A {@code List} of {@code Player} objects who are participating as attackers.
	 */
	@Override
	public List<Player> getAttackersInZone()
	{
		final List<Player> players = new ArrayList<>();
		for (SiegeClan siegeclan : _attackerClans)
		{
			final Clan clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
			for (Player player : clan.getOnlineMembers(0))
			{
				if (player == null)
				{
					continue;
				}
				
				if (player.isInSiege())
				{
					players.add(player);
				}
			}
		}
		
		return players;
	}
	
	/**
	 * Retrieves all players currently located within the siege zone.<br>
	 * This method fetches the list from the associated {@code SiegeZone}.
	 * @return A {@code List} of {@code Player} objects found in the zone.
	 */
	public List<Player> getPlayersInZone()
	{
		return _fort.getZone().getPlayersInside();
	}
	
	/**
	 * Retrieves a list of online members belonging to the fort owner clan.<br>
	 * It checks if the members are currently participating in a siege.<br>
	 * Returns {@code null} if the owner clan is not correctly loaded.
	 * @return A {@code List} of {@code Player} objects who own the fort and are in a siege.
	 */
	public List<Player> getOwnersInZone()
	{
		final List<Player> players = new ArrayList<>();
		if (_fort.getOwnerClan() != null)
		{
			final Clan clan = ClanTable.getInstance().getClan(getFort().getOwnerClan().getId());
			if (clan != _fort.getOwnerClan())
			{
				return null;
			}
			
			for (Player player : clan.getOnlineMembers(0))
			{
				if (player == null)
				{
					continue;
				}
				
				if (player.isInSiege())
				{
					players.add(player);
				}
			}
		}
		
		return players;
	}
	
	/**
	 * Handles the logic when a {@code FortCommander} is defeated.<br>
	 * It broadcasts specific NPC messages based on the spawn location.<br>
	 * If no commanders remain, it spawns fort flags and opens the doors.<br>
	 * Otherwise, it announces that the barracks have been seized.
	 * @param instance The {@code FortCommander} instance that was killed.
	 */
	public void killedCommander(FortCommander instance)
	{
		if ((_fort != null) && (!_commanders.isEmpty()))
		{
			final Spawn spawn = instance.getSpawn();
			if (spawn != null)
			{
				for (FortSpawnHolder spawn2 : FortSiegeManager.getInstance().getCommanderSpawnList(getFort().getResidenceId()))
				{
					if (spawn2.getId() == spawn.getId())
					{
						NpcStringId npcString = null;
						switch (spawn2.getMessageId())
						{
							case 1:
							{
								npcString = NpcStringId.YOU_MAY_HAVE_BROKEN_OUR_ARROWS_BUT_YOU_WILL_NEVER_BREAK_OUR_WILL_ARCHERS_RETREAT;
								break;
							}
							case 2:
							{
								npcString = NpcStringId.AIIEEEE_COMMAND_CENTER_THIS_IS_GUARD_UNIT_WE_NEED_BACKUP_RIGHT_AWAY;
								break;
							}
							case 3:
							{
								npcString = NpcStringId.AT_LAST_THE_MAGIC_CIRCLE_THAT_PROTECTS_THE_FORTRESS_HAS_WEAKENED_VOLUNTEERS_STAND_BACK;
								break;
							}
							case 4:
							{
								npcString = NpcStringId.I_FEEL_SO_MUCH_GRIEF_THAT_I_CAN_T_EVEN_TAKE_CARE_OF_MYSELF_THERE_ISN_T_ANY_REASON_FOR_ME_TO_STAY_HERE_ANY_LONGER;
								break;
							}
						}
						
						if (npcString != null)
						{
							instance.broadcastSay(ChatType.NPC_SHOUT, npcString);
						}
					}
				}
				
				_commanders.remove(spawn);
				if (_commanders.isEmpty())
				{
					// Spawn fort flags.
					spawnFlag(_fort.getResidenceId());
					
					// Cancel door/commanders respawn.
					if (_siegeRestore != null)
					{
						_siegeRestore.cancel(true);
					}
					
					// Open doors in main building.
					for (Door door : _fort.getDoors())
					{
						if (door.isShowHp())
						{
							continue;
						}
						
						// TODO this also opens control room door at big fort
						door.openMe();
					}
					
					_fort.getSiege().announceToPlayer(new SystemMessage(SystemMessageId.ALL_BARRACKS_ARE_OCCUPIED));
				}
				// Schedule restoring doors/commanders respawn.
				else if (_siegeRestore == null)
				{
					_fort.getSiege().announceToPlayer(new SystemMessage(SystemMessageId.THE_BARRACKS_HAVE_BEEN_SEIZED));
					_siegeRestore = ThreadPool.schedule(new ScheduleSiegeRestore(), FortSiegeManager.getInstance().getCountDownLength() * 60 * 1000);
				}
				else
				{
					_fort.getSiege().announceToPlayer(new SystemMessage(SystemMessageId.THE_BARRACKS_HAVE_BEEN_SEIZED));
				}
			}
			else
			{
				LOGGER.warning(getClass().getSimpleName() + ": FortSiege.killedCommander(): killed commander, but commander not registered for fortress. NpcId: " + instance.getId() + " FortId: " + _fort.getResidenceId());
			}
		}
	}
	
	/**
	 * Removes a specific flag from the attacker clans.<br>
	 * This method checks all clans in the {@code _attackerClans} list.<br>
	 * It stops processing as soon as the flag is successfully removed.
	 * @param flag The {@code Npc} object representing the flag to be removed.
	 */
	public void killedFlag(Npc flag)
	{
		if (flag == null)
		{
			return;
		}
		
		for (SiegeClan clan : _attackerClans)
		{
			if (clan.removeFlag(flag))
			{
				return;
			}
		}
	}
	
	/**
	 * Registers a {@code Player} as an attacker for the current siege.<br>
	 * This method checks if the player belongs to a clan and validates registration requirements.<br>
	 * It handles adena costs and starts auto tasks when necessary.
	 * @param player The {@code Player} attempting to join the siege.
	 * @param checkConditions Whether to perform full validation checks like adena balance and existing registrations.
	 * @return An integer status code: 0 for no clan, 1 for insufficient adena, 3 for already registered, or 4 for success.
	 */
	public int addAttacker(Player player, boolean checkConditions)
	{
		final Clan clan = player.getClan();
		if (clan == null)
		{
			return 0; // Player does not have clan.
		}
		
		if (checkConditions)
		{
			if (_fort.getSiege().getAttackerClans().isEmpty() && (player.getInventory().getAdena() < 250000))
			{
				return 1; // Player don't have enough adena to register.
			}
			
			for (Fort fort : FortManager.getInstance().getForts())
			{
				final FortSiege fortSiege = fort.getSiege();
				if (fortSiege.getAttackerClan(player.getClanId()) != null)
				{
					return 3; // Players clan is already registered to siege.
				}
				
				if ((fort.getOwnerClan() == clan) && (fortSiege.isInProgress() || (fortSiege._siegeStartTask != null)))
				{
					return 3; // Players clan is already registered to siege.
				}
			}
		}
		
		saveSiegeClan(clan);
		if (_attackerClans.size() == 1)
		{
			if (checkConditions)
			{
				player.reduceAdena(ItemProcessType.FEE, 250000, null, true);
			}
			
			startAutoTask(true);
		}
		
		return 4; // Players clan is successfully registered to siege.
	}
	
	/**
	 * Removes a specific clan from the current siege.<br>
	 * This method checks if the {@code Clan} is valid and registered for this fort.<br>
	 * If the conditions are met, it calls {@code removeSiegeClan}.
	 * @param clan The {@code Clan} to be removed from the siege.
	 */
	public void removeAttacker(Clan clan)
	{
		if ((clan == null) || (clan.getFortId() == getFort().getResidenceId()) || !FortSiegeManager.getInstance().checkIsRegistered(clan, getFort().getResidenceId()))
		{
			return;
		}
		
		removeSiegeClan(clan.getId());
	}
	
	/**
	 * Removes a specific clan or all clans from the fort siege data.<br>
	 * This method updates the database and refreshes the internal state.<br>
	 * If no attacker clans remain, it may end the current siege.
	 * @param clanId The unique identifier of the clan to remove. Use 0 to remove all clans.
	 */
	private void removeSiegeClan(int clanId)
	{
		final String query = (clanId != 0) ? DELETE_FORT_SIEGECLANS_BY_CLAN_ID : DELETE_FORT_SIEGECLANS;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(query))
		{
			statement.setInt(1, _fort.getResidenceId());
			if (clanId != 0)
			{
				statement.setInt(2, clanId);
			}
			
			statement.execute();
			
			loadSiegeClan();
			if (_attackerClans.isEmpty())
			{
				if (_isInProgress)
				{
					endSiege();
				}
				else
				{
					saveFortSiege(); // Clear siege time in DB.
				}
				
				if (_siegeStartTask != null)
				{
					_siegeStartTask.cancel(true);
					_siegeStartTask = null;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Exception on removeSiegeClan: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Checks and manages the automated tasks for a siege event.<br>
	 * It determines if a siege should start based on the current time.<br>
	 * If the siege time has passed, it clears existing clans and spawns a merchant.<br>
	 * Otherwise, it schedules the {@code ScheduleStartSiegeTask} or spawns a merchant depending on the remaining delay.
	 */
	public void checkAutoTask()
	{
		if (_siegeStartTask != null)
		{
			return;
		}
		
		final long delay = getFort().getSiegeDate().getTimeInMillis() - System.currentTimeMillis();
		if (delay < 0)
		{
			// siege time in past
			saveFortSiege();
			clearSiegeClan(); // remove all clans
			
			// spawn suspicious merchant immediately
			ThreadPool.execute(new ScheduleSuspiciousMerchantSpawn());
		}
		else
		{
			loadSiegeClan();
			if (_attackerClans.isEmpty())
			{
				// no attackers - waiting for suspicious merchant spawn
				ThreadPool.schedule(new ScheduleSuspiciousMerchantSpawn(), delay);
			}
			else
			{
				// preparing start siege task
				if (delay > 3600000) // more than hour, how this can happens ? spawn suspicious merchant
				{
					ThreadPool.execute(new ScheduleSuspiciousMerchantSpawn());
					_siegeStartTask = ThreadPool.schedule(new ScheduleStartSiegeTask(3600), delay - 3600000);
				}
				
				if (delay > 600000) // more than 10 min, spawn suspicious merchant
				{
					ThreadPool.execute(new ScheduleSuspiciousMerchantSpawn());
					_siegeStartTask = ThreadPool.schedule(new ScheduleStartSiegeTask(600), delay - 600000);
				}
				else if (delay > 300000)
				{
					_siegeStartTask = ThreadPool.schedule(new ScheduleStartSiegeTask(300), delay - 300000);
				}
				else if (delay > 60000)
				{
					_siegeStartTask = ThreadPool.schedule(new ScheduleStartSiegeTask(60), delay - 60000);
				}
				else
				{
					// lower than 1 min, set to 1 min
					_siegeStartTask = ThreadPool.schedule(new ScheduleStartSiegeTask(60), 0);
				}
				
				LOGGER.info(getClass().getSimpleName() + ": Siege of " + _fort.getName() + " fort: " + _fort.getSiegeDate().getTime());
			}
		}
	}
	
	/**
	 * Starts the automated task to begin a siege.<br>
	 * This method schedules a {@code ScheduleStartSiegeTask} to run after a delay.<br>
	 * It also broadcasts an attack message if the fort has an owner clan.
	 * @param setTime If {@code true}, it resets the siege date and time.
	 */
	public void startAutoTask(boolean setTime)
	{
		if (_siegeStartTask != null)
		{
			return;
		}
		
		if (setTime)
		{
			setSiegeDateTime(false);
		}
		
		final Clan broadcastOwnerClan = _fort.getOwnerClan();
		if (broadcastOwnerClan != null)
		{
			broadcastOwnerClan.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.A_FORTRESS_IS_UNDER_ATTACK));
		}
		
		// Execute siege auto start.
		_siegeStartTask = ThreadPool.schedule(new ScheduleStartSiegeTask(3600), 0);
	}
	
	/**
	 * Teleports a specific group of players to a new location.<br>
	 * The selection depends on the {@code teleportWho} type provided.<br>
	 * Players who are GMs or in jail will not be moved.
	 * @param teleportWho The category of players to teleport, such as owners or attackers.
	 * @param teleportWhere The destination where the selected players will be sent.
	 */
	public void teleportPlayer(FortTeleportWhoType teleportWho, TeleportWhereType teleportWhere)
	{
		List<Player> players;
		switch (teleportWho)
		{
			case Owner:
			{
				players = getOwnersInZone();
				break;
			}
			case Attacker:
			{
				players = getAttackersInZone();
				break;
			}
			default:
			{
				players = _fort.getZone().getPlayersInside();
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
	 * This method registers the clan for the current siege.
	 * @param clanId The unique identifier of the clan to add.
	 */
	private void addAttacker(int clanId)
	{
		_attackerClans.add(new SiegeClan(clanId, SiegeClanType.ATTACKER)); // Add registered attacker to attacker list.
	}
	
	/**
	 * Checks if the provided {@code Clan} is already registered for a siege on the same day.<br>
	 * It iterates through all active sieges in the manager.<br>
	 * If any other siege occurs on the same day of the week and involves this clan as an attacker or defender, it returns {@code true}.
	 * @param clan The {@code Clan} to check for registration status.
	 * @return {@code true} if the clan is already registered for a siege on the same day; {@code false} otherwise.
	 */
	public boolean checkIfAlreadyRegisteredForSameDay(Clan clan)
	{
		for (FortSiege siege : FortSiegeManager.getInstance().getSieges())
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
			}
		}
		
		return false;
	}
	
	/**
	 * Updates the siege date for the fort.<br>
	 * It calculates the new time based on whether the merchant is involved.<br>
	 * If {@code merchant} is {@code true}, it uses a specific delay from {@code FortSiegeManager}.<br>
	 * Otherwise, it adds 60 minutes to the current time.
	 * @param merchant Determines if the suspicious merchant respawn delay should be applied.
	 */
	private void setSiegeDateTime(boolean merchant)
	{
		final Calendar newDate = Calendar.getInstance();
		if (merchant)
		{
			newDate.add(Calendar.MINUTE, FortSiegeManager.getInstance().getSuspiciousMerchantRespawnDelay());
		}
		else
		{
			newDate.add(Calendar.MINUTE, 60);
		}
		
		_fort.setSiegeDate(newDate);
		saveSiegeDate();
	}
	
	/**
	 * Loads the list of attacker clans from the database.<br>
	 * It clears the current {@code _attackerClans} collection before loading.<br>
	 * The method fetches all {@code clan_id} values for the specific fort residence.
	 */
	private void loadSiegeClan()
	{
		_attackerClans.clear();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT clan_id FROM fortsiege_clans WHERE fort_id=?"))
		{
			ps.setInt(1, _fort.getResidenceId());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					addAttacker(rs.getInt("clan_id"));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Exception: loadSiegeClan(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Removes all instances of commanders for this fort.<br>
	 * Stops the respawn process for each {@code Spawn}.<br>
	 * Deletes any existing {@code Npc} from the last spawn point.<br>
	 * Clears the internal {@code _commanders} list.
	 */
	private void removeCommanders()
	{
		// Remove all instance of commanders for this fort.
		for (Spawn spawn : _commanders)
		{
			if (spawn != null)
			{
				spawn.stopRespawn();
				final Npc lastSpawn = spawn.getLastSpawn();
				if (lastSpawn != null)
				{
					lastSpawn.deleteMe();
				}
			}
		}
		
		_commanders.clear();
	}
	
	/**
	 * Iterates through all attacker clans.<br>
	 * Removes flags from each non-null {@code SiegeClan}.
	 */
	private void removeFlags()
	{
		for (SiegeClan sc : _attackerClans)
		{
			if (sc != null)
			{
				sc.removeFlags();
			}
		}
	}
	
	/**
	 * Saves the current siege date to the database.<br>
	 * This method clears the old date before saving a new one.
	 */
	private void saveFortSiege()
	{
		clearSiegeDate(); // clear siege date
		saveSiegeDate(); // Save the new date.
	}
	
	/**
	 * Saves the current siege date to the database.<br>
	 * This method updates the {@code siegeDate} for a specific fort.<br>
	 * It uses the {@code residenceId} to identify the correct record.
	 */
	private void saveSiegeDate()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE fort SET siegeDate = ? WHERE id = ?"))
		{
			ps.setLong(1, _fort.getSiegeDate().getTimeInMillis());
			ps.setInt(2, _fort.getResidenceId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Exception: saveSiegeDate(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Saves a {@code Clan} to the database for a siege.<br>
	 * This method checks if the maximum number of attacker clans is reached before saving.<br>
	 * It inserts the clan data into the {@code fortsiege_clans} table and updates the internal list.
	 * @param clan The {@code Clan} object to be saved.
	 */
	private void saveSiegeClan(Clan clan)
	{
		if (getAttackerClans().size() >= FortSiegeManager.getInstance().getAttackerMaxClans())
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO fortsiege_clans (clan_id,fort_id) values (?,?)"))
		{
			final int clanId = clan.getId();
			statement.setInt(1, clanId);
			statement.setInt(2, _fort.getResidenceId());
			statement.execute();
			
			addAttacker(clanId);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Exception: saveSiegeClan(Pledge clan): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Initializes and spawns the commanders for the current fort.<br>
	 * This method retrieves all commander spawn data from the {@code FortSiegeManager}.<br>
	 * It creates a new {@code Spawn} object for each location found.<br>
	 * Any errors during the spawning process are logged as warnings.
	 */
	private void spawnCommanders()
	{
		// Set commanders array size if one does not exist.
		try
		{
			_commanders.clear();
			for (FortSpawnHolder _sp : FortSiegeManager.getInstance().getCommanderSpawnList(getFort().getResidenceId()))
			{
				final Spawn spawnDat = new Spawn(_sp.getId());
				spawnDat.setAmount(1);
				final IPositionable spLocation = _sp.getLocation();
				spawnDat.setXYZ(spLocation);
				spawnDat.setHeading(spLocation.getHeading());
				spawnDat.setRespawnDelay(60);
				spawnDat.doSpawn(false);
				spawnDat.stopRespawn();
				_commanders.add(spawnDat);
			}
		}
		catch (Exception e)
		{
			// Problem with initializing spawn, go to next one.
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": FortSiege.spawnCommander: Spawn could not be initialized: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Spawns all flags associated with a specific ID.<br>
	 * It iterates through the list of {@code CombatFlag} objects for the given {@code id}.<br>
	 * Each flag is then instructed to spawn into the world.
	 * @param id The unique identifier for the set of flags to spawn.
	 */
	private void spawnFlag(int id)
	{
		for (CombatFlag cf : FortSiegeManager.getInstance().getFlagList(id))
		{
			cf.spawnMe();
		}
	}
	
	/**
	 * Removes the current object from all active flags.<br>
	 * This method checks if any {@code CombatFlag} objects exist for the fort residence.<br>
	 * It iterates through these flags and calls {@code unSpawnMe()} on each one.
	 */
	private void unSpawnFlags()
	{
		final int residenceId = getFort().getResidenceId();
		if (FortSiegeManager.getInstance().getFlagList(residenceId) == null)
		{
			return;
		}
		
		for (CombatFlag cf : FortSiegeManager.getInstance().getFlagList(residenceId))
		{
			cf.unSpawnMe();
		}
	}
	
	/**
	 * Loads the siege guards from the database for the current fort.<br>
	 * It clears existing guards and populates them based on the fort ID.<br>
	 * Any errors during loading are logged as warnings.
	 */
	public void loadSiegeGuard()
	{
		_siegeGuards.clear();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT npcId, x, y, z, heading, respawnDelay FROM fort_siege_guards WHERE fortId = ?"))
		{
			final int fortId = _fort.getResidenceId();
			ps.setInt(1, fortId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final Spawn spawn = new Spawn(rs.getInt("npcId"));
					spawn.setAmount(1);
					spawn.setXYZ(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
					spawn.setHeading(rs.getInt("heading"));
					spawn.setRespawnDelay(rs.getInt("respawnDelay"));
					spawn.setLocationId(0);
					
					_siegeGuards.add(spawn);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error loading siege guard for fort " + _fort.getName() + ": " + e.getMessage(), e);
		}
	}
	
	/**
	 * Spawns the siege guards for the current fort.<br>
	 * It iterates through all registered {@code Spawn} objects in the {@code _siegeGuards} list.<br>
	 * Each guard is spawned and its respawn logic is initialized based on its delay.<br>
	 * Any errors during the spawning process are logged as a warning.
	 */
	private void spawnSiegeGuard()
	{
		try
		{
			for (Spawn spawnDat : _siegeGuards)
			{
				spawnDat.doSpawn(false);
				if (spawnDat.getRespawnDelay() == 0)
				{
					spawnDat.stopRespawn();
				}
				else
				{
					spawnDat.startRespawn();
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error spawning siege guards for fort " + _fort.getName() + ":" + e.getMessage(), e);
		}
	}
	
	/**
	 * Stops the respawn of all active siege guards.<br>
	 * Kills any existing siege guard NPCs that are currently alive.<br>
	 * Logs a warning if an error occurs during this process.
	 */
	private void unspawnSiegeGuard()
	{
		try
		{
			for (Spawn spawnDat : _siegeGuards)
			{
				spawnDat.stopRespawn();
				final Npc lastSpawn = spawnDat.getLastSpawn();
				if (lastSpawn != null)
				{
					lastSpawn.doDie(lastSpawn);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error unspawning siege guards for fort " + _fort.getName() + ":" + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieves the {@code SiegeClan} associated with a specific {@code Clan}.<br>
	 * This method checks if the provided {@code clan} is null before searching.
	 * @param clan The {@code Clan} object to check.
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
	 * Retrieves a {@code SiegeClan} based on the provided unique identifier.<br>
	 * It searches through all registered attacker clans.<br>
	 * Returns {@code null} if no matching clan is found.
	 * @param clanId The unique ID of the clan to find.
	 * @return The matching {@code SiegeClan} object or {@code null}.
	 */
	@Override
	public SiegeClan getAttackerClan(int clanId)
	{
		for (SiegeClan sc : _attackerClans)
		{
			if ((sc != null) && (sc.getClanId() == clanId))
			{
				return sc;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves the collection of clans currently attacking.
	 * @return A {@code Collection} of {@code SiegeClan} objects.
	 */
	@Override
	public Collection<SiegeClan> getAttackerClans()
	{
		return _attackerClans;
	}
	
	/**
	 * Retrieves the current {@code Fort} instance.
	 * @return The {@code Fort} object associated with this siege.
	 */
	public Fort getFort()
	{
		return _fort;
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
	 * Retrieves the scheduled date for the siege.<br>
	 * This method fetches the date from the associated {@code Fort} object.
	 * @return a {@code Calendar} object representing the siege date.
	 */
	@Override
	public Calendar getSiegeDate()
	{
		return _fort.getSiegeDate();
	}
	
	/**
	 * Retrieves the flag associated with a specific clan.<br>
	 * This method checks if the provided {@code Clan} is currently an attacker in a siege.<br>
	 * If it is, it returns the set of flags for that clan.<br>
	 * It returns {@code null} if the clan is not found or is not part of the current siege.
	 * @param clan The {@code Clan} to check for siege flags.
	 * @return A {@code Set} of {@code Npc} objects representing the flags, or {@code null}.
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
	 * Resets the siege components to their default state.<br>
	 * This method removes existing commanders and spawns new ones.<br>
	 * It also repairs all doors in the {@code Fort}.
	 */
	public void resetSiege()
	{
		// Reload commanders and repair doors.
		removeCommanders();
		spawnCommanders();
		_fort.resetDoors();
	}
	
	/**
	 * Retrieves the current set of commanders.
	 * @return A {@code Set} containing all {@code Spawn} objects for the commanders.
	 */
	public Set<Spawn> getCommanders()
	{
		return _commanders;
	}
	
	/**
	 * Retrieves the {@code SiegeClan} for a specific clan. <br>
	 * This method currently returns {@code null}.
	 * @param clanId The unique identifier of the clan.
	 * @return The {@code SiegeClan} object or {@code null}.
	 */
	@Override
	public SiegeClan getDefenderClan(int clanId)
	{
		return null;
	}
	
	/**
	 * Retrieves the {@code SiegeClan} associated with a specific {@code Clan}.<br>
	 * This method currently returns {@code null}.
	 * @param clan The {@code Clan} to check.
	 * @return The {@code SiegeClan} object or {@code null}.
	 */
	@Override
	public SiegeClan getDefenderClan(Clan clan)
	{
		return null;
	}
	
	/**
	 * Retrieves the list of clans currently defending the siege.
	 * @return A {@code List} of {@code SiegeClan} objects.
	 */
	@Override
	public List<SiegeClan> getDefenderClans()
	{
		return null;
	}
	
	/**
	 * Checks if the siege can grant fame.<br>
	 * This method currently always returns {@code true}.
	 * @return {@code true} if fame can be given.
	 */
	@Override
	public boolean giveFame()
	{
		return true;
	}
	
	/**
	 * Retrieves the frequency for the fortress zone fame task.<br>
	 * This value is pulled from the {@code PlayerConfig} class.
	 * @return The integer frequency of the fame task.
	 */
	@Override
	public int getFameFrequency()
	{
		return PlayerConfig.FORTRESS_ZONE_FAME_TASK_FREQUENCY;
	}
	
	/**
	 * Retrieves the amount of fame points acquired within the fortress zone.<br>
	 * This value is pulled from the {@code PlayerConfig} class.
	 * @return the total fame points as an {@code int}.
	 */
	@Override
	public int getFameAmount()
	{
		return PlayerConfig.FORTRESS_ZONE_FAME_AQUIRE_POINTS;
	}
	
	/**
	 * Updates the current state of the siege.<br>
	 * This method refreshes all active siege mechanics and data.
	 */
	@Override
	public void updateSiege()
	{
	}
}
