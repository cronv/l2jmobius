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
package org.l2jmobius.gameserver.network.serverpackets.primeshop;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.enums.ExBrProductReplyType;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet handles the response for purchasing a product from the ExBR shop.<br>
 * It informs the client of the result based on the {@code ExBrProductReplyType}.
 * @author Gnacik, UnAfraid
 */
public class ExBRBuyProduct extends ServerPacket
{
	private final int _reply;
	
	/**
	 * This method creates a new {@code ExBRBuyProduct} packet.<br>
	 * It sets the internal reply ID based on the provided type.
	 * @param type The {@code ExBrProductReplyType} used to determine the response.
	 */
	public ExBRBuyProduct(ExBrProductReplyType type)
	{
		_reply = type.getId();
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_BR_BUY_PRODUCT_ACK.writeId(this, buffer);
		buffer.writeInt(_reply);
	}
}
