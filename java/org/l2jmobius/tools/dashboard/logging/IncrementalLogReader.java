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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads appended UTF-8 log bytes with a channel cursor and preserves a trailing partial line between polls.
 * @author BazookaRpm
 */
public final class IncrementalLogReader
{
	private final Path _file;
	private long _cursor;
	private String _partialLine = "";
	
	public IncrementalLogReader(Path file, boolean beginAtEnd)
	{
		_file = file;
		if (beginAtEnd)
		{
			try
			{
				_cursor = Files.size(file);
			}
			catch (IOException exception)
			{
				_cursor = 0;
			}
		}
	}
	
	public synchronized List<String> poll() throws IOException
	{
		final List<String> result = new ArrayList<>();
		if (!Files.exists(_file))
		{
			_cursor = 0;
			_partialLine = "";
			return result;
		}
		
		final long length = Files.size(_file);
		if (length < _cursor)
		{
			_cursor = 0;
			_partialLine = "";
		}
		
		final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (SeekableByteChannel channel = Files.newByteChannel(_file, StandardOpenOption.READ))
		{
			channel.position(_cursor);
			final ByteBuffer buffer = ByteBuffer.allocate(8192);
			while (channel.read(buffer) > 0)
			{
				buffer.flip();
				bytes.write(buffer.array(), buffer.position(), buffer.remaining());
				buffer.clear();
			}
			
			_cursor = channel.position();
		}
		
		if (bytes.size() == 0)
		{
			return result;
		}
		
		final String combined = _partialLine + bytes.toString(StandardCharsets.UTF_8);
		final String normalized = combined.replace("\r\n", "\n").replace('\r', '\n');
		final String[] segments = normalized.split("\n", -1);
		for (int index = 0; index < (segments.length - 1); index++)
		{
			result.add(segments[index]);
		}
		
		_partialLine = segments[segments.length - 1];
		return result;
	}
}
