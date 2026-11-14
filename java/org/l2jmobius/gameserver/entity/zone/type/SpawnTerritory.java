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
package org.l2jmobius.gameserver.entity.zone.type;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.zone.ZoneForm;

/**
 * This class represents a dummy zone used exclusively for geometry calculations.<br>
 * It does not contain functional gameplay logic.
 * @author GKR
 */
public class SpawnTerritory
{
	private final String _name;
	private final ZoneForm _territory;
	
	/**
	 * Creates a new {@code SpawnTerritory} instance.<br>
	 * This constructor initializes the territory with a specific name and shape.
	 * @param name The unique name for this territory.
	 * @param territory The {@code ZoneForm} defining the physical boundaries of the area.
	 */
	public SpawnTerritory(String name, ZoneForm territory)
	{
		_name = name;
		_territory = territory;
	}
	
	/**
	 * Retrieves the name of the {@code SpawnTerritory}.<br>
	 * This value is used to identify the territory.
	 * @return The name as a {@code String}.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Generates a random coordinate within the territory.<br>
	 * This method delegates the calculation to the underlying {@code ZoneForm}.
	 * @return a new {@code Location} object representing a random point.
	 */
	public Location getRandomPoint()
	{
		return _territory.getRandomPoint();
	}
	
	/**
	 * Retrieves the center point of this territory.<br>
	 * This method calls {@code getCenterPoint()} on the internal {@code ZoneForm}.
	 * @return the {@code Location} representing the center of the zone.
	 */
	public Location getCenterPoint()
	{
		return _territory.getCenterPoint();
	}
	
	/**
	 * Checks if a specific coordinate is within the territory.<br>
	 * This method uses the {@code ZoneForm} geometry to perform the check.
	 * @param x The X coordinate of the point.
	 * @param y The Y coordinate of the point.
	 * @param z The Z coordinate of the point.
	 * @return {@code true} if the coordinates are inside the zone, {@code false} otherwise.
	 */
	public boolean isInsideZone(int x, int y, int z)
	{
		return _territory.isInsideZone(x, y, z);
	}
	
	/**
	 * Tests containment on the XY plane only, ignoring the Z band.
	 * @param x
	 * @param y
	 * @return {@code true} if the point lies inside the territory polygon
	 */
	public boolean isInsideZone(int x, int y)
	{
		return _territory.isInsideZone(x, y, _territory.getLowZ());
	}
	
	/**
	 * Retrieves the lowest Z coordinate of the territory.<br>
	 * This value is obtained from the internal {@code ZoneForm}.
	 * @return The minimum height as an {@code int}.
	 */
	public int getLowZ()
	{
		return _territory.getLowZ();
	}
	
	/**
	 * Retrieves the highest Z coordinate of the territory.<br>
	 * This value is obtained from the internal {@code ZoneForm}.
	 * @return The maximum height as an {@code int}.
	 */
	public int getHighZ()
	{
		return _territory.getHighZ();
	}
	
	/**
	 * Displays the visual representation of the territory.<br>
	 * This method calls the visualization logic on the underlying {@code ZoneForm}.
	 * @param z The height level to visualize.
	 */
	public void visualizeZone(int z)
	{
		_territory.visualizeZone(z);
	}
}
