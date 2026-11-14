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
package org.l2jmobius.loginserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.loginserver.network.LoginClient;

/**
 * This packet handles the initial handshake between the client and the server.<br>
 * It contains the session ID, protocol revision, RSA public key, and the Blowfish key.
 */
public class Init extends LoginServerPacket
{
	private final int _sessionId;
	
	private final byte[] _publicKey;
	private final byte[] _blowfishKey;
	
	/**
	 * Initializes a new {@code Init} packet using data from the provided client.<br>
	 * This constructor extracts the public key, blowfish key, and session ID automatically.
	 * @param client The {@code LoginClient} instance used to retrieve the required connection data.
	 */
	public Init(LoginClient client)
	{
		this(client.getScrambledModulus(), client.getBlowfishKey(), client.getSessionId());
	}
	
	/**
	 * Initializes a new {@code Init} packet.<br>
	 * This constructor sets the required security keys and session identifier.
	 * @param publickey The RSA public key as a {@code byte[]} array.
	 * @param blowfishkey The Blowfish encryption key as a {@code byte[]} array.
	 * @param sessionId The unique identifier for the current session.
	 */
	public Init(byte[] publickey, byte[] blowfishkey, int sessionId)
	{
		_sessionId = sessionId;
		_publicKey = publickey;
		_blowfishKey = blowfishkey;
	}
	
	@Override
	protected void writeImpl(LoginClient client, WriteBuffer buffer)
	{
		buffer.writeByte(0x00); // Init packet id.
		
		buffer.writeInt(_sessionId); // Session id.
		buffer.writeInt(0x0000c621); // Protocol revision.
		
		buffer.writeBytes(_publicKey); // RSA Public Key.
		
		// GG related.
		buffer.writeInt(0x29DD954E);
		buffer.writeInt(0x77C39CFC);
		buffer.writeInt(0x97ADB620);
		buffer.writeInt(0x07BDE0F7);
		
		buffer.writeBytes(_blowfishKey); // BlowFish key.
		buffer.writeByte(0); // Null termination.
	}
}
