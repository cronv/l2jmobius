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
 * This class provides a configuration for applying a light visual theme to the user interface.<br>
 * The painting itself is done by {@link FlatLookAndFeel}, this class only holds the palette.
 * @author Mobius
 */
public class LightTheme
{
	/**
	 * Activates the light theme for the user interface.<br>
	 * This method sets a flat look and feel with the palette below.
	 */
	public static void activate()
	{
		final FlatLookAndFeel.Palette palette = new FlatLookAndFeel.Palette();
		palette.background = new Color(242, 242, 242);
		palette.menuBar = new Color(224, 224, 224);
		palette.titleBar = new Color(214, 214, 214);
		palette.dark = false;
		palette.content = new Color(255, 255, 255);
		palette.foreground = new Color(30, 30, 30);
		palette.disabled = new Color(140, 140, 140);
		palette.border = new Color(196, 196, 196);
		palette.hover = new Color(227, 227, 227);
		palette.selection = new Color(118, 105, 173);
		palette.selectionText = new Color(255, 255, 255);
		palette.thumb = new Color(196, 196, 196);
		palette.thumbHover = new Color(166, 166, 166);
		
		FlatLookAndFeel.install("Flat Light", palette);
	}
}
