/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.entity.shuttle;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.holders.creature.VehiclePathPoint;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class acts as a data container for shuttle-related information.<br>
 * It stores specific attributes and states required by the {@code Shuttle} entity.<br>
 * Use this class to manage persistent data associated with shuttles in the game world.
 * @author UnAfraid
 */
public class ShuttleDataHolder
{
	private final int _id;
	private final Location _loc;
	private final List<Integer> _doors = new ArrayList<>(2);
	private final List<ShuttleStop> _stops = new ArrayList<>(2);
	private final List<VehiclePathPoint[]> _routes = new ArrayList<>(2);
	
	/**
	 * Creates a new instance of {@code ShuttleDataHolder}.<br>
	 * This constructor initializes the shuttle's ID and location.<br>
	 * It extracts these values from the provided {@code StatSet}.
	 * @param set The {@code StatSet} containing the required data.
	 */
	public ShuttleDataHolder(StatSet set)
	{
		_id = set.getInt("id");
		_loc = new Location(set);
	}
	
	/**
	 * Retrieves the unique identifier for this shuttle.<br>
	 * This value is used to distinguish between different shuttles.
	 * @return The {@code int} ID of the shuttle.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the current location of the shuttle.<br>
	 * This method returns the {@code Location} object stored in this holder.
	 * @return The {@code Location} of the shuttle.
	 */
	public Location getLocation()
	{
		return _loc;
	}
	
	/**
	 * Adds a new door identifier to the shuttle.<br>
	 * This method updates the internal list of doors for this {@code ShuttleDataHolder}.
	 * @param id The unique identifier for the door to add.
	 */
	public void addDoor(int id)
	{
		_doors.add(id);
	}
	
	/**
	 * Retrieves the list of door identifiers for this shuttle.<br>
	 * Each {@code Integer} represents a unique door ID.
	 * @return A {@code List<Integer>} containing all door IDs.
	 */
	public List<Integer> getDoors()
	{
		return _doors;
	}
	
	/**
	 * Adds a new stop to the shuttle's list of stops.<br>
	 * This method updates the internal collection of {@code ShuttleStop} objects.
	 * @param stop The {@code ShuttleStop} object to be added.
	 */
	public void addStop(ShuttleStop stop)
	{
		_stops.add(stop);
	}
	
	/**
	 * Retrieves the list of stops for this shuttle.<br>
	 * This method returns all {@code ShuttleStop} objects associated with the holder.
	 * @return A {@code List} containing all {@code ShuttleStop} objects.
	 */
	public List<ShuttleStop> getStops()
	{
		return _stops;
	}
	
	/**
	 * Adds a new path to the shuttle.<br>
	 * This method stores an array of {@code VehiclePathPoint} objects in the route list.
	 * @param route The array of points defining the vehicle path.
	 */
	public void addRoute(VehiclePathPoint[] route)
	{
		_routes.add(route);
	}
	
	/**
	 * Retrieves the list of all paths for this shuttle.<br>
	 * Each path is represented as an array of {@code VehiclePathPoint}.
	 * @return A {@code List} containing all {@code VehiclePathPoint[]} arrays.
	 */
	public List<VehiclePathPoint[]> getRoutes()
	{
		return _routes;
	}
}
