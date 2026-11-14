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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldRegion;
import org.l2jmobius.gameserver.entity.actor.enums.creature.FenceState;
import org.l2jmobius.gameserver.entity.actor.instance.Fence;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Loads fence definitions from XML and tracks spawned {@code Fence} objects.
 * @author HorridoJoho, BazookaRpm
 */
public class FenceData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(FenceData.class.getSimpleName());
	
	private static final int MAX_Z_DIFF = 100;
	
	private final Map<Integer, Fence> _fences = new ConcurrentHashMap<>();
	private final Set<Integer> _dataPackFences = ConcurrentHashMap.newKeySet();
	
	/**
	 * Creates the {@code FenceData} singleton and loads the fence data.
	 */
	protected FenceData()
	{
		load();
	}
	
	/**
	 * Clears existing fences and reloads them from {@code data/FenceData.xml}.
	 */
	@Override
	public synchronized void load()
	{
		// Only the fences spawned from this file are removed; admin and instance fences stay registered.
		for (Integer objectId : _dataPackFences)
		{
			final Fence fence = _fences.get(objectId);
			if (fence != null)
			{
				fence.deleteMe();
			}
		}
		
		_dataPackFences.clear();
		parseDatapackFile("data/FenceData.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _dataPackFences.size() + " fences.");
	}
	
	/**
	 * Parses the fence list from the given XML document.
	 * @param document The XML {@code Document} to parse.
	 * @param file The source {@code File}.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "fence", this::spawnFence));
	}
	
	/**
	 * Spawns a fence parsed from the given XML node.
	 * @param fenceNode The {@code Node} holding the fence attributes.
	 */
	private void spawnFence(Node fenceNode)
	{
		final StatSet set = new StatSet(parseAttributes(fenceNode));
		final Fence fence = spawnFence(set.getInt("x"), set.getInt("y"), set.getInt("z"), set.getString("name"), set.getInt("width"), set.getInt("length"), set.getInt("height"), 0, set.getEnum("state", FenceState.class, FenceState.CLOSED));
		_dataPackFences.add(fence.getObjectId());
	}
	
	/**
	 * Spawns an unnamed {@code Fence} at the given position and registers it.
	 * @param x The X coordinate.
	 * @param y The Y coordinate.
	 * @param z The Z coordinate.
	 * @param width The fence width.
	 * @param length The fence length.
	 * @param height The fence height.
	 * @param instanceId The instance id, or {@code 0} for none.
	 * @param state The initial {@code FenceState}.
	 * @return The spawned {@code Fence}.
	 */
	public Fence spawnFence(int x, int y, int z, int width, int length, int height, int instanceId, FenceState state)
	{
		return spawnFence(x, y, z, null, width, length, height, instanceId, state);
	}
	
	/**
	 * Spawns a {@code Fence} at the given position and registers it.
	 * @param x The X coordinate.
	 * @param y The Y coordinate.
	 * @param z The Z coordinate.
	 * @param name The fence name.
	 * @param width The fence width.
	 * @param length The fence length.
	 * @param height The fence height.
	 * @param instanceId The instance id, or {@code 0} for none.
	 * @param state The initial {@code FenceState}.
	 * @return The spawned {@code Fence}.
	 */
	public Fence spawnFence(int x, int y, int z, String name, int width, int length, int height, int instanceId, FenceState state)
	{
		final Fence fence = new Fence(x, y, name, width, length, height, state);
		if (instanceId > 0)
		{
			fence.setInstanceById(instanceId);
		}
		
		fence.spawnMe(x, y, z);
		addFence(fence);
		
		return fence;
	}
	
	/**
	 * Registers a {@code Fence}, keyed by its object id.
	 * @param fence The {@code Fence} to add.
	 */
	private void addFence(Fence fence)
	{
		_fences.put(fence.getObjectId(), fence);
	}
	
	/**
	 * Removes a {@code Fence} from the registry.
	 * @param fence The {@code Fence} to remove.
	 */
	public void removeFence(Fence fence)
	{
		_fences.remove(fence.getObjectId());
	}
	
	/**
	 * Retrieves all loaded fences keyed by object id.
	 * @return A {@code Map} of object id to {@code Fence}.
	 */
	public Map<Integer, Fence> getFences()
	{
		return _fences;
	}
	
	/**
	 * Retrieves the fence with the given object id.
	 * @param objectId The fence object id.
	 * @return The {@code Fence}, or {@code null} if absent.
	 */
	public Fence getFence(int objectId)
	{
		return _fences.get(objectId);
	}
	
	/**
	 * Retrieves the number of loaded fences.
	 * @return The fence count as an {@code int}.
	 */
	public int getLoadedElementsCount()
	{
		return _fences.size();
	}
	
	/**
	 * Checks whether a geodata-enabled fence in the given instance blocks the segment between two points.
	 * @param x The start X coordinate.
	 * @param y The start Y coordinate.
	 * @param z The start Z coordinate.
	 * @param tx The target X coordinate.
	 * @param ty The target Y coordinate.
	 * @param tz The target Z coordinate.
	 * @param instance The {@code Instance} to check within.
	 * @return {@code true} if a fence blocks the segment, {@code false} otherwise.
	 */
	public boolean checkIfFenceBetween(int x, int y, int z, int tx, int ty, int tz, Instance instance)
	{
		final WorldRegion region = World.getRegion(x, y, z);
		final Collection<Fence> fences = region != null ? region.getFences() : null;
		if ((fences == null) || fences.isEmpty())
		{
			return false;
		}
		
		for (Fence fence : fences)
		{
			// Check if fence is geodata enabled.
			if (!fence.getState().isGeodataEnabled())
			{
				continue;
			}
			
			// Check if fence is within the instance we search for.
			final int instanceId = (instance == null) ? 0 : instance.getId();
			if (fence.getInstanceId() != instanceId)
			{
				continue;
			}
			
			final int xMin = fence.getXMin();
			if ((x < xMin) && (tx < xMin))
			{
				continue;
			}
			
			final int xMax = fence.getXMax();
			if ((x > xMax) && (tx > xMax))
			{
				continue;
			}
			
			final int yMin = fence.getYMin();
			if ((y < yMin) && (ty < yMin))
			{
				continue;
			}
			
			final int yMax = fence.getYMax();
			if ((y > yMax) && (ty > yMax))
			{
				continue;
			}
			
			if ((x > xMin) && (tx > xMin) && (x < xMax) && (tx < xMax) && (y > yMin) && (ty > yMin) && (y < yMax) && (ty < yMax))
			{
				continue;
			}
			
			final int fenceZ = fence.getZ();
			if ((crossLinePart(xMin, yMin, xMax, yMin, x, y, tx, ty, xMin, yMin, xMax, yMax) || crossLinePart(xMax, yMin, xMax, yMax, x, y, tx, ty, xMin, yMin, xMax, yMax) || crossLinePart(xMax, yMax, xMin, yMax, x, y, tx, ty, xMin, yMin, xMax, yMax) || crossLinePart(xMin, yMax, xMin, yMin, x, y, tx, ty, xMin, yMin, xMax, yMax)) && (z > (fenceZ - MAX_Z_DIFF)) && (z < (fenceZ + MAX_Z_DIFF)))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks whether the segment crosses a fence edge within the bounding box.
	 * @param x1 The first edge start X.
	 * @param y1 The first edge start Y.
	 * @param x2 The first edge end X.
	 * @param y2 The first edge end Y.
	 * @param x3 The segment start X.
	 * @param y3 The segment start Y.
	 * @param x4 The segment end X.
	 * @param y4 The segment end Y.
	 * @param xMin The bounding box minimum X.
	 * @param yMin The bounding box minimum Y.
	 * @param xMax The bounding box maximum X.
	 * @param yMax The bounding box maximum Y.
	 * @return {@code true} if the intersection falls within the box, {@code false} otherwise.
	 */
	private boolean crossLinePart(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4, double xMin, double yMin, double xMax, double yMax)
	{
		final double[] result = intersection(x1, y1, x2, y2, x3, y3, x4, y4);
		if (result == null)
		{
			return false;
		}
		
		// Both axes must contain the crossing point; testing them separately blocked segments that never touched the fence.
		final double xCross = result[0];
		final double yCross = result[1];
		return (xCross <= xMax) && (xCross >= xMin) && (yCross <= yMax) && (yCross >= yMin);
	}
	
	/**
	 * Calculates the intersection point of two lines.
	 * @param x1 The first line start X.
	 * @param y1 The first line start Y.
	 * @param x2 The first line end X.
	 * @param y2 The first line end Y.
	 * @param x3 The second line start X.
	 * @param y3 The second line start Y.
	 * @param x4 The second line end X.
	 * @param y4 The second line end Y.
	 * @return The {@code [x, y]} intersection, or {@code null} if the lines are parallel.
	 */
	private double[] intersection(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4)
	{
		final double d = ((x1 - x2) * (y3 - y4)) - ((y1 - y2) * (x3 - x4));
		if (d == 0)
		{
			return null;
		}
		
		final double xi = (((x3 - x4) * ((x1 * y2) - (y1 * x2))) - ((x1 - x2) * ((x3 * y4) - (y3 * x4)))) / d;
		final double yi = (((y3 - y4) * ((x1 * y2) - (y1 * x2))) - ((y1 - y2) * ((x3 * y4) - (y3 * x4)))) / d;
		return new double[]
		{
			xi,
			yi
		};
	}
	
	/**
	 * Retrieves the singleton {@code FenceData} instance.
	 * @return The single {@code FenceData} instance.
	 */
	public static FenceData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final FenceData INSTANCE = new FenceData();
	}
}
