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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.l2jmobius.gameserver.config.GeoEngineConfig;

/**
 * A region backed by compressed geodata storage.<br>
 * Each block is described by one index entry: type in bits 30-31, complex format in bits 28-29 and payload offset in bits 0-27.<br>
 * Flat blocks store their height directly in the index and need no payload.<br>
 * Complex blocks store a base height plus one byte per cell (height step and NSWE) when the block height spread allows it, falling back to raw shorts otherwise.<br>
 * Multilayer blocks keep their original layout. Payload lives in chunks of at most 1 MB to stay below the G1 humongous allocation threshold.<br>
 * Flat blocks converted to complex at runtime are kept in a small overlay list, saving as complex blocks like before.
 * @author HorridoJoho, Mobius
 */
public class GeoRegion extends Region
{
	private static final int TYPE_FLAT = 0;
	private static final int TYPE_COMPLEX = 1;
	private static final int TYPE_MULTILAYER = 2;
	private static final int TYPE_OVERLAY = 3;
	private static final int FMT_STEPS_4BIT = 0;
	private static final int FMT_STEPS_8BIT = 1;
	private static final int FMT_RAW = 2;
	private static final int OFFSET_MASK = 0x0FFFFFFF;
	private static final int CHUNK_SHIFT = 20;
	private static final int CHUNK_SIZE = 1 << CHUNK_SHIFT;
	private static final int POS_MASK = CHUNK_SIZE - 1;
	/** Height between two consecutive encoded height steps; geodata heights are multiples of it. */
	private static final int HEIGHT_STEP = 8;
	
	private byte[][] _data;
	private int[] _index;
	private final List<short[]> _overlay = new ArrayList<>();
	
	/**
	 * Creates a {@code Region} by loading its blocks from the given buffer.
	 * @param bb The {@code ByteBuffer} containing the region data.
	 */
	public GeoRegion(ByteBuffer bb)
	{
		load(bb);
	}
	
	/**
	 * Loads the region's blocks from the given buffer, compressing them into the packed storage.
	 * @param bb The {@code ByteBuffer} containing the region data.
	 */
	public void load(ByteBuffer bb)
	{
		final int[] index = new int[REGION_BLOCKS];
		final List<byte[]> chunks = new ArrayList<>();
		final byte[] scratch = new byte[CHUNK_SIZE];
		final short[] cells = new short[BLOCK_CELLS];
		int pos = 0;
		for (int blockOffset = 0; blockOffset < REGION_BLOCKS; blockOffset++)
		{
			final int blockType = bb.get();
			switch (blockType)
			{
				case TYPE_FLAT:
				{
					index[blockOffset] = bb.getShort() & 0xFFFF;
					break;
				}
				case TYPE_COMPLEX:
				{
					int min = Integer.MAX_VALUE;
					int max = Integer.MIN_VALUE;
					for (int i = 0; i < BLOCK_CELLS; i++)
					{
						cells[i] = bb.getShort();
						final int cellHeight = decodeHeight(cells[i]);
						min = Math.min(min, cellHeight);
						max = Math.max(max, cellHeight);
					}
					final int steps = (max - min) / HEIGHT_STEP;
					final int fmt = steps <= 15 ? FMT_STEPS_4BIT : steps <= 255 ? FMT_STEPS_8BIT : FMT_RAW;
					final int size = fmt == FMT_STEPS_4BIT ? 2 + BLOCK_CELLS : fmt == FMT_STEPS_8BIT ? 2 + BLOCK_CELLS + (BLOCK_CELLS / 2) : BLOCK_CELLS * 2;
					if ((pos + size) > CHUNK_SIZE)
					{
						chunks.add(Arrays.copyOf(scratch, pos));
						pos = 0;
					}
					index[blockOffset] = (TYPE_COMPLEX << 30) | (fmt << 28) | (chunks.size() << CHUNK_SHIFT) | pos;
					if (fmt == FMT_RAW)
					{
						for (int i = 0; i < BLOCK_CELLS; i++)
						{
							scratch[pos + (i * 2)] = (byte) (cells[i] & 0xff);
							scratch[pos + (i * 2) + 1] = (byte) ((cells[i] >> 8) & 0xff);
						}
					}
					else
					{
						scratch[pos] = (byte) (min & 0xff);
						scratch[pos + 1] = (byte) ((min >> 8) & 0xff);
						for (int i = 0; i < BLOCK_CELLS; i++)
						{
							final int step = (decodeHeight(cells[i]) - min) / HEIGHT_STEP;
							final int nswe = cells[i] & 0xF;
							if (fmt == FMT_STEPS_4BIT)
							{
								scratch[pos + 2 + i] = (byte) ((step << 4) | nswe);
							}
							else
							{
								scratch[pos + 2 + i] = (byte) step;
								final int nibbleAt = pos + 2 + BLOCK_CELLS + (i / 2);
								scratch[nibbleAt] = (byte) ((i % 2) == 0 ? ((scratch[nibbleAt] & 0xF0) | nswe) : ((scratch[nibbleAt] & 0x0F) | (nswe << 4)));
							}
						}
					}
					pos += size;
					break;
				}
				case TYPE_MULTILAYER:
				{
					final int start = bb.position();
					for (int i = 0; i < BLOCK_CELLS; i++)
					{
						final byte nLayers = bb.get();
						if ((nLayers <= 0) || (nLayers > 125))
						{
							throw new RuntimeException("Geo file corrupted! Invalid layers count!");
						}
						
						bb.position(bb.position() + (nLayers * 2));
					}
					final int size = bb.position() - start;
					if ((pos + size) > CHUNK_SIZE)
					{
						chunks.add(Arrays.copyOf(scratch, pos));
						pos = 0;
					}
					index[blockOffset] = (TYPE_MULTILAYER << 30) | (chunks.size() << CHUNK_SHIFT) | pos;
					bb.position(start);
					bb.get(scratch, pos, size);
					pos += size;
					break;
				}
				default:
				{
					throw new RuntimeException("Invalid block type " + blockType + "!");
				}
			}
		}
		chunks.add(Arrays.copyOf(scratch, pos));
		_overlay.clear();
		_data = chunks.toArray(new byte[0][]);
		_index = index;
	}
	
	/**
	 * Calculates the block index covering the given coordinate.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @return The block index as an {@code int}.
	 */
	private static int blockIndex(int geoX, int geoY)
	{
		return (((geoX / BLOCK_CELLS_X) % REGION_BLOCKS_X) * REGION_BLOCKS_Y) + ((geoY / BLOCK_CELLS_Y) % REGION_BLOCKS_Y);
	}
	
	/**
	 * Calculates the cell index within a block for the given coordinate.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @return The cell index as an {@code int}.
	 */
	private static int cellIndex(int geoX, int geoY)
	{
		return ((geoX % BLOCK_CELLS_X) * BLOCK_CELLS_Y) + (geoY % BLOCK_CELLS_Y);
	}
	
	/**
	 * Extracts the height from packed cell data.
	 * @param data The packed cell data.
	 * @return The height as an {@code int}.
	 */
	private static int decodeHeight(short data)
	{
		return (short) (data & 0x0FFF0) >> 1;
	}
	
	/**
	 * Reads a little endian {@code short} from a payload chunk.
	 * @param chunk The payload chunk.
	 * @param p The byte position.
	 * @return The value as a {@code short}.
	 */
	private static short leShort(byte[] chunk, int p)
	{
		return (short) ((chunk[p] & 0xff) | (chunk[p + 1] << 8));
	}
	
	/**
	 * Retrieves the packed cell data of a complex block.
	 * @param entry The block's index entry.
	 * @param cell The cell index within the block.
	 * @return The packed cell data as a {@code short}.
	 */
	private short complexCellData(int entry, int cell)
	{
		final int offset = entry & OFFSET_MASK;
		final byte[] chunk = _data[offset >>> CHUNK_SHIFT];
		final int p = offset & POS_MASK;
		final int fmt = (entry >>> 28) & 3;
		if (fmt == FMT_RAW)
		{
			return leShort(chunk, p + (cell * 2));
		}
		
		final int base = leShort(chunk, p);
		if (fmt == FMT_STEPS_4BIT)
		{
			final int packed = chunk[p + 2 + cell] & 0xFF;
			return (short) (((base + ((packed >>> 4) * HEIGHT_STEP)) << 1) | (packed & 0xF));
		}
		
		final int height = base + ((chunk[p + 2 + cell] & 0xFF) * HEIGHT_STEP);
		final byte pair = chunk[p + 2 + BLOCK_CELLS + (cell / 2)];
		final int nswe = (cell % 2) == 0 ? (pair & 0xF) : ((pair >>> 4) & 0xF);
		return (short) ((height << 1) | nswe);
	}
	
	/**
	 * Stores new NSWE flags for a cell of a complex block, keeping its height.
	 * @param entry The block's index entry.
	 * @param cell The cell index within the block.
	 * @param nswe The NSWE flags to store.
	 */
	private void complexSetCellNswe(int entry, int cell, int nswe)
	{
		final int offset = entry & OFFSET_MASK;
		final byte[] chunk = _data[offset >>> CHUNK_SHIFT];
		final int p = offset & POS_MASK;
		final int fmt = (entry >>> 28) & 3;
		if (fmt == FMT_STEPS_4BIT)
		{
			chunk[p + 2 + cell] = (byte) ((chunk[p + 2 + cell] & 0xF0) | nswe);
		}
		else if (fmt == FMT_STEPS_8BIT)
		{
			final int nibbleAt = p + 2 + BLOCK_CELLS + (cell / 2);
			chunk[nibbleAt] = (byte) ((cell % 2) == 0 ? ((chunk[nibbleAt] & 0xF0) | nswe) : ((chunk[nibbleAt] & 0x0F) | (nswe << 4)));
		}
		else
		{
			final int at = p + (cell * 2);
			chunk[at] = (byte) ((chunk[at] & 0xF0) | nswe);
		}
	}
	
	/**
	 * Finds the byte position of the given cell's layer count within a multilayer block.
	 * @param chunk The payload chunk.
	 * @param base The block's payload position.
	 * @param cell The cell index within the block.
	 * @return The cell data position as an {@code int}.
	 */
	private static int multiCellPos(byte[] chunk, int base, int cell)
	{
		int p = base;
		for (int i = 0; i < cell; i++)
		{
			p += 1 + (chunk[p] * 2);
		}
		
		return p;
	}
	
	/**
	 * Finds the byte position of the layer whose height is closest to {@code worldZ}.
	 * @param chunk The payload chunk.
	 * @param base The block's payload position.
	 * @param cell The cell index within the block.
	 * @param worldZ The world Z coordinate.
	 * @return The layer position as an {@code int}.
	 */
	private static int multiNearestLayerPos(byte[] chunk, int base, int cell, int worldZ)
	{
		final int start = multiCellPos(chunk, base, cell);
		final int end = start + 1 + (chunk[start] * 2);
		int nearestDZ = 0;
		int nearestPos = 0;
		for (int p = start + 1; p < end; p += 2)
		{
			final int layerZ = decodeHeight(leShort(chunk, p));
			if (layerZ == worldZ)
			{
				return p;
			}
			
			final int layerDZ = Math.abs(layerZ - worldZ);
			if ((p == (start + 1)) || (layerDZ < nearestDZ))
			{
				nearestDZ = layerDZ;
				nearestPos = p;
			}
		}
		
		return nearestPos;
	}
	
	/**
	 * Retrieves the packed data of the cell layer nearest to the given coordinate.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @param worldZ The world Z coordinate.
	 * @return The packed cell data as a {@code short}.
	 */
	private short nearestCellData(int geoX, int geoY, int worldZ)
	{
		final int entry = _index[blockIndex(geoX, geoY)];
		switch (entry >>> 30)
		{
			case TYPE_FLAT:
			{
				return (short) (((short) entry << 1) | NSWE_ALL);
			}
			case TYPE_COMPLEX:
			{
				return complexCellData(entry, cellIndex(geoX, geoY));
			}
			case TYPE_MULTILAYER:
			{
				final int offset = entry & OFFSET_MASK;
				final byte[] chunk = _data[offset >>> CHUNK_SHIFT];
				return leShort(chunk, multiNearestLayerPos(chunk, offset & POS_MASK, cellIndex(geoX, geoY), worldZ));
			}
			default:
			{
				return _overlay.get(entry & OFFSET_MASK)[cellIndex(geoX, geoY)];
			}
		}
	}
	
	/**
	 * Checks the NSWE passability at the given coordinate.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @param worldZ The world Z coordinate.
	 * @param nswe The NSWE flags to check.
	 * @return {@code true} if every requested flag is set, {@code false} otherwise.
	 */
	@Override
	public boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe)
	{
		return (nearestCellData(geoX, geoY, worldZ) & nswe) == nswe;
	}
	
	/**
	 * Sets the given NSWE flags at the coordinate; flat blocks are left unchanged.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @param worldZ The world Z coordinate.
	 * @param nswe The NSWE flags to set.
	 */
	@Override
	public void setNearestNswe(int geoX, int geoY, int worldZ, byte nswe)
	{
		final int entry = _index[blockIndex(geoX, geoY)];
		if ((entry >>> 30) == TYPE_FLAT)
		{
			// Flat block cells are enabled by default on all directions.
			return;
		}
		
		final int currentNswe = nearestCellData(geoX, geoY, worldZ) & 0xF;
		if ((currentNswe & nswe) != 0)
		{
			return;
		}
		
		writeNswe(entry, geoX, geoY, worldZ, currentNswe | nswe);
	}
	
	/**
	 * Clears the given NSWE flags at the coordinate, converting a flat block to complex first.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @param worldZ The world Z coordinate.
	 * @param nswe The NSWE flags to unset.
	 */
	@Override
	public void unsetNearestNswe(int geoX, int geoY, int worldZ, byte nswe)
	{
		int entry = _index[blockIndex(geoX, geoY)];
		if ((entry >>> 30) == TYPE_FLAT)
		{
			entry = convertFlatToComplex(entry, geoX, geoY);
		}
		
		final int currentNswe = nearestCellData(geoX, geoY, worldZ) & 0xF;
		if ((currentNswe & nswe) == 0)
		{
			return;
		}
		
		writeNswe(entry, geoX, geoY, worldZ, currentNswe & ~nswe);
	}
	
	/**
	 * Stores new NSWE flags at the coordinate for a non-flat block.
	 * @param entry The block's index entry.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @param worldZ The world Z coordinate.
	 * @param newNswe The NSWE flags to store.
	 */
	private void writeNswe(int entry, int geoX, int geoY, int worldZ, int newNswe)
	{
		switch (entry >>> 30)
		{
			case TYPE_COMPLEX:
			{
				complexSetCellNswe(entry, cellIndex(geoX, geoY), newNswe);
				break;
			}
			case TYPE_MULTILAYER:
			{
				final int offset = entry & OFFSET_MASK;
				final byte[] chunk = _data[offset >>> CHUNK_SHIFT];
				final int layerPos = multiNearestLayerPos(chunk, offset & POS_MASK, cellIndex(geoX, geoY), worldZ);
				final int layerZ = decodeHeight(leShort(chunk, layerPos));
				final short combined = (short) ((layerZ << 1) | newNswe);
				chunk[layerPos] = (byte) (combined & 0xff);
				chunk[layerPos + 1] = (byte) ((combined >> 8) & 0xff);
				break;
			}
			default:
			{
				final short[] cells = _overlay.get(entry & OFFSET_MASK);
				final int cell = cellIndex(geoX, geoY);
				cells[cell] = (short) ((decodeHeight(cells[cell]) << 1) | newNswe);
				break;
			}
		}
	}
	
	/**
	 * Replaces the flat block at the given coordinate with an equivalent complex overlay block.
	 * @param entry The flat block's index entry.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @return The new index entry as an {@code int}.
	 */
	private int convertFlatToComplex(int entry, int geoX, int geoY)
	{
		final short currentHeight = (short) entry;
		final short combinedData = (short) (((currentHeight << 1) & 0xffff) | NSWE_ALL);
		final short[] cells = new short[BLOCK_CELLS];
		Arrays.fill(cells, combinedData);
		final int newEntry = (TYPE_OVERLAY << 30) | _overlay.size();
		_overlay.add(cells);
		_index[blockIndex(geoX, geoY)] = newEntry;
		return newEntry;
	}
	
	/**
	 * Retrieves the nearest Z at the given coordinate.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @param worldZ The world Z coordinate.
	 * @return The nearest Z as an {@code int}.
	 */
	@Override
	public int getNearestZ(int geoX, int geoY, int worldZ)
	{
		final int entry = _index[blockIndex(geoX, geoY)];
		if ((entry >>> 30) == TYPE_FLAT)
		{
			return (short) entry;
		}
		
		if ((entry >>> 30) == TYPE_MULTILAYER)
		{
			final int nearestZ = decodeHeight(nearestCellData(geoX, geoY, worldZ));
			
			// If the nearest layer is more than 1000 units above current position (likely a different floor/level).
			if ((nearestZ - worldZ) > 1000)
			{
				// Find the next lower valid height instead to prevent unreachable positions.
				return getNextLowerZ(geoX, geoY, worldZ);
			}
			
			return nearestZ;
		}
		
		return decodeHeight(nearestCellData(geoX, geoY, worldZ));
	}
	
	/**
	 * Retrieves the next lower Z at the given coordinate.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @param worldZ The world Z coordinate.
	 * @return The resulting Z as an {@code int}.
	 */
	@Override
	public int getNextLowerZ(int geoX, int geoY, int worldZ)
	{
		final int entry = _index[blockIndex(geoX, geoY)];
		if ((entry >>> 30) == TYPE_MULTILAYER)
		{
			final int offset = entry & OFFSET_MASK;
			final byte[] chunk = _data[offset >>> CHUNK_SHIFT];
			final int start = multiCellPos(chunk, offset & POS_MASK, cellIndex(geoX, geoY));
			final int end = start + 1 + (chunk[start] * 2);
			int lowerZ = Integer.MIN_VALUE;
			for (int p = start + 1; p < end; p += 2)
			{
				final int layerZ = decodeHeight(leShort(chunk, p));
				if (layerZ == worldZ)
				{
					// Exact z.
					return layerZ;
				}
				
				if ((layerZ < worldZ) && (layerZ > lowerZ))
				{
					lowerZ = layerZ;
				}
			}
			
			return lowerZ == Integer.MIN_VALUE ? worldZ : lowerZ;
		}
		
		final int cellHeight = (entry >>> 30) == TYPE_FLAT ? (short) entry : decodeHeight(nearestCellData(geoX, geoY, worldZ));
		return cellHeight <= worldZ ? cellHeight : worldZ;
	}
	
	/**
	 * Retrieves the next higher Z at the given coordinate.
	 * @param geoX The geo X coordinate.
	 * @param geoY The geo Y coordinate.
	 * @param worldZ The world Z coordinate.
	 * @return The resulting Z as an {@code int}.
	 */
	@Override
	public int getNextHigherZ(int geoX, int geoY, int worldZ)
	{
		final int entry = _index[blockIndex(geoX, geoY)];
		if ((entry >>> 30) == TYPE_MULTILAYER)
		{
			final int offset = entry & OFFSET_MASK;
			final byte[] chunk = _data[offset >>> CHUNK_SHIFT];
			final int start = multiCellPos(chunk, offset & POS_MASK, cellIndex(geoX, geoY));
			final int end = start + 1 + (chunk[start] * 2);
			int higherZ = Integer.MAX_VALUE;
			for (int p = start + 1; p < end; p += 2)
			{
				final int layerZ = decodeHeight(leShort(chunk, p));
				if (layerZ == worldZ)
				{
					// Exact z.
					return layerZ;
				}
				
				if ((layerZ > worldZ) && (layerZ < higherZ))
				{
					higherZ = layerZ;
				}
			}
			
			return higherZ == Integer.MAX_VALUE ? worldZ : higherZ;
		}
		
		final int cellHeight = (entry >>> 30) == TYPE_FLAT ? (short) entry : decodeHeight(nearestCellData(geoX, geoY, worldZ));
		return cellHeight >= worldZ ? cellHeight : worldZ;
	}
	
	/**
	 * Indicates whether this region holds geodata; always {@code true} for a loaded region.
	 * @return {@code true} always.
	 */
	@Override
	public boolean hasGeo()
	{
		return true;
	}
	
	/**
	 * Saves this region's geodata under {@code GeoEngineConfig.GEOEDIT_PATH}, overwriting any existing file.<br>
	 * Unmodified regions are written byte identical to their source file; converted flat blocks are written as complex blocks.
	 * @param fileName The target file name.
	 * @return {@code true} on success, {@code false} on I/O error.
	 */
	@Override
	public boolean saveToFile(String fileName)
	{
		final Path filePath = new File(GeoEngineConfig.GEOEDIT_PATH + File.separator + fileName).toPath();
		if (Files.exists(filePath))
		{
			try
			{
				Files.delete(filePath);
			}
			catch (IOException e)
			{
				return false;
			}
		}
		
		try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath.toFile())))
		{
			for (int blockOffset = 0; blockOffset < REGION_BLOCKS; blockOffset++)
			{
				final int entry = _index[blockOffset];
				switch (entry >>> 30)
				{
					case TYPE_FLAT:
					{
						final ByteBuffer buffer = ByteBuffer.allocate(3);
						buffer.put((byte) TYPE_FLAT);
						buffer.putShort(Short.reverseBytes((short) entry));
						bos.write(buffer.array());
						break;
					}
					case TYPE_COMPLEX:
					{
						final ByteBuffer buffer = ByteBuffer.allocate(1 + (BLOCK_CELLS * 2));
						buffer.put((byte) TYPE_COMPLEX);
						for (int cell = 0; cell < BLOCK_CELLS; cell++)
						{
							buffer.putShort(Short.reverseBytes(complexCellData(entry, cell)));
						}
						
						bos.write(buffer.array());
						break;
					}
					case TYPE_MULTILAYER:
					{
						final int offset = entry & OFFSET_MASK;
						final byte[] chunk = _data[offset >>> CHUNK_SHIFT];
						final int start = offset & POS_MASK;
						final int end = multiCellPos(chunk, start, BLOCK_CELLS - 1);
						final int size = (end + 1 + (chunk[end] * 2)) - start;
						bos.write(TYPE_MULTILAYER);
						bos.write(chunk, start, size);
						break;
					}
					default:
					{
						final short[] cells = _overlay.get(entry & OFFSET_MASK);
						final ByteBuffer buffer = ByteBuffer.allocate(1 + (BLOCK_CELLS * 2));
						buffer.put((byte) TYPE_COMPLEX);
						for (short info : cells)
						{
							buffer.putShort(Short.reverseBytes(info));
						}
						
						bos.write(buffer.array());
						break;
					}
				}
			}
		}
		catch (IOException e)
		{
			return false;
		}
		
		return true;
	}
}
