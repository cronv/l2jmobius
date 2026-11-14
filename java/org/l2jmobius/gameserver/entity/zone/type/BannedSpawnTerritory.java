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
 * It serves as a placeholder to define areas where spawning is restricted.
 * @author UnAfraid
 */
public class BannedSpawnTerritory
{
	private final String _name;
	private final ZoneForm _territory;
	
	/**
	 * Creates a new {@code BannedSpawnTerritory} instance.<br>
	 * This constructor initializes the zone with a specific name and shape.
	 * @param name The unique name for this territory.
	 * @param territory The {@code ZoneForm} defining the physical area of the territory.
	 */
	public BannedSpawnTerritory(String name, ZoneForm territory)
	{
		_name = name;
		_territory = territory;
	}
	
	/**
	 * Retrieves the name of the {@code BannedSpawnTerritory}.<br>
	 * This value was provided during object creation.
	 * @return The name as a {@code String}.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Generates a random coordinate within the territory.<br>
	 * This method delegates the calculation to the {@code ZoneForm} object.
	 * @return A new {@code Location} object representing a random point.
	 */
	public Location getRandomPoint()
	{
		return _territory.getRandomPoint();
	}
	
	/**
	 * Checks if a specific point is within this territory.<br>
	 * It uses the coordinates provided to perform the calculation.
	 * @param x The x-coordinate of the point.
	 * @param y The y-coordinate of the point.
	 * @param z The z-coordinate of the point.
	 * @return {@code true} if the point is inside the zone, otherwise {@code false}.
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
	 * Displays the visual representation of this territory.<br>
	 * This method calls the visualization logic on the underlying {@code ZoneForm}.
	 * @param z The vertical coordinate used for rendering the zone.
	 */
	public void visualizeZone(int z)
	{
		_territory.visualizeZone(z);
	}
}
