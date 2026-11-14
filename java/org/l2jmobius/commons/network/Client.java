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

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.l2jmobius.commons.network.buffer.ReadBuffer;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.commons.network.packet.WritablePacket;
import org.l2jmobius.commons.network.pool.ResourcePool;
import org.l2jmobius.commons.network.protection.FloodGuard;
import org.l2jmobius.commons.network.protection.ProtectionConfig;

/**
 * Abstract client that manages packet I/O over an owned {@code Connection}, using a shared queue for fair sending.
 * @param <T> The {@code Connection} subtype bound to this client.
 * @author JoeAlisson, Mobius
 */
public abstract class Client<T extends Connection<?>>
{
	/**
	 * Global fair-send queue shared across all client instances.<br>
	 * Unbounded; clients add themselves when they start sending and are polled by the completing write to find the next sender.
	 */
	private static final ConcurrentLinkedQueue<Client<?>> PENDING_CLIENTS = new ConcurrentLinkedQueue<>();
	
	private final T _connection;
	private final Queue<WritablePacket<? extends Client<T>>> _packetsToWrite = new ConcurrentLinkedQueue<>();
	/** Inbound packets awaiting execution; drained one at a time so packets from one client never run concurrently. */
	private final Queue<Runnable> _packetsToExecute = new LinkedBlockingQueue<>(ProtectionConfig.PACKET_QUEUE_LIMIT);
	private final AtomicBoolean _executing = new AtomicBoolean();
	private final AtomicBoolean _writing = new AtomicBoolean();
	private final AtomicBoolean _disconnecting = new AtomicBoolean();
	private final AtomicBoolean _closing = new AtomicBoolean();
	private final AtomicInteger _estimateQueueSize = new AtomicInteger();
	private final AtomicInteger _dataSentSize = new AtomicInteger();
	private final FloodGuard _floodGuard = new FloodGuard();
	
	// Read-side state (accessed only on the single async-read thread for this client).
	private boolean _readingPayload;
	private int _expectedReadSize;
	
	/**
	 * Creates a client bound to the given connection, which must be open.
	 * @param connection The active connection to associate with this client.
	 */
	protected Client(T connection)
	{
		if ((connection == null) || !connection.isOpen())
		{
			throw new IllegalArgumentException("The connection is null or closed.");
		}
		
		_connection = connection;
	}
	
	/**
	 * Queues a packet for sending and triggers fair dispatch, ignoring it when disconnected, null or droppable.
	 * @param packet The packet to send.
	 */
	protected void writePacket(WritablePacket<? extends Client<T>> packet)
	{
		if (!isConnected() || (packet == null) || packetCanBeDropped(packet))
		{
			return;
		}
		
		_estimateQueueSize.incrementAndGet();
		_packetsToWrite.add(packet);
		writeFairPacket();
	}
	
	/**
	 * Queues a collection of packets for sending and triggers fair dispatch, ignoring it when disconnected, null or empty.
	 * @param packets The packets to send.
	 */
	protected void writePackets(Collection<WritablePacket<? extends Client<T>>> packets)
	{
		if (!isConnected() || (packets == null) || packets.isEmpty())
		{
			return;
		}
		
		_estimateQueueSize.addAndGet(packets.size());
		_packetsToWrite.addAll(packets);
		writeFairPacket();
	}
	
	/**
	 * Checks whether the packet may be discarded because dropping is enabled, the queue is over the threshold and the packet permits it.
	 * @param packet The packet to evaluate.
	 * @return {@code true} if the packet can be dropped, {@code false} otherwise.
	 */
	@SuppressWarnings(
	{
		"unchecked",
		"rawtypes"
	})
	private boolean packetCanBeDropped(WritablePacket packet)
	{
		return _connection.dropPackets() && (_estimateQueueSize.get() > _connection.dropPacketThreshold()) && packet.canBeDropped(this);
	}
	
	/**
	 * Starts fair dispatch via a compare-and-set on the write flag when no other thread is writing.
	 */
	private void writeFairPacket()
	{
		if (_writing.compareAndSet(false, true))
		{
			sendFairPacket();
		}
	}
	
	/**
	 * Enqueues this client and dispatches the next pending write turn.
	 */
	private void sendFairPacket()
	{
		PENDING_CLIENTS.offer(this);
		final Client<?> nextClient = PENDING_CLIENTS.poll();
		if (nextClient != null)
		{
			nextClient.writeNextPacket();
		}
	}
	
	/**
	 * Writes the next queued packet, or releases the write resource and disconnects when closing once the queue drains.
	 */
	private void writeNextPacket()
	{
		final WritablePacket<? extends Client<T>> packet = _packetsToWrite.poll();
		if (packet == null)
		{
			releaseWritingResource();
			
			// A packet may have been queued by another thread between the empty poll above and the release of the writing flag; that thread's CAS failed, so this thread must restart the send loop.
			if (!_packetsToWrite.isEmpty())
			{
				writeFairPacket();
				return;
			}
			
			// A close(packet) may have queued its final packet concurrently, or another thread may be writing it right now; that thread's completion path will come back here and disconnect.
			if (_closing.get() && !hasPendingWrite())
			{
				disconnect();
			}
		}
		else
		{
			_estimateQueueSize.decrementAndGet();
			write(packet);
		}
	}
	
	/**
	 * Serializes, encrypts and writes a single packet, discarding it on failure.
	 * @param packet The packet to send.
	 */
	@SuppressWarnings(
	{
		"unchecked",
		"rawtypes"
	})
	private void write(WritablePacket packet)
	{
		boolean written = false;
		WriteBuffer buffer = null;
		try
		{
			buffer = packet.writeData(this);
			
			final int payloadSize = buffer.limit() - ConnectionConfig.HEADER_SIZE;
			if (payloadSize <= 0)
			{
				return;
			}
			
			if (encrypt(buffer, ConnectionConfig.HEADER_SIZE, payloadSize))
			{
				final int bufferLimit = buffer.limit();
				_dataSentSize.set(bufferLimit);
				if (bufferLimit <= ConnectionConfig.HEADER_SIZE)
				{
					return;
				}
				
				packet.writeHeader(buffer, bufferLimit);
				written = _connection.write(buffer.toByteBuffers());
			}
		}
		catch (Exception e)
		{
			// Intentionally silent - a broken packet must not crash the I/O thread.
		}
		finally
		{
			if (!written)
			{
				handleNotWritten(buffer);
			}
		}
	}
	
	/**
	 * Releases the buffer on a failed write and resumes fair dispatch if still connected.
	 * @param buffer The buffer whose data was not written.
	 */
	private void handleNotWritten(WriteBuffer buffer)
	{
		if (!releaseWritingResource() && (buffer != null))
		{
			buffer.releaseResources();
		}
		
		if (isConnected())
		{
			writeFairPacket();
		}
	}
	
	/**
	 * Begins reading a new packet header of {@code ConnectionConfig.HEADER_SIZE} bytes.
	 */
	public void read()
	{
		_expectedReadSize = ConnectionConfig.HEADER_SIZE;
		_readingPayload = false;
		_connection.readHeader();
	}
	
	/**
	 * Begins reading a payload of the given size.
	 * @param dataSize The number of payload bytes to read.
	 */
	public void readPayload(int dataSize)
	{
		_expectedReadSize = dataSize;
		_readingPayload = true;
		_connection.read(dataSize);
	}
	
	/**
	 * Closes the connection with no final packet.
	 */
	public void close()
	{
		close(null);
	}
	
	/**
	 * Clears the queue, marks the client closing, optionally queues a final packet, and triggers dispatch.
	 * @param packet The final packet to send before closing, or {@code null}.
	 */
	public void close(WritablePacket<? extends Client<T>> packet)
	{
		if (!isConnected())
		{
			return;
		}
		
		_packetsToWrite.clear();
		if (packet != null)
		{
			_packetsToWrite.add(packet);
		}
		
		_closing.set(true);
		
		writeFairPacket();
	}
	
	/**
	 * Subtracts the sent byte count and resumes writing on the connection.
	 * @param result The number of bytes already sent.
	 */
	public void resumeSend(int result)
	{
		_dataSentSize.addAndGet(-result);
		_connection.write();
	}
	
	/**
	 * Checks whether a write is still queued or in progress.
	 * @return {@code true} if a packet is queued or another thread is writing, {@code false} otherwise.
	 */
	private boolean hasPendingWrite()
	{
		return !_packetsToWrite.isEmpty() || _writing.get();
	}
	
	/**
	 * Queues an inbound packet task for this client.<br>
	 * Packets from one client are executed one at a time, so a check performed by one packet cannot be invalidated by another packet of the same client running concurrently.
	 * @param task The packet execution task to queue.
	 * @return {@code true} if the caller must start draining the queue, {@code false} if another thread is already draining it or the queue is full.
	 */
	public boolean queuePacketExecution(Runnable task)
	{
		if (!_packetsToExecute.offer(task))
		{
			// Dropping the packet beats exhausting the heap; the read side rate limit is what keeps a flood from reaching this point.
			return false;
		}
		
		return _executing.compareAndSet(false, true);
	}
	
	/**
	 * Runs queued inbound packet tasks until none remain.
	 */
	public void drainPacketExecution()
	{
		while (true)
		{
			// The flag is released in a finally, so a task that throws cannot leave this client unable to ever execute another packet.
			try
			{
				Runnable task;
				while ((task = _packetsToExecute.poll()) != null)
				{
					task.run();
				}
			}
			finally
			{
				_executing.set(false);
			}
			
			// A packet may have been queued by another thread between the empty poll above and the release of the flag; that thread's CAS failed, so this thread must drain it.
			if (_packetsToExecute.isEmpty() || !_executing.compareAndSet(false, true))
			{
				return;
			}
		}
	}
	
	/**
	 * Releases the connection write buffer and dispatches the next fair packet.
	 */
	public void finishWriting()
	{
		_connection.releaseWritingBuffer();
		sendFairPacket();
	}
	
	/**
	 * Releases the connection write buffer and clears the writing flag.
	 * @return {@code true} if the buffer was released, {@code false} otherwise.
	 */
	private boolean releaseWritingResource()
	{
		final boolean released = _connection.releaseWritingBuffer();
		_writing.set(false);
		return released;
	}
	
	/**
	 * Disconnects once, invoking {@code onDisconnection()}, clearing the queue and closing the connection.
	 */
	public void disconnect()
	{
		if (_disconnecting.compareAndSet(false, true))
		{
			try
			{
				onDisconnection();
			}
			finally
			{
				_packetsToWrite.clear();
				_connection.close();
			}
		}
	}
	
	/**
	 * Retrieves the underlying connection.
	 * @return The {@code T} connection.
	 */
	public T getConnection()
	{
		return _connection;
	}
	
	/**
	 * Retrieves the tracked size of data sent.
	 * @return The data sent size in bytes.
	 */
	public int getDataSentSize()
	{
		return _dataSentSize.get();
	}
	
	/**
	 * Retrieves the remote host address, or an empty string when there is no connection.
	 * @return The remote host address.
	 */
	public String getHostAddress()
	{
		return _connection == null ? "" : _connection.getRemoteAddress();
	}
	
	/**
	 * Indicates whether the connection is open and not closing.
	 * @return {@code true} if connected, {@code false} otherwise.
	 */
	public boolean isConnected()
	{
		return _connection.isOpen() && !_closing.get();
	}
	
	/**
	 * Retrieves the estimated outbound queue size.
	 * @return The number of packets waiting in the queue.
	 */
	public int getEstimateQueueSize()
	{
		return _estimateQueueSize.get();
	}
	
	/**
	 * Retrieves the resource pool of the connection.
	 * @return The {@code ResourcePool} instance.
	 */
	public ResourcePool getResourcePool()
	{
		return _connection.getResourcePool();
	}
	
	/**
	 * Retrieves the guard metering the incoming packet rate of this connection.
	 * @return The {@code FloodGuard} of this client.
	 */
	public FloodGuard getFloodGuard()
	{
		return _floodGuard;
	}
	
	/**
	 * Indicates whether a payload is currently being read.
	 * @return {@code true} if reading a payload, {@code false} otherwise.
	 */
	public boolean isReadingPayload()
	{
		return _readingPayload;
	}
	
	/**
	 * Subtracts the bytes already read and resumes reading.
	 * @param bytesRead The number of bytes successfully read.
	 */
	public void resumeRead(int bytesRead)
	{
		_expectedReadSize -= bytesRead;
		_connection.read();
	}
	
	/**
	 * Retrieves the number of bytes expected for the current read.
	 * @return The expected read size in bytes.
	 */
	public int getExpectedReadSize()
	{
		return _expectedReadSize;
	}
	
	/**
	 * Encrypts the outbound buffer region in place.
	 * @param data The outbound buffer to encrypt.
	 * @param offset The offset where the region starts.
	 * @param size The number of bytes to encrypt.
	 * @return {@code true} if encryption succeeded, {@code false} to abort sending.
	 */
	public abstract boolean encrypt(WriteBuffer data, int offset, int size);
	
	/**
	 * Decrypts the inbound buffer region in place.
	 * @param data The inbound buffer to decrypt.
	 * @param offset The offset where the region starts.
	 * @param size The number of bytes to decrypt.
	 * @return {@code true} if decryption succeeded, {@code false} to abort processing.
	 */
	public abstract boolean decrypt(ReadBuffer data, int offset, int size);
	
	/**
	 * Invoked once on disconnect to persist state and release resources; no packets may be sent afterwards.
	 */
	protected abstract void onDisconnection();
	
	/**
	 * Invoked once after the connection is accepted; must not block and may send outbound packets.
	 */
	public abstract void onConnected();
}
