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
package org.l2jmobius.gameserver.mechanics.variables;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.l2jmobius.gameserver.util.StatSet;

/**
 * Base class for named variable storage with change tracking.
 * @author UnAfraid, Mobius
 */
public abstract class AbstractVariables extends StatSet
{
	private final AtomicBoolean _hasChanges = new AtomicBoolean(false);
	
	protected final Set<String> _added = new HashSet<>(4);
	protected final Set<String> _modified = new HashSet<>(4);
	protected final Set<String> _deleted = new HashSet<>(4);
	protected final Lock _saveLock = new ReentrantLock();
	
	/**
	 * Initializes a new instance backed by a {@code ConcurrentHashMap}.
	 */
	protected AbstractVariables()
	{
		super(ConcurrentHashMap::new);
	}
	
	/**
	 * Tracks the change then sets the named variable to a {@code boolean} value.
	 * @param name unique identifier for the variable
	 * @param value {@code boolean} value to assign
	 */
	
	@Override
	public void set(String name, boolean value)
	{
		trackChange(name);
		super.set(name, value);
	}
	
	/**
	 * Tracks the change then sets the named variable to a {@code byte} value.
	 * @param name unique identifier for the variable
	 * @param value {@code byte} value to assign
	 */
	@Override
	public void set(String name, byte value)
	{
		trackChange(name);
		super.set(name, value);
	}
	
	/**
	 * Tracks the change then sets the named variable to a {@code short} value.
	 * @param name unique identifier for the variable
	 * @param value {@code short} value to assign
	 */
	@Override
	public void set(String name, short value)
	{
		trackChange(name);
		super.set(name, value);
	}
	
	/**
	 * Tracks the change then sets the named variable to an {@code int} value.
	 * @param name unique identifier for the variable
	 * @param value {@code int} value to assign
	 */
	@Override
	public void set(String name, int value)
	{
		trackChange(name);
		super.set(name, value);
	}
	
	/**
	 * Tracks the change then sets the named variable to a {@code long} value.
	 * @param name unique identifier for the variable
	 * @param value {@code long} value to assign
	 */
	@Override
	public void set(String name, long value)
	{
		trackChange(name);
		super.set(name, value);
	}
	
	/**
	 * Tracks the change then sets the named variable to a {@code float} value.
	 * @param name unique identifier for the variable
	 * @param value {@code float} value to assign
	 */
	@Override
	public void set(String name, float value)
	{
		trackChange(name);
		super.set(name, value);
	}
	
	/**
	 * Tracks the change then sets the named variable to a {@code double} value.
	 * @param name unique identifier for the variable
	 * @param value {@code double} value to assign
	 */
	@Override
	public void set(String name, double value)
	{
		trackChange(name);
		super.set(name, value);
	}
	
	/**
	 * Tracks the change then sets the named variable to a {@code String} value.
	 * @param name unique identifier for the variable
	 * @param value {@code String} value to assign
	 */
	@Override
	public void set(String name, String value)
	{
		trackChange(name);
		super.set(name, value);
	}
	
	/**
	 * Tracks the change then sets the named variable to an {@code Enum} value.
	 * @param name unique identifier for the variable
	 * @param value {@code Enum} value to assign
	 */
	@Override
	public void set(String name, Enum<?> value)
	{
		trackChange(name);
		super.set(name, value);
	}
	
	/**
	 * Tracks the change then sets the named variable to an {@code Object} value.
	 * @param name unique identifier for the variable
	 * @param value {@code Object} value to assign
	 */
	@Override
	public void set(String name, Object value)
	{
		trackChange(name);
		super.set(name, value);
	}
	
	/**
	 * Records that the named variable was added or modified and flags pending changes.
	 * @param name name of the variable to track
	 */
	protected void trackChange(String name)
	{
		_saveLock.lock();
		
		try
		{
			_hasChanges.compareAndSet(false, true);
			
			if (hasVariable(name))
			{
				_modified.add(name);
				_deleted.remove(name);
			}
			else
			{
				_added.add(name);
			}
		}
		finally
		{
			_saveLock.unlock();
		}
	}
	
	/**
	 * Sets the named variable to a {@code String} value, optionally recording the change.
	 * @param name unique identifier for the variable
	 * @param value {@code String} value to assign
	 * @param markAsChanged {@code true} to record this update in the change tracker
	 */
	public void set(String name, String value, boolean markAsChanged)
	{
		if (markAsChanged)
		{
			trackChange(name);
		}
		
		super.set(name, value);
	}
	
	/**
	 * Checks whether a variable with the given name exists.
	 * @param name name of the variable to search for
	 * @return {@code true} if the variable exists, {@code false} otherwise
	 */
	public boolean hasVariable(String name)
	{
		return getSet().containsKey(name);
	}
	
	/**
	 * Returns whether any variables have pending changes.
	 * @return {@code true} if there are pending changes, {@code false} otherwise
	 */
	public boolean hasChanges()
	{
		return _hasChanges.get();
	}
	
	/**
	 * Atomically sets the change-tracking flag to the update value if it currently equals the expected value.
	 * @param expect expected current value
	 * @param update new value to set if the expectation holds
	 * @return {@code true} if the update succeeded, {@code false} otherwise
	 */
	public boolean compareAndSetChanges(boolean expect, boolean update)
	{
		return _hasChanges.compareAndSet(expect, update);
	}
	
	/**
	 * Removes the named variable and updates change tracking, scheduling a delete only if it was persisted.
	 * @param name unique name of the variable to remove
	 */
	@Override
	public void remove(String name)
	{
		_saveLock.lock();
		
		try
		{
			_hasChanges.compareAndSet(false, true);
			
			// First remove from map.
			final Object removed = getSet().remove(name);
			
			// Then update tracking based on whether it actually existed.
			if (removed != null)
			{
				// If the variable was newly added during this session and never persisted, removing it should simply drop it from the added set without scheduling a DB DELETE (there's nothing to delete).
				if (_added.remove(name))
				{
					// It was new and never persisted -> nothing more to do.
					return;
				}
				
				_modified.remove(name);
				_deleted.add(name);
			}
		}
		finally
		{
			_saveLock.unlock();
		}
	}
	
	/**
	 * Clears the added, modified and deleted change-tracking sets.
	 */
	protected void clearChangeTracking()
	{
		_added.clear();
		_modified.clear();
		_deleted.clear();
	}
}
