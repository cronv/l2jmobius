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
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Terrain heightmap model (G16 texture) with quad visibility holes, edge turns and triangle height sampling.
 * @author Mobius
 */
public class Terrain
{
	public float[] location; // TerrainInfo world position (terrain center)
	public float[] scale; // world units per texel (x, y), height scale (z)
	public int uSize;
	public int vSize;
	int[] heightmap; // G16 values, row-major [y * uSize + x]
	public BitSet quadVisible; // one bit per heightmap quad, row-major; null = all visible
	public BitSet edgeTurn; // one bit per quad, row-major: which way the quad's two triangles are split; null = all split the same way
	private static final Map<String, Edge> EDGES = new HashMap<>(); // every tile's first row and column, kept so a tile is read once however many neighbours ask for it: on a world build each tile is wanted four times, once as itself and once by each of the three tiles whose edge it closes.
	private double[] heights; // world heights, row-major [y * (uSize + 1) + x]: one row and column wider than the tile's own texels, since a tile's texels span 255 of its 256 steps and the last step belongs to the tile beyond it.
	
	public boolean isQuadVisible(int qx, int qy)
	{
		return (quadVisible == null) || quadVisible.get((qy * uSize) + qx);
	}
	
	public double worldZ(int x, int y)
	{
		return heights[(y * (uSize + 1)) + x];
	}
	
	/**
	 * This tile's own height at one of its own texels, in world units.
	 * @param x the texel column, 0 to uSize - 1
	 * @param y the texel row, 0 to vSize - 1
	 * @return the world height, with the geodata Z offset already in it
	 */
	private double ownZ(int x, int y)
	{
		return location[2] + (((heightmap[(y * uSize) + x] - 32768) * scale[2]) / 256.0) + GeoConfig.GEODATA_Z_OFFSET;
	}
	
	public double texelToWorldX(int x)
	{
		return location[0] + ((x - (uSize / 2)) * scale[0]);
	}
	
	public double texelToWorldY(int y)
	{
		return location[1] + ((y - (vSize / 2)) * scale[1]);
	}
	
	public boolean contains(double worldX, double worldY)
	{
		return (worldX >= texelToWorldX(0)) && (worldX <= texelToWorldX(uSize - 1)) && (worldY >= texelToWorldY(0)) && (worldY <= texelToWorldY(vSize - 1));
	}
	
	/**
	 * Bilinear interpolated height at world coordinates.
	 * @param worldX the world X coordinate
	 * @param worldY the world Y coordinate
	 * @return the terrain height
	 */
	public double heightAt(double worldX, double worldY)
	{
		final double fx = ((worldX - location[0]) / scale[0]) + (uSize / 2);
		final double fy = ((worldY - location[1]) / scale[1]) + (vSize / 2);
		final int x0 = Math.max(0, Math.min(uSize - 1, (int) Math.floor(fx)));
		final int y0 = Math.max(0, Math.min(vSize - 1, (int) Math.floor(fy)));
		final double tx = Math.max(0, Math.min(1, fx - x0)); // clamped: outside the grid this reads the edge line rather than carrying the last slope on past it.
		final double ty = Math.max(0, Math.min(1, fy - y0));
		final double z00 = worldZ(x0, y0);
		final double z10 = worldZ(x0 + 1, y0);
		final double z01 = worldZ(x0, y0 + 1);
		final double z11 = worldZ(x0 + 1, y0 + 1);
		
		// A quad is two triangles, not a bilinear sheet, and EdgeTurnBitmap says which way it is cut. Read bilinear, the middle of a quad sits at the mean of all four corners instead of the mean of the two on the cut, which on a saddle is half the spread between the corner pairs out, in the same range as MAX_CLIMB.
		if ((edgeTurn != null) && edgeTurn.get((y0 * uSize) + x0))
		{
			// Cut from z10 to z01.
			if ((tx + ty) <= 1)
			{
				return z00 + ((z10 - z00) * tx) + ((z01 - z00) * ty);
			}
			
			return ((z10 + z01) - z11) + ((z11 - z01) * tx) + ((z11 - z10) * ty);
		}
		
		// Cut from z00 to z11.
		if (tx >= ty)
		{
			return z00 + ((z10 - z00) * tx) + ((z11 - z10) * ty);
		}
		
		return z00 + ((z11 - z01) * tx) + ((z01 - z00) * ty);
	}
	
	public static Terrain load(Path clientRoot, String map) throws IOException
	{
		return load(clientRoot, map, true);
	}
	
	/**
	 * Loads terrain data from a client map.
	 * @param clientRoot the client root directory
	 * @param map the map tile name
	 * @param verbose whether terrain metadata is written to standard output
	 * @return the loaded terrain
	 * @throws IOException when the map or heightmap cannot be loaded
	 */
	public static Terrain load(Path clientRoot, String map, boolean verbose) throws IOException
	{
		final Terrain t = loadOne(clientRoot, map, verbose);
		remember(clientRoot, map, t);
		t.grow(clientRoot, map, verbose);
		return t;
	}
	
	/**
	 * Fills the world height grid, which is one row and column wider than the tile's own texels: a tile's texels reach 255 of its 256 steps, so the last step across and down belongs to the tile beyond it. Every tile gets this grid as it loads, with its own edge repeated along that last line, so a tile read on its own is still whole.
	 */
	private void fill()
	{
		final int wide = uSize + 1;
		heights = new double[wide * (vSize + 1)];
		for (int y = 0; y <= vSize; y++)
		{
			for (int x = 0; x <= uSize; x++)
			{
				heights[(y * wide) + x] = ownZ(Math.min(x, uSize - 1), Math.min(y, vSize - 1));
			}
		}
	}
	
	/**
	 * Replaces the edge line with the neighbouring tiles' own first line, where there are neighbours.
	 * <p>
	 * A tile's texels reach 255 of its 256 steps, so the last step across and down belongs to the tile beyond it, and that is where the client takes it from. It cannot be copied as it stands: neighbouring tiles keep their own ground and their own height scale, and this client's tiles differ by as much as 337 units in ground (17_25 sits at 160.65, 18_26 at -160.65), so the sample is turned into world units through the neighbour's own frame before it is used.
	 * <p>
	 * Where there is no neighbour to ask, the edge {@link #fill} already repeated stands, which is a clamp rather than the slope of the last two texels carried on past the data.
	 * @param clientRoot the client folder
	 * @param map the tile's name
	 * @param verbose whether terrain metadata is written to standard output
	 */
	private void grow(Path clientRoot, String map, boolean verbose)
	{
		final int wide = uSize + 1;
		final Edge east = beside(clientRoot, map, 1, 0, verbose);
		final Edge south = beside(clientRoot, map, 0, 1, verbose);
		final Edge corner = beside(clientRoot, map, 1, 1, verbose);
		for (int y = 0; (east != null) && (y < vSize); y++)
		{
			heights[(y * wide) + uSize] = east.firstColumn[y];
		}
		
		for (int x = 0; (south != null) && (x < uSize); x++)
		{
			heights[(vSize * wide) + x] = south.firstRow[x];
		}
		
		if (corner != null)
		{
			heights[(vSize * wide) + uSize] = corner.firstRow[0];
		}
		
		if (verbose)
		{
			System.out.println("Edge line taken from " + (east != null ? "east" : "-") + "/" + (south != null ? "south" : "-") + "/" + (corner != null ? "corner" : "-") + " (a missing neighbour repeats this tile's own edge).");
		}
	}
	
	/**
	 * The tile one step across or down, loaded for its edge alone, or null where there is none. A neighbour whose texels do not line up with this tile's is refused rather than misread.
	 * @param clientRoot the client folder
	 * @param map this tile's name
	 * @param acrossBy how many tiles east
	 * @param downBy how many tiles south
	 * @param verbose whether terrain metadata is written to standard output
	 * @return the neighbouring tile, or null
	 */
	private Edge beside(Path clientRoot, String map, int acrossBy, int downBy, boolean verbose)
	{
		final String[] parts = map.split("_");
		if (parts.length != 2)
		{
			return null; // Dungeons and lobbies are not part of the world grid, so they have no neighbours.
		}
		
		final String wanted;
		try
		{
			wanted = (Integer.parseInt(parts[0]) + acrossBy) + "_" + (Integer.parseInt(parts[1]) + downBy);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
		
		final Edge edge = edgeOf(clientRoot, wanted);
		if (edge == null)
		{
			return null; // Ocean tiles and the edge of the world carry no terrain to borrow.
		}
		
		if ((edge.uSize != uSize) || (edge.vSize != vSize) || (edge.acrossScale != scale[0]) || (edge.downScale != scale[1]))
		{
			if (verbose)
			{
				System.out.println("Neighbour " + wanted + " does not line up with " + map + ", so its edge is left alone.");
			}
			return null;
		}
		
		return edge;
	}
	
	/**
	 * Keeps a tile's edge as the tile itself is loaded, so the tiles that borrow it never read it again.
	 * @param clientRoot the client folder
	 * @param map the tile's name
	 * @param t the tile
	 */
	private static synchronized void remember(Path clientRoot, String map, Terrain t)
	{
		EDGES.putIfAbsent(clientRoot + "|" + map, new Edge(t));
	}
	
	/**
	 * One tile's first row and column, read once and kept.
	 * @param clientRoot the client folder
	 * @param map the tile's name
	 * @return the tile's edge, or null where there is no such tile
	 */
	private static synchronized Edge edgeOf(Path clientRoot, String map)
	{
		final String key = clientRoot + "|" + map;
		if (EDGES.containsKey(key))
		{
			return EDGES.get(key); // A tile that is not there is remembered as missing rather than looked for again.
		}
		
		Edge edge;
		try
		{
			edge = new Edge(loadOne(clientRoot, map, false));
		}
		catch (Exception e)
		{
			edge = null;
		}
		
		EDGES.put(key, edge);
		return edge;
	}
	
	/**
	 * The line of a tile its neighbours borrow: the heights along its first row and its first column, in world units.
	 */
	private static final class Edge
	{
		final int uSize;
		final int vSize;
		final float acrossScale;
		final float downScale;
		final double[] firstRow;
		final double[] firstColumn;
		
		Edge(Terrain t)
		{
			uSize = t.uSize;
			vSize = t.vSize;
			acrossScale = t.scale[0];
			downScale = t.scale[1];
			firstRow = new double[uSize];
			firstColumn = new double[vSize];
			for (int x = 0; x < uSize; x++)
			{
				firstRow[x] = t.ownZ(x, 0);
			}
			
			for (int y = 0; y < vSize; y++)
			{
				firstColumn[y] = t.ownZ(0, y);
			}
		}
	}
	
	private static Terrain loadOne(Path clientRoot, String map, boolean chatty) throws IOException
	{
		final UnrealPackage unr = new UnrealPackage(UnrealCrypt.decrypt(clientRoot.resolve("maps").resolve(map + ".unr")));
		
		Export terrainInfo = null;
		for (Export ex : unr.exports)
		{
			if (unr.objName(ex.classRef).equals("TerrainInfo"))
			{
				terrainInfo = ex;
				break;
			}
		}
		
		if (terrainInfo == null)
		{
			throw new IOException("No TerrainInfo actor in " + map + ".unr (dungeon-only map?)");
		}
		
		final Map<String, Object> props = UnrealPackage.readProps(unr, terrainInfo);
		if (chatty)
		{
			System.out.println("TerrainInfo '" + unr.names[terrainInfo.nameIdx] + "' properties:");
			for (Map.Entry<String, Object> e : props.entrySet())
			{
				final Object v = e.getValue();
				System.out.println("  " + e.getKey() + " = " + (v instanceof float[] f ? "(" + f[0] + ", " + f[1] + ", " + f[2] + ")" : v instanceof byte[] raw ? "array[" + raw.length + " bytes]" : String.valueOf(v)));
			}
		}
		
		final Terrain t = new Terrain();
		
		t.location = (float[]) props.get("Location");
		if (t.location == null)
		{
			// The client positions terrain from MapX/MapY when the Location property is absent.
			// World tile origin = ((MapX - 20) * 32768, (MapY - 18) * 32768), Location is the tile center.
			// Z = 160.65: the constant TerrainInfo Location.Z observed on all checked maps.
			final Object mapXProp = props.get("MapX");
			final Object mapYProp = props.get("MapY");
			final int mapX = mapXProp == null ? Integer.parseInt(map.split("_")[0]) : (Integer) mapXProp;
			final int mapY = mapYProp == null ? Integer.parseInt(map.split("_")[1]) : (Integer) mapYProp;
			t.location = new float[]
			{
				((mapX - 20) * 32768) + 16384,
				((mapY - 18) * 32768) + 16384,
				160.65f
			};
		}
		
		t.scale = (float[]) props.getOrDefault("TerrainScale", new float[]
		{
			64,
			64,
			64
		});
		
		// Resolve the referenced heightmap first. Some client maps omit TerrainMap entirely; only then use the map filename as a guarded base-terrain fallback.
		final Object terrainMapProperty = props.get("TerrainMap");
		final boolean fallbackTerrainMap = terrainMapProperty == null;
		final String texName;
		final UnrealPackage texPkg;
		if (terrainMapProperty != null)
		{
			if (!(terrainMapProperty instanceof Integer))
			{
				throw new IOException("Unsupported TerrainMap property in " + map + ".unr: " + terrainMapProperty);
			}
			final int mapRef = ((Integer) terrainMapProperty).intValue();
			texName = unr.objName(mapRef);
			if (mapRef < 0)
			{
				texPkg = new UnrealPackage(UnrealCrypt.decrypt(clientRoot.resolve("textures").resolve(unr.rootPackage(mapRef) + ".utx")));
			}
			else
			{
				texPkg = unr; // embedded in the map package itself
			}
		}
		else
		{
			final String[] mapParts = map.split("_");
			if (mapParts.length < 2)
			{
				throw new IOException("TerrainMap is absent and the map name does not contain an XX_YY region: " + map);
			}
			final String region = mapParts[0] + "_" + mapParts[1];
			texName = region;
			final Path fallbackPackage = clientRoot.resolve("textures").resolve("T_" + region + ".utx");
			if (!Files.isRegularFile(fallbackPackage))
			{
				throw new IOException("TerrainMap is absent in " + map + ".unr and fallback terrain package T_" + region + ".utx was not found");
			}
			texPkg = new UnrealPackage(UnrealCrypt.decrypt(fallbackPackage));
		}
		
		Export texExport = null;
		for (Export ex : texPkg.exports)
		{
			if (!texPkg.names[ex.nameIdx].equalsIgnoreCase(texName) || !texPkg.objName(ex.classRef).equals("Texture"))
			{
				continue;
			}
			if (fallbackTerrainMap)
			{
				if (ex.outerRef <= 0)
				{
					continue;
				}
				if (!texPkg.names[texPkg.exports[ex.outerRef - 1].nameIdx].equalsIgnoreCase("Height"))
				{
					continue;
				}
			}
			texExport = ex;
			break;
		}
		
		if (texExport == null)
		{
			throw new IOException("Heightmap texture '" + texName + "' not found" + (fallbackTerrainMap ? " in Height group" : ""));
		}
		
		final Map<String, Object> texProps = UnrealPackage.readProps(texPkg, texExport);
		final int format = (Integer) texProps.getOrDefault("Format", 0);
		t.uSize = (Integer) texProps.getOrDefault("USize", 256);
		t.vSize = (Integer) texProps.getOrDefault("VSize", 256);
		if (fallbackTerrainMap && (format != 10))
		{
			throw new IOException("Fallback heightmap '" + texName + "' is not G16 (format=" + format + ")");
		}
		final long heightmapBytes = (long) t.uSize * t.vSize * 2;
		if (fallbackTerrainMap && ((t.uSize < 2) || (t.vSize < 2) || (heightmapBytes > Integer.MAX_VALUE)))
		{
			throw new IOException("Fallback heightmap '" + texName + "' has invalid dimensions " + t.uSize + "x" + t.vSize);
		}
		if (chatty)
		{
			System.out.println("Heightmap '" + texName + "': format=" + format + (format == 10 ? " (G16)" : " (NOT G16!)") + " size=" + t.uSize + "x" + t.vSize + (fallbackTerrainMap ? " [filename fallback]" : ""));
		}
		
		try
		{
			t.heightmap = UnrealPackage.readMip0(texPkg.buf, (int) heightmapBytes); // G16 = two bytes per texel; the size doubles as the signature that locates the mip header.
		}
		catch (IllegalStateException e)
		{
			if (!fallbackTerrainMap)
			{
				throw e;
			}
			throw new IOException("Fallback heightmap '" + texName + "' does not contain a valid top mip", e);
		}
		if (t.heightmap.length != (t.uSize * t.vSize))
		{
			throw new IOException("Heightmap size mismatch: " + t.heightmap.length + " != " + (t.uSize * t.vSize));
		}
		
		// Edge turns: the same shape of bit field, one bit per quad, saying which way the quad's two triangles are cut.
		if (props.get("EdgeTurnBitmap") instanceof byte[] turns)
		{
			final ByteBuffer tb = ByteBuffer.wrap(turns).order(ByteOrder.LITTLE_ENDIAN);
			UnrealPackage.readIndex(tb); // element count (int32 words)
			final byte[] bits = new byte[tb.remaining()];
			tb.get(bits);
			t.edgeTurn = BitSet.valueOf(bits);
		}
		
		// Quad visibility: TArray<int32> bit field, one bit per quad, bit clear = terrain hole (cave/dungeon opening).
		if (props.get("QuadVisibilityBitmap") instanceof byte[] raw)
		{
			final ByteBuffer qb = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
			UnrealPackage.readIndex(qb); // element count (int32 words)
			final byte[] bits = new byte[qb.remaining()];
			qb.get(bits);
			t.quadVisible = BitSet.valueOf(bits);
			final int holes = (t.uSize * t.vSize) - t.quadVisible.cardinality();
			if (chatty && (holes > 0))
			{
				System.out.println("Terrain holes (invisible quads): " + holes);
			}
		}
		
		t.fill();
		return t;
	}
}
