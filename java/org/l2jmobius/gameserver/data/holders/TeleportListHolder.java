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

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.Location;

/**
 * This class acts as a container for managing lists of teleport locations.<br>
 * It provides a structured way to store and access {@code Location} data for the game world.
 * @author NviX, Index
 */
public class TeleportListHolder
{
	private final int _locId;
	private final List<Location> _locations;
	private final int _price;
	
	/**
	 * Creates a new {@code TeleportListHolder} with a single destination.<br>
	 * This constructor initializes the holder using specific coordinates.
	 * @param locId The unique identifier for this teleport location.
	 * @param x The X coordinate of the destination.
	 * @param y The Y coordinate of the destination.
	 * @param z The Z coordinate of the destination.
	 * @param price The cost required to use this teleport.
	 */
	public TeleportListHolder(int locId, int x, int y, int z, int price)
	{
		_locId = locId;
		_locations = new ArrayList<>(1);
		_locations.add(new Location(x, y, z));
		_price = price;
	}
	
	/**
	 * Creates a new {@code TeleportListHolder} object.<br>
	 * This constructor initializes the teleport data with specific coordinates and costs.
	 * @param locId The unique identifier for the location.
	 * @param locations A list of {@code Location} objects to be used for teleportation.
	 * @param price The cost required to use this teleport.
	 */
	public TeleportListHolder(int locId, List<Location> locations, int price)
	{
		_locId = locId;
		_locations = locations;
		_price = price;
	}
	
	/**
	 * Retrieves the unique identifier for this location.<br>
	 * This value is used to identify a specific teleport point.
	 * @return The {@code int} ID of the location.
	 */
	public int getLocId()
	{
		return _locId;
	}
	
	/**
	 * Retrieves the list of all locations associated with this holder.<br>
	 * This method returns a {@code List} of {@code Location} objects.
	 * @return A {@code List} containing all {@code Location} data.
	 */
	public List<Location> getLocations()
	{
		return _locations;
	}
	
	/**
	 * Retrieves the cost of the teleport list.<br>
	 * This value is stored as an {@code int}.
	 * @return The price of the teleport list.
	 */
	public int getPrice()
	{
		return _price;
	}
	
	/**
	 * Retrieves a random location from the list.<br>
	 * This method picks one {@code Location} at random.
	 * @return A random {@code Location} object.
	 */
	public Location getLocation()
	{
		return _locations.get(Rnd.get(_locations.size()));
	}
}
