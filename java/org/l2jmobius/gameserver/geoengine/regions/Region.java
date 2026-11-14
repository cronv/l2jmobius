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
package org.l2jmobius.gameserver.geoengine.regions;

/**
 * Base region holding the geodata constants and the behavior of a region without geodata:<br>
 * every coordinate is passable in all directions at its own Z. {@code GeoRegion} overrides this with real geodata.
 * @author Mobius
 */
public class Region
{
	/** East NSWE flag */
	public static final byte NSWE_EAST = 1 << 0;
	/** West NSWE flag */
	public static final byte NSWE_WEST = 1 << 1;
	/** South NSWE flag */
	public static final byte NSWE_SOUTH = 1 << 2;
	/** North NSWE flag */
	public static final byte NSWE_NORTH = 1 << 3;
	
	/** North-East NSWE flags */
	public static final byte NSWE_NORTH_EAST = NSWE_NORTH | NSWE_EAST;
	/** North-West NSWE flags */
	public static final byte NSWE_NORTH_WEST = NSWE_NORTH | NSWE_WEST;
	/** South-East NSWE flags */
	public static final byte NSWE_SOUTH_EAST = NSWE_SOUTH | NSWE_EAST;
	/** South-West NSWE flags */
	public static final byte NSWE_SOUTH_WEST = NSWE_SOUTH | NSWE_WEST;
	
	/** All directions NSWE flags */
	public static final byte NSWE_ALL = NSWE_EAST | NSWE_WEST | NSWE_SOUTH | NSWE_NORTH;
	
	/** Cells in a block on the x axis. */
	public static final int BLOCK_CELLS_X = 8;
	/** Cells in a block on the y axis. */
	public static final int BLOCK_CELLS_Y = 8;
	/** Cells in a block. */
	public static final int BLOCK_CELLS = BLOCK_CELLS_X * BLOCK_CELLS_Y;
	
	/** Blocks in a region on the x axis. */
	public static final int REGION_BLOCKS_X = 256;
	/** Blocks in a region on the y axis. */
	public static final int REGION_BLOCKS_Y = 256;
	/** Blocks in a region. */
	public static final int REGION_BLOCKS = REGION_BLOCKS_X * REGION_BLOCKS_Y;
	
	/** Cells in a region on the x axis. */
	public static final int REGION_CELLS_X = REGION_BLOCKS_X * BLOCK_CELLS_X;
	/** Cells in a region on the y axis. */
	public static final int REGION_CELLS_Y = REGION_BLOCKS_Y * BLOCK_CELLS_Y;
	/** Cells in a region. */
	public static final int REGION_CELLS = REGION_CELLS_X * REGION_CELLS_Y;
	
	/** Shared instance used where geodata is absent. */
	public static final Region INSTANCE = new Region();
	
	/**
	 * Checks the NSWE passability at the given coordinate; always {@code true} without geodata.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @param worldZ The world Z coordinate.
	 * @param nswe The NSWE flags to check.
	 * @return {@code true} always.
	 */
	public boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe)
	{
		return true;
	}
	
	/**
	 * No-op without geodata.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @param worldZ The world Z coordinate.
	 * @param nswe The NSWE flags to set.
	 */
	public void setNearestNswe(int geoX, int geoY, int worldZ, byte nswe)
	{
	}
	
	/**
	 * No-op without geodata.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @param worldZ The world Z coordinate.
	 * @param nswe The NSWE flags to unset.
	 */
	public void unsetNearestNswe(int geoX, int geoY, int worldZ, byte nswe)
	{
	}
	
	/**
	 * Retrieves the nearest Z at the given coordinate; always {@code worldZ} without geodata.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @param worldZ The world Z coordinate.
	 * @return The given {@code worldZ} as an {@code int}.
	 */
	public int getNearestZ(int geoX, int geoY, int worldZ)
	{
		return worldZ;
	}
	
	/**
	 * Retrieves the next lower Z at the given coordinate; always {@code worldZ} without geodata.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @param worldZ The world Z coordinate.
	 * @return The given {@code worldZ} as an {@code int}.
	 */
	public int getNextLowerZ(int geoX, int geoY, int worldZ)
	{
		return worldZ;
	}
	
	/**
	 * Retrieves the next higher Z at the given coordinate; always {@code worldZ} without geodata.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @param worldZ The world Z coordinate.
	 * @return The given {@code worldZ} as an {@code int}.
	 */
	public int getNextHigherZ(int geoX, int geoY, int worldZ)
	{
		return worldZ;
	}
	
	/**
	 * Indicates whether this region holds geodata; always {@code false} without geodata.
	 * @return {@code false} always.
	 */
	public boolean hasGeo()
	{
		return false;
	}
	
	/**
	 * Saves this region's geodata to a file; always {@code false} without geodata.
	 * @param fileName The target file name.
	 * @return {@code false} always.
	 */
	public boolean saveToFile(String fileName)
	{
		return false;
	}
}
