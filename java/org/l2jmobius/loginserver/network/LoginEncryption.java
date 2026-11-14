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

import org.l2jmobius.commons.crypt.NewCrypt;
import org.l2jmobius.commons.network.buffer.ReadBuffer;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.commons.util.Rnd;

/**
 * This class handles the Blowfish cipher encryption used by the login protocol.<br>
 * It manages a 16-byte static key for the initial handshake and switches to a session-specific key thereafter.<br>
 * The class also processes XOR headers during the static phase and checksums during the session phase.
 * @author BazookaRpm, Mobius
 */
public class LoginEncryption
{
	// Static Blowfish key used for the initial handshake.
	private static final byte[] STATIC_BLOWFISH_KEY =
	{
		(byte) 0x6b,
		(byte) 0x60,
		(byte) 0xcb,
		(byte) 0x5b,
		(byte) 0x82,
		(byte) 0xce,
		(byte) 0x90,
		(byte) 0xb1,
		(byte) 0xcc,
		(byte) 0x2b,
		(byte) 0x6c,
		(byte) 0x55,
		(byte) 0x6c,
		(byte) 0x6c,
		(byte) 0x6c,
		(byte) 0x6c
	};
	
	// Static crypt instance used while the protocol is in the bootstrap phase.
	private static final NewCrypt STATIC_CRYPT = new NewCrypt(STATIC_BLOWFISH_KEY);
	
	// Blowfish block size and protocol overhead constants.
	private static final int BLOWFISH_BLOCK_SIZE = 8;
	private static final int STATIC_HEADER_SIZE = 8;
	private static final int DYNAMIC_HEADER_SIZE = 4;
	private static final int CHECKSUM_SIZE = 8;
	
	// Session-specific crypt instance, set once the client key is negotiated.
	private NewCrypt _sessionCrypt;
	
	// Tracks whether the connection is still using the static key for encryption.
	private boolean _usingStaticKey = true;
	
	/**
	 * Sets the session-specific encryption key.<br>
	 * This method initializes a new {@code NewCrypt} instance using the provided bytes.<br>
	 * It is used after the client key has been successfully negotiated.
	 * @param key The byte array containing the new encryption key.
	 */
	public void setKey(byte[] key)
	{
		_sessionCrypt = new NewCrypt(key);
	}
	
	/**
	 * Decrypts a specific portion of the {@code ReadBuffer}.<br>
	 * It verifies the checksum after decryption.<br>
	 * If the data is valid, it updates the buffer with decrypted bytes.
	 * @param data The {@code ReadBuffer} containing the encrypted information.
	 * @param offset The starting position in the buffer to begin decryption.
	 * @param size The number of bytes to process.
	 * @return {@code true} if the checksum is valid, {@code false} otherwise.
	 */
	public boolean decrypt(ReadBuffer data, int offset, int size)
	{
		// Extract to byte array.
		final byte[] raw = new byte[size];
		for (int i = 0; i < size; i++)
		{
			raw[i] = data.readByte(offset + i);
		}
		
		// Decrypt and verify.
		_sessionCrypt.decrypt(raw, 0, size);
		final boolean valid = NewCrypt.verifyChecksum(raw, 0, size);
		
		// Write back to buffer if valid.
		if (valid)
		{
			for (int i = 0; i < size; i++)
			{
				data.writeByte(offset + i, raw[i]);
			}
		}
		
		return valid;
	}
	
	/**
	 * Calculates the total size of the data after encryption.<br>
	 * This includes the header, padding for block alignment, and the checksum.
	 * @param dataSize The original size of the raw data.
	 * @return The final size of the encrypted byte array.
	 */
	private int encryptedSize(int dataSize)
	{
		final int headerSize = _usingStaticKey ? STATIC_HEADER_SIZE : DYNAMIC_HEADER_SIZE;
		int sizeWithHeader = dataSize + headerSize;
		
		// Align to Blowfish block size (always pad at least one block).
		final int remainder = sizeWithHeader % BLOWFISH_BLOCK_SIZE;
		sizeWithHeader += (BLOWFISH_BLOCK_SIZE - remainder);
		
		// Append checksum/XOR header size.
		return sizeWithHeader + CHECKSUM_SIZE;
	}
	
	/**
	 * Encrypts the data provided in the {@code WriteBuffer}.<br>
	 * This method handles both static and session-based encryption.<br>
	 * It updates the buffer with the encrypted result.
	 * @param data The {@code WriteBuffer} containing the raw data to encrypt.
	 * @param offset The starting position of the data in the buffer.
	 * @param size The number of bytes to encrypt.
	 * @return Always returns {@code true} after successful encryption.
	 */
	public boolean encrypt(WriteBuffer data, int offset, int size)
	{
		final int packetSize = encryptedSize(size);
		
		// Extract to byte array.
		final byte[] raw = new byte[packetSize];
		for (int i = 0; i < size; i++)
		{
			raw[i] = data.readByte(offset + i);
		}
		
		// Encrypt.
		if (_usingStaticKey)
		{
			NewCrypt.encXORPass(raw, 0, packetSize, Rnd.nextInt());
			STATIC_CRYPT.crypt(raw, 0, packetSize);
			_usingStaticKey = false;
		}
		else
		{
			NewCrypt.appendChecksum(raw, 0, packetSize);
			_sessionCrypt.crypt(raw, 0, packetSize);
		}
		
		// Write back to buffer.
		data.limit(offset + packetSize);
		for (int i = 0; i < packetSize; i++)
		{
			data.writeByte(offset + i, raw[i]);
		}
		
		return true;
	}
}
