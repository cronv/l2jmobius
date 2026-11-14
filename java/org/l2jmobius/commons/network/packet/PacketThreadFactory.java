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

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This factory creates and manages threads specifically for network server tasks.<br>
 * It provides custom naming conventions and priority management to improve thread identification and performance tuning.
 * @author BazookaRpm
 */
public class PacketThreadFactory implements ThreadFactory
{
	// Constants.
	private static final String DEFAULT_BASE_NAME = "Thread";
	private static final int INITIAL_SEQUENCE_VALUE = 1;
	private static final int STACK_SIZE = 0; // Use JVM default stack size.
	
	// Global pool sequence counter.
	private static final AtomicInteger POOL_SEQUENCE = new AtomicInteger(INITIAL_SEQUENCE_VALUE);
	
	// Thread naming and priority.
	private final AtomicInteger _threadSequence = new AtomicInteger(INITIAL_SEQUENCE_VALUE);
	private final String _threadPrefix;
	private final int _threadPriority;
	
	/**
	 * Creates a new {@code PacketThreadFactory} instance.<br>
	 * This constructor sets the naming prefix and thread priority.<br>
	 * It ensures that the provided values are safe for use by the factory.
	 * @param baseName The string used as the start of the thread name. If {@code null} or empty, it defaults to {@code Thread}.
	 * @param priority The priority level for new threads. This value is clamped between {@code Thread.MIN_PRIORITY} and {@code Thread.MAX_PRIORITY}.
	 */
	public PacketThreadFactory(String baseName, int priority)
	{
		final String safeBaseName = ((baseName == null) || baseName.isEmpty()) ? DEFAULT_BASE_NAME : baseName;
		_threadPrefix = safeBaseName + "-network-pool-" + POOL_SEQUENCE.getAndIncrement() + "-thread-";
		
		// Clamp priority to valid range.
		if (priority < Thread.MIN_PRIORITY)
		{
			_threadPriority = Thread.MIN_PRIORITY;
		}
		else if (priority > Thread.MAX_PRIORITY)
		{
			_threadPriority = Thread.MAX_PRIORITY;
		}
		else
		{
			_threadPriority = priority;
		}
	}
	
	/**
	 * Creates and configures a new {@code Thread}.<br>
	 * This method assigns the task to a new thread with a unique name.<br>
	 * It also sets the appropriate priority based on the factory settings.
	 * @param task The {@code Runnable} task to be executed by the new thread.
	 * @return A newly created {@code Thread} instance.
	 */
	@Override
	public Thread newThread(Runnable task)
	{
		final int threadIndex = nextIndex();
		final Thread thread = new Thread(null, task, _threadPrefix + threadIndex, STACK_SIZE);
		
		// Apply priority respecting thread group constraints.
		final ThreadGroup threadGroup = thread.getThreadGroup();
		final int groupMaxPriority = (threadGroup != null) ? threadGroup.getMaxPriority() : Thread.MAX_PRIORITY;
		final int effectivePriority = (_threadPriority > groupMaxPriority) ? groupMaxPriority : _threadPriority;
		
		thread.setPriority(effectivePriority);
		thread.setDaemon(false);
		
		return thread;
	}
	
	/**
	 * Retrieves the next unique sequence number for thread naming.<br>
	 * This method handles potential integer overflows.<br>
	 * It resets the counter to {@code INITIAL_SEQUENCE_VALUE} if it wraps around.
	 * @return the next available integer index.
	 */
	private int nextIndex()
	{
		final int currentValue = _threadSequence.getAndIncrement();
		return (currentValue == Integer.MIN_VALUE) ? _threadSequence.updateAndGet(x -> (x <= 0) ? INITIAL_SEQUENCE_VALUE : x) : currentValue; // Handle integer overflow by resetting to 1.
	}
}
