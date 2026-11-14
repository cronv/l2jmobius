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
package org.l2jmobius.gameserver.geoengine;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.GeoEngineConfig;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.data.xml.FenceData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.actor.templates.DoorTemplate;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.geoengine.regions.GeoRegion;
import org.l2jmobius.gameserver.geoengine.regions.Region;
import org.l2jmobius.gameserver.geoengine.util.GridLineIterator2D;
import org.l2jmobius.gameserver.geoengine.util.GridLineIterator3D;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.util.GeoUtils;

/**
 * Loads geodata and answers movement, height and line-of-sight queries for the game world.
 * @author -Nemesiss-, HorridoJoho, Mobius
 */
public class GeoEngine
{
	private static final Logger LOGGER = Logger.getLogger(GeoEngine.class.getName());
	
	// Constants.
	public static final String FILE_NAME_FORMAT = "%d_%d.l2j";
	/** Steps from the observer that still measure the see-over ceiling from the observer height instead of the bee line. */
	public static final int ELEVATED_SEE_OVER_DISTANCE = 2;
	/** Height a surface may rise above the sight line before it blocks. */
	public static final int MAX_SEE_OVER_HEIGHT = 48;
	private static final int SPAWN_Z_DELTA_LIMIT = 100;
	private static final int WORLD_MIN_X = -655360;
	private static final int WORLD_MIN_Y = -589824;
	private static final int GEO_REGIONS_X = 32;
	private static final int GEO_REGIONS_Y = 32;
	private static final int GEO_REGIONS = GEO_REGIONS_X * GEO_REGIONS_Y;
	private static final int COORDINATE_SCALE = 16;
	private static final int COORDINATE_OFFSET = 8;
	private static final int HEIGHT_INCREASE_LIMIT = 40;
	private static final int SPAWN_HEIGHT_OFFSET = 20;
	/** When the height check in pathing fails on a single cell, accept the move if any NSWE neighbour of that cell has a layer within this many game units of the source Z - the cell is then treated as a continuous-surface gap. */
	private static final int PATH_CONTINUITY_TOLERANCE = 16;
	
	// Region Management.
	private static final AtomicReferenceArray<Region> REGIONS = new AtomicReferenceArray<>(GEO_REGIONS);
	
	/**
	 * Loads every available geodata region from the configured path, disabling pathfinding when none are found.
	 */
	protected GeoEngine()
	{
		// Initially set all regions to Region.
		for (int i = 0; i < GEO_REGIONS; i++)
		{
			REGIONS.set(i, Region.INSTANCE);
		}
		
		int loadedRegions = 0;
		try
		{
			for (int regionX = World.TILE_X_MIN; regionX <= World.TILE_X_MAX; regionX++)
			{
				for (int regionY = World.TILE_Y_MIN; regionY <= World.TILE_Y_MAX; regionY++)
				{
					final Path geoFilePath = GeoEngineConfig.GEODATA_PATH.resolve(String.format(FILE_NAME_FORMAT, regionX, regionY));
					if (Files.exists(geoFilePath))
					{
						try
						{
							// LOGGER.info(getClass().getSimpleName() + ": Loading " + geoFilePath.getFileName() + "...");
							loadRegion(geoFilePath, regionX, regionY);
							loadedRegions++;
						}
						catch (Exception e)
						{
							LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed to load " + geoFilePath.getFileName() + "!", e);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Critical error during geodata initialization!", e);
			System.exit(1);
		}
		
		final String simpleName = getClass().getSimpleName();
		LOGGER.info(simpleName + ": Loaded " + loadedRegions + " regions.");
		
		// Avoid wrong configuration when no files are loaded.
		if ((loadedRegions == 0) && (GeoEngineConfig.PATHFINDING > 0))
		{
			GeoEngineConfig.PATHFINDING = 0;
			LOGGER.info(simpleName + ": Pathfinding is disabled.");
		}
	}
	
	/**
	 * Memory-maps a region file and stores it as a {@code Region}.
	 * @param filePath The path to the region file.
	 * @param regionX The region X tile.
	 * @param regionY The region Y tile.
	 * @throws IOException If the file cannot be read.
	 */
	private void loadRegion(Path filePath, int regionX, int regionY) throws IOException
	{
		final int regionOffset = (regionX * GEO_REGIONS_Y) + regionY;
		try (RandomAccessFile randomAccessFile = new RandomAccessFile(filePath.toFile(), "r"))
		{
			REGIONS.set(regionOffset, new GeoRegion(randomAccessFile.getChannel().map(MapMode.READ_ONLY, 0, randomAccessFile.length()).order(ByteOrder.LITTLE_ENDIAN)));
		}
	}
	
	/**
	 * Reloads a region's geodata from disk.
	 * @param regionX The region X tile.
	 * @param regionY The region Y tile.
	 * @return {@code true} if reloaded, {@code false} if the file is missing or an error occurs.
	 */
	public boolean reloadRegion(int regionX, int regionY)
	{
		final Path geoFilePath = GeoEngineConfig.GEODATA_PATH.resolve(String.format(FILE_NAME_FORMAT, regionX, regionY));
		if (!Files.exists(geoFilePath))
		{
			LOGGER.warning(getClass().getSimpleName() + ": Cannot reload, file not found for region " + regionX + "_" + regionY);
			return false;
		}
		
		final int regionOffset = (regionX * GEO_REGIONS_Y) + regionY;
		try
		{
			final Region region = REGIONS.get(regionOffset);
			if (region instanceof GeoRegion)
			{
				final ByteBuffer buffer = ByteBuffer.wrap(Files.readAllBytes(geoFilePath)).order(ByteOrder.LITTLE_ENDIAN);
				((GeoRegion) region).load(buffer);
				LOGGER.info(getClass().getSimpleName() + ": Reloaded region " + regionX + "_" + regionY + " from bytes.");
				return true;
			}
			
			// Not a real region? fallback load.
			loadRegion(geoFilePath, regionX, regionY);
			LOGGER.info(getClass().getSimpleName() + ": Replaced Region with new region " + regionX + "_" + regionY);
			return true;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed to reload region " + regionX + "_" + regionY + "!", e);
			return false;
		}
	}
	
	/**
	 * Assigns a {@code Region} to the given region tile.
	 * @param regionX The region X tile.
	 * @param regionY The region Y tile.
	 * @param region The {@code Region} to assign.
	 */
	public void setRegion(int regionX, int regionY, GeoRegion region)
	{
		final int regionOffset = (regionX * GEO_REGIONS_Y) + regionY;
		REGIONS.set(regionOffset, region);
	}
	
	/**
	 * Retrieves the region covering the given geo coordinate.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @return The {@code Region} at the coordinate.
	 */
	public Region getRegion(int geoX, int geoY)
	{
		return REGIONS.get(((geoX / Region.REGION_CELLS_X) * GEO_REGIONS_Y) + (geoY / Region.REGION_CELLS_Y));
	}
	
	/**
	 * Checks whether geodata exists at the given geo coordinate.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @return {@code true} if geodata exists, {@code false} otherwise.
	 */
	public boolean hasGeoPos(int geoX, int geoY)
	{
		return getRegion(geoX, geoY).hasGeo();
	}
	
	/**
	 * Checks the NSWE passability at the given coordinate.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @param worldZ The world Z coordinate.
	 * @param nswe The NSWE flags to check.
	 * @return {@code true} if every requested flag is set, {@code false} otherwise.
	 */
	public boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe)
	{
		return getRegion(geoX, geoY).checkNearestNswe(geoX, geoY, worldZ, nswe);
	}
	
	/**
	 * Checks NSWE passability while forbidding diagonal moves that cut across blocked corners.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @param worldZ The world Z coordinate.
	 * @param nswe The NSWE flags to check.
	 * @return {@code true} if the move is allowed, {@code false} otherwise.
	 */
	public boolean checkNearestNsweAntiCornerCut(int geoX, int geoY, int worldZ, int nswe)
	{
		boolean canMove = true;
		
		final Region region = getRegion(geoX, geoY);
		if ((nswe & Region.NSWE_NORTH_EAST) == Region.NSWE_NORTH_EAST)
		{
			canMove = region.checkNearestNswe(geoX, geoY - 1, worldZ, Region.NSWE_EAST) && region.checkNearestNswe(geoX + 1, geoY, worldZ, Region.NSWE_NORTH);
		}
		
		if (canMove && ((nswe & Region.NSWE_NORTH_WEST) == Region.NSWE_NORTH_WEST))
		{
			canMove = region.checkNearestNswe(geoX, geoY - 1, worldZ, Region.NSWE_WEST) && region.checkNearestNswe(geoX - 1, geoY, worldZ, Region.NSWE_NORTH);
		}
		
		if (canMove && ((nswe & Region.NSWE_SOUTH_EAST) == Region.NSWE_SOUTH_EAST))
		{
			canMove = region.checkNearestNswe(geoX, geoY + 1, worldZ, Region.NSWE_EAST) && region.checkNearestNswe(geoX + 1, geoY, worldZ, Region.NSWE_SOUTH);
		}
		
		if (canMove && ((nswe & Region.NSWE_SOUTH_WEST) == Region.NSWE_SOUTH_WEST))
		{
			canMove = region.checkNearestNswe(geoX, geoY + 1, worldZ, Region.NSWE_WEST) && region.checkNearestNswe(geoX - 1, geoY, worldZ, Region.NSWE_SOUTH);
		}
		
		return canMove && region.checkNearestNswe(geoX, geoY, worldZ, nswe);
	}
	
	/**
	 * Sets the given NSWE flags at the coordinate.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @param worldZ The world Z coordinate.
	 * @param nswe The NSWE flags to set.
	 */
	public void setNearestNswe(int geoX, int geoY, int worldZ, byte nswe)
	{
		getRegion(geoX, geoY).setNearestNswe(geoX, geoY, worldZ, nswe);
	}
	
	/**
	 * Clears the given NSWE flags at the coordinate.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @param worldZ The world Z coordinate.
	 * @param nswe The NSWE flags to unset.
	 */
	public void unsetNearestNswe(int geoX, int geoY, int worldZ, byte nswe)
	{
		getRegion(geoX, geoY).unsetNearestNswe(geoX, geoY, worldZ, nswe);
	}
	
	/**
	 * Retrieves the nearest geodata height to {@code worldZ} at the coordinate.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @param worldZ The world Z coordinate.
	 * @return The nearest Z as an {@code int}.
	 */
	public int getNearestZ(int geoX, int geoY, int worldZ)
	{
		return getRegion(geoX, geoY).getNearestZ(geoX, geoY, worldZ);
	}
	
	/**
	 * Retrieves the next geodata height at or below {@code worldZ} at the coordinate.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @param worldZ The world Z coordinate.
	 * @return The resulting Z as an {@code int}.
	 */
	public int getNextLowerZ(int geoX, int geoY, int worldZ)
	{
		return getRegion(geoX, geoY).getNextLowerZ(geoX, geoY, worldZ);
	}
	
	/**
	 * Retrieves the next geodata height at or above {@code worldZ} at the coordinate.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @param worldZ The world Z coordinate.
	 * @return The resulting Z as an {@code int}.
	 */
	public int getNextHigherZ(int geoX, int geoY, int worldZ)
	{
		return getRegion(geoX, geoY).getNextHigherZ(geoX, geoY, worldZ);
	}
	
	/**
	 * Converts a world X coordinate to a geo X coordinate.
	 * @param worldX The world X coordinate.
	 * @return The geo X coordinate as an {@code int}.
	 */
	public static int getGeoX(int worldX)
	{
		return (worldX - WORLD_MIN_X) / COORDINATE_SCALE;
	}
	
	/**
	 * Converts a world Y coordinate to a geo Y coordinate.
	 * @param worldY The world Y coordinate.
	 * @return The geo Y coordinate as an {@code int}.
	 */
	public static int getGeoY(int worldY)
	{
		return (worldY - WORLD_MIN_Y) / COORDINATE_SCALE;
	}
	
	/**
	 * Converts a geo X coordinate to its world X coordinate.
	 * @param geoX The geo X coordinate.
	 * @return The world X coordinate as an {@code int}.
	 */
	public static int getWorldX(int geoX)
	{
		return (geoX * COORDINATE_SCALE) + WORLD_MIN_X + COORDINATE_OFFSET;
	}
	
	/**
	 * Converts a geo Y coordinate to its world Y coordinate.
	 * @param geoY The geo Y coordinate.
	 * @return The world Y coordinate as an {@code int}.
	 */
	public static int getWorldY(int geoY)
	{
		return (geoY * COORDINATE_SCALE) + WORLD_MIN_Y + COORDINATE_OFFSET;
	}
	
	/**
	 * Retrieves the nearest geodata height at the given world position.
	 * @param x The world X coordinate.
	 * @param y The world Y coordinate.
	 * @param z The world Z coordinate.
	 * @return The nearest height as an {@code int}.
	 */
	public int getHeight(int x, int y, int z)
	{
		return getNearestZ(getGeoX(x), getGeoY(y), z);
	}
	
	/**
	 * Calculates a safe spawn height near the given world position, falling back to {@code z} where there is no geodata.
	 * @param x The world X coordinate.
	 * @param y The world Y coordinate.
	 * @param z The world Z coordinate.
	 * @return The spawn height as an {@code int}.
	 */
	public int getSpawnHeight(int x, int y, int z)
	{
		final int geoX = getGeoX(x);
		final int geoY = getGeoY(y);
		
		if (!hasGeoPos(geoX, geoY))
		{
			return z;
		}
		
		final int nextLowerZ = getNextLowerZ(geoX, geoY, z + SPAWN_HEIGHT_OFFSET);
		return Math.abs(nextLowerZ - z) <= SPAWN_Z_DELTA_LIMIT ? nextLowerZ : z;
	}
	
	/**
	 * Calculates a safe spawn height for the given {@code Location}.
	 * @param location The {@code Location} to check.
	 * @return The spawn height as an {@code int}.
	 */
	public int getSpawnHeight(Location location)
	{
		return getSpawnHeight(location.getX(), location.getY(), location.getZ());
	}
	
	/**
	 * Checks whether the creature has line of sight to the target, with artefacts always visible.
	 * @param creature The observing {@code WorldObject}.
	 * @param target The target {@code WorldObject}.
	 * @return {@code true} if the target is visible, {@code false} otherwise.
	 */
	public boolean canSeeTarget(WorldObject creature, WorldObject target)
	{
		if (target == null)
		{
			return false;
		}
		
		if (target.isArtefact())
		{
			return true;
		}
		
		if (target.isDoor())
		{
			final Door door = target.asDoor();
			final DoorTemplate doorTemplate = door.getTemplate();
			final int[] nodeX = doorTemplate.getNodeX();
			final int[] nodeY = doorTemplate.getNodeY();
			final int middleX = (nodeX[0] + nodeX[1] + nodeX[2] + nodeX[3]) / 4;
			final int middleY = (nodeY[0] + nodeY[1] + nodeY[2] + nodeY[3]) / 4;
			final int bottomZ = door.getZMin() + (door.getHeight() / 4);
			return canSeeTarget(creature.getX(), creature.getY(), creature.getZ(), middleX, middleY, bottomZ);
		}
		
		return canSeeTarget(creature.getX(), creature.getY(), creature.getZ(), creature.getInstanceWorld(), target.getX(), target.getY(), target.getZ(), target.getInstanceWorld());
	}
	
	/**
	 * Checks whether the creature has line of sight to the given position.
	 * @param creature The observing {@code WorldObject}.
	 * @param worldPosition The target {@code ILocational} position.
	 * @return {@code true} if the position is visible, {@code false} otherwise.
	 */
	public boolean canSeeTarget(WorldObject creature, ILocational worldPosition)
	{
		return canSeeTarget(creature.getX(), creature.getY(), creature.getZ(), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), creature.getInstanceWorld());
	}
	
	/**
	 * Checks line of sight between two points, requiring both to share the same instance.
	 * @param x The observer world X coordinate.
	 * @param y The observer world Y coordinate.
	 * @param z The observer world Z coordinate.
	 * @param instance The observer {@code Instance}.
	 * @param targetX The target world X coordinate.
	 * @param targetY The target world Y coordinate.
	 * @param targetZ The target world Z coordinate.
	 * @param targetInstance The target {@code Instance}.
	 * @return {@code true} if the target is visible, {@code false} otherwise.
	 */
	public boolean canSeeTarget(int x, int y, int z, Instance instance, int targetX, int targetY, int targetZ, Instance targetInstance)
	{
		return (instance == targetInstance) && canSeeTarget(x, y, z, targetX, targetY, targetZ, instance);
	}
	
	/**
	 * Checks line of sight between two points, blocked by doors, fences or terrain.
	 * @param x The observer world X coordinate.
	 * @param y The observer world Y coordinate.
	 * @param z The observer world Z coordinate.
	 * @param targetX The target world X coordinate.
	 * @param targetY The target world Y coordinate.
	 * @param targetZ The target world Z coordinate.
	 * @param instance The {@code Instance} to check within.
	 * @return {@code true} if the target is visible, {@code false} otherwise.
	 */
	public boolean canSeeTarget(int x, int y, int z, int targetX, int targetY, int targetZ, Instance instance)
	{
		// Door checks.
		if (DoorData.getInstance().checkIfDoorsBetween(x, y, z, targetX, targetY, targetZ, instance, true))
		{
			return false;
		}
		
		// Fence checks.
		if (FenceData.getInstance().checkIfFenceBetween(x, y, z, targetX, targetY, targetZ, instance))
		{
			return false;
		}
		
		return canSeeTarget(x, y, z, targetX, targetY, targetZ);
	}
	
	/**
	 * Computes the line-of-sight geo Z at the next step, rising when the step is corner-cut blocked.
	 * @param previousX The previous geo X coordinate.
	 * @param previousY The previous geo Y coordinate.
	 * @param previousGeoZ The previous geo Z coordinate.
	 * @param currentX The current geo X coordinate.
	 * @param currentY The current geo Y coordinate.
	 * @param nswe The step NSWE flags.
	 * @return The geo Z at the current step as an {@code int}.
	 */
	private int getLosGeoZ(int previousX, int previousY, int previousGeoZ, int currentX, int currentY, int nswe)
	{
		if ((((nswe & Region.NSWE_NORTH) != 0) && ((nswe & Region.NSWE_SOUTH) != 0)) || (((nswe & Region.NSWE_WEST) != 0) && ((nswe & Region.NSWE_EAST) != 0)))
		{
			throw new RuntimeException("Multiple directions specified in NSWE: " + nswe);
		}
		
		return checkNearestNsweAntiCornerCut(previousX, previousY, previousGeoZ, nswe) ? getNearestZ(currentX, currentY, previousGeoZ) : getNextHigherZ(currentX, currentY, previousGeoZ);
	}
	
	/**
	 * Checks terrain line of sight between two points by tracing the geodata grid line.
	 * @param x The observer world X coordinate.
	 * @param y The observer world Y coordinate.
	 * @param z The observer world Z coordinate.
	 * @param targetX The target world X coordinate.
	 * @param targetY The target world Y coordinate.
	 * @param targetZ The target world Z coordinate.
	 * @return {@code true} if the target is visible, {@code false} otherwise.
	 */
	public boolean canSeeTarget(int x, int y, int z, int targetX, int targetY, int targetZ)
	{
		int geoX = getGeoX(x);
		int geoY = getGeoY(y);
		int targetGeoX = getGeoX(targetX);
		int targetGeoY = getGeoY(targetY);
		
		int nearestFromZ = getNearestZ(geoX, geoY, z);
		int nearestToZ = getNearestZ(targetGeoX, targetGeoY, targetZ);
		
		// Fastpath for same coordinates.
		if ((geoX == targetGeoX) && (geoY == targetGeoY))
		{
			return !hasGeoPos(targetGeoX, targetGeoY) || (nearestFromZ == nearestToZ);
		}
		
		int fromX = x;
		int fromY = y;
		int toX = targetX;
		int toY = targetY;
		if (nearestToZ > nearestFromZ)
		{
			int temp = toX;
			toX = fromX;
			fromX = temp;
			
			temp = toY;
			toY = fromY;
			fromY = temp;
			
			temp = nearestToZ;
			nearestToZ = nearestFromZ;
			nearestFromZ = temp;
			
			temp = targetGeoX;
			targetGeoX = geoX;
			geoX = temp;
			
			temp = targetGeoY;
			targetGeoY = geoY;
			geoY = temp;
		}
		
		final GridLineIterator3D pointIterator = new GridLineIterator3D(geoX, geoY, nearestFromZ, targetGeoX, targetGeoY, nearestToZ);
		
		// First point is guaranteed to be available, skip it, we can always see our own position.
		pointIterator.next();
		int previousX = pointIterator.x();
		int previousY = pointIterator.y();
		int previousGeoZ = pointIterator.z();
		int pointIndex = 0;
		while (pointIterator.next())
		{
			final int currentX = pointIterator.x();
			final int currentY = pointIterator.y();
			
			if ((currentX == previousX) && (currentY == previousY))
			{
				continue;
			}
			
			final int beeCurrentZ = pointIterator.z();
			int currentGeoZ = previousGeoZ;
			
			// Check if the position has geodata.
			if (hasGeoPos(currentX, currentY))
			{
				final int nswe = GeoUtils.computeNswe(previousX, previousY, currentX, currentY);
				currentGeoZ = getLosGeoZ(previousX, previousY, previousGeoZ, currentX, currentY, nswe);
				final int maxHeight = pointIndex < ELEVATED_SEE_OVER_DISTANCE ? nearestFromZ + MAX_SEE_OVER_HEIGHT : beeCurrentZ + MAX_SEE_OVER_HEIGHT;
				boolean canSeeThrough = false;
				if (currentGeoZ <= maxHeight)
				{
					if ((nswe & Region.NSWE_NORTH_EAST) == Region.NSWE_NORTH_EAST)
					{
						final int northGeoZ = getLosGeoZ(previousX, previousY, previousGeoZ, previousX, previousY - 1, Region.NSWE_NORTH);
						final int eastGeoZ = getLosGeoZ(previousX, previousY, previousGeoZ, previousX + 1, previousY, Region.NSWE_EAST);
						canSeeThrough = (northGeoZ <= maxHeight) && (eastGeoZ <= maxHeight) && (northGeoZ <= getNearestZ(previousX, previousY - 1, beeCurrentZ)) && (eastGeoZ <= getNearestZ(previousX + 1, previousY, beeCurrentZ));
					}
					else if ((nswe & Region.NSWE_NORTH_WEST) == Region.NSWE_NORTH_WEST)
					{
						final int northGeoZ = getLosGeoZ(previousX, previousY, previousGeoZ, previousX, previousY - 1, Region.NSWE_NORTH);
						final int westGeoZ = getLosGeoZ(previousX, previousY, previousGeoZ, previousX - 1, previousY, Region.NSWE_WEST);
						canSeeThrough = (northGeoZ <= maxHeight) && (westGeoZ <= maxHeight) && (northGeoZ <= getNearestZ(previousX, previousY - 1, beeCurrentZ)) && (westGeoZ <= getNearestZ(previousX - 1, previousY, beeCurrentZ));
					}
					else if ((nswe & Region.NSWE_SOUTH_EAST) == Region.NSWE_SOUTH_EAST)
					{
						final int southGeoZ = getLosGeoZ(previousX, previousY, previousGeoZ, previousX, previousY + 1, Region.NSWE_SOUTH);
						final int eastGeoZ = getLosGeoZ(previousX, previousY, previousGeoZ, previousX + 1, previousY, Region.NSWE_EAST);
						canSeeThrough = (southGeoZ <= maxHeight) && (eastGeoZ <= maxHeight) && (southGeoZ <= getNearestZ(previousX, previousY + 1, beeCurrentZ)) && (eastGeoZ <= getNearestZ(previousX + 1, previousY, beeCurrentZ));
					}
					else if ((nswe & Region.NSWE_SOUTH_WEST) == Region.NSWE_SOUTH_WEST)
					{
						final int southGeoZ = getLosGeoZ(previousX, previousY, previousGeoZ, previousX, previousY + 1, Region.NSWE_SOUTH);
						final int westGeoZ = getLosGeoZ(previousX, previousY, previousGeoZ, previousX - 1, previousY, Region.NSWE_WEST);
						canSeeThrough = (southGeoZ <= maxHeight) && (westGeoZ <= maxHeight) && (southGeoZ <= getNearestZ(previousX, previousY + 1, beeCurrentZ)) && (westGeoZ <= getNearestZ(previousX - 1, previousY, beeCurrentZ));
					}
					else
					{
						canSeeThrough = true;
					}
				}
				
				if (!canSeeThrough)
				{
					return false;
				}
			}
			
			previousX = currentX;
			previousY = currentY;
			previousGeoZ = currentGeoZ;
			++pointIndex;
		}
		
		return true;
	}
	
	/**
	 * Checks whether any cardinal neighbor has a geodata layer within {@code tolerance} of {@code worldZ}.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @param worldZ The world Z coordinate.
	 * @param tolerance The maximum allowed height difference.
	 * @return {@code true} if such a neighbor exists, {@code false} otherwise.
	 */
	private boolean hasNeighbourLayerNear(int geoX, int geoY, int worldZ, int tolerance)
	{
		// @formatter:off
		final int[] dx = { 1, -1, 0, 0 };
		final int[] dy = { 0, 0, 1, -1 };
		// @formatter:on
		
		for (int i = 0; i < 4; i++)
		{
			final int nx = geoX + dx[i];
			final int ny = geoY + dy[i];
			if (hasGeoPos(nx, ny) && (Math.abs(getNearestZ(nx, ny, worldZ) - worldZ) <= tolerance))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Retrieves the furthest reachable {@code Location} from the origin toward the destination.
	 * @param origin The starting {@code ILocational}.
	 * @param destination The target {@code ILocational}.
	 * @return The furthest reachable {@code Location}.
	 */
	public Location getValidLocation(ILocational origin, ILocational destination)
	{
		return getValidLocation(origin.getX(), origin.getY(), origin.getZ(), destination.getX(), destination.getY(), destination.getZ(), null);
	}
	
	/**
	 * Retrieves the furthest reachable {@code Location} toward the target, stopping at doors, fences or walls.
	 * @param x The start world X coordinate.
	 * @param y The start world Y coordinate.
	 * @param z The start world Z coordinate.
	 * @param targetX The target world X coordinate.
	 * @param targetY The target world Y coordinate.
	 * @param targetZ The target world Z coordinate.
	 * @param instance The {@code Instance} to move within.
	 * @return The furthest reachable {@code Location}.
	 */
	public Location getValidLocation(int x, int y, int z, int targetX, int targetY, int targetZ, Instance instance)
	{
		final int geoX = getGeoX(x);
		final int geoY = getGeoY(y);
		final int nearestFromZ = getNearestZ(geoX, geoY, z);
		final int targetGeoX = getGeoX(targetX);
		final int targetGeoY = getGeoY(targetY);
		final int nearestToZ = getNearestZ(targetGeoX, targetGeoY, targetZ);
		
		// Door checks.
		if (DoorData.getInstance().checkIfDoorsBetween(x, y, nearestFromZ, targetX, targetY, nearestToZ, instance, false))
		{
			return new Location(x, y, getHeight(x, y, nearestFromZ));
		}
		
		// Fence checks.
		if (FenceData.getInstance().checkIfFenceBetween(x, y, nearestFromZ, targetX, targetY, nearestToZ, instance))
		{
			return new Location(x, y, getHeight(x, y, nearestFromZ));
		}
		
		final GridLineIterator2D pointIterator = new GridLineIterator2D(geoX, geoY, targetGeoX, targetGeoY);
		
		// First point is guaranteed to be available.
		pointIterator.next();
		int previousX = pointIterator.x();
		int previousY = pointIterator.y();
		int previousZ = nearestFromZ;
		
		while (pointIterator.next())
		{
			final int currentX = pointIterator.x();
			final int currentY = pointIterator.y();
			final int rawCurrentZ = getNearestZ(currentX, currentY, previousZ);
			final int currentZ;
			if ((rawCurrentZ - previousZ) > HEIGHT_INCREASE_LIMIT) // Check for sudden height increase.
			{
				// Single-cell layer-gap fallback: if any NSWE neighbour of this cell has a layer near previousZ, the surface is continuous through this cell even though its own nearest layer is far.
				// Step over the gap by treating the current cell as if it had a layer at previousZ.
				if (!hasNeighbourLayerNear(currentX, currentY, previousZ, PATH_CONTINUITY_TOLERANCE))
				{
					// Genuine wall, return previous location.
					return new Location(getWorldX(previousX), getWorldY(previousY), previousZ);
				}
				
				currentZ = previousZ;
			}
			else
			{
				currentZ = rawCurrentZ;
			}
			
			if (hasGeoPos(previousX, previousY))
			{
				if (isCompletelyBlocked(currentX, currentY, currentZ))
				{
					// Can't move, return previous location.
					return new Location(getWorldX(previousX), getWorldY(previousY), previousZ);
				}
				
				if (!checkNearestNsweAntiCornerCut(previousX, previousY, previousZ, GeoUtils.computeNswe(previousX, previousY, currentX, currentY)))
				{
					// Can't move, return previous location.
					return new Location(getWorldX(previousX), getWorldY(previousY), previousZ);
				}
			}
			
			previousX = currentX;
			previousY = currentY;
			previousZ = currentZ;
		}
		
		return hasGeoPos(previousX, previousY) && (previousZ != nearestToZ) ? new Location(x, y, nearestFromZ) : new Location(targetX, targetY, nearestToZ);
	}
	
	/**
	 * Checks whether a straight move between two points is unobstructed by doors, fences, walls or corner cuts.
	 * @param fromX The start world X coordinate.
	 * @param fromY The start world Y coordinate.
	 * @param fromZ The start world Z coordinate.
	 * @param toX The target world X coordinate.
	 * @param toY The target world Y coordinate.
	 * @param toZ The target world Z coordinate.
	 * @param instance The {@code Instance} to move within.
	 * @return {@code true} if the move is unobstructed, {@code false} otherwise.
	 */
	public boolean canMoveToTarget(int fromX, int fromY, int fromZ, int toX, int toY, int toZ, Instance instance)
	{
		final int geoX = getGeoX(fromX);
		final int geoY = getGeoY(fromY);
		final int nearestFromZ = getNearestZ(geoX, geoY, fromZ);
		final int targetGeoX = getGeoX(toX);
		final int targetGeoY = getGeoY(toY);
		final int nearestToZ = getNearestZ(targetGeoX, targetGeoY, toZ);
		
		// Door checks.
		if (DoorData.getInstance().checkIfDoorsBetween(fromX, fromY, nearestFromZ, toX, toY, nearestToZ, instance, false))
		{
			return false;
		}
		
		// Fence checks.
		if (FenceData.getInstance().checkIfFenceBetween(fromX, fromY, nearestFromZ, toX, toY, nearestToZ, instance))
		{
			return false;
		}
		
		final GridLineIterator2D pointIterator = new GridLineIterator2D(geoX, geoY, targetGeoX, targetGeoY);
		
		// First point is guaranteed to be available.
		pointIterator.next();
		int previousX = pointIterator.x();
		int previousY = pointIterator.y();
		int previousZ = nearestFromZ;
		
		while (pointIterator.next())
		{
			final int currentX = pointIterator.x();
			final int currentY = pointIterator.y();
			final int rawCurrentZ = getNearestZ(currentX, currentY, previousZ);
			final int currentZ;
			if ((rawCurrentZ - previousZ) > HEIGHT_INCREASE_LIMIT) // Check for sudden height increase.
			{
				// Single-cell layer-gap fallback (mirrors getValidLocation): treat as continuous surface if any NSWE neighbour of the current cell has a layer near previousZ.
				if (!hasNeighbourLayerNear(currentX, currentY, previousZ, PATH_CONTINUITY_TOLERANCE))
				{
					return false;
				}
				
				currentZ = previousZ;
			}
			else
			{
				currentZ = rawCurrentZ;
			}
			
			if (hasGeoPos(previousX, previousY))
			{
				final int nswe = GeoUtils.computeNswe(previousX, previousY, currentX, currentY);
				if (GeoEngineConfig.AVOID_OBSTRUCTED_PATH_NODES && !checkNearestNswe(previousX, previousY, previousZ, nswe))
				{
					return false;
				}
				
				if (!checkNearestNsweAntiCornerCut(previousX, previousY, previousZ, nswe))
				{
					return false;
				}
			}
			
			previousX = currentX;
			previousY = currentY;
			previousZ = currentZ;
		}
		
		return !hasGeoPos(previousX, previousY) || (previousZ == nearestToZ);
	}
	
	/**
	 * Checks whether a straight move from the given location to the target coordinate is unobstructed.
	 * @param from The starting {@code ILocational}.
	 * @param toX The target world X coordinate.
	 * @param toY The target world Y coordinate.
	 * @param toZ The target world Z coordinate.
	 * @return {@code true} if the move is unobstructed, {@code false} otherwise.
	 */
	public boolean canMoveToTarget(ILocational from, int toX, int toY, int toZ)
	{
		return canMoveToTarget(from.getX(), from.getY(), from.getZ(), toX, toY, toZ, null);
	}
	
	/**
	 * Checks whether a straight move between the two locations is unobstructed.
	 * @param from The starting {@code ILocational}.
	 * @param to The target {@code ILocational}.
	 * @return {@code true} if the move is unobstructed, {@code false} otherwise.
	 */
	public boolean canMoveToTarget(ILocational from, ILocational to)
	{
		return canMoveToTarget(from, to.getX(), to.getY(), to.getZ());
	}
	
	/**
	 * Checks whether geodata exists at the given world position.
	 * @param x The world X coordinate.
	 * @param y The world Y coordinate.
	 * @return {@code true} if geodata exists, {@code false} otherwise.
	 */
	public boolean hasGeo(int x, int y)
	{
		return hasGeoPos(getGeoX(x), getGeoY(y));
	}
	
	/**
	 * Checks whether every direction is blocked at the coordinate; always {@code false} when pathfinding is disabled.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @param geoZ The geo Z coordinate.
	 * @return {@code true} if fully blocked, {@code false} otherwise.
	 */
	public boolean isCompletelyBlocked(int geoX, int geoY, int geoZ)
	{
		if (GeoEngineConfig.PATHFINDING < 1)
		{
			return false;
		}
		
		final Region region = getRegion(geoX, geoY);
		if (region != null)
		{
			return region.hasGeo() && !region.checkNearestNswe(geoX, geoY, geoZ, Region.NSWE_NORTH) && !region.checkNearestNswe(geoX, geoY, geoZ, Region.NSWE_SOUTH) && !region.checkNearestNswe(geoX, geoY, geoZ, Region.NSWE_EAST) && !region.checkNearestNswe(geoX, geoY, geoZ, Region.NSWE_WEST);
		}
		
		return true;
	}
	
	/**
	 * Retrieves the singleton {@code GeoEngine} instance.
	 * @return The single {@code GeoEngine} instance.
	 */
	public static GeoEngine getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final GeoEngine INSTANCE = new GeoEngine();
	}
}
