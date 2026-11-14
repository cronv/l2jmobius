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

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.network.Client;
import org.l2jmobius.commons.network.Connection;
import org.l2jmobius.commons.network.ConnectionConfig;
import org.l2jmobius.commons.network.protection.ProtectionConfig;

/**
 * This class provides a thread-pool-based executor for handling incoming network packets.<br>
 * It ensures that the {@code run()} logic of a {@code ReadablePacket} executes off the I/O thread after successful parsing.
 * @param <T> the client type associated with the packets being executed
 * @author Mobius
 */
public class PacketExecutor<T extends Client<Connection<T>>>
{
	private static final Logger LOGGER = Logger.getLogger(PacketExecutor.class.getName());
	
	private final ThreadPoolExecutor _executor;
	
	/**
	 * Creates a new {@code PacketExecutor} instance.<br>
	 * This constructor initializes the internal thread pool using settings from the provided configuration.
	 * @param config the {@code ConnectionConfig} used to set the pool size and thread priority
	 */
	public PacketExecutor(ConnectionConfig config)
	{
		// The queue is bounded so that packets arriving faster than they are executed cannot grow the heap without limit.
		// Maximum pool size equals the core size because a bounded queue would otherwise spawn threads up to the maximum once it fills.
		_executor = new ThreadPoolExecutor(config.threadPoolSize, config.threadPoolSize, 1, TimeUnit.MINUTES, new LinkedBlockingQueue<>(ProtectionConfig.PACKET_QUEUE_LIMIT), new PacketThreadFactory("PacketExecutor", config.threadPriority), new DiscardPolicy());
	}
	
	/**
	 * This method processes a network packet.<br>
	 * It submits the {@code ReadablePacket} to a thread pool for execution.<br>
	 * The logic runs off the main I/O thread to improve performance.
	 * @param packet the {@code ReadablePacket} to be executed
	 */
	public void execute(ReadablePacket<T> packet)
	{
		try
		{
			// Queue on the client so its packets run one at a time; a check made by one packet cannot be invalidated by another packet of the same client running beside it.
			final T client = packet.getClient();
			if (client == null)
			{
				_executor.execute(new PacketRunnable<>(packet));
				return;
			}
			
			if (client.queuePacketExecution(new PacketRunnable<>(packet)))
			{
				_executor.execute(client::drainPacketExecution);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Failed to submit " + packet.getClass().getSimpleName(), e);
		}
	}
	
	/**
	 * Discards packets that arrive once the execution queue is full, logging at most one warning per interval.
	 */
	private static class DiscardPolicy implements RejectedExecutionHandler
	{
		private static final long LOG_INTERVAL = 60000;
		
		private final AtomicLong _lastLog = new AtomicLong();
		
		@Override
		public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor)
		{
			// Discarding the packet beats exhausting the heap; the read side rate limit is what keeps a flood from reaching this point.
			final long currentTime = System.currentTimeMillis();
			final long lastLog = _lastLog.get();
			if (((currentTime - lastLog) > LOG_INTERVAL) && _lastLog.compareAndSet(lastLog, currentTime))
			{
				LOGGER.warning("PacketExecutor: Queue limit of " + ProtectionConfig.PACKET_QUEUE_LIMIT + " reached, discarding packets.");
			}
		}
	}
	
	private static class PacketRunnable<T extends Client<Connection<T>>> implements Runnable
	{
		private final ReadablePacket<T> _packet;
		
		public PacketRunnable(ReadablePacket<T> packet)
		{
			_packet = packet;
		}
		
		@Override
		public void run()
		{
			try
			{
				_packet.run();
			}
			catch (Throwable t)
			{
				final Thread currentThread = Thread.currentThread();
				final UncaughtExceptionHandler handler = currentThread.getUncaughtExceptionHandler();
				if (handler != null)
				{
					handler.uncaughtException(currentThread, t);
				}
				else
				{
					LOGGER.log(Level.SEVERE, "Uncaught exception in " + _packet.getClass().getSimpleName(), t);
				}
			}
		}
	}
}
