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
package org.l2jmobius.loginserver.network;

import org.l2jmobius.commons.network.packet.SimpleWritablePacket;

/**
 * This class defines the unique identifiers for all network packets used by the login server.<br>
 * It serves as a central registry to map packet types to their respective {@code SimpleWritablePacket} implementations.
 * @author Mobius
 */
public enum LoginServerPackets
{
	INIT(0x00),
	LOGIN_FAIL(0x01),
	ACCOUNT_KICKED(0x02),
	LOGIN_OK(0x03),
	SERVER_LIST(0x04),
	PLAY_FAIL(0x06),
	PLAY_OK(0x07),
	
	PI_AGREEMENT_CHECK(0x11),
	PI_AGREEMENT_ACK(0x12),
	GG_AUTH(0x0b),
	LOGIN_OPT_FAIL(0x0D);
	
	private final int _id;
	
	/**
	 * Creates a new instance of the {@code LoginServerPackets} enum.<br>
	 * This method assigns a unique identifier to the packet type.
	 * @param id The unique integer ID for the packet.
	 */
	LoginServerPackets(int id)
	{
		_id = id;
	}
	
	/**
	 * Writes the unique identifier of this packet to the provided packet object.<br>
	 * This method uses {@code SimpleWritablePacket#writeByte(int)} to store the ID.
	 * @param packet The {@code SimpleWritablePacket} where the ID will be written.
	 */
	public void writeId(SimpleWritablePacket packet)
	{
		packet.writeByte(_id);
	}
}
