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
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/**
 * The geodata builder: heightfield sampling, floor selection, NSWE, reachability flood fill and threshold bridging.
 * @author Mobius
 */
public class GridBuilder
{
	public static GeoGrid buildGrid(Path clientRoot, String map) throws IOException
	{
		return buildGrid(clientRoot, map, () -> false);
	}
	
	/**
	 * Builds a geodata grid while periodically checking a cooperative cancellation signal.
	 * @param clientRoot the client root directory
	 * @param map the map basename
	 * @param stopRequested cancellation signal
	 * @return the generated geodata grid
	 * @throws IOException when client data cannot be read
	 */
	public static GeoGrid buildGrid(Path clientRoot, String map, BooleanSupplier stopRequested) throws IOException
	{
		checkCancelled(stopRequested);
		Terrain loaded = null;
		try
		{
			loaded = Terrain.load(clientRoot, map);
		}
		catch (IOException e)
		{
			// Some blank ocean tiles ship without a heightmap texture: build from BSP/meshes only.
			System.out.println("No terrain for " + map + " (" + e.getMessage() + ") - using geometry only.");
		}
		
		final Terrain terrain = loaded;
		final double originX = terrain != null ? terrain.texelToWorldX(0) - 64 : 0; // quad(0,0) covers [texel0 .. texel1)
		final double originY = terrain != null ? terrain.texelToWorldY(0) - 64 : 0;
		final String[] xy = map.split("_");
		final double tileOriginX = (Integer.parseInt(xy[0]) - 20) * 32768;
		final double tileOriginY = (Integer.parseInt(xy[1]) - 18) * 32768;
		
		System.out.println("Collecting triangles...");
		final List<double[]> tris = GeometryCollector.collectTriangles(clientRoot, map, stopRequested);
		System.out.println("Triangles: " + tris.size());
		final List<double[]> water = GeometryCollector.collectWaterTriangles(new UnrealPackage(UnrealCrypt.decrypt(clientRoot.resolve("maps").resolve(map + ".unr"))), stopRequested);
		
		// Thin vertical walls (often lying exactly on cell boundaries) close NSWE edges instead of voiding cells: rasterize wall triangle footprints onto the cell-boundary grid.
		final Map<Long, List<double[]>> wallEdges = new HashMap<>();
		final Map<Long, List<double[]>> walkEdges = new HashMap<>();
		final Map<Long, List<double[]>> bevelEdges = new HashMap<>(); // walkable-but-sloped joints (steeper than BEVEL_NZ) crossing boundaries - butt-joined sheet seams
		final Map<Long, List<double[]>> connectEdges = new HashMap<>(); // walkable surfaces reaching EXACTLY onto the boundary line (hairline strips) - the connected-vs-butt-joint discriminator
		for (double[] t : tris)
		{
			checkCancelled(stopRequested);
			final double eax = t[3] - t[0];
			final double eay = t[4] - t[1];
			final double eaz = t[5] - t[2];
			final double ebx = t[6] - t[0];
			final double eby = t[7] - t[1];
			final double ebz = t[8] - t[2];
			final double enx = (eay * ebz) - (eaz * eby);
			final double eny = (eaz * ebx) - (eax * ebz);
			final double enz = (eax * eby) - (eay * ebx);
			final double enlen = Math.sqrt((enx * enx) + (eny * eny) + (enz * enz));
			if ((enlen > 0) && ((Math.abs(enz) / enlen) < GeoConfig.WALL_NZ))
			{
				GeoMath.rasterWallEdges(t, wallEdges, tileOriginX, tileOriginY);
			}
			else if (enlen > 0)
			{
				// Walkable-surface continuity: crossing a boundary requires walkable geometry ON it.
				GeoMath.rasterWallEdges(t, walkEdges, tileOriginX, tileOriginY);
				GeoMath.rasterConnectEdges(t, connectEdges, tileOriginX, tileOriginY);
				if ((Math.abs(enz) / enlen) < GeoConfig.BEVEL_NZ)
				{
					GeoMath.rasterWallEdges(t, bevelEdges, tileOriginX, tileOriginY);
				}
			}
		}
		
		// Bridge rails and balustrades exist ONLY as non-colliding visual sections (the collision data carries no rail at all):
		// their wall-like faces raster as movement fences on the boundary grid - crossings die in the body window and characters stay ON the deck,
		// while sight passes through the railing exactly as in the client.
		int fenceFaces = 0;
		for (double[] t : GeometryCollector.BRIDGE_FENCE_TRIS)
		{
			checkCancelled(stopRequested);
			final double eax = t[3] - t[0];
			final double eay = t[4] - t[1];
			final double eaz = t[5] - t[2];
			final double ebx = t[6] - t[0];
			final double eby = t[7] - t[1];
			final double ebz = t[8] - t[2];
			final double enx = (eay * ebz) - (eaz * eby);
			final double eny = (eaz * ebx) - (eax * ebz);
			final double enz = (eax * eby) - (eay * ebx);
			final double enlen = Math.sqrt((enx * enx) + (eny * eny) + (enz * enz));
			if ((enlen > 0) && ((Math.abs(enz) / enlen) < GeoConfig.WALL_NZ))
			{
				GeoMath.rasterWallEdges(t, wallEdges, tileOriginX, tileOriginY);
				fenceFaces++;
			}
		}
		
		System.out.println("Wall-blocked cell edges: " + wallEdges.size() + ", walk-connected edges: " + walkEdges.size() + ", bridge fence faces: " + fenceFaces);
		
		// Bucket triangles per geodata block (128x128 units).
		final int BUCKETS = 256;
		@SuppressWarnings("unchecked")
		final List<double[]>[] buckets = new List[BUCKETS * BUCKETS];
		for (double[] t : tris)
		{
			checkCancelled(stopRequested);
			final double minX = Math.min(t[0], Math.min(t[3], t[6]));
			final double maxX = Math.max(t[0], Math.max(t[3], t[6]));
			final double minY = Math.min(t[1], Math.min(t[4], t[7]));
			final double maxY = Math.max(t[1], Math.max(t[4], t[7]));
			final int bx0 = Math.max(0, (int) ((minX - tileOriginX) / 128));
			final int bx1 = Math.min(BUCKETS - 1, (int) ((maxX - tileOriginX) / 128));
			final int by0 = Math.max(0, (int) ((minY - tileOriginY) / 128));
			final int by1 = Math.min(BUCKETS - 1, (int) ((maxY - tileOriginY) / 128));
			for (int bx = bx0; bx <= bx1; bx++)
			{
				for (int by = by0; by <= by1; by++)
				{
					if (buckets[(bx * BUCKETS) + by] == null)
					{
						buckets[(bx * BUCKETS) + by] = new ArrayList<>();
					}
					
					buckets[(bx * BUCKETS) + by].add(t);
				}
			}
		}
		
		// Bucket water surface triangles the same way.
		@SuppressWarnings("unchecked")
		final List<double[]>[] waterBuckets = new List[BUCKETS * BUCKETS];
		for (double[] t : water)
		{
			checkCancelled(stopRequested);
			final int bx0 = Math.max(0, (int) ((Math.min(t[0], Math.min(t[3], t[6])) - tileOriginX) / 128));
			final int bx1 = Math.min(BUCKETS - 1, (int) ((Math.max(t[0], Math.max(t[3], t[6])) - tileOriginX) / 128));
			final int by0 = Math.max(0, (int) ((Math.min(t[1], Math.min(t[4], t[7])) - tileOriginY) / 128));
			final int by1 = Math.min(BUCKETS - 1, (int) ((Math.max(t[1], Math.max(t[4], t[7])) - tileOriginY) / 128));
			for (int bx = bx0; bx <= bx1; bx++)
			{
				for (int by = by0; by <= by1; by++)
				{
					if (waterBuckets[(bx * BUCKETS) + by] == null)
					{
						waterBuckets[(bx * BUCKETS) + by] = new ArrayList<>();
					}
					
					waterBuckets[(bx * BUCKETS) + by].add(t);
				}
			}
		}
		
		System.out.println("Sampling " + GeoConfig.CELLS + "x" + GeoConfig.CELLS + " cells (heightfield spans)...");
		final GeoGrid grid = new GeoGrid();
		final List<double[]> spans = new ArrayList<>();
		final List<double[]> wallPieces = new ArrayList<>(); // {lo, hi} wall-class clips inside the full cell
		double[] ceilHits = new double[64]; // reused center-ray ceiling hits, grown on demand (per-cell allocation shows up at 2048x2048)
		final short[] wallColZ = new short[GeoConfig.CELLS * GeoConfig.CELLS]; // pending wall-column tops (0 = none), applied AFTER threshold bridging so doorway flanks do not starve the bridge
		
		// Bridge-class footprints (bridge/pier/dock/quay mesh names) are exempt from the wall-column verdict: pier and abutment faces rising from the water read as wall chains and used to erase the walkable decks above them.
		final BitSet bridgeCells = new BitSet(GeoConfig.CELLS * GeoConfig.CELLS);
		for (double[] t : GeometryCollector.BRIDGE_TRIS)
		{
			checkCancelled(stopRequested);
			final int bcx0 = Math.max(0, (int) ((Math.min(t[0], Math.min(t[3], t[6])) - tileOriginX) / 16));
			final int bcx1 = Math.min(GeoConfig.CELLS - 1, (int) ((Math.max(t[0], Math.max(t[3], t[6])) - tileOriginX) / 16));
			final int bcy0 = Math.max(0, (int) ((Math.min(t[1], Math.min(t[4], t[7])) - tileOriginY) / 16));
			final int bcy1 = Math.min(GeoConfig.CELLS - 1, (int) ((Math.max(t[1], Math.max(t[4], t[7])) - tileOriginY) / 16));
			for (int bx = bcx0; bx <= bcx1; bx++)
			{
				for (int by = bcy0; by <= bcy1; by++)
				{
					bridgeCells.set((bx * GeoConfig.CELLS) + by);
				}
			}
		}
		
		// Stair-class footprints (mesh names carrying "stair"/"step") are exempt from the LONE-STEP rule: a staircase IS a sequence of risers, whatever its tread depth.
		// The rule looks one cell past a rise to tell a flight from a butt-joint seam, but Giran square's steps are 3 cells (48 units) deep,
		// so the next cell is flat tread and every riser read as a lone step - closing both flights one-way uphill. The mesh already says it is a staircase; believe it.
		final BitSet stairCells = new BitSet(GeoConfig.CELLS * GeoConfig.CELLS);
		for (double[] t : GeometryCollector.STAIR_TRIS)
		{
			checkCancelled(stopRequested);
			final int scx0 = Math.max(0, (int) ((Math.min(t[0], Math.min(t[3], t[6])) - tileOriginX) / 16));
			final int scx1 = Math.min(GeoConfig.CELLS - 1, (int) ((Math.max(t[0], Math.max(t[3], t[6])) - tileOriginX) / 16));
			final int scy0 = Math.max(0, (int) ((Math.min(t[1], Math.min(t[4], t[7])) - tileOriginY) / 16));
			final int scy1 = Math.min(GeoConfig.CELLS - 1, (int) ((Math.max(t[1], Math.max(t[4], t[7])) - tileOriginY) / 16));
			for (int sx = scx0; sx <= scx1; sx++)
			{
				for (int sy = scy0; sy <= scy1; sy++)
				{
					stairCells.set((sx * GeoConfig.CELLS) + sy);
				}
			}
		}
		
		final short[] wallColLo = new short[GeoConfig.CELLS * GeoConfig.CELLS]; // matching chain bases: a column only applies while a surviving floor still anchors its chain
		final BitSet wallCol = new BitSet(GeoConfig.CELLS * GeoConfig.CELLS);
		final Map<double[], String> spanSrc = new IdentityHashMap<>(); // debug only: span -> the actor/mesh/BSP that made it (see GeometryCollector.TRI_SOURCE)
		final List<double[]> pieces = new ArrayList<>(); // {top, walkableArea} per contributing surface, for void-strip detection.
		for (int cx = 0; cx < GeoConfig.CELLS; cx++)
		{
			checkCancelled(stopRequested);
			for (int cy = 0; cy < GeoConfig.CELLS; cy++)
			{
				if ((cy & 63) == 0)
				{
					checkCancelled(stopRequested);
				}
				final double cellX0 = tileOriginX + (cx * 16);
				final double cellY0 = tileOriginY + (cy * 16);
				spans.clear();
				pieces.clear();
				
				// Geometry spans (Recast-style): every triangle overlapping the CENTER 8x8 of this cell contributes the Z-range of its clipped area.
				// Walls become tall spans that leave no standable floor, arches/bridges become elevated spans that keep the passage below walkable.
				// The center sub-rectangle mirrors geodata semantics ("can a character stand at the cell center") and keeps thin edge structures (parapets, door jambs) from poisoning the whole cell - a wall thicker than ~8 units still covers cell centers.
				wallPieces.clear(); // wall-class geometry inside the FULL cell, for the wall-column verdict
				final List<double[]> bucket = buckets[((cx / 8) * BUCKETS) + (cy / 8)];
				if (bucket != null)
				{
					final double subX0 = cellX0 - 0.5; // Half-unit growth: surfaces touching exactly on the cell boundary must register (zero-area clips are rejected).
					final double subY0 = cellY0 - 0.5;
					final double subX1 = cellX0 + 16.5;
					final double subY1 = cellY0 + 16.5;
					for (double[] t : bucket)
					{
						// Cheap bounding-box reject before the polygon clip.
						if ((Math.min(t[0], Math.min(t[3], t[6])) > subX1) || (Math.max(t[0], Math.max(t[3], t[6])) < subX0) || (Math.min(t[1], Math.min(t[4], t[7])) > subY1) || (Math.max(t[1], Math.max(t[4], t[7])) < subY0))
						{
							continue;
						}
						
						final double ax = t[3] - t[0];
						final double ay = t[4] - t[1];
						final double az = t[5] - t[2];
						final double bx2 = t[6] - t[0];
						final double by2 = t[7] - t[1];
						final double bz2 = t[8] - t[2];
						final double nx = (ay * bz2) - (az * by2);
						final double ny = (az * bx2) - (ax * bz2);
						final double nz = (ax * by2) - (ay * bx2);
						final double nlen = Math.sqrt((nx * nx) + (ny * ny) + (nz * nz));
						final double tnz = nlen > 0 ? Math.abs(nz) / nlen : 0;
						final double walkable = (tnz >= GeoConfig.WALL_NZ) ? 1 : 0;
						
						// TALL wall faces (door jambs, building walls) clip against the strict center 8x8 only: an edge-grazing jamb must not merge the cell's real floor into a wall column (its blocking is handled by the edge-NSWE raster).
						// Floors and short ledge faces (curbs, lips - the seam logic) keep the grown full rect.
						final boolean tallWall = (walkable < 1) && ((Math.max(t[2], Math.max(t[5], t[8])) - Math.min(t[2], Math.min(t[5], t[8]))) > 64);
						
						// Wall extent over the FULL cell for the wall-column verdict: line-of-sight blocks by HEIGHT, so cells crossed by real walls must end up as wall-top columns, wherever the face lies in the cell.
						// The grown full-rect clip is needed by the wall-column verdict below AND by every non-tall-wall span, so it is computed at most once per triangle.
						final double[] fullRange = (walkable < 1) ? GeoMath.clipZRange(t, subX0, subY0, subX1, subY1) : null;
						if (fullRange != null)
						{
							wallPieces.add(new double[]
							{
								fullRange[0] + GeoConfig.GEODATA_Z_OFFSET,
								fullRange[1] + GeoConfig.GEODATA_Z_OFFSET
							});
						}
						
						final double[] range;
						if (tallWall)
						{
							range = GeoMath.clipZRange(t, cellX0 + 4, cellY0 + 4, cellX0 + 12, cellY0 + 12);
						}
						else if (walkable < 1)
						{
							range = fullRange;
						}
						else
						{
							range = GeoMath.clipZRange(t, subX0, subY0, subX1, subY1);
						}
						
						if (range == null)
						{
							continue;
						}
						
						final double[] span = new double[]
						{
							range[0] + GeoConfig.GEODATA_Z_OFFSET,
							range[1] + GeoConfig.GEODATA_Z_OFFSET,
							walkable,
							walkable >= 1 ? range[1] + GeoConfig.GEODATA_Z_OFFSET : -1e9,
							tnz,
							0
						};
						spans.add(span);
						if (GeoConfig.DEBUG_CELL != null)
						{
							spanSrc.put(span, GeometryCollector.TRI_SOURCE.get(t));
						}
						
						if (walkable >= 1)
						{
							pieces.add(new double[]
							{
								range[1] + GeoConfig.GEODATA_Z_OFFSET,
								range[2]
							});
						}
					}
				}
				
				// Terrain seed span (respecting quad visibility holes).
				// Added AFTER geometry so phantom shelves can be suppressed: where the heightmap grade runs THROUGH a building whose real floor lies well below it (sunken interiors - the Talking Island church nave sits 600 under the hill grade),
				// terrain would seed a mid-air layer that walls the interior off and starves its floors in the reachability pass.
				// A grade sandwiched between a walkable geometry floor below and cover starting above head height is such a phantom: skip it.
				// Cliff ledges (sky above) and pavement over buried grade (no floor below) keep their terrain.
				if (terrain != null)
				{
					final double px = cellX0 + 8;
					final double py = cellY0 + 8;
					final int qx = (int) ((px - originX) / 128);
					final int qy = (int) ((py - originY) / 128);
					
					// The heightmap is one texel narrower than the tile, so the last ~8-cell strip at the high edges maps one quad PAST coverage and would be left empty (a gap the client fills from the shared edge texel of the adjacent tile).
					// Allow one quad past and clamp the visibility/height sample to the last row, so terrain reaches the tile border and neighbouring regions meet seamlessly.
					if ((qx >= 0) && (qx <= terrain.uSize) && (qy >= 0) && (qy <= terrain.vSize) && terrain.isQuadVisible(Math.min(qx, terrain.uSize - 1), Math.min(qy, terrain.vSize - 1)))
					{
						final double z = terrain.heightAt(px, py);
						boolean floorBelow = false;
						boolean coverAbove = false;
						for (double[] sp : spans)
						{
							if ((sp[2] >= 1) && (sp[1] <= (z - 32)))
							{
								floorBelow = true;
							}
							
							if (sp[0] >= (z + GeoConfig.CHAR_HEIGHT_GEOM))
							{
								coverAbove = true;
							}
						}
						
						if (!floorBelow || !coverAbove)
						{
							// Local slope from the bilinear surface (16-unit gradient), like the old 16-unit ray sampling - quad-level slope overcloses 8x8 cell areas.
							final double sx = (terrain.heightAt(px + 8, py) - terrain.heightAt(px - 8, py)) / 16.0;
							final double sy = (terrain.heightAt(px, py + 8) - terrain.heightAt(px, py - 8)) / 16.0;
							final double tnz = 1.0 / Math.sqrt(1 + (sx * sx) + (sy * sy));
							
							// Cell height = MAX over the cell (center + corners), matching the reference sampler: a character anywhere in the cell stands at its highest point.
							// On uniform slopes neighbors shift equally, but at slope BREAKS (building foundation berms) the straddling cell takes the high side - real climbs cross the >16 boundary exactly where the reference weaves its wall aprons.
							double zMax = z;
							zMax = Math.max(zMax, terrain.heightAt(cellX0 + 4, cellY0 + 4));
							zMax = Math.max(zMax, terrain.heightAt(cellX0 + 12, cellY0 + 4));
							zMax = Math.max(zMax, terrain.heightAt(cellX0 + 4, cellY0 + 12));
							zMax = Math.max(zMax, terrain.heightAt(cellX0 + 12, cellY0 + 12));
							spans.add(new double[]
							{
								zMax - 4,
								zMax,
								1,
								zMax,
								tnz,
								1
							});
							pieces.add(new double[]
							{
								z,
								256
							});
						}
					}
				}
				
				// Span debug: -DdebugCell=cx,cy dumps every raw span of one cell.
				if ((GeoConfig.DEBUG_CELL != null) && (cx == GeoConfig.DEBUG_CELL[0]) && (cy == GeoConfig.DEBUG_CELL[1]))
				{
					System.out.println("DEBUG cell(" + cx + "," + cy + ") spans:");
					for (double[] s : spans)
					{
						System.out.printf("  [%.1f .. %.1f] walkable=%.0f bestWalk=%.1f nz=%.2f terrain=%.0f  <- %s%n", s[0], s[1], s[2], s[3], s[4], s[5], spanSrc.getOrDefault(s, "terrain/water/other"));
					}
				}
				
				if (spans.isEmpty())
				{
					continue;
				}
				
				// Merge overlapping spans into a heightfield column.
				spans.sort((a, b3) -> Double.compare(a[0], b3[0]));
				if ((GeoConfig.DEBUG_CELL != null) && (cx == GeoConfig.DEBUG_CELL[0]) && (cy == GeoConfig.DEBUG_CELL[1]))
				{
					System.out.println("DEBUG cell(" + cx + "," + cy + ") sorted spans:");
					for (double[] s : spans)
					{
						System.out.printf("  [%.1f .. %.1f] walkable=%.0f bestWalk=%.1f nz=%.2f terrain=%.0f  <- %s%n", s[0], s[1], s[2], s[3], s[4], s[5], spanSrc.getOrDefault(s, "terrain/water/other"));
					}
				}
				
				int merged = 0;
				for (int i = 1; i < spans.size(); i++)
				{
					final double[] cur = spans.get(merged);
					final double[] next = spans.get(i);
					if (next[0] <= (cur[1] + 8))
					{
						cur[3] = Math.max(cur[3], next[3]); // Remember the highest walkable surface inside the span.
						cur[5] = Math.min(cur[5], next[5]); // Any geometry piece makes the merged span geometry-sourced.
						if (next[1] > cur[1])
						{
							// The top of the merged span (and its walkability/slope) comes from the higher piece, unless the previous walkable top is nearly at the same height.
							final boolean keepPrev = (next[2] < 1) && (cur[2] >= 1) && ((next[1] - cur[1]) < 4);
							cur[2] = keepPrev ? 1 : next[2];
							cur[4] = keepPrev ? cur[4] : next[4];
							cur[1] = next[1];
						}
						else if ((next[2] >= 1) && ((cur[1] - next[1]) < 4))
						{
							cur[2] = 1;
							cur[4] = Math.max(cur[4], next[4]);
						}
					}
					else
					{
						merged++;
						spans.set(merged, next);
					}
				}
				
				// NOTE on the Giran harbor "walk-on-water" plane: an invisible collidable BSP sheet spans the whole bay at plaza height (flags 0x0 like real streets, ONE surface continuous with the plaza floor, quay rail carries no collision).
				// Every generic discriminator was tried and failed: water volumes overlap land and run under towns legitimately, bay terrain quads are invisible holes exactly like town building holes,
				// thin-BSP-sheet detection matches ordinary town pavement, the quay skirt supports the plane like a real deck, and hover or seabed tests read identically on the dry plaza.
				// Bounded client-data limit: server-side zones or the future navmesh pass are the remaining candidates.
				
				// Standable floors: walkable span tops with enough clearance to the span above.
				final int base = ((cx * GeoConfig.CELLS) + cy) * GeoConfig.MAX_LAYERS;
				int layers = 0;
				for (int i = merged; (i >= 0) && (layers < GeoConfig.MAX_LAYERS); i--)
				{
					final double[] s = spans.get(i);
					
					// Standable when the top is walkable, or when it is a small non-walkable lip (beveled curb, coping stone) within climb range of a walkable surface below.
					if ((s[2] < 1) && ((s[1] - s[3]) > GeoConfig.STAND_LIP))
					{
						continue;
					}
					
					if (i < merged)
					{
						// Headroom at the CELL CENTER (single vertical ray): the lowest surface above this floor along the center vertical.
						// Off-center decor hanging into the subrect (railing balusters over the Giran shop walkway) is ignored.
						// Hits are clustered (gap <= 8, the span-merge convention) and THIN FLOATING clusters are skipped: a hanging prop under 16 units thick starting 48+ over the floor (the Gludio wall lamp bottoms out 79.9 over the street and killed the floor by 0.1 units) is brushed past by the client, not a ceiling.
						// Thick clusters (beams, vaults) and low starts (fences, sills) block as before.
						double ceil = spans.get(i + 1)[0];
						if (bucket != null)
						{
							int hitCount = 0;
							for (double[] t : bucket)
							{
								final double cz2 = GeoMath.triangleZ(t, cellX0 + 8, cellY0 + 8) + GeoConfig.GEODATA_Z_OFFSET;
								if (!Double.isNaN(cz2) && (cz2 > (s[1] + 8)))
								{
									if (hitCount == ceilHits.length)
									{
										ceilHits = Arrays.copyOf(ceilHits, ceilHits.length * 2);
									}
									
									ceilHits[hitCount++] = cz2;
								}
							}
							
							Arrays.sort(ceilHits, 0, hitCount);
							ceil = Double.POSITIVE_INFINITY;
							int c = 0;
							while (c < hitCount)
							{
								final double clusterBase = ceilHits[c];
								double clusterTop = clusterBase;
								while (((c + 1) < hitCount) && (ceilHits[c + 1] <= (clusterTop + 8)))
								{
									c++;
									clusterTop = ceilHits[c];
								}
								
								if (((clusterTop - clusterBase) > 16) || ((clusterBase - s[1]) < 48))
								{
									ceil = clusterBase;
									break;
								}
								
								c++;
							}
						}
						
						if ((ceil - s[1]) < GeoConfig.CHAR_HEIGHT_GEOM)
						{
							continue;
						}
					}
					
					double zTop = s[1];
					boolean lifted = false;
					if ((layers == 0) && (bucket != null))
					{
						// Corner-ray rule: a ledge lip crossing the cell's ORIGIN corner owns the cell and lifts it to lip height.
						// Lifted heights quantize UP (e.g. -3006 -> -3000), preserving the 24-unit jump that closes ledge crossings while 16-unit stair risers stay open.
						for (double[] t : bucket)
						{
							final double cz = GeoMath.triangleZ(t, cellX0, cellY0) + GeoConfig.GEODATA_Z_OFFSET;
							if (!Double.isNaN(cz) && (cz > zTop) && ((cz - s[1]) <= 40))
							{
								zTop = cz;
								lifted = true;
							}
						}
					}
					
					// NOTE on sealed tower interiors (walk-through stone towers): every static rule tried for them (steep cover over ground-grade floors,
					// with neighbor and flood enclosure tests) also fired on legitimate covered ground - orc village halls, plaza strips under roof eaves,
					// church naves - because the battered tower base reads as passable to every geometric test.
					// Left OPEN by design: walking into a tower is cosmetic (cells stay open both ways, no stuck spot), false blocks are not.
					// Sealing specific towers is what the --blockcells hand-edit command is for.
					
					// Void-strip detection: a cell whose walkable surfaces near the floor cover almost none of its area (gap between pavement sheets, sliver overhangs) wedges characters in the client - block it entirely.
					if (layers == 0)
					{
						double coverage = 0;
						for (double[] piece : pieces)
						{
							if ((piece[0] >= (s[1] - 16)) && (piece[0] <= (zTop + 8)))
							{
								coverage += piece[1];
							}
						}
						
						if (coverage < 90)
						{
							// A THIN sliver far above a real floor is a lintel band / trim (church porch arches, span extent ~2): skip it and keep selecting floors below.
							// A standable surface NEAR the sliver means a true void strip (sub-cell trench): block the cell.
							// Judge the low-coverage span by its height over the cell's next standable floor: NEAR (<=32) = void strip between sheets (sub-cell trench) -> block;
							// a THIN band above that (lintels, extent <=16) or ANY span floating >300 (chandeliers over the guild hall) -> skip, keep selecting floors below;
							// a THICK span within 32..300 of its floor (bridge parapets, rocky stair risers) -> STAND ON ITS TOP: the real-height climb rule blocks stepping onto it and the plunge/abyss rules guard the far side,
							// so it acts as a bump like the reference data, not a solid cell. No standable below at all (true pillars) -> wall verdict.
							boolean skipSpan = false;
							boolean nearTrench = false;
							boolean anchored = false;
							double anchorTop = 0;
							double fallbackTop = Double.NaN;
							for (int j = i - 1; j >= 0; j--)
							{
								final double[] s2 = spans.get(j);
								if ((s2[2] >= 1) || ((s2[1] - s2[3]) <= GeoConfig.STAND_LIP))
								{
									// A candidate without standing headroom is decorative trim (church rood-screen moldings 60 under the arch) ONLY when a deeper real floor exists - keep it as fallback: for a stair riser rising straight off the street, the street IS the walking level.
									// The DEEPEST fallback wins: a stacked fixture (the Gludio wall lamp, two tiers 12 apart over a street with 79.9 headroom) must anchor on the street below it, not on its own lower tier - anchoring on the tier read as "sliver 16.8 over its floor" = the trench signature and walled the whole cell.
									if ((spans.get(j + 1)[0] - s2[1]) < GeoConfig.CHAR_HEIGHT_GEOM)
									{
										fallbackTop = s2[1];
										continue;
									}
									
									anchored = true;
									anchorTop = s2[1];
									break;
								}
							}
							
							if (!anchored && !Double.isNaN(fallbackTop))
							{
								anchored = true;
								anchorTop = fallbackTop;
							}
							
							if (anchored)
							{
								final double gap = zTop - anchorTop;
								nearTrench = gap <= 32;
								
								// Tiny floating fixtures (wall lamps, brackets: a few units of walkable area hovering 100+ over the street) are decor, not standable bumps - skip them entirely.
								skipSpan = (gap > 32) && (((s[1] - s[0]) <= 16) || (gap > 300) || ((gap > 100) && (coverage < 40)));
							}
							
							if (skipSpan)
							{
								continue;
							}
							
							// The no-anchor wall verdict needs NEAR-ZERO coverage (true void strips and pillars): a wall-hugging street sliver still carries real walkable area and stays a floor - the wall itself blocks crossings via the edge intervals and wall columns, matching the reference's open wall-base cells.
							if (nearTrench || (!anchored && (coverage < 16)))
							{
								grid.layerZ[base] = (short) (Math.floorDiv((int) Math.round(zTop), 8) * 8);
								grid.noFloor.set((cx * GeoConfig.CELLS) + cy);
								if ((GeoConfig.DEBUG_CELL != null) && (cx == GeoConfig.DEBUG_CELL[0]) && (cy == GeoConfig.DEBUG_CELL[1]))
								{
									System.out.println("DEBUG noFloor(" + cx + "," + cy + ") <- VOID-STRIP: nearTrench=" + nearTrench + " anchored=" + anchored + " coverage=" + coverage + " zTop=" + zTop + " anchorTop=" + anchorTop);
								}
								
								layers = 1;
								break;
							}
							
							// Thick 32..300 span: fall through and store its top as a standable layer.
						}
					}
					
					grid.layerZ[base + layers] = (short) ((lifted ? -Math.floorDiv(-((int) Math.round(zTop)), 8) : Math.floorDiv((int) Math.round(zTop), 8)) * 8); // Heightunit 8: lifted cells ceil, others floor.
					grid.layerZFrac[base + layers] = (byte) (Math.round(zTop) - grid.layerZ[base + layers]); // Real height survives for the climb rules; the file stores the quantized value.
					layers++;
				}
				
				if ((layers == 0) && (merged >= 0))
				{
					final double[] top = spans.get(merged);
					if (((top[1] - top[3]) <= 64) || ((top[1] - top[0]) <= 64))
					{
						// Short ledge/step face: stand on its top, normal NSWE (climb rule guards entry).
						// Includes faces with NO walkable piece in-cell (rocky stair risers crossing the center) - the reference weaves directional closures over them, never solid cells.
						grid.layerZ[base] = (short) (Math.floorDiv((int) Math.round(top[1]), 8) * 8);
						grid.layerZFrac[base] = (byte) (Math.round(top[1]) - grid.layerZ[base]);
					}
					else if (top[3] > -1e8)
					{
						// Tall wall piece crossing a cell that still has real floor beside it: stand on the floor; the wall itself blocks crossing via the edge-NSWE pass.
						grid.layerZ[base] = (short) (Math.floorDiv((int) Math.round(top[3]), 8) * 8);
						grid.layerZFrac[base] = (byte) (Math.round(top[3]) - grid.layerZ[base]);
					}
					else
					{
						// Pure wall/tower body: all-blocked 0000.
						grid.layerZ[base] = (short) (Math.floorDiv((int) Math.round(top[1]), 8) * 8);
						grid.noFloor.set((cx * GeoConfig.CELLS) + cy);
						if ((GeoConfig.DEBUG_CELL != null) && (cx == GeoConfig.DEBUG_CELL[0]) && (cy == GeoConfig.DEBUG_CELL[1]))
						{
							System.out.println("DEBUG noFloor(" + cx + "," + cy + ") <- PURE-WALL-BODY: top=[" + top[0] + ".." + top[1] + "]");
						}
					}
					
					layers = 1;
				}
				
				// Wall-column verdict, the LINE-OF-SIGHT rule: canSeeTarget blocks by HEIGHT,
				// so a cell crossed by a real wall must carry the WALL-TOP height as a solid column - directional bits on a ground-height cell stop movement but sight (and spells) pass straight over them.
				// Wall pieces are chained bottom-up (gap <= 16) so a low curb plus a high eave cannot fake a wall; a chain rising from walking level (start <= floor+24) to more than WALL_COLUMN above the floor converts the cell.
				// Door boxes exempt the actual openings, so doors and town-gate holes stay walkable.
				if ((layers > 0) && !grid.noFloor.get((cx * GeoConfig.CELLS) + cy) && !wallPieces.isEmpty())
				{
					// Bridge-class cells (bridge/pier/dock/quay mesh footprints) qualify chains against ANY of their floors,
					// but the chain top must REACH the deck (the top walking layer): pier and abutment faces rising from the water cap far below the deck and never qualify - converting them erased the decks - while quay parapets and rails top out AT or above deck level (the Giran quay wall climbs 100+ from the water to a lip 3 over the deck, anchored on its own mid-ledge) and become the side-line columns.
					final boolean bridgeCell = bridgeCells.get((cx * GeoConfig.CELLS) + cy);
					final double topReal = grid.layerZ[base] + grid.layerZFrac[base];
					final double floorZ = grid.layerZ[(base + layers) - 1] + grid.layerZFrac[(base + layers) - 1];
					
					// Local water surface: a wall chain whose BASE lies under it is a SEAWALL (quay/retaining wall) rising out of the harbor - see the seawall guard below.
					double waterZ = Double.NaN;
					final List<double[]> wallWb = waterBuckets[((cx / 8) * BUCKETS) + (cy / 8)];
					if (wallWb != null)
					{
						for (double[] w : wallWb)
						{
							final double wz = GeoMath.triangleZ(w, cellX0 + 8, cellY0 + 8);
							if (!Double.isNaN(wz) && (Double.isNaN(waterZ) || ((wz + GeoConfig.GEODATA_Z_OFFSET) > waterZ)))
							{
								waterZ = wz + GeoConfig.GEODATA_Z_OFFSET;
							}
						}
					}
					
					// Real water HERE: the ground under this cell lies below the surface (exposed seabed).
					// The bay WaterVolume spans the whole map at -3780, so "base under the surface" alone also matches inland town walls whose foundations merely sink into terrain - those stand on ground hundreds of units ABOVE the buried plane.
					final boolean realWater = (terrain != null) && !Double.isNaN(waterZ) && (terrain.heightAt(cellX0 + 8, cellY0 + 8) < waterZ);
					wallPieces.sort((a, b4) -> Double.compare(a[0], b4[0]));
					double chainLo = Double.NaN;
					double chainHi = 0;
					double wallTop = Double.NaN;
					double wallLo = Double.NaN;
					for (double[] wp : wallPieces)
					{
						if (Double.isNaN(chainLo) || (wp[0] > (chainHi + 16)))
						{
							// Close the previous chain: the FIRST chain rising from walking level past the threshold sets the column top - its own top,
							// so a pew or statue caps at object height instead of chaining through pillars into the vault and going solid at roof height.
							boolean qualifies = false;
							if (!Double.isNaN(chainLo))
							{
								if (bridgeCell)
								{
									if (chainHi >= (topReal - 16))
									{
										for (int k = 0; (k < layers) && !qualifies; k++)
										{
											final double f = grid.layerZ[base + k] + grid.layerZFrac[base + k];
											qualifies = (chainLo <= (f + 24)) && (chainHi > (f + GeoConfig.WALL_COLUMN));
										}
									}
								}
								else
								{
									qualifies = (chainLo <= (floorZ + 24)) && (chainHi > (floorZ + GeoConfig.WALL_COLUMN));
								}
							}
							
							// Seawall guard: a wall rising OUT OF THE WATER is a quay/retaining wall - its top IS the shoreline you stand on, not a sight-blocker,
							// so it must stay a walkable floor or the plaza dead-ends one cell short of the harbor and you cannot reach the edge (or step off into the bay).
							// Gated on realWater so only walls standing in the actual bay qualify; town walls (magic shop) stand on dry ground and still column, keeping line-of-sight blocked.
							if (qualifies && realWater && (chainLo < waterZ))
							{
								qualifies = false;
							}
							
							if (qualifies)
							{
								wallTop = chainHi;
								wallLo = chainLo;
								break;
							}
							
							chainLo = wp[0];
							chainHi = wp[1];
						}
						else
						{
							chainHi = Math.max(chainHi, wp[1]);
						}
					}
					
					if (Double.isNaN(wallTop) && !Double.isNaN(chainLo))
					{
						boolean qualifies = false;
						if (bridgeCell)
						{
							if (chainHi >= (topReal - 16))
							{
								for (int k = 0; (k < layers) && !qualifies; k++)
								{
									final double f = grid.layerZ[base + k] + grid.layerZFrac[base + k];
									qualifies = (chainLo <= (f + 24)) && (chainHi > (f + GeoConfig.WALL_COLUMN));
								}
							}
						}
						else
						{
							qualifies = (chainLo <= (floorZ + 24)) && (chainHi > (floorZ + GeoConfig.WALL_COLUMN));
						}
						
						// Seawall guard: a wall rising OUT OF THE WATER is a quay/retaining wall - its top IS the shoreline you stand on, not a sight-blocker,
						// so it must stay a walkable floor or the plaza dead-ends one cell short of the harbor and you cannot reach the edge (or step off into the bay).
						// Gated on realWater so only walls standing in the actual bay qualify; town walls (magic shop) stand on dry ground and still column, keeping line-of-sight blocked.
						if (qualifies && realWater && (chainLo < waterZ))
						{
							qualifies = false;
						}
						
						if (qualifies)
						{
							wallTop = chainHi;
							wallLo = chainLo;
						}
					}
					
					if (!Double.isNaN(wallTop) && !GeometryCollector.inDoorBox(cellX0 + 8, cellY0 + 8, floorZ))
					{
						// Recorded now, applied AFTER threshold bridging: converting doorway flank cells here would starve the bridge of its flanking floors (Giran blacksmith threshold).
						wallColZ[(cx * GeoConfig.CELLS) + cy] = (short) (Math.floorDiv((int) Math.round(wallTop), 8) * 8);
						wallColLo[(cx * GeoConfig.CELLS) + cy] = (short) (Math.floorDiv((int) Math.round(wallLo), 8) * 8);
						wallCol.set((cx * GeoConfig.CELLS) + cy);
					}
				}
				
				grid.layerCount[(cx * GeoConfig.CELLS) + cy] = (byte) layers;
				if ((GeoConfig.DEBUG_CELL != null) && (cx == GeoConfig.DEBUG_CELL[0]) && (cy == GeoConfig.DEBUG_CELL[1]))
				{
					System.out.print("DEBUG floors pre-BFS:");
					for (int k = 0; k < layers; k++)
					{
						System.out.print(" " + grid.layerZ[base + k]);
					}
					
					System.out.println(grid.noFloor.get((cx * GeoConfig.CELLS) + cy) ? " (noFloor)" : "");
				}
			}
		}
		
		// Threshold bridging: a cell with no floor near z, flanked on OPPOSITE sides by floors at matching heights, is a doorway threshold whose walk plate has no collision geometry (Giran blacksmith: street and interior both -3600, threshold row floorless).
		// Bridge it with the lower neighbor height. Runs AFTER the reachability flood fill: gatehouse cells arrive here with all layer slots eaten by shelf layers the BFS later removes (Giran castle clan hall door), so bridging must see the post-BFS stack.
		final int[] bridged =
		{
			0
		};
		final Runnable bridgePass = () ->
		{
			for (int cx = 1; cx < (GeoConfig.CELLS - 1); cx++)
			{
				checkCancelled(stopRequested);
				for (int cy = 1; cy < (GeoConfig.CELLS - 1); cy++)
				{
					if ((cy & 63) == 0)
					{
						checkCancelled(stopRequested);
					}
					final int cell = (cx * GeoConfig.CELLS) + cy;
					if ((GeoConfig.DEBUG_CELL != null) && (cx == GeoConfig.DEBUG_CELL[0]) && (cy == GeoConfig.DEBUG_CELL[1]))
					{
						System.out.println("DEBUG bridge(" + cx + "," + cy + ") count=" + grid.layerCount[cell] + " noFloor=" + grid.noFloor.get(cell) + " neighborsW/E counts=" + grid.layerCount[((cx - 1) * GeoConfig.CELLS) + cy] + "/" + grid.layerCount[((cx + 1) * GeoConfig.CELLS) + cy] + " neighborW z0=" + grid.layerZ[(((cx - 1) * GeoConfig.CELLS) + cy) * GeoConfig.MAX_LAYERS] + " neighborE z0=" + grid.layerZ[(((cx + 1) * GeoConfig.CELLS) + cy) * GeoConfig.MAX_LAYERS]);
					}
					
					if (grid.layerCount[cell] >= GeoConfig.MAX_LAYERS)
					{
						continue;
					}
					
					final boolean noFloorCell = grid.noFloor.get(cell);
					for (int axis = 0; axis < 2; axis++)
					{
						final int ax = axis == 0 ? 1 : 0;
						final int ay = axis == 0 ? 0 : 1;
						final int cellA = (((cx - ax) * GeoConfig.CELLS) + (cy - ay));
						final int cellB = (((cx + ax) * GeoConfig.CELLS) + (cy + ay));
						if (grid.noFloor.get(cellA) || grid.noFloor.get(cellB))
						{
							continue;
						}
						
						for (int i = 0; i < grid.layerCount[cellA]; i++)
						{
							final short za = grid.layerZ[(cellA * GeoConfig.MAX_LAYERS) + i];
							for (int j = 0; j < grid.layerCount[cellB]; j++)
							{
								final short zb = grid.layerZ[(cellB * GeoConfig.MAX_LAYERS) + j];
								if (Math.abs(za - zb) > GeoConfig.MAX_CLIMB)
								{
									continue;
								}
								
								final short zm = (short) Math.min(za, zb);
								boolean has = false;
								for (int k = 0; k < grid.layerCount[cell]; k++)
								{
									if (Math.abs(grid.layerZ[(cell * GeoConfig.MAX_LAYERS) + k] - zm) <= GeoConfig.MAX_CLIMB)
									{
										has = true;
										break;
									}
								}
								
								if (!has && noFloorCell)
								{
									// Wall-marker cell (0000): bridge ONLY the arch-over-threshold case - the stored top sits far above the flanking floors (a doorway arch, dark elf guild: -3312 over -3600) and no wall face occupies walking height on either crossing boundary.
									// Real thin walls and void strips (Gludio trench) fail these tests and keep their 0000.
									if ((grid.layerZ[cell * GeoConfig.MAX_LAYERS] - zm) < 128)
									{
										continue;
									}
									
									final long ekA = axis == 0 ? GeoMath.edgeKey(cx - 1, cy, 0) : GeoMath.edgeKey(cx, cy - 1, 1);
									final long ekB = axis == 0 ? GeoMath.edgeKey(cx, cy, 0) : GeoMath.edgeKey(cx, cy, 1);
									boolean walled = false;
									for (int e = 0; e < 2; e++)
									{
										final List<double[]> wl = wallEdges.get((e == 0) ? ekA : ekB);
										if (wl != null)
										{
											for (double[] we : wl)
											{
												if ((we[0] <= (zm + 80)) && (we[1] >= (zm + 8)))
												{
													walled = true;
													break;
												}
											}
										}
										
										if (walled)
										{
											break;
										}
									}
									
									if (walled)
									{
										continue;
									}
									
									// Replace the wall marker with the bridged floor.
									grid.layerCount[cell] = 0;
									grid.noFloor.clear(cell);
								}
								
								if (!has && (grid.layerCount[cell] < GeoConfig.MAX_LAYERS))
								{
									// Insert keeping top-first (descending z) order.
									int pos = grid.layerCount[cell];
									for (int k = 0; k < grid.layerCount[cell]; k++)
									{
										if (grid.layerZ[(cell * GeoConfig.MAX_LAYERS) + k] < zm)
										{
											pos = k;
											break;
										}
									}
									
									for (int k = grid.layerCount[cell]; k > pos; k--)
									{
										grid.layerZ[(cell * GeoConfig.MAX_LAYERS) + k] = grid.layerZ[((cell * GeoConfig.MAX_LAYERS) + k) - 1];
										grid.layerZFrac[(cell * GeoConfig.MAX_LAYERS) + k] = grid.layerZFrac[((cell * GeoConfig.MAX_LAYERS) + k) - 1];
									}
									
									grid.layerZ[(cell * GeoConfig.MAX_LAYERS) + pos] = zm;
									grid.layerZFrac[(cell * GeoConfig.MAX_LAYERS) + pos] = 0;
									grid.layerCount[cell]++;
									bridged[0]++;
								}
							}
						}
					}
				}
			}
			
			System.out.println("Bridged " + bridged[0] + " threshold cells.");
		};
		
		final BitSet loneStepEdge = new BitSet(GeoConfig.CELLS * GeoConfig.CELLS * 4); // cell*4 + dir index: crossings the lone-step rule closed, seeds for seam-line propagation
		final BitSet wallSealed = new BitSet(GeoConfig.CELLS * GeoConfig.CELLS); // cells the in-cell VISUAL-WALL seal closed: they sit at FLOOR height (the red wall line), so the flat-neighborhood opener must not mistake them for quay artifacts (church pillar rows bridged by the threshold pass looked perfectly flat)
		
		// THE WALL-BODY MAP (rule: "store wall geometry, any cell touching the wall blocks that direction"): every stored wall face is clipped into every cell it crosses,
		// giving each cell the exact z-intervals of wall standing IN it - the buttress body, the mid-cell wall plane, the base flare alike, independent of chains, anchors or boundary strips.
		// The NSWE pass then refuses to step INTO a cell whose wall interval crosses the mover's body window.
		final Map<Integer, List<double[]>> wallBody = new HashMap<>();
		for (double[] t : GeometryCollector.VISUAL_WALL_TRIS)
		{
			final int wbx0 = Math.max(0, (int) ((Math.min(t[0], Math.min(t[3], t[6])) - tileOriginX) / 16));
			final int wbx1 = Math.min(GeoConfig.CELLS - 1, (int) ((Math.max(t[0], Math.max(t[3], t[6])) - tileOriginX) / 16));
			final int wby0 = Math.max(0, (int) ((Math.min(t[1], Math.min(t[4], t[7])) - tileOriginY) / 16));
			final int wby1 = Math.min(GeoConfig.CELLS - 1, (int) ((Math.max(t[1], Math.max(t[4], t[7])) - tileOriginY) / 16));
			for (int wx = wbx0; wx <= wbx1; wx++)
			{
				for (int wy = wby0; wy <= wby1; wy++)
				{
					final double[] range = GeoMath.clipZRange(t, tileOriginX + (wx * 16), tileOriginY + (wy * 16), tileOriginX + (wx * 16) + 16, tileOriginY + (wy * 16) + 16);
					if (range == null)
					{
						continue;
					}
					
					wallBody.computeIfAbsent((wx * GeoConfig.CELLS) + wy, k -> new ArrayList<>()).add(new double[]
					{
						range[0] + GeoConfig.GEODATA_Z_OFFSET,
						range[1] + GeoConfig.GEODATA_Z_OFFSET
					});
				}
			}
		}
		
		System.out.println("Wall-body cells: " + wallBody.size());
		
		// THE STORED WALL MESH (rule: store MESH geometry, not cell geometry): raw world-space triangles of the collisionless architecture, bucketed for lookup.
		// The NSWE pass tests each crossing against these actual faces - when the segment between two cells intersects a stored wall face, that direction blocks.
		@SuppressWarnings("unchecked")
		final List<double[]>[] wallMesh = new List[BUCKETS * BUCKETS];
		for (double[] t : GeometryCollector.VISUAL_WALL_TRIS)
		{
			final int b0x = Math.max(0, (int) ((Math.min(t[0], Math.min(t[3], t[6])) - tileOriginX) / 128));
			final int b1x = Math.min(BUCKETS - 1, (int) ((Math.max(t[0], Math.max(t[3], t[6])) - tileOriginX) / 128));
			final int b0y = Math.max(0, (int) ((Math.min(t[1], Math.min(t[4], t[7])) - tileOriginY) / 128));
			final int b1y = Math.min(BUCKETS - 1, (int) ((Math.max(t[1], Math.max(t[4], t[7])) - tileOriginY) / 128));
			for (int bx = b0x; bx <= b1x; bx++)
			{
				for (int by = b0y; by <= b1y; by++)
				{
					if (wallMesh[(bx * BUCKETS) + by] == null)
					{
						wallMesh[(bx * BUCKETS) + by] = new ArrayList<>();
					}
					
					wallMesh[(bx * BUCKETS) + by].add(t);
				}
			}
		}
		
		final Runnable nswePass = () ->
		{
			System.out.println("Computing NSWE...");
			loneStepEdge.clear();
			wallSealed.clear();
			
			// CORE wall cells: the face crosses the walking band inside the cell's OWN rect (+-0.5). Precomputed so the seal below can paint the wall line ONE cell thick: a cell caught only by the +-16 reach stays open when a core cell sits next to it - the face owns its own cell,
			// the reach exists solely for battered facades whose band-height face hangs over a floorless wall-body cell (no core bit there, reach survives).
			final BitSet coreSealed = new BitSet(GeoConfig.CELLS * GeoConfig.CELLS); // any layer core-crossed (neighborhood test)
			final BitSet coreSealedLayer = new BitSet(GeoConfig.CELLS * GeoConfig.CELLS * GeoConfig.MAX_LAYERS); // this layer core-crossed (the seal itself stays per-layer: a crypt floor under a core-sealed nave layer keeps its openness)
			for (int cx = 0; cx < GeoConfig.CELLS; cx++)
			{
				checkCancelled(stopRequested);
				for (int cy = 0; cy < GeoConfig.CELLS; cy++)
				{
					if ((cy & 63) == 0)
					{
						checkCancelled(stopRequested);
					}
					final List<double[]> wmc = wallMesh[((cx / 8) * BUCKETS) + (cy / 8)];
					if ((wmc == null) || grid.noFloor.get((cx * GeoConfig.CELLS) + cy))
					{
						continue;
					}
					
					final int cn = grid.count(cx, cy);
					for (int layer = 0; layer < cn; layer++)
					{
						final double zRealCell = grid.realZ(cx, cy, layer) - GeoConfig.GEODATA_Z_OFFSET;
						for (double[] wt : wmc)
						{
							final double[] range = GeoMath.clipZRange(wt, (tileOriginX + (cx * 16)) - 0.5, (tileOriginY + (cy * 16)) - 0.5, tileOriginX + (cx * 16) + 16.5, tileOriginY + (cy * 16) + 16.5);
							if ((range != null) && (range[0] <= (zRealCell + GeoConfig.WALL_WINDOW_TOP)) && (range[1] >= (zRealCell + 24)))
							{
								coreSealed.set((cx * GeoConfig.CELLS) + cy);
								coreSealedLayer.set((((cx * GeoConfig.CELLS) + cy) * GeoConfig.MAX_LAYERS) + layer);
								break;
							}
						}
					}
				}
			}
			
			// E=1, W=2, S=4, N=8. North = -Y, East = +X.
			final int[][] dirs =
			{
				// @formatter:off
				{ 1, 0, 1 }, // east
				{ -1, 0, 2 }, // west
				{ 0, 1, 4 }, // south
				{ 0, -1, 8 }, // north
				// @formatter:on
			};
			
			for (int cx = 0; cx < GeoConfig.CELLS; cx++)
			{
				checkCancelled(stopRequested);
				for (int cy = 0; cy < GeoConfig.CELLS; cy++)
				{
					if ((cy & 63) == 0)
					{
						checkCancelled(stopRequested);
					}
					final int n = grid.count(cx, cy);
					if (grid.noFloor.get((cx * GeoConfig.CELLS) + cy))
					{
						continue; // Wall body: stays all-blocked (0000).
					}
					
					for (int layer = 0; layer < n; layer++)
					{
						final short z = grid.z(cx, cy, layer);
						int nswe = 0;
						
						// THE CELL INTERSECTS A MESH WALL (rule): clip the stored wall faces to THIS cell - a face crossing the walking band [z+24 .. z+WALL_WINDOW_TOP] means the wall stands IN the cell,
						// so the cell is blocked outright: a red, impassable cell at floor height along the whole wall footprint. Stair risers stay under +24 and never qualify; doorway lanes hold no face at all.
						final List<double[]> wmc = wallMesh[((cx / 8) * BUCKETS) + (cy / 8)];
						boolean inDoorLane = false;
						if (wmc != null)
						{
							final double dpx = tileOriginX + (cx * 16) + 8;
							final double dpy = tileOriginY + (cy * 16) + 8;
							for (double[] db : GeometryCollector.DOOR_BOXES)
							{
								if ((dpx >= db[0]) && (dpx <= db[2]) && (dpy >= db[1]) && (dpy <= db[3]))
								{
									inDoorLane = true; // a doorway: the full-cell reach would catch the jamb faces from every lane cell and seal the entrance shut
									break;
								}
							}
						}
						
						if ((wmc != null) && !inDoorLane)
						{
							final double zRealCell = grid.realZ(cx, cy, layer) - GeoConfig.GEODATA_Z_OFFSET;
							boolean wallInCell = coreSealedLayer.get((((cx * GeoConfig.CELLS) + cy) * GeoConfig.MAX_LAYERS) + layer);
							if (!wallInCell)
							{
								// Not a core cell: the +-16 reach (one full cell) still seals battered facades - the Gothic ground band slopes inward, its band-height face hanging over a floorless wall-body cell - but ONLY when no core cell exists in the 3x3 neighborhood: next to a core cell the reach would just fatten the wall line to 2-3 cells (one face used to paint every touching cell red).
								boolean coreNear = false;
								for (int ox = Math.max(0, cx - 1); (ox <= Math.min(GeoConfig.CELLS - 1, cx + 1)) && !coreNear; ox++)
								{
									for (int oy = Math.max(0, cy - 1); oy <= Math.min(GeoConfig.CELLS - 1, cy + 1); oy++)
									{
										if (coreSealed.get((ox * GeoConfig.CELLS) + oy))
										{
											coreNear = true;
											break;
										}
									}
								}
								
								if (!coreNear)
								{
									for (double[] wt : wmc)
									{
										final double[] range = GeoMath.clipZRange(wt, (tileOriginX + (cx * 16)) - 16, (tileOriginY + (cy * 16)) - 16, tileOriginX + (cx * 16) + 32, tileOriginY + (cy * 16) + 32);
										if ((range != null) && (range[0] <= (zRealCell + GeoConfig.WALL_WINDOW_TOP)) && (range[1] >= (zRealCell + 24)))
										{
											wallInCell = true;
											break;
										}
									}
								}
							}
							
							if (wallInCell)
							{
								grid.layerNswe[(((cx * GeoConfig.CELLS) + cy) * GeoConfig.MAX_LAYERS) + layer] = 0;
								if (layer == 0)
								{
									wallSealed.set((cx * GeoConfig.CELLS) + cy);
								}
								continue;
							}
						}
						
						for (int[] d : dirs)
						{
							final int nx = cx + d[0];
							final int ny = cy + d[1];
							if ((nx < 0) || (nx >= GeoConfig.CELLS) || (ny < 0) || (ny >= GeoConfig.CELLS))
							{
								nswe |= d[2]; // Region border: leave open, the next region continues.
								continue;
							}
							
							// Empty padding in the region's EDGE BAND (a cell with no geometry, near the tile border) is where our terrain data stops before the seam - the adjacent map continues the world there.
							// Leave the edge OPEN toward it so cross-region movement is not walled off by our own ocean padding (do not block toward the nearby map).
							// Scoped to the border band so interior voids elsewhere still block normally.
							if ((grid.count(nx, ny) == 0) && ((nx < 48) || (nx >= (GeoConfig.CELLS - 48)) || (ny < 48) || (ny >= (GeoConfig.CELLS - 48))))
							{
								nswe |= d[2];
								continue;
							}
							
							if (grid.noFloor.get((nx * GeoConfig.CELLS) + ny))
							{
								continue; // Never step into a wall body.
							}
							
							// Stored-mesh intersection (rule: when a cell intersects a mesh wall, block that direction - as simple as it can be): the segment from this cell's centre to the neighbour's centre,
							// cast at knee and chest height from THIS layer's z to the neighbour's nearest z. Crossing a stored wall face blocks the direction, exactly where the mesh stands.
							final List<double[]> wm = wallMesh[((cx / 8) * BUCKETS) + (cy / 8)];
							if (wm != null)
							{
								final double zRealHere = grid.realZ(cx, cy, layer);
								double nbz2 = Double.NaN;
								double nbd2 = Double.MAX_VALUE;
								final int nbn2 = grid.count(nx, ny);
								for (int k = 0; k < nbn2; k++)
								{
									final double f = grid.realZ(nx, ny, k);
									final double diff = Math.abs(f - zRealHere);
									if (diff < nbd2)
									{
										nbd2 = diff;
										nbz2 = f;
									}
								}
								
								if (!Double.isNaN(nbz2))
								{
									final double sax = tileOriginX + (cx * 16) + 8;
									final double say = tileOriginY + (cy * 16) + 8;
									final double sbx = tileOriginX + (nx * 16) + 8;
									final double sby = tileOriginY + (ny * 16) + 8;
									final double saz = zRealHere - GeoConfig.GEODATA_Z_OFFSET;
									final double sbz = nbz2 - GeoConfig.GEODATA_Z_OFFSET;
									boolean hit = false;
									for (double[] wt : wm)
									{
										if (GeoMath.segmentHitsTriangle(wt, sax, say, saz + 32, sbx, sby, sbz + 32) || GeoMath.segmentHitsTriangle(wt, sax, say, saz + 64, sbx, sby, sbz + 64))
										{
											hit = true;
											break;
										}
									}
									
									if (hit)
									{
										continue;
									}
								}
							}
							
							// Touching the wall (rule): the neighbour cell holds stored wall geometry crossing THIS layer's body window - the wall stands in the cell ahead at walking height,
							// so this direction closes, whatever the chains and verdicts elsewhere make of that cell.
							final List<double[]> wb2 = wallBody.get((nx * GeoConfig.CELLS) + ny);
							if (wb2 != null)
							{
								boolean wallAhead = false;
								for (double[] w : wb2)
								{
									if ((w[0] <= (z + GeoConfig.WALL_WINDOW_TOP)) && (w[1] >= (z + 24)))
									{
										wallAhead = true;
										break;
									}
								}
								
								if (wallAhead)
								{
									continue;
								}
							}
							
							// A wall face on the shared boundary blocks crossing when it intersects body height [z+50, z+90]: above climbable steps/curbs, below pass-under lintels.
							// Interval lists keep door sills separate from lintels.
							final long ekey = d[2] == 1 ? GeoMath.edgeKey(cx, cy, 0) : d[2] == 2 ? GeoMath.edgeKey(cx - 1, cy, 0) : d[2] == 4 ? GeoMath.edgeKey(cx, cy, 1) : GeoMath.edgeKey(cx, cy - 1, 1);
							final List<double[]> ebList = wallEdges.get(ekey);
							boolean edgeBlocked = false;
							if (ebList != null)
							{
								// The crossing is judged from BOTH sides: the window tests against this layer's z AND against the neighbour's nearest layer. A wall interval sitting in the window of only ONE side (raised interior floor against a street-level wall base - the wall tops out below the high side's +24) used to leave the crossing open from the high side, walking characters out through the wall the low side could never enter.
								double nbz = Double.NaN;
								double nbDiff = Double.MAX_VALUE;
								final int nbn = grid.count(nx, ny);
								for (int k = 0; k < nbn; k++)
								{
									final double f = grid.realZ(nx, ny, k);
									final double diff = Math.abs(f - z);
									if (diff < nbDiff)
									{
										nbDiff = diff;
										nbz = f;
									}
								}
								
								for (double[] eb : ebList)
								{
									// Body window: anything solid crossing [z+24 .. z+WALL_WINDOW_TOP] stops a character - sills and lintels (50-76), knee walls and fences rising past 24, and free-floating waist-height bands (bench tops, rails) alike.
									// Below 24 = climbable steps/curbs; intervals starting above the top are ceilings legally walked under (vault undersides 85-92 over temple floors).
									if (((eb[0] <= (z + GeoConfig.WALL_WINDOW_TOP)) && (eb[1] >= (z + 24))) || (!Double.isNaN(nbz) && (eb[0] <= (nbz + GeoConfig.WALL_WINDOW_TOP)) && (eb[1] >= (nbz + 24))))
									{
										edgeBlocked = true;
										break;
									}
								}
							}
							
							if ((GeoConfig.DEBUG_CELL != null) && (cx == GeoConfig.DEBUG_CELL[0]) && (cy == GeoConfig.DEBUG_CELL[1]))
							{
								final StringBuilder sb = new StringBuilder("DEBUG nswe(" + cx + "," + cy + ") z=" + z + " dir=" + d[2] + " blocked=" + edgeBlocked + " intervals:");
								if (ebList == null)
								{
									sb.append(" NONE");
								}
								else
								{
									for (double[] eb : ebList)
									{
										sb.append(String.format(" [%.0f..%.0f]", eb[0], eb[1]));
									}
								}
								
								System.out.println(sb);
							}
							
							if (edgeBlocked)
							{
								continue;
							}
							
							// Nearest neighbor layer by REAL (unquantized) height: the climb rule runs on real sampled heights.
							// Quantized storage collapses climbs 17-23 into 16 or 24, hiding the retail boundary (close > 16 real).
							final double zReal = grid.realZ(cx, cy, layer);
							final int nn = grid.count(nx, ny);
							double best = -Double.MAX_VALUE;
							double bestDiff = Double.MAX_VALUE;
							for (int k = 0; k < nn; k++)
							{
								final double candidateZ = grid.realZ(nx, ny, k);
								final double diff = Math.abs(candidateZ - zReal);
								if (diff < bestDiff)
								{
									bestDiff = diff;
									best = candidateZ;
								}
							}
							
							// Inside a stair-class mesh footprint (both cells) the cap loosens to STAIR_CLIMB: church-square flights carry 17-32 unit risers the client climbs freely.
							if ((nn == 0) || ((best - zReal) > ((stairCells.get((cx * GeoConfig.CELLS) + cy) && stairCells.get((nx * GeoConfig.CELLS) + ny)) ? GeoConfig.STAIR_CLIMB : GeoConfig.MAX_CLIMB)))
							{
								continue;
							}
							
							// Lone-step rule: stairs are SEQUENCES of risers, butt-joined pavement seams are lone steps with flat sheets on both sides.
							// A stair-sized-plus rise (over STAIRS_HEIGHT, legal under MAX_CLIMB) closes one-way uphill unless the ascent continues ahead or arrives from a rise behind -
							// the Gludio trench seam is a 12-unit sheet step whose 45-degree joint exists only as texture (no collision face of any class, hulls and passable surfaces included),
							// so it is byte-identical to a boundary stair riser locally and only the missing staircase context gives it away. The client capsule wedges into such joints.
							// A sloped walkable joint ON the boundary (real beveled seams that DO carry collision) closes the same way via bevelEdges.
							if ((best - zReal) > GeoConfig.STAIRS_HEIGHT)
							{
								final List<double[]> bvList = bevelEdges.get(ekey);
								boolean bevelBlocked = false;
								if (bvList != null)
								{
									for (double[] bv : bvList)
									{
										if ((bv[0] <= (best + 8)) && (bv[1] >= (zReal - 8)))
										{
											bevelBlocked = true;
											break;
										}
									}
								}
								
								// A walkable surface at the UPPER level reaching exactly onto the boundary line means the sheets are JOINED (an architectural step - the Gludio temple nave crosses a 12-unit interior step the client walks freely): only DISCONNECTED butt joints wedge the capsule.
								// The hairline strip misses upper sheets that stop 1+ units short.
								boolean joined = false;
								if (!bevelBlocked)
								{
									final List<double[]> cnList = connectEdges.get(ekey);
									if (cnList != null)
									{
										for (double[] cn : cnList)
										{
											if ((cn[0] <= (best + 8)) && (cn[1] >= (best - 8)))
											{
												joined = true;
												break;
											}
										}
									}
								}
								
								boolean stairContext = joined || stairCells.get((cx * GeoConfig.CELLS) + cy);
								if (!bevelBlocked && !stairContext)
								{
									// Ahead: the cell beyond the target keeps climbing.
									final int ax = nx + d[0];
									final int ay = ny + d[1];
									if ((ax >= 0) && (ax < GeoConfig.CELLS) && (ay >= 0) && (ay < GeoConfig.CELLS) && !grid.noFloor.get((ax * GeoConfig.CELLS) + ay))
									{
										final int an = grid.count(ax, ay);
										double aBest = Double.NaN;
										double aDiff = Double.MAX_VALUE;
										for (int k = 0; k < an; k++)
										{
											final double candidateZ = grid.realZ(ax, ay, k);
											final double diff = Math.abs(candidateZ - best);
											if (diff < aDiff)
											{
												aDiff = diff;
												aBest = candidateZ;
											}
										}
										
										stairContext = !Double.isNaN(aBest) && ((aBest - best) > 4) && ((aBest - best) <= GeoConfig.MAX_CLIMB);
									}
									
									// Behind: this cell was itself reached by climbing.
									if (!stairContext)
									{
										final int bx = cx - d[0];
										final int by = cy - d[1];
										if ((bx >= 0) && (bx < GeoConfig.CELLS) && (by >= 0) && (by < GeoConfig.CELLS) && !grid.noFloor.get((bx * GeoConfig.CELLS) + by))
										{
											final int bn = grid.count(bx, by);
											double bBest = Double.NaN;
											double bDiff = Double.MAX_VALUE;
											for (int k = 0; k < bn; k++)
											{
												final double candidateZ = grid.realZ(bx, by, k);
												final double diff = Math.abs(candidateZ - zReal);
												if (diff < bDiff)
												{
													bDiff = diff;
													bBest = candidateZ;
												}
											}
											
											stairContext = !Double.isNaN(bBest) && ((zReal - bBest) > 4) && ((zReal - bBest) <= GeoConfig.MAX_CLIMB);
										}
									}
								}
								
								// A stair-class mesh footprint beats the bevel closure too: its sloped tread variants (Giran_Square_stair03_Summer, nz 0.5-0.9) read as beveled boundary joints,
								// but the mesh says stairs - the wedge-seam bevels this rule exists for are plain pavement, never stair-named.
								if ((bevelBlocked && !stairCells.get((cx * GeoConfig.CELLS) + cy)) || !stairContext)
								{
									if ((GeoConfig.DEBUG_CELL != null) && (cx == GeoConfig.DEBUG_CELL[0]) && (cy == GeoConfig.DEBUG_CELL[1]))
									{
										System.out.println("DEBUG lone-step(" + cx + "," + cy + ") dir=" + d[2] + " CLOSED: bevelBlocked=" + bevelBlocked + " stairContext=" + stairContext + " rise=" + (best - zReal));
									}
									
									loneStepEdge.set((((cx * GeoConfig.CELLS) + cy) * 4) + Integer.numberOfTrailingZeros(d[2]));
									continue;
								}
							}
							
							// Fall-off rule: a side whose neighbour floor is more than 32 below is a real drop-off - a dock edge, quay lip, building roof or ledge.
							// Leave it OPEN so the character can step/fall off by choice; the plunge and abyss rules below (which would close it as a lethal/water dive) are skipped.
							// Walls and railings still block (they are wall faces caught by the edge test above, or noFloor neighbours), and the bridge fall-guard re-closes bridge-class decks afterwards - so bridges stay blocked, everything else is fall-able.
							// Small drops (<= 32) fall through to the normal rules (stairs, curbs).
							if (((z - best) > 32) && ((z - best) <= GeoConfig.ABYSS_FALL))
							{
								nswe |= d[2];
								continue;
							}
							
							// Abyss rule: a fall deeper than ABYSS_FALL is lethal-depth (dark elf village walkways 3100 over the pit; the web railings carry no collision geometry at all) - the edge closes.
							if ((z - best) > GeoConfig.ABYSS_FALL)
							{
								continue;
							}
							
							// Deep-water plunge rule: falls higher than GeoConfig.WATER_FALL_MIN whose landing lies under the local water surface close the edge (quay walls, bridge sides over the Giran harbor).
							// Gentle shore entries (small drops) stay open - the server runs AllowWater=true and swimmers enter at beaches and stairs.
							if ((z - best) > GeoConfig.WATER_FALL_MIN)
							{
								final List<double[]> wb = waterBuckets[((nx / 8) * BUCKETS) + (ny / 8)];
								if (wb != null)
								{
									final double wpx = tileOriginX + (nx * 16) + 8;
									final double wpy = tileOriginY + (ny * 16) + 8;
									boolean plunge = false;
									for (double[] w : wb)
									{
										final double wz = GeoMath.triangleZ(w, wpx, wpy) + GeoConfig.GEODATA_Z_OFFSET;
										if (!Double.isNaN(wz) && (wz >= (best - 8)))
										{
											plunge = true;
											break;
										}
									}
									
									if (plunge)
									{
										continue;
									}
								}
							}
							
							// Surface continuity: crossing requires walkable geometry ON the shared boundary near floor level, or visible terrain connecting it - otherwise the client wedges characters into the gap between disconnected sheets (sub-cell trenches).
							final boolean inGapsBox = (GeoConfig.GAPS_BOX != null) && (cx >= GeoConfig.GAPS_BOX[0]) && (cx <= GeoConfig.GAPS_BOX[2]) && (cy >= GeoConfig.GAPS_BOX[1]) && (cy <= GeoConfig.GAPS_BOX[3]);
							if (inGapsBox)
							{
								final double loZ = Math.min(z, best) - 24;
								final double hiZ = Math.max(z, best) + 24;
								boolean connected = false;
								
								// In surgical box mode the target is structured pavement: buried terrain grading close to floor level must not masquerade as a connection.
								if ((terrain != null) && !inGapsBox)
								{
									final double midX = tileOriginX + (cx * 16) + 8 + (d[0] * 8);
									final double midY = tileOriginY + (cy * 16) + 8 + (d[1] * 8);
									final int tqx = (int) ((midX - originX) / 128);
									final int tqy = (int) ((midY - originY) / 128);
									if ((tqx >= 0) && (tqx < terrain.uSize) && (tqy >= 0) && (tqy < terrain.vSize) && terrain.isQuadVisible(tqx, tqy))
									{
										final double tz = terrain.heightAt(midX, midY);
										connected = (tz >= loZ) && (tz <= hiZ);
									}
								}
								
								if (!connected)
								{
									final List<double[]> weList = walkEdges.get(ekey);
									if (weList != null)
									{
										for (double[] we : weList)
										{
											if ((we[0] <= hiZ) && (we[1] >= loZ))
											{
												connected = true;
												break;
											}
										}
									}
								}
								
								if (!connected)
								{
									continue;
								}
							}
							
							nswe |= d[2];
						}
						
						grid.layerNswe[(((cx * GeoConfig.CELLS) + cy) * GeoConfig.MAX_LAYERS) + layer] = (byte) nswe;
					}
				}
			}
		};
				
		// Sub-cell grooves: a narrow depression between two walkable sheets (like the diagonal pavement groove at Gludio, 10-20 units wide and 24-40 deep with a solid bottom) hides INSIDE cells,
		// so boundary and coverage tests never see it while the client wedges characters into it.
		// A 4x4 ray grid per geometry cell finds them: a ray is DEEP when the topmost surface under it lies more than 16 units below the cell floor,
		// and a deep run of 8+ units BOUNDED by at-floor coverage on both sides (in-cell or a same-height neighbor floor) is a groove - the cell becomes a wall.
		// Stairs and ledge edges stay open: their low side has no same-height bound.
		System.out.println("Groove scan...");
		int grooves = 0;
		final boolean[][] atFloor = new boolean[4][4];
		for (int cx = 1; cx < (GeoConfig.CELLS - 1); cx++)
		{
			checkCancelled(stopRequested);
			for (int cy = 1; cy < (GeoConfig.CELLS - 1); cy++)
			{
				if ((cy & 63) == 0)
				{
					checkCancelled(stopRequested);
				}
				final int cell = (cx * GeoConfig.CELLS) + cy;
				
				// SINGLE-LAYER cells only. Sub-cell trenches live in plain walking surfaces (the Gludio pavement seams are all single-layer);
				// judging any layer of a stacked cell has repeatedly deleted real floors instead - roof-eave slivers (Talking Island),
				// buried caves under roads (Orc Village), cave-roof layers over the whole dark elf village.
				// Stair-class mesh footprints are exempt (same as the lone-step rule): a flight ascending ALONG an axis keeps same-height neighbors on both sides (+-8 per cell),
				// so its lower treads read as a bounded deep run and the scan walls the staircase.
				if ((grid.layerCount[cell] != 1) || grid.noFloor.get(cell) || stairCells.get(cell))
				{
					continue;
				}
				
				final List<double[]> bucket = buckets[((cx / 8) * BUCKETS) + (cy / 8)];
				if (bucket == null)
				{
					continue;
				}
				
				final double cellX0 = tileOriginX + (cx * 16);
				final double cellY0 = tileOriginY + (cy * 16);
				final double z0 = grid.layerZ[cell * GeoConfig.MAX_LAYERS];
				
				// Terrain-backed cells are continuous by construction.
				boolean terrainBacked = false;
				if (terrain != null)
				{
					final double px = cellX0 + 8;
					final double py = cellY0 + 8;
					final int qx = (int) ((px - originX) / 128);
					final int qy = (int) ((py - originY) / 128);
					terrainBacked = (qx >= 0) && (qx < terrain.uSize) && (qy >= 0) && (qy < terrain.vSize) && terrain.isQuadVisible(qx, qy);
					if (terrainBacked && (Math.abs(terrain.heightAt(px, py) - z0) <= 16))
					{
						continue;
					}
				}
				
				int deepCount = 0;
				for (int i = 0; i < 4; i++)
				{
					for (int j = 0; j < 4; j++)
					{
						final double px = cellX0 + 2 + (i * 4);
						final double py = cellY0 + 2 + (j * 4);
						boolean hit = false;
						for (double[] tri : bucket)
						{
							final double tz = GeoMath.triangleZ(tri, px, py);
							if (!Double.isNaN(tz) && (Math.abs((tz + GeoConfig.GEODATA_Z_OFFSET) - z0) <= 20))
							{
								hit = true;
								break;
							}
						}
						
						if (!hit && terrainBacked && (terrain != null) && (Math.abs(terrain.heightAt(px, py) - z0) <= 20))
						{
							hit = true;
						}
						
						atFloor[i][j] = hit;
						
						if (!hit)
						{
							deepCount++;
						}
					}
				}
				
				if (deepCount < 2)
				{
					continue;
				}
				
				// Same-height floors beyond the cell border count as at-floor bounds.
				final boolean west = hasFloorNear(grid, cx - 1, cy, z0);
				final boolean east = hasFloorNear(grid, cx + 1, cy, z0);
				final boolean north = hasFloorNear(grid, cx, cy - 1, z0);
				final boolean south = hasFloorNear(grid, cx, cy + 1, z0);
				boolean groove = false;
				for (int j = 0; (j < 4) && !groove; j++)
				{
					groove = boundedMissRun(atFloor[0][j], atFloor[1][j], atFloor[2][j], atFloor[3][j], west, east);
				}
				
				for (int i = 0; (i < 4) && !groove; i++)
				{
					groove = boundedMissRun(atFloor[i][0], atFloor[i][1], atFloor[i][2], atFloor[i][3], north, south);
				}
				
				if (groove)
				{
					grid.layerCount[cell] = 1;
					grid.noFloor.set(cell);
					grooves++;
					
					if ((GeoConfig.DEBUG_CELL != null) && (cell == ((GeoConfig.DEBUG_CELL[0] * GeoConfig.CELLS) + GeoConfig.DEBUG_CELL[1])))
					{
						System.out.println("DEBUG noFloor(" + GeoConfig.DEBUG_CELL[0] + "," + GeoConfig.DEBUG_CELL[1] + ") <- GROOVE");
					}
				}
			}
		}
		
		System.out.println("Groove cells: " + grooves);
		
		// Void-strip bridge (the DIAGONAL-edge fix): a small pocket of cells sitting far BELOW a surrounding continuous surface is not a real hole - it is a sub-cell VOID STRIP between two BSP sheets the sampler found NO geometry in at all (empty spans, so its only floor is the terrain far below).
		// The Giran dock is the class: its plaza edge and its parapet both run DIAGONALLY and are offset by a cell, so strips of nothing sit between them and a player walking the dock drops through into the bay.
		// An AXIS-ALIGNED edge (the Giran town bridge) tiles onto the cell grid exactly and never leaves these - hence its clean continuous edge.
		// The client carries no collision in the strip either and simply wedges characters, so bridging it at the surrounding level matches client behaviour.
		// Flood the REGION rather than judging one cell: strips run two or more cells wide along the diagonal, and adjacent voids would otherwise disqualify each other (each sees a neighbour that is not above it) so neither would ever bridge.
		// VOID_MAX caps the pocket, so the open bay - thousands of connected cells - can never fill.
		// Runs BEFORE nswePass so NSWE is computed against the repaired surface.
		final int VOID_MAX = 3;
		final int[][] voidDirs =
		{
			// @formatter:off
			{ 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 },
			// @formatter:on
		};
		final BitSet voidSeen = new BitSet(GeoConfig.CELLS * GeoConfig.CELLS);
		int bridgedVoids = 0;
		final List<int[]> region = new ArrayList<>();
		final ArrayDeque<int[]> voidQueue = new ArrayDeque<>();
		for (int scx = 1; scx < (GeoConfig.CELLS - 1); scx++)
		{
			checkCancelled(stopRequested);
			for (int scy = 1; scy < (GeoConfig.CELLS - 1); scy++)
			{
				final int seed = (scx * GeoConfig.CELLS) + scy;
				if (voidSeen.get(seed) || (grid.layerCount[seed] == 0) || grid.noFloor.get(seed))
				{
					continue;
				}
				
				// The pocket must sit ON the terrain: where the sampler found no geometry the floor falls through to the landscape underneath.
				// A man-made surface floats well clear of it, and that gap is what separates a BSP seam from an ordinary dip in the ground.
				final double z0 = grid.realZ(scx, scy, 0);
				if ((terrain == null) || (Math.abs(z0 - terrain.heightAt(tileOriginX + (scx * 16) + 8, tileOriginY + (scy * 16) + 8)) > 32))
				{
					continue;
				}
				
				region.clear();
				voidQueue.clear();
				voidQueue.add(new int[]
				{
					scx,
					scy
				});
				voidSeen.set(seed);
				boolean tooBig = false;
				while (!voidQueue.isEmpty() && !tooBig)
				{
					checkCancelled(stopRequested);
					final int[] c = voidQueue.poll();
					region.add(c);
					if (region.size() > VOID_MAX)
					{
						tooBig = true;
						break;
					}
					
					for (int[] d : voidDirs)
					{
						final int nx = c[0] + d[0];
						final int ny = c[1] + d[1];
						if ((nx < 1) || (nx >= (GeoConfig.CELLS - 1)) || (ny < 1) || (ny >= (GeoConfig.CELLS - 1)))
						{
							continue;
						}
						
						final int nc = (nx * GeoConfig.CELLS) + ny;
						if (voidSeen.get(nc) || (grid.layerCount[nc] == 0) || grid.noFloor.get(nc))
						{
							continue;
						}
						
						if (Math.abs(grid.realZ(nx, ny, 0) - z0) <= 32)
						{
							voidSeen.set(nc);
							voidQueue.add(new int[]
							{
								nx,
								ny
							});
						}
					}
				}
				
				// Anything bigger than a seam is real ground - the plaza, the seabed, the open bay - and must be left exactly as it is.
				if (tooBig)
				{
					continue;
				}
				
				// Every cell bordering the pocket must stand well above it and form ONE surface; a wall body (noFloor) or a side level with us means this is not an enclosed seam.
				double lo = Double.MAX_VALUE;
				double hi = -Double.MAX_VALUE;
				boolean enclosed = true;
				for (int i = 0; (i < region.size()) && enclosed; i++)
				{
					for (int[] d : voidDirs)
					{
						final int nx = region.get(i)[0] + d[0];
						final int ny = region.get(i)[1] + d[1];
						if ((nx < 1) || (nx >= (GeoConfig.CELLS - 1)) || (ny < 1) || (ny >= (GeoConfig.CELLS - 1)))
						{
							enclosed = false;
							break;
						}
						
						final int nc = (nx * GeoConfig.CELLS) + ny;
						if ((grid.layerCount[nc] == 0) || grid.noFloor.get(nc))
						{
							enclosed = false;
							break;
						}
						
						final double f = grid.realZ(nx, ny, 0);
						if (Math.abs(f - z0) <= 32)
						{
							continue; // a fellow void inside the pocket
						}
						
						if ((f - z0) <= 32)
						{
							enclosed = false;
							break;
						}
						
						// The rim must be BUILT (a floor standing clear of the landscape). Terrain rimmed by terrain is an ordinary hollow in the ground - a rock pocket,
						// a stream bed, a crater - and filling those would flatten the map: 23_12 alone offered ~30k of them. Only a pocket walled in by structure is a BSP seam.
						if (Math.abs(f - terrain.heightAt(tileOriginX + (nx * 16) + 8, tileOriginY + (ny * 16) + 8)) <= 32)
						{
							enclosed = false;
							break;
						}
						
						lo = Math.min(lo, f);
						hi = Math.max(hi, f);
					}
				}
				
				if (!enclosed || (lo == Double.MAX_VALUE) || ((hi - lo) > GeoConfig.STAND_LIP))
				{
					continue;
				}
				
				for (int[] c : region)
				{
					final int fc = (c[0] * GeoConfig.CELLS) + c[1];
					if (grid.layerCount[fc] >= GeoConfig.MAX_LAYERS)
					{
						continue;
					}
					
					final int fbase = fc * GeoConfig.MAX_LAYERS;
					for (int k = grid.layerCount[fc]; k > 0; k--)
					{
						grid.layerZ[fbase + k] = grid.layerZ[(fbase + k) - 1];
						grid.layerZFrac[fbase + k] = grid.layerZFrac[(fbase + k) - 1];
						grid.layerNswe[fbase + k] = grid.layerNswe[(fbase + k) - 1];
					}
					
					grid.layerZ[fbase] = (short) (Math.floorDiv((int) Math.round(lo), 8) * 8);
					grid.layerZFrac[fbase] = (byte) (Math.round(lo) - grid.layerZ[fbase]);
					grid.layerNswe[fbase] = 15;
					grid.layerCount[fc]++;
					bridgedVoids++;
				}
			}
		}
		
		System.out.println("Void strips bridged: " + bridgedVoids);
		
		nswePass.run();
		
		// Reachability: layers no character can reach from terrain-level ground - enclosed underground shells, crypts, hollow wall interiors - serve no purpose and pollute the data, so they are dropped.
		// BFS from terrain-matching layers across open NSWE edges (climb up to GeoConfig.MAX_CLIMB, falls of any depth), then drop unreached NON-TOP layers.
		// Top layers always stay: the landscape itself is not in question.
		System.out.println("Reachability flood fill...");
		final BitSet reachable = new BitSet(GeoConfig.CELLS * GeoConfig.CELLS * GeoConfig.MAX_LAYERS);
		final BitSet coveredGround = new BitSet(GeoConfig.CELLS * GeoConfig.CELLS); // terrain-grade top layer with geometry overhead (roofed interiors)
		final IntQueue seedIds = new IntQueue();
		final IntQueue queue = new IntQueue();
		for (int cx = 0; cx < GeoConfig.CELLS; cx++)
		{
			checkCancelled(stopRequested);
			for (int cy = 0; cy < GeoConfig.CELLS; cy++)
			{
				if ((cy & 63) == 0)
				{
					checkCancelled(stopRequested);
				}
				final int count = grid.layerCount[(cx * GeoConfig.CELLS) + cy];
				if (count == 0)
				{
					continue;
				}
				
				double tz = Double.NaN;
				if (terrain != null)
				{
					final double px = tileOriginX + (cx * 16) + 8;
					final double py = tileOriginY + (cy * 16) + 8;
					final int qx = (int) ((px - originX) / 128);
					final int qy = (int) ((py - originY) / 128);
					if ((qx >= 0) && (qx < terrain.uSize) && (qy >= 0) && (qy < terrain.vSize) && terrain.isQuadVisible(qx, qy))
					{
						tz = terrain.heightAt(px, py);
					}
				}
				
				// Seed: TOP layers only - at terrain level (outdoor ground), or unconditionally where no terrain exists.
				// Buried terrain under raised floors also sits at "terrain level" but is never the top layer, so sealed crawlspaces do not seed themselves reachable.
				if (Double.isNaN(tz) || (Math.abs(grid.z(cx, cy, 0) - tz) <= 32))
				{
					// Covered ground does not seed itself: sealed tower interiors sit at terrain grade under their cone roof and must stay unreached (the reference emits flood-VISITED cells only - towers are solid there).
					// Enterable covered interiors (halls, naves, underpasses) are reached through their openings instead.
					boolean covered = false;
					final List<double[]> seedBucket = buckets[((cx / 8) * BUCKETS) + (cy / 8)];
					if (seedBucket != null)
					{
						final double px = tileOriginX + (cx * 16) + 8;
						final double py = tileOriginY + (cy * 16) + 8;
						final double zTop = grid.realZ(cx, cy, 0);
						for (double[] t : seedBucket)
						{
							final double cz = GeoMath.triangleZ(t, px, py) + GeoConfig.GEODATA_Z_OFFSET;
							if (!Double.isNaN(cz) && (cz > (zTop + GeoConfig.CHAR_HEIGHT_GEOM)))
							{
								covered = true;
								break;
							}
						}
					}
					
					if (covered)
					{
						coveredGround.set((cx * GeoConfig.CELLS) + cy);
					}
					else
					{
						final int id = ((cx * GeoConfig.CELLS) + cy) * GeoConfig.MAX_LAYERS;
						if (!reachable.get(id))
						{
							reachable.set(id);
							queue.add(id);
							seedIds.add(id);
						}
					}
				}
			}
		}
		
		final int[][] dirs4 =
		{
			// @formatter:off
			{ 1, 0, 1 },
			{ -1, 0, 2 },
			{ 0, 1, 4 },
			{ 0, -1, 8 }
			// @formatter:on
		}; // E W S N (north = -Y)
		
		while (!queue.isEmpty())
		{
			checkCancelled(stopRequested);
			final int id = queue.poll();
			final int layer = id % GeoConfig.MAX_LAYERS;
			final int cell = id / GeoConfig.MAX_LAYERS;
			final int bx = cell / GeoConfig.CELLS;
			final int by = cell % GeoConfig.CELLS;
			final int nswe = grid.layerNswe[id] & 0xFF;
			final double z = grid.realZ(bx, by, layer);
			for (int[] d : dirs4)
			{
				if ((nswe & d[2]) == 0)
				{
					continue;
				}
				
				final int nx = bx + d[0];
				final int ny = by + d[1];
				if ((nx < 0) || (nx >= GeoConfig.CELLS) || (ny < 0) || (ny >= GeoConfig.CELLS))
				{
					continue;
				}
				
				final int nid = landingId(grid, nx, ny, z);
				if (nid < 0)
				{
					continue;
				}
				
				if (!reachable.get(nid))
				{
					reachable.set(nid);
					queue.add(nid);
				}
			}
		}
		
		// Sealed-interior rescue: an instance hall is walled geometry with no walkable way in from the surface (the Throne of Heroes boss room sits 8700 below terrain),
		// so the flood above never seeds it and the drop below erased its floors, leaving only the cavern shell 1156 overhead.
		// Line of sight then read that shell and blocked inside a room the grid still painted flat and open, while getNearestZ hid the hole by handing the player back their own Z.
		// The layers the flood missed are therefore re-flooded among themselves and a component survives when it covers more than SEALED_ROOM_CELLS cells: rooms span hundreds, the buried slivers this pass exists to drop span a handful.
		// Terrain-grade components stay dropped whatever their size - that is the buried terrain under town pavement, never a room.
		final BitSet componentVisited = new BitSet(GeoConfig.CELLS * GeoConfig.CELLS * GeoConfig.MAX_LAYERS);
		final BitSet componentCells = new BitSet(GeoConfig.CELLS * GeoConfig.CELLS);
		int[] componentIds = new int[1024];
		int keptRooms = 0;
		int keptLayers = 0;
		for (int startCell = 0; startCell < (GeoConfig.CELLS * GeoConfig.CELLS); startCell++)
		{
			checkCancelled(stopRequested);
			final int startCount = grid.layerCount[startCell];
			for (int startLayer = 0; startLayer < startCount; startLayer++)
			{
				final int startId = (startCell * GeoConfig.MAX_LAYERS) + startLayer;
				if (reachable.get(startId) || componentVisited.get(startId))
				{
					continue;
				}
				
				// Walk one connected component of unreached layers, over the same landings the flood above uses.
				int size = 0;
				int cellCount = 0;
				int terrainGrade = 0;
				componentVisited.set(startId);
				queue.add(startId);
				while (!queue.isEmpty())
				{
					checkCancelled(stopRequested);
					final int id = queue.poll();
					if (size == componentIds.length)
					{
						componentIds = Arrays.copyOf(componentIds, size * 2);
					}
					
					componentIds[size++] = id;
					final int layer = id % GeoConfig.MAX_LAYERS;
					final int cell = id / GeoConfig.MAX_LAYERS;
					final int bx = cell / GeoConfig.CELLS;
					final int by = cell % GeoConfig.CELLS;
					if (!componentCells.get(cell))
					{
						componentCells.set(cell);
						cellCount++;
					}
					
					final double z = grid.realZ(bx, by, layer);
					if (terrain != null)
					{
						final double px = tileOriginX + (bx * 16) + 8;
						final double py = tileOriginY + (by * 16) + 8;
						final int qx = (int) ((px - originX) / 128);
						final int qy = (int) ((py - originY) / 128);
						if ((qx >= 0) && (qx < terrain.uSize) && (qy >= 0) && (qy < terrain.vSize) && terrain.isQuadVisible(qx, qy) && (Math.abs(z - terrain.heightAt(px, py)) <= 32))
						{
							terrainGrade++;
						}
					}
					
					final int nswe = grid.layerNswe[id] & 0xFF;
					for (int[] d : dirs4)
					{
						if ((nswe & d[2]) == 0)
						{
							continue;
						}
						
						final int nx = bx + d[0];
						final int ny = by + d[1];
						if ((nx < 0) || (nx >= GeoConfig.CELLS) || (ny < 0) || (ny >= GeoConfig.CELLS))
						{
							continue;
						}
						
						final int nid = landingId(grid, nx, ny, z);
						if (nid < 0)
						{
							continue;
						}
						
						if (!reachable.get(nid) && !componentVisited.get(nid))
						{
							componentVisited.set(nid);
							queue.add(nid);
						}
					}
				}
				
				// Big enough to be a room, and not the buried terrain plane: keep every floor in it.
				if ((cellCount > GeoConfig.SEALED_ROOM_CELLS) && ((terrainGrade * 2) < size))
				{
					for (int i = 0; i < size; i++)
					{
						reachable.set(componentIds[i]);
					}
					
					keptRooms++;
					keptLayers += size;
				}
				
				// Clear only the cells this component touched: clearing the whole set per component would scan four million bits every time.
				for (int i = 0; i < size; i++)
				{
					componentCells.clear(componentIds[i] / GeoConfig.MAX_LAYERS);
				}
			}
		}
		
		System.out.println("Sealed interiors kept: " + keptRooms + " (" + keptLayers + " layers).");
		
		int dropped = 0;
		for (int cx = 0; cx < GeoConfig.CELLS; cx++)
		{
			checkCancelled(stopRequested);
			for (int cy = 0; cy < GeoConfig.CELLS; cy++)
			{
				if ((cy & 63) == 0)
				{
					checkCancelled(stopRequested);
				}
				final int cell = (cx * GeoConfig.CELLS) + cy;
				final int count = grid.layerCount[cell];
				if (count <= 1)
				{
					continue;
				}
				
				int w = 1; // Top layer always kept.
				for (int k = 1; k < count; k++)
				{
					if (reachable.get((cell * GeoConfig.MAX_LAYERS) + k))
					{
						grid.layerZ[(cell * GeoConfig.MAX_LAYERS) + w] = grid.layerZ[(cell * GeoConfig.MAX_LAYERS) + k];
						grid.layerZFrac[(cell * GeoConfig.MAX_LAYERS) + w] = grid.layerZFrac[(cell * GeoConfig.MAX_LAYERS) + k];
						grid.layerNswe[(cell * GeoConfig.MAX_LAYERS) + w] = grid.layerNswe[(cell * GeoConfig.MAX_LAYERS) + k];
						w++;
					}
					else
					{
						dropped++;
					}
				}
				
				grid.layerCount[cell] = (byte) w;
			}
		}
		
		System.out.println("Dropped " + dropped + " unreachable sub-layers.");
		
		// Reference semantics: only flood-VISITED cells exist in the reference data - everything unreached is solid.
		// Seal unreached SINGLE-layer cells (sealed tower interiors at terrain grade under their roof, isolated crags): multilayer cells keep their unreached roof tops, their walkable under-layers survived the drop above.
		int sealedTops = 0;
		for (int cell = 0; cell < (GeoConfig.CELLS * GeoConfig.CELLS); cell++)
		{
			checkCancelled(stopRequested);
			if ((grid.layerCount[cell] != 1) || grid.noFloor.get(cell) || reachable.get(cell * GeoConfig.MAX_LAYERS))
			{
				continue;
			}
			
			grid.noFloor.set(cell);
			sealedTops++;
			if ((GeoConfig.DEBUG_CELL != null) && (cell == ((GeoConfig.DEBUG_CELL[0] * GeoConfig.CELLS) + GeoConfig.DEBUG_CELL[1])))
			{
				System.out.println("DEBUG noFloor(" + GeoConfig.DEBUG_CELL[0] + "," + GeoConfig.DEBUG_CELL[1] + ") <- UNREACHED-GROUND-SEAL");
			}
		}
		
		System.out.println("Sealed " + sealedTops + " unreached ground cells.");
		
		// Covered ground that can only be entered by FALLING in (wall-walkway drops into sealed tower interiors,
		// 287 units at the Gludio gate) is dead space in the reference: rerun the flood with falls capped at hop depth and seal covered single-layer cells the capped flood never visits.
		final BitSet reachClimb = new BitSet(GeoConfig.CELLS * GeoConfig.CELLS * GeoConfig.MAX_LAYERS);
		for (int s = 0; s < seedIds.size(); s++)
		{
			final int id = seedIds.get(s);
			reachClimb.set(id);
			queue.add(id);
		}
		
		while (!queue.isEmpty())
		{
			checkCancelled(stopRequested);
			final int id = queue.poll();
			final int layer = id % GeoConfig.MAX_LAYERS;
			final int cell = id / GeoConfig.MAX_LAYERS;
			final int bx = cell / GeoConfig.CELLS;
			final int by = cell % GeoConfig.CELLS;
			final int nswe = grid.layerNswe[id] & 0xFF;
			final double z = grid.realZ(bx, by, layer);
			for (int[] d : dirs4)
			{
				if ((nswe & d[2]) == 0)
				{
					continue;
				}
				
				final int nx = bx + d[0];
				final int ny = by + d[1];
				if ((nx < 0) || (nx >= GeoConfig.CELLS) || (ny < 0) || (ny >= GeoConfig.CELLS))
				{
					continue;
				}
				
				final int ncount = grid.layerCount[(nx * GeoConfig.CELLS) + ny];
				int bestK = -1;
				double best = Double.NEGATIVE_INFINITY;
				for (int k = 0; k < ncount; k++)
				{
					final double nz2 = grid.realZ(nx, ny, k);
					if ((nz2 <= (z + GeoConfig.MAX_CLIMB)) && (nz2 > best))
					{
						best = nz2;
						bestK = k;
					}
				}
				
				if ((bestK < 0) || ((z - best) > 240))
				{
					continue; // falls deeper than a terrace hop do not open covered interiors
				}
				
				final int nid = (((nx * GeoConfig.CELLS) + ny) * GeoConfig.MAX_LAYERS) + bestK;
				if (!reachClimb.get(nid))
				{
					reachClimb.set(nid);
					queue.add(nid);
				}
			}
		}
		
		int sealedCovered = 0;
		for (int cell = coveredGround.nextSetBit(0); cell >= 0; cell = coveredGround.nextSetBit(cell + 1))
		{
			checkCancelled(stopRequested);
			if ((grid.layerCount[cell] != 1) || grid.noFloor.get(cell) || reachClimb.get(cell * GeoConfig.MAX_LAYERS))
			{
				continue;
			}
			
			grid.noFloor.set(cell);
			sealedCovered++;
			if ((GeoConfig.DEBUG_CELL != null) && (cell == ((GeoConfig.DEBUG_CELL[0] * GeoConfig.CELLS) + GeoConfig.DEBUG_CELL[1])))
			{
				System.out.println("DEBUG noFloor(" + GeoConfig.DEBUG_CELL[0] + "," + GeoConfig.DEBUG_CELL[1] + ") <- FALL-ONLY-COVERED-SEAL");
			}
		}
		
		System.out.println("Sealed " + sealedCovered + " fall-only covered ground cells.");
		
		// Bridge-footprint fence, the walk-on-water fix: a LAND flood (same seeds and rules, but never ENTERING bridge-class footprints) marks everything walkable without stepping on a bridge, pier, dock or quay.
		// Cells no land path reaches, that are not bridge-class themselves and have NO terrain backing, are phantom continuations - the invisible plane spanning the Giran bay at plaza height is reachable ONLY across the quay footprint and its terrain quads are holes.
		// They keep just their BOTTOM layer (the basin floor), and the plunge/climb rules then close the true deck edges naturally.
		// Real islands connected only by a bridge keep everything: their ground is terrain-backed.
		final BitSet landReach = new BitSet(GeoConfig.CELLS * GeoConfig.CELLS * GeoConfig.MAX_LAYERS);
		for (int s = 0; s < seedIds.size(); s++)
		{
			final int id = seedIds.get(s);
			if (!bridgeCells.get(id / GeoConfig.MAX_LAYERS))
			{
				landReach.set(id);
				queue.add(id);
			}
		}
		
		while (!queue.isEmpty())
		{
			checkCancelled(stopRequested);
			final int id = queue.poll();
			final int layer = id % GeoConfig.MAX_LAYERS;
			final int cell = id / GeoConfig.MAX_LAYERS;
			final int bx = cell / GeoConfig.CELLS;
			final int by = cell % GeoConfig.CELLS;
			final int nswe = grid.layerNswe[id] & 0xFF;
			final double z = grid.realZ(bx, by, layer);
			for (int[] d : dirs4)
			{
				if ((nswe & d[2]) == 0)
				{
					continue;
				}
				
				final int nx = bx + d[0];
				final int ny = by + d[1];
				if ((nx < 0) || (nx >= GeoConfig.CELLS) || (ny < 0) || (ny >= GeoConfig.CELLS) || bridgeCells.get((nx * GeoConfig.CELLS) + ny))
				{
					continue;
				}
				
				final int ncount = grid.layerCount[(nx * GeoConfig.CELLS) + ny];
				int bestK = -1;
				double best = Double.NEGATIVE_INFINITY;
				for (int k = 0; k < ncount; k++)
				{
					final double nz2 = grid.realZ(nx, ny, k);
					if ((nz2 <= (z + GeoConfig.MAX_CLIMB)) && (nz2 > best))
					{
						best = nz2;
						bestK = k;
					}
				}
				
				if (bestK < 0)
				{
					continue;
				}
				
				final int nid = (((nx * GeoConfig.CELLS) + ny) * GeoConfig.MAX_LAYERS) + bestK;
				if (!landReach.get(nid))
				{
					landReach.set(nid);
					queue.add(nid);
				}
			}
		}
		
		int fenced = 0;
		for (int cx = 0; cx < GeoConfig.CELLS; cx++)
		{
			checkCancelled(stopRequested);
			for (int cy = 0; cy < GeoConfig.CELLS; cy++)
			{
				if ((cy & 63) == 0)
				{
					checkCancelled(stopRequested);
				}
				final int cell = (cx * GeoConfig.CELLS) + cy;
				if (grid.noFloor.get(cell) || (grid.layerCount[cell] <= 1) || bridgeCells.get(cell))
				{
					continue;
				}
				
				boolean anyLand = false;
				for (int k = 0; (k < grid.layerCount[cell]) && !anyLand; k++)
				{
					anyLand = landReach.get((cell * GeoConfig.MAX_LAYERS) + k);
				}
				
				if (anyLand)
				{
					continue;
				}
				
				boolean backed = false;
				if (terrain != null)
				{
					final double px = tileOriginX + (cx * 16) + 8;
					final double py = tileOriginY + (cy * 16) + 8;
					final int qx = (int) ((px - originX) / 128);
					final int qy = (int) ((py - originY) / 128);
					if ((qx >= 0) && (qx < terrain.uSize) && (qy >= 0) && (qy < terrain.vSize) && terrain.isQuadVisible(qx, qy))
					{
						final double tz2 = terrain.heightAt(px, py);
						for (int k = 0; (k < grid.layerCount[cell]) && !backed; k++)
						{
							backed = Math.abs(grid.realZ(cx, cy, k) - tz2) <= 32;
						}
					}
				}
				
				if (backed)
				{
					continue;
				}
				
				// Keep only the bottom layer (layers are stored top-first).
				fenced += grid.layerCount[cell] - 1;
				grid.layerZ[cell * GeoConfig.MAX_LAYERS] = grid.layerZ[((cell * GeoConfig.MAX_LAYERS) + grid.layerCount[cell]) - 1];
				grid.layerZFrac[cell * GeoConfig.MAX_LAYERS] = grid.layerZFrac[((cell * GeoConfig.MAX_LAYERS) + grid.layerCount[cell]) - 1];
				grid.layerNswe[cell * GeoConfig.MAX_LAYERS] = grid.layerNswe[((cell * GeoConfig.MAX_LAYERS) + grid.layerCount[cell]) - 1];
				grid.layerCount[cell] = 1;
			}
		}
		
		System.out.println("Fenced " + fenced + " bridge-only phantom layers.");
		
		// Bridge doorway thresholds now that dead layers are gone (slots free, blockers real).
		bridgePass.run();
		
		// Apply the recorded wall columns now that thresholds are bridged: the cell becomes a solid wall-top column (line-of-sight blocks by height), doorway lanes were exempted via door boxes at sampling time.
		// A verdict anchored on a floor the reachability pass DROPPED (pit bottoms under pavement cracks, basement slivers) is a pit side, not a wall -
		// its top IS the walking surface (the Gludio guild staircase base), so the column only applies while a surviving floor still anchors its chain.
		int columns = 0;
		int expired = 0;
		for (int cell = wallCol.nextSetBit(0); cell >= 0; cell = wallCol.nextSetBit(cell + 1))
		{
			checkCancelled(stopRequested);
			if (!grid.noFloor.get(cell))
			{
				boolean anchored = false;
				boolean topIsFloor = false;
				for (int k = 0; k < grid.layerCount[cell]; k++)
				{
					final double f = grid.layerZ[(cell * GeoConfig.MAX_LAYERS) + k] + grid.layerZFrac[(cell * GeoConfig.MAX_LAYERS) + k];
					anchored |= (wallColLo[cell] <= (f + 24)) && (wallColZ[cell] > (f + GeoConfig.WALL_COLUMN));
					topIsFloor |= Math.abs(wallColZ[cell] - f) <= 16;
				}
				
				// A floor AT the chain top is only a walkable terrace/plaza edge (the Gludio guild staircase plinth rises 128 from the street to the plaza floor) when the top level CONTINUES onto adjacent single-layer GROUND - the plaza itself, nothing underneath.
				// A shed's eave/roof also matches its wall-chain top, but every neighbor still carries the street BELOW the roof plane (two layers) - those columns must stand, or the shed walls open to walking and ground-level sight.
				// (Neither flood reachability nor terrain backing can arbitrate: buildings AND raised plazas both sit on terrain holes, and hole cells seed their TOP layers.
				// Building interiors whose dropped-street cells go single-layer expire their wall columns through this test too - harmless: a lone high layer blocks sight by HEIGHT exactly like a column, the reference stores walls the same way.)
				if (topIsFloor)
				{
					topIsFloor = false;
					final int ccx = cell / GeoConfig.CELLS;
					final int ccy = cell % GeoConfig.CELLS;
					for (int d = 0; (d < 4) && !topIsFloor; d++)
					{
						final int nx = ccx + (d == 0 ? 1 : d == 1 ? -1 : 0);
						final int ny = ccy + (d == 2 ? 1 : d == 3 ? -1 : 0);
						if ((nx < 0) || (nx >= GeoConfig.CELLS) || (ny < 0) || (ny >= GeoConfig.CELLS))
						{
							continue;
						}
						
						final int ncell = (nx * GeoConfig.CELLS) + ny;
						topIsFloor = !grid.noFloor.get(ncell) && (grid.layerCount[ncell] == 1) && (Math.abs(grid.realZ(nx, ny, 0) - wallColZ[cell]) <= GeoConfig.MAX_CLIMB);
					}
				}
				
				if (!anchored || topIsFloor)
				{
					expired++;
					continue;
				}
			}
			
			grid.layerZ[cell * GeoConfig.MAX_LAYERS] = wallColZ[cell];
			grid.layerZFrac[cell * GeoConfig.MAX_LAYERS] = 0;
			grid.layerNswe[cell * GeoConfig.MAX_LAYERS] = 0;
			grid.layerCount[cell] = 1;
			grid.noFloor.set(cell);
			columns++;
			if ((GeoConfig.DEBUG_CELL != null) && (cell == ((GeoConfig.DEBUG_CELL[0] * GeoConfig.CELLS) + GeoConfig.DEBUG_CELL[1])))
			{
				System.out.println("DEBUG noFloor(" + GeoConfig.DEBUG_CELL[0] + "," + GeoConfig.DEBUG_CELL[1] + ") <- WALL-COLUMN: z=" + wallColZ[cell]);
			}
		}
		
		System.out.println("Wall columns: " + columns + " (" + expired + " skipped, anchor floor dropped)");
		
		// Bridge parapet line-join: balustrades collide at their POSTS only, so the side line comes out dashed - in any direction,
		// the quay lines run diagonally - and characters slip between the columns onto the water plane.
		// Every PAIR of bridge-class columns within 4 cells and 32 height of each other joins into a continuous line: the straight run between them converts,
		// but only through deck-level bridge-class cells whose own walk layer matches the column tops - raised parapets sit 40+ over real footbridge decks, so decks between rails never fill.
		final List<int[]> railCols = new ArrayList<>();
		for (int cx = 0; cx < GeoConfig.CELLS; cx++)
		{
			checkCancelled(stopRequested);
			for (int cy = 0; cy < GeoConfig.CELLS; cy++)
			{
				if ((cy & 63) == 0)
				{
					checkCancelled(stopRequested);
				}
				final int cell = (cx * GeoConfig.CELLS) + cy;
				if (grid.noFloor.get(cell) && wallCol.get(cell) && bridgeCells.get(cell))
				{
					railCols.add(new int[]
					{
						cx,
						cy,
						grid.layerZ[cell * GeoConfig.MAX_LAYERS]
					});
				}
			}
		}
		
		int railFilled = 0;
		for (int i = 0; i < railCols.size(); i++)
		{
			final int[] a = railCols.get(i);
			for (int j = i + 1; j < railCols.size(); j++)
			{
				final int[] b = railCols.get(j);
				final int dx = b[0] - a[0];
				final int dy = b[1] - a[1];
				if ((Math.abs(dx) > 8) || (Math.abs(dy) > 8) || (Math.abs(a[2] - b[2]) > 32) || ((Math.abs(dx) <= 1) && (Math.abs(dy) <= 1)))
				{
					continue;
				}
				
				final int steps = Math.max(Math.abs(dx), Math.abs(dy));
				for (int s = 1; s < steps; s++)
				{
					final int fx = a[0] + Math.round((float) (dx * s) / steps);
					final int fy = a[1] + Math.round((float) (dy * s) / steps);
					final int fcell = (fx * GeoConfig.CELLS) + fy;
					if (grid.noFloor.get(fcell) || !bridgeCells.get(fcell) || (grid.layerCount[fcell] == 0))
					{
						continue;
					}
					
					final double top = grid.realZ(fx, fy, 0);
					if ((Math.abs(top - a[2]) > 32) || (Math.abs(top - b[2]) > 32))
					{
						continue;
					}
					
					grid.layerZ[fcell * GeoConfig.MAX_LAYERS] = (short) Math.max(a[2], b[2]);
					grid.layerZFrac[fcell * GeoConfig.MAX_LAYERS] = 0;
					grid.layerNswe[fcell * GeoConfig.MAX_LAYERS] = 0;
					grid.layerCount[fcell] = 1;
					grid.noFloor.set(fcell);
					wallCol.set(fcell);
					railFilled++;
					if ((GeoConfig.DEBUG_CELL != null) && (fcell == ((GeoConfig.DEBUG_CELL[0] * GeoConfig.CELLS) + GeoConfig.DEBUG_CELL[1])))
					{
						System.out.println("DEBUG noFloor(" + GeoConfig.DEBUG_CELL[0] + "," + GeoConfig.DEBUG_CELL[1] + ") <- PARAPET-JOIN");
					}
				}
			}
		}
		
		System.out.println("Bridge parapet gaps filled: " + railFilled);
		
		// Crawl-layer purge with EROSION, the line-of-sight rule: canSeeTarget blocks by height, so ground layers surviving under wall/roof bands let sight and spells pass under walls.
		// A pure ceiling-gap threshold fails (the reference keeps a 120 gap under a shop eave but purges the 128 wall band) - the real distinction is DEPTH: legit covered walkways (eave rims, doorways, porches) are 1-2 cells deep with open ground beside them,
		// wall-band interiors are wide. Purge crawl layers only where ALL four orthogonal neighbors are also crawl-covered: rims and passages survive, band interiors go roof-only, and any sight line crossing a band still passes through roof-height cells.
		final BitSet crawlCovered = new BitSet(GeoConfig.CELLS * GeoConfig.CELLS);
		final BitSet wallShadow = new BitSet(GeoConfig.CELLS * GeoConfig.CELLS); // covered up to WALL_SHADOW: purged only when hugging a tall wall face
		for (int cell = 0; cell < (GeoConfig.CELLS * GeoConfig.CELLS); cell++)
		{
			checkCancelled(stopRequested);
			final int count = grid.layerCount[cell];
			for (int k = 1; k < count; k++)
			{
				final int gap = grid.layerZ[((cell * GeoConfig.MAX_LAYERS) + k) - 1] - grid.layerZ[(cell * GeoConfig.MAX_LAYERS) + k];
				if (gap < GeoConfig.CRAWL_GAP)
				{
					crawlCovered.set(cell);
				}
				
				if (gap < GeoConfig.WALL_SHADOW)
				{
					wallShadow.set(cell);
				}
			}
		}
		
		int purged = 0;
		final long[] cellEdges = new long[4]; // refilled per cell: the four boundary keys, hoisted because this pass walks every cell in the region
		for (int cx = 1; cx < (GeoConfig.CELLS - 1); cx++)
		{
			checkCancelled(stopRequested);
			for (int cy = 1; cy < (GeoConfig.CELLS - 1); cy++)
			{
				if ((cy & 63) == 0)
				{
					checkCancelled(stopRequested);
				}
				final int cell = (cx * GeoConfig.CELLS) + cy;
				if (!crawlCovered.get(cell) && !wallShadow.get(cell))
				{
					continue;
				}
				
				// Navmesh-style agent-radius erosion (the UE navmesh gap around obstacles): a covered cell whose boundary carries a TALL wall face is inside the character radius of that wall - purge its crawl layers like the reference's roof-only rows at wall faces.
				// Door passages survive: their center lane has jambs one cell away, only the flank cells touch wall intervals.
				boolean wallAdjacent = false;
				final double walkZ = grid.layerCount[cell] > 1 ? grid.layerZ[(cell * GeoConfig.MAX_LAYERS) + 1] : grid.layerZ[cell * GeoConfig.MAX_LAYERS];
				
				// NOTE: extending adjacency to recorded wall-column chains (to seal mid-cell wall faces) was tried and REVERTED: balcony support posts register as "the opposite wall" so under-overhang bands read as passages,
				// while genuine door lanes (dark elf guild) lost their street to jamb chains - door boxes are pivot-sized and do not cover lanes.
				// Sight into buildings is already blocked by the single wall-top layers on the wall line itself.
				cellEdges[0] = GeoMath.edgeKey(cx, cy, 0);
				cellEdges[1] = GeoMath.edgeKey(cx - 1, cy, 0);
				cellEdges[2] = GeoMath.edgeKey(cx, cy, 1);
				cellEdges[3] = GeoMath.edgeKey(cx, cy - 1, 1);
				for (long ek : cellEdges)
				{
					final List<double[]> wl = wallEdges.get(ek);
					if (wl == null)
					{
						continue;
					}
					
					for (double[] we : wl)
					{
						if ((we[0] <= (walkZ + 24)) && (we[1] >= (walkZ + 120)))
						{
							wallAdjacent = true;
							break;
						}
					}
					
					if (wallAdjacent)
					{
						break;
					}
				}
				
				// Erosion applies to CRAWL cells only (interior of covered regions); wall-adjacent purging additionally takes tall covered strips up to WALL_SHADOW (building overhang bands) so sight lines die AT the wall base like the reference.
				final boolean erosion = crawlCovered.get(cell) //
					&& crawlCovered.get(((cx - 1) * GeoConfig.CELLS) + cy) && crawlCovered.get(((cx + 1) * GeoConfig.CELLS) + cy) //
					&& crawlCovered.get((cx * GeoConfig.CELLS) + (cy - 1)) && crawlCovered.get((cx * GeoConfig.CELLS) + (cy + 1));
				if (!wallAdjacent && !erosion)
				{
					continue;
				}
				
				final double purgeGap = wallAdjacent ? GeoConfig.WALL_SHADOW : GeoConfig.CRAWL_GAP;
				final int count = grid.layerCount[cell];
				int w = 1;
				for (int k = 1; k < count; k++)
				{
					if ((grid.layerZ[((cell * GeoConfig.MAX_LAYERS) + w) - 1] - grid.layerZ[(cell * GeoConfig.MAX_LAYERS) + k]) < purgeGap)
					{
						purged++;
						continue;
					}
					
					grid.layerZ[(cell * GeoConfig.MAX_LAYERS) + w] = grid.layerZ[(cell * GeoConfig.MAX_LAYERS) + k];
					grid.layerZFrac[(cell * GeoConfig.MAX_LAYERS) + w] = grid.layerZFrac[(cell * GeoConfig.MAX_LAYERS) + k];
					grid.layerNswe[(cell * GeoConfig.MAX_LAYERS) + w] = grid.layerNswe[(cell * GeoConfig.MAX_LAYERS) + k];
					w++;
				}
				
				grid.layerCount[cell] = (byte) w;
			}
		}
		
		System.out.println("Purged " + purged + " interior crawl layers (line-of-sight rule).");
		
		// Final NSWE after bridging and purge: bridged floors get their movement bits, purged-band edges recompute.
		nswePass.run();
		
		// Seam-line propagation: a lone-step closure extends along its boundary while the step persists.
		// Butt-joint seams wander sub-cell, and max-sampling hides the step wherever both cells catch the UPPER sheet (dz collapses to 0-8),
		// so the lone-step rule only fires where the step crosses cell centers - neighbors along the line with any remaining rise (> 4 real) continue the stopper line one-way uphill,
		// producing the reference's unbroken e-columns instead of a dashed line characters slip around.
		final IntQueue seamQueue = new IntQueue();
		for (int e = loneStepEdge.nextSetBit(0); e >= 0; e = loneStepEdge.nextSetBit(e + 1))
		{
			seamQueue.add(e);
		}
		
		final int[][] dirMeta =
		{
			// {dx, dy, bit} indexed by Integer.numberOfTrailingZeros(bit): E, W, S, N.
			// @formatter:off
			{ 1, 0, 1 },
			{ -1, 0, 2 },
			{ 0, 1, 4 },
			{ 0, -1, 8 },
			// @formatter:on
		};
		int seamExtended = 0;
		while (!seamQueue.isEmpty())
		{
			checkCancelled(stopRequested);
			final int e = seamQueue.poll();
			final int dirIdx = e & 3;
			final int cell = e >> 2;
			final int cx = cell / GeoConfig.CELLS;
			final int cy = cell % GeoConfig.CELLS;
			final int dx = dirMeta[dirIdx][0];
			final int dy = dirMeta[dirIdx][1];
			final int bit = dirMeta[dirIdx][2];
			
			// The line runs perpendicular to the crossing direction.
			final int lx = dx == 0 ? 1 : 0;
			final int ly = dx == 0 ? 0 : 1;
			for (int s = -1; s <= 1; s += 2)
			{
				final int px = cx + (lx * s);
				final int py = cy + (ly * s);
				final int tx = px + dx;
				final int ty = py + dy;
				if ((px < 0) || (px >= GeoConfig.CELLS) || (py < 0) || (py >= GeoConfig.CELLS) || (tx < 0) || (tx >= GeoConfig.CELLS) || (ty < 0) || (ty >= GeoConfig.CELLS))
				{
					continue;
				}
				
				final int pEdge = ((((px * GeoConfig.CELLS) + py) * 4) + dirIdx);
				if (loneStepEdge.get(pEdge) || grid.noFloor.get((px * GeoConfig.CELLS) + py) || grid.noFloor.get((tx * GeoConfig.CELLS) + ty))
				{
					continue;
				}
				
				boolean closedAny = false;
				final int cellCount = grid.count(px, py);
				for (int layer = 0; layer < cellCount; layer++)
				{
					if ((grid.layerNswe[(((px * GeoConfig.CELLS) + py) * GeoConfig.MAX_LAYERS) + layer] & bit) == 0)
					{
						continue;
					}
					
					final double zReal = grid.realZ(px, py, layer);
					double best = Double.NaN;
					double bestDiff = Double.MAX_VALUE;
					final int targetCount = grid.count(tx, ty);
					for (int k = 0; k < targetCount; k++)
					{
						final double candidateZ = grid.realZ(tx, ty, k);
						final double diff = Math.abs(candidateZ - zReal);
						if (diff < bestDiff)
						{
							bestDiff = diff;
							best = candidateZ;
						}
					}
					
					if (Double.isNaN(best) || ((best - zReal) <= 4) || ((best - zReal) > GeoConfig.MAX_CLIMB))
					{
						continue;
					}
					
					// Joined sheets never extend the line: an upper level reaching exactly onto the boundary is an architectural step (see the lone-step rule).
					final List<double[]> cnList = connectEdges.get(bit == 1 ? GeoMath.edgeKey(px, py, 0) : bit == 2 ? GeoMath.edgeKey(px - 1, py, 0) : bit == 4 ? GeoMath.edgeKey(px, py, 1) : GeoMath.edgeKey(px, py - 1, 1));
					boolean joined = false;
					if (cnList != null)
					{
						for (double[] cn : cnList)
						{
							if ((cn[0] <= (best + 8)) && (cn[1] >= (best - 8)))
							{
								joined = true;
								break;
							}
						}
					}
					
					if (joined)
					{
						continue;
					}
					
					// The extension must itself be a LONE step: a crossing whose ascent continues ahead (or arrived climbing) is a slope or staircase,
					// not the wandering seam - without this test the line chains along every natural hillside contour.
					final int ax = tx + dx;
					final int ay = ty + dy;
					boolean context = false;
					if ((ax >= 0) && (ax < GeoConfig.CELLS) && (ay >= 0) && (ay < GeoConfig.CELLS) && !grid.noFloor.get((ax * GeoConfig.CELLS) + ay))
					{
						double aBest = Double.NaN;
						double aDiff = Double.MAX_VALUE;
						final int aheadCount = grid.count(ax, ay);
						for (int k = 0; k < aheadCount; k++)
						{
							final double candidateZ = grid.realZ(ax, ay, k);
							final double diff = Math.abs(candidateZ - best);
							if (diff < aDiff)
							{
								aDiff = diff;
								aBest = candidateZ;
							}
						}
						
						context = !Double.isNaN(aBest) && ((aBest - best) > 4) && ((aBest - best) <= GeoConfig.MAX_CLIMB);
					}
					
					final int bx = px - dx;
					final int by = py - dy;
					if (!context && (bx >= 0) && (bx < GeoConfig.CELLS) && (by >= 0) && (by < GeoConfig.CELLS) && !grid.noFloor.get((bx * GeoConfig.CELLS) + by))
					{
						double bBest = Double.NaN;
						double bDiff = Double.MAX_VALUE;
						final int behindCount = grid.count(bx, by);
						for (int k = 0; k < behindCount; k++)
						{
							final double candidateZ = grid.realZ(bx, by, k);
							final double diff = Math.abs(candidateZ - zReal);
							if (diff < bDiff)
							{
								bDiff = diff;
								bBest = candidateZ;
							}
						}
						
						context = !Double.isNaN(bBest) && ((zReal - bBest) > 4) && ((zReal - bBest) <= GeoConfig.MAX_CLIMB);
					}
					
					if (!context)
					{
						grid.layerNswe[(((px * GeoConfig.CELLS) + py) * GeoConfig.MAX_LAYERS) + layer] &= (byte) ~bit;
						closedAny = true;
					}
				}
				
				if (closedAny)
				{
					loneStepEdge.set(pEdge);
					seamQueue.add(pEdge);
					seamExtended++;
				}
			}
		}
		
		System.out.println("Seam lines extended over " + seamExtended + " crossings.");
		
		// Bridge fall-guard (rule: block bridge edges so players cannot walk off to their deaths).
		// Normal ground lets you step/fall off any ledge shorter than ABYSS_FALL; bridges tighten that to FALL_STOP.
		// For each bridge-class deck cell, clear the OUTWARD movement bit in any direction where the step DOWN exceeds FALL_STOP (the neighbor's nearest walkable surface is more than FALL_STOP below the deck, or the neighbor is a wall body / off the map).
		// FALL_STOP=16 matches the deck's own internal slope (<=16/cell, so the deck stays fully walkable and the road stays open) and sits below the quay's ~48-unit tier steps, so the WHOLE perimeter closes continuously, no dashing.
		// Per-cell (no run gate), one-directional (walking ONTO the bridge stays legal), NSWE-only (never a layer, so the deck is never sealed - only the step OFF the edge dies).
		final double FALL_STOP = 16;
		final int[][] guardDirs =
		{
			// @formatter:off
			{ 1, 0, 1 }, // east
			{ -1, 0, 2 }, // west
			{ 0, 1, 4 }, // south
			{ 0, -1, 8 }, // north
			// @formatter:on
		};
		int fallGuarded = 0;
		for (int cx = 0; cx < GeoConfig.CELLS; cx++)
		{
			checkCancelled(stopRequested);
			for (int cy = 0; cy < GeoConfig.CELLS; cy++)
			{
				if ((cy & 63) == 0)
				{
					checkCancelled(stopRequested);
				}
				final int cell = (cx * GeoConfig.CELLS) + cy;
				if (!bridgeCells.get(cell) || grid.noFloor.get(cell) || (grid.layerCount[cell] == 0))
				{
					continue;
				}
				
				final double z = grid.realZ(cx, cy, 0);
				for (int[] d : guardDirs)
				{
					if ((grid.layerNswe[cell * GeoConfig.MAX_LAYERS] & d[2]) == 0)
					{
						continue;
					}
					
					final int nx = cx + d[0];
					final int ny = cy + d[1];
					if ((nx < 0) || (nx >= GeoConfig.CELLS) || (ny < 0) || (ny >= GeoConfig.CELLS))
					{
						continue; // Region border: the next region continues here - leave it OPEN, never wall the map seam.
					}
					
					final int nc = (nx * GeoConfig.CELLS) + ny;
					boolean fall = grid.noFloor.get(nc) || (grid.layerCount[nc] == 0);
					if (!fall)
					{
						double best = Double.NEGATIVE_INFINITY;
						for (int k = 0; k < grid.layerCount[nc]; k++)
						{
							final double f = grid.realZ(nx, ny, k);
							if ((f <= (z + GeoConfig.MAX_CLIMB)) && (f > best))
							{
								best = f;
							}
						}
						
						fall = (best == Double.NEGATIVE_INFINITY) || ((z - best) > FALL_STOP);
					}
					
					if (fall)
					{
						// NOTE: do NOT exempt drops that land in water.
						// The Giran TOWN BRIDGE spans a river - its deck edge drops to riverbed -3872 under the surface - so a "dock-over-water is fall-able" exemption disabled the guard on the bridge itself and opened its edges (deck cell cx1276 went e -> fully open, walk straight off).
						// Water does not separate docks from bridges; both sit over it. Bridge-class scoping is what already separates them, so every bridge fall edge closes here.
						grid.layerNswe[cell * GeoConfig.MAX_LAYERS] &= (byte) ~d[2];
						fallGuarded++;
					}
				}
			}
		}
		
		System.out.println("Bridge fall-guard edges closed: " + fallGuarded);
		
		// Flat-neighborhood walkability (rule): if EVERY cell in the neighborhood sits within 8 units of this one, the ground here is genuinely flat - nothing rises or drops for anything to legitimately block against, so whatever closed it was an artifact of the quay/bridge machinery. Force it open.
		// Height is the discriminator that survives: a sealed plaza cell keeps its PLAZA height and matches its neighbors, while a real wall column stores its WALL-TOP height (magic shop -2920 over a -3120 street) and never looks flat - so line-of-sight walls are untouched.
		int flatOpened = 0;
		for (int cx = 0; cx < GeoConfig.CELLS; cx++)
		{
			checkCancelled(stopRequested);
			for (int cy = 0; cy < GeoConfig.CELLS; cy++)
			{
				if ((cy & 63) == 0)
				{
					checkCancelled(stopRequested);
				}
				final int cell = (cx * GeoConfig.CELLS) + cy;
				if (grid.layerCount[cell] == 0)
				{
					continue;
				}
				
				final double z = grid.realZ(cx, cy, 0);
				boolean flat = true;
				for (int dx = -GeoConfig.FLAT_RADIUS; (dx <= GeoConfig.FLAT_RADIUS) && flat; dx++)
				{
					for (int dy = -GeoConfig.FLAT_RADIUS; (dy <= GeoConfig.FLAT_RADIUS) && flat; dy++)
					{
						if ((dx == 0) && (dy == 0))
						{
							continue;
						}
						
						final int nx = cx + dx;
						final int ny = cy + dy;
						if ((nx < 0) || (nx >= GeoConfig.CELLS) || (ny < 0) || (ny >= GeoConfig.CELLS) || (grid.layerCount[(nx * GeoConfig.CELLS) + ny] == 0))
						{
							flat = false;
							break;
						}
						
						flat = Math.abs(grid.realZ(nx, ny, 0) - z) <= 8;
					}
				}
				
				if (flat && !wallSealed.get(cell) && (grid.noFloor.get(cell) || (grid.layerNswe[cell * GeoConfig.MAX_LAYERS] != 15)))
				{
					grid.noFloor.clear(cell);
					grid.layerNswe[cell * GeoConfig.MAX_LAYERS] = 15;
					flatOpened++;
				}
			}
		}
		
		System.out.println("Flat-neighborhood cells opened: " + flatOpened);
		
		if (GeoConfig.DEBUG_CELL != null)
		{
			System.out.print("DEBUG floors post-BFS:");
			final int cell = (GeoConfig.DEBUG_CELL[0] * GeoConfig.CELLS) + GeoConfig.DEBUG_CELL[1];
			for (int k = 0; k < grid.layerCount[cell]; k++)
			{
				System.out.print(" " + grid.layerZ[(cell * GeoConfig.MAX_LAYERS) + k] + "/nswe" + grid.layerNswe[(cell * GeoConfig.MAX_LAYERS) + k]);
			}
			
			System.out.println();
		}
		
		return grid;
	}
	
	private static void checkCancelled(BooleanSupplier stopRequested)
	{
		if ((stopRequested != null) && stopRequested.getAsBoolean())
		{
			throw new CancellationException("Map extraction stopped.");
		}
	}
	
	/**
	 * Checks whether the neighbor cell has a floor within 24 units of the given height.
	 * @param grid the region grid
	 * @param cx the neighbor cell X index
	 * @param cy the neighbor cell Y index
	 * @param z the reference height
	 * @return true when a same-height floor exists
	 */
	private static boolean hasFloorNear(GeoGrid grid, int cx, int cy, double z)
	{
		final int cell = (cx * GeoConfig.CELLS) + cy;
		if (grid.noFloor.get(cell))
		{
			return false;
		}
		
		for (int k = 0; k < grid.layerCount[cell]; k++)
		{
			if (Math.abs(grid.z(cx, cy, k) - z) <= 8)
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks one 4-sample line for a missed run of two or more samples bounded by coverage on both ends.
	 * @param a the first sample
	 * @param b the second sample
	 * @param c the third sample
	 * @param d the fourth sample
	 * @param lowBound coverage beyond the line start (same-height neighbor floor)
	 * @param highBound coverage beyond the line end (same-height neighbor floor)
	 * @return true when a bounded void run exists
	 */
	private static boolean boundedMissRun(boolean a, boolean b, boolean c, boolean d, boolean lowBound, boolean highBound)
	{
		// Samples 0..5 are lowBound, a, b, c, d, highBound, one bit each.
		final int line = (lowBound ? 1 : 0) | (a ? 2 : 0) | (b ? 4 : 0) | (c ? 8 : 0) | (d ? 16 : 0) | (highBound ? 32 : 0);
		
		int start = -1;
		for (int i = 1; i <= 4; i++)
		{
			if (((line >> i) & 1) == 0)
			{
				if (start < 0)
				{
					start = i;
				}
			}
			else
			{
				// Bounded run: at least one bound must be an IN-CELL at-floor sample, so a groove is anchored to this cell's own sheet and gentle cross-cell slopes cannot qualify.
				if ((start >= 0) && ((i - start) >= 2) && (((line >> (start - 1)) & 1) != 0) && ((start > 1) || (i <= 4)))
				{
					return true;
				}
				
				start = -1;
			}
		}
		
		return (start >= 0) && ((5 - start) >= 2) && (((line >> (start - 1)) & 1) != 0) && ((line & 32) != 0) && (start > 1);
	}
	
	/**
	 * Finds the layer a walker lands on when crossing into a neighbour cell: the highest layer within climb range, measured on REAL heights (falls of any depth allowed).
	 * @param grid the grid being built
	 * @param nx the neighbour cell X
	 * @param ny the neighbour cell Y
	 * @param z the REAL height the walker leaves from
	 * @return the layer id landed on, or -1 when nothing lies within climb range
	 */
	private static int landingId(GeoGrid grid, int nx, int ny, double z)
	{
		final int ncount = grid.layerCount[(nx * GeoConfig.CELLS) + ny];
		int bestK = -1;
		double best = Double.NEGATIVE_INFINITY;
		for (int k = 0; k < ncount; k++)
		{
			final double nz2 = grid.realZ(nx, ny, k);
			if ((nz2 <= (z + GeoConfig.MAX_CLIMB)) && (nz2 > best))
			{
				best = nz2;
				bestK = k;
			}
		}
		
		return bestK < 0 ? -1 : ((((nx * GeoConfig.CELLS) + ny) * GeoConfig.MAX_LAYERS) + bestK);
	}
	
	/**
	 * A growable int FIFO, used as both a queue and an append-and-index list. The flood fills push one entry per cell layer over a 2048x2048 region, so the boxed ArrayDeque this replaces allocated an Integer for every single one.
	 */
	private static class IntQueue
	{
		private int[] items = new int[1024];
		private int head = 0;
		private int tail = 0;
		
		void add(int value)
		{
			if (tail == items.length)
			{
				// Compact only once the dead prefix is worth reclaiming, otherwise grow: compacting a nearly-full buffer every push would turn this into quadratic work.
				if (head > (items.length / 2))
				{
					System.arraycopy(items, head, items, 0, tail - head);
					tail -= head;
					head = 0;
				}
				else
				{
					items = Arrays.copyOf(items, items.length * 2);
				}
			}
			
			items[tail++] = value;
		}
		
		int poll()
		{
			return items[head++];
		}
		
		boolean isEmpty()
		{
			return head == tail;
		}
		
		int size()
		{
			return tail - head;
		}
		
		int get(int index)
		{
			return items[head + index];
		}
	}
}
