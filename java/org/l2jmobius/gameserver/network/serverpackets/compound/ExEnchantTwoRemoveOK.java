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
 * This packet is sent to the client to confirm a successful removal of an enchantment.<br>
 * It informs the player that the {@code ExEnchantTwo} action was completed successfully.
 * @author UnAfraid
 */
public class ExEnchantTwoRemoveOK extends ServerPacket
{
	public static final ExEnchantTwoRemoveOK STATIC_PACKET = new ExEnchantTwoRemoveOK();
	
	/**
	 * This is a private constructor for the {@code ExEnchantTwoRemoveOK} class.<br>
	 * It is used to initialize the packet object.<br>
	 * The {@code STATIC_PACKET} instance uses this constructor.
	 */
	private ExEnchantTwoRemoveOK()
	{
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_ENCHANT_TWO_REMOVE_OK.writeId(this, buffer);
	}
}
