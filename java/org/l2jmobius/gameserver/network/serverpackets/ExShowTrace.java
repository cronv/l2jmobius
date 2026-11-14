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
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is used to display a trace on the client side.<br>
 * It provides information about an entity's movement or pathing.
 * @author KenM
 */
public class ExShowTrace extends ServerPacket
{
	private final List<Location> _locations = new ArrayList<>();
	
	/**
	 * Adds a new location to the internal list.<br>
	 * This method creates a {@code Location} object using the provided coordinates.
	 * @param x The X coordinate of the location.
	 * @param y The Y coordinate of the location.
	 * @param z The Z coordinate of the location.
	 */
	public void addLocation(int x, int y, int z)
	{
		_locations.add(new Location(x, y, z));
	}
	
	/**
	 * Adds a new location to the list of traces.<br>
	 * This method extracts coordinates from an {@code ILocational} object.<br>
	 * It then calls the overloaded {@code addLocation(int, int, int)} method.
	 * @param loc The {@code ILocational} object containing the coordinates to add.
	 */
	public void addLocation(ILocational loc)
	{
		addLocation(loc.getX(), loc.getY(), loc.getZ());
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_SHOW_TRACE.writeId(this, buffer);
		buffer.writeShort(0); // type broken in H5
		buffer.writeInt(0); // time broken in H5
		buffer.writeShort(_locations.size());
		for (Location loc : _locations)
		{
			buffer.writeInt(loc.getX());
			buffer.writeInt(loc.getY());
			buffer.writeInt(loc.getZ());
		}
	}
}
