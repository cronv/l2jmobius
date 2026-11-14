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
 * This packet is sent to the client to request a list of available characters.<br>
 * It allows the client to retrieve and display character data for the user.
 * @author mrTJO
 */
public class RequestCharacters extends SimpleWritablePacket
{
	/**
	 * Creates a new {@code RequestCharacters} packet.<br>
	 * This packet is used to request character information for a specific account.
	 * @param account The unique identifier of the user account.
	 */
	public RequestCharacters(String account)
	{
		writeByte(0x05);
		writeString(account);
	}
}
