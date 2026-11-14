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
package org.l2jmobius.gameserver.network.serverpackets.adenadistribution;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet initiates the process of dividing {@code Adena} for a specific event.<br>
 * It is sent to the client to signal the start of the distribution sequence.
 * @author Sdw
 */
public class ExDivideAdenaStart extends ServerPacket
{
	public static final ExDivideAdenaStart STATIC_PACKET = new ExDivideAdenaStart();
	
	/**
	 * Private constructor for the {@code ExDivideAdenaStart} class.<br>
	 * This prevents other classes from creating new instances of this packet.<br>
	 * Use {@code ExDivideAdenaStart.STATIC_PACKET} to access the singleton instance.
	 */
	private ExDivideAdenaStart()
	{
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_DIVIDE_ADENA_START.writeId(this, buffer);
	}
}
