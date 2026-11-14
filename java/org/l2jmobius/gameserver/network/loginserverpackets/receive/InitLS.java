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
package org.l2jmobius.gameserver.network.loginserverpackets.receive;

import org.l2jmobius.commons.network.packet.SimpleReadablePacket;

/**
 * Handles the initial connection request from a client to the login server.<br>
 * This packet is used to initialize the communication session between the client and {@code InitLS}.
 */
public class InitLS extends SimpleReadablePacket
{
	private final int _rev;
	private final byte[] _key;
	
	/**
	 * Retrieves the revision number of the packet.<br>
	 * This value is used to verify the version compatibility.
	 * @return The {@code int} revision value.
	 */
	public int getRevision()
	{
		return _rev;
	}
	
	/**
	 * Retrieves the RSA key from this packet.<br>
	 * This method returns the raw byte array used for decryption.
	 * @return The {@code byte[]} containing the RSA key.
	 */
	public byte[] getRSAKey()
	{
		return _key;
	}
	
	/**
	 * Initializes a new {@code InitLS} packet instance.<br>
	 * This constructor reads the revision, size, and key from the provided data.
	 * @param decrypt The raw byte array containing the encrypted packet data.
	 */
	public InitLS(byte[] decrypt)
	{
		super(decrypt);
		readByte(); // Packet id, it is already processed.
		
		_rev = readInt();
		final int size = readInt();
		_key = readBytes(size);
	}
}
