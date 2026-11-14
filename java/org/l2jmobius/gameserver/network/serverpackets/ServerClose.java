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
 * This packet is sent to the client to initiate a connection closure.<br>
 * It informs the {@code GameClient} that the server is closing the session.
 * @author devScarlet, mrTJO
 */
public class ServerClose extends ServerPacket
{
	public static final ServerClose STATIC_PACKET = new ServerClose();
	
	/**
	 * This is a private constructor for the {@code ServerClose} class.<br>
	 * It prevents other classes from creating new instances of this packet.<br>
	 * The {@code STATIC_PACKET} instance should be used instead.
	 */
	private ServerClose()
	{
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.SERVER_CLOSE.writeId(this, buffer);
	}
}
