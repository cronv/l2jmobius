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

import java.nio.file.Files;
import java.nio.file.Path;

import org.l2jmobius.tools.dashboard.settings.ControlCenterSettings;

/**
 * Resolves the effective client-facing port from the Interlude Server.ini file with a safe fallback.
 * @author BazookaRpm
 */
public final class ServicePortResolver
{
	private ServicePortResolver()
	{
	}
	
	public static int resolve(ControlCenterSettings settings, ServiceRole role)
	{
		final int configuredFallback = settings.getServicePort(role);
		final Path serverConfig = settings.getServerConfig(role);
		if (!Files.isRegularFile(serverConfig))
		{
			return configuredFallback;
		}
		
		try
		{
			for (String line : Files.readAllLines(serverConfig))
			{
				final String stripped = stripComment(line).trim();
				final int separator = stripped.indexOf('=');
				if (separator <= 0)
				{
					continue;
				}
				
				final String key = stripped.substring(0, separator).trim();
				if (!role.configPortKey().equalsIgnoreCase(key))
				{
					continue;
				}
				
				final int parsed = Integer.parseInt(stripped.substring(separator + 1).trim());
				if ((parsed > 0) && (parsed <= 65535))
				{
					return parsed;
				}
			}
		}
		catch (Exception e)
		{
			return configuredFallback;
		}
		
		return configuredFallback;
	}
	
	private static String stripComment(String line)
	{
		final int hash = line.indexOf('#');
		final int semicolon = line.indexOf(';');
		int end = line.length();
		if (hash >= 0)
		{
			end = Math.min(end, hash);
		}
		
		if (semicolon >= 0)
		{
			end = Math.min(end, semicolon);
		}
		
		return line.substring(0, end);
	}
}
