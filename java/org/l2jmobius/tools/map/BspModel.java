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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * BSP level geometry: nodes, surfaces, points and the polygon flags that mark passable or hull-only faces.
 * @author Mobius
 */
public class BspModel
{
	public static final int NF_PASSABLE = 0x01; // NF_NotCsg
	public static final int PF_PASSABLE = 0x47 | 0x80; // PF_Masked | PF_Translucent | PF_Invisible | PF_Modulated | PF_FakeBackdrop (sky)
	public static final int PF_HULL = 0x40A000; // Zone/collision-hull sheets: duplicate real floors 1-2 units above, or form buried cavity-base planes (-3179/-3243/-16355 on 19_21). Never real floors.
	
	float[][] points;
	int[] vertPointIdx; // vert pool -> point index
	List<BspNode> nodes = new ArrayList<>();
	int[] surfaceFlags;
	
	/**
	 * Parses one Model export (level BSP or a volume/brush model).
	 * @param pkg the package containing the model
	 * @param model the Model export entry
	 * @return the parsed BSP model
	 */
	public static BspModel parseModel(UnrealPackage pkg, Export model)
	{
		final int licenseeVersion = (pkg.buf.getInt(4) >> 16) & 0xFFFF;
		final ByteBuffer b = pkg.buf;
		UnrealPackage.readProps(pkg, model);
		b.position(b.position() + 25 + 16); // Primitive: bounding box + sphere.
		
		final int vectorCount = UnrealPackage.readIndex(b); // vectors: unused for collision
		b.position(b.position() + (vectorCount * 12));
		
		final BspModel bsp = new BspModel();
		final int pointCount = UnrealPackage.readIndex(b);
		bsp.points = new float[pointCount][3];
		for (int i = 0; i < pointCount; i++)
		{
			bsp.points[i][0] = b.getFloat();
			bsp.points[i][1] = b.getFloat();
			bsp.points[i][2] = b.getFloat();
		}
		
		final int nodeCount = UnrealPackage.readIndex(b);
		for (int i = 0; i < nodeCount; i++)
		{
			final BspNode node = new BspNode();
			b.position(b.position() + 16 + 8); // plane, zone mask
			node.flags = b.get() & 0xFF;
			node.vertexPool = UnrealPackage.readIndex(b);
			node.surface = UnrealPackage.readIndex(b);
			UnrealPackage.readIndex(b); // back
			UnrealPackage.readIndex(b); // front
			UnrealPackage.readIndex(b); // plane
			UnrealPackage.readIndex(b); // collision bound
			UnrealPackage.readIndex(b); // render bound
			b.position(b.position() + 12 + 4 + 8 + 8); // unknown point, unknown id, connectivity, visibility
			UnrealPackage.readIndex(b); // zone 0
			UnrealPackage.readIndex(b); // zone 1
			node.vertexCount = b.get() & 0xFF;
			b.position(b.position() + 4 + 4 + 12); // leaf 0, leaf 1, projected texture pointers
			bsp.nodes.add(node);
		}
		
		final int surfaceCount = UnrealPackage.readIndex(b);
		bsp.surfaceFlags = new int[surfaceCount];
		for (int i = 0; i < surfaceCount; i++)
		{
			UnrealPackage.readIndex(b); // material
			bsp.surfaceFlags[i] = b.getInt();
			UnrealPackage.readIndex(b); // base
			UnrealPackage.readIndex(b); // normal
			UnrealPackage.readIndex(b); // u
			UnrealPackage.readIndex(b); // v
			UnrealPackage.readIndex(b); // brush polygon
			UnrealPackage.readIndex(b); // actor
			b.position(b.position() + 16); // plane
			b.getInt(); // nodes[0]
			if (licenseeVersion > 20)
			{
				b.getInt(); // nodes[1]
			}
		}
		
		final int vertCount = UnrealPackage.readIndex(b);
		bsp.vertPointIdx = new int[vertCount];
		for (int i = 0; i < vertCount; i++)
		{
			bsp.vertPointIdx[i] = UnrealPackage.readIndex(b);
			UnrealPackage.readIndex(b); // side
		}
		
		return bsp;
	}
	
	public static void bspSurfaces(Path clientRoot, String map, double px, double py) throws IOException
	{
		final UnrealPackage unr = new UnrealPackage(UnrealCrypt.decrypt(clientRoot.resolve("maps").resolve(map + ".unr")));
		Export model = null;
		for (Export ex : unr.exports)
		{
			if (unr.objName(ex.classRef).equals("Model") && ((model == null) || (ex.size > model.size)))
			{
				model = ex;
			}
		}
		
		if (model == null)
		{
			System.out.println("No BSP model in " + map + ".unr");
			return;
		}
		
		final BspModel bsp = parseModel(unr, model);
		
		System.out.println("BSP: " + bsp.points.length + " points, " + bsp.nodes.size() + " nodes, " + bsp.surfaceFlags.length + " surfaces");
		
		final List<double[]> hits = new ArrayList<>();
		for (BspNode node : bsp.nodes)
		{
			if (((node.flags & NF_PASSABLE) != 0) || (node.vertexCount < 3))
			{
				continue;
			}
			
			if ((bsp.surfaceFlags[node.surface] & PF_PASSABLE) != 0)
			{
				continue;
			}
			
			final float[] v0 = bsp.points[bsp.vertPointIdx[node.vertexPool]];
			final double[] p0 =
			{
				v0[0],
				v0[1],
				v0[2]
			};
			final double[] p1 = new double[3];
			final double[] p2 = new double[3];
			for (int i = 2; i < node.vertexCount; i++)
			{
				final float[] v1 = bsp.points[bsp.vertPointIdx[(node.vertexPool + i) - 1]];
				final float[] v2 = bsp.points[bsp.vertPointIdx[node.vertexPool + i]];
				p1[0] = v1[0];
				p1[1] = v1[1];
				p1[2] = v1[2];
				p2[0] = v2[0];
				p2[1] = v2[1];
				p2[2] = v2[2];
				final double d1 = StaticMeshes.sign(px, py, p0, p1);
				final double d2 = StaticMeshes.sign(px, py, p1, p2);
				final double d3 = StaticMeshes.sign(px, py, p2, p0);
				if (((d1 < 0) || (d2 < 0) || (d3 < 0)) && ((d1 > 0) || (d2 > 0) || (d3 > 0)))
				{
					continue;
				}
				
				final double area = ((p1[0] - p0[0]) * (p2[1] - p0[1])) - ((p2[0] - p0[0]) * (p1[1] - p0[1]));
				if (area == 0)
				{
					continue;
				}
				
				final double w0 = (((p1[0] - px) * (p2[1] - py)) - ((p2[0] - px) * (p1[1] - py))) / area;
				final double w1 = (((p2[0] - px) * (p0[1] - py)) - ((p0[0] - px) * (p2[1] - py))) / area;
				hits.add(new double[]
				{
					(w0 * p0[2]) + (w1 * p1[2]) + ((1 - w0 - w1) * p2[2]),
					bsp.surfaceFlags[node.surface]
				});
			}
		}
		
		if (hits.isEmpty())
		{
			System.out.println("No BSP surfaces above/below this point.");
			return;
		}
		
		hits.sort((a, c) -> Double.compare(c[0], a[0]));
		System.out.println("BSP surfaces at this point (top to bottom):");
		for (int i = 0; i < Math.min(hits.size(), 12); i++)
		{
			System.out.printf("  z=%.1f  polyFlags=0x%x%n", hits.get(i)[0] + GeoConfig.GEODATA_Z_OFFSET, (long) hits.get(i)[1]);
		}
	}
}
