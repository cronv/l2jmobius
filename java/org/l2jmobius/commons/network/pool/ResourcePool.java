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
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.l2jmobius.commons.network.ConnectionConfig;

/**
 * Manages multiple {@code BufferPool} instances indexed by buffer size to facilitate efficient {@code ByteBuffer} reuse.<br>
 * It maintains parallel data structures to support both ceiling lookups and O(1) recycling for consistent performance.
 * @author JoeAlisson, Mobius
 */
public class ResourcePool
{
	/** Ordered map for ceiling-entry lookups (find smallest pool >= requested size). */
	private final ConcurrentSkipListMap<Integer, BufferPool> _sizedPools = new ConcurrentSkipListMap<>();
	
	/** Fast O(1) exact-capacity lookup used by the recycle path. */
	private final ConcurrentHashMap<Integer, BufferPool> _exactPools = new ConcurrentHashMap<>();
	
	private volatile boolean _autoExpandCapacity = true;
	private volatile boolean _initBufferPools = false;
	private volatile float _initBufferPoolFactor = 0;
	private volatile int _bufferSegmentSize = 64;
	
	/**
	 * Creates a new instance of the {@code ResourcePool}.<br>
	 * This initializes the internal data structures for buffer management.
	 */
	public ResourcePool()
	{
	}
	
	/**
	 * Retrieves a {@code ByteBuffer} for the header data.<br>
	 * This method uses the size defined in {@code ConnectionConfig}.
	 * @return A {@code ByteBuffer} with the correct capacity for headers.
	 */
	public ByteBuffer getHeaderBuffer()
	{
		return getSizedBuffer(ConnectionConfig.HEADER_SIZE);
	}
	
	/**
	 * Retrieves a {@code ByteBuffer} from the pool.<br>
	 * This method finds the smallest available buffer that can fit the requested size.<br>
	 * It uses internal logic to determine the correct capacity for the buffer.
	 * @param size The minimum capacity required for the buffer.
	 * @return A {@code ByteBuffer} ready for use.
	 */
	public ByteBuffer getBuffer(int size)
	{
		return getSizedBuffer(determineBufferSize(size));
	}
	
	/**
	 * This method recycles an existing {@code ByteBuffer} and provides a new one.<br>
	 * It checks if the provided {@code buffer} matches the required size for reuse.<br>
	 * If it does not match, the old {@code buffer} is recycled into the pool.<br>
	 * A new {@code ByteBuffer} is then returned with the requested limit.
	 * @param buffer The existing {@code ByteBuffer} to potentially recycle.
	 * @param newSize The desired size for the resulting {@code ByteBuffer}.
	 * @return A {@code ByteBuffer} ready for use with the specified limit.
	 */
	public ByteBuffer recycleAndGetNew(ByteBuffer buffer, int newSize)
	{
		final int poolSize = determineBufferSize(newSize);
		if (buffer != null)
		{
			if (buffer.clear().limit() == poolSize)
			{
				return buffer.limit(newSize);
			}
			
			recycleBuffer(buffer);
		}
		
		return getSizedBuffer(poolSize).limit(newSize);
	}
	
	/**
	 * Retrieves a {@code ByteBuffer} from the appropriate pool based on the requested size.<br>
	 * This method finds the smallest available pool that can accommodate the size.<br>
	 * It handles buffer expansion and creates new pools if necessary.
	 * @param size The required capacity for the buffer.
	 * @return A {@code ByteBuffer} with at least the requested capacity.
	 */
	private ByteBuffer getSizedBuffer(int size)
	{
		final Entry<Integer, BufferPool> entry = _sizedPools.ceilingEntry(size);
		if (entry != null)
		{
			final BufferPool pool = entry.getValue();
			if (_autoExpandCapacity)
			{
				if (_initBufferPools)
				{
					if (pool.isEmpty())
					{
						pool.expandCapacity(_initBufferPoolFactor, pool.getMaxSize());
					}
				}
				else if (pool.isFull())
				{
					pool.expandCapacity(_initBufferPoolFactor, pool.getMaxSize());
				}
			}
			
			final ByteBuffer buffer = pool.get();
			if (buffer != null)
			{
				return buffer;
			}
		}
		
		// No pool or pool was empty and no expansion produced a buffer - allocate directly.
		if (entry == null)
		{
			// Unknown size: register a new pool so future requests are served from the pool.
			final BufferPool pool = new BufferPool(10, size);
			if (_initBufferPools)
			{
				pool.initialize(_initBufferPoolFactor);
			}
			
			_sizedPools.putIfAbsent(size, pool);
			_exactPools.putIfAbsent(size, _sizedPools.get(size));
		}
		
		return ByteBuffer.allocateDirect(size).order(ByteOrder.LITTLE_ENDIAN);
	}
	
	/**
	 * Calculates the appropriate buffer size for a given request.<br>
	 * It looks for the smallest available pool that fits the requested {@code size}.<br>
	 * If no suitable pool exists, it creates and registers a new one.
	 * @param size The requested buffer capacity.
	 * @return The determined buffer size to be used.
	 */
	private int determineBufferSize(int size)
	{
		final Entry<Integer, BufferPool> entry = _sizedPools.ceilingEntry(size);
		if (entry != null)
		{
			return entry.getKey();
		}
		
		// Unknown size - register a new pool.
		final BufferPool pool = new BufferPool(10, size);
		if (_initBufferPools)
		{
			pool.initialize(_initBufferPoolFactor);
		}
		
		_sizedPools.putIfAbsent(size, pool);
		_exactPools.putIfAbsent(size, _sizedPools.get(size));
		return size;
	}
	
	/**
	 * Returns a {@code ByteBuffer} to its corresponding pool.<br>
	 * This method checks if the buffer belongs to an existing pool based on its capacity.<br>
	 * If a matching pool is found, it calls the recycle logic for that pool.
	 * @param buffer The {@code ByteBuffer} to be returned to the pool.
	 */
	public void recycleBuffer(ByteBuffer buffer)
	{
		if (buffer == null)
		{
			return;
		}
		
		final BufferPool pool = _exactPools.get(buffer.capacity());
		if (pool != null)
		{
			pool.recycle(buffer);
		}
	}
	
	/**
	 * Adds a new {@code BufferPool} to the internal collections.<br>
	 * This method registers a pool for both exact and ceiling lookups.<br>
	 * It uses the provided {@code bufferSize} as the unique key.
	 * @param bufferSize The capacity of the buffers in this pool.
	 * @param bufferPool The {@code BufferPool} instance to register.
	 */
	public void addBufferPool(int bufferSize, BufferPool bufferPool)
	{
		_sizedPools.putIfAbsent(bufferSize, bufferPool);
		_exactPools.putIfAbsent(bufferSize, bufferPool);
	}
	
	/**
	 * Returns the total number of buffer pools currently managed.<br>
	 * This count represents the number of entries in the {@code _sizedPools} map.
	 * @return The current size of the pool collection.
	 */
	public int bufferPoolSize()
	{
		return _sizedPools.size();
	}
	
	/**
	 * Sets up the initial state for buffer management.<br>
	 * This method configures whether capacity expands automatically.<br>
	 * It also determines if existing pools should be initialized based on the provided factor.
	 * @param autoExpandCapacity Set to {@code true} to allow automatic expansion of buffers.
	 * @param initBufferPoolFactor The multiplier used to determine initial pool sizes.
	 */
	public void initializeBuffers(boolean autoExpandCapacity, float initBufferPoolFactor)
	{
		_autoExpandCapacity = autoExpandCapacity;
		_initBufferPoolFactor = initBufferPoolFactor;
		_initBufferPools = initBufferPoolFactor > 0;
		if (_initBufferPools)
		{
			_sizedPools.values().forEach(pool -> pool.initialize(initBufferPoolFactor));
		}
	}
	
	/**
	 * Retrieves the current segment size.<br>
	 * This value represents the size of a single buffer segment.
	 * @return The current {@code int} value for the buffer segment size.
	 */
	public int getSegmentSize()
	{
		return _bufferSegmentSize;
	}
	
	/**
	 * Sets the default segment size for buffers.<br>
	 * This value determines how data is divided into segments.
	 * @param size The new segment size to apply.
	 */
	public void setBufferSegmentSize(int size)
	{
		_bufferSegmentSize = size;
	}
	
	/**
	 * Retrieves a summary of the current buffer pool statistics.<br>
	 * This method iterates through all pools in {@code _sizedPools}.<br>
	 * It returns a formatted string containing information for each pool.
	 * @return A {@code String} representing the status of all internal buffer pools.
	 */
	public String stats()
	{
		final StringBuilder sb = new StringBuilder();
		for (BufferPool pool : _sizedPools.values())
		{
			sb.append(pool.toString()).append(System.lineSeparator());
		}
		
		return sb.toString();
	}
}
