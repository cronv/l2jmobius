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
package org.l2jmobius.tools.editor.npc.model;

import java.util.Objects;

/**
 * Editable minion entry contained by one named minion set.
 * @author BazookaRpm
 */
public final class NpcMinionEntry
{
	private final int _originalIndex;
	private final int _originalId;
	private final int _originalCount;
	private final Integer _originalMax;
	private final int _originalRespawnTime;
	private final Integer _originalWeightPoint;
	private int _id;
	private int _count;
	private Integer _max;
	private int _respawnTime;
	private Integer _weightPoint;
	private boolean _removed;
	
	public NpcMinionEntry(int originalIndex, int id, int count, Integer max, int respawnTime, Integer weightPoint)
	{
		_originalIndex = originalIndex;
		_originalId = id;
		_originalCount = count;
		_originalMax = max;
		_originalRespawnTime = respawnTime;
		_originalWeightPoint = weightPoint;
		_id = id;
		_count = count;
		_max = max;
		_respawnTime = respawnTime;
		_weightPoint = weightPoint;
	}
	
	public static NpcMinionEntry create(int id, int count, Integer max, int respawnTime, Integer weightPoint)
	{
		return new NpcMinionEntry(-1, id, count, max, respawnTime, weightPoint);
	}
	
	public int getOriginalIndex()
	{
		return _originalIndex;
	}
	
	public int getOriginalId()
	{
		return _originalId;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public void setId(int id)
	{
		_id = id;
	}
	
	public int getCount()
	{
		return _count;
	}
	
	public void setCount(int count)
	{
		_count = count;
	}
	
	public Integer getMax()
	{
		return _max;
	}
	
	public void setMax(Integer max)
	{
		_max = max;
	}
	
	public int getRespawnTime()
	{
		return _respawnTime;
	}
	
	public void setRespawnTime(int respawnTime)
	{
		_respawnTime = respawnTime;
	}
	
	public Integer getWeightPoint()
	{
		return _weightPoint;
	}
	
	public void setWeightPoint(Integer weightPoint)
	{
		_weightPoint = weightPoint;
	}
	
	public boolean isNew()
	{
		return _originalIndex < 0;
	}
	
	public boolean isRemoved()
	{
		return _removed;
	}
	
	public void setRemoved(boolean removed)
	{
		_removed = removed;
	}
	
	public boolean isChanged()
	{
		return isNew() || _removed || (_id != _originalId) || (_count != _originalCount) || !Objects.equals(_max, _originalMax) || (_respawnTime != _originalRespawnTime) || !Objects.equals(_weightPoint, _originalWeightPoint);
	}
	
	public void revert()
	{
		_id = _originalId;
		_count = _originalCount;
		_max = _originalMax;
		_respawnTime = _originalRespawnTime;
		_weightPoint = _originalWeightPoint;
		_removed = false;
	}
}
