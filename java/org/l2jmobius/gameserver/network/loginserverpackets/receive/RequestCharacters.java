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
 * Handles the request from a client to retrieve a list of available characters.<br>
 * This packet is processed by the login server to return character data for a specific account.
 * @author mrTJO, mochitto
 */
public class RequestCharacters extends SimpleReadablePacket
{
	private final String _account;
	
	/**
	 * This constructor initializes a new {@code RequestCharacters} object.<br>
	 * It takes raw decrypted data and parses the account information.
	 * @param decrypt The raw byte array containing the decrypted packet data.
	 */
	public RequestCharacters(byte[] decrypt)
	{
		super(decrypt);
		readByte(); // Packet id, it is already processed.
		
		_account = readString();
	}
	
	/**
	 * Retrieves the account name associated with this request.<br>
	 * This method returns the value stored in the {@code _account} field.
	 * @return The account name as a {@code String}.
	 */
	public String getAccount()
	{
		return _account;
	}
}
