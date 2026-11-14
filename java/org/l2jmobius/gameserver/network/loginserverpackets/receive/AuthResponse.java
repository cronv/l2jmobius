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
package org.l2jmobius.gameserver.network.loginserverpackets.receive;

import org.l2jmobius.commons.network.packet.SimpleReadablePacket;

/**
 * This class handles the response packet received from the login server.<br>
 * It contains information regarding the success or failure of an authentication attempt.
 * @author -Wooden-
 */
public class AuthResponse extends SimpleReadablePacket
{
	private final int _serverId;
	private final String _serverName;
	
	/**
	 * Creates a new {@code AuthResponse} from decrypted data.<br>
	 * This constructor reads the server ID and server name from the packet.
	 * @param decrypt The raw byte array received from the network.
	 */
	public AuthResponse(byte[] decrypt)
	{
		super(decrypt);
		readByte(); // Packet id, it is already processed.
		
		_serverId = readByte();
		_serverName = readString();
	}
	
	/**
	 * Retrieves the unique identifier for the server.<br>
	 * This value is used to identify which game server the user is connecting to.
	 * @return The {@code int} ID of the server.
	 */
	public int getServerId()
	{
		return _serverId;
	}
	
	/**
	 * Retrieves the name of the server.<br>
	 * This value is provided by the {@code AuthResponse} packet.
	 * @return The name of the server as a {@code String}.
	 */
	public String getServerName()
	{
		return _serverName;
	}
}
