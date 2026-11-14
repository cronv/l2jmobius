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

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.Window;

/**
 * Gives a window its initial size.
 * @author Mobius
 */
public class WindowSize
{
	/** Screens this size or smaller keep the size the window was built with. */
	private static final int SMALL_SCREEN_WIDTH = 1280;
	private static final int SMALL_SCREEN_HEIGHT = 768;
	
	/**
	 * Resizes the window to half the screen, unless the screen is small enough that the built in size fits better.<br>
	 * Call before centering the window, so the new size is taken into account.<br>
	 * A minimum size set on the window still wins, Swing does not shrink a window below it.
	 * @param window the window to resize
	 */
	public static void applyDefault(Window window)
	{
		if (GraphicsEnvironment.isHeadless())
		{
			return;
		}
		
		final Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		if ((screen.width <= SMALL_SCREEN_WIDTH) || (screen.height <= SMALL_SCREEN_HEIGHT))
		{
			return;
		}
		
		window.setSize(screen.width / 2, screen.height / 2);
	}
}
