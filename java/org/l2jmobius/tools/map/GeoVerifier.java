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
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Comparison and dump utilities against reference geodata files.
 * @author Mobius
 */
public class GeoVerifier
{
	public static void verify(GeoGrid grid, Path referenceFile) throws IOException
	{
		final ByteBuffer b = ByteBuffer.wrap(Files.readAllBytes(referenceFile)).order(ByteOrder.LITTLE_ENDIAN);
		long cells = 0;
		long heightOk = 0;
		long nsweOk = 0;
		long nsweCells = 0;
		final long[] deltaHist = new long[5]; // <=8, <=16, <=32, <=64, >64
		final long[] typeCells = new long[3];
		final long[] typeOk = new long[3];
		final long[] typeWater = new long[3]; // Reference far ABOVE ours: water surface vs our seabed.
		int debugSamples = 8;
		final short[] refZ = new short[256];
		outer: for (int bx = 0; bx < 256; bx++)
		{
			for (int by = 0; by < 256; by++)
			{
				final int type = b.get();
				if ((type < 0) || (type > 2))
				{
					System.out.println("Reference file corrupt at block " + bx + "," + by + " - comparing what was read.");
					break outer;
				}
				
				if (type == 0)
				{
					final short h = b.getShort();
					for (int i = 0; i < 8; i++)
					{
						for (int j = 0; j < 8; j++)
						{
							final int cx = (bx * 8) + i;
							final int cy = (by * 8) + j;
							if (grid.count(cx, cy) == 0)
							{
								continue;
							}
							
							cells++;
							final int dz = Math.abs(grid.z(cx, cy, 0) - h);
							bucketDelta(deltaHist, dz);
							typeCells[0]++;
							if ((h - grid.z(cx, cy, 0)) > 200)
							{
								typeWater[0]++;
							}
							
							if (dz <= 16)
							{
								typeOk[0]++;
								heightOk++;
								nsweCells++;
								if (grid.nswe(cx, cy, 0) == 15)
								{
									nsweOk++;
								}
							}
						}
					}
				}
				else if (type == 1)
				{
					for (int i = 0; i < 8; i++)
					{
						for (int j = 0; j < 8; j++)
						{
							final short data = b.getShort();
							final int cx = (bx * 8) + i;
							final int cy = (by * 8) + j;
							if (grid.count(cx, cy) == 0)
							{
								continue;
							}
							
							final short h = (short) ((short) (data & 0xFFF0) >> 1);
							final int refNswe = data & 0x000F;
							cells++;
							final int dz = Math.abs(grid.z(cx, cy, 0) - h);
							bucketDelta(deltaHist, dz);
							typeCells[1]++;
							if ((h - grid.z(cx, cy, 0)) > 200)
							{
								typeWater[1]++;
							}
							
							if (dz <= 16)
							{
								typeOk[1]++;
								heightOk++;
								nsweCells++;
								if (grid.nswe(cx, cy, 0) == refNswe)
								{
									nsweOk++;
								}
							}
						}
					}
				}
				else
				{
					for (int c = 0; c < 64; c++)
					{
						final int n = b.get() & 0xFF;
						final int i = c / 8;
						final int j = c % 8;
						final int cx = (bx * 8) + i;
						final int cy = (by * 8) + j;
						
						// Only layer 0 is compared; the rest is kept solely for the debug sample line below.
						short refTop = 0;
						int refNswe = 0;
						for (int k = 0; k < n; k++)
						{
							final short data = b.getShort();
							final short value = (short) ((short) (data & 0xFFF0) >> 1);
							refZ[k] = value;
							if (k == 0)
							{
								refTop = value;
								refNswe = data & 0x000F;
							}
						}
						
						if ((n == 0) || (grid.count(cx, cy) == 0))
						{
							continue;
						}
						
						final short h = refTop;
						cells++;
						final int dz = Math.abs(grid.z(cx, cy, 0) - h);
						bucketDelta(deltaHist, dz);
						typeCells[2]++;
						if ((h - grid.z(cx, cy, 0)) > 200)
						{
							typeWater[2]++;
							if (debugSamples > 0)
							{
								debugSamples--;
								final StringBuilder mine = new StringBuilder();
								final int cellCount = grid.count(cx, cy);
								for (int k = 0; k < cellCount; k++)
								{
									mine.append(grid.z(cx, cy, k)).append(' ');
								}
								
								System.out.println("  sample cell=(" + cx + "," + cy + ") reference=" + Arrays.toString(Arrays.copyOf(refZ, n)) + " mine=[" + mine.toString().trim() + "]");
							}
						}
						
						if (dz <= 16)
						{
							typeOk[2]++;
							heightOk++;
							nsweCells++;
							if (grid.nswe(cx, cy, 0) == refNswe)
							{
								nsweOk++;
							}
						}
					}
				}
			}
		}
		
		System.out.printf("Compared cells: %d%n", cells);
		System.out.printf("Top-layer height within 16: %.1f%% (delta histogram <=8:%d <=16:%d <=32:%d <=64:%d >64:%d)%n", (100.0 * heightOk) / cells, deltaHist[0], deltaHist[1], deltaHist[2], deltaHist[3], deltaHist[4]);
		System.out.printf("NSWE exact match (where height matches): %.1f%%%n", (100.0 * nsweOk) / nsweCells);
		final String[] typeNames =
		{
			"flat",
			"complex",
			"multilayer"
		};
		
		for (int t = 0; t < 3; t++)
		{
			System.out.printf("  %s: cells=%d heightOk=%.1f%% waterLike=%.1f%%%n", typeNames[t], typeCells[t], (100.0 * typeOk[t]) / Math.max(1, typeCells[t]), (100.0 * typeWater[t]) / Math.max(1, typeCells[t]));
		}
	}
	
	public static void bucketDelta(long[] hist, int dz)
	{
		hist[dz <= 8 ? 0 : dz <= 16 ? 1 : dz <= 32 ? 2 : dz <= 64 ? 3 : 4]++;
	}
	
	/**
	 * Compares terrain heights against an L2J geodata region file (256x256 blocks, 8x8 cells of 16 units).<br>
	 * Flat blocks are mostly open terrain, so the median delta exposes any systematic bias in the conversion.
	 * @param t the terrain to compare
	 * @param l2jFile the geodata region file
	 * @param mapX the region X coordinate
	 * @param mapY the region Y coordinate
	 * @throws IOException on unreadable file
	 */
	public static void geoCompare(Terrain t, Path l2jFile, int mapX, int mapY) throws IOException
	{
		final ByteBuffer b = ByteBuffer.wrap(Files.readAllBytes(l2jFile)).order(ByteOrder.LITTLE_ENDIAN);
		final double originX = (mapX - 20) * 32768;
		final double originY = (mapY - 18) * 32768;
		
		double[] deltas = new double[1024];
		int deltaCount = 0;
		final List<double[]> pairs = new ArrayList<>();
		for (int bx = 0; bx < 256; bx++)
		{
			for (int by = 0; by < 256; by++)
			{
				final int type = b.get();
				switch (type)
				{
					case 0: // Flat: one height for the whole block.
					{
						final short geoZ = b.getShort();
						final double terrainZ = t.heightAt(originX + (bx * 128) + 64, originY + (by * 128) + 64);
						if (deltaCount == deltas.length)
						{
							deltas = Arrays.copyOf(deltas, deltas.length * 2);
						}
						
						deltas[deltaCount++] = terrainZ - geoZ;
						pairs.add(new double[]
						{
							geoZ,
							terrainZ
						});
						break;
					}
					case 1: // Complex: 64 cells, skip (buildings/bridges pollute).
					{
						b.position(b.position() + 128);
						break;
					}
					case 2: // Multilayer: variable, skip.
					{
						for (int c = 0; c < 64; c++)
						{
							final int layers = b.get() & 0xFF;
							b.position(b.position() + (layers * 2));
						}
						break;
					}
					default:
					{
						// Corrupt/unknown data past this point: keep what was parsed so far.
						System.out.printf("Warning: invalid block type %d at block %d,%d (offset %d) - using %d blocks parsed so far.%n", type, bx, by, b.position() - 1, (bx * 256) + by);
						bx = 256;
						by = 256;
						break;
					}
				}
				
				if (bx == 256)
				{
					break;
				}
			}
		}
		
		Arrays.sort(deltas, 0, deltaCount);
		final int n = deltaCount;
		if (n == 0)
		{
			System.out.println("No flat blocks to compare.");
			return;
		}
		
		double sum = 0;
		for (int i = 0; i < n; i++)
		{
			sum += deltas[i];
		}
		
		System.out.printf("Flat blocks compared: %d%n", n);
		System.out.printf("terrain - geodata delta: median=%.1f mean=%.1f p10=%.1f p90=%.1f min=%.1f max=%.1f%n", deltas[n / 2], sum / n, deltas[n / 10], deltas[(9 * n) / 10], deltas[0], deltas[n - 1]);
		
		// Regress (terrain - geo) against geo height on non-water samples (|delta - median| < 100) to distinguish a constant bias (slope ~ 0) from a scale error (slope != 0).
		final double median = deltas[n / 2];
		double sx = 0;
		double sy = 0;
		double sxx = 0;
		double sxy = 0;
		int m = 0;
		for (double[] p : pairs)
		{
			final double d = p[1] - p[0];
			if (Math.abs(d - median) < 100)
			{
				sx += p[0];
				sy += d;
				sxx += p[0] * p[0];
				sxy += p[0] * d;
				m++;
			}
		}
		
		final double slope = ((m * sxy) - (sx * sy)) / ((m * sxx) - (sx * sx));
		System.out.printf("clean samples=%d regression: delta = %.5f * geoZ + %.1f%n", m, slope, (sy - (slope * sx)) / m);
	}
	
	public static void dump(Terrain t, Path out) throws IOException
	{
		try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(out)))
		{
			for (int y = 0; y < t.vSize; y++)
			{
				for (int x = 0; x < t.uSize; x++)
				{
					w.printf("%.0f %.0f %.1f%n", t.texelToWorldX(x), t.texelToWorldY(y), t.worldZ(x, y));
				}
			}
		}
		
		System.out.println("Dumped " + (t.uSize * t.vSize) + " heights to " + out);
	}
}
