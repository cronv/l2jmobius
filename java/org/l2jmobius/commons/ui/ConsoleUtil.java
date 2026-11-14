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
package org.l2jmobius.commons.ui;

import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStreamReader;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * Reports the width of the terminal the process is attached to.<br>
 * Java has no call for this, so Windows is asked through kernel32 and Linux through {@code stty}.<br>
 * The width is read once, a terminal resized afterwards is not noticed.
 * @author Mobius
 */
public class ConsoleUtil
{
	/** Standard output, as {@code GetStdHandle} names it. */
	private static final int STD_OUTPUT_HANDLE = -11;
	
	/**
	 * Size of {@code CONSOLE_SCREEN_BUFFER_INFO}, laid out as two {@code COORD}, one {@code WORD},<br>
	 * the {@code SMALL_RECT} visible window and a last {@code COORD}, every field two bytes wide.
	 */
	private static final int BUFFER_INFO_SIZE = 22;
	/** Offset of {@code srWindow.Left} inside {@code CONSOLE_SCREEN_BUFFER_INFO}. */
	private static final int SR_WINDOW_LEFT = 10;
	/** Offset of {@code srWindow.Right} inside {@code CONSOLE_SCREEN_BUFFER_INFO}. */
	private static final int SR_WINDOW_RIGHT = 14;
	
	private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase().startsWith("windows");
	
	private static final MethodHandle GET_STD_HANDLE;
	private static final MethodHandle GET_BUFFER_INFO;
	
	private static final int COLUMNS;
	
	static
	{
		MethodHandle getStdHandle = null;
		MethodHandle getBufferInfo = null;
		
		if (WINDOWS)
		{
			try
			{
				final Linker linker = Linker.nativeLinker();
				final SymbolLookup kernel32 = SymbolLookup.libraryLookup("kernel32.dll", Arena.global());
				
				getStdHandle = linker.downcallHandle(kernel32.find("GetStdHandle").orElseThrow(), FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
				getBufferInfo = linker.downcallHandle(kernel32.find("GetConsoleScreenBufferInfo").orElseThrow(), FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
			}
			catch (Throwable t)
			{
				// Native access is unavailable, the caller keeps its own default width.
			}
		}
		
		GET_STD_HANDLE = getStdHandle;
		GET_BUFFER_INFO = getBufferInfo;
		COLUMNS = readColumns();
	}
	
	/**
	 * Retrieves the width of the terminal in characters.
	 * @return the column count, or {@code 0} when the output is not a terminal or the width cannot be read
	 */
	public static int getColumns()
	{
		return COLUMNS;
	}
	
	private static int readColumns()
	{
		// Redirected output still hands out a Console since Java 22, so ask whether it is a real terminal.
		final Console console = System.console();
		if ((console == null) || !console.isTerminal())
		{
			return 0;
		}
		
		if (!WINDOWS)
		{
			return readColumnsUnix();
		}
		
		// Native access was denied, and Windows has no other way of telling.
		return GET_BUFFER_INFO == null ? 0 : readColumnsWindows();
	}
	
	private static int readColumnsWindows()
	{
		try (Arena arena = Arena.ofConfined())
		{
			final MemorySegment info = arena.allocate(BUFFER_INFO_SIZE, 2);
			if ((int) GET_BUFFER_INFO.invoke((MemorySegment) GET_STD_HANDLE.invoke(STD_OUTPUT_HANDLE), info) == 0)
			{
				return 0;
			}
			
			// The buffer is usually taller and wider than the window, only the visible part can hold a line.
			return (info.get(ValueLayout.JAVA_SHORT, SR_WINDOW_RIGHT) - info.get(ValueLayout.JAVA_SHORT, SR_WINDOW_LEFT)) + 1;
		}
		catch (Throwable t)
		{
			return 0;
		}
	}
	
	private static int readColumnsUnix()
	{
		try
		{
			// The shell reads its own terminal, so stdin is pointed at the one this process owns.
			final Process process = new ProcessBuilder("sh", "-c", "stty size < /dev/tty").start();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream())))
			{
				// A single line holding the row count, a space, then the column count.
				final String line = reader.readLine();
				if (line == null)
				{
					return 0;
				}
				
				final int space = line.indexOf(' ');
				return space < 0 ? 0 : Integer.parseInt(line.substring(space + 1).trim());
			}
		}
		catch (Exception e)
		{
			return 0;
		}
	}
}
