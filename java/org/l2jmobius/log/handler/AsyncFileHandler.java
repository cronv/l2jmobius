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
package org.l2jmobius.log.handler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.ErrorManager;
import java.util.logging.FileHandler;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Asynchronous file log handler that offloads disk writes to a dedicated worker thread.
 * @author Mobius
 */
public abstract class AsyncFileHandler extends FileHandler
{
	private static final int QUEUE_CAPACITY = 8192;
	private static final long DRAIN_POLL_MILLIS = 200;
	private static final long CLOSE_JOIN_MILLIS = 5000;
	
	private static final Formatter PASSTHROUGH = new Formatter()
	{
		@Override
		public String format(LogRecord logRecord)
		{
			return logRecord.getMessage();
		}
	};
	
	private static final Set<AsyncFileHandler> INSTANCES = ConcurrentHashMap.newKeySet();
	private static final AtomicBoolean SHUTDOWN_HOOK_INSTALLED = new AtomicBoolean();
	
	private final LinkedBlockingQueue<String> _queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
	private final AtomicLong _dropped = new AtomicLong();
	private final Thread _worker;
	private volatile boolean _running = true;
	private final Filter _userFilter;
	private final Formatter _userFormatter;
	private final Level _userLevel;
	
	/**
	 * Captures the configured filter, formatter and level, then starts the daemon worker thread.
	 * @throws IOException if the underlying file handler cannot be opened
	 */
	protected AsyncFileHandler() throws IOException
	{
		super();
		
		// Capture the JUL-configured filter/formatter/level.
		// We apply them on the producer thread so that live game state (Item counts, Player references) is sampled at call time, not later.
		_userFilter = getFilter();
		_userFormatter = getFormatter();
		
		final Level level = getLevel();
		_userLevel = level == null ? Level.ALL : level;
		
		// Disable filtering and formatting on the underlying StreamHandler path because the worker provides pre-formatted strings and the synthetic LogRecord lacks the logger name and parameters required by user filters.
		setFilter(null);
		setFormatter(PASSTHROUGH);
		setLevel(Level.ALL);
		
		_worker = new Thread(this::drain, getClass().getSimpleName() + "-Writer");
		_worker.setDaemon(true);
		_worker.start();
		
		INSTANCES.add(this);
		ensureShutdownHook();
	}
	
	/**
	 * Applies the configured level and filter, formats the record, and enqueues it without blocking, dropping on overflow.
	 * @param logRecord the record to format and enqueue
	 */
	@Override
	public void publish(LogRecord logRecord)
	{
		if (logRecord == null)
		{
			return;
		}
		
		// Producer-side level + filter check, using the originally configured values.
		if ((logRecord.getLevel().intValue() < _userLevel.intValue()) || (_userLevel == Level.OFF))
		{
			return;
		}
		
		if ((_userFilter != null) && !_userFilter.isLoggable(logRecord))
		{
			return;
		}
		
		final String formatted;
		try
		{
			formatted = _userFormatter.format(logRecord);
		}
		catch (Exception e)
		{
			reportError(null, e, ErrorManager.FORMAT_FAILURE);
			return;
		}
		
		// offer() - non-blocking; drop on overflow rather than stall the game thread.
		if (!_queue.offer(formatted))
		{
			_dropped.incrementAndGet();
		}
	}
	
	/**
	 * Worker loop that polls queued strings and writes them through the underlying handler until stopped and drained.
	 */
	private void drain()
	{
		while (_running || !_queue.isEmpty())
		{
			try
			{
				final String s = _queue.poll(DRAIN_POLL_MILLIS, TimeUnit.MILLISECONDS);
				if (s == null)
				{
					continue;
				}
				
				final LogRecord wrapper = new LogRecord(Level.INFO, s);
				super.publish(wrapper);
			}
			catch (InterruptedException ignored)
			{
				Thread.currentThread().interrupt();
				return;
			}
			catch (Exception e)
			{
				reportError(null, e, ErrorManager.WRITE_FAILURE);
			}
		}
		
		try
		{
			super.flush();
		}
		catch (Exception ignored)
		{
			// Ignored - shutdown path.
		}
	}
	
	/**
	 * Performs a best-effort flush of the underlying stream while the queue drains asynchronously.
	 */
	@Override
	public void flush()
	{
		// Nudge the worker to make progress, then flush the underlying stream.
		// The queue is drained asynchronously, so this is a best-effort flush.
		super.flush();
	}
	
	/**
	 * Stops the worker, awaits its completion, reports any dropped records, and closes the underlying handler.
	 * @throws SecurityException if a security manager denies the close
	 */
	@Override
	public void close() throws SecurityException
	{
		_running = false;
		try
		{
			_worker.join(CLOSE_JOIN_MILLIS);
		}
		catch (InterruptedException ignored)
		{
			Thread.currentThread().interrupt();
		}
		
		final long dropped = _dropped.get();
		if (dropped > 0)
		{
			// The logging system may be tearing down - go straight to stderr.
			System.err.println(getClass().getSimpleName() + ": dropped " + dropped + " log records due to queue overflow.");
		}
		
		INSTANCES.remove(this);
		super.close();
	}
	
	/**
	 * Installs, at most once, a JVM shutdown hook that closes every registered handler.
	 */
	private static void ensureShutdownHook()
	{
		if (SHUTDOWN_HOOK_INSTALLED.compareAndSet(false, true))
		{
			Runtime.getRuntime().addShutdownHook(new Thread(() ->
			{
				for (AsyncFileHandler handler : INSTANCES)
				{
					try
					{
						handler.close();
					}
					catch (Exception ignored)
					{
						// Ignored - JVM is exiting.
					}
				}
			}, "AsyncFileHandler-Shutdown"));
		}
	}
}
