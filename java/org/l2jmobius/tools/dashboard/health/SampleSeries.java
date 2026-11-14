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
package org.l2jmobius.tools.dashboard.health;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.l2jmobius.tools.dashboard.telemetry.RuntimeSnapshot;

/**
 * Maintains a fixed-size circular telemetry history with interval-based sampling.
 * @author BazookaRpm
 */
public final class SampleSeries
{
	private final RuntimeSample[] _samples;
	private final long _minimumIntervalMillis;
	private int _nextIndex;
	private int _size;
	private long _lastSampleTime;
	
	public SampleSeries(int capacity, long minimumIntervalMillis)
	{
		_samples = new RuntimeSample[Math.max(1, capacity)];
		_minimumIntervalMillis = Math.max(0, minimumIntervalMillis);
	}
	
	public synchronized boolean offer(RuntimeSnapshot snapshot)
	{
		if ((snapshot == null) || !snapshot.available())
		{
			return false;
		}
		
		final long now = System.currentTimeMillis();
		if ((_lastSampleTime > 0) && ((now - _lastSampleTime) < _minimumIntervalMillis))
		{
			return false;
		}
		
		_samples[_nextIndex] = RuntimeSample.from(now, snapshot);
		_nextIndex = (_nextIndex + 1) % _samples.length;
		if (_size < _samples.length)
		{
			_size++;
		}
		
		_lastSampleTime = now;
		return true;
	}
	
	public synchronized List<RuntimeSample> snapshot()
	{
		if (_size == 0)
		{
			return Collections.emptyList();
		}
		
		final List<RuntimeSample> result = new ArrayList<>(_size);
		final int start = (_size == _samples.length) ? _nextIndex : 0;
		for (int offset = 0; offset < _size; offset++)
		{
			result.add(_samples[(start + offset) % _samples.length]);
		}
		
		return result;
	}
	
	public synchronized int size()
	{
		return _size;
	}
	
	public int capacity()
	{
		return _samples.length;
	}
}
