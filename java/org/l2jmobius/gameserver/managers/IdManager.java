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
package org.l2jmobius.gameserver.managers;

import java.util.BitSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.IdManagerConfig;
import org.l2jmobius.gameserver.util.PrimeCapacityAllocator;

/**
 * This class manages the generation and assignment of unique identifiers for game objects.<br>
 * It ensures that every object in the server receives a distinct {@code long} ID.<br>
 * Use this manager to coordinate ID allocation across different systems.
 * @author Mobius
 */
public class IdManager
{
	private static final Logger LOGGER = Logger.getLogger(IdManager.class.getName());
	
	private static final int TOTAL_ID_COUNT = IdManagerConfig.LAST_OBJECT_ID - IdManagerConfig.FIRST_OBJECT_ID;
	
	private BitSet _freeIds;
	private int _freeIdCount;
	private int _nextFreeId;
	private final Lock _lock = new ReentrantLock();
	
	/**
	 * Initializes the {@code IdManager} instance.<br>
	 * This constructor cleans up database records and prepares the internal ID pool.<br>
	 * It populates the initial set of free IDs based on existing data.
	 */
	public IdManager()
	{
		// Cleanup database.
		DatabaseIdManager.cleanDatabase();
		DatabaseIdManager.cleanCharacterStatus();
		DatabaseIdManager.cleanTimestamps();
		
		// Initialize BitSet with initial capacity or next prime.
		try
		{
			_freeIds = new BitSet(PrimeCapacityAllocator.nextCapacity(IdManagerConfig.INITIAL_CAPACITY));
			_freeIds.clear();
			_freeIdCount = TOTAL_ID_COUNT;
			
			// Register used ids.
			for (int usedObjectId : DatabaseIdManager.getUsedIds())
			{
				final int objectId = usedObjectId - IdManagerConfig.FIRST_OBJECT_ID;
				if (objectId < 0)
				{
					continue;
				}
				
				_freeIds.set(objectId);
				_freeIdCount--;
			}
			
			_nextFreeId = _freeIds.nextClearBit(0);
		}
		catch (Exception e)
		{
			LOGGER.severe("IdManager: Could not be initialized properly: " + e.getMessage());
		}
		
		LOGGER.info("IdManager: " + _freeIds.size() + " ids available.");
	}
	
	/**
	 * Increases the internal storage size of the {@code _freeIds} bit set.<br>
	 * This happens when the current capacity is too small for the required IDs.<br>
	 * It ensures the new size does not exceed {@code TOTAL_ID_COUNT}.
	 */
	private void increaseBitSetCapacity()
	{
		final int currentSize = _freeIds.size();
		final int newSize = Math.min(PrimeCapacityAllocator.nextCapacity((int) (currentSize * IdManagerConfig.RESIZE_MULTIPLIER)), TOTAL_ID_COUNT);
		
		// Only resize if the new size is larger than the current size.
		if (newSize > currentSize)
		{
			final BitSet newBitSet = new BitSet(newSize);
			newBitSet.or(_freeIds);
			_freeIds = newBitSet;
			LOGGER.info("IdManager: Increased BitSet capacity to " + newSize);
		}
	}
	
	/**
	 * Retrieves the next available unique identifier for an object.<br>
	 * This method handles internal capacity checks and bitset updates.<br>
	 * It ensures that every requested ID is unique within the system.
	 * @return The next available {@code int} ID.
	 */
	public int getNextId()
	{
		_lock.lock();
		try
		{
			final int newId = _nextFreeId;
			_freeIds.set(newId);
			_freeIdCount--;
			
			// Check utilization and increase capacity if needed.
			final double utilization = (double) (TOTAL_ID_COUNT - _freeIdCount) / _freeIds.size();
			if (utilization >= IdManagerConfig.RESIZE_THRESHOLD)
			{
				increaseBitSetCapacity();
				_nextFreeId = _freeIds.nextClearBit(0); // Reset to the first available ID in the resized BitSet.
			}
			
			int nextFree = _freeIds.nextClearBit(newId);
			if (nextFree < 0)
			{
				nextFree = _freeIds.nextClearBit(0);
			}
			
			if (nextFree < 0)
			{
				if (_freeIds.size() < TOTAL_ID_COUNT)
				{
					increaseBitSetCapacity();
					_nextFreeId = _freeIds.nextClearBit(0); // Reset again if resizing occurred.
				}
				else
				{
					throw new NullPointerException("IdManager: Ran out of valid ids.");
				}
			}
			
			_nextFreeId = nextFree;
			
			return newId + IdManagerConfig.FIRST_OBJECT_ID;
		}
		finally
		{
			_lock.unlock();
		}
	}
	
	/**
	 * Releases a specific ID back into the pool of available IDs.<br>
	 * This method marks the {@code objectId} as free for future use.<br>
	 * It checks if the provided ID is within the valid range defined in {@code IdManagerConfig}.
	 * @param objectId The unique identifier to be released.
	 */
	public void releaseId(int objectId)
	{
		_lock.lock();
		try
		{
			if ((objectId - IdManagerConfig.FIRST_OBJECT_ID) > -1)
			{
				_freeIds.clear(objectId - IdManagerConfig.FIRST_OBJECT_ID);
				_freeIdCount++;
			}
			else
			{
				LOGGER.warning("IdManager: Release objectID " + objectId + " failed (< " + IdManagerConfig.FIRST_OBJECT_ID + ")");
			}
		}
		finally
		{
			_lock.unlock();
		}
	}
	
	/**
	 * Returns the total number of IDs currently available for use.<br>
	 * This method retrieves the count from the internal {@code _freeIdCount} variable.<br>
	 * It uses a lock to ensure thread safety during access.
	 * @return The number of free IDs available in the system.
	 */
	public int getAvailableIdCount()
	{
		_lock.lock();
		try
		{
			return _freeIdCount;
		}
		finally
		{
			_lock.unlock();
		}
	}
	
	/**
	 * Provides access to the singleton instance of {@code IdManager}.<br>
	 * Use this method to get the global manager for unique IDs.
	 * @return The singleton instance of {@code IdManager}.
	 */
	public static IdManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final IdManager INSTANCE = new IdManager();
	}
}
