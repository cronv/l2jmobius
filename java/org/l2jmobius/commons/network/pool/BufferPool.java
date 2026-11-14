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
package org.l2jmobius.commons.network.pool;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class manages a pool of {@code ByteBuffer} objects to reduce memory allocation overhead.<br>
 * It uses an {@code ArrayDeque} and a {@code ReentrantLock} to provide thread-safe access to the buffers.
 * @author Mobius
 */
public class BufferPool
{
	private final ArrayDeque<ByteBuffer> _buffers;
	private final ReentrantLock _lock = new ReentrantLock();
	private final int _bufferSize;
	private int _maxSize;
	
	/**
	 * Creates a new {@code BufferPool} instance.<br>
	 * This initializes the pool with a specific capacity and buffer size.
	 * @param maxSize The maximum number of buffers allowed in the pool.
	 * @param bufferSize The size in bytes for each individual buffer.
	 */
	public BufferPool(int maxSize, int bufferSize)
	{
		_maxSize = maxSize;
		_bufferSize = bufferSize;
		_buffers = new ArrayDeque<>(maxSize);
	}
	
	/**
	 * Pre-allocates buffers into the pool based on a multiplier.<br>
	 * This method uses the {@code factor} to determine how many new buffers to create.<br>
	 * The total number of new buffers will not exceed the current {@code _maxSize}.
	 * @param factor The multiplier used to calculate the number of buffers to allocate.
	 */
	public void initialize(float factor)
	{
		final int amount = (int) Math.min(_maxSize, _maxSize * factor);
		_lock.lock();
		try
		{
			for (int i = 0; i < amount; i++)
			{
				_buffers.offer(ByteBuffer.allocateDirect(_bufferSize).order(ByteOrder.LITTLE_ENDIAN));
			}
		}
		finally
		{
			_lock.unlock();
		}
	}
	
	/**
	 * Retrieves a {@code ByteBuffer} from the pool.<br>
	 * This method is thread-safe.<br>
	 * It returns {@code null} if no buffers are available in the pool.
	 * @return the next available {@code ByteBuffer} or {@code null}
	 */
	public ByteBuffer get()
	{
		_lock.lock();
		try
		{
			return _buffers.poll();
		}
		finally
		{
			_lock.unlock();
		}
	}
	
	/**
	 * Returns a {@code ByteBuffer} to the pool for reuse.<br>
	 * The buffer is cleared before being added back.<br>
	 * This method returns {@code true} if the buffer was successfully recycled.<br>
	 * It returns {@code false} if the pool has reached its maximum capacity.
	 * @param buffer The {@code ByteBuffer} to return to the pool.
	 * @return {@code true} if successful, or {@code false} otherwise.
	 */
	public boolean recycle(ByteBuffer buffer)
	{
		_lock.lock();
		try
		{
			if (_buffers.size() < _maxSize)
			{
				_buffers.offer(buffer.clear());
				return true;
			}
			
			return false;
		}
		finally
		{
			_lock.unlock();
		}
	}
	
	/**
	 * Increases the total capacity of the buffer pool.<br>
	 * This method adds new {@code ByteBuffer} objects to the internal collection.<br>
	 * It ensures that the new size does not exceed the specified limit.
	 * @param factor The multiplier used to calculate how many new buffers to create.
	 * @param limit The maximum allowed capacity for the pool.
	 */
	public void expandCapacity(float factor, int limit)
	{
		_lock.lock();
		try
		{
			if (_maxSize > limit)
			{
				return;
			}
			
			if (factor > 0)
			{
				final int amount = (int) (_maxSize * factor);
				for (int i = 0; i < amount; i++)
				{
					_buffers.offer(ByteBuffer.allocateDirect(_bufferSize).order(ByteOrder.LITTLE_ENDIAN));
				}
				
				_maxSize += amount;
			}
			else
			{
				_maxSize *= 2;
			}
		}
		finally
		{
			_lock.unlock();
		}
	}
	
	/**
	 * Retrieves the maximum number of buffers allowed in the pool.<br>
	 * This method is thread-safe and uses a {@code ReentrantLock}.
	 * @return the current value of {@code _maxSize} as an {@code int}
	 */
	public int getMaxSize()
	{
		_lock.lock();
		try
		{
			return _maxSize;
		}
		finally
		{
			_lock.unlock();
		}
	}
	
	/**
	 * Checks if the pool has reached its maximum capacity.<br>
	 * This method returns {@code true} if the number of buffers is equal to or greater than {@code _maxSize}.
	 * @return {@code true} if the pool is full, {@code false} otherwise.
	 */
	public boolean isFull()
	{
		_lock.lock();
		try
		{
			return _buffers.size() >= _maxSize;
		}
		finally
		{
			_lock.unlock();
		}
	}
	
	/**
	 * Checks if the pool contains no buffers.<br>
	 * This method is thread-safe.
	 * @return {@code true} if the pool is empty, {@code false} otherwise.
	 */
	public boolean isEmpty()
	{
		_lock.lock();
		try
		{
			return _buffers.isEmpty();
		}
		finally
		{
			_lock.unlock();
		}
	}
	
	/**
	 * Returns a string representation of the {@code BufferPool}.<br>
	 * This includes the maximum size, buffer size, and current count.
	 * @return A formatted string describing the pool state.
	 */
	@Override
	public String toString()
	{
		_lock.lock();
		try
		{
			return "Pool {maxSize=" + _maxSize + ", bufferSize=" + _bufferSize + ", currentSize=" + _buffers.size() + '}';
		}
		finally
		{
			_lock.unlock();
		}
	}
}
