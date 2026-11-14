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
package org.l2jmobius.gameserver.mechanics.undergroundColiseum;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.UndergroundColiseumConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Team;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExPVPMatchRecord;
import org.l2jmobius.gameserver.network.serverpackets.ExPVPMatchUserDie;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * Manages the logic and lifecycle of the Underground Coliseum arena.<br>
 * This class handles player matchmaking, team assignment, and match progression for the {@code UCArena} system.
 */
public class UCArena
{
	private static final long MINUTES_IN_MILISECONDS = 10 * 60000;
	
	private final int _id;
	private final int _minLevel;
	private final int _maxLevel;
	
	private final UCPoint[] _points = new UCPoint[4];
	private final UCTeam[] _teams = new UCTeam[2];
	private Npc _manager = null;
	
	private ScheduledFuture<?> _taskFuture = null;
	private final List<UCWaiting> _waitingPartys = new CopyOnWriteArrayList<>();
	private final List<UCReward> _rewards = new ArrayList<>();
	private boolean _isBattleNow = false;
	
	/**
	 * Creates a new instance of {@code UCArena}.<br>
	 * This constructor initializes the arena with specific level requirements.<br>
	 * It also sets up the manager based on the provided curator ID.
	 * @param id The unique identifier for this arena.
	 * @param curator The ID used to determine the arena manager spawn.
	 * @param minLevel The minimum player level required to enter.
	 * @param maxLevel The maximum player level allowed in the arena.
	 */
	public UCArena(int id, int curator, int minLevel, int maxLevel)
	{
		_id = id;
		final Spawn spawn = SpawnTable.getInstance().getAnySpawn(curator);
		if (spawn != null)
		{
			_manager = spawn.getLastSpawn();
		}
		
		_minLevel = minLevel;
		_maxLevel = maxLevel;
		_rewards.clear();
	}
	
	/**
	 * Retrieves the unique identifier for this {@code UCArena}.
	 * @return the {@code int} id of the arena.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Returns the minimum level required to enter this arena.<br>
	 * This value is used to filter eligible players.
	 * @return the minimum level as an {@code int}.
	 */
	public int getMinLevel()
	{
		return _minLevel;
	}
	
	/**
	 * Retrieves the maximum level allowed for this arena.
	 * @return the {@code int} value of the maximum level.
	 */
	public int getMaxLevel()
	{
		return _maxLevel;
	}
	
	/**
	 * Retrieves the manager associated with this arena.
	 * @return the {@code Npc} instance that manages the arena.
	 */
	public Npc getManager()
	{
		return _manager;
	}
	
	/**
	 * Sets a specific {@code UCPoint} at the given index.<br>
	 * This method updates the internal points array.<br>
	 * It ignores any index greater than 4.
	 * @param index The position in the points array to update.
	 * @param point The {@code UCPoint} object to assign.
	 */
	public void setUCPoint(int index, UCPoint point)
	{
		if (index > 4)
		{
			return;
		}
		
		_points[index] = point;
	}
	
	/**
	 * Sets the {@code UCTeam} for a specific index.<br>
	 * This method updates the team at the given position in the internal array.<br>
	 * It only accepts indices between 0 and 2 inclusive.
	 * @param index The position of the team to update.
	 * @param team The {@code UCTeam} object to assign.
	 */
	public void setUCTeam(int index, UCTeam team)
	{
		if (index > 2)
		{
			return;
		}
		
		_teams[index] = team;
	}
	
	/**
	 * Retrieves all teams associated with this arena.
	 * @return an array of {@code UCTeam} objects.
	 */
	public UCTeam[] getTeams()
	{
		return _teams;
	}
	
	/**
	 * Retrieves all points associated with this arena.
	 * @return an array of {@code UCPoint} objects.
	 */
	public UCPoint[] getPoints()
	{
		return _points;
	}
	
	/**
	 * Retrieves the list of parties currently waiting for a match.
	 * @return A {@code List} containing all {@code UCWaiting} objects.
	 */
	public List<UCWaiting> getWaitingList()
	{
		return _waitingPartys;
	}
	
	/**
	 * Changes the current state of the arena.<br>
	 * If {@code start} is {@code true}, it initiates a new task if one is not already running.<br>
	 * If {@code start} is {@code false}, it cancels the current task and resets all arena elements.
	 * @param start The boolean flag indicating whether to start or stop the arena activity.
	 */
	public void switchStatus(boolean start)
	{
		if ((_taskFuture == null) && start)
		{
			runNewTask(false);
		}
		else
		{
			if (_taskFuture != null)
			{
				_taskFuture.cancel(true);
				_taskFuture = null;
			}
			
			generateWinner();
			removeTeams();
			for (UCTeam team : getTeams())
			{
				team.cleanUp();
			}
			
			for (UCPoint point : getPoints())
			{
				point.actionDoors(false);
				point.getPlayers().clear();
			}
			
			_isBattleNow = false;
		}
	}
	
	/**
	 * Starts a new task for the arena.<br>
	 * This method schedules an {@code UCRunningTask} to execute after a specific delay.<br>
	 * The duration depends on whether the match is full time or not.
	 * @param isFullTime Determines if the task should run after 10 minutes or 9 minutes.
	 */
	public void runNewTask(boolean isFullTime)
	{
		final long time = isFullTime ? MINUTES_IN_MILISECONDS : MINUTES_IN_MILISECONDS - 60000L;
		_taskFuture = ThreadPool.schedule(new UCRunningTask(this), time);
	}
	
	/**
	 * Executes the current arena task immediately.<br>
	 * This method cancels any existing scheduled task.<br>
	 * It then schedules a new {@code UCRunningTask} to run now.
	 */
	public void runTaskNow()
	{
		if (_taskFuture != null)
		{
			_taskFuture.cancel(true);
			_taskFuture = null;
		}
		
		_taskFuture = ThreadPool.schedule(new UCRunningTask(this), 0);
	}
	
	/**
	 * Determines the winner of the current match.<br>
	 * Checks team statuses, party existence, and kill counts to decide the result.<br>
	 * Updates the status of both teams based on the outcome.<br>
	 * Broadcasts the final match record to all players.
	 */
	public void generateWinner()
	{
		final UCTeam blueTeam = _teams[0];
		final UCTeam redTeam = _teams[1];
		UCTeam winnerTeam = null;
		
		if ((blueTeam.getStatus() == UCTeam.WIN) || (redTeam.getStatus() == UCTeam.WIN))
		{
			winnerTeam = blueTeam.getStatus() == UCTeam.WIN ? blueTeam : redTeam;
		}
		else
		{
			if ((blueTeam.getParty() == null) && (redTeam.getParty() != null))
			{
				redTeam.setStatus(UCTeam.WIN);
				winnerTeam = redTeam;
			}
			else if ((redTeam.getParty() == null) && (blueTeam.getParty() != null))
			{
				blueTeam.setStatus(UCTeam.WIN);
				winnerTeam = blueTeam;
			}
			else if ((redTeam.getParty() != null) && (blueTeam.getParty() != null))
			{
				final int blueKillCount = blueTeam.getKillCount();
				final int redKillCount = redTeam.getKillCount();
				if (blueKillCount > redKillCount)
				{
					blueTeam.setStatus(UCTeam.WIN);
					redTeam.setStatus(UCTeam.FAIL);
					winnerTeam = blueTeam;
				}
				else if (redKillCount > blueKillCount)
				{
					blueTeam.setStatus(UCTeam.FAIL);
					redTeam.setStatus(UCTeam.WIN);
					winnerTeam = redTeam;
				}
				else if (blueKillCount == redKillCount)
				{
					if (blueTeam.getRegisterTime() > redTeam.getRegisterTime())
					{
						blueTeam.setStatus(UCTeam.FAIL);
						redTeam.setStatus(UCTeam.WIN);
						winnerTeam = redTeam;
					}
					else
					{
						blueTeam.setStatus(UCTeam.WIN);
						redTeam.setStatus(UCTeam.FAIL);
						winnerTeam = blueTeam;
					}
				}
			}
		}
		
		if (winnerTeam != null)
		{
			broadcastRecord(ExPVPMatchRecord.FINISH, (winnerTeam.getIndex() + 1));
		}
		else
		{
			broadcastRecord(ExPVPMatchRecord.FINISH, 0);
		}
		
		blueTeam.setLastParty(redTeam.getParty());
		redTeam.setLastParty(blueTeam.getParty());
	}
	
	/**
	 * Sends a specific packet to every player in all teams.<br>
	 * This method iterates through each {@code UCTeam} and delivers the {@code ServerPacket} to every member.<br>
	 * It ensures that only non-null players receive the data.
	 * @param packet The {@code ServerPacket} to be sent to all participants.
	 */
	public void broadcastToAll(ServerPacket packet)
	{
		for (UCTeam team : getTeams())
		{
			final Party party = team.getParty();
			if (party != null)
			{
				for (Player member : party.getMembers())
				{
					if (member != null)
					{
						member.sendPacket(packet);
					}
				}
			}
		}
	}
	
	/**
	 * Prepares the arena for a new match by starting a countdown.<br>
	 * It validates that all teams are present and near the manager.<br>
	 * If validation fails, the match is canceled automatically.<br>
	 * Otherwise, it splits members, teleports them, and starts the fight.
	 */
	public void prepareStart()
	{
		_isBattleNow = true;
		broadcastToAll(new ExShowScreenMessage(NpcStringId.THE_MATCH_BEGINS_IN_S1_MIN_PLEASE_GATHER_AROUND_THE_ADMINISTRATOR, 2, 5000, "1"));
		try
		{
			Thread.sleep(30000);
		}
		catch (InterruptedException e)
		{
		}
		
		broadcastToAll(new ExShowScreenMessage(NpcStringId.TIME_LEFT_S1_SEC, 2, 5000, "30"));
		try
		{
			Thread.sleep(20000);
		}
		catch (InterruptedException e)
		{
		}
		
		broadcastToAll(new ExShowScreenMessage(NpcStringId.TIME_LEFT_S1_SEC, 2, 3000, "10"));
		
		try
		{
			Thread.sleep(5000);
		}
		catch (InterruptedException e)
		{
		}
		
		broadcastToAll(new ExShowScreenMessage(NpcStringId.TIME_LEFT_S1_SEC, 2, 1000, "5"));
		
		try
		{
			Thread.sleep(1000);
		}
		catch (InterruptedException e)
		{
		}
		
		broadcastToAll(new ExShowScreenMessage(NpcStringId.TIME_LEFT_S1_SEC, 2, 1000, "4"));
		
		try
		{
			Thread.sleep(1000);
		}
		catch (InterruptedException e)
		{
		}
		
		broadcastToAll(new ExShowScreenMessage(NpcStringId.TIME_LEFT_S1_SEC, 2, 1000, "3"));
		
		try
		{
			Thread.sleep(1000);
		}
		catch (InterruptedException e)
		{
		}
		
		broadcastToAll(new ExShowScreenMessage(NpcStringId.TIME_LEFT_S1_SEC, 2, 1000, "2"));
		
		try
		{
			Thread.sleep(1000);
		}
		catch (InterruptedException e)
		{
		}
		
		broadcastToAll(new ExShowScreenMessage(NpcStringId.TIME_LEFT_S1_SEC, 2, 1000, "1"));
		
		try
		{
			Thread.sleep(1000);
		}
		catch (InterruptedException e)
		{
		}
		
		boolean isValid = true;
		for (UCTeam team : _teams)
		{
			if (team.getParty() == null)
			{
				isValid = false;
				continue;
			}
			
			if (team.getParty().getMemberCount() < UndergroundColiseumConfig.UC_PARTY_SIZE)
			{
				isValid = false;
				continue;
			}
			
			for (Player pl : team.getParty().getMembers())
			{
				if ((pl != null) && (pl.calculateDistance3D(_manager) > PlayerConfig.ALT_PARTY_RANGE))
				{
					isValid = false;
					break;
				}
			}
		}
		
		if (!isValid)
		{
			broadcastToAll(new ExShowScreenMessage(NpcStringId.THE_MATCH_IS_CANCELLED_AS_YOU_ARE_TOO_FAR_FROM_THE_MANAGER, 2, 5000));
			for (UCTeam team : _teams)
			{
				team.setParty(null);
				team.setRegisterTime(0);
			}
			
			_isBattleNow = false;
			runNewTask(false);
			return;
		}
		
		runNewTask(true);
		splitMembersAndTeleport();
		startFight();
	}
	
	/**
	 * Splits the players into different teams and teleports them to their starting positions.<br>
	 * This method clears all current points and assigns each player to a {@code UCPoint}.<br>
	 * It also broadcasts the start of the match to all participants.
	 */
	public void splitMembersAndTeleport()
	{
		final UCPoint[] positions = getPoints();
		for (UCPoint point : positions)
		{
			point.getPlayers().clear();
		}
		
		broadcastRecord(ExPVPMatchRecord.START, 0);
		
		for (UCTeam team : getTeams())
		{
			final Party party = team.getParty();
			if (party != null)
			{
				int i = 0;
				for (Player player : party.getMembers())
				{
					if (player != null)
					{
						player.setUCState(Player.UC_STATE_POINT);
						positions[i].teleportPlayer(player);
						i++;
						if (i >= 3)
						{
							i = 0;
						}
					}
				}
			}
		}
		
		broadcastRecord(ExPVPMatchRecord.UPDATE, 0);
	}
	
	/**
	 * Sends a match record packet to all players in the arena.<br>
	 * It also sends a death notification if the {@code type} is {@code ExPVPMatchRecord.UPDATE}.
	 * @param type The type of the match record to broadcast.
	 * @param teamType The specific team type associated with the record.
	 */
	public void broadcastRecord(int type, int teamType)
	{
		final ExPVPMatchRecord packet = new ExPVPMatchRecord(type, teamType, this);
		final ExPVPMatchUserDie packet2 = type == ExPVPMatchRecord.UPDATE ? new ExPVPMatchUserDie(this) : null;
		for (UCTeam team : getTeams())
		{
			final Party party = team.getParty();
			if (party != null)
			{
				for (Player member : party.getMembers())
				{
					if (member != null)
					{
						member.sendPacket(packet);
						if (packet2 != null)
						{
							member.sendPacket(packet2);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Initiates the combat phase of the arena.<br>
	 * This method spawns towers for every {@code UCTeam}.<br>
	 * It also assigns a {@code Team} color to each {@code Player} based on their team index.
	 */
	public void startFight()
	{
		for (UCTeam team : _teams)
		{
			team.spawnTower();
			for (Player player : team.getParty().getMembers())
			{
				if (player != null)
				{
					player.setTeam(team.getIndex() == 1 ? Team.BLUE : Team.RED);
				}
			}
		}
	}
	
	/**
	 * Clears all team assignments for players in the arena.<br>
	 * Resets player stats and states to their default values.<br>
	 * Teleports players back to their original location or to town.
	 */
	public void removeTeams()
	{
		for (UCTeam team : _teams)
		{
			if (team.getParty() != null)
			{
				for (Player player : team.getParty().getMembers())
				{
					if (player == null)
					{
						continue;
					}
					
					player.setTeam(Team.NONE);
					player.cleanUCStats();
					player.setUCState(Player.UC_STATE_NONE);
					if (player.isDead())
					{
						UCTeam.resPlayer(player);
					}
					
					final String restore = player.getVariables().getString(PlayerVariables.RESTORE_LOCATION, "");
					if (!restore.isEmpty())
					{
						final String[] split = restore.split(";");
						player.teleToLocation(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]), player.getHeading(), true);
					}
					else
					{
						player.teleToLocation(TeleportWhereType.TOWN);
					}
				}
			}
		}
	}
	
	/**
	 * Checks if a battle is currently in progress.
	 * @return {@code true} if the battle is active, {@code false} otherwise.
	 */
	public boolean isBattleNow()
	{
		return _isBattleNow;
	}
	
	/**
	 * Updates the current battle status of the arena.<br>
	 * Sets whether a fight is currently taking place.
	 * @param value The new status to set as {@code true} for active battles or {@code false} otherwise.
	 */
	public void setIsBattleNow(boolean value)
	{
		_isBattleNow = value;
	}
	
	/**
	 * Adds a new reward to the arena.<br>
	 * This method updates the list of rewards for this {@code UCArena}.
	 * @param ucReward The {@code UCReward} object to add.
	 */
	public void setReward(UCReward ucReward)
	{
		_rewards.add(ucReward);
	}
	
	/**
	 * Retrieves the list of rewards for this arena.
	 * @return a {@code List} containing all {@code UCReward} objects.
	 */
	public List<UCReward> getRewards()
	{
		return _rewards;
	}
}
