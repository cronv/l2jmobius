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
package org.l2jmobius.gameserver.entity.groups.matching;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.data.sql.PartyMatchingHistoryTable;
import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.MatchingRoomManager;
import org.l2jmobius.gameserver.network.enums.UserInfoType;

/**
 * Represents a container for players who are currently in the process of being matched.<br>
 * This class manages the logic for grouping {@code Player} entities based on specific criteria.<br>
 * It serves as the base structure for different types of matchmaking rooms.
 * @author Sdw
 */
public abstract class MatchingRoom
{
	private final int _id;
	private String _title;
	private int _loot;
	private int _minLevel;
	private int _maxLevel;
	private int _maxCount;
	private Set<Player> _members;
	private Player _leader;
	
	/**
	 * Creates a new {@code MatchingRoom} instance.<br>
	 * This constructor initializes the room properties and registers it with the manager.<br>
	 * It also automatically adds the leader to the member list.
	 * @param title The display name of the matching room.
	 * @param loot The type of loot available in this room.
	 * @param minLevel The minimum level required to join.
	 * @param maxLevel The maximum level allowed in the room.
	 * @param maxmem The maximum number of members allowed.
	 * @param leader The {@code Player} who will lead the room.
	 */
	public MatchingRoom(String title, int loot, int minLevel, int maxLevel, int maxmem, Player leader)
	{
		_id = MatchingRoomManager.getInstance().addMatchingRoom(this);
		_title = title;
		_loot = loot;
		_minLevel = minLevel;
		_maxLevel = maxLevel;
		_maxCount = maxmem;
		_leader = leader;
		addMember(leader);
		onRoomCreation(leader);
		PartyMatchingHistoryTable.getInstance().addRoomHistory(title, leader.getName());
	}
	
	/**
	 * Retrieves the set of all players currently in this matching room.<br>
	 * This method ensures that the internal member collection is initialized before returning it.
	 * @return a {@code Set} containing all {@code Player} objects in the room.
	 */
	public Set<Player> getMembers()
	{
		if (_members == null)
		{
			synchronized (this)
			{
				if (_members == null)
				{
					_members = ConcurrentHashMap.newKeySet(1);
				}
			}
		}
		
		return _members;
	}
	
	/**
	 * Adds a {@code Player} to the current matching room.<br>
	 * This method checks if the player meets the level requirements and if there is space available.<br>
	 * If valid, it updates the player's status and removes them from the waiting list.
	 * @param player The {@code Player} object to be added to the room.
	 */
	public void addMember(Player player)
	{
		final int playerLevel = player.getLevel();
		if ((playerLevel < _minLevel) || (playerLevel > _maxLevel) || ((_members != null) && (_members.size() >= _maxCount)))
		{
			notifyInvalidCondition(player);
			return;
		}
		
		getMembers().add(player);
		MatchingRoomManager.getInstance().removeFromWaitingList(player);
		notifyNewMember(player);
		player.setMatchingRoom(this);
		player.broadcastUserInfo(UserInfoType.CLAN);
	}
	
	/**
	 * Removes a {@code Player} from the current matching room.<br>
	 * This method handles leader reassignment if the removed player was the leader.<br>
	 * It also returns the player to the waiting list.
	 * @param player The {@code Player} to be removed from the room.
	 * @param kicked A boolean indicating if the player was forcibly kicked or left voluntarily.
	 */
	public void deleteMember(Player player, boolean kicked)
	{
		boolean leaderChanged = false;
		// The leader is a member like any other, so the room is only empty once the leaving player is out of the set.
		getMembers().remove(player);
		if (player == _leader)
		{
			if (getMembers().isEmpty())
			{
				MatchingRoomManager.getInstance().removeMatchingRoom(this);
			}
			else
			{
				_leader = getMembers().iterator().next();
				leaderChanged = true;
			}
		}
		
		player.setMatchingRoom(null);
		player.broadcastUserInfo(UserInfoType.CLAN);
		MatchingRoomManager.getInstance().addToWaitingList(player);
		
		notifyRemovedMember(player, kicked, leaderChanged);
	}
	
	/**
	 * Retrieves the unique identifier for this {@code MatchingRoom}.<br>
	 * This ID is used to distinguish between different rooms.
	 * @return The unique integer ID of the room.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Returns the type of loot associated with this matching room.<br>
	 * This value is used to determine what rewards players receive.
	 * @return The {@code int} value representing the loot type.
	 */
	public int getLootType()
	{
		return _loot;
	}
	
	/**
	 * Returns the minimum level required to join this matching room.<br>
	 * This value is used to filter eligible players.
	 * @return the minimum level as an {@code int}.
	 */
	public int getMinLevel()
	{
		return _minLevel;
	}
	
	/**
	 * Returns the maximum level allowed for this matching room.<br>
	 * This value is used to filter eligible players.
	 * @return The maximum level as an {@code int}.
	 */
	public int getMaxLevel()
	{
		return _maxLevel;
	}
	
	/**
	 * Retrieves the bounding box ID for the room location.<br>
	 * This method uses the current leader's position to find the area.
	 * @return The {@code int} identifier of the map region.
	 */
	public int getLocation()
	{
		return MapRegionData.getInstance().getBBs(_leader.getLocation());
	}
	
	/**
	 * Returns the total number of players currently in this matching room.<br>
	 * If there are no members, it returns {@code 0}.
	 * @return The current count of members as an {@code int}.
	 */
	public int getMembersCount()
	{
		return _members == null ? 0 : _members.size();
	}
	
	/**
	 * Retrieves the maximum number of members allowed in this room.<br>
	 * This value is stored in the {@code _maxCount} field.
	 * @return The maximum member limit as an {@code int}.
	 */
	public int getMaxMembers()
	{
		return _maxCount;
	}
	
	/**
	 * Retrieves the name of the matching room.<br>
	 * This returns the {@code String} value stored in the private field.
	 * @return The title of the matching room.
	 */
	public String getTitle()
	{
		return _title;
	}
	
	/**
	 * Retrieves the leader of this matching room.<br>
	 * Returns the {@code Player} who is currently leading the group.
	 * @return the {@code Player} object representing the leader, or {@code null} if no leader exists.
	 */
	public Player getLeader()
	{
		return _leader;
	}
	
	/**
	 * Checks if a specific {@code Player} is the leader of this room.<br>
	 * It compares the provided {@code Player} with the current leader.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if the player is the leader, otherwise {@code false}.
	 */
	public boolean isLeader(Player player)
	{
		return player == _leader;
	}
	
	/**
	 * Sets the minimum level required to join this matching room.<br>
	 * This value is used to filter eligible players.
	 * @param minLevel The minimum character level allowed.
	 */
	public void setMinLevel(int minLevel)
	{
		_minLevel = minLevel;
	}
	
	/**
	 * Sets the maximum level allowed for this matching room.<br>
	 * This value determines the upper limit for players joining.
	 * @param maxLevel The new maximum level to set.
	 */
	public void setMaxLevel(int maxLevel)
	{
		_maxLevel = maxLevel;
	}
	
	/**
	 * Sets the loot type for this matching room.<br>
	 * This updates the internal {@code _loot} value.
	 * @param loot The new loot type identifier.
	 */
	public void setLootType(int loot)
	{
		_loot = loot;
	}
	
	/**
	 * Sets the maximum number of members allowed in this room.<br>
	 * This updates the {@code _maxCount} field.
	 * @param maxCount The new limit for the total number of members.
	 */
	public void setMaxMembers(int maxCount)
	{
		_maxCount = maxCount;
	}
	
	/**
	 * Sets the display name for this matching room.<br>
	 * This updates the {@code _title} field.
	 * @param title The new name to assign to the room.
	 */
	public void setTitle(String title)
	{
		_title = title;
	}
	
	protected abstract void onRoomCreation(Player player);
	
	protected abstract void notifyInvalidCondition(Player player);
	
	protected abstract void notifyNewMember(Player player);
	
	protected abstract void notifyRemovedMember(Player player, boolean kicked, boolean leaderChanged);
	
	public abstract void disbandRoom();
	
	public abstract MatchingRoomType getRoomType();
	
	public abstract MatchingMemberType getMemberType(Player player);
}
