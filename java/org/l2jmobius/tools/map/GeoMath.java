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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Geometry helpers: triangle height sampling, cell/boundary clipping and wall-edge rasterization.
 * @author Mobius
 */
public class GeoMath
{
	/**
	 * Segment-triangle intersection (Moller-Trumbore, segment-limited): does the segment from a to b pass through the triangle?
	 * @param t the triangle {x0,y0,z0, x1,y1,z1, x2,y2,z2}
	 * @param ax segment start x
	 * @param ay segment start y
	 * @param az segment start z
	 * @param bx segment end x
	 * @param by segment end y
	 * @param bz segment end z
	 * @return true when the segment crosses the triangle
	 */
	public static boolean segmentHitsTriangle(double[] t, double ax, double ay, double az, double bx, double by, double bz)
	{
		final double dx = bx - ax;
		final double dy = by - ay;
		final double dz = bz - az;
		final double e1x = t[3] - t[0];
		final double e1y = t[4] - t[1];
		final double e1z = t[5] - t[2];
		final double e2x = t[6] - t[0];
		final double e2y = t[7] - t[1];
		final double e2z = t[8] - t[2];
		final double px = (dy * e2z) - (dz * e2y);
		final double py = (dz * e2x) - (dx * e2z);
		final double pz = (dx * e2y) - (dy * e2x);
		final double det = (e1x * px) + (e1y * py) + (e1z * pz);
		if (Math.abs(det) < 1e-9)
		{
			return false;
		}
		
		final double inv = 1.0 / det;
		final double tx = ax - t[0];
		final double ty = ay - t[1];
		final double tz = az - t[2];
		final double u = ((tx * px) + (ty * py) + (tz * pz)) * inv;
		if ((u < 0) || (u > 1))
		{
			return false;
		}
		
		final double qx = (ty * e1z) - (tz * e1y);
		final double qy = (tz * e1x) - (tx * e1z);
		final double qz = (tx * e1y) - (ty * e1x);
		final double v = ((dx * qx) + (dy * qy) + (dz * qz)) * inv;
		if ((v < 0) || ((u + v) > 1))
		{
			return false;
		}
		
		final double dist = ((e2x * qx) + (e2y * qy) + (e2z * qz)) * inv;
		return (dist >= 0) && (dist <= 1);
	}
	
	public static double triangleZ(double[] t, double px, double py)
	{
		final double d1 = ((px - t[3]) * (t[1] - t[4])) - ((t[0] - t[3]) * (py - t[4]));
		final double d2 = ((px - t[6]) * (t[4] - t[7])) - ((t[3] - t[6]) * (py - t[7]));
		final double d3 = ((px - t[0]) * (t[7] - t[1])) - ((t[6] - t[0]) * (py - t[1]));
		if ((((d1 < 0) || (d2 < 0) || (d3 < 0)) && ((d1 > 0) || (d2 > 0) || (d3 > 0))))
		{
			return Double.NaN;
		}
		
		final double area = ((t[3] - t[0]) * (t[7] - t[1])) - ((t[6] - t[0]) * (t[4] - t[1]));
		if (area == 0)
		{
			return Double.NaN;
		}
		
		final double w0 = (((t[3] - px) * (t[7] - py)) - ((t[6] - px) * (t[4] - py))) / area;
		final double w1 = (((t[6] - px) * (t[1] - py)) - ((t[0] - px) * (t[7] - py))) / area;
		return (w0 * t[2]) + (w1 * t[5]) + ((1 - w0 - w1) * t[8]);
	}
	
	/**
	 * Key for the boundary east (axis 0) or south (axis 1) of cell (cx, cy).
	 * @param cx the cell X index
	 * @param cy the cell Y index
	 * @param axis 0 for the east boundary, 1 for the south boundary
	 * @return the packed boundary key
	 */
	public static long edgeKey(int cx, int cy, int axis)
	{
		return ((((long) cx * GeoConfig.CELLS) + cy) * 2) + axis;
	}
	
	/**
	 * Records a wall Z-interval on a cell boundary.<br>
	 * Intervals are kept as a LIST per boundary (merging only overlaps) - a single min/max span would fuse a gate's door sill with its lintel and block the passage between them.
	 * @param edges the boundary interval map
	 * @param cx the cell X index
	 * @param cy the cell Y index
	 * @param axis 0 for the east boundary, 1 for the south boundary
	 * @param zMin the interval bottom
	 * @param zMax the interval top
	 */
	public static void markEdge(Map<Long, List<double[]>> edges, int cx, int cy, int axis, double zMin, double zMax)
	{
		if ((cx < 0) || (cx >= GeoConfig.CELLS) || (cy < 0) || (cy >= GeoConfig.CELLS))
		{
			return;
		}
		
		final List<double[]> list = edges.computeIfAbsent(edgeKey(cx, cy, axis), k -> new ArrayList<>(2));
		for (double[] span : list)
		{
			if ((zMin <= (span[1] + 8)) && (zMax >= (span[0] - 8)))
			{
				span[0] = Math.min(span[0], zMin);
				span[1] = Math.max(span[1], zMax);
				return;
			}
		}
		
		list.add(new double[]
		{
			zMin,
			zMax
		});
	}
	
	/**
	 * Clips the wall triangle against every cell-boundary strip its bounding box crosses and records the EXACT Z-range of the wall surface at that boundary.<br>
	 * (Interpolating along triangle edges instead poisons passages: a tall wall panel's diagonal edges cross gate openings at low Z.)
	 * @param t the wall triangle {x0,y0,z0, x1,y1,z1, x2,y2,z2}
	 * @param edges the boundary interval map to fill
	 * @param originX the region origin world X
	 * @param originY the region origin world Y
	 */
	public static void rasterWallEdges(double[] t, Map<Long, List<double[]>> edges, double originX, double originY)
	{
		final double minX = Math.min(t[0], Math.min(t[3], t[6]));
		final double maxX = Math.max(t[0], Math.max(t[3], t[6]));
		final double minY = Math.min(t[1], Math.min(t[4], t[7]));
		final double maxY = Math.max(t[1], Math.max(t[4], t[7]));
		final int cy0 = Math.max(0, (int) Math.floor((minY - originY) / 16));
		final int cy1 = Math.min(GeoConfig.CELLS - 1, (int) Math.floor((maxY - originY) / 16));
		final int cx0 = Math.max(0, (int) Math.floor((minX - originX) / 16));
		final int cx1 = Math.min(GeoConfig.CELLS - 1, (int) Math.floor((maxX - originX) / 16));
		
		// Boundary strips cover only the CENTER 8 units of each 16-unit edge: characters cross boundaries at cell centers,
		// so a pilaster/arch rib touching only the boundary ENDS must not close the open middle (Giran temple nave: rib-closed N/S edges broke every diagonal step via the engine's anti-corner-cut).
		// Real walls span whole boundaries and still hit.
		// Strips reach +-4 units to the sides of each boundary: a wall face 1-4 units OFF the boundary line sits outside the tall-wall center clip too and would otherwise be invisible to both mechanisms (the Gludio town-wall stretch ran unblocked through that dead zone) - and a wall within 4 units blocks a character's body from crossing anyway.
		// Vertical boundaries x = m*16 (between cells m-1 and m).
		for (int m = cx0 + 1; m <= cx1; m++)
		{
			final double bx = originX + (m * 16);
			for (int j = cy0; j <= cy1; j++)
			{
				final double[] range = clipZRange(t, bx - 4, originY + (j * 16) + 4, bx + 4, originY + (j * 16) + 12);
				if (range != null)
				{
					markEdge(edges, m - 1, j, 0, range[0] + GeoConfig.GEODATA_Z_OFFSET, range[1] + GeoConfig.GEODATA_Z_OFFSET);
				}
			}
		}
		
		// Horizontal boundaries y = m*16 (between cells m-1 and m).
		for (int m = cy0 + 1; m <= cy1; m++)
		{
			final double by = originY + (m * 16);
			for (int i = cx0; i <= cx1; i++)
			{
				final double[] range = clipZRange(t, originX + (i * 16) + 4, by - 4, originX + (i * 16) + 12, by + 4);
				if (range != null)
				{
					markEdge(edges, i, m - 1, 1, range[0] + GeoConfig.GEODATA_Z_OFFSET, range[1] + GeoConfig.GEODATA_Z_OFFSET);
				}
			}
		}
	}
	
	/**
	 * Clips the triangle against a HAIRLINE strip (0.5 units) on every cell boundary it crosses and records the surface Z-range EXACTLY ON the boundary line.<br>
	 * Surfaces that stop 1+ units short of the boundary leave no interval: butt-joined pavement sheets whose upper sheet never reaches the line (the client-wedging seam class) read as DISCONNECTED, while an architectural step whose upper tread meets the line reads as connected.
	 * @param t the triangle {x0,y0,z0, x1,y1,z1, x2,y2,z2}
	 * @param edges the boundary interval map to fill
	 * @param originX the region origin world X
	 * @param originY the region origin world Y
	 */
	public static void rasterConnectEdges(double[] t, Map<Long, List<double[]>> edges, double originX, double originY)
	{
		final double minX = Math.min(t[0], Math.min(t[3], t[6]));
		final double maxX = Math.max(t[0], Math.max(t[3], t[6]));
		final double minY = Math.min(t[1], Math.min(t[4], t[7]));
		final double maxY = Math.max(t[1], Math.max(t[4], t[7]));
		final int cy0 = Math.max(0, (int) Math.floor((minY - originY) / 16));
		final int cy1 = Math.min(GeoConfig.CELLS - 1, (int) Math.floor((maxY - originY) / 16));
		final int cx0 = Math.max(0, (int) Math.floor((minX - originX) / 16));
		final int cx1 = Math.min(GeoConfig.CELLS - 1, (int) Math.floor((maxX - originX) / 16));
		for (int m = cx0 + 1; m <= cx1; m++)
		{
			final double bx = originX + (m * 16);
			for (int j = cy0; j <= cy1; j++)
			{
				final double[] range = clipZRange(t, bx - 0.5, originY + (j * 16) + 4, bx + 0.5, originY + (j * 16) + 12);
				if (range != null)
				{
					markEdge(edges, m - 1, j, 0, range[0] + GeoConfig.GEODATA_Z_OFFSET, range[1] + GeoConfig.GEODATA_Z_OFFSET);
				}
			}
		}
		
		for (int m = cy0 + 1; m <= cy1; m++)
		{
			final double by = originY + (m * 16);
			for (int i = cx0; i <= cx1; i++)
			{
				final double[] range = clipZRange(t, originX + (i * 16) + 4, by - 0.5, originX + (i * 16) + 12, by + 0.5);
				if (range != null)
				{
					markEdge(edges, i, m - 1, 1, range[0] + GeoConfig.GEODATA_Z_OFFSET, range[1] + GeoConfig.GEODATA_Z_OFFSET);
				}
			}
		}
	}
	
	// One scratch pair for the whole sampler, which runs single-threaded: give clipZRange its own buffers if a parallel builder ever calls it.
	private static final double[] CLIP_A = new double[24];
	private static final double[] CLIP_B = new double[24];
	
	/**
	 * Z-range and XY area of the triangle clipped to the cell rectangle.
	 * @param t the triangle {x0,y0,z0, x1,y1,z1, x2,y2,z2}
	 * @param x0 the rectangle minimum X
	 * @param y0 the rectangle minimum Y
	 * @param x1 the rectangle maximum X
	 * @param y1 the rectangle maximum Y
	 * @return {zMin, zMax, area}, or null when the triangle lies outside the rectangle
	 */
	public static double[] clipZRange(double[] t, double x0, double y0, double x1, double y1)
	{
		// Sutherland-Hodgman against the four rect half-planes, ping-ponging between two flat {x, y, z, x, y, z, ...} buffers.
		// Clipping a convex polygon by one half-plane adds at most one vertex, so a triangle tops out at 7 and 8 slots always suffice.
		// The sampler calls this per triangle per cell, so the old per-edge ArrayList and per-vertex double[3] are what these two buffers replace.
		System.arraycopy(t, 0, CLIP_A, 0, 9);
		int count = clipAxis(CLIP_A, 3, 0, x0, true, CLIP_B);
		if (count < 3)
		{
			return null;
		}
		
		count = clipAxis(CLIP_B, count, 0, x1, false, CLIP_A);
		if (count < 3)
		{
			return null;
		}
		
		count = clipAxis(CLIP_A, count, 1, y0, true, CLIP_B);
		if (count < 3)
		{
			return null;
		}
		
		count = clipAxis(CLIP_B, count, 1, y1, false, CLIP_A);
		if (count < 3)
		{
			return null;
		}
		
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		for (int i = 0; i < count; i++)
		{
			final double z = CLIP_A[(i * 3) + 2];
			min = Math.min(min, z);
			max = Math.max(max, z);
		}
		
		double area = 0;
		for (int i = 0; i < count; i++)
		{
			final int ai = i * 3;
			final int bi = ((i + 1) % count) * 3;
			area += (CLIP_A[ai] * CLIP_A[bi + 1]) - (CLIP_A[bi] * CLIP_A[ai + 1]);
		}
		
		return new double[]
		{
			min,
			max,
			Math.abs(area) / 2
		};
	}
	
	/**
	 * Sutherland-Hodgman clip of a polygon against one axis-aligned half-plane, interpolating Z. Allocates nothing: the result is written into the caller's buffer.
	 * @param in the source vertices, flat {x, y, z, x, y, z, ...}
	 * @param count the number of vertices in the source buffer
	 * @param axis 0 to clip on X, 1 to clip on Y
	 * @param bound the half-plane boundary value
	 * @param keepGreater true to keep the side above the bound
	 * @param out the destination buffer, must hold count + 1 vertices
	 * @return the number of vertices written to the destination buffer
	 */
	private static int clipAxis(double[] in, int count, int axis, double bound, boolean keepGreater, double[] out)
	{
		int n = 0;
		for (int i = 0; i < count; i++)
		{
			final int ai = i * 3;
			final int bi = ((i + 1) % count) * 3;
			final boolean aIn = keepGreater ? (in[ai + axis] >= bound) : (in[ai + axis] <= bound);
			final boolean bIn = keepGreater ? (in[bi + axis] >= bound) : (in[bi + axis] <= bound);
			if (aIn)
			{
				out[n * 3] = in[ai];
				out[(n * 3) + 1] = in[ai + 1];
				out[(n * 3) + 2] = in[ai + 2];
				n++;
			}
			
			if (aIn != bIn)
			{
				final double f = (bound - in[ai + axis]) / (in[bi + axis] - in[ai + axis]);
				out[n * 3] = in[ai] + ((in[bi] - in[ai]) * f);
				out[(n * 3) + 1] = in[ai + 1] + ((in[bi + 1] - in[ai + 1]) * f);
				out[(n * 3) + 2] = in[ai + 2] + ((in[bi + 2] - in[ai + 2]) * f);
				n++;
			}
		}
		
		return n;
	}
}
