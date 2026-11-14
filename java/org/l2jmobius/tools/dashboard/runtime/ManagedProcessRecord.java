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
package org.l2jmobius.tools.dashboard.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.Properties;

import org.l2jmobius.tools.dashboard.bridge.DashboardProfile;

/**
 * Stores enough process identity data to avoid attaching to a reused operating-system PID.
 * @author BazookaRpm
 */
public final class ManagedProcessRecord
{
	private ManagedProcessRecord()
	{
	}
	
	public static Optional<ProcessHandle> find(Path recordFile, Path expectedJar)
	{
		if (!Files.isRegularFile(recordFile))
		{
			return Optional.empty();
		}
		
		final Properties values = new Properties();
		try (InputStream input = Files.newInputStream(recordFile))
		{
			values.load(input);
			final long pid = Long.parseLong(values.getProperty("pid", "-1"));
			final long expectedStart = Long.parseLong(values.getProperty("startEpochMillis", "-1"));
			final ProcessHandle handle = ProcessHandle.of(pid).orElse(null);
			if ((handle == null) || !handle.isAlive())
			{
				return Optional.empty();
			}
			
			if (!matchesStartTime(handle, expectedStart))
			{
				return Optional.empty();
			}
			
			if (!matchesCommand(handle, expectedJar))
			{
				return Optional.empty();
			}
			
			return Optional.of(handle);
		}
		catch (Exception e)
		{
			return Optional.empty();
		}
	}
	
	public static void store(Path recordFile, ProcessHandle process, Path expectedJar) throws IOException
	{
		Files.createDirectories(recordFile.getParent());
		final Properties values = new Properties();
		values.setProperty("pid", Long.toString(process.pid()));
		values.setProperty("jar", expectedJar.toAbsolutePath().normalize().toString());
		final Instant started = process.info().startInstant().orElse(Instant.now());
		values.setProperty("startEpochMillis", Long.toString(started.toEpochMilli()));
		try (OutputStream output = Files.newOutputStream(recordFile))
		{
			values.store(output, DashboardProfile.WINDOW_TITLE + " process identity");
		}
	}
	
	public static void remove(Path recordFile)
	{
		try
		{
			Files.deleteIfExists(recordFile);
		}
		catch (IOException e)
		{
			// A stale record is rejected on the next identity check.
		}
	}
	
	private static boolean matchesStartTime(ProcessHandle process, long expectedStart)
	{
		if (expectedStart < 0)
		{
			return false;
		}
		
		final Instant actual = process.info().startInstant().orElse(null);
		return (actual != null) && (Math.abs(actual.toEpochMilli() - expectedStart) <= 2000);
	}
	
	private static boolean matchesCommand(ProcessHandle process, Path expectedJar)
	{
		final String commandLine = process.info().commandLine().orElse("");
		if (commandLine.isEmpty())
		{
			return true;
		}
		
		return commandLine.contains(expectedJar.getFileName().toString());
	}
}
