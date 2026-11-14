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
 * This packet is sent to the client when an attempt to retry placing an item during an enchantment fails.<br>
 * It informs the user that the action could not be completed.
 * @author Sdw
 */
public class ExEnchantRetryToPutItemFail extends ServerPacket
{
	public static final ExEnchantRetryToPutItemFail STATIC_PACKET = new ExEnchantRetryToPutItemFail();
	
	/**
	 * Creates a new instance of the {@code ExEnchantRetryToPutItemFail} packet.<br>
	 * This constructor is used to initialize the packet for sending to the client.
	 */
	private ExEnchantRetryToPutItemFail()
	{
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_ENCHANT_RETRY_TO_PUT_ITEMS_FAIL.writeId(this, buffer);
	}
}
