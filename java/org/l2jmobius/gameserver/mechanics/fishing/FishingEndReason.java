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
package org.l2jmobius.gameserver.mechanics.fishing;

/**
 * Defines the various reasons why a fishing action can be terminated.<br>
 * This enum is used by {@code Fishing} to determine how the fishing process ends.
 */
public enum FishingEndReason
{
	LOSE(0),
	WIN(1),
	STOP(2);
	
	private final int _reason;
	
	/**
	 * Creates a new {@code FishingEndReason} instance.<br>
	 * This constructor sets the internal reason value.
	 * @param reason The integer code representing why the fishing ended.
	 */
	FishingEndReason(int reason)
	{
		_reason = reason;
	}
	
	/**
	 * Retrieves the internal numeric code for the fishing end reason.<br>
	 * This value corresponds to the {@code _reason} field.
	 * @return The integer representation of the {@code FishingEndReason}.
	 */
	public int getReason()
	{
		return _reason;
	}
}
