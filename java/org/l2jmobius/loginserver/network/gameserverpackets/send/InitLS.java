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
import org.l2jmobius.loginserver.LoginServer;

/**
 * This packet is sent to the client to initialize the {@code LoginServer} connection.<br>
 * It provides necessary configuration data for the client to start communication.
 * @author -Wooden-
 */
public class InitLS extends SimpleWritablePacket
{
	/**
	 * Initializes the login server packet with a public key.<br>
	 * This method sets the protocol revision and writes the key data.
	 * @param publickey The {@code byte[]} containing the public key.
	 */
	public InitLS(byte[] publickey)
	{
		writeByte(0x00);
		writeInt(LoginServer.PROTOCOL_REV);
		writeInt(publickey.length);
		writeBytes(publickey);
	}
}
