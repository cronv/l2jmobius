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
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet handles the display of territory information to the client.<br>
 * Note: There is a known issue where this packet cannot be removed unless the game client is restarted.
 * @author UnAfraid
 */
public class ExShowTerritory extends ServerPacket
{
	private final int _minZ;
	private final int _maxZ;
	private final List<ILocational> _vertices = new ArrayList<>();
	
	/**
	 * Creates a new {@code ExShowTerritory} instance.<br>
	 * This constructor sets the vertical boundaries for the territory.
	 * @param minZ The minimum Z coordinate.
	 * @param maxZ The maximum Z coordinate.
	 */
	public ExShowTerritory(int minZ, int maxZ)
	{
		_minZ = minZ;
		_maxZ = maxZ;
	}
	
	/**
	 * Adds a new vertex to the territory.<br>
	 * This method stores the provided {@code ILocational} object in the internal list.
	 * @param loc The location to be added as a vertex.
	 */
	public void addVertice(ILocational loc)
	{
		_vertices.add(loc);
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_SHOW_TERRITORY.writeId(this, buffer);
		buffer.writeInt(_vertices.size());
		buffer.writeInt(_minZ);
		buffer.writeInt(_maxZ);
		for (ILocational loc : _vertices)
		{
			buffer.writeInt(loc.getX());
			buffer.writeInt(loc.getY());
		}
	}
}
