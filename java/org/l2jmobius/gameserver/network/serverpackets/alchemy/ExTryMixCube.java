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
package org.l2jmobius.gameserver.network.serverpackets.alchemy;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.enums.TryMixCubeType;
import org.l2jmobius.gameserver.network.holders.AlchemyResult;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet handles the result of an extended Mix Cube attempt.<br>
 * It informs the client about the outcome of the alchemy process.
 * @author Sdw
 */
public class ExTryMixCube extends ServerPacket
{
	private final TryMixCubeType _type;
	private final List<AlchemyResult> _items = new ArrayList<>();
	
	/**
	 * Creates a new {@code ExTryMixCube} packet.<br>
	 * This constructor initializes the packet with a specific cube type.
	 * @param type The {@code TryMixCubeType} to assign to this packet.
	 */
	public ExTryMixCube(TryMixCubeType type)
	{
		_type = type;
	}
	
	/**
	 * Adds a new result to the list of items.<br>
	 * This method updates the internal collection of {@code AlchemyResult} objects.
	 * @param item The {@code AlchemyResult} object to be added.
	 */
	public void addItem(AlchemyResult item)
	{
		_items.add(item);
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_TRY_MIX_CUBE.writeId(this, buffer);
		buffer.writeByte(_type.ordinal());
		buffer.writeInt(_items.size());
		for (AlchemyResult holder : _items)
		{
			buffer.writeByte(holder.getType().ordinal());
			buffer.writeInt(holder.getId());
			buffer.writeLong(holder.getCount());
		}
	}
}
