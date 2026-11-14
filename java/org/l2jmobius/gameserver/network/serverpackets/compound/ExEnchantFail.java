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
package org.l2jmobius.gameserver.network.serverpackets.compound;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet is sent to the client when an enchanting attempt fails.<br>
 * It informs the player that the enchantment process was unsuccessful.
 * @author UnAfraid
 */
public class ExEnchantFail extends ServerPacket
{
	public static final ExEnchantFail STATIC_PACKET = new ExEnchantFail(0, 0);
	
	private final int _itemOne;
	private final int _itemTwo;
	
	/**
	 * This constructor creates a new {@code ExEnchantFail} packet.<br>
	 * It stores the IDs of two items involved in a failed enchantment.
	 * @param itemOne The ID of the first item.
	 * @param itemTwo The ID of the second item.
	 */
	public ExEnchantFail(int itemOne, int itemTwo)
	{
		_itemOne = itemOne;
		_itemTwo = itemTwo;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_ENCHANT_FAIL.writeId(this, buffer);
		buffer.writeInt(_itemOne);
		buffer.writeInt(_itemTwo);
	}
}
