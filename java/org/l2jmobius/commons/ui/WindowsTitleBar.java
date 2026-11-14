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

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

/**
 * Paints the Windows title bar of a window in dark or light mode.<br>
 * The operating system owns the title bar, so this is the one thing a look and feel cannot do on its own.<br>
 * It asks the desktop window manager to recolor it, which keeps the native buttons and window behavior intact.<br>
 * Every call is a no operation on other platforms, on older Windows builds, or when native access is denied.
 * @author Mobius
 */
public class WindowsTitleBar
{
	/** Windows 10 build 18985 and later, including Windows 11. */
	private static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
	/** Windows 10 builds 17763 to 18984. */
	private static final int DWMWA_USE_IMMERSIVE_DARK_MODE_BEFORE_20H1 = 19;
	/** Exact title bar color, Windows 11 build 22000 and later. */
	private static final int DWMWA_CAPTION_COLOR = 35;
	
	private static final int SWP_NOMOVE = 0x0002;
	private static final int SWP_NOZORDER = 0x0004;
	private static final int SWP_NOACTIVATE = 0x0010;
	private static final int SWP_FRAMECHANGED = 0x0020;
	
	private static final MethodHandle FIND_WINDOW;
	private static final MethodHandle GET_WINDOW_PROCESS_ID;
	private static final MethodHandle GET_CURRENT_PROCESS_ID;
	private static final MethodHandle GET_WINDOW_RECT;
	private static final MethodHandle SET_WINDOW_ATTRIBUTE;
	private static final MethodHandle SET_WINDOW_POSITION;
	private static final boolean AVAILABLE;
	
	static
	{
		MethodHandle findWindow = null;
		MethodHandle getWindowProcessId = null;
		MethodHandle getCurrentProcessId = null;
		MethodHandle getWindowRect = null;
		MethodHandle setWindowAttribute = null;
		MethodHandle setWindowPosition = null;
		boolean available = false;
		
		if (System.getProperty("os.name", "").toLowerCase().startsWith("windows"))
		{
			try
			{
				final Linker linker = Linker.nativeLinker();
				final Arena arena = Arena.global();
				final SymbolLookup user32 = SymbolLookup.libraryLookup("user32.dll", arena);
				final SymbolLookup kernel32 = SymbolLookup.libraryLookup("kernel32.dll", arena);
				final SymbolLookup dwmapi = SymbolLookup.libraryLookup("dwmapi.dll", arena);
				
				findWindow = linker.downcallHandle(user32.find("FindWindowW").orElseThrow(), FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
				getWindowProcessId = linker.downcallHandle(user32.find("GetWindowThreadProcessId").orElseThrow(), FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
				getCurrentProcessId = linker.downcallHandle(kernel32.find("GetCurrentProcessId").orElseThrow(), FunctionDescriptor.of(ValueLayout.JAVA_INT));
				getWindowRect = linker.downcallHandle(user32.find("GetWindowRect").orElseThrow(), FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
				setWindowAttribute = linker.downcallHandle(dwmapi.find("DwmSetWindowAttribute").orElseThrow(), FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
				setWindowPosition = linker.downcallHandle(user32.find("SetWindowPos").orElseThrow(), FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
				available = true;
			}
			catch (Throwable t)
			{
				// Native access is unavailable, the title bar keeps its default color.
			}
		}
		
		FIND_WINDOW = findWindow;
		GET_WINDOW_PROCESS_ID = getWindowProcessId;
		GET_CURRENT_PROCESS_ID = getCurrentProcessId;
		GET_WINDOW_RECT = getWindowRect;
		SET_WINDOW_ATTRIBUTE = setWindowAttribute;
		SET_WINDOW_POSITION = setWindowPosition;
		AVAILABLE = available;
	}
	
	/**
	 * Recolors the title bar of the given window.
	 * @param window the window to recolor, matched by its title
	 * @param dark {@code true} for light title bar text and buttons, {@code false} for dark ones
	 * @param caption the exact title bar color, ignored before Windows 11
	 */
	public static void apply(Window window, boolean dark, Color caption)
	{
		if (!AVAILABLE || (window == null))
		{
			return;
		}
		
		final String title = getTitle(window);
		if ((title == null) || title.isEmpty())
		{
			return;
		}
		
		try (Arena arena = Arena.ofConfined())
		{
			final MemorySegment handle = (MemorySegment) FIND_WINDOW.invoke(MemorySegment.NULL, arena.allocateFrom(title, StandardCharsets.UTF_16LE));
			if (handle.address() == 0)
			{
				return;
			}
			
			// Another application can own a window with the same title, only touch our own.
			final MemorySegment processId = arena.allocate(ValueLayout.JAVA_INT);
			GET_WINDOW_PROCESS_ID.invoke(handle, processId);
			if (processId.get(ValueLayout.JAVA_INT, 0) != (int) GET_CURRENT_PROCESS_ID.invoke())
			{
				return;
			}
			
			final MemorySegment value = arena.allocate(ValueLayout.JAVA_INT);
			value.set(ValueLayout.JAVA_INT, 0, dark ? 1 : 0);
			if ((int) SET_WINDOW_ATTRIBUTE.invoke(handle, DWMWA_USE_IMMERSIVE_DARK_MODE, value, 4) != 0)
			{
				SET_WINDOW_ATTRIBUTE.invoke(handle, DWMWA_USE_IMMERSIVE_DARK_MODE_BEFORE_20H1, value, 4);
			}
			
			// Windows 11 accepts an exact shade, older builds reject it and keep the mode above.
			if (caption != null)
			{
				final MemorySegment color = arena.allocate(ValueLayout.JAVA_INT);
				color.set(ValueLayout.JAVA_INT, 0, (caption.getBlue() << 16) | (caption.getGreen() << 8) | caption.getRed());
				SET_WINDOW_ATTRIBUTE.invoke(handle, DWMWA_CAPTION_COLOR, color, 4);
			}
			
			// The frame keeps its old color until the window is resized, a frame change alone is ignored on Windows Server.
			// Growing it by one pixel and shrinking it straight back is the smallest resize that forces the repaint.
			final MemorySegment rect = arena.allocate(ValueLayout.JAVA_INT, 4);
			if ((int) GET_WINDOW_RECT.invoke(handle, rect) != 0)
			{
				final int width = rect.getAtIndex(ValueLayout.JAVA_INT, 2) - rect.getAtIndex(ValueLayout.JAVA_INT, 0);
				final int height = rect.getAtIndex(ValueLayout.JAVA_INT, 3) - rect.getAtIndex(ValueLayout.JAVA_INT, 1);
				final int flags = SWP_NOMOVE | SWP_NOZORDER | SWP_NOACTIVATE | SWP_FRAMECHANGED;
				SET_WINDOW_POSITION.invoke(handle, MemorySegment.NULL, 0, 0, width, height + 1, flags);
				SET_WINDOW_POSITION.invoke(handle, MemorySegment.NULL, 0, 0, width, height, flags);
			}
		}
		catch (Throwable t)
		{
			// Leave the title bar alone rather than take the window down with us.
		}
	}
	
	private static String getTitle(Window window)
	{
		if (window instanceof Frame)
		{
			return ((Frame) window).getTitle();
		}
		
		if (window instanceof Dialog)
		{
			return ((Dialog) window).getTitle();
		}
		
		return null;
	}
}
