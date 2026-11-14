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
package org.l2jmobius.gameserver.network;

import org.l2jmobius.commons.util.Rnd;

/**
 * This class generates Blowfish keys for {@code GameServer} sessions.<br>
 * It returns a new byte array of length {@code 16} consisting of {@code 8} random bytes and {@code 8} fixed suffix bytes.
 * @author BazookaRpm
 */
public class BlowFishKeygen
{
	private static final int KEY_LENGTH_BYTES = 16;
	private static final int RANDOM_PREFIX_LENGTH = 8;
	
	private static final byte[] KEY_TAIL_BYTES =
	{
		(byte) 0xC8,
		(byte) 0x27,
		(byte) 0x93,
		(byte) 0x01,
		(byte) 0xA1,
		(byte) 0x6C,
		(byte) 0x31,
		(byte) 0x97
	};
	
	/**
	 * Private constructor for the {@code BlowFishKeygen} class.<br>
	 * This prevents other classes from creating new instances of this utility.
	 */
	private BlowFishKeygen()
	{
	}
	
	/**
	 * Generates a random encryption key for game sessions.<br>
	 * This method creates a {@code byte[]} of 16 bytes.<br>
	 * The first 8 bytes are random and the last 8 bytes are fixed.
	 * @return A new {@code byte[]} containing the generated key.
	 */
	public static byte[] getRandomKey()
	{
		final byte[] key = new byte[KEY_LENGTH_BYTES];
		Rnd.nextBytes(key);
		System.arraycopy(KEY_TAIL_BYTES, 0, key, RANDOM_PREFIX_LENGTH, KEY_TAIL_BYTES.length);
		return key;
	}
}
