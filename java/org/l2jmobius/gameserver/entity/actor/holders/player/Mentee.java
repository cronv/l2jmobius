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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * Represents a player who is currently being mentored by another player.<br>
 * This class manages the relationship and data associated with the mentorship system.
 * @author UnAfraid
 */
public class Mentee
{
	private static final Logger LOGGER = Logger.getLogger(Mentee.class.getName());
	
	private final int _objectId;
	private String _name;
	private int _classId;
	private int _currentLevel;
	
	/**
	 * Creates a new {@code Mentee} instance.<br>
	 * This constructor sets the unique identifier and loads data from the database.
	 * @param objectId The unique ID of the mentee.
	 */
	public Mentee(int objectId)
	{
		_objectId = objectId;
		load();
	}
	
	/**
	 * Loads the character data for this {@code Mentee}.<br>
	 * It retrieves information from the database if the player is offline.<br>
	 * If the player is online, it updates the fields using the {@code Player} object.
	 */
	public void load()
	{
		final Player player = getPlayer();
		if (player == null) // Only if player is offline
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement statement = con.prepareStatement("SELECT char_name, level, base_class FROM characters WHERE charId = ?"))
			{
				statement.setInt(1, _objectId);
				try (ResultSet rset = statement.executeQuery())
				{
					if (rset.next())
					{
						_name = rset.getString("char_name");
						_classId = rset.getInt("base_class");
						_currentLevel = rset.getInt("level");
					}
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, e.getMessage(), e);
			}
		}
		else
		{
			_name = player.getName();
			_classId = player.getBaseClass();
			_currentLevel = player.getLevel();
		}
	}
	
	/**
	 * Retrieves the unique identifier for this {@code Mentee}.<br>
	 * This ID is used to identify the object in the system.
	 * @return The unique integer ID of the {@code Mentee}.
	 */
	public int getObjectId()
	{
		return _objectId;
	}
	
	/**
	 * Retrieves the name of the {@code Mentee}.<br>
	 * This method returns the current name stored in the object.
	 * @return The name as a {@code String}.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Retrieves the current class ID of the mentee.<br>
	 * This method updates the internal {@code _classId} if the player is online and has changed their class.
	 * @return The integer ID of the mentee's class.
	 */
	public int getClassId()
	{
		if (isOnline())
		{
			final int classId = getPlayer().getPlayerClass().getId();
			if (classId != _classId)
			{
				_classId = classId;
			}
		}
		
		return _classId;
	}
	
	/**
	 * Retrieves the current level of the mentee.<br>
	 * This method updates the internal {@code _currentLevel} if the player is online.
	 * @return The current level as an {@code int}.
	 */
	public int getLevel()
	{
		if (isOnline())
		{
			final int playerLevel = getPlayer().getLevel();
			if (playerLevel != _currentLevel)
			{
				_currentLevel = playerLevel;
			}
		}
		
		return _currentLevel;
	}
	
	/**
	 * Retrieves the {@code Player} object associated with this mentee.<br>
	 * It uses the internal {@code _objectId} to find the player in the world.
	 * @return the {@code Player} instance or {@code null} if not found.
	 */
	public Player getPlayer()
	{
		return World.getPlayer(_objectId);
	}
	
	/**
	 * Checks if the player associated with this {@code Mentee} is currently online.<br>
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
	
	/**
	 * Checks if the player is currently online.<br>
	 * It returns a specific integer status based on the connection state.
	 * @return The online status as an {@code int}, or {@code 0} if the player is offline.
	 */
	public int isOnlineInt()
	{
		return isOnline() ? getPlayer().isOnlineInt() : 0;
	}
	
	/**
	 * Sends a network packet to the player associated with this {@code Mentee}.<br>
	 * This method checks if the player is currently online before sending.
	 * @param packet The {@code ServerPacket} to be sent.
	 */
	public void sendPacket(ServerPacket packet)
	{
		if (isOnline())
		{
			getPlayer().sendPacket(packet);
		}
	}
}
