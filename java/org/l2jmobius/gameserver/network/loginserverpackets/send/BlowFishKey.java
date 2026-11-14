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

import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;

import org.l2jmobius.commons.network.packet.SimpleWritablePacket;

/**
 * Sends the encrypted Blowfish session key to the login server during the initial handshake.<br>
 * This class uses {@code RSA/ECB/NoPadding} to encrypt the data and ensures that the packet is only written if encryption succeeds.
 * @author BazookaRpm
 */
public final class BlowFishKey extends SimpleWritablePacket
{
	// Logger.
	private static final Logger LOGGER = Logger.getLogger(BlowFishKey.class.getName());
	
	// Constants.
	private static final int PACKET_OPCODE = 0x00; // Opcode for Blowfish key packet.
	private static final String RSA_ECB_NOPADDING = "RSA/ECB/nopadding"; // Transformation used by login protocol.
	private static final String LOG_PREFIX = "BlowFishKey: ";
	private static final int EXPECTED_MIN_BF_LEN = 16; // Sanity-only lower bound; do not enforce exact size.
	private static final int EXPECTED_MAX_BF_LEN = 64; // Sanity-only upper bound; do not enforce exact size.
	
	/**
	 * Creates a new {@code BlowFishKey} packet.<br>
	 * This constructor encrypts the provided blowfish key using an RSA public key.<br>
	 * It validates that both keys are not {@code null} or empty before processing.
	 * @param blowfishKey The raw blowfish key to be encrypted.
	 * @param publicKey The {@code RSAPublicKey} used for the encryption process.
	 */
	public BlowFishKey(byte[] blowfishKey, RSAPublicKey publicKey)
	{
		if ((blowfishKey == null) || (blowfishKey.length == 0))
		{
			throw new IllegalArgumentException("blowfishKey must not be null/empty");
		}
		if (publicKey == null)
		{
			throw new IllegalArgumentException("publicKey must not be null");
		}
		
		// Non-intrusive sanity log to help field diagnostics without enforcing exact sizes.
		if ((blowfishKey.length < EXPECTED_MIN_BF_LEN) || (blowfishKey.length > EXPECTED_MAX_BF_LEN))
		{
			LOGGER.warning(LOG_PREFIX + "Unexpected Blowfish key length: " + blowfishKey.length + " bytes.");
		}
		
		try
		{
			final Cipher rsaCipher = Cipher.getInstance(RSA_ECB_NOPADDING);
			rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
			final byte[] encryptedKey = rsaCipher.doFinal(blowfishKey);
			
			// Write only after successful encryption to avoid partial buffers.
			writeByte(PACKET_OPCODE);
			writeInt(encryptedKey.length);
			writeBytes(encryptedKey);
		}
		catch (GeneralSecurityException e)
		{
			// Keep concise; avoid leaking sensitive material. Caller compatibility requires no checked throws here.
			LOGGER.log(Level.SEVERE, LOG_PREFIX + "RSA encryption failed.", e);
		}
	}
}
