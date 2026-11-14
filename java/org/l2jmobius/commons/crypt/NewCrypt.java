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
package org.l2jmobius.commons.crypt;

import java.nio.charset.StandardCharsets;

/**
 * This class handles the Blowfish cipher implementation with {@code ECB} processing for L2J packet encryption.<br>
 * It provides checksum validation and XOR encryption to ensure secure communication between the server and the client.
 */
public class NewCrypt
{
	// Constants.
	private static final int BYTES_PER_BLOCK = 4;
	private static final int CHECKSUM_SIZE = 4;
	private static final int BLOWFISH_BLOCK_SIZE = 8;
	private static final int XOR_KEY_OFFSET = 4;
	private static final int XOR_FINAL_OFFSET = 8;
	private static final int BYTE_MASK = 0xFF;
	private static final int SHIFT_8_BITS = 8;
	private static final int SHIFT_16_BITS = 16;
	private static final int SHIFT_24_BITS = 24;
	private static final long MASK_16_BITS = 0xFF00;
	private static final long MASK_24_BITS = 0xFF0000;
	private static final long MASK_32_BITS = 0xFF000000;
	
	// Encryption Engine.
	private final BlowfishEngine _blowfishCipher;
	
	/**
	 * Creates a new instance of {@code NewCrypt}.<br>
	 * This constructor initializes the internal {@code BlowfishEngine} using the provided key.
	 * @param blowfishKey The byte array containing the secret key for encryption.
	 */
	public NewCrypt(byte[] blowfishKey)
	{
		_blowfishCipher = new BlowfishEngine();
		_blowfishCipher.init(blowfishKey);
	}
	
	/**
	 * Creates a new instance of {@code NewCrypt} using a string key.<br>
	 * This method converts the provided {@code String} into bytes using UTF-8 encoding.<br>
	 * The resulting bytes are used to initialize the internal encryption engine.
	 * @param key The string value used to generate the encryption key.
	 */
	public NewCrypt(String key)
	{
		this(key.getBytes(StandardCharsets.UTF_8));
	}
	
	/**
	 * Validates the integrity of the provided data.<br>
	 * This method checks if the {@code rawData} contains a valid checksum.<br>
	 * It uses the full length of the array for verification.
	 * @param rawData The byte array to be checked.
	 * @return {@code true} if the checksum is valid, {@code false} otherwise.
	 */
	public static boolean verifyChecksum(byte[] rawData)
	{
		return verifyChecksum(rawData, 0, rawData.length);
	}
	
	/**
	 * Verifies the integrity of a data packet using a checksum.<br>
	 * This method checks if the last 4 bytes match the calculated XOR sum.<br>
	 * It returns {@code false} if the size is invalid or the checksum fails.
	 * @param rawData The byte array containing the data to verify.
	 * @param offset The starting position in the array.
	 * @param size The total number of bytes to process.
	 * @return {@code true} if the checksum is valid, otherwise {@code false}.
	 */
	public static boolean verifyChecksum(byte[] rawData, int offset, int size)
	{
		// Check if size is multiple of 4 and if there is more than only the checksum.
		if (((size & 3) != 0) || (size <= CHECKSUM_SIZE))
		{
			return false;
		}
		
		long calculatedChecksum = 0;
		final int dataLength = size - CHECKSUM_SIZE;
		long currentBlock = -1;
		int position;
		
		for (position = offset; position < dataLength; position += BYTES_PER_BLOCK)
		{
			currentBlock = rawData[position] & BYTE_MASK;
			currentBlock |= (rawData[position + 1] << SHIFT_8_BITS) & MASK_16_BITS;
			currentBlock |= (rawData[position + 2] << SHIFT_16_BITS) & MASK_24_BITS;
			currentBlock |= (rawData[position + 3] << SHIFT_24_BITS) & MASK_32_BITS;
			
			calculatedChecksum ^= currentBlock;
		}
		
		currentBlock = rawData[position] & BYTE_MASK;
		currentBlock |= (rawData[position + 1] << SHIFT_8_BITS) & MASK_16_BITS;
		currentBlock |= (rawData[position + 2] << SHIFT_16_BITS) & MASK_24_BITS;
		currentBlock |= (rawData[position + 3] << SHIFT_24_BITS) & MASK_32_BITS;
		
		return currentBlock == calculatedChecksum;
	}
	
	/**
	 * This method calculates and adds a checksum to the provided data.<br>
	 * It modifies the {@code rawData} array directly.<br>
	 * Use this to ensure data integrity before transmission.
	 * @param rawData The byte array to be updated with a checksum.
	 */
	public static void appendChecksum(byte[] rawData)
	{
		appendChecksum(rawData, 0, rawData.length);
	}
	
	/**
	 * Calculates and appends a checksum to the provided byte array.<br>
	 * This method ensures data integrity by updating the last 4 bytes of the specified range.
	 * @param rawData The {@code byte[]} containing the data to process.
	 * @param offset The starting index in {@code rawData}.
	 * @param size The total length of the data segment to include in the checksum.
	 */
	public static void appendChecksum(byte[] rawData, int offset, int size)
	{
		long calculatedChecksum = 0;
		final int dataLength = size - CHECKSUM_SIZE;
		long currentBlock;
		int position;
		
		for (position = offset; position < dataLength; position += BYTES_PER_BLOCK)
		{
			currentBlock = rawData[position] & BYTE_MASK;
			currentBlock |= (rawData[position + 1] << SHIFT_8_BITS) & MASK_16_BITS;
			currentBlock |= (rawData[position + 2] << SHIFT_16_BITS) & MASK_24_BITS;
			currentBlock |= (rawData[position + 3] << SHIFT_24_BITS) & MASK_32_BITS;
			
			calculatedChecksum ^= currentBlock;
		}
		
		currentBlock = rawData[position] & BYTE_MASK;
		currentBlock |= (rawData[position + 1] << SHIFT_8_BITS) & MASK_16_BITS;
		currentBlock |= (rawData[position + 2] << SHIFT_16_BITS) & MASK_24_BITS;
		currentBlock |= (rawData[position + 3] << SHIFT_24_BITS) & MASK_32_BITS;
		
		rawData[position] = (byte) (calculatedChecksum & BYTE_MASK);
		rawData[position + 1] = (byte) ((calculatedChecksum >> SHIFT_8_BITS) & BYTE_MASK);
		rawData[position + 2] = (byte) ((calculatedChecksum >> SHIFT_16_BITS) & BYTE_MASK);
		rawData[position + 3] = (byte) ((calculatedChecksum >> SHIFT_24_BITS) & BYTE_MASK);
	}
	
	/**
	 * Encrypts the provided data using an XOR operation.<br>
	 * This method applies a simple XOR cipher to every byte in the array.<br>
	 * It is used for basic packet encryption during the handshake process.
	 * @param rawData The {@code byte[]} array containing the data to be encrypted.
	 * @param xorKey The {@code int} key used to perform the XOR operation.
	 */
	public static void encXORPass(byte[] rawData, int xorKey)
	{
		encXORPass(rawData, 0, rawData.length, xorKey);
	}
	
	/**
	 * Performs an XOR encryption pass on a specific section of the data.<br>
	 * This method modifies the {@code rawData} array in place.<br>
	 * It uses a rolling key based on the provided {@code xorKey}.
	 * @param rawData The byte array to be encrypted.
	 * @param offset The starting position within the array.
	 * @param size The number of bytes to process.
	 * @param xorKey The initial key used for the XOR operation.
	 */
	public static void encXORPass(byte[] rawData, int offset, int size, int xorKey)
	{
		final int endPosition = size - XOR_FINAL_OFFSET;
		int currentPosition = XOR_KEY_OFFSET + offset;
		int dataBlock;
		int encryptionKey = xorKey; // Initial xor key.
		
		while (currentPosition < endPosition)
		{
			dataBlock = rawData[currentPosition] & BYTE_MASK;
			dataBlock |= (rawData[currentPosition + 1] & BYTE_MASK) << SHIFT_8_BITS;
			dataBlock |= (rawData[currentPosition + 2] & BYTE_MASK) << SHIFT_16_BITS;
			dataBlock |= (rawData[currentPosition + 3] & BYTE_MASK) << SHIFT_24_BITS;
			
			encryptionKey += dataBlock;
			
			dataBlock ^= encryptionKey;
			
			rawData[currentPosition++] = (byte) (dataBlock & BYTE_MASK);
			rawData[currentPosition++] = (byte) ((dataBlock >> SHIFT_8_BITS) & BYTE_MASK);
			rawData[currentPosition++] = (byte) ((dataBlock >> SHIFT_16_BITS) & BYTE_MASK);
			rawData[currentPosition++] = (byte) ((dataBlock >> SHIFT_24_BITS) & BYTE_MASK);
		}
		
		rawData[currentPosition++] = (byte) (encryptionKey & BYTE_MASK);
		rawData[currentPosition++] = (byte) ((encryptionKey >> SHIFT_8_BITS) & BYTE_MASK);
		rawData[currentPosition++] = (byte) ((encryptionKey >> SHIFT_16_BITS) & BYTE_MASK);
		rawData[currentPosition++] = (byte) ((encryptionKey >> SHIFT_24_BITS) & BYTE_MASK);
	}
	
	/**
	 * Decrypts a specific portion of the provided byte array.<br>
	 * This method uses the {@code BlowfishEngine} to process data in blocks.<br>
	 * It modifies the {@code rawData} array directly.
	 * @param rawData The byte array containing the encrypted data.
	 * @param offset The starting position within the array to begin decryption.
	 * @param size The number of bytes to decrypt from the starting offset.
	 */
	public void decrypt(byte[] rawData, int offset, int size)
	{
		for (int i = offset; i < (offset + size); i += BLOWFISH_BLOCK_SIZE)
		{
			_blowfishCipher.decryptBlock(rawData, i);
		}
	}
	
	/**
	 * Encrypts a specific portion of the provided byte array.<br>
	 * This method uses the {@code BlowfishEngine} to process data in blocks.
	 * @param rawData The byte array containing the data to be encrypted.
	 * @param offset The starting position within the array.
	 * @param size The number of bytes to encrypt from the starting position.
	 */
	public void crypt(byte[] rawData, int offset, int size)
	{
		for (int i = offset; i < (offset + size); i += BLOWFISH_BLOCK_SIZE)
		{
			_blowfishCipher.encryptBlock(rawData, i);
		}
	}
}
