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

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet handles requests from the client to enchant an item.<br>
 * It processes the enchantment logic and sends the result back to the player.
 * @author nBd
 */
public class RequestEnchant extends ServerPacket
{
	private final int _result;
	
	/**
	 * Creates a new {@code RequestEnchant} packet.<br>
	 * This constructor sets the enchantment result value.
	 * @param result The integer result of the enchantment.
	 */
	public RequestEnchant(int result)
	{
		_result = result;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_PRIVATE_STORE_WHOLE_MSG.writeId(this, buffer);
		buffer.writeInt(_result);
	}
}
