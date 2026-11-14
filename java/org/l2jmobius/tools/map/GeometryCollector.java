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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/**
 * Collects world-space collision triangles: level BSP, BlockingVolumes, static meshes, water surfaces; excludes openable door panels via Mover-derived boxes.
 * @author Mobius
 */
public class GeometryCollector
{
	/** One openable door found in the client map: a Mover actor (id = L2ServerObjectRealID) or a door-named StaticMeshActor the client opens locally (id = 0). */
	public static class DoorInfo
	{
		public int id;
		public String name;
		public float[] pos;
		public double[] box; // {minX, minY, maxX, maxY, minZ, maxZ} raw client space, used to unbake the panel from geodata.
		public double[] nodeX; // the four footprint corner X's in world space (Mover RangeDelta, matching the server DoorData nodes).
		public double[] nodeY; // the four footprint corner Y's in world space.
		public double nodeZ; // the footprint floor Z.
	}
	
	/** One WaterVolume found in the client map. */
	public static class WaterInfo
	{
		public String name;
		public double[] box; // {minX, minY, maxX, maxY, minZ, maxZ} raw client space.
	}
	
	/**
	 * Collects the map's openable doors with transformed mesh bounding boxes: every Mover panel (server-operated when it carries L2ServerObjectRealID), plus door-named plain StaticMeshActors.<br>
	 * Town auto-doors (H_Door_MV_02 at the Gludio dark elf guild entrance) are ordinary StaticMeshActors without a server id - the client opens them locally, so their panel must not bake as a wall; they get id 0 and stay out of doors.xml.
	 * @param unr the opened map package
	 * @param clientRoot the client folder
	 * @param pkgCache shared package cache
	 * @param geomCache shared mesh geometry cache
	 * @return the doors found in the map
	 */
	public static List<DoorInfo> collectDoorActors(UnrealPackage unr, Path clientRoot, Map<String, UnrealPackage> pkgCache, Map<String, MeshGeometry> geomCache)
	{
		return collectDoorActors(unr, clientRoot, pkgCache, geomCache, () -> false);
	}
	
	private static List<DoorInfo> collectDoorActors(UnrealPackage unr, Path clientRoot, Map<String, UnrealPackage> pkgCache, Map<String, MeshGeometry> geomCache, BooleanSupplier stopRequested)
	{
		final List<DoorInfo> doors = new ArrayList<>();
		for (Export ex : unr.exports)
		{
			checkCancelled(stopRequested);
			final String cls = unr.objName(ex.classRef);
			final boolean mover = cls.equals("Mover");
			if (!mover && !cls.equals("StaticMeshActor"))
			{
				continue;
			}
			
			final Map<String, Object> props = UnrealPackage.readProps(unr, ex);
			if (!(props.get("StaticMesh") instanceof Integer ref) || (ref >= 0))
			{
				continue;
			}
			
			if (!mover && !unr.objName(ref).toLowerCase().contains("door"))
			{
				continue;
			}
			
			final int id = props.get("L2ServerObjectRealID") instanceof Integer realId ? realId : 0;
			
			final MeshActor actor = new MeshActor();
			actor.meshPackage = unr.rootPackage(ref);
			actor.meshName = unr.objName(ref);
			if (props.get("Location") instanceof float[] l)
			{
				actor.location = l;
			}
			else if (props.get("BasePos") instanceof float[] bp)
			{
				actor.location = bp;
			}
			
			if (props.get("Rotation") instanceof int[] r)
			{
				actor.rotation = r;
			}
			else if (props.get("BaseRot") instanceof int[] br)
			{
				actor.rotation = br;
			}
			
			if (props.get("DrawScale3D") instanceof float[] s3)
			{
				actor.scale3d = s3;
			}
			
			if (props.get("DrawScale") instanceof Float ds)
			{
				actor.drawScale = ds;
			}
			
			try
			{
				final MeshGeometry geom = StaticMeshes.loadMeshGeom(clientRoot, actor.meshPackage, actor.meshName, pkgCache, geomCache);
				final double[] rot = StaticMeshes.rotationMatrix(actor.rotation);
				double minX = Double.MAX_VALUE;
				double minY = Double.MAX_VALUE;
				double minZ = Double.MAX_VALUE;
				double maxX = -Double.MAX_VALUE;
				double maxY = -Double.MAX_VALUE;
				double maxZ = -Double.MAX_VALUE;
				int vertexIndex = 0;
				for (float[] v : geom.verts)
				{
					if ((vertexIndex++ & 1023) == 0)
					{
						checkCancelled(stopRequested);
					}
					final double[] w = StaticMeshes.toWorld(actor, v, rot);
					minX = Math.min(minX, w[0]);
					maxX = Math.max(maxX, w[0]);
					minY = Math.min(minY, w[1]);
					maxY = Math.max(maxY, w[1]);
					minZ = Math.min(minZ, w[2]);
					maxZ = Math.max(maxZ, w[2]);
				}
				
				final DoorInfo door = new DoorInfo();
				door.id = id;
				door.name = unr.fullName(ref);
				door.pos = actor.location;
				door.box = new double[]
				{
					minX,
					minY,
					maxX,
					maxY,
					minZ,
					maxZ
				};
				
				// The server DoorData footprint is the door's oriented rectangle, not an axis box: the four RangeDelta vectors are world-space offsets from the door location (rotation and scale already applied) and reproduce the client nodes exactly.
				// Fall back to the mesh box corners when a door carries no RangeDelta.
				if ((actor.location != null) && (props.get("RangeDelta") instanceof float[] d0) && (props.get("RangeDelta[1]") instanceof float[] d1) && (props.get("RangeDelta[2]") instanceof float[] d2) && (props.get("RangeDelta[3]") instanceof float[] d3))
				{
					door.nodeX = new double[]
					{
						actor.location[0] + d0[0],
						actor.location[0] + d1[0],
						actor.location[0] + d2[0],
						actor.location[0] + d3[0]
					};
					door.nodeY = new double[]
					{
						actor.location[1] + d0[1],
						actor.location[1] + d1[1],
						actor.location[1] + d2[1],
						actor.location[1] + d3[1]
					};
					door.nodeZ = actor.location[2] + d0[2];
				}
				else
				{
					door.nodeX = new double[]
					{
						minX,
						minX,
						maxX,
						maxX
					};
					door.nodeY = new double[]
					{
						minY,
						maxY,
						maxY,
						minY
					};
					door.nodeZ = minZ;
				}
				
				doors.add(door);
			}
			catch (Exception e)
			{
				// Unreadable door mesh: no box, panel still excluded via the Mover class filter.
			}
		}
		
		return doors;
	}
	
	/**
	 * Opens the map package and collects its openable doors.
	 * @param clientRoot the client folder
	 * @param map the region name (XX_YY)
	 * @return the doors found in the map
	 * @throws IOException on unreadable map package
	 */
	public static List<DoorInfo> extractDoors(Path clientRoot, String map) throws IOException
	{
		return extractDoors(clientRoot, map, () -> false);
	}
	
	/**
	 * Opens the map package and collects its openable doors with cooperative cancellation.
	 * @param clientRoot the client folder
	 * @param map the region name (XX_YY)
	 * @param stopRequested cancellation signal
	 * @return the doors found in the map
	 * @throws IOException on unreadable map package
	 */
	public static List<DoorInfo> extractDoors(Path clientRoot, String map, BooleanSupplier stopRequested) throws IOException
	{
		checkCancelled(stopRequested);
		final UnrealPackage unr = new UnrealPackage(UnrealCrypt.decrypt(clientRoot.resolve("maps").resolve(map + ".unr")));
		checkCancelled(stopRequested);
		return collectDoorActors(unr, clientRoot, new HashMap<>(), new HashMap<>(), stopRequested);
	}
	
	/**
	 * Opens the map package and collects WaterVolume bounding boxes.
	 * @param clientRoot the client folder
	 * @param map the region name (XX_YY)
	 * @return the water volumes found in the map
	 * @throws IOException on unreadable map package
	 */
	public static List<WaterInfo> extractWater(Path clientRoot, String map) throws IOException
	{
		return extractWater(clientRoot, map, () -> false);
	}
	
	/**
	 * Opens the map package and collects WaterVolume bounding boxes with cooperative cancellation.
	 * @param clientRoot the client folder
	 * @param map the region name (XX_YY)
	 * @param stopRequested cancellation signal
	 * @return the water volumes found in the map
	 * @throws IOException on unreadable map package
	 */
	public static List<WaterInfo> extractWater(Path clientRoot, String map, BooleanSupplier stopRequested) throws IOException
	{
		checkCancelled(stopRequested);
		final UnrealPackage unr = new UnrealPackage(UnrealCrypt.decrypt(clientRoot.resolve("maps").resolve(map + ".unr")));
		checkCancelled(stopRequested);
		final List<WaterInfo> volumes = new ArrayList<>();
		for (Export ex : unr.exports)
		{
			checkCancelled(stopRequested);
			if (!unr.objName(ex.classRef).equals("WaterVolume"))
			{
				continue;
			}
			
			final Map<String, Object> props = UnrealPackage.readProps(unr, ex);
			if (!(props.get("Brush") instanceof Integer ref) || (ref <= 0))
			{
				continue;
			}
			
			float[] location = (float[]) props.getOrDefault("Location", new float[3]);
			if (props.get("ColLocation") instanceof float[] col)
			{
				location = col;
			}
			
			final float[] prepivot = props.get("PrePivot") instanceof float[] pp ? pp : new float[3];
			final int[] rotation = props.get("Rotation") instanceof int[] r ? r : new int[3];
			final float[] scale3d =
			{
				1,
				1,
				1
			};
			
			if (props.get("MainScale") instanceof float[] ms)
			{
				scale3d[0] *= ms[0];
				scale3d[1] *= ms[1];
				scale3d[2] *= ms[2];
			}
			
			if (props.get("PostScale") instanceof float[] ps)
			{
				scale3d[0] *= ps[0];
				scale3d[1] *= ps[1];
				scale3d[2] *= ps[2];
			}
			
			final BspModel brush = BspModel.parseModel(unr, unr.exports[ref - 1]);
			final double[] rot = StaticMeshes.rotationMatrix(rotation);
			double minX = Double.MAX_VALUE;
			double minY = Double.MAX_VALUE;
			double minZ = Double.MAX_VALUE;
			double maxX = -Double.MAX_VALUE;
			double maxY = -Double.MAX_VALUE;
			double maxZ = -Double.MAX_VALUE;
			int points = 0;
			for (BspNode node : brush.nodes)
			{
				checkCancelled(stopRequested);
				for (int i = 0; i < node.vertexCount; i++)
				{
					final double[] w = StaticMeshes.transformPoint(brush.points[brush.vertPointIdx[node.vertexPool + i]], scale3d, rot, location, prepivot);
					minX = Math.min(minX, w[0]);
					maxX = Math.max(maxX, w[0]);
					minY = Math.min(minY, w[1]);
					maxY = Math.max(maxY, w[1]);
					minZ = Math.min(minZ, w[2]);
					maxZ = Math.max(maxZ, w[2]);
					points++;
				}
			}
			
			if (points == 0)
			{
				continue;
			}
			
			final WaterInfo water = new WaterInfo();
			water.name = unr.names[ex.nameIdx];
			water.box = new double[]
			{
				minX,
				minY,
				maxX,
				maxY,
				minZ,
				maxZ
			};
			volumes.add(water);
		}
		
		return volumes;
	}
	
	public static List<double[]> collectWaterTriangles(UnrealPackage unr)
	{
		return collectWaterTriangles(unr, () -> false);
	}
	
	public static List<double[]> collectWaterTriangles(UnrealPackage unr, BooleanSupplier stopRequested)
	{
		final List<double[]> water = new ArrayList<>();
		for (Export ex : unr.exports)
		{
			checkCancelled(stopRequested);
			if (!unr.objName(ex.classRef).equals("WaterVolume"))
			{
				continue;
			}
			
			final Map<String, Object> props = UnrealPackage.readProps(unr, ex);
			if (!(props.get("Brush") instanceof Integer ref) || (ref <= 0))
			{
				continue;
			}
			
			float[] location = (float[]) props.getOrDefault("Location", new float[3]);
			if (props.get("ColLocation") instanceof float[] col)
			{
				location = col;
			}
			
			final float[] prepivot = props.get("PrePivot") instanceof float[] pp ? pp : new float[3];
			final int[] rotation = props.get("Rotation") instanceof int[] r ? r : new int[3];
			final float[] scale3d =
			{
				1,
				1,
				1
			};
			
			if (props.get("MainScale") instanceof float[] ms)
			{
				scale3d[0] *= ms[0];
				scale3d[1] *= ms[1];
				scale3d[2] *= ms[2];
			}
			
			if (props.get("PostScale") instanceof float[] ps)
			{
				scale3d[0] *= ps[0];
				scale3d[1] *= ps[1];
				scale3d[2] *= ps[2];
			}
			
			final BspModel brush = BspModel.parseModel(unr, unr.exports[ref - 1]);
			final double[] rot = StaticMeshes.rotationMatrix(rotation);
			int kept = 0;
			for (BspNode node : brush.nodes)
			{
				checkCancelled(stopRequested);
				if (node.vertexCount < 3)
				{
					continue;
				}
				
				// The fan pivot is the same point for every triangle of this node, so it is transformed once instead of once per triangle.
				final double[] p0 = StaticMeshes.transformPoint(brush.points[brush.vertPointIdx[node.vertexPool]], scale3d, rot, location, prepivot);
				for (int i = 2; i < node.vertexCount; i++)
				{
					final double[] p1 = StaticMeshes.transformPoint(brush.points[brush.vertPointIdx[(node.vertexPool + i) - 1]], scale3d, rot, location, prepivot);
					final double[] p2 = StaticMeshes.transformPoint(brush.points[brush.vertPointIdx[node.vertexPool + i]], scale3d, rot, location, prepivot);
					
					// Keep only near-horizontal faces: the water top.
					final double ax = p1[0] - p0[0];
					final double ay = p1[1] - p0[1];
					final double az = p1[2] - p0[2];
					final double bx = p2[0] - p0[0];
					final double by = p2[1] - p0[1];
					final double bz = p2[2] - p0[2];
					final double cx = (ay * bz) - (az * by);
					final double cy2 = (az * bx) - (ax * bz);
					final double cz = (ax * by) - (ay * bx);
					final double nlen = Math.sqrt((cx * cx) + (cy2 * cy2) + (cz * cz));
					if ((nlen == 0) || ((Math.abs(cz) / nlen) < 0.7))
					{
						continue; // Side/bottom face, not the surface.
					}
					
					water.add(new double[]
					{
						p0[0],
						p0[1],
						p0[2],
						p1[0],
						p1[1],
						p1[2],
						p2[0],
						p2[1],
						p2[2]
					});
					kept++;
				}
			}
			
			if (kept > 0)
			{
				System.out.println("WaterVolume '" + unr.names[ex.nameIdx] + "': " + kept + " surface triangles");
			}
		}
		
		return water;
	}
	
	public static List<double[]> collectTriangles(Path clientRoot, String map) throws IOException
	{
		return collectTriangles(clientRoot, map, () -> false);
	}
	
	public static List<double[]> collectTriangles(Path clientRoot, String map, BooleanSupplier stopRequested) throws IOException
	{
		checkCancelled(stopRequested);
		final List<double[]> tris = new ArrayList<>(); // {x0,y0,z0,x1,y1,z1,x2,y2,z2}
		
		int doorTris = 0;
		
		final UnrealPackage unr = new UnrealPackage(UnrealCrypt.decrypt(clientRoot.resolve("maps").resolve(map + ".unr")));
		final Map<String, UnrealPackage> pkgCache = new HashMap<>();
		final Map<String, MeshGeometry> geomCache = new HashMap<>();
		
		// Door boxes come from the CLIENT map itself: Mover panels (server doors when they carry L2ServerObjectRealID) and door-named StaticMeshActors (client-local town auto-doors).
		// Their transformed mesh bounding boxes mark where decorative grates / BlockingVolume stand-ins must not bake as walls.
		DOOR_BOXES = new ArrayList<>();
		BRIDGE_TRIS = new ArrayList<>();
		STAIR_TRIS = new ArrayList<>();
		VISUAL_WALL_TRIS = new ArrayList<>();
		BRIDGE_FENCE_TRIS = new ArrayList<>();
		checkCancelled(stopRequested);
		for (DoorInfo door : collectDoorActors(unr, clientRoot, pkgCache, geomCache, stopRequested))
		{
			checkCancelled(stopRequested);
			DOOR_BOXES.add(new double[]
			{
				door.box[0] - 8,
				door.box[1] - 8,
				door.box[2] + 8,
				door.box[3] + 8,
				door.box[4] - 32,
				door.box[5] + 64
			});
		}
		
		System.out.println("Door boxes from map Movers/door meshes: " + DOOR_BOXES.size());
		
		// Brush-source models (Brush/Mover/*Volume actors) are LOCAL space - they must not be added as world geometry here.
		// Everything else of class Model is a level BSP (main + secondary levels).
		final Set<Integer> brushModels = new HashSet<>();
		for (Export ex : unr.exports)
		{
			checkCancelled(stopRequested);
			final String cls = unr.objName(ex.classRef);
			if (cls.equals("Brush") || cls.equals("Mover") || cls.endsWith("Volume"))
			{
				if (UnrealPackage.readProps(unr, ex).get("Brush") instanceof Integer r && (r > 0))
				{
					brushModels.add(r - 1);
				}
			}
		}
		
		int levelModels = 0;
		for (int m = 0; m < unr.exports.length; m++)
		{
			checkCancelled(stopRequested);
			final Export modelExport = unr.exports[m];
			if (!unr.objName(modelExport.classRef).equals("Model") || brushModels.contains(m) || (modelExport.size < 100))
			{
				continue;
			}
			
			final BspModel bsp = BspModel.parseModel(unr, modelExport);
			if (bsp.nodes.isEmpty())
			{
				continue;
			}
			
			levelModels++;
			for (BspNode node : bsp.nodes)
			{
				checkCancelled(stopRequested);
				if (((node.flags & BspModel.NF_PASSABLE) != 0) || (node.vertexCount < 3) || ((bsp.surfaceFlags[node.surface] & BspModel.PF_PASSABLE) != 0) || ((bsp.surfaceFlags[node.surface] & BspModel.PF_HULL) == BspModel.PF_HULL))
				{
					continue;
				}
				
				for (int i = 2; i < node.vertexCount; i++)
				{
					final float[] v0 = bsp.points[bsp.vertPointIdx[node.vertexPool]];
					final float[] v1 = bsp.points[bsp.vertPointIdx[(node.vertexPool + i) - 1]];
					final float[] v2 = bsp.points[bsp.vertPointIdx[node.vertexPool + i]];
					if ((v0[2] < -16000) && (v1[2] < -16000) && (v2[2] < -16000))
					{
						continue; // World bottom plane.
					}
					
					final double[] bspTri = new double[]
					{
						v0[0],
						v0[1],
						v0[2],
						v1[0],
						v1[1],
						v1[2],
						v2[0],
						v2[1],
						v2[2]
					};
					tris.add(bspTri);
					
					if (GeoConfig.DEBUG_CELL != null)
					{
						TRI_SOURCE.put(bspTri, "BSP level node surface#" + node.surface + " flags=0x" + Integer.toHexString(bsp.surfaceFlags[node.surface]));
					}
				}
			}
		}
		
		System.out.println("Level BSP models: " + levelModels + " (brush models excluded: " + brushModels.size() + ")");
		
		// Coleta triângulos de BlockingVolume (paredes invisíveis).
		for (Export ex : unr.exports)
		{
			checkCancelled(stopRequested);
			if (!unr.objName(ex.classRef).equals("BlockingVolume"))
			{
				continue;
			}
			
			final Map<String, Object> props = UnrealPackage.readProps(unr, ex);
			if (!(props.get("Brush") instanceof Integer ref) || (ref <= 0))
			{
				continue;
			}
			
			final float[] location;
			if (props.containsKey("ColLocation"))
			{
				location = (float[]) props.get("ColLocation");
			}
			else if (props.containsKey("Location"))
			{
				location = (float[]) props.get("Location");
			}
			else
			{
				location = new float[3];
			}
			
			final float[] prepivot = props.containsKey("PrePivot") ? (float[]) props.get("PrePivot") : new float[3];
			
			final int[] rotation = props.containsKey("Rotation") ? (int[]) props.get("Rotation") : new int[3];
			
			float[] scale3d = new float[]
			{
				1,
				1,
				1
			};
			
			if (props.containsKey("MainScale"))
			{
				float[] mainScale = (float[]) props.get("MainScale");
				scale3d[0] *= mainScale[0];
				scale3d[1] *= mainScale[1];
				scale3d[2] *= mainScale[2];
			}
			
			if (props.containsKey("PostScale"))
			{
				float[] postScale = (float[]) props.get("PostScale");
				scale3d[0] *= postScale[0];
				scale3d[1] *= postScale[1];
				scale3d[2] *= postScale[2];
			}
			
			final BspModel volBsp = BspModel.parseModel(unr, unr.exports[ref - 1]);
			if ((volBsp != null) && (volBsp.points != null))
			{
				final double[] rot = StaticMeshes.rotationMatrix(rotation);
				for (BspNode node : volBsp.nodes)
				{
					checkCancelled(stopRequested);
					if (((node.flags & BspModel.NF_PASSABLE) != 0) || (node.vertexCount < 3))
					{
						continue;
					}
					
					// The fan pivot is the same point for every triangle of this node, so it is transformed once instead of once per triangle.
					final double[] p0 = StaticMeshes.transformPoint(volBsp.points[volBsp.vertPointIdx[node.vertexPool]], scale3d, rot, location, prepivot);
					for (int i = 2; i < node.vertexCount; i++)
					{
						final float[] v1 = volBsp.points[volBsp.vertPointIdx[(node.vertexPool + i) - 1]];
						final float[] v2 = volBsp.points[volBsp.vertPointIdx[node.vertexPool + i]];
						final double[] p1 = StaticMeshes.transformPoint(v1, scale3d, rot, location, prepivot);
						final double[] p2 = StaticMeshes.transformPoint(v2, scale3d, rot, location, prepivot);
						
						// BlockingVolumes standing in for openable doors must not bake either.
						if (isDoorWallTri(p0, p1, p2))
						{
							doorTris++;
							continue;
						}
						
						tris.add(new double[]
						{
							p0[0],
							p0[1],
							p0[2],
							p1[0],
							p1[1],
							p1[2],
							p2[0],
							p2[1],
							p2[2]
						});
					}
				}
			}
		}
		
		int skipped = 0;
		int signs = 0;
		int visualWalls = 0;
		checkCancelled(stopRequested);
		for (MeshActor actor : StaticMeshes.loadMeshActors(clientRoot, map))
		{
			checkCancelled(stopRequested);
			final String lowerName = actor.meshName.toLowerCase();
			
			// Hanging shop signs (interior_B_sign/fsign, FDeco_sign) dangle into the head window over entrances - the dark elf guild sign closed every boundary across its own doorway. Chars walk under them everywhere: never bake.
			if (lowerName.contains("sign"))
			{
				signs++;
				continue;
			}
			
			// Bridge-class meshes (bridges, piers, docks, quays) are built to be WALKED ON over water: their footprints are exempt from the wall-column verdict - pier faces rising from the water read as walls and used to erase decks.
			final boolean bridgeClass = lowerName.contains("bridge") || lowerName.contains("pier") || lowerName.contains("dock") || lowerName.contains("quay");
			
			// A mesh that calls itself a stair IS one: exempt its cells from the lone-step rule, whose one-cell lookahead cannot see past a deep tread.
			final boolean stairClass = lowerName.contains("stair") || lowerName.contains("step");
			final MeshGeometry geom;
			try
			{
				geom = StaticMeshes.loadMeshGeom(clientRoot, actor.meshPackage, actor.meshName, pkgCache, geomCache);
			}
			catch (Exception e)
			{
				skipped++;
				continue;
			}
			
			final double[] rot = StaticMeshes.rotationMatrix(actor.rotation);
			
			// Built-in door sections (door-named materials) publish a DOOR_BOX: the surrounding frame must not bake or raster as wall, or the widened boundary strips seal the passage (Dwarven Village round doors).
			if (geom.doorBounds != null)
			{
				double minX = Double.MAX_VALUE;
				double minY = Double.MAX_VALUE;
				double minZ = Double.MAX_VALUE;
				double maxX = -Double.MAX_VALUE;
				double maxY = -Double.MAX_VALUE;
				double maxZ = -Double.MAX_VALUE;
				for (int c = 0; c < 8; c++)
				{
					final double[] w = StaticMeshes.toWorld(actor, new float[]
					{
						geom.doorBounds[(c & 1) == 0 ? 0 : 3],
						geom.doorBounds[(c & 2) == 0 ? 1 : 4],
						geom.doorBounds[(c & 4) == 0 ? 2 : 5]
					}, rot);
					minX = Math.min(minX, w[0]);
					maxX = Math.max(maxX, w[0]);
					minY = Math.min(minY, w[1]);
					maxY = Math.max(maxY, w[1]);
					minZ = Math.min(minZ, w[2]);
					maxZ = Math.max(maxZ, w[2]);
				}
				
				// A door OPENING is at most ~150 units wide: larger bounds mean the door material appears at several places across the building mesh and the union spans the whole structure - such a box would exclude entire real walls from the bake and the edge raster (the Gludio magic shop lost its portico side wall to one, and/ heals were cast through it in-game).
				// Oversized accumulations are dropped, the real panels stay covered.
				if (((maxX - minX) <= 200) && ((maxY - minY) <= 200))
				{
					DOOR_BOXES.add(new double[]
					{
						minX - 8,
						minY - 8,
						maxX + 8,
						maxY + 8,
						minZ - 32,
						maxZ + 64
					});
				}
			}
			
			for (int i = 0; (i + 2) < geom.indices.length; i += 3)
			{
				if ((i & 3071) == 0)
				{
					checkCancelled(stopRequested);
				}
				final double[] w0 = StaticMeshes.toWorld(actor, geom.verts[geom.indices[i]], rot);
				final double[] w1 = StaticMeshes.toWorld(actor, geom.verts[geom.indices[i + 1]], rot);
				final double[] w2 = StaticMeshes.toWorld(actor, geom.verts[geom.indices[i + 2]], rot);
				
				// Openable-door panels are plain StaticMeshActors in some buildings (clan hall "Agit_gate") - like Movers they must NOT be baked: the server blocks them dynamically (DoorData) while closed.
				// Doors.xml node boxes identify them. Only WALL-like triangles are dropped: the door sill/steps belong to the same mesh and must stay walkable.
				if (isDoorWallTri(w0, w1, w2))
				{
					doorTris++;
					continue;
				}
				
				final double[] tri = new double[]
				{
					w0[0],
					w0[1],
					w0[2],
					w1[0],
					w1[1],
					w1[2],
					w2[0],
					w2[1],
					w2[2]
				};
				tris.add(tri);
				
				if (GeoConfig.DEBUG_CELL != null)
				{
					TRI_SOURCE.put(tri, "mesh '" + actor.meshName + "' pkg=" + actor.meshPackage);
				}
				
				if (bridgeClass)
				{
					BRIDGE_TRIS.add(tri);
				}
				
				if (stairClass)
				{
					STAIR_TRIS.add(tri);
				}
			}
			
			// Bridge rails and balustrades exist ONLY as non-colliding visual sections: collect them so the builder can raster their wall-like faces as movement fences along the deck sides.
			// ARCHITECTURE THAT DECLARES NO COLLISION: large buildings ship every section EnableCollision=false and expect collision from BlockingVolumes or BSP.
			// The Superion-era EU client gives the Giran church neither, so the whole building baked as thin air. STORE ITS WALL GEOMETRY from what it draws - the VERTICAL faces are its walls and pillars (horizontal faces stay out: roofs and ceilings must not become walkable floors in mid-air). The faces enter the ordinary pipeline, so the boundary raster closes the NSWE direction of EVERY cell touching a wall face,
			// and the wall-column verdict makes qualifying cells sight-blocking columns. Scoped to zero-collision meshes; the Interlude client has none (0 in 22_22/19_21/21_23), so its data cannot move.
			if ((geom.indices.length == 0) && (geom.visualIndices != null) && !stairClass)
			{
				for (int i = 0; (i + 2) < geom.visualIndices.length; i += 3)
				{
					if ((i & 3071) == 0)
					{
						checkCancelled(stopRequested);
					}
					final double[] v0 = StaticMeshes.toWorld(actor, geom.verts[geom.visualIndices[i]], rot);
					final double[] v1 = StaticMeshes.toWorld(actor, geom.verts[geom.visualIndices[i + 1]], rot);
					final double[] v2 = StaticMeshes.toWorld(actor, geom.verts[geom.visualIndices[i + 2]], rot);
					final double nx = ((v1[1] - v0[1]) * (v2[2] - v0[2])) - ((v1[2] - v0[2]) * (v2[1] - v0[1]));
					final double ny = ((v1[2] - v0[2]) * (v2[0] - v0[0])) - ((v1[0] - v0[0]) * (v2[2] - v0[2]));
					final double nz = ((v1[0] - v0[0]) * (v2[1] - v0[1])) - ((v1[1] - v0[1]) * (v2[0] - v0[0]));
					final double nlen = Math.sqrt((nx * nx) + (ny * ny) + (nz * nz));
					if ((nlen <= 0) || ((Math.abs(nz) / nlen) >= GeoConfig.STEEP_NZ))
					{
						continue; // walkable-grade face (flatter than 45 degrees): roofs, ceilings, ramps and entrance steps - storing those sealed the church doors; only true batter (steeper than 45) is wall material
					}
					
					if (isDoorWallTri(v0, v1, v2))
					{
						doorTris++;
						continue;
					}
					
					final double[] vtri = new double[]
					{
						v0[0],
						v0[1],
						v0[2],
						v1[0],
						v1[1],
						v1[2],
						v2[0],
						v2[1],
						v2[2]
					};
					tris.add(vtri);
					VISUAL_WALL_TRIS.add(vtri);
					visualWalls++;
					
					if (GeoConfig.DEBUG_CELL != null)
					{
						TRI_SOURCE.put(vtri, "VISUAL WALL of collisionless mesh '" + actor.meshName + "' pkg=" + actor.meshPackage);
					}
				}
			}
			
			if (bridgeClass && (geom.visualIndices != null))
			{
				for (int i = 0; (i + 2) < geom.visualIndices.length; i += 3)
				{
					final double[] v0 = StaticMeshes.toWorld(actor, geom.verts[geom.visualIndices[i]], rot);
					final double[] v1 = StaticMeshes.toWorld(actor, geom.verts[geom.visualIndices[i + 1]], rot);
					final double[] v2 = StaticMeshes.toWorld(actor, geom.verts[geom.visualIndices[i + 2]], rot);
					BRIDGE_FENCE_TRIS.add(new double[]
					{
						v0[0],
						v0[1],
						v0[2],
						v1[0],
						v1[1],
						v1[2],
						v2[0],
						v2[1],
						v2[2]
					});
				}
			}
		}
		
		if (signs > 0)
		{
			System.out.println("Sign meshes excluded: " + signs);
		}
		
		if (visualWalls > 0)
		{
			System.out.println("Wall geometry stored from collisionless architecture meshes: " + visualWalls + " vertical faces.");
		}
		
		if (skipped > 0)
		{
			System.out.println("Meshes skipped (parse failures): " + skipped);
		}
		
		if (doorTris > 0)
		{
			System.out.println("Door-panel triangles excluded (map Movers): " + doorTris);
		}
		
		return tris;
	}
	
	private static void checkCancelled(BooleanSupplier stopRequested)
	{
		if ((stopRequested != null) && stopRequested.getAsBoolean())
		{
			throw new CancellationException("Map extraction cancelled.");
		}
	}
	
	/**
	 * Per-map door boxes {minX, minY, maxX, maxY, minZ, maxZ}, derived from Mover actors carrying L2ServerObjectRealID (the client's own door ids).<br>
	 * Wall geometry inside is not baked.
	 */
	public static List<double[]> DOOR_BOXES = new ArrayList<>();
	public static List<double[]> BRIDGE_TRIS = new ArrayList<>(); // triangles of bridge-class meshes (bridge/pier/dock/quay): their footprints are exempt from the wall-column verdict
	public static List<double[]> STAIR_TRIS = new ArrayList<>(); // triangles of stair-class meshes (stair/step): their footprints are exempt from the lone-step rule - a staircase is a run of risers however deep its treads
	public static List<double[]> VISUAL_WALL_TRIS = new ArrayList<>(); // wall faces stored from collisionless architecture: their in-cell footprints block every touching cell toward them
	
	/**
	 * Debug aid: which actor/mesh/BSP produced a collision triangle, keyed by array IDENTITY (the same trick BRIDGE_TRIS uses). Populated only under -DdebugCell: on a real run it would hold millions of entries. Lets a span in the debug dump name the thing that made it.
	 */
	public static final Map<double[], String> TRI_SOURCE = new IdentityHashMap<>();
	public static List<double[]> BRIDGE_FENCE_TRIS = new ArrayList<>(); // NON-colliding visual triangles of bridge-class meshes (rails, balustrades): rastered as movement fences - the collision data carries no rail at all
	
	public static boolean inDoorBox(double x, double y, double z)
	{
		for (double[] b : DOOR_BOXES)
		{
			if ((x >= b[0]) && (x <= b[2]) && (y >= b[1]) && (y <= b[3]) && (z >= b[4]) && (z <= b[5]))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * True for WALL-like triangles whose bounding box overlaps a door box: the openable panel (or its BlockingVolume stand-in) - the server blocks these dynamically, they must not bake.
	 * @param a the first triangle vertex {x, y, z}
	 * @param b the second triangle vertex {x, y, z}
	 * @param c the third triangle vertex {x, y, z}
	 * @return true when the triangle belongs to an openable door
	 */
	public static boolean isDoorWallTri(double[] a, double[] b, double[] c)
	{
		final double minX = Math.min(a[0], Math.min(b[0], c[0]));
		final double maxX = Math.max(a[0], Math.max(b[0], c[0]));
		final double minY = Math.min(a[1], Math.min(b[1], c[1]));
		final double maxY = Math.max(a[1], Math.max(b[1], c[1]));
		final double minZ = Math.min(a[2], Math.min(b[2], c[2]));
		final double maxZ = Math.max(a[2], Math.max(b[2], c[2]));
		boolean overlap = false;
		for (double[] box : DOOR_BOXES)
		{
			if ((minX <= box[2]) && (maxX >= box[0]) && (minY <= box[3]) && (maxY >= box[1]) && (minZ <= box[5]) && (maxZ >= box[4]))
			{
				overlap = true;
				break;
			}
		}
		
		if (!overlap)
		{
			return false;
		}
		
		final double nx = ((b[1] - a[1]) * (c[2] - a[2])) - ((b[2] - a[2]) * (c[1] - a[1]));
		final double ny = ((b[2] - a[2]) * (c[0] - a[0])) - ((b[0] - a[0]) * (c[2] - a[2]));
		final double nz = ((b[0] - a[0]) * (c[1] - a[1])) - ((b[1] - a[1]) * (c[0] - a[0]));
		final double nlen = Math.sqrt((nx * nx) + (ny * ny) + (nz * nz));
		return (nlen > 0) && ((Math.abs(nz) / nlen) < GeoConfig.WALL_NZ);
	}
}
