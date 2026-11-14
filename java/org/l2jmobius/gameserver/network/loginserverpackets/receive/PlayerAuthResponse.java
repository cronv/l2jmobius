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
 * This class handles the response packet received from the login server regarding player authentication.<br>
 * It contains the necessary data to confirm if a player's credentials have been successfully validated.
 * @author -Wooden-
 */
public class PlayerAuthResponse extends SimpleReadablePacket
{
	private final String _account;
	private final boolean _authed;
	
	/**
	 * Creates a new {@code PlayerAuthResponse} from decrypted data.<br>
	 * This constructor reads the account name and authentication status.
	 * @param decrypt The raw byte array received from the network.
	 */
	public PlayerAuthResponse(byte[] decrypt)
	{
		super(decrypt);
		readByte(); // Packet id, it is already processed.
		
		_account = readString();
		_authed = readByte() != 0;
	}
	
	/**
	 * Retrieves the account name from the packet.<br>
	 * This method returns the string value stored in {@code _account}.
	 * @return The account name as a {@code String}.
	 */
	public String getAccount()
	{
		return _account;
	}
	
	/**
	 * Checks if the player has been successfully authenticated.<br>
	 * This method returns the authentication status of the current packet.
	 * @return {@code true} if the player is authenticated, {@code false} otherwise.
	 */
	public boolean isAuthed()
	{
		return _authed;
	}
}
