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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/**
 * L2J region writers and hand editors: .l2j, cell masking and layer insertion.
 * @author Mobius
 */
public class GeoWriter
{
	/**
	 * Hand-edit an existing .l2j: ADDS a layer (z, nswe) to every cell of the rectangle.<br>
	 * For geometry the grid cannot express, like the Giran wagon ramp rising 48+/cell: synthetic intermediate treads keep every step within the engine's 40-unit rise limit.<br>
	 * MapExtractor x XX_YY --addlayer file.l2j cx1 cy1 cx2 cy2 z nswe
	 * @param file the .l2j file to edit in place
	 * @param cx1 the rectangle minimum cell X
	 * @param cy1 the rectangle minimum cell Y
	 * @param cx2 the rectangle maximum cell X
	 * @param cy2 the rectangle maximum cell Y
	 * @param addZ the layer height to insert
	 * @param addNswe the layer NSWE bits
	 * @throws IOException on read or write failure
	 */
	public static void addLayer(Path file, int cx1, int cy1, int cx2, int cy2, int addZ, int addNswe) throws IOException
	{
		final byte[] data = Files.readAllBytes(file);
		final ByteBuffer in = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		final ByteBuffer out = ByteBuffer.allocate(data.length + (64 * 1024 * 1024)).order(ByteOrder.LITTLE_ENDIAN);
		final short addData = (short) (((addZ << 1) & 0xFFF0) | (addNswe & 15));
		int changed = 0;
		
		// The 65536 blocks share one holder, every branch below fills all 64 cells before they are read.
		final short[][] cells = new short[64][];
		for (int bx = 0; bx < 256; bx++)
		{
			for (int by = 0; by < 256; by++)
			{
				final int type = in.get();
				final boolean touched = ((bx * 8) <= cx2) && (((bx * 8) + 7) >= cx1) && ((by * 8) <= cy2) && (((by * 8) + 7) >= cy1);
				
				if ((type == 0) && !touched)
				{
					out.put((byte) 0);
					out.putShort(in.getShort());
					continue;
				}
				
				// Collect the block's current per-cell layer lists.
				if (type == 0)
				{
					final short h = in.getShort();
					for (int c = 0; c < 64; c++)
					{
						cells[c] = new short[]
						{
							(short) (((h << 1) & 0xFFF0) | 15)
						};
					}
				}
				else if (type == 1)
				{
					for (int c = 0; c < 64; c++)
					{
						cells[c] = new short[]
						{
							in.getShort()
						};
					}
				}
				else
				{
					for (int c = 0; c < 64; c++)
					{
						final int n = in.get() & 0xFF;
						cells[c] = new short[n];
						for (int k = 0; k < n; k++)
						{
							cells[c][k] = in.getShort();
						}
					}
				}
				
				if (!touched)
				{
					// Re-emit unchanged in original form.
					out.put((byte) type);
					if (type == 1)
					{
						for (int c = 0; c < 64; c++)
						{
							out.putShort(cells[c][0]);
						}
					}
					else
					{
						for (int c = 0; c < 64; c++)
						{
							out.put((byte) cells[c].length);
							for (short s : cells[c])
							{
								out.putShort(s);
							}
						}
					}
					continue;
				}
				
				// Insert the new layer (top-first descending z) into hit cells, emit as multilayer.
				out.put((byte) 2);
				for (int c = 0; c < 64; c++)
				{
					final int cx = (bx * 8) + (c / 8);
					final int cy = (by * 8) + (c % 8);
					short[] layers = cells[c];
					if ((cx >= cx1) && (cx <= cx2) && (cy >= cy1) && (cy <= cy2))
					{
						final short[] grown = new short[layers.length + 1];
						int pos = layers.length;
						for (int k = 0; k < layers.length; k++)
						{
							if (((short) (layers[k] & 0xFFF0) >> 1) < addZ)
							{
								pos = k;
								break;
							}
						}
						
						System.arraycopy(layers, 0, grown, 0, pos);
						grown[pos] = addData;
						System.arraycopy(layers, pos, grown, pos + 1, layers.length - pos);
						layers = grown;
						changed++;
					}
					
					out.put((byte) layers.length);
					for (short s : layers)
					{
						out.putShort(s);
					}
				}
			}
		}
		
		out.flip();
		final byte[] result = new byte[out.remaining()];
		out.get(result);
		Files.write(file, result);
		System.out.println("Added layer z=" + addZ + " nswe=" + addNswe + " to " + changed + " cells in [" + cx1 + "," + cy1 + "]..[" + cx2 + "," + cy2 + "] -> " + file);
	}
	
	/**
	 * Hand-edit an existing .l2j: sets the NSWE of every cell in the given rectangle (region-local cell coordinates).<br>
	 * Mask 0 closes all directions, 15 opens all; e.g. mask 14 closes only East.<br>
	 * For client-collision spots no general rule can catch (sub-cell trenches, decorative rails): MapExtractor x XX_YY --blockcells file.l2j cx1 cy1 cx2 cy2 [nsweMask]
	 * @param file the .l2j file to edit in place
	 * @param cx1 the rectangle minimum cell X
	 * @param cy1 the rectangle minimum cell Y
	 * @param cx2 the rectangle maximum cell X
	 * @param cy2 the rectangle maximum cell Y
	 * @param mask the NSWE bits to set on every layer of the touched cells
	 * @throws IOException on read or write failure
	 */
	public static void blockCells(Path file, int cx1, int cy1, int cx2, int cy2, int mask) throws IOException
	{
		final byte[] data = Files.readAllBytes(file);
		final ByteBuffer in = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		final ByteBuffer out = ByteBuffer.allocate(data.length + (64 * 1024 * 1024)).order(ByteOrder.LITTLE_ENDIAN);
		int changed = 0;
		for (int bx = 0; bx < 256; bx++)
		{
			for (int by = 0; by < 256; by++)
			{
				final int type = in.get();
				final boolean touched = ((bx * 8) <= cx2) && (((bx * 8) + 7) >= cx1) && ((by * 8) <= cy2) && (((by * 8) + 7) >= cy1);
				if (type == 0)
				{
					final short h = in.getShort();
					if (!touched)
					{
						out.put((byte) 0);
						out.putShort(h);
					}
					else
					{
						// Expand to complex so single cells can close.
						out.put((byte) 1);
						for (int i = 0; i < 8; i++)
						{
							for (int j = 0; j < 8; j++)
							{
								final int cx = (bx * 8) + i;
								final int cy = (by * 8) + j;
								final boolean hit = (cx >= cx1) && (cx <= cx2) && (cy >= cy1) && (cy <= cy2);
								if (hit)
								{
									changed++;
								}
								
								out.putShort((short) (((h << 1) & 0xFFF0) | (hit ? (15 & mask) : 15)));
							}
						}
					}
				}
				else if (type == 1)
				{
					out.put((byte) 1);
					for (int i = 0; i < 8; i++)
					{
						for (int j = 0; j < 8; j++)
						{
							short v = in.getShort();
							final int cx = (bx * 8) + i;
							final int cy = (by * 8) + j;
							if ((cx >= cx1) && (cx <= cx2) && (cy >= cy1) && (cy <= cy2))
							{
								v = (short) ((v & 0xFFF0) | (15 & mask));
								changed++;
							}
							
							out.putShort(v);
						}
					}
				}
				else
				{
					out.put((byte) 2);
					for (int c = 0; c < 64; c++)
					{
						final int n = in.get() & 0xFF;
						out.put((byte) n);
						final int cx = (bx * 8) + (c / 8);
						final int cy = (by * 8) + (c % 8);
						final boolean hit = (cx >= cx1) && (cx <= cx2) && (cy >= cy1) && (cy <= cy2);
						if (hit && (n > 0))
						{
							changed++;
						}
						
						for (int k = 0; k < n; k++)
						{
							short v = in.getShort();
							if (hit)
							{
								v = (short) ((v & 0xFFF0) | (15 & mask));
							}
							
							out.putShort(v);
						}
					}
				}
			}
		}
		
		out.flip();
		final byte[] result = new byte[out.remaining()];
		out.get(result);
		Files.write(file, result);
		System.out.println("Blocked " + changed + " cells in [" + cx1 + "," + cy1 + "]..[" + cx2 + "," + cy2 + "] -> " + file);
	}
	
	/**
	 * Writes the grid as an L2J region file: flat / complex / multilayer blocks.
	 * @param grid the sampled region grid
	 * @param out the .l2j file to write
	 * @throws IOException on write failure
	 */
	public static void writeL2j(GeoGrid grid, Path out) throws IOException
	{
		writeL2j(grid, out, () -> false);
	}
	
	/**
	 * Writes the grid as an L2J region file with cooperative cancellation before the target file is published.
	 * @param grid the sampled region grid
	 * @param out the .l2j file to write
	 * @param stopRequested cancellation signal
	 * @throws IOException on write failure
	 */
	public static void writeL2j(GeoGrid grid, Path out, BooleanSupplier stopRequested) throws IOException
	{
		final ByteBuffer buf = ByteBuffer.allocate(48 * 1024 * 1024).order(ByteOrder.LITTLE_ENDIAN);
		for (int bx = 0; bx < 256; bx++)
		{
			checkCancelled(stopRequested);
			for (int by = 0; by < 256; by++)
			{
				// Flat-block rule: flat = every cell has one layer, all NSWE fully open, height span <= 32; the stored height is the MINIMUM.
				boolean allSingle = true;
				boolean open = true;
				boolean empty = true;
				int minZ = Integer.MAX_VALUE;
				int maxZ = Integer.MIN_VALUE;
				for (int i = 0; i < 8; i++)
				{
					for (int j = 0; j < 8; j++)
					{
						final int cx = (bx * 8) + i;
						final int cy = (by * 8) + j;
						final int n = grid.count(cx, cy);
						if (n != 1)
						{
							allSingle = false;
						}
						
						if (n >= 1)
						{
							empty = false;
							final short z = grid.z(cx, cy, 0);
							minZ = Math.min(minZ, z);
							maxZ = Math.max(maxZ, z);
							if ((n == 1) && (grid.nswe(cx, cy, 0) != 15))
							{
								open = false;
							}
						}
					}
				}
				
				if (empty)
				{
					// No surfaces at all (bare ocean tile): deep flat filler.
					buf.put((byte) 0);
					buf.putShort((short) -16384);
				}
				else if (allSingle && open && ((maxZ - minZ) <= 32))
				{
					buf.put((byte) 0);
					buf.putShort((short) minZ);
				}
				else if (allSingle)
				{
					buf.put((byte) 1);
					for (int i = 0; i < 8; i++)
					{
						for (int j = 0; j < 8; j++)
						{
							final int cx = (bx * 8) + i;
							final int cy = (by * 8) + j;
							buf.putShort((short) (((grid.z(cx, cy, 0) << 1) & 0xFFF0) | grid.nswe(cx, cy, 0)));
						}
					}
				}
				else
				{
					buf.put((byte) 2);
					for (int i = 0; i < 8; i++)
					{
						for (int j = 0; j < 8; j++)
						{
							final int cx = (bx * 8) + i;
							final int cy = (by * 8) + j;
							final int n = Math.max(1, grid.layerCount[(cx * GeoConfig.CELLS) + cy]);
							buf.put((byte) n);
							if (grid.count(cx, cy) == 0)
							{
								buf.putShort((short) ((((-16384) << 1) & 0xFFF0))); // No data: deep filler, all blocked.
								continue;
							}
							
							for (int layer = 0; layer < n; layer++)
							{
								buf.putShort((short) (((grid.z(cx, cy, layer) << 1) & 0xFFF0) | grid.nswe(cx, cy, layer)));
							}
						}
					}
				}
			}
		}
		
		buf.flip();
		final byte[] data = new byte[buf.remaining()];
		buf.get(data);
		checkCancelled(stopRequested);
		Files.write(out, data);
		System.out.println("Wrote " + data.length + " bytes to " + out);
	}
	
	private static void checkCancelled(BooleanSupplier stopRequested)
	{
		if ((stopRequested != null) && stopRequested.getAsBoolean())
		{
			throw new CancellationException("Map extraction stopped.");
		}
	}
}
