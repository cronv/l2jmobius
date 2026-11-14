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
package org.l2jmobius.gameserver.entity.clan.entry;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * This class stores information regarding a {@code Player} who is currently in the process of joining a clan.<br>
 * It tracks the temporary state and data required while the pledge request is pending.
 * @author Sdw
 */
public class PledgeWaitingInfo
{
	private int _playerId;
	private int _playerClassId;
	private int _playerLvl;
	private final int _karma;
	private String _playerName;
	
	/**
	 * Creates a new {@code PledgeWaitingInfo} object.<br>
	 * This constructor initializes the basic data for a player waiting to join a pledge.
	 * @param playerId The unique identifier of the player.
	 * @param playerLvl The current level of the player.
	 * @param karma The karma value of the player.
	 * @param classId The class identifier of the player.
	 * @param playerName The display name of the player.
	 */
	public PledgeWaitingInfo(int playerId, int playerLvl, int karma, int classId, String playerName)
	{
		_playerId = playerId;
		_playerClassId = classId;
		_playerLvl = playerLvl;
		_karma = karma;
		_playerName = playerName;
	}
	
	/**
	 * Retrieves the unique identifier for the player.<br>
	 * This value corresponds to the {@code playerId} field.
	 * @return The {@code int} ID of the player.
	 */
	public int getPlayerId()
	{
		return _playerId;
	}
	
	/**
	 * Sets the unique identifier for the player.<br>
	 * This value is stored in the {@code _playerId} field.
	 * @param playerId The unique ID of the player to set.
	 */
	public void setPlayerId(int playerId)
	{
		_playerId = playerId;
	}
	
	/**
	 * Retrieves the current class ID of the player.<br>
	 * This method updates the internal value if the player is online and their base class has changed.
	 * @return The integer ID representing the player's class.
	 */
	public int getPlayerClassId()
	{
		if (isOnline() && (getPlayer().getBaseClass() != _playerClassId))
		{
			_playerClassId = getPlayer().getPlayerClass().getId();
		}
		
		return _playerClassId;
	}
	
	/**
	 * Retrieves the current level of the player.<br>
	 * This method updates the internal level if the player is online and their level has changed.
	 * @return The current level of the player as an {@code int}.
	 */
	public int getPlayerLvl()
	{
		if (isOnline())
		{
			final int playerLevel = getPlayer().getLevel();
			if (playerLevel != _playerLvl)
			{
				_playerLvl = playerLevel;
			}
		}
		
		return _playerLvl;
	}
	
	/**
	 * Retrieves the karma value of the player.<br>
	 * This value represents the moral standing of the character.
	 * @return The current {@code int} karma value.
	 */
	public int getKarma()
	{
		return _karma;
	}
	
	/**
	 * Retrieves the name of the player.<br>
	 * This method updates the internal {@code _playerName} if the player is online and has a different name.
	 * @return The current name of the player as a {@code String}.
	 */
	public String getPlayerName()
	{
		final String name = getPlayer().getName();
		if (isOnline() && !name.equalsIgnoreCase(_playerName))
		{
			_playerName = name;
		}
		
		return _playerName;
	}
	
	/**
	 * Retrieves the {@code Player} object associated with this info.<br>
	 * It uses the internal {@code _playerId} to find the player in the world.
	 * @return The {@code Player} instance, or {@code null} if not found.
	 */
	public Player getPlayer()
	{
		return World.getPlayer(_playerId);
	}
	
	/**
	 * Checks if the player associated with this info is currently online.<br>
	 * It verifies that the {@code Player} object is not {@code null}.<br>
	 * It also checks if the player's online status is greater than {@code 0}.
	 * @return {@code true} if the player is online, {@code false} otherwise.
	 */
	public boolean isOnline()
	{
		final Player player = getPlayer();
		if (player != null)
		{
			return player.isOnlineInt() > 0;
		}
		
		return false;
	}
}
