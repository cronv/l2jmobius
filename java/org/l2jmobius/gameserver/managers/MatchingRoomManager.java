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
package org.l2jmobius.gameserver.managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.groups.matching.MatchingRoom;
import org.l2jmobius.gameserver.entity.groups.matching.MatchingRoomType;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchingRoomLevelType;

/**
 * Manages the lifecycle and organization of {@code MatchingRoom} instances.<br>
 * It handles the creation, tracking, and retrieval of various matching rooms for players.
 * @author Sdw, BazookaRpm
 */
public class MatchingRoomManager
{
	private final Set<Player> _waitingList = ConcurrentHashMap.newKeySet();
	
	private static final Map<MatchingRoomType, Map<Integer, MatchingRoom>> _rooms = new ConcurrentHashMap<>(2);
	
	private final AtomicInteger _id = new AtomicInteger(0);
	
	/**
	 * Adds a {@code Player} to the global waiting list.
	 * @param player The {@code Player} object to add to the list.
	 */
	public void addToWaitingList(Player player)
	{
		_waitingList.add(player);
	}
	
	/**
	 * Removes a specific {@code Player} from the current waiting list.<br>
	 * This method updates the internal set of players who are looking for a match.
	 * @param player The {@code Player} object to be removed.
	 */
	public void removeFromWaitingList(Player player)
	{
		getPlayerInWaitingList().remove(player);
	}
	
	/**
	 * Retrieves all players currently in the waiting list.
	 * @return A {@code Set} of {@code Player} objects.
	 */
	public Set<Player> getPlayerInWaitingList()
	{
		return _waitingList;
	}
	
	/**
	 * Retrieves a list of players from the waiting list based on specific filters.<br>
	 * The method checks for level range, character classes, and name queries.
	 * @param minLevel The minimum level required to be included in the results.
	 * @param maxLevel The maximum level allowed to be included in the results.
	 * @param classIds A list of {@code PlayerClass} types to filter by; if {@code null}, all classes are included.
	 * @param query A string to search for within player names; if {@code null} or empty, no name filtering is applied.
	 * @return A {@code List} of {@code Player} objects that match all the provided criteria.
	 */
	public List<Player> getPlayerInWaitingList(int minLevel, int maxLevel, List<PlayerClass> classIds, String query)
	{
		final List<Player> players = new ArrayList<>();
		for (Player player : _waitingList)
		{
			if ((player != null) && (player.getLevel() >= minLevel) && (player.getLevel() <= maxLevel) && ((classIds == null) || classIds.contains(player.getPlayerClass())) && ((query == null) || query.isEmpty() || player.getName().toLowerCase().contains(query)))
			{
				players.add(player);
			}
		}
		
		return players;
	}
	
	/**
	 * Adds a new {@code MatchingRoom} to the manager.<br>
	 * This method assigns a unique ID to the room.<br>
	 * It organizes the room by its specific type.
	 * @param room The {@code MatchingRoom} object to be added.
	 * @return The unique integer ID assigned to the new room.
	 */
	public int addMatchingRoom(MatchingRoom room)
	{
		final int roomId = _id.incrementAndGet();
		_rooms.computeIfAbsent(room.getRoomType(), _ -> new ConcurrentHashMap<>()).put(roomId, room);
		return roomId;
	}
	
	/**
	 * Removes a specific {@code MatchingRoom} from the active rooms collection.<br>
	 * This method identifies the room by its type and unique ID.<br>
	 * It ensures that the room is no longer tracked by the manager.
	 * @param room The {@code MatchingRoom} object to be removed.
	 */
	public void removeMatchingRoom(MatchingRoom room)
	{
		_rooms.getOrDefault(room.getRoomType(), Collections.emptyMap()).remove(room.getId());
	}
	
	/**
	 * Retrieves all active party matching rooms.<br>
	 * This method returns a map where the key is the room ID.
	 * @return A {@code Map} containing all {@code MatchingRoom} objects for the party type.
	 */
	public Map<Integer, MatchingRoom> getPartyMathchingRooms()
	{
		return _rooms.get(MatchingRoomType.PARTY);
	}
	
	/**
	 * Retrieves a list of party matching rooms based on specific filters.<br>
	 * It checks the location, level type, and the requestor's level.
	 * @param location The map location ID to filter by.
	 * @param type The {@code PartyMatchingRoomLevelType} category.
	 * @param requestorLevel The character level of the player making the request.
	 * @return A {@code List} of {@code MatchingRoom} objects that meet the criteria.
	 */
	public List<MatchingRoom> getPartyMathchingRooms(int location, PartyMatchingRoomLevelType type, int requestorLevel)
	{
		final List<MatchingRoom> result = new ArrayList<>();
		final Map<Integer, MatchingRoom> rooms = _rooms.get(MatchingRoomType.PARTY);
		if (rooms != null)
		{
			for (MatchingRoom room : rooms.values())
			{
				if (((location < 0) || (room.getLocation() == location)) //
					&& ((type == PartyMatchingRoomLevelType.ALL) || ((room.getMinLevel() <= requestorLevel) && (room.getMaxLevel() >= requestorLevel))))
				{
					result.add(room);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves all active command channel matching rooms.<br>
	 * This method returns a map where the key is the room ID.
	 * @return A {@code Map} containing the IDs and {@code MatchingRoom} objects for command channels.
	 */
	public Map<Integer, MatchingRoom> getCCMathchingRooms()
	{
		return _rooms.get(MatchingRoomType.COMMAND_CHANNEL);
	}
	
	/**
	 * Retrieves a list of command channel matching rooms.<br>
	 * The results are filtered by the specified location and level range.
	 * @param location The map location ID to filter by.
	 * @param level The player level to check against room limits.
	 * @return A {@code List} of {@code MatchingRoom} objects that match the criteria.
	 */
	public List<MatchingRoom> getCCMathchingRooms(int location, int level)
	{
		final List<MatchingRoom> result = new ArrayList<>();
		final Map<Integer, MatchingRoom> rooms = _rooms.get(MatchingRoomType.COMMAND_CHANNEL);
		if (rooms != null)
		{
			for (MatchingRoom room : rooms.values())
			{
				if ((room.getLocation() == location) //
					&& ((room.getMinLevel() <= level) && (room.getMaxLevel() >= level)))
				{
					result.add(room);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves a specific {@code MatchingRoom} for the Command Channel.<br>
	 * It looks up the room using the provided {@code roomId}.<br>
	 * Returns {@code null} if no matching room is found.
	 * @param roomId The unique identifier of the room to retrieve.
	 * @return The {@code MatchingRoom} object or {@code null}.
	 */
	public MatchingRoom getCCMatchingRoom(int roomId)
	{
		return _rooms.getOrDefault(MatchingRoomType.COMMAND_CHANNEL, Collections.emptyMap()).get(roomId);
	}
	
	/**
	 * Finds a party matching room based on location and level.<br>
	 * It searches through all available rooms of type {@code MatchingRoomType.PARTY}.<br>
	 * The method returns the first room that matches both criteria.
	 * @param location The specific map or area ID to search in.
	 * @param level The character level used to filter the matching range.
	 * @return The matching {@code MatchingRoom} object, or {@code null} if no match is found.
	 */
	public MatchingRoom getPartyMathchingRoom(int location, int level)
	{
		final Map<Integer, MatchingRoom> rooms = _rooms.get(MatchingRoomType.PARTY);
		if (rooms != null)
		{
			for (MatchingRoom room : rooms.values())
			{
				// A negative location stands for any, and a full room cannot take the player being matched.
				if (((location < 0) || (room.getLocation() == location)) //
					&& (room.getMinLevel() <= level) && (room.getMaxLevel() >= level) //
					&& (room.getMembersCount() < room.getMaxMembers()))
				{
					return room;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves a specific party matching room by its unique identifier.<br>
	 * This method looks up the room within the {@code MatchingRoomType.PARTY} category.<br>
	 * It returns {@code null} if no room exists for the given ID.
	 * @param roomId The unique ID of the matching room to find.
	 * @return The {@code MatchingRoom} object, or {@code null} if not found.
	 */
	public MatchingRoom getPartyMathchingRoom(int roomId)
	{
		return _rooms.getOrDefault(MatchingRoomType.PARTY, Collections.emptyMap()).get(roomId);
	}
	
	/**
	 * Retrieves the singleton instance of {@code MatchingRoomManager}.<br>
	 * This method provides a global access point to the manager.
	 * @return The single instance of {@code MatchingRoomManager}.
	 */
	public static MatchingRoomManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final MatchingRoomManager INSTANCE = new MatchingRoomManager();
	}
}
