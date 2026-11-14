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
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.friend.FriendStatus;

/**
 * Manages the friend list of a {@code Player}.<br>
 * It handles loading the friends, their memos and the online status notifications.
 * @author Mobius
 */
public class Friends
{
	private static final Logger LOGGER = Logger.getLogger(Friends.class.getName());
	
	private final Player _player;
	private final Set<Integer> _friends = ConcurrentHashMap.newKeySet();
	
	/**
	 * Creates a new {@code Friends} for a specific player.
	 * @param player The {@code Player} object associated with this instance.
	 */
	public Friends(Player player)
	{
		_player = player;
	}
	
	/**
	 * This method populates the internal friend list from the database.<br>
	 * It clears the current {@code _friends} before loading new data.<br>
	 * It fetches all friends where the relation is equal to {@code 0}.<br>
	 * If an error occurs during the database query, it logs a warning.
	 */
	public void restore()
	{
		_friends.clear();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT friendId FROM character_friends WHERE charId=? AND relation=0"))
		{
			final int objectId = _player.getObjectId();
			statement.setInt(1, objectId);
			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					final int friendId = rset.getInt("friendId");
					if (friendId == objectId)
					{
						continue;
					}
					
					_friends.add(friendId);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error found in " + _player.getName() + "'s FriendList: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieves the list of friends for this player.
	 * @return a {@code Set<Integer>} containing the IDs of all friends.
	 */
	public Set<Integer> getIds()
	{
		return _friends;
	}
	
	/**
	 * Sends a notification packet to all friends.<br>
	 * The message is delivered to every player in the {@code _friends}.
	 * @param type The type of notification to send.
	 */
	public void notifyStatus(int type)
	{
		final FriendStatus pkt = new FriendStatus(_player, type);
		for (int id : _friends)
		{
			final Player friend = World.getPlayer(id);
			if (friend != null)
			{
				friend.sendPacket(pkt);
			}
		}
	}
	
	/**
	 * Updates the personal note for a specific friend.<br>
	 * The {@code memo} must not exceed 50 characters in length.<br>
	 * This method updates both the database and the local cache.
	 * @param name The unique name of the friend to update.
	 * @param memo The new text for the friend's note.
	 */
	public void updateMemo(String name, String memo)
	{
		if (memo.length() > 50)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE character_friends SET memo=? WHERE charId=? AND friendId=?"))
		{
			final int friendId = PlayerInfoTable.getInstance().getIdByName(name);
			statement.setString(1, memo);
			final int objectId = _player.getObjectId();
			statement.setInt(2, objectId);
			statement.setInt(3, friendId);
			statement.execute();
			
			PlayerInfoTable.getInstance().setFriendMemo(objectId, friendId, memo);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error occurred while updating friend memo: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Adds a friend to the list.
	 * @param objectId The object id of the friend to add.
	 */
	public void add(int objectId)
	{
		_friends.add(objectId);
	}
	
	/**
	 * Removes a friend from the list.
	 * @param objectId The object id of the friend to remove.
	 */
	public void remove(int objectId)
	{
		_friends.remove(Integer.valueOf(objectId));
	}
	
	/**
	 * Checks whether a player is on the friend list.
	 * @param objectId The object id to look for.
	 * @return {@code true} if the player is a friend, {@code false} otherwise.
	 */
	public boolean contains(int objectId)
	{
		return _friends.contains(objectId);
	}
}
