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
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Static mesh loading: actors, collision sections, vertex/index streams, world transforms.
 * @author Mobius
 */
public class StaticMeshes
{
	public static List<MeshActor> loadMeshActors(Path clientRoot, String map) throws IOException
	{
		final UnrealPackage unr = new UnrealPackage(UnrealCrypt.decrypt(clientRoot.resolve("maps").resolve(map + ".unr")));
		final List<MeshActor> actors = new ArrayList<>();
		final boolean debug = System.getenv("MR_DEBUG") != null;
		int stacked = 0;
		int located = 0;
		int dumped = 0;
		int deleted = 0;
		for (Export ex : unr.exports)
		{
			// Movers (doors/gates) are deliberately NOT baked into geodata: their cells must stay walkable, the server blocks them dynamically (DoorData) while the door is closed.
			if (!unr.objName(ex.classRef).endsWith("StaticMeshActor"))
			{
				continue;
			}
			
			if ((ex.flags & UnrealPackage.RF_HAS_STACK) != 0)
			{
				stacked++;
			}
			
			final Map<String, Object> props = UnrealPackage.readProps(unr, ex);
			if (props.get("Location") instanceof float[])
			{
				located++;
			}
			
			// A DELETED actor is not in the world. The map file still carries it, but the client drops it on load, so baking it invents collision no player can see.
			// Newer clients leave Giran square's old balustrade behind exactly this way: Giran_V_Plaza_Wall02 - 2781 long, 395 tall, marked bDeleteMe/bPendingDelete - lying straight across the plaza.
			// It baked as a wall, and because its face stops 15 units above the pavement the headroom test then purged the floor beneath it, turning whole rows of the square into a one-cell trench nobody could enter.
			if (Boolean.TRUE.equals(props.getOrDefault("bDeleteMe", false)) || Boolean.TRUE.equals(props.getOrDefault("bPendingDelete", false)))
			{
				deleted++;
				continue;
			}
			
			// Check actor collision flags, skip if not solid.
			boolean collideActors = !Boolean.FALSE.equals(props.getOrDefault("bCollideActors", true));
			boolean blockActors = !Boolean.FALSE.equals(props.getOrDefault("bBlockActors", true));
			boolean blockPlayers = !Boolean.FALSE.equals(props.getOrDefault("bBlockPlayers", true));
			if ((Boolean.TRUE.equals(props.getOrDefault("bHidden", false)) && !blockPlayers) || !collideActors || !blockActors || !blockPlayers)
			{
				continue;
			}
			
			if (debug && (dumped++ < 3))
			{
				System.out.println("  actor " + unr.names[ex.nameIdx] + " flags=0x" + Integer.toHexString(ex.flags) + " props=" + props.keySet());
				final StringBuilder sb = new StringBuilder("  raw @" + ex.offset + " size=" + ex.size + ":");
				for (int i = 0; i < Math.min(ex.size, 72); i++)
				{
					sb.append(String.format(" %02x", unr.buf.get(ex.offset + i)));
				}
				
				System.out.println(sb);
			}
			
			if (!(props.get("StaticMesh") instanceof Integer ref) || (ref >= 0))
			{
				continue;
			}
			
			final MeshActor actor = new MeshActor();
			actor.meshPackage = unr.rootPackage(ref);
			actor.meshName = unr.objName(ref);
			if (props.get("Location") instanceof float[] l)
			{
				actor.location = l;
			}
			
			if (props.get("Rotation") instanceof int[] r)
			{
				actor.rotation = r;
			}
			
			if (props.get("DrawScale3D") instanceof float[] s)
			{
				actor.scale3d = s;
			}
			
			if (props.get("DrawScale") instanceof Float f)
			{
				actor.drawScale = f;
			}
			
			actors.add(actor);
		}
		
		if (debug)
		{
			System.out.println("  actors=" + actors.size() + " withStack=" + stacked + " withLocation=" + located);
		}
		
		if (deleted > 0)
		{
			System.out.println("Skipped " + deleted + " actors the client deletes on load (bDeleteMe/bPendingDelete).");
		}
		
		return actors;
	}
	
	public static int readIndexFromBytes(byte[] raw, int[] pos)
	{
		int c = raw[pos[0]++] & 0xFF;
		final boolean neg = (c & 0x80) != 0;
		int out = c & 0x3F;
		if ((c & 0x40) != 0)
		{
			for (int shift = 6;; shift += 7)
			{
				c = raw[pos[0]++] & 0xFF;
				out |= (c & 0x7F) << shift;
				if ((c & 0x80) == 0)
				{
					break;
				}
			}
		}
		
		return neg ? -out : out;
	}
	
	public static List<Boolean> parseMaterialsCollision(byte[] raw, UnrealPackage pkg, int sectionCount, BitSet doorSections)
	{
		List<Boolean> result = new ArrayList<>();
		int[] pos =
		{
			0
		};
		try
		{
			int countParsed = readIndexFromBytes(raw, pos);
			for (int i = 0; (i < countParsed) && (i < sectionCount); i++)
			{
				boolean enableCollision = true;
				String materialName = null;
				while (pos[0] < raw.length)
				{
					int subNameIdx = readIndexFromBytes(raw, pos);
					String subName = pkg.names[subNameIdx];
					if ("None".equals(subName) || (subNameIdx == 0))
					{
						break;
					}
					
					int subInfo = raw[pos[0]++] & 0xFF;
					int subType = subInfo & 0x0F;
					int subSizeType = (subInfo >> 4) & 0x07;
					boolean subBoolVal = (subInfo & 0x80) != 0;
					
					if (subType == 10) // StructProperty
					{
						readIndexFromBytes(raw, pos);
					}
					
					final int subSize;
					switch (subSizeType)
					{
						case 0:
						{
							subSize = 1;
							break;
						}
						case 1:
						{
							subSize = 2;
							break;
						}
						case 2:
						{
							subSize = 4;
							break;
						}
						case 3:
						{
							subSize = 12;
							break;
						}
						case 4:
						{
							subSize = 16;
							break;
						}
						case 5:
						{
							subSize = raw[pos[0]++] & 0xFF;
							break;
						}
						case 6:
						{
							subSize = (raw[pos[0]] & 0xFF) | ((raw[pos[0] + 1] & 0xFF) << 8);
							pos[0] += 2;
							break;
						}
						default:
						{
							subSize = (raw[pos[0]] & 0xFF) | ((raw[pos[0] + 1] & 0xFF) << 8) | ((raw[pos[0] + 2] & 0xFF) << 16) | ((raw[pos[0] + 3] & 0xFF) << 24);
							pos[0] += 4;
							break;
						}
					}
					
					if (((subInfo & 0x80) != 0) && (subType != 3))
					{
						readIndexFromBytes(raw, pos);
					}
					
					if (subType == 3) // BoolProperty
					{
						if ("EnableCollision".equals(subName))
						{
							enableCollision = subBoolVal;
						}
					}
					else if ((subType == 5) && "Material".equals(subName)) // ObjectProperty: the section's material reference.
					{
						final int[] refPos =
						{
							pos[0]
						};
						materialName = pkg.objName(readIndexFromBytes(raw, refPos));
						pos[0] += subSize;
					}
					else
					{
						pos[0] += subSize;
					}
				}
				
				// Town doors are baked INTO building meshes as always-open panels (Dwarven Village Dwarf_Agit sections textured DW_door01): a door-named material marks the section as never-colliding, like door-named actors.
				final boolean doorMaterial = (materialName != null) && materialName.toLowerCase().contains("door");
				if (doorMaterial && (doorSections != null))
				{
					doorSections.set(result.size());
				}
				
				result.add(enableCollision && !doorMaterial);
			}
		}
		catch (Exception e)
		{
			// An unknown or truncated Materials layout leaves the sections parsed so far and pads the rest as colliding, the safe default for geodata.
			System.out.println("  Materials parse stopped early, the remaining sections default to colliding: " + e);
		}
		
		while (result.size() < sectionCount)
		{
			result.add(true);
		}
		
		return result;
	}
	
	/**
	 * Reads UE2 StaticMesh geometry: props, bounds, surfaces, vertex/color/alpha/UV/index streams.
	 * @param pkg the package containing the mesh
	 * @param ex the StaticMesh export entry
	 * @return the collision geometry
	 */
	public static MeshGeometry loadMeshGeom(UnrealPackage pkg, Export ex)
	{
		final Map<String, Object> props = UnrealPackage.readProps(pkg, ex);
		final ByteBuffer b = pkg.buf;
		b.position(b.position() + 25 + 16); // Primitive: bounding box (6 floats + valid byte) + sphere (4 floats)
		
		final int surfaceCount = UnrealPackage.readIndex(b); // surfaces: int32 + 5 x uint16 each
		final int[][] surfaces = new int[surfaceCount][2]; // {firstIndex, triangleMax}
		
		for (int i = 0; i < surfaceCount; i++)
		{
			b.getInt(); // unknown
			surfaces[i][0] = b.getShort() & 0xFFFF; // first index
			b.getShort(); // min vertex index
			b.getShort(); // max vertex index
			b.getShort(); // triangle count
			surfaces[i][1] = b.getShort() & 0xFFFF; // triangle max
		}
		
		b.position(b.position() + 25); // second bounding box
		
		final int vertexCount = UnrealPackage.readIndex(b);
		if ((vertexCount < 0) || (vertexCount > 200_000))
		{
			throw new IllegalStateException("Implausible vertex count " + vertexCount + " in " + pkg.names[ex.nameIdx]);
		}
		
		final MeshGeometry geom = new MeshGeometry();
		geom.verts = new float[vertexCount][3];
		for (int i = 0; i < vertexCount; i++)
		{
			geom.verts[i][0] = b.getFloat();
			geom.verts[i][1] = b.getFloat();
			geom.verts[i][2] = b.getFloat();
			b.position(b.position() + 12); // normal
		}
		
		b.getInt(); // revision
		
		final boolean debug = System.getenv("MR_DEBUG2") != null;
		if (debug)
		{
			System.out.println("mesh " + pkg.names[ex.nameIdx] + " surfaces=" + surfaceCount + " verts=" + vertexCount);
		}
		
		for (int s = 0; s < 2; s++) // color + alpha streams
		{
			final int n = UnrealPackage.readIndex(b);
			if (debug)
			{
				System.out.println("  colorStream[" + s + "] n=" + n);
			}
			
			b.position(b.position() + (n * 4));
			b.getInt(); // revision
		}
		
		final int uvStreams = UnrealPackage.readIndex(b);
		if (debug)
		{
			System.out.println("  uvStreams=" + uvStreams);
		}
		
		for (int s = 0; s < uvStreams; s++)
		{
			final int n = UnrealPackage.readIndex(b);
			if (debug)
			{
				System.out.println("  uv[" + s + "] n=" + n);
			}
			
			b.position(b.position() + (n * 8));
			b.getInt(); // coordinate index
			b.getInt(); // revision
		}
		
		final int indexCount = UnrealPackage.readIndex(b);
		if (debug)
		{
			System.out.println("  indexCount=" + indexCount);
		}
		
		final int[] stream = new int[indexCount];
		for (int i = 0; i < indexCount; i++)
		{
			stream[i] = b.getShort() & 0xFFFF;
		}
		
		b.getInt(); // revision
		
		// Parse EnableCollision from Materials array if present. -DincludeNoCollision=true keeps every section (probe/testing aid).
		List<Boolean> matCollisions = null;
		final BitSet doorSections = new BitSet();
		if (!Boolean.getBoolean("includeNoCollision") && props.containsKey("Materials"))
		{
			final Object matsRaw = props.get("Materials");
			if (matsRaw instanceof byte[])
			{
				matCollisions = parseMaterialsCollision((byte[]) matsRaw, pkg, surfaceCount, doorSections);
			}
		}
		
		// Assemble triangles per surface (surfaces may not tile the stream contiguously).
		int triangles = 0;
		int visualTriangles = 0;
		for (int s = 0; s < surfaceCount; s++)
		{
			if ((matCollisions == null) || (s >= matCollisions.size()) || matCollisions.get(s))
			{
				triangles += surfaces[s][1];
			}
			else if (!doorSections.get(s))
			{
				visualTriangles += surfaces[s][1];
			}
		}
		
		geom.indices = new int[triangles * 3];
		geom.visualIndices = new int[visualTriangles * 3];
		int out = 0;
		int vout = 0;
		for (int s = 0; s < surfaceCount; s++)
		{
			if ((matCollisions != null) && (s < matCollisions.size()) && !matCollisions.get(s))
			{
				// Door-material sections still contribute their bounding box: it becomes a DOOR_BOX so the door frame around a built-in door does not bake/raster as wall and the passage stays open.
				if (doorSections.get(s))
				{
					final int[] doorSurf = surfaces[s];
					for (int i = 0; i < (doorSurf[1] * 3); i++)
					{
						final int idx = doorSurf[0] + i;
						if (idx >= stream.length)
						{
							break;
						}
						
						final float[] v = geom.verts[stream[idx]];
						if (geom.doorBounds == null)
						{
							geom.doorBounds = new float[]
							{
								v[0],
								v[1],
								v[2],
								v[0],
								v[1],
								v[2]
							};
						}
						else
						{
							geom.doorBounds[0] = Math.min(geom.doorBounds[0], v[0]);
							geom.doorBounds[1] = Math.min(geom.doorBounds[1], v[1]);
							geom.doorBounds[2] = Math.min(geom.doorBounds[2], v[2]);
							geom.doorBounds[3] = Math.max(geom.doorBounds[3], v[0]);
							geom.doorBounds[4] = Math.max(geom.doorBounds[4], v[1]);
							geom.doorBounds[5] = Math.max(geom.doorBounds[5], v[2]);
						}
					}
				}
				else
				{
					// Non-colliding decor sections (rails, trims): kept separately so bridge-class meshes can raster their wall-like visual faces as movement fences - collision data carries no rail at all there.
					final int[] vsurf = surfaces[s];
					for (int i = 0; i < vsurf[1]; i++)
					{
						final int vbase = vsurf[0] + (i * 3);
						if ((vbase + 2) >= stream.length)
						{
							break;
						}
						
						geom.visualIndices[vout++] = stream[vbase];
						geom.visualIndices[vout++] = stream[vbase + 1];
						geom.visualIndices[vout++] = stream[vbase + 2];
					}
				}
				
				continue;
			}
			
			final int[] surf = surfaces[s];
			for (int i = 0; i < surf[1]; i++)
			{
				final int base = surf[0] + (i * 3);
				if ((base + 2) >= stream.length)
				{
					break;
				}
				
				geom.indices[out++] = stream[base];
				geom.indices[out++] = stream[base + 1];
				geom.indices[out++] = stream[base + 2];
				if (debug && (i == 0))
				{
					System.out.println("  surface firstIndex=" + surf[0] + " triangleMax=" + surf[1]);
				}
			}
		}
		
		if (out < geom.indices.length)
		{
			geom.indices = Arrays.copyOf(geom.indices, out);
		}
		
		if (vout < geom.visualIndices.length)
		{
			geom.visualIndices = Arrays.copyOf(geom.visualIndices, vout);
		}
		
		return geom;
	}
	
	public static MeshGeometry loadMeshGeom(Path clientRoot, String pkgName, String meshName, Map<String, UnrealPackage> pkgCache, Map<String, MeshGeometry> geomCache) throws IOException
	{
		final String key = pkgName + "." + meshName;
		MeshGeometry geom = geomCache.get(key);
		if (geom != null)
		{
			return geom;
		}
		
		UnrealPackage pkg = pkgCache.get(pkgName);
		if (pkg == null)
		{
			pkg = new UnrealPackage(UnrealCrypt.decrypt(clientRoot.resolve("staticmeshes").resolve(pkgName + ".usx")));
			pkgCache.put(pkgName, pkg);
		}
		
		for (Export ex : pkg.exports)
		{
			if (pkg.names[ex.nameIdx].equalsIgnoreCase(meshName) && pkg.objName(ex.classRef).equals("StaticMesh"))
			{
				geom = loadMeshGeom(pkg, ex);
				geomCache.put(key, geom);
				return geom;
			}
		}
		
		throw new IOException("StaticMesh '" + meshName + "' not found in " + pkgName + ".usx");
	}
	
	/**
	 * The UE2 FRotationMatrix for one actor rotation, row-vector convention (world = local * M + Location). Depends only on the rotation, so callers transforming many vertices of one actor build it once.
	 * @param rotation pitch, yaw, roll (65536 = 360 degrees)
	 * @return the 3x3 matrix, row-major
	 */
	public static double[] rotationMatrix(int[] rotation)
	{
		final double toRad = (Math.PI * 2) / 65536.0;
		final double sp = Math.sin(rotation[0] * toRad);
		final double cp = Math.cos(rotation[0] * toRad);
		final double sy = Math.sin(rotation[1] * toRad);
		final double cy = Math.cos(rotation[1] * toRad);
		final double sr = Math.sin(rotation[2] * toRad);
		final double cr = Math.cos(rotation[2] * toRad);
		return new double[]
		{
			cp * cy,
			(sr * sp * cy) - (cr * sy),
			-((cr * sp * cy) + (sr * sy)),
			cp * sy,
			(sr * sp * sy) + (cr * cy),
			(cy * sr) - (cr * sp * sy),
			sp,
			-sr * cp,
			cr * cp
		};
	}
	
	/**
	 * UE2 actor transform against a prebuilt rotation matrix.
	 * @param a the placed actor
	 * @param v the local-space vertex
	 * @param m the rotation matrix from {@code rotationMatrix(int[])}
	 * @return the world-space point {x, y, z}
	 */
	public static double[] toWorld(MeshActor a, float[] v, double[] m)
	{
		final double x = v[0] * a.scale3d[0] * a.drawScale;
		final double y = v[1] * a.scale3d[1] * a.drawScale;
		final double z = v[2] * a.scale3d[2] * a.drawScale;
		return new double[]
		{
			(x * m[0]) + (y * m[1]) + (z * m[2]) + a.location[0],
			(x * m[3]) + (y * m[4]) + (z * m[5]) + a.location[1],
			(x * m[6]) + (y * m[7]) + (z * m[8]) + a.location[2]
		};
	}
	
	/**
	 * Prints every mesh surface directly above/below the given world point, top to bottom.
	 * @param clientRoot the client folder
	 * @param map the region name (XX_YY)
	 * @param px the world X coordinate
	 * @param py the world Y coordinate
	 * @throws IOException on unreadable packages
	 */
	public static void meshSurfaces(Path clientRoot, String map, double px, double py) throws IOException
	{
		final List<MeshActor> actors = loadMeshActors(clientRoot, map);
		System.out.println("StaticMeshActors on map " + map + ": " + actors.size());
		
		final Map<String, UnrealPackage> pkgCache = new HashMap<>();
		final Map<String, MeshGeometry> geomCache = new HashMap<>();
		final List<String> hitNames = new ArrayList<>();
		final boolean debug = System.getenv("MR_DEBUG") != null;
		double[] hits = new double[64];
		int hitCount = 0;
		
		for (MeshActor actor : actors)
		{
			// Cheap distance filter: skip actors far from the query point.
			final double dx = actor.location[0] - px;
			final double dy = actor.location[1] - py;
			if (((dx * dx) + (dy * dy)) > (8192.0 * 8192.0))
			{
				continue;
			}
			
			final MeshGeometry geom;
			try
			{
				geom = loadMeshGeom(clientRoot, actor.meshPackage, actor.meshName, pkgCache, geomCache);
			}
			catch (Exception e)
			{
				System.out.println("  Skipping " + actor.meshPackage + "." + actor.meshName + ": " + e.getMessage());
				continue;
			}
			
			final double[] rot = rotationMatrix(actor.rotation);
			if (debug)
			{
				double minX = Double.MAX_VALUE;
				double maxX = -Double.MAX_VALUE;
				double minY = Double.MAX_VALUE;
				double maxY = -Double.MAX_VALUE;
				double minZ = Double.MAX_VALUE;
				double maxZ = -Double.MAX_VALUE;
				for (float[] v : geom.verts)
				{
					final double[] w = toWorld(actor, v, rot);
					minX = Math.min(minX, w[0]);
					maxX = Math.max(maxX, w[0]);
					minY = Math.min(minY, w[1]);
					maxY = Math.max(maxY, w[1]);
					minZ = Math.min(minZ, w[2]);
					maxZ = Math.max(maxZ, w[2]);
				}
				
				System.out.printf("  near: %s.%s verts=%d idx=%d X[%.0f..%.0f] Y[%.0f..%.0f] Z[%.0f..%.0f]%n", actor.meshPackage, actor.meshName, geom.verts.length, geom.indices.length, minX, maxX, minY, maxY, minZ, maxZ);
			}
			
			for (int i = 0; (i + 2) < geom.indices.length; i += 3)
			{
				final double[] v0 = toWorld(actor, geom.verts[geom.indices[i]], rot);
				final double[] v1 = toWorld(actor, geom.verts[geom.indices[i + 1]], rot);
				final double[] v2 = toWorld(actor, geom.verts[geom.indices[i + 2]], rot);
				
				// 2D point-in-triangle via signed areas.
				final double d1 = sign(px, py, v0, v1);
				final double d2 = sign(px, py, v1, v2);
				final double d3 = sign(px, py, v2, v0);
				if (((d1 < 0) || (d2 < 0) || (d3 < 0)) && ((d1 > 0) || (d2 > 0) || (d3 > 0)))
				{
					continue;
				}
				
				// Barycentric interpolation for Z.
				final double area = ((v1[0] - v0[0]) * (v2[1] - v0[1])) - ((v2[0] - v0[0]) * (v1[1] - v0[1]));
				if (area == 0)
				{
					continue;
				}
				
				final double w0 = (((v1[0] - px) * (v2[1] - py)) - ((v2[0] - px) * (v1[1] - py))) / area;
				final double w1 = (((v2[0] - px) * (v0[1] - py)) - ((v0[0] - px) * (v2[1] - py))) / area;
				final double z = (w0 * v0[2]) + (w1 * v1[2]) + ((1 - w0 - w1) * v2[2]);
				if (hitCount == hits.length)
				{
					hits = Arrays.copyOf(hits, hits.length * 2);
				}
				
				hits[hitCount++] = z;
				hitNames.add(actor.meshPackage + "." + actor.meshName);
			}
		}
		
		if (hitCount == 0)
		{
			System.out.println("No mesh surfaces above/below this point.");
			return;
		}
		
		final Integer[] order = new Integer[hitCount];
		for (int i = 0; i < order.length; i++)
		{
			order[i] = i;
		}
		
		final double[] hitZ = hits;
		Arrays.sort(order, (i, j) -> Double.compare(hitZ[j], hitZ[i]));
		System.out.println("Mesh surfaces at this point (top to bottom):");
		for (int i = 0; i < Math.min(order.length, 12); i++)
		{
			System.out.printf("  z=%.1f  %s%n", hits[order[i]] + GeoConfig.GEODATA_Z_OFFSET, hitNames.get(order[i]));
		}
	}
	
	public static double sign(double px, double py, double[] a, double[] b)
	{
		return ((px - b[0]) * (a[1] - b[1])) - ((a[0] - b[0]) * (py - b[1]));
	}
	
	/**
	 * Brush point transform against a prebuilt rotation matrix.
	 * @param v the local-space point
	 * @param scale3d the brush scale
	 * @param m the rotation matrix from {@code rotationMatrix(int[])}
	 * @param location the brush world location
	 * @param prepivot the brush prepivot
	 * @return the world-space point {x, y, z}
	 */
	public static double[] transformPoint(float[] v, float[] scale3d, double[] m, float[] location, float[] prepivot)
	{
		final double x = (v[0] - prepivot[0]) * scale3d[0];
		final double y = (v[1] - prepivot[1]) * scale3d[1];
		final double z = (v[2] - prepivot[2]) * scale3d[2];
		return new double[]
		{
			(x * m[0]) + (y * m[1]) + (z * m[2]) + location[0],
			(x * m[3]) + (y * m[4]) + (z * m[5]) + location[1],
			(x * m[6]) + (y * m[7]) + (z * m[8]) + location[2]
		};
	}
}
