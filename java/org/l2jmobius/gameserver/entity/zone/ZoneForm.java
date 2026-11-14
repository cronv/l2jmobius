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
package org.l2jmobius.gameserver.entity.zone;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.managers.ZoneManager;

/**
 * This is the abstract base class for all zone types in the game world.<br>
 * It provides the core structure and common properties shared by different {@code Zone} implementations.
 * @author durgus
 */
public abstract class ZoneForm
{
	protected static final int STEP = 10;
	
	public abstract boolean isInsideZone(int x, int y, int z);
	
	public abstract boolean intersectsRectangle(int x1, int x2, int y1, int y2);
	
	public abstract double getDistanceToZone(int x, int y);
	
	public abstract int getLowZ(); // Support for the ability to extract the z coordinates of zones.
	
	public abstract int getHighZ(); // New fishing patch makes use of that to get the Z for the hook.
	
	// landing coordinates.
	
	/**
	 * Checks if two line segments cross each other.<br>
	 * This method uses {@code java.awt.geom.Line2D} to perform the calculation.
	 * @param ax1 The x-coordinate of the first point on the first segment.
	 * @param ay1 The y-coordinate of the first point on the first segment.
	 * @param ax2 The x-coordinate of the second point on the first segment.
	 * @param ay2 The y-coordinate of the second point on the first segment.
	 * @param bx1 The x-coordinate of the first point on the second segment.
	 * @param by1 The y-coordinate of the first point on the second segment.
	 * @param bx2 The x-coordinate of the second point on the second segment.
	 * @param by2 The y-coordinate of the second point on the second segment.
	 * @return {@code true} if the segments intersect, otherwise {@code false}.
	 */
	protected boolean lineSegmentsIntersect(int ax1, int ay1, int ax2, int ay2, int bx1, int by1, int bx2, int by2)
	{
		return java.awt.geom.Line2D.linesIntersect(ax1, ay1, ax2, ay2, bx1, by1, bx2, by2);
	}
	
	public abstract void visualizeZone(int z);
	
	/**
	 * Spawns a debug item at a specific location.<br>
	 * This is used for testing purposes only.
	 * @param itemId The ID of the item to spawn.
	 * @param num The quantity of the item.
	 * @param x The X coordinate.
	 * @param y The Y coordinate.
	 * @param z The Z coordinate.
	 */
	protected void dropDebugItem(int itemId, int num, int x, int y, int z)
	{
		final Item item = new Item(IdManager.getInstance().getNextId(), itemId);
		item.setCount(num);
		item.spawnMe(x, y, z + 5);
		ZoneManager.getInstance().getDebugItems().add(item);
	}
	
	public abstract Location getRandomPoint();
	
	public abstract Location getCenterPoint();
}
