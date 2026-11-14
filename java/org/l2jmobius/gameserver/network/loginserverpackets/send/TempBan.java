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

/**
 * Sends a packet to the login server to issue a temporary ban for an account and/or IP.<br>
 * The ban is set to expire at {@code now + minutes * 60000}.
 * @author BazookaRpm
 */
public class TempBan extends SimpleWritablePacket
{
	// Opcode.
	private static final int OPCODE = 0x0A;
	private static final long MINUTE_MILLIS = 60000L;
	
	// Data.
	private final String _accountName;
	private final String _ip;
	private final long _minutes;
	
	/**
	 * Creates a new {@code TempBan} packet.<br>
	 * This packet handles temporary bans for specific accounts and IP addresses.
	 * @param accountName The name of the account to ban.
	 * @param ip The IP address associated with the ban.
	 * @param time The duration of the ban in minutes.
	 */
	public TempBan(String accountName, String ip, long time)
	{
		_accountName = (accountName != null) ? accountName : "";
		_ip = (ip != null) ? ip : "";
		_minutes = (time > 0L) ? time : 0L;
	}
	
	/**
	 * Writes the packet data to the network stream.<br>
	 * This method serializes the account name, IP address, and expiration time.<br>
	 * It also includes the {@code OPCODE} and a reserved byte.
	 */
	@Override
	public void write()
	{
		writeByte(OPCODE);
		writeString(_accountName);
		writeString(_ip);
		writeLong(System.currentTimeMillis() + (_minutes * MINUTE_MILLIS));
		writeByte(0x00);
	}
}
