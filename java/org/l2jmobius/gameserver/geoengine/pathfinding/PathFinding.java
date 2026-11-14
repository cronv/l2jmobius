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
package org.l2jmobius.gameserver.geoengine.pathfinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.GeoEngineConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.WorldRegion;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.actor.instance.Fence;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.geoengine.GeoEngine;

/**
 * Entry point for cell pathfinding, using shared bounded pools of {@code NodeBuffer}s.
 * @author Mobius
 */
public class PathFinding
{
	private static final Logger LOGGER = Logger.getLogger(PathFinding.class.getName());
	
	/** Height a fence blocks above and below its own Z coordinate, matching the fence check of {@code FenceData}. */
	private static final int FENCE_MAX_Z_DIFF = 100;
	
	/** Buffers kept for a size class whose configuration entry omits a count. */
	private static final int DEFAULT_BUFFER_COUNT = 4;
	
	/** Milliseconds a search waits on the busy pool of the largest size class before giving up. */
	private static final int BUFFER_WAIT_MILLIS = 50;
	
	private final int[] _sizeClasses;
	private final ArrayBlockingQueue<NodeBuffer>[] _bufferPools;
	
	/**
	 * Parses the configured buffer size classes and counts, then creates every buffer up front in one bounded pool per size class.
	 */
	protected PathFinding()
	{
		try
		{
			final String[] array = GeoEngineConfig.PATHFIND_BUFFERS.split(";");
			if (array.length == 0)
			{
				throw new Exception("PathFindBuffers holds no buffer definition.");
			}
			
			final int[] sizes = new int[array.length];
			final int[] counts = new int[array.length];
			for (int i = 0; i < array.length; i++)
			{
				final String[] args = array[i].split("x");
				if (args.length < 1)
				{
					throw new Exception("Invalid buffer definition: " + array[i]);
				}
				
				sizes[i] = Integer.parseInt(args[0]);
				
				// A broken count never stops the server, the entry loads with the default count instead.
				int count = DEFAULT_BUFFER_COUNT;
				if (args.length > 1)
				{
					try
					{
						count = Integer.parseInt(args[1]);
					}
					catch (NumberFormatException nfe)
					{
						LOGGER.warning("CellPathFinding: Ignoring invalid buffer count of entry " + array[i] + ".");
					}
				}
				
				counts[i] = count;
			}
			
			// Keep the size classes ascending, carrying each count along with its own size.
			for (int i = 1; i < sizes.length; i++)
			{
				final int size = sizes[i];
				final int count = counts[i];
				int j = i - 1;
				while ((j >= 0) && (sizes[j] > size))
				{
					sizes[j + 1] = sizes[j];
					counts[j + 1] = counts[j];
					j--;
				}
				
				sizes[j + 1] = size;
				counts[j + 1] = count;
			}
			
			// Every buffer is created here, so pathfinding memory is fixed at startup and a pool miss never allocates.
			@SuppressWarnings("unchecked")
			final ArrayBlockingQueue<NodeBuffer>[] pools = new ArrayBlockingQueue[sizes.length];
			for (int i = 0; i < sizes.length; i++)
			{
				final int count = Math.max(1, counts[i]);
				pools[i] = new ArrayBlockingQueue<>(count);
				for (int j = 0; j < count; j++)
				{
					pools[i].add(new NodeBuffer(sizes[i]));
				}
			}
			
			_sizeClasses = sizes;
			_bufferPools = pools;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "CellPathFinding: Problem during buffer init: " + e.getMessage(), e);
			throw new Error("CellPathFinding: load aborted");
		}
	}
	
	/**
	 * Converts a geo coordinate to its node coordinate.
	 * @param geoPos The geo coordinate.
	 * @return The node coordinate as a {@code short}.
	 */
	public short getNodePos(int geoPos)
	{
		return (short) (geoPos >> 3);
	}
	
	/**
	 * Retrieves the block index within a region for the given node position.
	 * @param nodePos The node position.
	 * @return The block index as a {@code short}.
	 */
	public short getNodeBlock(int nodePos)
	{
		return (short) (nodePos % 256);
	}
	
	/**
	 * Retrieves the region X tile for the given node position.
	 * @param nodePos The node position.
	 * @return The region X tile as a {@code byte}.
	 */
	public byte getRegionX(int nodePos)
	{
		return (byte) ((nodePos >> 8) + World.TILE_X_MIN);
	}
	
	/**
	 * Retrieves the region Y tile for the given node position.
	 * @param nodePos The node position.
	 * @return The region Y tile as a {@code byte}.
	 */
	public byte getRegionY(int nodePos)
	{
		return (byte) ((nodePos >> 8) + World.TILE_Y_MIN);
	}
	
	/**
	 * Calculates the packed offset of a region tile.
	 * @param rx The region X tile.
	 * @param ry The region Y tile.
	 * @return The region offset as a {@code short}.
	 */
	public short getRegionOffset(byte rx, byte ry)
	{
		return (short) ((rx << 5) + ry);
	}
	
	/**
	 * Converts a node X coordinate to its world X coordinate.
	 * @param nodeX The node X coordinate.
	 * @return The world X coordinate as an {@code int}.
	 */
	public int calculateWorldX(short nodeX)
	{
		return World.WORLD_X_MIN + (nodeX * 128) + 48;
	}
	
	/**
	 * Converts a node Y coordinate to its world Y coordinate.
	 * @param nodeY The node Y coordinate.
	 * @return The world Y coordinate as an {@code int}.
	 */
	public int calculateWorldY(short nodeY)
	{
		return World.WORLD_Y_MIN + (nodeY * 128) + 48;
	}
	
	/**
	 * Retrieves the furthest distance a path can reach, which is where the largest buffer stops.<br>
	 * A target beyond it is clamped into range by {@code findPath}, so callers that judge a path by its end point have to keep their target within this distance.
	 * @return The reach in world units as an {@code int}.
	 */
	public int getMaxPathDistance()
	{
		// Same limit findPath applies, converted from geo cells to world units.
		final int limit = (_sizeClasses[_sizeClasses.length - 1] - 64) / 2;
		return GeoEngine.getWorldX(limit) - GeoEngine.getWorldX(0);
	}
	
	/**
	 * Finds a world-coordinate path between two points, applying post-filtering when enabled.<br>
	 * The search starts with the smallest buffer that fits the distance and expands to larger buffers while no path is found.<br>
	 * When the target cannot be reached, the largest attempt settles for a path to the reachable point closest to the target.
	 * @param x The start X coordinate.
	 * @param y The start Y coordinate.
	 * @param z The start Z coordinate.
	 * @param tx The target X coordinate.
	 * @param ty The target Y coordinate.
	 * @param tz The target Z coordinate.
	 * @param instance The {@code Instance} to path within.
	 * @param playable {@code true} to apply playable-character post-filtering.
	 * @return The path as a {@code List} of {@code GeoLocation}, or {@code null} if none is found.
	 */
	public List<GeoLocation> findPath(int x, int y, int z, int tx, int ty, int tz, Instance instance, boolean playable)
	{
		final GeoEngine geoEngine = GeoEngine.getInstance();
		if (!geoEngine.hasGeo(x, y))
		{
			return null;
		}
		
		if (!geoEngine.hasGeo(tx, ty))
		{
			return null;
		}
		
		final int gx = GeoEngine.getGeoX(x);
		final int gy = GeoEngine.getGeoY(y);
		int gtx = GeoEngine.getGeoX(tx);
		int gty = GeoEngine.getGeoY(ty);
		final int gz = geoEngine.getHeight(x, y, z);
		int gtz = geoEngine.getHeight(tx, ty, tz);
		
		// When the target lies beyond the largest buffer, clamp it into range and path toward it.
		final int largest = _sizeClasses[_sizeClasses.length - 1];
		final int delta = Math.max(Math.abs(gx - gtx), Math.abs(gy - gty));
		if ((64 + (2 * delta)) > largest)
		{
			final int limit = (largest - 64) / 2;
			gtx = gx + (((gtx - gx) * limit) / delta);
			gty = gy + (((gty - gy) * limit) / delta);
			gtz = gz;
		}
		
		// Try the smallest fitting buffer first, expanding to larger buffers while no path is found.
		// The largest attempt may return a partial path to the reachable point closest to the target.
		List<GeoLocation> path = null;
		final int lastIndex = _sizeClasses.length - 1;
		for (int i = firstFitIndex(64 + (2 * Math.max(Math.abs(gx - gtx), Math.abs(gy - gty)))); (path == null) && (i <= lastIndex); i++)
		{
			// A busy size class is skipped immediately for the next larger one; only the largest is briefly waited on, so the partial-path attempt survives load. When every class is busy the search gives up and movement falls back to a direct path.
			final NodeBuffer buffer = borrowBuffer(i, i == lastIndex);
			if (buffer == null)
			{
				continue;
			}
			
			try
			{
				path = buffer.findPath(gx, gy, gz, gtx, gty, gtz, i == lastIndex, collectBlockers(gx, gy, gz, gtx, gty, gtz, buffer.getMapSize(), instance));
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "CellPathFinding: Problem finding path: " + e.getMessage(), e);
				return null;
			}
			finally
			{
				releaseBuffer(i, buffer);
			}
		}
		
		if (path == null)
		{
			return null;
		}
		
		if ((path.size() < 3) || (GeoEngineConfig.MAX_POSTFILTER_PASSES <= 0))
		{
			return path;
		}
		
		return applyPostFiltering(path, x, y, z, instance, playable);
	}
	
	/**
	 * Removes waypoints reachable by a direct move, repeating up to the configured pass limit.
	 * @param initialPath The unfiltered list of {@code GeoLocation} points.
	 * @param startX The start X coordinate.
	 * @param startY The start Y coordinate.
	 * @param startZ The start Z coordinate.
	 * @param instance The {@code Instance} to path within.
	 * @param playable {@code true} to apply playable-character filtering.
	 * @return The filtered list of {@code GeoLocation} points.
	 */
	private List<GeoLocation> applyPostFiltering(List<GeoLocation> initialPath, int startX, int startY, int startZ, Instance instance, boolean playable)
	{
		List<GeoLocation> path = initialPath;
		
		int pass = 0;
		boolean changed;
		do
		{
			pass++;
			changed = false;
			int currentX = startX;
			int currentY = startY;
			int currentZ = startZ;
			
			final List<GeoLocation> optimizedPath = new ArrayList<>();
			for (int i = 0; i < (path.size() - 1); i++)
			{
				final GeoLocation current = path.get(i);
				final GeoLocation next = path.get(i + 1);
				
				// Check if we can move directly to the next waypoint with enough wall clearance.
				if (canSkipWaypoint(currentX, currentY, currentZ, next, instance))
				{
					// Skip current waypoint.
					changed = true;
				}
				else
				{
					// Keep current waypoint.
					optimizedPath.add(current);
					currentX = current.getX();
					currentY = current.getY();
					currentZ = current.getZ();
				}
			}
			
			// Always add the final destination.
			if (!path.isEmpty())
			{
				optimizedPath.add(path.get(path.size() - 1));
			}
			
			path = optimizedPath;
		}
		while (playable && changed && (path.size() > 2) && (pass < GeoEngineConfig.MAX_POSTFILTER_PASSES));
		
		return path;
	}
	
	/**
	 * Checks whether a waypoint can be removed by moving straight to the next one without grazing a wall.<br>
	 * Besides the direct line, two lines shifted sideways by one geo cell must also be walkable,<br>
	 * keeping the smoothed path far enough from corners for the client collision radius.
	 * @param fromX The segment start world X coordinate.
	 * @param fromY The segment start world Y coordinate.
	 * @param fromZ The segment start world Z coordinate.
	 * @param next The following waypoint as a {@code GeoLocation}.
	 * @param instance The {@code Instance} to path within.
	 * @return {@code true} if the intermediate waypoint can be skipped, {@code false} otherwise.
	 */
	private boolean canSkipWaypoint(int fromX, int fromY, int fromZ, GeoLocation next, Instance instance)
	{
		if (!GeoEngine.getInstance().canMoveToTarget(fromX, fromY, fromZ, next.getX(), next.getY(), next.getZ(), instance))
		{
			return false;
		}
		
		final int dx = next.getX() - fromX;
		final int dy = next.getY() - fromY;
		final double length = Math.sqrt((dx * dx) + (dy * dy));
		if (length < 1)
		{
			return true;
		}
		
		// Perpendicular offset of one geo cell (16 world units).
		final int offsetX = (int) Math.round((-dy / length) * 16);
		final int offsetY = (int) Math.round((dx / length) * 16);
		return GeoEngine.getInstance().canMoveToTarget(fromX + offsetX, fromY + offsetY, fromZ, next.getX() + offsetX, next.getY() + offsetY, next.getZ(), instance) //
			&& GeoEngine.getInstance().canMoveToTarget(fromX - offsetX, fromY - offsetY, fromZ, next.getX() - offsetX, next.getY() - offsetY, next.getZ(), instance);
	}
	
	/**
	 * Collects the doors and fences that block movement inside the search window of a buffer.<br>
	 * Doors and fences are registered with the world regions they stand in, so every region the window covers is visited.<br>
	 * A door or fence is registered with its surrounding regions as well, hence the duplicate filtering.
	 * @param gx The start geo X coordinate.
	 * @param gy The start geo Y coordinate.
	 * @param gz The start world Z coordinate.
	 * @param gtx The target geo X coordinate.
	 * @param gty The target geo Y coordinate.
	 * @param gtz The target world Z coordinate.
	 * @param mapSize The side length of the buffer window in geo cells.
	 * @param instance The {@code Instance} to path within.
	 * @return The blocking outlines as a {@code List} of {@code PathBlocker}, or {@code null} when there are none.
	 */
	private List<PathBlocker> collectBlockers(int gx, int gy, int gz, int gtx, int gty, int gtz, int mapSize, Instance instance)
	{
		// The window is the one NodeBuffer centers on the start and target cells.
		final int baseGeoX = gx + ((gtx - gx - mapSize) / 2);
		final int baseGeoY = gy + ((gty - gy - mapSize) / 2);
		final int minX = GeoEngine.getWorldX(baseGeoX);
		final int minY = GeoEngine.getWorldY(baseGeoY);
		final int maxX = GeoEngine.getWorldX(baseGeoX + mapSize);
		final int maxY = GeoEngine.getWorldY(baseGeoY + mapSize);
		final int instanceId = instance == null ? 0 : instance.getId();
		final int regionXMin = minX >> World.SHIFT_BY;
		final int regionXMax = maxX >> World.SHIFT_BY;
		final int regionYMin = minY >> World.SHIFT_BY;
		final int regionYMax = maxY >> World.SHIFT_BY;
		
		List<PathBlocker> blockers = null;
		Set<WorldObject> visited = null;
		for (int regionX = regionXMin; regionX <= regionXMax; regionX++)
		{
			for (int regionY = regionYMin; regionY <= regionYMax; regionY++)
			{
				final int worldX = regionX << World.SHIFT_BY;
				final int worldY = regionY << World.SHIFT_BY;
				
				// Regions are also split by height, so both ends of the path are looked up.
				for (int pass = 0; pass < 2; pass++)
				{
					if ((pass == 1) && (gtz == gz))
					{
						continue;
					}
					
					final WorldRegion region = World.getRegion(worldX, worldY, pass == 0 ? gz : gtz);
					if (region == null)
					{
						continue;
					}
					
					for (Door door : region.getDoors())
					{
						if (door.isOpen() || door.isDead() || !door.checkCollision() || (door.getInstanceWorld() != instance) || (door.getX(0) == 0))
						{
							continue;
						}
						
						if (visited == null)
						{
							visited = new HashSet<>();
						}
						
						if (!visited.add(door))
						{
							continue;
						}
						
						final PathBlocker blocker = new PathBlocker(door.getTemplate().getNodeX(), door.getTemplate().getNodeY(), door.getZMin(), door.getZMax());
						if (blocker.isWithin(minX, minY, maxX, maxY))
						{
							if (blockers == null)
							{
								blockers = new ArrayList<>();
							}
							
							blockers.add(blocker);
						}
					}
					
					for (Fence fence : region.getFences())
					{
						if (!fence.getState().isGeodataEnabled() || (fence.getInstanceId() != instanceId))
						{
							continue;
						}
						
						if (visited == null)
						{
							visited = new HashSet<>();
						}
						
						if (!visited.add(fence))
						{
							continue;
						}
						
						final int fenceMinX = fence.getXMin();
						final int fenceMaxX = fence.getXMax();
						final int fenceMinY = fence.getYMin();
						final int fenceMaxY = fence.getYMax();
						final int fenceZ = fence.getZ();
						final PathBlocker blocker = new PathBlocker(new int[]
						{
							fenceMinX,
							fenceMaxX,
							fenceMaxX,
							fenceMinX
						}, new int[]
						{
							fenceMinY,
							fenceMinY,
							fenceMaxY,
							fenceMaxY
						}, fenceZ - FENCE_MAX_Z_DIFF, fenceZ + FENCE_MAX_Z_DIFF);
						if (blocker.isWithin(minX, minY, maxX, maxY))
						{
							if (blockers == null)
							{
								blockers = new ArrayList<>();
							}
							
							blockers.add(blocker);
						}
					}
				}
			}
		}
		
		return blockers;
	}
	
	/**
	 * Retrieves the index of the smallest size class that fits the given side length.
	 * @param size The minimum side length required.
	 * @return The size class index, falling back to the largest class when none fits.
	 */
	private int firstFitIndex(int size)
	{
		for (int i = 0; i < _sizeClasses.length; i++)
		{
			if (_sizeClasses[i] >= size)
			{
				return i;
			}
		}
		
		return _sizeClasses.length - 1;
	}
	
	/**
	 * Borrows a buffer from the pool of the given size class.<br>
	 * A miss never allocates, keeping pathfinding memory bounded by the configured counts.
	 * @param index The size class index.
	 * @param wait {@code true} to wait briefly on a busy pool, used for the largest size class where no larger fallback exists.
	 * @return A {@code NodeBuffer} ready for use, or {@code null} when every buffer is busy.
	 */
	private NodeBuffer borrowBuffer(int index, boolean wait)
	{
		try
		{
			return wait ? _bufferPools[index].poll(BUFFER_WAIT_MILLIS, TimeUnit.MILLISECONDS) : _bufferPools[index].poll();
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			return null;
		}
	}
	
	/**
	 * Clears a buffer and returns it to the pool of its size class.
	 * @param index The size class index.
	 * @param buffer The {@code NodeBuffer} to return.
	 */
	private void releaseBuffer(int index, NodeBuffer buffer)
	{
		buffer.free();
		_bufferPools[index].offer(buffer);
	}
	
	/**
	 * Retrieves the singleton {@code PathFinding} instance.
	 * @return The single {@code PathFinding} instance.
	 */
	public static PathFinding getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PathFinding INSTANCE = new PathFinding();
	}
}
