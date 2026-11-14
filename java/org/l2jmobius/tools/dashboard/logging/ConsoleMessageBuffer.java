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
package org.l2jmobius.tools.dashboard.logging;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores one bounded console history per managed service with constant-time append and eviction. <br>
 * Monotonic sequence numbers let visible Swing views merge a snapshot with live messages without duplicates.
 * @author BazookaRpm
 */
public final class ConsoleMessageBuffer
{
	private final int _capacity;
	private final ArrayDeque<ConsoleEntry> _entries;
	private long _sequence;
	private long _information;
	private long _warnings;
	private long _errors;
	private long _trace;
	
	public ConsoleMessageBuffer(int capacity)
	{
		_capacity = Math.max(100, capacity);
		_entries = new ArrayDeque<>(Math.min(_capacity, 4096));
	}
	
	public synchronized ConsoleEntry append(ConsoleMessage message)
	{
		final ConsoleEntry entry = new ConsoleEntry(++_sequence, message);
		_entries.addLast(entry);
		while (_entries.size() > _capacity)
		{
			_entries.removeFirst();
		}
		
		increment(message == null ? MessageSeverity.INFO : message.severity());
		return entry;
	}
	
	public synchronized List<ConsoleEntry> snapshot()
	{
		return new ArrayList<>(_entries);
	}
	
	public synchronized ConsoleCounts counts()
	{
		return new ConsoleCounts(_information, _warnings, _errors, _trace);
	}
	
	public synchronized void clear()
	{
		_entries.clear();
		_information = 0;
		_warnings = 0;
		_errors = 0;
		_trace = 0;
	}
	
	public synchronized long latestSequence()
	{
		return _sequence;
	}
	
	public int capacity()
	{
		return _capacity;
	}
	
	private void increment(MessageSeverity severity)
	{
		switch (severity)
		{
			case WARNING:
			{
				_warnings++;
				break;
			}
			case ERROR:
			{
				_errors++;
				break;
			}
			case TRACE:
			{
				_trace++;
				break;
			}
			default:
			{
				_information++;
				break;
			}
		}
	}
	
	/**
	 * Associates a console message with its monotonic service-local sequence number.
	 * @param sequence service-local sequence number
	 * @param message normalized console message
	 * @author BazookaRpm
	 */
	public record ConsoleEntry(long sequence, ConsoleMessage message)
	{
	}
	
	/**
	 * Captures accumulated message counts without requiring UI observers to process every console line.
	 * @param information information message count
	 * @param warnings warning message count
	 * @param errors error message count
	 * @param trace trace/system message count
	 * @author BazookaRpm
	 */
	public record ConsoleCounts(long information, long warnings, long errors, long trace)
	{
	}
}
