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
package org.l2jmobius.commons.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.l2jmobius.commons.network.handler.ReadHandler;
import org.l2jmobius.commons.network.handler.WriteHandler;
import org.l2jmobius.commons.network.pool.ResourcePool;

/**
 * Manages a single network connection over an {@code AsynchronousSocketChannel}, handling read/write buffer lifecycle.
 * @param <T> The client type bound to this connection.
 * @author JoeAlisson, Mobius
 */
public class Connection<T extends Client<Connection<T>>>
{
	private final AsynchronousSocketChannel _channel;
	private final ReadHandler<T> _readHandler;
	private final WriteHandler<T> _writeHandler;
	private final ConnectionConfig _config;
	
	/** Volatile because NIO completion-handler threads read this field. */
	private volatile T _client;
	
	/** Ensures {@link #close()} body executes at most once. */
	private final AtomicBoolean _closed = new AtomicBoolean();
	
	private ByteBuffer _readingBuffer;
	private ByteBuffer[] _writingBuffers;
	
	/**
	 * Creates a connection with its required handlers and configuration.
	 * @param channel The {@code AsynchronousSocketChannel} used for communication.
	 * @param readHandler The {@code ReadHandler} for incoming data.
	 * @param writeHandler The {@code WriteHandler} for outgoing data.
	 * @param config The {@code ConnectionConfig} for this connection.
	 */
	public Connection(AsynchronousSocketChannel channel, ReadHandler<T> readHandler, WriteHandler<T> writeHandler, ConnectionConfig config)
	{
		_channel = channel;
		_readHandler = readHandler;
		_writeHandler = writeHandler;
		_config = config;
	}
	
	/**
	 * Sets the client associated with this connection.
	 * @param client The client to assign.
	 */
	public void setClient(T client)
	{
		_client = client;
	}
	
	// ------------------------------------------------------------------
	// Read operations.
	// ------------------------------------------------------------------
	
	/**
	 * Starts an asynchronous read using the current buffer when the channel is open.
	 */
	public void read()
	{
		if (_channel.isOpen())
		{
			_channel.read(_readingBuffer, _client, _readHandler);
		}
	}
	
	/**
	 * Recycles the read buffer, acquires a header buffer and begins reading when the channel is open.
	 */
	public void readHeader()
	{
		if (_channel.isOpen())
		{
			recycleReadBuffer();
			_readingBuffer = _config.resourcePool.getHeaderBuffer();
			read();
		}
	}
	
	/**
	 * Resizes the read buffer to the requested size and begins reading when the channel is open.
	 * @param size The number of bytes to read.
	 */
	public void read(int size)
	{
		if (_channel.isOpen())
		{
			_readingBuffer = _config.resourcePool.recycleAndGetNew(_readingBuffer, size);
			read();
		}
	}
	
	/**
	 * Retrieves the buffer used for reading.
	 * @return The current reading {@code ByteBuffer}.
	 */
	public ByteBuffer getReadingBuffer()
	{
		return _readingBuffer;
	}
	
	// ------------------------------------------------------------------
	// Write operations.
	// ------------------------------------------------------------------
	
	/**
	 * Assigns the given buffers and starts writing them when the channel is open.
	 * @param buffers The {@code ByteBuffer} array to send.
	 * @return {@code true} if the write started, {@code false} if the channel is closed.
	 */
	public boolean write(ByteBuffer[] buffers)
	{
		if (!_channel.isOpen())
		{
			return false;
		}
		
		_writingBuffers = buffers;
		write();
		return true;
	}
	
	/**
	 * Writes the current buffers, or notifies the client to finish writing when none are available.
	 */
	public void write()
	{
		if (_channel.isOpen() && (_writingBuffers != null))
		{
			_channel.write(_writingBuffers, 0, _writingBuffers.length, -1, TimeUnit.MILLISECONDS, _client, _writeHandler);
		}
		else if (_client != null)
		{
			_client.finishWriting();
		}
	}
	
	/**
	 * Releases the writing buffers back to the resource pool and clears the reference.
	 * @return {@code true} if buffers were recycled, {@code false} if there were none.
	 */
	public boolean releaseWritingBuffer()
	{
		final ByteBuffer[] buffers = _writingBuffers;
		if (buffers == null)
		{
			return false;
		}
		
		_writingBuffers = null;
		for (ByteBuffer buf : buffers)
		{
			_config.resourcePool.recycleBuffer(buf);
		}
		
		return true;
	}
	
	// ------------------------------------------------------------------
	// Lifecycle.
	// ------------------------------------------------------------------
	
	/**
	 * Closes the connection once, recycling buffers and shutting down the channel.
	 */
	public void close()
	{
		if (!_closed.compareAndSet(false, true))
		{
			return;
		}
		
		recycleReadBuffer();
		releaseWritingBuffer();
		
		try
		{
			if (_channel.isOpen())
			{
				_channel.close();
			}
		}
		catch (IOException ignored)
		{
			// Channel was already broken - nothing useful to do.
		}
		finally
		{
			_client = null;
		}
	}
	
	/**
	 * Indicates whether the underlying channel is open.
	 * @return {@code true} if open, {@code false} otherwise.
	 */
	public boolean isOpen()
	{
		return _channel.isOpen();
	}
	
	// ------------------------------------------------------------------
	// Accessors.
	// ------------------------------------------------------------------
	
	/**
	 * Retrieves the remote host address, or an empty string on {@code IOException}.
	 * @return The host address as a {@code String}.
	 */
	public String getRemoteAddress()
	{
		try
		{
			final InetSocketAddress remote = (InetSocketAddress) _channel.getRemoteAddress();
			return remote.getAddress().getHostAddress();
		}
		catch (IOException e)
		{
			return "";
		}
	}
	
	/**
	 * Retrieves the resource pool managing the connection buffers.
	 * @return The {@code ResourcePool}.
	 */
	public ResourcePool getResourcePool()
	{
		return _config.resourcePool;
	}
	
	/**
	 * Indicates whether the configuration drops packets.
	 * @return {@code true} if packets are dropped, {@code false} otherwise.
	 */
	public boolean dropPackets()
	{
		return _config.dropPackets;
	}
	
	/**
	 * Retrieves the queue threshold above which packets are dropped.
	 * @return The drop threshold as an {@code int}.
	 */
	public int dropPacketThreshold()
	{
		return _config.dropPacketThreshold;
	}
	
	// ------------------------------------------------------------------
	// Internal helpers.
	// ------------------------------------------------------------------
	
	/**
	 * Releases the read buffer back to the resource pool and clears the reference.
	 */
	private void recycleReadBuffer()
	{
		final ByteBuffer buf = _readingBuffer;
		if (buf != null)
		{
			_readingBuffer = null;
			_config.resourcePool.recycleBuffer(buf);
		}
	}
}
