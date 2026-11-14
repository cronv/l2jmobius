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
package org.l2jmobius.gameserver.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Audits Game Master actions to per-GM log files.
 */
public class GMAudit
{
	private static final Logger LOGGER = Logger.getLogger("gmaudit");
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy H:mm:ss");
	static
	{
		new File("log/GMAudit").mkdirs();
	}
	
	// List of characters not allowed in file names.
	private static final char[] ILLEGAL_CHARACTERS =
	{
		// @formatter:off
		'/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':'
		// @formatter:on
	};
	
	/**
	 * Appends a Game Master action to a per-GM audit file, sanitizing the name into the filename.
	 * @param gmName the acting Game Master name
	 * @param action the action description
	 * @param target the affected entity name
	 * @param params additional action parameters
	 */
	public static void logAction(String gmName, String action, String target, String params)
	{
		final String timestamp = DATE_FORMAT.format(LocalDateTime.now());
		
		// Sanitize the GM's name for the filename.
		String sanitizedGmName = sanitizeFileName(gmName);
		if (!isValidFileName(sanitizedGmName))
		{
			sanitizedGmName = "INVALID_GM_NAME_" + timestamp;
		}
		
		// Create the audit file and log the action.
		final File logFile = new File("log/GMAudit/" + sanitizedGmName + ".txt");
		try (FileWriter writer = new FileWriter(logFile, true))
		{
			writer.write(timestamp + ">" + gmName + ">" + action + ">" + target + ">" + params + System.lineSeparator());
		}
		catch (IOException e)
		{
			LOGGER.log(Level.SEVERE, "Could not save GMAudit log for GM " + gmName + ":", e);
		}
	}
	
	/**
	 * Records a Game Master action to the audit log with no extra parameters.
	 * @param gmName the acting Game Master name
	 * @param action the action description
	 * @param target the affected entity name
	 */
	public static void logAction(String gmName, String action, String target)
	{
		logAction(gmName, action, target, "");
	}
	
	/**
	 * Replaces all filename-illegal characters in the name with underscores.
	 * @param name the original name
	 * @return the sanitized name
	 */
	private static String sanitizeFileName(String name)
	{
		String sanitized = name;
		for (char illegalChar : ILLEGAL_CHARACTERS)
		{
			sanitized = sanitized.replace(illegalChar, '_');
		}
		
		return sanitized;
	}
	
	/**
	 * Checks whether the name resolves to a valid file path via its canonical path.
	 * @param name the name to validate
	 * @return {@code true} if valid, {@code false} if an {@code IOException} occurs
	 */
	private static boolean isValidFileName(String name)
	{
		final File file = new File(name);
		try
		{
			file.getCanonicalPath();
			return true;
		}
		catch (IOException e)
		{
			return false;
		}
	}
}
