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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.sql.PlayerInfoTable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Manages the collection of contacts for a {@code Player}.<br>
 * It handles adding, removing, and retrieving players from the user's contact list.<br>
 * This class interacts with the database to persist contact information.
 * @author UnAfraid, mrTJO
 */
public class ContactList
{
	private static final Logger LOGGER = Logger.getLogger(ContactList.class.getName());
	
	private final Player _player;
	private final Set<String> _contacts = ConcurrentHashMap.newKeySet();
	
	private static final String QUERY_ADD = "REPLACE INTO character_contacts (charId, contactId) VALUES (?, ?)";
	private static final String QUERY_REMOVE = "DELETE FROM character_contacts WHERE charId = ? and contactId = ?";
	private static final String QUERY_LOAD = "SELECT contactId FROM character_contacts WHERE charId = ?";
	
	/**
	 * Creates a new {@code ContactList} for a specific player.<br>
	 * This constructor initializes the list and loads existing data from the database.
	 * @param player The {@code Player} object associated with this contact list.
	 */
	public ContactList(Player player)
	{
		_player = player;
		restore();
	}
	
	/**
	 * Reloads the contact list from the database.<br>
	 * This method clears the current {@code _contacts} set.<br>
	 * It then fetches all valid contacts for the associated {@code Player}.<br>
	 * Any errors during the database operation are logged as a warning.
	 */
	public void restore()
	{
		_contacts.clear();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(QUERY_LOAD))
		{
			final int objectId = _player.getObjectId();
			statement.setInt(1, objectId);
			try (ResultSet rset = statement.executeQuery())
			{
				int contactId;
				String contactName;
				while (rset.next())
				{
					contactId = rset.getInt(1);
					contactName = PlayerInfoTable.getInstance().getNameById(contactId);
					if ((contactName == null) || contactName.equals(_player.getName()) || (contactId == objectId))
					{
						continue;
					}
					
					_contacts.add(contactName);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error found in " + _player.getName() + "'s ContactsList: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Adds a new name to the player's contact list.<br>
	 * This method checks if the name is valid and not already present.<br>
	 * It also verifies that the list does not exceed the maximum size of 100.
	 * @param name The name of the contact to add.
	 * @return {@code true} if the name was added successfully, or {@code false} otherwise.
	 */
	public boolean add(String name)
	{
		if (_contacts.contains(name))
		{
			_player.sendPacket(SystemMessageId.THE_CHARACTER_IS_ALREADY_IN_YOUR_FAVORITE_CONTACTS_LIST);
			return false;
		}
		
		final String playerName = _player.getName();
		if (playerName.equals(name))
		{
			_player.sendPacket(SystemMessageId.YOU_CANNOT_ADD_YOURSELF);
			return false;
		}
		
		if (_contacts.size() >= 100)
		{
			_player.sendPacket(SystemMessageId.YOU_CANNOT_ADD_MORE_THAN_100_CHARACTERS_TO_THE_LIST);
			return false;
		}
		
		SystemMessage sm;
		final int contactId = PlayerInfoTable.getInstance().getIdByName(name);
		if (contactId < 1)
		{
			sm = new SystemMessage(SystemMessageId.S1_NOT_FOUND_PLEASE_TRY_ANOTHER_NAME);
			sm.addString(name);
			_player.sendPacket(sm);
			return false;
		}
		
		for (String contactName : _contacts)
		{
			if (contactName.equalsIgnoreCase(name))
			{
				_player.sendPacket(SystemMessageId.THE_CHARACTER_IS_ALREADY_IN_YOUR_FAVORITE_CONTACTS_LIST);
				return false;
			}
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(QUERY_ADD))
		{
			statement.setInt(1, _player.getObjectId());
			statement.setInt(2, contactId);
			statement.execute();
			
			_contacts.add(name);
			
			sm = new SystemMessage(SystemMessageId.S1_IS_ADDED_TO_YOUR_FAVORITE_CONTACTS_LIST);
			sm.addString(name);
			_player.sendPacket(sm);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error found in " + playerName + "'s ContactsList: " + e.getMessage(), e);
		}
		
		return true;
	}
	
	/**
	 * Removes a contact from the player's list.<br>
	 * This method deletes the entry from the database and updates the local set.<br>
	 * If the name is not found, it sends a system message to the {@code Player}.
	 * @param name The name of the contact to remove.
	 */
	public void remove(String name)
	{
		if (!_contacts.contains(name))
		{
			_player.sendPacket(SystemMessageId.THE_NAME_IS_NOT_CURRENTLY_REGISTERED);
			return;
		}
		
		final int contactId = PlayerInfoTable.getInstance().getIdByName(name);
		if (contactId < 1)
		{
			// TODO: Message?
			return;
		}
		
		_contacts.remove(name);
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(QUERY_REMOVE))
		{
			statement.setInt(1, _player.getObjectId());
			statement.setInt(2, contactId);
			statement.execute();
			
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_REMOVED_FROM_YOUR_FAVORITE_CONTACTS_LIST);
			sm.addString(name);
			_player.sendPacket(sm);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error found in " + _player.getName() + "'s ContactsList: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieves all contacts for the current player.<br>
	 * This method returns a {@code Set} of contact names.
	 * @return A {@code Set<String>} containing all registered contacts.
	 */
	public Set<String> getAllContacts()
	{
		return _contacts;
	}
}
