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
 * This class stores information regarding a player who has applied to join a clan.<br>
 * It acts as a data container for the application process within {@code PledgeApplicantInfo}.
 * @author Sdw
 */
public class PledgeApplicantInfo
{
	private final int _playerId;
	private final int _requestClanId;
	private String _playerName;
	private int _playerLvl;
	private int _classId;
	private final int _karma;
	private final String _message;
	
	/**
	 * Creates a new {@code PledgeApplicantInfo} object.<br>
	 * This constructor initializes the applicant data with provided values.
	 * @param playerId The unique identifier of the player.
	 * @param playerName The display name of the player.
	 * @param playerLevel The current level of the player.
	 * @param karma The karma value of the player.
	 * @param requestClanId The ID of the clan being requested.
	 * @param message The custom message sent by the applicant.
	 */
	public PledgeApplicantInfo(int playerId, String playerName, int playerLevel, int karma, int requestClanId, String message)
	{
		_playerId = playerId;
		_requestClanId = requestClanId;
		_playerName = playerName;
		_playerLvl = playerLevel;
		_karma = karma;
		_message = message;
	}
	
	/**
	 * Retrieves the unique identifier for the player.<br>
	 * This value corresponds to the {@code playerId} provided during object creation.
	 * @return The {@code int} ID of the player.
	 */
	public int getPlayerId()
	{
		return _playerId;
	}
	
	/**
	 * Retrieves the ID of the clan requested by the applicant.<br>
	 * This value was provided during the pledge application process.
	 * @return The unique identifier for the requested clan.
	 */
	public int getRequestClanId()
	{
		return _requestClanId;
	}
	
	/**
	 * Retrieves the name of the player who submitted the pledge application.<br>
	 * This method checks if the player is currently online to ensure the name is up to date.
	 * @return The {@code String} name of the player.
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
	 * Retrieves the current level of the player.<br>
	 * This method updates the internal {@code _playerLvl} if the player is online and their level has changed.
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
	 * Retrieves the class ID of the player.<br>
	 * This method updates the internal {@code _classId} if the player is online and their current base class has changed.
	 * @return The integer ID representing the player's class.
	 */
	public int getClassId()
	{
		if (isOnline() && (getPlayer().getBaseClass() != _classId))
		{
			_classId = getPlayer().getPlayerClass().getId();
		}
		
		return _classId;
	}
	
	/**
	 * Retrieves the message provided by the applicant.<br>
	 * This returns the text associated with the pledge request.
	 * @return The {@code String} containing the applicant's message.
	 */
	public String getMessage()
	{
		return _message;
	}
	
	/**
	 * Retrieves the karma value of the applicant.<br>
	 * This value represents the player's moral standing.
	 * @return The current {@code int} karma value.
	 */
	public int getKarma()
	{
		return _karma;
	}
	
	/**
	 * Retrieves the {@code Player} object associated with this application.<br>
	 * It uses the internal {@code _playerId} to fetch the player from the world.
	 * @return the {@code Player} instance or {@code null} if not found.
	 */
	public Player getPlayer()
	{
		return World.getPlayer(_playerId);
	}
	
	/**
	 * Checks if the player associated with this application is currently online.<br>
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
