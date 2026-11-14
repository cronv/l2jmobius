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
package org.l2jmobius.loginserver.network.gameserverpackets.receive;

import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;

import org.l2jmobius.commons.crypt.NewCrypt;
import org.l2jmobius.commons.network.packet.SimpleReadablePacket;
import org.l2jmobius.loginserver.GameServerThread;
import org.l2jmobius.loginserver.network.GameServerPacketHandler.GameServerState;

/**
 * Receives the Blowfish session key from the game server, encrypted with {@code RSA}.<br>
 * The decrypted {@code RSA} block may contain leading zero bytes that must be stripped before use.
 * @author BazookaRpm
 */
public class BlowFishKey extends SimpleReadablePacket
{
	private static final Logger LOGGER = Logger.getLogger(BlowFishKey.class.getName());
	
	/**
	 * Initializes a new {@code BlowFishKey} packet.<br>
	 * This constructor decrypts the RSA block and sets up the Blowfish encryption for the session.
	 * @param decrypt The encrypted byte array received from the server.
	 * @param server The {@code GameServerThread} instance to update with the new key.
	 */
	public BlowFishKey(byte[] decrypt, GameServerThread server)
	{
		super(decrypt);
		
		readByte(); // Packet id, it is already processed.
		
		// Reads the size of the RSA encrypted block.
		final int encryptedBlockSize = readInt();
		if (encryptedBlockSize <= 0)
		{
			LOGGER.warning("Invalid RSA block size for Blowfish key: " + encryptedBlockSize);
			return;
		}
		
		// Reads the encrypted data block.
		final byte[] encryptedBlock = readBytes(encryptedBlockSize);
		
		try
		{
			// Decrypts the RSA block.
			final byte[] decryptedBlock = decryptRsaBlock(encryptedBlock, server);
			
			// Extracts the Blowfish key from the decrypted block.
			final byte[] blowfishKey = extractKeyMaterial(decryptedBlock);
			
			if (blowfishKey.length == 0)
			{
				LOGGER.warning("Decrypted Blowfish key is empty.");
				return;
			}
			
			// Configures Blowfish encryption and updates connection state.
			server.setBlowFish(new NewCrypt(blowfishKey));
			server.setLoginConnectionState(GameServerState.BF_CONNECTED);
		}
		catch (GeneralSecurityException e)
		{
			LOGGER.log(Level.SEVERE, "Error while decrypting Blowfish key (RSA).", e);
		}
	}
	
	/**
	 * Decrypts an RSA encrypted block using the private key from a {@code GameServerThread}.<br>
	 * This method is used to retrieve the session key sent by the game server.
	 * @param encryptedBlock The raw bytes of the encrypted data received from the network.
	 * @param server The thread instance containing the necessary private key for decryption.
	 * @return A byte array containing the decrypted data.
	 * @throws GeneralSecurityException If an error occurs during the RSA decryption process.
	 */
	private static byte[] decryptRsaBlock(byte[] encryptedBlock, GameServerThread server) throws GeneralSecurityException
	{
		final Cipher rsaCipher = Cipher.getInstance("RSA/ECB/NoPadding");
		rsaCipher.init(Cipher.DECRYPT_MODE, server.getPrivateKey());
		return rsaCipher.doFinal(encryptedBlock);
	}
	
	/**
	 * This method removes leading zero bytes from a decrypted block.<br>
	 * It extracts the actual key material for use in encryption.
	 * @param decryptedBlock The {@code byte[]} containing the raw decrypted data.
	 * @return A new {@code byte[]} containing only the useful key material.
	 */
	private static byte[] extractKeyMaterial(byte[] decryptedBlock)
	{
		// Finds the first non-zero byte.
		int offset = 0;
		while ((offset < decryptedBlock.length) && (decryptedBlock[offset] == 0))
		{
			offset++;
		}
		
		// Calculates the useful key length.
		final int keyLength = decryptedBlock.length - offset;
		if (keyLength <= 0)
		{
			return new byte[0];
		}
		
		// Copies the useful key material to a new array.
		final byte[] key = new byte[keyLength];
		System.arraycopy(decryptedBlock, offset, key, 0, keyLength);
		return key;
	}
}
