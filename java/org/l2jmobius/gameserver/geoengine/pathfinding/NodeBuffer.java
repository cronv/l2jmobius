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
package org.l2jmobius.gameserver.geoengine.pathfinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.l2jmobius.gameserver.config.GeoEngineConfig;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.geoengine.regions.Region;

/**
 * A reusable A* search buffer backed by parallel primitive arrays; not thread-safe.
 * @author Mobius
 */
public class NodeBuffer
{
	private static final int MAX_ITERATIONS = 7000;
	private static final int COST_SCALE = 1000;
	private static final int Z_TOLERANCE = 64;
	private static final int Z_STEP_LIMIT = 16;
	
	private static final byte FLAG_TOUCHED = 0x1;
	private static final byte FLAG_OPEN = 0x2;
	private static final byte FLAG_CLOSED = 0x4;
	
	private final int _mapSize;
	private final int _capacity;
	private final int _maxIterations;
	
	// Scaled weight constants captured at construction.
	private final int _wLow;
	private final int _wMedium;
	private final int _wHigh;
	private final int _wDiagonal;
	private final int _hStraight; // octile straight step cost
	private final int _hDiagonalExcess; // octile diagonal step extra cost (diag - straight)
	private final boolean _diagonalStrategy;
	
	// Cell state (indexed by cellIdx).
	private final int[] _parent;
	private final int[] _gCost;
	private final int[] _hCost;
	private final int[] _fCost;
	private final byte[] _flags;
	private final byte[] _nswe;
	private final short[] _z;
	
	// Touched-cell ledger so free() runs in O(touched).
	private final int[] _touched;
	private int _touchedCount;
	
	// Primitive indexed binary min-heap.
	private final int[] _heap;
	private final int[] _heapPos;
	private int _heapSize;
	
	private int _baseX;
	private int _baseY;
	private int _targetX;
	private int _targetY;
	private int _targetZ;
	private List<PathBlocker> _blockers;
	
	/**
	 * Creates a {@code NodeBuffer} sized for a square map and loads the weight constants from config.
	 * @param size The length of one side of the map.
	 */
	public NodeBuffer(int size)
	{
		_mapSize = size;
		_capacity = size * size;
		_maxIterations = Math.max(MAX_ITERATIONS, _capacity / 4);
		_parent = new int[_capacity];
		_gCost = new int[_capacity];
		_hCost = new int[_capacity];
		_fCost = new int[_capacity];
		_flags = new byte[_capacity];
		_nswe = new byte[_capacity];
		_z = new short[_capacity];
		_touched = new int[_capacity];
		_heap = new int[_capacity];
		_heapPos = new int[_capacity];
		
		_wLow = (int) (GeoEngineConfig.LOW_WEIGHT * COST_SCALE);
		_wMedium = (int) (GeoEngineConfig.MEDIUM_WEIGHT * COST_SCALE);
		_wHigh = (int) (GeoEngineConfig.HIGH_WEIGHT * COST_SCALE);
		_wDiagonal = (int) (GeoEngineConfig.DIAGONAL_WEIGHT * COST_SCALE);
		_hStraight = _wLow;
		_hDiagonalExcess = _wDiagonal - _wLow;
		_diagonalStrategy = GeoEngineConfig.ADVANCED_DIAGONAL_STRATEGY;
	}
	
	/**
	 * Retrieves the side length of the map this buffer was sized for.
	 * @return The map size as an {@code int}.
	 */
	public int getMapSize()
	{
		return _mapSize;
	}
	
	/**
	 * Searches for an A* path from the start to the target coordinate.
	 * @param x The start X coordinate.
	 * @param y The start Y coordinate.
	 * @param z The start Z coordinate.
	 * @param tx The target X coordinate.
	 * @param ty The target Y coordinate.
	 * @param tz The target Z coordinate.
	 * @param partial {@code true} to return a path to the reachable point closest to the target when the target itself cannot be reached.
	 * @param blockers The doors and fences inside the search window, or {@code null} when there are none.
	 * @return The path as a {@code List} of {@code GeoLocation}, or {@code null} if none is found.
	 */
	public List<GeoLocation> findPath(int x, int y, int z, int tx, int ty, int tz, boolean partial, List<PathBlocker> blockers)
	{
		_baseX = x + ((tx - x - _mapSize) / 2);
		_baseY = y + ((ty - y - _mapSize) / 2);
		_targetX = tx;
		_targetY = ty;
		_targetZ = tz;
		_blockers = blockers;
		
		final int startIdx = cellIdx(x, y);
		if (startIdx < 0)
		{
			return null;
		}
		
		touch(startIdx, x, y, z);
		_gCost[startIdx] = 0;
		_fCost[startIdx] = _hCost[startIdx];
		heapPush(startIdx);
		
		int bestIdx = startIdx;
		int bestH = _hCost[startIdx];
		
		for (int count = 0; count < _maxIterations; count++)
		{
			if (_heapSize == 0)
			{
				break;
			}
			
			final int currIdx = heapPoll();
			_flags[currIdx] &= ~FLAG_OPEN;
			_flags[currIdx] |= FLAG_CLOSED;
			
			final int currX = idxToX(currIdx);
			final int currY = idxToY(currIdx);
			final int currZ = _z[currIdx];
			
			if ((currX == _targetX) && (currY == _targetY) && (Math.abs(currZ - _targetZ) < Z_TOLERANCE))
			{
				return constructPath(currIdx);
			}
			
			if (_hCost[currIdx] < bestH)
			{
				bestH = _hCost[currIdx];
				bestIdx = currIdx;
			}
			
			expandNeighbors(currIdx, currX, currY, currZ);
		}
		
		// Target not reached; optionally settle for the closest point the search did reach.
		if (partial && (bestIdx != startIdx))
		{
			return constructPath(bestIdx);
		}
		
		return null;
	}
	
	/**
	 * Resets the buffer for reuse by clearing every touched cell and emptying the heap.
	 */
	public void free()
	{
		for (int i = 0; i < _touchedCount; i++)
		{
			_flags[_touched[i]] = 0;
		}
		
		_touchedCount = 0;
		_heapSize = 0;
		_blockers = null;
	}
	
	/**
	 * Converts a coordinate to its flat cell index, or {@code -1} if outside the map.
	 * @param gx The X coordinate.
	 * @param gy The Y coordinate.
	 * @return The cell index, or {@code -1} if out of bounds.
	 */
	private int cellIdx(int gx, int gy)
	{
		final int aX = gx - _baseX;
		if ((aX < 0) || (aX >= _mapSize))
		{
			return -1;
		}
		
		final int aY = gy - _baseY;
		if ((aY < 0) || (aY >= _mapSize))
		{
			return -1;
		}
		
		return (aY * _mapSize) + aX;
	}
	
	/**
	 * Converts a flat cell index back to its X coordinate.
	 * @param idx The flat cell index.
	 * @return The X coordinate as an {@code int}.
	 */
	private int idxToX(int idx)
	{
		return _baseX + (idx % _mapSize);
	}
	
	/**
	 * Converts a flat cell index back to its Y coordinate.
	 * @param idx The flat cell index.
	 * @return The Y coordinate as an {@code int}.
	 */
	private int idxToY(int idx)
	{
		return _baseY + (idx / _mapSize);
	}
	
	/**
	 * Initializes a cell on first visit, caching its NSWE flags, height and heuristic cost.
	 * @param idx The flat cell index.
	 * @param gx The X coordinate.
	 * @param gy The Y coordinate.
	 * @param refZ The reference Z coordinate.
	 */
	private void touch(int idx, int gx, int gy, int refZ)
	{
		if ((_flags[idx] & FLAG_TOUCHED) != 0)
		{
			return;
		}
		
		final GeoEngine geo = GeoEngine.getInstance();
		byte nswe = 0;
		if (geo.checkNearestNswe(gx, gy, refZ, Region.NSWE_NORTH))
		{
			nswe |= Region.NSWE_NORTH;
		}
		
		if (geo.checkNearestNswe(gx, gy, refZ, Region.NSWE_EAST))
		{
			nswe |= Region.NSWE_EAST;
		}
		
		if (geo.checkNearestNswe(gx, gy, refZ, Region.NSWE_SOUTH))
		{
			nswe |= Region.NSWE_SOUTH;
		}
		
		if (geo.checkNearestNswe(gx, gy, refZ, Region.NSWE_WEST))
		{
			nswe |= Region.NSWE_WEST;
		}
		
		_nswe[idx] = nswe;
		_z[idx] = (short) geo.getNearestZ(gx, gy, refZ);
		_flags[idx] = FLAG_TOUCHED;
		_parent[idx] = -1;
		_gCost[idx] = Integer.MAX_VALUE;
		_hCost[idx] = computeH(gx, gy);
		_fCost[idx] = Integer.MAX_VALUE;
		_heapPos[idx] = -1;
		
		_touched[_touchedCount++] = idx;
	}
	
	/**
	 * Computes the octile-distance heuristic to the target.
	 * @param gx The X coordinate.
	 * @param gy The Y coordinate.
	 * @return The heuristic cost as an {@code int}.
	 */
	private int computeH(int gx, int gy)
	{
		final int dx = Math.abs(gx - _targetX);
		final int dy = Math.abs(gy - _targetY);
		final int maxD = Math.max(dx, dy);
		final int minD = Math.min(dx, dy);
		return (maxD * _hStraight) + (minD * _hDiagonalExcess);
	}
	
	/**
	 * Considers the cardinal, and when enabled diagonal, neighbors reachable from the current cell.
	 * @param currIdx The flat index of the current cell.
	 * @param x The X coordinate of the current cell.
	 * @param y The Y coordinate of the current cell.
	 * @param z The Z coordinate of the current cell.
	 */
	private void expandNeighbors(int currIdx, int x, int y, int z)
	{
		final byte nswe = _nswe[currIdx];
		if (nswe == 0)
		{
			return;
		}
		
		int idxE = -1;
		int idxS = -1;
		int idxW = -1;
		int idxN = -1;
		
		if ((nswe & Region.NSWE_EAST) != 0)
		{
			idxE = considerNeighbor(currIdx, x + 1, y, z, false);
		}
		
		if ((nswe & Region.NSWE_SOUTH) != 0)
		{
			idxS = considerNeighbor(currIdx, x, y + 1, z, false);
		}
		
		if ((nswe & Region.NSWE_WEST) != 0)
		{
			idxW = considerNeighbor(currIdx, x - 1, y, z, false);
		}
		
		if ((nswe & Region.NSWE_NORTH) != 0)
		{
			idxN = considerNeighbor(currIdx, x, y - 1, z, false);
		}
		
		if (!_diagonalStrategy)
		{
			return;
		}
		
		if ((idxE >= 0) && (idxS >= 0) && ((_nswe[idxE] & Region.NSWE_SOUTH) != 0) && ((_nswe[idxS] & Region.NSWE_EAST) != 0))
		{
			considerNeighbor(currIdx, x + 1, y + 1, z, true);
		}
		
		if ((idxS >= 0) && (idxW >= 0) && ((_nswe[idxW] & Region.NSWE_SOUTH) != 0) && ((_nswe[idxS] & Region.NSWE_WEST) != 0))
		{
			considerNeighbor(currIdx, x - 1, y + 1, z, true);
		}
		
		if ((idxN >= 0) && (idxE >= 0) && ((_nswe[idxE] & Region.NSWE_NORTH) != 0) && ((_nswe[idxN] & Region.NSWE_EAST) != 0))
		{
			considerNeighbor(currIdx, x + 1, y - 1, z, true);
		}
		
		if ((idxN >= 0) && (idxW >= 0) && ((_nswe[idxW] & Region.NSWE_NORTH) != 0) && ((_nswe[idxN] & Region.NSWE_WEST) != 0))
		{
			considerNeighbor(currIdx, x - 1, y - 1, z, true);
		}
	}
	
	/**
	 * Relaxes a neighboring cell, updating its cost and heap position when a cheaper path is found.
	 * @param parentIdx The flat index of the current cell.
	 * @param nx The neighbor X coordinate.
	 * @param ny The neighbor Y coordinate.
	 * @param parentZ The Z coordinate of the current cell.
	 * @param diagonal {@code true} if the step to the neighbor is diagonal.
	 * @return The neighbor index, or {@code -1} if out of bounds.
	 */
	private int considerNeighbor(int parentIdx, int nx, int ny, int parentZ, boolean diagonal)
	{
		final int idx = cellIdx(nx, ny);
		if (idx < 0)
		{
			return -1;
		}
		
		touch(idx, nx, ny, parentZ);
		
		// Doors and fences are not part of geodata, so the step is tested against their outlines.
		// The neighbor is reported as unusable, which also stops diagonals from cutting the same corner.
		if ((_blockers != null) && isBlocked(idxToX(parentIdx), idxToY(parentIdx), parentZ, nx, ny, _z[idx]))
		{
			return -1;
		}
		
		if ((_flags[idx] & FLAG_CLOSED) != 0)
		{
			return idx;
		}
		
		final int neighborZ = _z[idx];
		final int stepZ = Math.abs(neighborZ - parentZ);
		
		final int weight;
		if (((_nswe[idx] & Region.NSWE_ALL) != Region.NSWE_ALL) || (stepZ > Z_STEP_LIMIT))
		{
			weight = _wHigh;
		}
		else if (isHighWeight(nx + 1, ny, neighborZ) || isHighWeight(nx - 1, ny, neighborZ) || isHighWeight(nx, ny + 1, neighborZ) || isHighWeight(nx, ny - 1, neighborZ))
		{
			weight = _wMedium;
		}
		else
		{
			weight = diagonal ? _wDiagonal : _wLow;
		}
		
		final int newGCost = _gCost[parentIdx] + weight;
		final boolean inOpen = (_flags[idx] & FLAG_OPEN) != 0;
		if (inOpen && (newGCost >= _gCost[idx]))
		{
			return idx;
		}
		
		_parent[idx] = parentIdx;
		_gCost[idx] = newGCost;
		_fCost[idx] = newGCost + _hCost[idx];
		if (inOpen)
		{
			heapSiftUp(_heapPos[idx]);
		}
		else
		{
			heapPush(idx);
		}
		
		return idx;
	}
	
	/**
	 * Checks whether a cell is high weight due to being out of bounds, blocked or a steep step.
	 * @param gx The X coordinate.
	 * @param gy The Y coordinate.
	 * @param refZ The reference Z coordinate.
	 * @return {@code true} if the cell is high weight, {@code false} otherwise.
	 */
	private boolean isHighWeight(int gx, int gy, int refZ)
	{
		final int idx = cellIdx(gx, gy);
		if (idx < 0)
		{
			return true;
		}
		
		touch(idx, gx, gy, refZ);
		return ((_nswe[idx] & Region.NSWE_ALL) != Region.NSWE_ALL) || (Math.abs(_z[idx] - refZ) > Z_STEP_LIMIT);
	}
	
	/**
	 * Checks whether the world segment of a cell step crosses a door or a fence.
	 * @param fromGeoX The step start geo X coordinate.
	 * @param fromGeoY The step start geo Y coordinate.
	 * @param fromZ The step start world Z coordinate.
	 * @param toGeoX The step end geo X coordinate.
	 * @param toGeoY The step end geo Y coordinate.
	 * @param toZ The step end world Z coordinate.
	 * @return {@code true} if the step is blocked, {@code false} otherwise.
	 */
	private boolean isBlocked(int fromGeoX, int fromGeoY, int fromZ, int toGeoX, int toGeoY, int toZ)
	{
		final int fromX = GeoEngine.getWorldX(fromGeoX);
		final int fromY = GeoEngine.getWorldY(fromGeoY);
		final int toX = GeoEngine.getWorldX(toGeoX);
		final int toY = GeoEngine.getWorldY(toGeoY);
		for (int i = 0; i < _blockers.size(); i++)
		{
			if (_blockers.get(i).intersects(fromX, fromY, fromZ, toX, toY, toZ))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Pushes a cell onto the open-set min-heap and marks it open.
	 * @param idx The flat cell index to push.
	 */
	private void heapPush(int idx)
	{
		final int pos = _heapSize++;
		_heap[pos] = idx;
		_heapPos[idx] = pos;
		_flags[idx] |= FLAG_OPEN;
		heapSiftUp(pos);
	}
	
	/**
	 * Removes and returns the lowest-cost cell from the open-set min-heap.
	 * @return The flat index of the polled cell.
	 */
	private int heapPoll()
	{
		final int root = _heap[0];
		_heapPos[root] = -1;
		final int lastPos = --_heapSize;
		if (lastPos > 0)
		{
			final int moved = _heap[lastPos];
			_heap[0] = moved;
			_heapPos[moved] = 0;
			heapSiftDown(0);
		}
		
		return root;
	}
	
	/**
	 * Restores the heap order by sifting an element toward the root.
	 * @param startPos The heap position to sift up from.
	 */
	private void heapSiftUp(int startPos)
	{
		int pos = startPos;
		final int idx = _heap[pos];
		final int fCost = _fCost[idx];
		final int hCost = _hCost[idx];
		while (pos > 0)
		{
			final int parentPos = (pos - 1) >>> 1;
			final int parentIdx = _heap[parentPos];
			final int parentF = _fCost[parentIdx];
			if ((fCost > parentF) || ((fCost == parentF) && (hCost >= _hCost[parentIdx])))
			{
				break;
			}
			
			_heap[pos] = parentIdx;
			_heapPos[parentIdx] = pos;
			pos = parentPos;
		}
		
		_heap[pos] = idx;
		_heapPos[idx] = pos;
	}
	
	/**
	 * Restores the heap order by sifting an element toward the leaves.
	 * @param startPos The heap position to sift down from.
	 */
	private void heapSiftDown(int startPos)
	{
		int pos = startPos;
		final int idx = _heap[pos];
		final int fCost = _fCost[idx];
		final int hCost = _hCost[idx];
		final int half = _heapSize >>> 1;
		while (pos < half)
		{
			int childPos = (pos << 1) + 1;
			int childIdx = _heap[childPos];
			int childF = _fCost[childIdx];
			final int rightPos = childPos + 1;
			if (rightPos < _heapSize)
			{
				final int rightIdx = _heap[rightPos];
				final int rightF = _fCost[rightIdx];
				if ((rightF < childF) || ((rightF == childF) && (_hCost[rightIdx] < _hCost[childIdx])))
				{
					childPos = rightPos;
					childIdx = rightIdx;
					childF = rightF;
				}
			}
			
			if ((fCost < childF) || ((fCost == childF) && (hCost <= _hCost[childIdx])))
			{
				break;
			}
			
			_heap[pos] = childIdx;
			_heapPos[childIdx] = pos;
			pos = childPos;
		}
		
		_heap[pos] = idx;
		_heapPos[idx] = pos;
	}
	
	/**
	 * Rebuilds the path by walking parents from the target, collapsing collinear points.
	 * @param endIdx The flat index of the target cell.
	 * @return The path as a {@code List} of {@code GeoLocation}, ordered start to target.
	 */
	private List<GeoLocation> constructPath(int endIdx)
	{
		final List<GeoLocation> reversed = new ArrayList<>();
		int prevDirX = Integer.MIN_VALUE;
		int prevDirY = Integer.MIN_VALUE;
		int curr = endIdx;
		
		while (_parent[curr] != -1)
		{
			final int parent = _parent[curr];
			final int grand = _parent[parent];
			final int currX = idxToX(curr);
			final int currY = idxToY(curr);
			final int parentX = idxToX(parent);
			final int parentY = idxToY(parent);
			
			int dirX;
			int dirY;
			if (!_diagonalStrategy && (grand != -1))
			{
				final int tmpX = currX - idxToX(grand);
				final int tmpY = currY - idxToY(grand);
				if (Math.abs(tmpX) == Math.abs(tmpY))
				{
					dirX = tmpX;
					dirY = tmpY;
				}
				else
				{
					dirX = currX - parentX;
					dirY = currY - parentY;
				}
			}
			else
			{
				dirX = currX - parentX;
				dirY = currY - parentY;
			}
			
			if ((dirX != prevDirX) || (dirY != prevDirY))
			{
				prevDirX = dirX;
				prevDirY = dirY;
				reversed.add(new GeoLocation(currX, currY, _z[curr]));
			}
			
			curr = parent;
		}
		
		Collections.reverse(reversed);
		return reversed;
	}
}
