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
package org.l2jmobius.gameserver.data.holders;

/**
 * Immutable announcement action of a boat station, loaded from BoatData.xml.<br>
 * Broadcasts up to two system messages with an optional sound, then waits before the next action.
 * @author Mobius
 */
public class BoatActionHolder
{
	private final String _sound;
	private final int _messageId;
	private final int _messageId2;
	private final int _wait;
	
	/**
	 * Creates a new {@code BoatActionHolder}.
	 * @param sound The sound name played with the announcement, or {@code null} for none.
	 * @param messageId The system message id broadcast at the boat broadcast points.
	 * @param messageId2 The second system message id, or 0 for none.
	 * @param wait The wait time in milliseconds before the next action, or 0 for none.
	 */
	public BoatActionHolder(String sound, int messageId, int messageId2, int wait)
	{
		_sound = sound;
		_messageId = messageId;
		_messageId2 = messageId2;
		_wait = wait;
	}
	
	public String getSound()
	{
		return _sound;
	}
	
	public int getMessageId()
	{
		return _messageId;
	}
	
	public int getMessageId2()
	{
		return _messageId2;
	}
	
	public int getWait()
	{
		return _wait;
	}
}
