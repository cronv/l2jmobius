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

import java.util.BitSet;

/**
 * The sampled region grid: per-cell layer heights, NSWE bits and wall markers.
 * @author Mobius
 */
public class GeoGrid
{
	public final short[] layerZ = new short[GeoConfig.CELLS * GeoConfig.CELLS * GeoConfig.MAX_LAYERS]; // descending per cell
	public final byte[] layerZFrac = new byte[GeoConfig.CELLS * GeoConfig.CELLS * GeoConfig.MAX_LAYERS]; // real sampled height minus stored quantized height (-7..7): climb rules run on REAL heights, the file stores quantized
	public final byte[] layerCount = new byte[GeoConfig.CELLS * GeoConfig.CELLS];
	public final byte[] layerNswe = new byte[GeoConfig.CELLS * GeoConfig.CELLS * GeoConfig.MAX_LAYERS];
	public final BitSet noFloor = new BitSet(GeoConfig.CELLS * GeoConfig.CELLS); // wall/tower body: top kept, forced all-blocked (0000)
	
	public int count(int cx, int cy)
	{
		return layerCount[(cx * GeoConfig.CELLS) + cy];
	}
	
	public short z(int cx, int cy, int layer)
	{
		return layerZ[(((cx * GeoConfig.CELLS) + cy) * GeoConfig.MAX_LAYERS) + layer];
	}
	
	public double realZ(int cx, int cy, int layer)
	{
		final int idx = (((cx * GeoConfig.CELLS) + cy) * GeoConfig.MAX_LAYERS) + layer;
		return layerZ[idx] + layerZFrac[idx];
	}
	
	public byte nswe(int cx, int cy, int layer)
	{
		return layerNswe[(((cx * GeoConfig.CELLS) + cy) * GeoConfig.MAX_LAYERS) + layer];
	}
}
