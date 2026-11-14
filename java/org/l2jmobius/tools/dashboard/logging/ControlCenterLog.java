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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Writes a small append-only operator log without modifying server logging configuration.
 * @author BazookaRpm
 */
public final class ControlCenterLog
{
	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
	private static Path _file;
	
	private ControlCenterLog()
	{
	}
	
	public static synchronized void initialize(Path file)
	{
		_file = file;
		try
		{
			// A path with no parent would throw a NullPointerException past the catch below.
			final Path folder = file.getParent();
			if (folder != null)
			{
				Files.createDirectories(folder);
			}
		}
		catch (IOException e)
		{
			_file = null;
		}
	}
	
	public static synchronized void write(String message)
	{
		if (_file == null)
		{
			return;
		}
		
		final String line = FORMAT.format(Instant.now()) + " | " + message + System.lineSeparator();
		try
		{
			Files.writeString(_file, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		}
		catch (IOException e)
		{
			// Operator logging must never terminate process supervision.
		}
	}
}
