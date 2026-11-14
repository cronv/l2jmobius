/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.loginserver.network.gameserverpackets.receive;

import org.l2jmobius.commons.network.packet.SimpleReadablePacket;
import org.l2jmobius.loginserver.GameServerThread;

/**
 * Handles a logout notification received from a connected game server.<br>
 * This class extracts the account name and removes it from the {@code GameServerThread} state.<br>
 * It performs basic validation before updating the login server records.
 * @author BazookaRpm
 */
public class PlayerLogout extends SimpleReadablePacket
{
	/**
	 * Constructs a new {@code PlayerLogout} packet.<br>
	 * This constructor validates the input data and initializes the logout process.<br>
	 * It removes the specified account from the provided game server thread.
	 * @param decrypt The decrypted byte array containing the packet data.
	 * @param server The {@code GameServerThread} where the player is logging out from.
	 */
	public PlayerLogout(byte[] decrypt, GameServerThread server)
	{
		super(decrypt);
		
		if (decrypt == null)
		{
			throw new IllegalArgumentException("PlayerLogout packet data cannot be null.");
		}
		
		if (server == null)
		{
			throw new IllegalArgumentException("PlayerLogout target GameServerThread cannot be null.");
		}
		
		if (getRemainingLength() < 3)
		{
			throw new IllegalArgumentException("PlayerLogout packet is too short. Length: " + getLength() + ".");
		}
		
		readByte(); // Packet id, it is already processed.
		
		final String accountName = readString();
		if (!accountName.isEmpty())
		{
			server.removeAccountOnGameServer(accountName);
		}
	}
}
