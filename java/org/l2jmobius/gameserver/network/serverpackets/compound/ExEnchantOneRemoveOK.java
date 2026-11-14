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
 * This packet is sent to the client to confirm that an enchantment has been successfully removed.<br>
 * It serves as a response for the {@code ExEnchantOneRemove} action.
 * @author UnAfraid
 */
public class ExEnchantOneRemoveOK extends ServerPacket
{
	public static final ExEnchantOneRemoveOK STATIC_PACKET = new ExEnchantOneRemoveOK();
	
	/**
	 * This is a private constructor for the {@code ExEnchantOneRemoveOK} class.<br>
	 * It is used to instantiate the {@code STATIC_PACKET} instance.
	 */
	private ExEnchantOneRemoveOK()
	{
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_ENCHANT_ONE_REMOVE_OK.writeId(this, buffer);
	}
}
