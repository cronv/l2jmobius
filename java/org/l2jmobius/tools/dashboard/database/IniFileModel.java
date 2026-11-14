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
package org.l2jmobius.tools.dashboard.database;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Preserves comments and ordering while reading or replacing simple key-value INI properties.
 * @author BazookaRpm
 */
public final class IniFileModel
{
	private final Path _source;
	private final List<String> _lines;
	private final Map<String, Integer> _keyLines;
	
	private IniFileModel(Path source, List<String> lines, Map<String, Integer> keyLines)
	{
		_source = source;
		_lines = lines;
		_keyLines = keyLines;
	}
	
	public static IniFileModel load(Path source) throws IOException
	{
		final List<String> lines = Files.readAllLines(source, StandardCharsets.UTF_8);
		final Map<String, Integer> keyLines = new LinkedHashMap<>();
		for (int index = 0; index < lines.size(); index++)
		{
			final String line = lines.get(index).trim();
			if (line.isEmpty() || line.startsWith("#") || line.startsWith(";"))
			{
				continue;
			}
			
			final int separator = line.indexOf('=');
			if (separator > 0)
			{
				keyLines.put(line.substring(0, separator).trim().toLowerCase(Locale.ROOT), index);
			}
		}
		
		return new IniFileModel(source, new ArrayList<>(lines), keyLines);
	}
	
	public String get(String key, String fallback)
	{
		final Integer lineIndex = _keyLines.get(key.toLowerCase(Locale.ROOT));
		if (lineIndex == null)
		{
			return fallback;
		}
		
		final String line = _lines.get(lineIndex);
		final int separator = line.indexOf('=');
		return (separator < 0) ? fallback : line.substring(separator + 1).trim();
	}
	
	public byte[] withValues(Map<String, String> replacements)
	{
		final List<String> output = new ArrayList<>(_lines);
		for (Entry<String, String> entry : replacements.entrySet())
		{
			final String key = entry.getKey();
			final Integer lineIndex = _keyLines.get(key.toLowerCase(Locale.ROOT));
			if (lineIndex == null)
			{
				output.add(key + " = " + entry.getValue());
			}
			else
			{
				final String existing = output.get(lineIndex);
				final int separator = existing.indexOf('=');
				final String prefix = (separator >= 0) ? existing.substring(0, separator + 1) : entry.getKey() + " =";
				output.set(lineIndex, prefix + " " + entry.getValue());
			}
		}
		
		return String.join(System.lineSeparator(), output).concat(System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
	}
	
	public Path source()
	{
		return _source;
	}
}
