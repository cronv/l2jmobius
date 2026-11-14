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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.RadarControl;

/**
 * Manages the radar state and visibility for a {@code Player}.<br>
 * It handles how entities are displayed on the player's radar interface.
 * @author dalrond
 */
public class Radar
{
	private final Player _player;
	private final Set<RadarMarker> _markers = ConcurrentHashMap.newKeySet();
	
	/**
	 * Creates a new {@code Radar} instance for a specific player.<br>
	 * This object manages the radar markers associated with that player.
	 * @param player The {@code Player} who will own this radar.
	 */
	public Radar(Player player)
	{
		_player = player;
	}
	
	// Add a marker to player's radar.
	/**
	 * Adds a new marker to the player's radar.<br>
	 * This method creates a {@code RadarMarker} at the specified coordinates.<br>
	 * It also sends update packets to the {@code Player}.
	 * @param x The x-coordinate of the marker.
	 * @param y The y-coordinate of the marker.
	 * @param z The z-coordinate of the marker.
	 */
	public void addMarker(int x, int y, int z)
	{
		final RadarMarker newMarker = new RadarMarker(x, y, z);
		_markers.add(newMarker);
		_player.sendPacket(new RadarControl(2, 2, x, y, z));
		_player.sendPacket(new RadarControl(0, 1, x, y, z));
	}
	
	// Remove a marker from player's radar.
	/**
	 * Removes a specific marker from the player's radar.<br>
	 * This method checks for a marker at the given coordinates.<br>
	 * It also sends a {@code RadarControl} packet to update the client.
	 * @param x The x-coordinate of the marker to remove.
	 * @param y The y-coordinate of the marker to remove.
	 * @param z The z-coordinate of the marker to remove.
	 */
	public void removeMarker(int x, int y, int z)
	{
		for (RadarMarker rm : _markers)
		{
			if ((rm._x == x) && (rm._y == y) && (rm._z == z))
			{
				_markers.remove(rm);
			}
		}
		
		_player.sendPacket(new RadarControl(1, 1, x, y, z));
	}
	
	/**
	 * Removes all markers from the current radar.<br>
	 * This method sends a {@code RadarControl} packet to the {@code Player} for each marker removed.<br>
	 * It then clears the internal set of markers.
	 */
	public void removeAllMarkers()
	{
		for (RadarMarker tempMarker : _markers)
		{
			_player.sendPacket(new RadarControl(2, 2, tempMarker._x, tempMarker._y, tempMarker._z));
		}
		
		_markers.clear();
	}
	
	/**
	 * This method synchronizes the radar markers with the client.<br>
	 * It sends a {@code RadarControl} packet for every marker in the {@code _markers} set.<br>
	 * The player's current coordinates are also sent to update their position.
	 */
	public void loadMarkers()
	{
		_player.sendPacket(new RadarControl(2, 2, _player.getX(), _player.getY(), _player.getZ()));
		for (RadarMarker tempMarker : _markers)
		{
			_player.sendPacket(new RadarControl(0, 1, tempMarker._x, tempMarker._y, tempMarker._z));
		}
	}
	
	public static class RadarMarker
	{
		// Simple class to model radar points.
		public int _type;
		public int _x;
		public int _y;
		public int _z;
		
		public RadarMarker(int type, int x, int y, int z)
		{
			_type = type;
			_x = x;
			_y = y;
			_z = z;
		}
		
		public RadarMarker(int x, int y, int z)
		{
			_type = 1;
			_x = x;
			_y = y;
			_z = z;
		}
		
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = (prime * result) + _type;
			result = (prime * result) + _x;
			result = (prime * result) + _y;
			result = (prime * result) + _z;
			return result;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
			{
				return true;
			}
			
			if (!(obj instanceof RadarMarker))
			{
				return false;
			}
			
			final RadarMarker other = (RadarMarker) obj;
			return (_type == other._type) && (_x == other._x) && (_y == other._y) && (_z == other._z);
		}
	}
}
