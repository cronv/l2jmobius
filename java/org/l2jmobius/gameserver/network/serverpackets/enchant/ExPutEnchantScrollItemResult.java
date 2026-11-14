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
package org.l2jmobius.gameserver.network.serverpackets.enchant;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet is sent to the client to provide the result of using an enchantment scroll.<br>
 * It informs the player whether the enchantment was successful or failed.
 * @author Sdw
 */
public class ExPutEnchantScrollItemResult extends ServerPacket
{
	private final int _result;
	
	/**
	 * Creates a new instance of {@code ExPutEnchantScrollItemResult}.<br>
	 * This packet carries the outcome of an enchantment scroll action.
	 * @param result The integer code representing the final result.
	 */
	public ExPutEnchantScrollItemResult(int result)
	{
		_result = result;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_PUT_ENCHANT_SCROLL_ITEM_RESULT.writeId(this, buffer);
		buffer.writeInt(_result);
	}
}
