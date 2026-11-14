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
package org.l2jmobius.loginserver.network.gameserverpackets.send;

import org.l2jmobius.commons.network.packet.SimpleWritablePacket;
import org.l2jmobius.loginserver.data.GameServerTable;

/**
 * This packet handles the response sent to a client after an authentication attempt.<br>
 * It informs the client whether the login was successful or failed.
 * @author -Wooden-
 */
public class AuthResponse extends SimpleWritablePacket
{
	/**
	 * Creates a new {@code AuthResponse} packet.<br>
	 * This method initializes the response with specific server details.
	 * @param serverId The unique identifier for the game server.
	 */
	public AuthResponse(int serverId)
	{
		writeByte(0x02);
		writeByte(serverId);
		writeString(GameServerTable.getInstance().getServerNameById(serverId));
	}
}
