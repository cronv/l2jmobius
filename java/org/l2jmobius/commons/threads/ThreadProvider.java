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
package org.l2jmobius.commons.threads;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is an implementation of {@code ThreadFactory}.<br>
 * It allows for customizing the thread name prefix, priority, and daemon status when creating new threads.
 * @author Mobius
 * @since October 18th 2022
 */
public class ThreadProvider implements ThreadFactory
{
	private final AtomicInteger _id = new AtomicInteger();
	private final String _prefix;
	private final int _priority;
	private final boolean _daemon;
	
	/**
	 * Creates a new {@code ThreadProvider}.<br>
	 * This constructor sets the thread priority to {@code ThreadPriority.PRIORITY_5}.<br>
	 * It also sets the daemon status to {@code false}.
	 * @param prefix The string used as a prefix for naming new threads.
	 */
	public ThreadProvider(String prefix)
	{
		this(prefix, ThreadPriority.PRIORITY_5, false);
	}
	
	/**
	 * Creates a new {@code ThreadProvider}.<br>
	 * This constructor sets the thread priority to {@code ThreadPriority.PRIORITY_5}.
	 * @param prefix The string used as a prefix for naming new threads.
	 * @param daemon Set to {@code true} if the threads should be daemon threads, otherwise {@code false}.
	 */
	public ThreadProvider(String prefix, boolean daemon)
	{
		this(prefix, ThreadPriority.PRIORITY_5, daemon);
	}
	
	/**
	 * Creates a new {@code ThreadProvider}.<br>
	 * This constructor sets the thread priority.<br>
	 * It defaults to non-daemon threads.
	 * @param prefix The string used as a prefix for naming new threads.
	 * @param priority The {@code ThreadPriority} level assigned to new threads.
	 */
	public ThreadProvider(String prefix, ThreadPriority priority)
	{
		this(prefix, priority, false);
	}
	
	/**
	 * Creates a new {@code ThreadProvider} with custom settings.<br>
	 * This constructor sets the name prefix, thread priority, and daemon status.
	 * @param prefix The string used as a prefix for naming new threads.
	 * @param priority The {@code ThreadPriority} level assigned to new threads.
	 * @param daemon Set to {@code true} if the threads should be daemon threads, otherwise {@code false}.
	 */
	public ThreadProvider(String prefix, ThreadPriority priority, boolean daemon)
	{
		_prefix = prefix + " ";
		_priority = priority.getId();
		_daemon = daemon;
	}
	
	/**
	 * Creates a new {@code Thread} for the given task.<br>
	 * The thread is assigned a unique name based on the internal prefix.<br>
	 * It also inherits the priority and daemon status of this provider.<br>
	 * The returned thread is not started; starting it is the caller's responsibility.
	 * @param runnable The task to be executed by the new thread.
	 * @return The newly created {@code Thread}.
	 */
	@Override
	public Thread newThread(Runnable runnable)
	{
		final Thread thread = new Thread(runnable, _prefix + _id.incrementAndGet());
		thread.setPriority(_priority);
		thread.setDaemon(_daemon);
		return thread;
	}
}
