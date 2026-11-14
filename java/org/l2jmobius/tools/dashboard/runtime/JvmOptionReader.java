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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses the whitespace-oriented java.cfg format used by Interlude without invoking a shell.
 * @author BazookaRpm
 */
public final class JvmOptionReader
{
	private JvmOptionReader()
	{
	}
	
	public static List<String> read(Path file) throws IOException
	{
		final List<String> result = new ArrayList<>();
		if (!Files.isRegularFile(file))
		{
			return result;
		}
		
		final String content = Files.readString(file);
		final StringBuilder token = new StringBuilder();
		char quote = 0;
		boolean escaping = false;
		for (int index = 0; index < content.length(); index++)
		{
			final char current = content.charAt(index);
			if (escaping)
			{
				token.append(current);
				escaping = false;
				continue;
			}
			
			if (current == '\\')
			{
				escaping = true;
				continue;
			}
			
			if ((current == '\'') || (current == '"'))
			{
				if (quote == 0)
				{
					quote = current;
				}
				else if (quote == current)
				{
					quote = 0;
				}
				else
				{
					token.append(current);
				}
				continue;
			}
			
			if (Character.isWhitespace(current) && (quote == 0))
			{
				flush(result, token);
				continue;
			}
			
			token.append(current);
		}
		
		if (escaping)
		{
			token.append('\\');
		}
		
		flush(result, token);
		return result;
	}
	
	private static void flush(List<String> destination, StringBuilder token)
	{
		if (token.length() == 0)
		{
			return;
		}
		
		destination.add(token.toString());
		token.setLength(0);
	}
}
