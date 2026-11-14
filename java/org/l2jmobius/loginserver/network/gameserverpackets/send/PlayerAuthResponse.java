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

/**
 * This packet is sent to the client to provide a response regarding a player's authentication attempt.<br>
 * It informs the client whether the login process was successful or failed.
 * @author -Wooden-
 */
public class PlayerAuthResponse extends SimpleWritablePacket
{
	/**
	 * Creates a new {@code PlayerAuthResponse} packet.<br>
	 * This constructor initializes the response for a specific account.
	 * @param account The name of the user account.
	 * @param response The success status of the authentication. Use {@code true} for success and {@code false} for failure.
	 */
	public PlayerAuthResponse(String account, boolean response)
	{
		writeByte(0x03);
		writeString(account);
		writeByte(response ? 1 : 0);
	}
}
