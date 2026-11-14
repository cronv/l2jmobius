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
package org.l2jmobius.gameserver.entity.actor.request;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import org.l2jmobius.gameserver.data.holders.SayuneEntry;
import org.l2jmobius.gameserver.data.xml.SayuneData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.enums.SayuneType;
import org.l2jmobius.gameserver.network.serverpackets.sayune.ExFlyMove;
import org.l2jmobius.gameserver.network.serverpackets.sayune.ExFlyMoveBroadcast;

/**
 * Handles requests related to the {@code Sayune} entity movement and actions.<br>
 * This class processes incoming network packets to update the state of a {@code Sayune}.
 * @author UnAfraid
 */
public class SayuneRequest extends AbstractRequest
{
	private final int _mapId;
	private boolean _isSelecting;
	private final Deque<SayuneEntry> _possibleEntries = new ArrayDeque<>();
	
	/**
	 * Creates a new {@code SayuneRequest} for a specific player and map.<br>
	 * This constructor initializes the request with data from the provided {@code mapId}.<br>
	 * It populates the possible entries based on the map's inner entries.
	 * @param player The {@code Player} who initiated the request.
	 * @param mapId The unique identifier for the map.
	 */
	public SayuneRequest(Player player, int mapId)
	{
		super(player);
		_mapId = mapId;
		
		final SayuneEntry map = SayuneData.getInstance().getMap(_mapId);
		Objects.requireNonNull(map);
		
		_possibleEntries.addAll(map.getInnerEntries());
	}
	
	/**
	 * Checks if the request is currently using a specific object.<br>
	 * This method always returns {@code false}.
	 * @param objectId The unique identifier of the object to check.
	 * @return {@code false} regardless of the input.
	 */
	@Override
	public boolean isUsing(int objectId)
	{
		return false;
	}
	
	/**
	 * Retrieves a {@code SayuneEntry} based on the provided position.<br>
	 * It checks if the entry matches the {@code pos} when selection is active.<br>
	 * If no selection is active, it removes and returns the first available entry.
	 * @param pos The position identifier to search for.
	 * @return The matching {@code SayuneEntry} or {@code null} if not found.
	 */
	private SayuneEntry findEntry(int pos)
	{
		if (_possibleEntries.isEmpty())
		{
			return null;
		}
		else if (_isSelecting)
		{
			for (SayuneEntry entry : _possibleEntries)
			{
				if (entry.getId() == pos)
				{
					return entry;
				}
			}
			
			return null;
		}
		
		return _possibleEntries.removeFirst();
	}
	
	/**
	 * Moves the player to a specific position within the current request.<br>
	 * This method updates the player's location and handles path selection logic.
	 * @param player The {@code Player} object to move.
	 * @param pos The position index to move to.
	 */
	public synchronized void move(Player player, int pos)
	{
		final SayuneEntry map = SayuneData.getInstance().getMap(_mapId);
		if ((map == null) || map.getInnerEntries().isEmpty())
		{
			player.sendMessage("MapId: " + _mapId + " was not found in the map!");
			return;
		}
		
		final SayuneEntry nextEntry = findEntry(pos);
		if (nextEntry == null)
		{
			player.removeRequest(getClass());
			return;
		}
		
		// If player was selecting unset and set his possible path.
		if (_isSelecting)
		{
			_isSelecting = false;
			
			// Set next possible path.
			if (!nextEntry.isSelector())
			{
				_possibleEntries.clear();
				_possibleEntries.addAll(nextEntry.getInnerEntries());
			}
		}
		
		final SayuneType type = (pos == 0) && nextEntry.isSelector() ? SayuneType.START_LOC : nextEntry.isSelector() ? SayuneType.MULTI_WAY_LOC : SayuneType.ONE_WAY_LOC;
		final List<SayuneEntry> locations = nextEntry.isSelector() ? nextEntry.getInnerEntries() : Arrays.asList(nextEntry);
		if (nextEntry.isSelector())
		{
			_possibleEntries.clear();
			_possibleEntries.addAll(locations);
			_isSelecting = true;
		}
		
		player.sendPacket(new ExFlyMove(player, type, _mapId, locations));
		
		final SayuneEntry activeEntry = locations.get(0);
		World.broadcastToVisiblePlayersInRange(player, new ExFlyMoveBroadcast(player, type, map.getId(), activeEntry), 1000);
		player.setXYZ(activeEntry);
	}
	
	/**
	 * Handles the logic when a player logs out while using this request.<br>
	 * It resets the player's position to a valid location on the map.<br>
	 * This ensures the player does not get stuck in an invalid state.
	 */
	public void onLogout()
	{
		final SayuneEntry map = SayuneData.getInstance().getMap(_mapId);
		if ((map != null) && !map.getInnerEntries().isEmpty())
		{
			final SayuneEntry nextEntry = findEntry(0);
			if (_isSelecting || ((nextEntry != null) && nextEntry.isSelector()))
			{
				// If player is on selector or next entry is selector go back to first entry.
				getPlayer().setXYZ(map);
			}
			else
			{
				// Try to find last entry to set player, if not set him to first entry.
				final SayuneEntry lastEntry = map.getInnerEntries().get(map.getInnerEntries().size() - 1);
				if (lastEntry != null)
				{
					getPlayer().setXYZ(lastEntry);
				}
				else
				{
					getPlayer().setXYZ(map);
				}
			}
		}
	}
}
