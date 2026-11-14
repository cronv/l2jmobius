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
package org.l2jmobius.gameserver.network.loginserverpackets.receive;

import org.l2jmobius.commons.network.packet.SimpleReadablePacket;

/**
 * This packet is sent to the client when a login attempt fails.<br>
 * It informs the user that they cannot enter the game server at this time.
 */
public class LoginServerFail extends SimpleReadablePacket
{
	private static final String[] REASONS =
	{
		"None",
		"Reason: ip banned",
		"Reason: ip reserved",
		"Reason: wrong hexid",
		"Reason: id reserved",
		"Reason: no free ID",
		"Not authed",
		"Reason: already logged in"
	};
	private final int _reason;
	
	/**
	 * This constructor initializes a {@code LoginServerFail} packet.<br>
	 * It processes the decrypted data to extract the failure reason.
	 * @param decrypt The raw byte array containing the encrypted packet data.
	 */
	public LoginServerFail(byte[] decrypt)
	{
		super(decrypt);
		readByte(); // Packet id, it is already processed.
		
		_reason = readByte();
	}
	
	/**
	 * Retrieves the human-readable reason for a login failure.<br>
	 * This method maps the internal {@code _reason} integer to a descriptive string.
	 * @return A {@code String} containing the specific failure message.
	 */
	public String getReasonString()
	{
		return REASONS[_reason];
	}
	
	/**
	 * Retrieves the numerical reason code for a failed login.<br>
	 * This value corresponds to an entry in the internal {@code REASONS} array.
	 * @return The integer code representing the failure reason.
	 */
	public int getReason()
	{
		return _reason;
	}
}
