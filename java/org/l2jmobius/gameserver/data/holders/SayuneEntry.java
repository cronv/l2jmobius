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
package org.l2jmobius.gameserver.data.holders;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.interfaces.ILocational;

/**
 * Represents an entry in the {@code Sayune} data structure.<br>
 * It stores specific location information for a {@code Sayune} entity.<br>
 * This class implements {@code ILocational} to provide coordinate data.
 * @author UnAfraid, Mobius
 */
public class SayuneEntry implements ILocational
{
	private boolean _isSelector = false;
	private final int _id;
	private final Location _location;
	private final List<SayuneEntry> _innerEntries = new ArrayList<>();
	
	/**
	 * Creates a new {@code SayuneEntry} instance.<br>
	 * This constructor initializes the entry with a default location.
	 * @param id The unique identifier for this entry.
	 */
	public SayuneEntry(int id)
	{
		_id = id;
		_location = new Location(0, 0, -10000);
	}
	
	/**
	 * Creates a new {@code SayuneEntry} with specific coordinates and properties.<br>
	 * This constructor initializes the entry's location and its selector status.
	 * @param isSelector The boolean flag determining if this entry acts as a selector.
	 * @param id The unique identifier for this entry.
	 * @param x The X coordinate of the location.
	 * @param y The Y coordinate of the location.
	 * @param z The Z coordinate of the location.
	 */
	public SayuneEntry(boolean isSelector, int id, int x, int y, int z)
	{
		_isSelector = isSelector;
		_id = id;
		_location = new Location(x, y, z);
	}
	
	/**
	 * Retrieves the unique identifier for this {@code SayuneEntry}.<br>
	 * This value is assigned when the object is created.
	 * @return The integer ID of the entry.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the X coordinate of the location.<br>
	 * This value is obtained from the internal {@code Location} object.
	 * @return The X coordinate as an {@code int}.
	 */
	@Override
	public int getX()
	{
		return _location.getX();
	}
	
	/**
	 * Retrieves the vertical coordinate of the location.<br>
	 * This value is obtained from the internal {@code Location} object.
	 * @return The current y-coordinate as an {@code int}.
	 */
	@Override
	public int getY()
	{
		return _location.getY();
	}
	
	/**
	 * Retrieves the Z coordinate of the location.<br>
	 * This value is obtained from the internal {@code Location} object.
	 * @return The Z coordinate as an {@code int}.
	 */
	@Override
	public int getZ()
	{
		return _location.getZ();
	}
	
	/**
	 * Retrieves the heading value of this entry.<br>
	 * This method currently returns a constant value of {@code 0}.
	 * @return The current heading as an {@code int}.
	 */
	@Override
	public int getHeading()
	{
		return 0;
	}
	
	/**
	 * Retrieves the location of this entry.<br>
	 * This method returns the {@code Location} object associated with the entry.
	 * @return the {@code ILocational} object representing the current position.
	 */
	@Override
	public ILocational getLocation()
	{
		return _location;
	}
	
	/**
	 * Checks if this entry is a selector.
	 * @return {@code true} if the entry is a selector, {@code false} otherwise.
	 */
	public boolean isSelector()
	{
		return _isSelector;
	}
	
	/**
	 * Retrieves the list of nested {@code SayuneEntry} objects.<br>
	 * This method returns all entries contained within this specific entry.
	 * @return a {@code List} of {@code SayuneEntry} objects.
	 */
	public List<SayuneEntry> getInnerEntries()
	{
		return _innerEntries;
	}
	
	/**
	 * Adds a new {@code SayuneEntry} to the internal list of nested entries.<br>
	 * This allows for creating hierarchical structures of locations.
	 * @param innerEntry The {@code SayuneEntry} object to be added.
	 * @return The {@code SayuneEntry} that was just added.
	 */
	public SayuneEntry addInnerEntry(SayuneEntry innerEntry)
	{
		_innerEntries.add(innerEntry);
		return innerEntry;
	}
}
