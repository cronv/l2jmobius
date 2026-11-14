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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves MySQL client tools from the configured directory and common local installations.
 * @author BazookaRpm
 */
public final class MySqlBinaryResolver
{
	private MySqlBinaryResolver()
	{
	}
	
	public static Path resolve(DatabaseEndpoint endpoint, String toolName)
	{
		final String executable = isWindows() ? toolName + ".exe" : toolName;
		final List<Path> candidates = new ArrayList<>();
		if ((endpoint.mysqlBinDirectory() != null) && !endpoint.mysqlBinDirectory().isBlank())
		{
			candidates.add(Paths.get(endpoint.mysqlBinDirectory()).resolve(executable));
		}
		
		final String path = System.getenv("PATH");
		if (path != null)
		{
			for (String part : path.split(File.pathSeparator))
			{
				if (!part.isBlank())
				{
					candidates.add(Paths.get(part).resolve(executable));
				}
			}
		}
		
		if (isWindows())
		{
			candidates.add(Paths.get("C:/xampp/mysql/bin").resolve(executable));
			candidates.add(Paths.get("C:/Program Files/MariaDB 11.4/bin").resolve(executable));
			candidates.add(Paths.get("C:/Program Files/MySQL/MySQL Server 8.4/bin").resolve(executable));
		}
		else
		{
			candidates.add(Paths.get("/usr/bin").resolve(executable));
			candidates.add(Paths.get("/usr/local/bin").resolve(executable));
		}
		
		for (Path candidate : candidates)
		{
			if (Files.isRegularFile(candidate))
			{
				return candidate.toAbsolutePath().normalize();
			}
		}
		
		return null;
	}
	
	private static boolean isWindows()
	{
		return System.getProperty("os.name", "").toLowerCase().contains("win");
	}
}
