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
package org.l2jmobius.commons.network.handler;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.logging.Logger;

import org.l2jmobius.commons.network.Client;
import org.l2jmobius.commons.network.Connection;
import org.l2jmobius.commons.network.ConnectionConfig;
import org.l2jmobius.commons.network.buffer.ReadBuffer;
import org.l2jmobius.commons.network.packet.PacketExecutor;
import org.l2jmobius.commons.network.packet.PacketHandler;
import org.l2jmobius.commons.network.packet.ReadablePacket;

/**
 * Asynchronous read {@code CompletionHandler} that assembles, decrypts and dispatches incoming packets, guarding against oversized data and excessive packet rates.
 * @param <T> The client type handled.
 * @author JoeAlisson, Mobius
 */
public class ReadHandler<T extends Client<Connection<T>>> implements CompletionHandler<Integer, T>
{
	private static final Logger LOGGER = Logger.getLogger(ReadHandler.class.getName());
	
	/** Maximum accepted payload size (65533 bytes = 0xFFFF total − 2-byte header). */
	private static final int MAX_PACKET_SIZE = 65533;
	
	private final PacketHandler<T> _packetHandler;
	private final PacketExecutor<T> _executor;
	
	/**
	 * Creates a {@code ReadHandler} with the given packet handler and executor.
	 * @param packetHandler The {@code PacketHandler} processing incoming packets.
	 * @param executor The {@code PacketExecutor} running packet logic.
	 */
	public ReadHandler(PacketHandler<T> packetHandler, PacketExecutor<T> executor)
	{
		_packetHandler = packetHandler;
		_executor = executor;
	}
	
	/**
	 * Handles a completed read, validating the byte count and dispatching to header or payload handling.
	 * @param bytesRead The number of bytes read.
	 * @param client The {@code T} client for this connection.
	 */
	@Override
	public void completed(Integer bytesRead, T client)
	{
		if (!client.isConnected())
		{
			return;
		}
		
		if (bytesRead < 0)
		{
			// Clean peer close.
			client.disconnect();
			return;
		}
		
		if (bytesRead < client.getExpectedReadSize())
		{
			// Partial read - resume until the full segment arrives.
			client.resumeRead(bytesRead);
			return;
		}
		
		if (client.isReadingPayload())
		{
			handlePayload(client);
		}
		else
		{
			handleHeader(client);
		}
	}
	
	/**
	 * Reads the packet header, handling keep-alives and disconnecting on oversized payloads.
	 * @param client The {@code T} client to process.
	 */
	private void handleHeader(T client)
	{
		// Exactly one header is read per packet, so metering here counts packets rather than TCP segments.
		if (!client.getFloodGuard().allow())
		{
			LOGGER.warning("ReadHandler: Packet rate limit exceeded, disconnecting " + client.getHostAddress() + ".");
			client.disconnect();
			return;
		}
		
		final ByteBuffer buffer = client.getConnection().getReadingBuffer();
		if (buffer == null)
		{
			client.disconnect();
			return;
		}
		
		buffer.flip();
		
		// The 2-byte header is the total packet length (header + payload).
		final int dataSize = Short.toUnsignedInt(buffer.getShort()) - ConnectionConfig.HEADER_SIZE;
		if (dataSize <= 0)
		{
			// Zero-payload packet (keep-alive / heartbeat) - skip silently.
			client.read();
			return;
		}
		
		// Guard against packets large enough to cause OOM or exceed protocol limits.
		if (dataSize > MAX_PACKET_SIZE)
		{
			client.disconnect();
			return;
		}
		
		client.readPayload(dataSize);
	}
	
	/**
	 * Reads the packet payload and starts the next read.
	 * @param client The {@code T} client to process.
	 */
	private void handlePayload(T client)
	{
		final ByteBuffer buffer = client.getConnection().getReadingBuffer();
		if (buffer == null)
		{
			client.disconnect();
			return;
		}
		
		buffer.flip();
		parseAndExecutePacket(client, buffer);
		
		// Immediately start reading the next packet's header.
		client.read();
	}
	
	/**
	 * Decrypts, parses and executes a packet from the buffer.
	 * @param client The {@code T} client receiving the packet.
	 * @param incomingBuffer The {@code ByteBuffer} with the raw packet.
	 */
	private void parseAndExecutePacket(T client, ByteBuffer incomingBuffer)
	{
		try
		{
			final ReadBuffer buffer = ReadBuffer.of(incomingBuffer);
			if (client.decrypt(buffer, 0, buffer.remaining()))
			{
				final ReadablePacket<T> packet = _packetHandler.handle(buffer, client);
				if (packet != null)
				{
					packet.init(client, buffer);
					if (packet.read())
					{
						_executor.execute(packet);
					}
				}
			}
		}
		catch (Exception e)
		{
			failed(e, client);
		}
	}
	
	/**
	 * Disconnects the client on a read failure.
	 * @param e The {@code Throwable} that occurred.
	 * @param client The {@code T} client to disconnect.
	 */
	@Override
	public void failed(Throwable e, T client)
	{
		client.disconnect();
	}
}
