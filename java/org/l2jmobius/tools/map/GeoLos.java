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
package org.l2jmobius.tools.map;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;

import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.geoengine.regions.GeoRegion;
import org.l2jmobius.gameserver.geoengine.regions.Region;
import org.l2jmobius.gameserver.geoengine.util.GridLineIterator3D;

/**
 * Line-of-sight probe over a generated .l2j region: replays the exact {@code GeoEngine.canSeeTarget} walk and prints every step, so a "cannot see target" report names the cell that blocks and the layer stack behind it.<br>
 * Both points must lie on the tile the probed file holds.
 * @author Mobius
 */
public class GeoLos
{
	private final GeoRegion _region;
	
	private GeoLos(GeoRegion region)
	{
		_region = region;
	}
	
	/**
	 * Loads a region file and traces the sight line between two world points, printing every step and the blocking cell.
	 * @param file the .l2j region file
	 * @param x the observer world X
	 * @param y the observer world Y
	 * @param z the observer world Z
	 * @param targetX the target world X
	 * @param targetY the target world Y
	 * @param targetZ the target world Z
	 * @throws IOException on read failure
	 */
	public static void probe(Path file, int x, int y, int z, int targetX, int targetY, int targetZ) throws IOException
	{
		final GeoRegion region;
		try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r"))
		{
			region = new GeoRegion(raf.getChannel().map(MapMode.READ_ONLY, 0, raf.length()).order(ByteOrder.LITTLE_ENDIAN));
		}
		
		final int tileX = (int) Math.floor(x / 32768.0) + 20;
		final int tileY = (int) Math.floor(y / 32768.0) + 18;
		final int targetTileX = (int) Math.floor(targetX / 32768.0) + 20;
		final int targetTileY = (int) Math.floor(targetY / 32768.0) + 18;
		System.out.println("Region file: " + file);
		System.out.println("From (" + x + ", " + y + ", " + z + ") on tile " + tileX + "_" + tileY + " to (" + targetX + ", " + targetY + ", " + targetZ + ") on tile " + targetTileX + "_" + targetTileY);
		if ((tileX != targetTileX) || (tileY != targetTileY))
		{
			System.out.println("WARNING: the points sit on different tiles, this probe holds only one file.");
		}
		
		System.out.println();
		new GeoLos(region).trace(x, y, z, targetX, targetY, targetZ);
	}
	
	/**
	 * Replays GeoEngine.canSeeTarget step by step, printing the state the engine sees at every cell.
	 * @param x the observer world X
	 * @param y the observer world Y
	 * @param z the observer world Z
	 * @param targetX the target world X
	 * @param targetY the target world Y
	 * @param targetZ the target world Z
	 */
	private void trace(int x, int y, int z, int targetX, int targetY, int targetZ)
	{
		int geoX = GeoEngine.getGeoX(x);
		int geoY = GeoEngine.getGeoY(y);
		int targetGeoX = GeoEngine.getGeoX(targetX);
		int targetGeoY = GeoEngine.getGeoY(targetY);
		
		int nearestFromZ = _region.getNearestZ(geoX, geoY, z);
		int nearestToZ = _region.getNearestZ(targetGeoX, targetGeoY, targetZ);
		System.out.println("Observer cell [" + geoX + ", " + geoY + "] nearestZ " + nearestFromZ + " (asked " + z + ", off by " + (nearestFromZ - z) + ") layers " + layers(geoX, geoY));
		System.out.println("Target   cell [" + targetGeoX + ", " + targetGeoY + "] nearestZ " + nearestToZ + " (asked " + targetZ + ", off by " + (nearestToZ - targetZ) + ") layers " + layers(targetGeoX, targetGeoY));
		System.out.println();
		
		if ((geoX == targetGeoX) && (geoY == targetGeoY))
		{
			System.out.println("Same cell: visible = " + (nearestFromZ == nearestToZ) + " (the engine compares the two nearest heights).");
			return;
		}
		
		// The engine always walks downhill, swapping the endpoints when the target stands higher.
		if (nearestToZ > nearestFromZ)
		{
			int temp = nearestToZ;
			nearestToZ = nearestFromZ;
			nearestFromZ = temp;
			
			temp = targetGeoX;
			targetGeoX = geoX;
			geoX = temp;
			
			temp = targetGeoY;
			targetGeoY = geoY;
			geoY = temp;
			System.out.println("Endpoints swapped: the walk runs from the higher point down.");
			System.out.println();
		}
		
		final GridLineIterator3D pointIterator = new GridLineIterator3D(geoX, geoY, nearestFromZ, targetGeoX, targetGeoY, nearestToZ);
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
			final int nswe = computeNswe(previousX, previousY, currentX, currentY);
			final boolean cornerCut = !checkNsweAntiCornerCut(previousX, previousY, previousGeoZ, nswe);
			final int currentGeoZ = cornerCut ? _region.getNextHigherZ(currentX, currentY, previousGeoZ) : _region.getNearestZ(currentX, currentY, previousGeoZ);
			final int maxHeight = pointIndex < GeoEngine.ELEVATED_SEE_OVER_DISTANCE ? nearestFromZ + GeoEngine.MAX_SEE_OVER_HEIGHT : beeCurrentZ + GeoEngine.MAX_SEE_OVER_HEIGHT;
			final String currentLayers = layers(currentX, currentY);
			System.out.println("step " + pointIndex + " cell [" + currentX + ", " + currentY + "] world (" + GeoEngine.getWorldX(currentX) + ", " + GeoEngine.getWorldY(currentY) + ") losZ " + currentGeoZ + " bee " + beeCurrentZ + " max " + maxHeight + " move " + nsweText(nswe) + (cornerCut ? " CLOSED (losZ taken from getNextHigherZ)" : "") + " layers " + currentLayers);
			if (currentGeoZ > maxHeight)
			{
				System.out.println();
				System.out.println("BLOCKED here: losZ " + currentGeoZ + " rises " + (currentGeoZ - maxHeight) + " above the " + GeoEngine.MAX_SEE_OVER_HEIGHT + " unit see-over margin.");
				System.out.println("Cell [" + currentX + ", " + currentY + "] layers " + currentLayers);
				System.out.println("Previous cell [" + previousX + ", " + previousY + "] carried losZ " + previousGeoZ + " layers " + layers(previousX, previousY));
				System.out.println(cornerCut ? "Cause: the move out of the previous cell is closed, so the engine climbed to the next higher layer." : "Cause: this cell's own nearest layer stands too high.");
				return;
			}
			
			// A diagonal step only sees through when BOTH cells it cuts between clear the margin and stay at or under their own surface.
			final String flankBlock = flankBlocker(previousX, previousY, previousGeoZ, nswe, maxHeight, beeCurrentZ);
			if (flankBlock != null)
			{
				System.out.println();
				System.out.println("BLOCKED here: the diagonal move " + nsweText(nswe) + " is cut by a corner.");
				System.out.println("Cause: " + flankBlock);
				System.out.println("Previous cell [" + previousX + ", " + previousY + "] carried losZ " + previousGeoZ + " layers " + layers(previousX, previousY));
				return;
			}
			
			previousX = currentX;
			previousY = currentY;
			previousGeoZ = currentGeoZ;
			pointIndex++;
		}
		
		System.out.println();
		System.out.println("Visible: the whole line stays inside the see-over margin.");
	}
	
	/**
	 * Names the flanking cell that kills a diagonal step, mirroring the corner rule in {@code GeoEngine.canSeeTarget}: each of the two cells the diagonal cuts between must clear the see-over margin AND stay at or under its own nearest surface at bee height.
	 * @param previousX the previous geo X coordinate
	 * @param previousY the previous geo Y coordinate
	 * @param previousGeoZ the sight height carried out of the previous cell
	 * @param nswe the step direction bits
	 * @param maxHeight the see-over ceiling for this step
	 * @param beeCurrentZ the bee-line height at this step
	 * @return the reason the diagonal is blocked, or {@code null} when it is open or the step is straight
	 */
	private String flankBlocker(int previousX, int previousY, int previousGeoZ, int nswe, int maxHeight, int beeCurrentZ)
	{
		int verticalY = 0;
		int horizontalX = 0;
		int verticalNswe = 0;
		int horizontalNswe = 0;
		switch (nswe)
		{
			case Region.NSWE_NORTH_EAST:
			{
				verticalY = previousY - 1;
				horizontalX = previousX + 1;
				verticalNswe = Region.NSWE_EAST;
				horizontalNswe = Region.NSWE_NORTH;
				break;
			}
			case Region.NSWE_NORTH_WEST:
			{
				verticalY = previousY - 1;
				horizontalX = previousX - 1;
				verticalNswe = Region.NSWE_WEST;
				horizontalNswe = Region.NSWE_NORTH;
				break;
			}
			case Region.NSWE_SOUTH_EAST:
			{
				verticalY = previousY + 1;
				horizontalX = previousX + 1;
				verticalNswe = Region.NSWE_EAST;
				horizontalNswe = Region.NSWE_SOUTH;
				break;
			}
			case Region.NSWE_SOUTH_WEST:
			{
				verticalY = previousY + 1;
				horizontalX = previousX - 1;
				verticalNswe = Region.NSWE_WEST;
				horizontalNswe = Region.NSWE_SOUTH;
				break;
			}
			default:
			{
				// Straight step, no corner to cut.
				return null;
			}
		}
		
		final int verticalGeoZ = losGeoZ(previousX, previousY, previousGeoZ, previousX, verticalY, verticalNswe);
		final int horizontalGeoZ = losGeoZ(previousX, previousY, previousGeoZ, horizontalX, previousY, horizontalNswe);
		if (verticalGeoZ > maxHeight)
		{
			return "flank cell [" + previousX + ", " + verticalY + "] sits at " + verticalGeoZ + ", " + (verticalGeoZ - maxHeight) + " over the margin. layers " + layers(previousX, verticalY);
		}
		
		if (horizontalGeoZ > maxHeight)
		{
			return "flank cell [" + horizontalX + ", " + previousY + "] sits at " + horizontalGeoZ + ", " + (horizontalGeoZ - maxHeight) + " over the margin. layers " + layers(horizontalX, previousY);
		}
		
		final int verticalNearestZ = _region.getNearestZ(previousX, verticalY, beeCurrentZ);
		if (verticalGeoZ > verticalNearestZ)
		{
			return "flank cell [" + previousX + ", " + verticalY + "] climbs to " + verticalGeoZ + ", above its own surface " + verticalNearestZ + " at bee height. layers " + layers(previousX, verticalY);
		}
		
		final int horizontalNearestZ = _region.getNearestZ(horizontalX, previousY, beeCurrentZ);
		if (horizontalGeoZ > horizontalNearestZ)
		{
			return "flank cell [" + horizontalX + ", " + previousY + "] climbs to " + horizontalGeoZ + ", above its own surface " + horizontalNearestZ + " at bee height. layers " + layers(horizontalX, previousY);
		}
		
		return null;
	}
	
	/**
	 * The engine's sight height for one step: the nearest layer when the move is open, the next higher layer when it is not.
	 * @param previousX the previous geo X coordinate
	 * @param previousY the previous geo Y coordinate
	 * @param previousGeoZ the sight height carried out of the previous cell
	 * @param currentX the target geo X coordinate
	 * @param currentY the target geo Y coordinate
	 * @param nswe the step direction bits
	 * @return the sight height at the target cell as an {@code int}
	 */
	private int losGeoZ(int previousX, int previousY, int previousGeoZ, int currentX, int currentY, int nswe)
	{
		return checkNsweAntiCornerCut(previousX, previousY, previousGeoZ, nswe) ? _region.getNearestZ(currentX, currentY, previousGeoZ) : _region.getNextHigherZ(currentX, currentY, previousGeoZ);
	}
	
	/**
	 * Diagonal moves additionally need both flanking cells open, exactly as the engine checks them.
	 * @param geoX the geo X coordinate
	 * @param geoY the geo Y coordinate
	 * @param worldZ the world Z coordinate
	 * @param nswe the direction bits to check
	 * @return {@code true} when the move is open, {@code false} otherwise
	 */
	private boolean checkNsweAntiCornerCut(int geoX, int geoY, int worldZ, int nswe)
	{
		boolean canMove = true;
		if (nswe == (Region.NSWE_NORTH | Region.NSWE_EAST))
		{
			canMove = _region.checkNearestNswe(geoX, geoY - 1, worldZ, Region.NSWE_EAST) && _region.checkNearestNswe(geoX + 1, geoY, worldZ, Region.NSWE_NORTH);
		}
		else if (nswe == (Region.NSWE_NORTH | Region.NSWE_WEST))
		{
			canMove = _region.checkNearestNswe(geoX, geoY - 1, worldZ, Region.NSWE_WEST) && _region.checkNearestNswe(geoX - 1, geoY, worldZ, Region.NSWE_NORTH);
		}
		else if (nswe == (Region.NSWE_SOUTH | Region.NSWE_EAST))
		{
			canMove = _region.checkNearestNswe(geoX, geoY + 1, worldZ, Region.NSWE_EAST) && _region.checkNearestNswe(geoX + 1, geoY, worldZ, Region.NSWE_SOUTH);
		}
		else if (nswe == (Region.NSWE_SOUTH | Region.NSWE_WEST))
		{
			canMove = _region.checkNearestNswe(geoX, geoY + 1, worldZ, Region.NSWE_WEST) && _region.checkNearestNswe(geoX - 1, geoY, worldZ, Region.NSWE_SOUTH);
		}
		
		return canMove && _region.checkNearestNswe(geoX, geoY, worldZ, nswe);
	}
	
	/**
	 * Lists a cell's layers bottom-up by climbing getNextHigherZ, which stops repeating itself once the top layer is reached.
	 * @param geoX the geo X coordinate
	 * @param geoY the geo Y coordinate
	 * @return the layer heights with their NSWE bits as a {@code String}
	 */
	private String layers(int geoX, int geoY)
	{
		final StringBuilder sb = new StringBuilder("{");
		int probeZ = -32000;
		for (int i = 0; i < 64; i++)
		{
			// Probe one unit up: getNextHigherZ returns worldZ itself on an exact layer hit, which would stop the climb on the first layer.
			// Layer heights are multiples of 8, so a reply of exactly probeZ + 1 is the "nothing higher" answer and ends the climb.
			final int higher = _region.getNextHigherZ(geoX, geoY, probeZ + 1);
			if (higher <= (probeZ + 1))
			{
				break;
			}
			
			sb.append(i > 0 ? ", " : "").append(higher).append(" ").append(nsweText(nsweOf(geoX, geoY, higher)));
			probeZ = higher;
		}
		
		return sb.append("}").toString();
	}
	
	/**
	 * Reads back a layer's open directions one bit at a time, the only NSWE view the region exposes.
	 * @param geoX the geo X coordinate
	 * @param geoY the geo Y coordinate
	 * @param worldZ the world Z coordinate
	 * @return the open direction bits as an {@code int}
	 */
	private int nsweOf(int geoX, int geoY, int worldZ)
	{
		int nswe = 0;
		for (int bit = Region.NSWE_EAST; bit <= Region.NSWE_NORTH; bit <<= 1)
		{
			if (_region.checkNearestNswe(geoX, geoY, worldZ, bit))
			{
				nswe |= bit;
			}
		}
		
		return nswe;
	}
	
	private static String nsweText(int nswe)
	{
		return ((nswe & Region.NSWE_NORTH) != 0 ? "N" : "-") + ((nswe & Region.NSWE_SOUTH) != 0 ? "S" : "-") + ((nswe & Region.NSWE_WEST) != 0 ? "W" : "-") + ((nswe & Region.NSWE_EAST) != 0 ? "E" : "-");
	}
	
	private static int computeNswe(int lastX, int lastY, int x, int y)
	{
		int nswe = 0;
		if (x > lastX)
		{
			nswe |= Region.NSWE_EAST;
		}
		else if (x < lastX)
		{
			nswe |= Region.NSWE_WEST;
		}
		
		if (y > lastY)
		{
			nswe |= Region.NSWE_SOUTH;
		}
		else if (y < lastY)
		{
			nswe |= Region.NSWE_NORTH;
		}
		
		return nswe;
	}
}
