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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.sql.PlayerInfoTable;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.BlockListPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Manages the list of blocked players for a specific {@code Player} instance.<br>
 * It handles checking, adding, and removing entries from the block list.<br>
 * This class ensures that interactions between blocked users are restricted.
 */
public class BlockList
{
	private static final Logger LOGGER = Logger.getLogger(BlockList.class.getName());
	
	private static final Map<Integer, Set<Integer>> OFFLINE_LIST = new ConcurrentHashMap<>();
	
	private final Player _owner;
	private Set<Integer> _blockList;
	
	/**
	 * Creates a new {@code BlockList} for a specific player.<br>
	 * This constructor assigns the owner and loads their block list from the database.
	 * @param owner The {@code Player} who owns this block list.
	 */
	public BlockList(Player owner)
	{
		_owner = owner;
		_blockList = OFFLINE_LIST.get(owner.getObjectId());
		if (_blockList == null)
		{
			_blockList = loadList(_owner.getObjectId());
		}
	}
	
	/**
	 * Adds a specific player to the block list.<br>
	 * This method updates the local set and saves the change to the database.
	 * @param target The {@code int} ID of the player to block.
	 */
	private void addToBlockList(int target)
	{
		_blockList.add(target);
		updateInDB(target, true);
	}
	
	/**
	 * Removes a specific player from the block list.<br>
	 * This method updates the internal set and the database.
	 * @param target The {@code int} ID of the player to remove.
	 */
	private void removeFromBlockList(int target)
	{
		_blockList.remove(Integer.valueOf(target));
		updateInDB(target, false);
	}
	
	/**
	 * Saves the current block list to the {@code OFFLINE_LIST}.<br>
	 * This ensures the data is preserved when the {@code Player} logs out.<br>
	 * The list is mapped using the owner's object ID.
	 */
	public void playerLogout()
	{
		OFFLINE_LIST.put(_owner.getObjectId(), _blockList);
	}
	
	/**
	 * Loads the block list for a specific player from the database.<br>
	 * It retrieves all friend IDs associated with the given {@code objId}.<br>
	 * The method handles potential database errors by logging a warning.
	 * @param objId The unique identifier of the character to load the list for.
	 * @return A {@code Set} containing the IDs of blocked players.
	 */
	private static Set<Integer> loadList(int objId)
	{
		final Set<Integer> list = new HashSet<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT friendId FROM character_friends WHERE charId=? AND relation=1"))
		{
			statement.setInt(1, objId);
			try (ResultSet rset = statement.executeQuery())
			{
				int friendId;
				while (rset.next())
				{
					friendId = rset.getInt("friendId");
					if (friendId == objId)
					{
						continue;
					}
					
					list.add(friendId);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error found in " + objId + " FriendList while loading BlockList: " + e.getMessage(), e);
		}
		
		return list;
	}
	
	/**
	 * Updates the database to save the current block status.<br>
	 * It inserts a record if {@code state} is {@code true}.<br>
	 * It deletes the record if {@code state} is {@code false}.
	 * @param targetId The unique identifier of the player to update.
	 * @param state The new block status to save in the database.
	 */
	private void updateInDB(int targetId, boolean state)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			if (state) // add
			{
				try (PreparedStatement statement = con.prepareStatement("INSERT INTO character_friends (charId, friendId, relation) VALUES (?, ?, 1)"))
				{
					statement.setInt(1, _owner.getObjectId());
					statement.setInt(2, targetId);
					statement.execute();
				}
			}
			else
			// remove
			{
				try (PreparedStatement statement = con.prepareStatement("DELETE FROM character_friends WHERE charId=? AND friendId=? AND relation=1"))
				{
					statement.setInt(1, _owner.getObjectId());
					statement.setInt(2, targetId);
					statement.execute();
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not add block player: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Checks if a specific player is on the block list.<br>
	 * This method returns {@code true} if the target is blocked.<br>
	 * It returns {@code false} otherwise.
	 * @param target The {@code Player} to check.
	 * @return The result of the check as a {@code boolean}.
	 */
	public boolean isInBlockList(Player target)
	{
		return _blockList.contains(target.getObjectId());
	}
	
	/**
	 * Checks if a specific ID is present in the block list.<br>
	 * This method returns {@code true} if the ID is blocked.<br>
	 * It returns {@code false} otherwise.
	 * @param targetId The unique identifier of the entity to check.
	 * @return The result of the block list check.
	 */
	public boolean isInBlockList(int targetId)
	{
		return _blockList.contains(targetId);
	}
	
	/**
	 * Checks if the player is currently refusing all messages.<br>
	 * This method returns {@code true} if message refusal is active.<br>
	 * It returns {@code false} otherwise.
	 * @return {@code true} if all messages are blocked, {@code false} otherwise.
	 */
	public boolean isBlockAll()
	{
		return _owner.getMessageRefusal();
	}
	
	/**
	 * Checks if a target player is blocked by the owner.<br>
	 * This method returns {@code true} if all players are blocked or if the specific target is in the list.
	 * @param listOwner The {@code Player} who owns the block list.
	 * @param target The {@code Player} to check against the block list.
	 * @return {@code true} if the target is blocked, {@code false} otherwise.
	 */
	public static boolean isBlocked(Player listOwner, Player target)
	{
		final BlockList blockList = listOwner.getBlockList();
		return blockList.isBlockAll() || blockList.isInBlockList(target);
	}
	
	/**
	 * Checks if a specific player is blocked by another player.<br>
	 * This method returns {@code true} if the target is in the owner's block list or if all players are blocked.
	 * @param listOwner The {@code Player} who owns the block list.
	 * @param targetId The unique identifier of the target player to check.
	 * @return {@code true} if the target is blocked, {@code false} otherwise.
	 */
	public static boolean isBlocked(Player listOwner, int targetId)
	{
		final BlockList blockList = listOwner.getBlockList();
		return blockList.isBlockAll() || blockList.isInBlockList(targetId);
	}
	
	/**
	 * Updates the message refusal status for the player.<br>
	 * This method sets whether the {@code _owner} should refuse all messages.
	 * @param value The new state to set for message refusal.
	 */
	private void setBlockAll(boolean value)
	{
		_owner.setMessageRefusal(value);
	}
	
	/**
	 * Retrieves the set of blocked IDs for this player.
	 * @return a {@code Set<Integer>} containing all blocked IDs.
	 */
	public Set<Integer> getBlockList()
	{
		return _blockList;
	}
	
	/**
	 * Adds a specific player to the block list of another player.<br>
	 * This method checks if the target is already on the friends list or block list before adding.<br>
	 * It sends a confirmation message to the owner and a notification to the blocked player.
	 * @param listOwner The {@code Player} who owns the block list.
	 * @param targetId The unique identifier of the player to be blocked.
	 */
	public static void addToBlockList(Player listOwner, int targetId)
	{
		if (listOwner == null)
		{
			return;
		}
		
		if (listOwner.getFriends().contains(targetId))
		{
			listOwner.sendPacket(SystemMessageId.THIS_PLAYER_IS_ALREADY_REGISTERED_ON_YOUR_FRIENDS_LIST);
			return;
		}
		
		if (listOwner.getBlockList().getBlockList().contains(targetId))
		{
			listOwner.sendMessage("Already in ignore list.");
			return;
		}
		
		if (listOwner.getBlockList().getBlockList().size() >= 512)
		{
			listOwner.sendPacket(SystemMessageId.YOU_CAN_ADD_UP_TO_512_CHARACTERS_TO_THE_BLOCK_LIST);
			return;
		}
		
		listOwner.getBlockList().addToBlockList(targetId);
		
		final String charName = PlayerInfoTable.getInstance().getNameById(targetId);
		SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_ADDED_TO_YOUR_IGNORE_LIST);
		sm.addString(charName);
		listOwner.sendPacket(sm);
		
		final Player player = World.getPlayer(targetId);
		if (player != null)
		{
			sm = new SystemMessage(SystemMessageId.C1_HAS_ADDED_YOU_TO_THEIR_IGNORE_LIST);
			sm.addString(listOwner.getName());
			player.sendPacket(sm);
		}
	}
	
	/**
	 * Removes a specific player from the block list of another player.<br>
	 * This method checks if the target is currently blocked before removal.<br>
	 * It sends a confirmation message to the owner upon success.<br>
	 * If the target is not in the list, it sends an error message.
	 * @param listOwner The {@code Player} who owns the block list.
	 * @param targetId The unique ID of the player to remove.
	 */
	public static void removeFromBlockList(Player listOwner, int targetId)
	{
		if (listOwner == null)
		{
			return;
		}
		
		SystemMessage sm;
		if (!listOwner.getBlockList().getBlockList().contains(targetId))
		{
			sm = new SystemMessage(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
			listOwner.sendPacket(sm);
			return;
		}
		
		listOwner.getBlockList().removeFromBlockList(targetId);
		
		final String charName = PlayerInfoTable.getInstance().getNameById(targetId);
		sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_REMOVED_FROM_YOUR_IGNORE_LIST);
		sm.addString(charName);
		listOwner.sendPacket(sm);
	}
	
	/**
	 * Checks if a specific player is in the block list of another player.<br>
	 * This method returns {@code true} if the target is blocked.<br>
	 * It returns {@code false} otherwise.
	 * @param listOwner The {@code Player} who owns the block list.
	 * @param target The {@code Player} to check against the block list.
	 * @return {@code true} if the target is blocked, {@code false} otherwise.
	 */
	public static boolean isInBlockList(Player listOwner, Player target)
	{
		return listOwner.getBlockList().isInBlockList(target);
	}
	
	/**
	 * Checks if the block list for a specific player has all players blocked.<br>
	 * This method retrieves the {@code BlockList} from the provided {@code Player}.<br>
	 * It returns {@code true} if every player is currently blocked.
	 * @param listOwner The {@code Player} whose block list status is being checked.
	 * @return {@code true} if all players are blocked, otherwise {@code false}.
	 */
	public boolean isBlockAll(Player listOwner)
	{
		return listOwner.getBlockList().isBlockAll();
	}
	
	/**
	 * Updates the block all status for a specific player.<br>
	 * This method sets whether the {@code listOwner} should block everyone.
	 * @param listOwner The {@code Player} who owns the block list.
	 * @param newValue The new state to set, either {@code true} or {@code false}.
	 */
	public static void setBlockAll(Player listOwner, boolean newValue)
	{
		listOwner.getBlockList().setBlockAll(newValue);
	}
	
	/**
	 * Sends the current block list to a specific player.<br>
	 * This method creates a {@code BlockListPacket} for the owner.<br>
	 * It then transmits that packet to the {@code Player}.
	 * @param listOwner The {@code Player} who owns the block list.
	 */
	public static void sendListToOwner(Player listOwner)
	{
		listOwner.sendPacket(new BlockListPacket(listOwner.getBlockList().getBlockList()));
	}
	
	/**
	 * Checks if a specific player is blocked by another player.<br>
	 * This method works for both online and offline players.
	 * @param ownerId The unique identifier of the player who owns the block list.
	 * @param targetId The unique identifier of the player being checked.
	 * @return {@code true} if the target is blocked, {@code false} otherwise.
	 */
	public static boolean isInBlockList(int ownerId, int targetId)
	{
		final Player player = World.getPlayer(ownerId);
		if (player != null)
		{
			return isBlocked(player, targetId);
		}
		
		return OFFLINE_LIST.computeIfAbsent(ownerId, k -> loadList(k)).contains(targetId);
	}
}
