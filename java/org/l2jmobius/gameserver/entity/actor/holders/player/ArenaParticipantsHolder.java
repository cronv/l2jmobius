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
package org.l2jmobius.gameserver.entity.actor.holders.player;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.HandysBlockCheckerManager;
import org.l2jmobius.gameserver.managers.games.BlockCheckerManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This class manages the list of players participating in an arena event.<br>
 * It tracks active participants to facilitate game logic and communication.
 * @author xban1x
 */
public class ArenaParticipantsHolder
{
	private final int _arena;
	private final List<Player> _redPlayers;
	private final List<Player> _bluePlayers;
	private final BlockCheckerManager _engine;
	
	/**
	 * Creates a new instance of {@code ArenaParticipantsHolder}.<br>
	 * This constructor initializes the player lists and the {@code BlockCheckerManager}.
	 * @param arena The unique identifier for the arena.
	 */
	public ArenaParticipantsHolder(int arena)
	{
		_arena = arena;
		_redPlayers = new ArrayList<>(6);
		_bluePlayers = new ArrayList<>(6);
		_engine = new BlockCheckerManager(this, _arena);
	}
	
	/**
	 * Retrieves the list of players on the red team.<br>
	 * This method returns all {@code Player} objects currently assigned to the red side.
	 * @return a {@code List} containing all red team {@code Player} objects.
	 */
	public List<Player> getRedPlayers()
	{
		return _redPlayers;
	}
	
	/**
	 * Retrieves the list of players on the blue team.<br>
	 * This method returns all {@code Player} objects currently assigned to the blue side.
	 * @return A {@code List} containing all blue team {@code Player} objects.
	 */
	public List<Player> getBluePlayers()
	{
		return _bluePlayers;
	}
	
	/**
	 * Retrieves a list of every player currently in the arena.<br>
	 * This method combines both red and blue teams into one list.
	 * @return A {@code List} containing all {@code Player} objects.
	 */
	public List<Player> getAllPlayers()
	{
		final List<Player> all = new ArrayList<>(12);
		all.addAll(_redPlayers);
		all.addAll(_bluePlayers);
		return all;
	}
	
	/**
	 * Adds a {@code Player} to the arena.<br>
	 * The player is assigned to a specific team based on the provided integer.<br>
	 * Use {@code 0} for the red team and any other value for the blue team.
	 * @param player The {@code Player} object to add to the list.
	 * @param team The team ID where the player should be placed.
	 */
	public void addPlayer(Player player, int team)
	{
		if (team == 0)
		{
			_redPlayers.add(player);
		}
		else
		{
			_bluePlayers.add(player);
		}
	}
	
	/**
	 * Removes a specific {@code Player} from the arena participants list.<br>
	 * The removal depends on which team the player belongs to.
	 * @param player The {@code Player} object to be removed.
	 * @param team The team ID of the player, where {@code 0} represents the red team and any other value represents the blue team.
	 */
	public void removePlayer(Player player, int team)
	{
		if (team == 0)
		{
			_redPlayers.remove(player);
		}
		else
		{
			_bluePlayers.remove(player);
		}
	}
	
	/**
	 * Determines which team a specific player belongs to.<br>
	 * Returns the team ID based on the current arena participants.
	 * @param player The {@code Player} object to check.
	 * @return 0 for the red team, 1 for the blue team, or -1 if the player is not in any team.
	 */
	public int getPlayerTeam(Player player)
	{
		if (_redPlayers.contains(player))
		{
			return 0;
		}
		else if (_bluePlayers.contains(player))
		{
			return 1;
		}
		else
		{
			return -1;
		}
	}
	
	/**
	 * Gets the total number of players on the red team.<br>
	 * This method returns the size of the {@code _redPlayers} list.
	 * @return The count of players currently assigned to the red team.
	 */
	public int getRedTeamSize()
	{
		return _redPlayers.size();
	}
	
	/**
	 * Gets the total number of players on the blue team.<br>
	 * This method counts the elements in the {@code _bluePlayers} list.
	 * @return The size of the blue team as an {@code int}.
	 */
	public int getBlueTeamSize()
	{
		return _bluePlayers.size();
	}
	
	/**
	 * Sends a network packet to every player in the arena.<br>
	 * This method iterates through both red and blue teams.<br>
	 * It ensures all participants receive the same {@code ServerPacket}.
	 * @param packet The {@code ServerPacket} to be sent to all players.
	 */
	public void broadCastPacketToTeam(ServerPacket packet)
	{
		for (Player p : _redPlayers)
		{
			p.sendPacket(packet);
		}
		
		for (Player p : _bluePlayers)
		{
			p.sendPacket(packet);
		}
	}
	
	/**
	 * Removes all players from the arena.<br>
	 * This method clears both the red and blue player lists.
	 */
	public void clearPlayers()
	{
		_redPlayers.clear();
		_bluePlayers.clear();
	}
	
	/**
	 * Retrieves the block checker engine for this event.<br>
	 * This provides access to the {@code BlockCheckerManager} instance.
	 * @return the {@code BlockCheckerManager} used by this holder.
	 */
	public BlockCheckerManager getEvent()
	{
		return _engine;
	}
	
	/**
	 * Updates the current event state.<br>
	 * This method notifies the {@code BlockCheckerManager} to refresh the player list.
	 */
	public void updateEvent()
	{
		_engine.updatePlayersOnStart(this);
	}
	
	/**
	 * Checks if the teams are balanced in size.<br>
	 * If one team is too large, it moves players to the other team.<br>
	 * It sends a system message to the affected team when changes occur.
	 */
	public void checkAndShuffle()
	{
		final int redSize = _redPlayers.size();
		final int blueSize = _bluePlayers.size();
		if (redSize > (blueSize + 1))
		{
			broadCastPacketToTeam(new SystemMessage(SystemMessageId.TEAM_MEMBERS_WERE_MODIFIED_BECAUSE_THE_TEAMS_WERE_UNBALANCED));
			final int needed = redSize - (blueSize + 1);
			for (int i = 0; i < (needed + 1); i++)
			{
				final Player plr = _redPlayers.get(i);
				if (plr == null)
				{
					continue;
				}
				
				HandysBlockCheckerManager.getInstance().changePlayerToTeam(plr, _arena);
			}
		}
		else if (blueSize > (redSize + 1))
		{
			broadCastPacketToTeam(new SystemMessage(SystemMessageId.TEAM_MEMBERS_WERE_MODIFIED_BECAUSE_THE_TEAMS_WERE_UNBALANCED));
			final int needed = blueSize - (redSize + 1);
			for (int i = 0; i < (needed + 1); i++)
			{
				final Player plr = _bluePlayers.get(i);
				if (plr == null)
				{
					continue;
				}
				
				HandysBlockCheckerManager.getInstance().changePlayerToTeam(plr, _arena);
			}
		}
	}
}
