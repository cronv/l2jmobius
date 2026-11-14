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
package org.l2jmobius.commons.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.StringJoiner;

/**
 * This utility class provides methods for handling and formatting stack traces.<br>
 * It simplifies the process of converting exceptions into readable strings.
 * @author Mobius
 */
public class TraceUtil
{
	/**
	 * Converts a {@code Throwable} object into a readable string.<br>
	 * This method captures the full stack trace of the provided exception.<br>
	 * It is useful for logging errors to a file or console.
	 * @param throwable The {@code Throwable} object to process.
	 * @return A {@code String} containing the formatted stack trace.
	 */
	public static String getStackTrace(Throwable throwable)
	{
		final StringWriter writer = new StringWriter();
		throwable.printStackTrace(new PrintWriter(writer));
		return writer.toString();
	}
	
	/**
	 * Converts an array of {@code StackTraceElement} objects into a single string.<br>
	 * Each element is joined by the system line separator.<br>
	 * This method is useful for logging or displaying stack traces manually.
	 * @param stackTraceElements An array of {@code StackTraceElement} objects to format.
	 * @return A formatted string containing all elements from the array.
	 */
	public static String getTraceString(StackTraceElement[] stackTraceElements)
	{
		final StringJoiner joiner = new StringJoiner(System.lineSeparator());
		for (StackTraceElement stackTraceElement : stackTraceElements)
		{
			joiner.add(stackTraceElement.toString());
		}
		
		return joiner.toString();
	}
}
