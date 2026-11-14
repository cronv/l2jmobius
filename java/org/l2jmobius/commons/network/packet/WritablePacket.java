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
package org.l2jmobius.commons.network.packet;

import org.l2jmobius.commons.network.Client;
import org.l2jmobius.commons.network.Connection;
import org.l2jmobius.commons.network.ConnectionConfig;
import org.l2jmobius.commons.network.buffer.WriteBuffer;

/**
 * This is the base class for all packets sent to clients.<br>
 * It handles a 2-byte little-endian header that encodes the total packet length.<br>
 * Use {@code sendInBroadcast()} to optimize sending the same data to multiple recipients.
 * @param <T> The client type associated with this packet.
 * @author Mobius
 */
public abstract class WritablePacket<T extends Client<Connection<T>>>
{
	private volatile boolean _broadcast;
	
	/**
	 * Cached snapshot of the serialized packet bytes for broadcast reuse.<br>
	 * Populated on the first send when {@link #_broadcast} is {@code true}; every later send bulk-copies these bytes into a fresh per-client {@link WriteBuffer}. Guarded by {@code this} (synchronized on this {@code WritablePacket} instance).
	 */
	private byte[] _broadcastCacheBytes;
	private int _broadcastCacheLength;
	
	/**
	 * Creates a new instance of this packet.<br>
	 * This is the default constructor for {@code WritablePacket}.
	 */
	protected WritablePacket()
	{
	}
	
	/**
	 * Writes the packet data into a {@code WriteBuffer}.<br>
	 * This method handles both standard and broadcast writes.<br>
	 * It returns the buffer used for the operation.
	 * @param client The {@code Client} instance to write the data to.
	 * @return The populated {@code WriteBuffer}.
	 * @throws Exception If the packet's {@code write(...)} method returns false.
	 */
	public WriteBuffer writeData(T client) throws Exception
	{
		if (_broadcast)
		{
			return writeDataWithCache(client);
		}
		
		return writeDataToBuffer(client);
	}
	
	/**
	 * Writes the packet data to a {@code WriteBuffer}.<br>
	 * This method checks if a broadcast cache already exists.<br>
	 * If it exists, it returns a new buffer seeded from that cache.<br>
	 * Otherwise, it writes the data and saves it to the cache for future use.
	 * @param client The {@code Client} instance used to write the data.
	 * @return A {@code WriteBuffer} containing the serialized packet data.
	 * @throws Exception If the packet's {@code write(...)} method returns false.
	 */
	private synchronized WriteBuffer writeDataWithCache(T client) throws Exception
	{
		if (_broadcastCacheBytes != null)
		{
			return new WriteBuffer(_broadcastCacheBytes, _broadcastCacheLength, client.getResourcePool(), getClass());
		}
		
		final WriteBuffer buffer = writeDataToBuffer(client);
		_broadcastCacheBytes = buffer.toByteArray();
		_broadcastCacheLength = buffer.limit();
		return buffer;
	}
	
	/**
	 * Prepares a {@code WriteBuffer} for the given client.<br>
	 * This method initializes the buffer and attempts to write the packet data.<br>
	 * If writing fails, it releases resources and throws an {@code Exception}.
	 * @param client The {@code Client} instance used to write the data.
	 * @return A populated {@code WriteBuffer} if successful.
	 * @throws Exception If the {@code write(...)} method returns false.
	 */
	private WriteBuffer writeDataToBuffer(T client) throws Exception
	{
		final WriteBuffer buffer = new WriteBuffer(client.getResourcePool(), getClass());
		buffer.position(ConnectionConfig.HEADER_SIZE);
		if (write(client, buffer))
		{
			buffer.mark();
			return buffer;
		}
		
		buffer.releaseResources();
		throw new Exception("WritablePacket.write() returned false for " + getClass().getSimpleName());
	}
	
	/**
	 * Writes the packet header to the provided buffer.<br>
	 * The header is stored at the start of the buffer as a 2-byte little-endian short.
	 * @param buffer The {@code WriteBuffer} where the header will be written.
	 * @param header The integer value of the header to write.
	 */
	public void writeHeader(WriteBuffer buffer, int header)
	{
		buffer.writeShort(0, (short) header);
	}
	
	/**
	 * Enables broadcast optimization for this packet.<br>
	 * Once enabled, the serialized bytes are cached on the first send and reused for each subsequent recipient.<br>
	 * Call this before sending the packet to multiple clients.
	 */
	public void sendInBroadcast()
	{
		_broadcast = true;
	}
	
	/**
	 * Checks if the packet is allowed to be dropped for a specific client.<br>
	 * This default implementation always returns {@code false}; override it to allow dropping.
	 * @param client The {@code Client} instance to check.
	 * @return {@code true} if the packet can be dropped, otherwise {@code false}.
	 */
	public boolean canBeDropped(T client)
	{
		return false;
	}
	
	/**
	 * Writes the packet's payload into {@code buffer}.<br>
	 * The buffer's position is already set to {@link ConnectionConfig#HEADER_SIZE} on entry; the implementation should write all payload bytes and return {@code true} on success.
	 * @param client The recipient client, available for per-client customization.
	 * @param buffer The buffer to write payload data into.
	 * @return {@code true} if the packet was written successfully; {@code false} to cancel sending.
	 */
	protected abstract boolean write(T client, WriteBuffer buffer);
	
	/**
	 * Returns a string representation of this object.<br>
	 * This method returns the simple name of the current class.
	 * @return The simple name of the class as a {@code String}.
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName();
	}
}
