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

import org.l2jmobius.commons.network.buffer.ReadBuffer;
import org.l2jmobius.commons.network.buffer.WriteBuffer;

/**
 * This class provides a stateful XOR cipher used for securing the game protocol.<br>
 * It manages session keys and maintains a rolling offset to encrypt and decrypt network buffers. The {@code Encryption} class ensures efficient processing by using pre-extended integer masks for key operations.
 * @author BazookaRpm, Mobius
 */
public class Encryption
{
	// Constants.
	private static final int KEY_LENGTH = 16;
	private static final int BYTE_MASK = 0xFF;
	private static final int NIBBLE_MASK = 0x0F;
	private static final int SHIFT_8 = 8;
	private static final int SHIFT_16 = 16;
	private static final int SHIFT_24 = 24;
	
	// Rolling offset is stored at key[8..11] (little-endian).
	private static final int OFFSET_INDEX = 8;
	
	// Session keys (references are immutable; contents are intentionally mutable).
	private final byte[] _inKey = new byte[KEY_LENGTH];
	private final byte[] _outKey = new byte[KEY_LENGTH];
	
	// Pre-zero-extended mirrors of _inKey / _outKey for the hot XOR loops.
	private final int[] _inKeyMasked = new int[KEY_LENGTH];
	private final int[] _outKeyMasked = new int[KEY_LENGTH];
	
	// Enabled state.
	private boolean _isEnabled;
	
	/**
	 * Sets the session keys for encryption and decryption.<br>
	 * This method initializes both the inbound and outbound keys.<br>
	 * The provided {@code byte[]} must be at least 16 bytes long.
	 * @param key The byte array containing the encryption key.
	 */
	public void setKey(byte[] key)
	{
		if ((key == null) || (key.length < KEY_LENGTH))
		{
			throw new IllegalArgumentException("Encryption key must be at least 16 bytes.");
		}
		
		System.arraycopy(key, 0, _inKey, 0, KEY_LENGTH);
		System.arraycopy(key, 0, _outKey, 0, KEY_LENGTH);
		for (int i = 0; i < KEY_LENGTH; i++)
		{
			final int v = key[i] & BYTE_MASK;
			_inKeyMasked[i] = v;
			_outKeyMasked[i] = v;
		}
	}
	
	/**
	 * Encrypts a portion of the provided buffer using an XOR cipher.<br>
	 * This method updates the internal state and advances the rolling offset.<br>
	 * If the cipher is not yet enabled, this call will only enable it.
	 * @param data The {@code WriteBuffer} containing the data to be encrypted.
	 * @param offset The starting position in the buffer.
	 * @param size The number of bytes to process.
	 */
	public void encrypt(WriteBuffer data, int offset, int size)
	{
		if (!_isEnabled)
		{
			_isEnabled = true;
			return;
		}
		
		if (size <= 0)
		{
			return;
		}
		
		final int[] key = _outKeyMasked;
		int prev = 0;
		for (int i = 0; i < size; i++)
		{
			final int raw = data.readByte(offset + i) & BYTE_MASK;
			prev = raw ^ key[i & NIBBLE_MASK] ^ prev;
			data.writeByte(offset + i, (byte) prev);
		}
		
		advanceOffset(_outKey, _outKeyMasked, size);
	}
	
	/**
	 * Decrypts a portion of the data in the provided buffer.<br>
	 * This method uses an XOR cipher to reverse encryption.<br>
	 * It updates the internal rolling offset after processing.
	 * @param data The {@code ReadBuffer} containing the encrypted data.
	 * @param offset The starting position in the buffer.
	 * @param size The number of bytes to decrypt.
	 */
	public void decrypt(ReadBuffer data, int offset, int size)
	{
		if (!_isEnabled)
		{
			return;
		}
		
		if (size <= 0)
		{
			return;
		}
		
		final int[] key = _inKeyMasked;
		int last = 0;
		for (int i = 0; i < size; i++)
		{
			final int enc = data.readByte(offset + i) & BYTE_MASK;
			data.writeByte(offset + i, (byte) (enc ^ key[i & NIBBLE_MASK] ^ last));
			last = enc;
		}
		
		advanceOffset(_inKey, _inKeyMasked, size);
	}
	
	/**
	 * Updates the rolling offset used for encryption.<br>
	 * This method increments the value stored in {@code key} at index {@code OFFSET_INDEX}.<br>
	 * It also synchronizes the updated values with the {@code keyMasked} array.
	 * @param key The byte array containing the session key.
	 * @param keyMasked The masked integer array used for fast encryption operations.
	 * @param size The number of bytes to add to the current offset.
	 */
	private static void advanceOffset(byte[] key, int[] keyMasked, int size)
	{
		// Advance rolling offset at key[8..11] by size (little-endian int).
		int old = (key[OFFSET_INDEX] & BYTE_MASK);
		old |= (key[OFFSET_INDEX + 1] & BYTE_MASK) << SHIFT_8;
		old |= (key[OFFSET_INDEX + 2] & BYTE_MASK) << SHIFT_16;
		old |= (key[OFFSET_INDEX + 3] & BYTE_MASK) << SHIFT_24;
		
		old += size;
		
		final byte b0 = (byte) (old & BYTE_MASK);
		final byte b1 = (byte) ((old >> SHIFT_8) & BYTE_MASK);
		final byte b2 = (byte) ((old >> SHIFT_16) & BYTE_MASK);
		final byte b3 = (byte) ((old >> SHIFT_24) & BYTE_MASK);
		
		key[OFFSET_INDEX] = b0;
		key[OFFSET_INDEX + 1] = b1;
		key[OFFSET_INDEX + 2] = b2;
		key[OFFSET_INDEX + 3] = b3;
		
		// Keep the masked mirror in sync - only [8..11] change.
		keyMasked[OFFSET_INDEX] = b0 & BYTE_MASK;
		keyMasked[OFFSET_INDEX + 1] = b1 & BYTE_MASK;
		keyMasked[OFFSET_INDEX + 2] = b2 & BYTE_MASK;
		keyMasked[OFFSET_INDEX + 3] = b3 & BYTE_MASK;
	}
}
