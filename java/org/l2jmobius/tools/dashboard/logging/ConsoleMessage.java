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

import org.l2jmobius.tools.dashboard.runtime.ServiceRole;

/**
 * Represents one normalized console line from a managed service.
 * @param timestamp timestamp assigned to the normalized console line
 * @param role managed service that produced the line
 * @param severity normalized message severity
 * @param text normalized console text
 * @author BazookaRpm
 */
public record ConsoleMessage(Instant timestamp, ServiceRole role, MessageSeverity severity, String text)
{
	public ConsoleMessage
	{
		if (timestamp == null)
		{
			timestamp = Instant.now();
		}
		
		if (severity == null)
		{
			severity = MessageSeverity.INFO;
		}
		
		if (text == null)
		{
			text = "";
		}
	}
}
