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

/**
 * Immutable definition of a scheduled boat, loaded from BoatData.xml.<br>
 * Holds the broadcast points and the circular station route.<br>
 * The boat spawns at its first station.
 * @author Mobius
 */
public class BoatHolder
{
	private final int _id;
	private final String _name;
	private final int _heading;
	private final int _initialDelay;
	private final List<Location> _broadcastPoints = new ArrayList<>(2);
	private final List<BoatStationHolder> _stations = new ArrayList<>();
	
	/**
	 * Creates a new {@code BoatHolder}.
	 * @param id The boat template id.
	 * @param name The unique boat name.
	 * @param heading The spawn heading.
	 * @param initialDelay The delay in milliseconds before the schedule starts.
	 */
	public BoatHolder(int id, String name, int heading, int initialDelay)
	{
		_id = id;
		_name = name;
		_heading = heading;
		_initialDelay = initialDelay;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public int getHeading()
	{
		return _heading;
	}
	
	public int getInitialDelay()
	{
		return _initialDelay;
	}
	
	public void addBroadcastPoint(Location location)
	{
		_broadcastPoints.add(location);
	}
	
	public List<Location> getBroadcastPoints()
	{
		return _broadcastPoints;
	}
	
	public void addStation(BoatStationHolder station)
	{
		_stations.add(station);
	}
	
	public List<BoatStationHolder> getStations()
	{
		return _stations;
	}
}
