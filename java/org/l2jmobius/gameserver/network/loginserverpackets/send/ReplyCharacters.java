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

import java.util.Collections;
import java.util.List;

import org.l2jmobius.commons.network.packet.SimpleWritablePacket;

/**
 * Reports the character count and pending delete timers for a specific account.<br>
 * Each pending delete time is serialized as epoch milliseconds.
 * @author BazookaRpm
 */
public class ReplyCharacters extends SimpleWritablePacket
{
	// Opcode.
	private static final int OPCODE = 0x08;
	
	// Data.
	private final String _account;
	private final int _chars;
	private final List<Long> _timeToDelete;
	
	/**
	 * Creates a new {@code ReplyCharacters} packet.<br>
	 * This constructor initializes the character data for an account.<br>
	 * It handles {@code null} values by providing default empty values.
	 * @param account The name of the account.
	 * @param chars The total number of characters associated with the account.
	 * @param timeToDel A list of epoch milliseconds representing when characters will be deleted.
	 */
	public ReplyCharacters(String account, int chars, List<Long> timeToDel)
	{
		_account = (account != null) ? account : "";
		_chars = chars;
		_timeToDelete = (timeToDel != null) ? timeToDel : Collections.emptyList();
	}
	
	/**
	 * Writes the packet data to the network stream.<br>
	 * This method serializes the account name, character count, and delete timers.<br>
	 * It handles {@code null} values in the time list by writing {@code 0L}.
	 */
	@Override
	public void write()
	{
		writeByte(OPCODE);
		writeString(_account);
		writeByte(_chars);
		writeByte(_timeToDelete.size());
		for (Long t : _timeToDelete)
		{
			writeLong((t != null) ? t.longValue() : 0L);
		}
	}
}
