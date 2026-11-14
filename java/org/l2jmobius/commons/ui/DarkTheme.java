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

/**
 * This class provides a configuration for applying a dark visual theme to the user interface.<br>
 * The painting itself is done by {@link FlatLookAndFeel}, this class only holds the palette.
 * @author Mobius
 */
public class DarkTheme
{
	/**
	 * Activates the dark theme for the user interface.<br>
	 * This method sets a flat look and feel with the palette below.
	 */
	public static void activate()
	{
		final FlatLookAndFeel.Palette palette = new FlatLookAndFeel.Palette();
		palette.background = new Color(60, 63, 65);
		palette.menuBar = new Color(60, 63, 65);
		palette.titleBar = new Color(30, 30, 30);
		palette.dark = true;
		palette.content = new Color(43, 43, 43);
		palette.foreground = new Color(187, 187, 187);
		palette.disabled = new Color(128, 128, 128);
		palette.border = new Color(85, 85, 85);
		palette.hover = new Color(76, 80, 82);
		palette.selection = new Color(104, 93, 156);
		palette.selectionText = new Color(255, 255, 255);
		palette.thumb = new Color(90, 93, 95);
		palette.thumbHover = new Color(110, 113, 115);
		
		FlatLookAndFeel.install("Flat Dark", palette);
	}
}
