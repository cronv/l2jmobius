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
package org.l2jmobius.gameserver.network.loginserverpackets.send;

import org.l2jmobius.commons.network.packet.SimpleWritablePacket;
import org.l2jmobius.gameserver.LoginServerThread.SessionKey;

/**
 * Sends the session key parts for an account to the login server.<br>
 * This packet includes the {@code account} string and various status integers.<br>
 * It accepts a {@code LoginServerThread.SessionKey} to ensure compatibility with existing callsites.
 * @author BazookaRpm
 */
public class PlayerAuthRequest extends SimpleWritablePacket
{
	// Opcode.
	private static final int OPCODE = 0x05;
	
	// Data.
	private final String _account;
	private final SessionKey _key;
	
	/**
	 * Creates a new {@code PlayerAuthRequest} for a specific player.<br>
	 * This constructor initializes the request with an account name and a session key.
	 * @param account The name of the player account.
	 * @param key The unique session key for the current connection.
	 */
	public PlayerAuthRequest(String account, SessionKey key)
	{
		_account = (account != null) ? account : "";
		_key = key;
	}
	
	/**
	 * Writes the packet data to the output stream.<br>
	 * This method serializes the {@code OPCODE}, account name, and session key IDs.<br>
	 * It handles null checks for the {@code SessionKey} object.
	 */
	@Override
	public void write()
	{
		writeByte(OPCODE);
		writeString(_account);
		writeInt((_key != null) ? _key.playOkID1 : 0);
		writeInt((_key != null) ? _key.playOkID2 : 0);
		writeInt((_key != null) ? _key.loginOkID1 : 0);
		writeInt((_key != null) ? _key.loginOkID2 : 0);
	}
}
