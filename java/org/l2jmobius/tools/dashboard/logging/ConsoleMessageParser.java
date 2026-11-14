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

import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

import org.l2jmobius.tools.dashboard.runtime.ServiceRole;

/**
 * Converts raw server console lines into structured dashboard messages.
 * @author BazookaRpm
 */
public final class ConsoleMessageParser
{
	private static final Pattern JAVA_FAILURE_PATTERN = Pattern.compile(".*\\b[A-Z0-9_$.]+(?:EXCEPTION|ERROR)(?::.*)?$");
	
	private ConsoleMessageParser()
	{
	}
	
	public static ConsoleMessage parse(ServiceRole role, String raw)
	{
		final String cleaned = clean(raw);
		return new ConsoleMessage(Instant.now(), role, classify(cleaned), cleaned);
	}
	
	public static ConsoleMessage parse(ServiceRole role, String raw, boolean errorStream)
	{
		return parse(role, raw);
	}
	
	public static String clean(String raw)
	{
		if (raw == null)
		{
			return "";
		}
		
		final StringBuilder output = new StringBuilder(raw.length());
		boolean escape = false;
		for (int index = 0; index < raw.length(); index++)
		{
			final char current = raw.charAt(index);
			if (escape)
			{
				if ((current >= '@') && (current <= '~'))
				{
					escape = false;
				}
				continue;
			}
			
			if (current == 27)
			{
				escape = true;
				continue;
			}
			
			if ((current >= 32) || (current == '\t'))
			{
				output.append(current);
			}
		}
		
		return output.toString().stripTrailing();
	}
	
	private static MessageSeverity classify(String message)
	{
		if (message.isEmpty())
		{
			return MessageSeverity.INFO;
		}
		
		final String normalized = message.toUpperCase(Locale.ROOT);
		if (hasLevel(normalized, "SEVERE") || hasLevel(normalized, "ERROR") || normalized.contains("SEVERITY=ERROR") || isFailureLine(normalized))
		{
			return MessageSeverity.ERROR;
		}
		
		if (hasLevel(normalized, "WARNING") || hasLevel(normalized, "WARN") || normalized.contains("SEVERITY=WARNING"))
		{
			return MessageSeverity.WARNING;
		}
		
		if (hasLevel(normalized, "FINE") || hasLevel(normalized, "FINER") || hasLevel(normalized, "FINEST") || hasLevel(normalized, "DEBUG") || hasLevel(normalized, "TRACE"))
		{
			return MessageSeverity.TRACE;
		}
		
		return MessageSeverity.INFO;
	}
	
	private static boolean hasLevel(String message, String level)
	{
		return message.contains("[" + level + "]") || message.contains("\t" + level + "\t") || message.startsWith(level + ":");
	}
	
	private static boolean isFailureLine(String message)
	{
		final String normalized = message.stripLeading();
		return normalized.startsWith("EXCEPTION IN THREAD") || normalized.startsWith("CAUSED BY:") || normalized.startsWith("SUPPRESSED:") || normalized.startsWith("AT ") || normalized.startsWith("\tAT ") || JAVA_FAILURE_PATTERN.matcher(normalized).matches();
	}
}
