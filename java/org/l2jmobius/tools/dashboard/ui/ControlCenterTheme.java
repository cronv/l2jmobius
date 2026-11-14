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
package org.l2jmobius.tools.dashboard.ui;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;

import org.l2jmobius.commons.ui.DarkTheme;

/**
 * Exposes the official L2J Mobius DarkTheme palette to Dashboard custom-painted components.
 * @author BazookaRpm
 */
public final class ControlCenterTheme
{
	private static final String FACE = Font.SANS_SERIF;
	public static final String NAME = "L2J Mobius DarkTheme";
	
	// Exact base palette from org.l2jmobius.commons.ui.DarkTheme.
	public static final Color BACKGROUND = new Color(60, 63, 65);
	public static final Color NAVIGATION = new Color(30, 30, 30);
	public static final Color TOP_BAR = new Color(60, 63, 65);
	public static final Color SURFACE = new Color(43, 43, 43);
	public static final Color SURFACE_ALT = new Color(60, 63, 65);
	public static final Color SURFACE_HOVER = new Color(76, 80, 82);
	public static final Color BORDER = new Color(85, 85, 85);
	public static final Color BORDER_BRIGHT = new Color(110, 113, 115);
	public static final Color TEXT = new Color(187, 187, 187);
	public static final Color TEXT_BRIGHT = new Color(255, 255, 255);
	public static final Color MUTED = new Color(128, 128, 128);
	public static final Color DISABLED = new Color(128, 128, 128);
	public static final Color ACCENT = new Color(104, 93, 156);
	public static final Color ACCENT_HOVER = new Color(120, 108, 177);
	public static final Color ACCENT_DARK = new Color(86, 78, 129);
	public static final Color ACCENT_DIM = new Color(51, 50, 58);
	public static final Color ACTION_PRIMARY = new Color(104, 93, 156);
	public static final Color CONSOLE = new Color(43, 43, 43);
	public static final Color GAUGE_TRACK = new Color(85, 85, 85);
	public static final Color METRIC = new Color(187, 187, 187);
	public static final Color SCROLL_THUMB = new Color(90, 93, 95);
	public static final Color SCROLL_THUMB_HOVER = new Color(110, 113, 115);
	
	// Semantic state colors are intentionally independent from the neutral theme palette.
	public static final Color GREEN = new Color(88, 157, 110);
	public static final Color GREEN_DARK = new Color(48, 92, 63);
	public static final Color YELLOW = new Color(205, 170, 85);
	public static final Color YELLOW_DARK = new Color(104, 82, 36);
	public static final Color RED = new Color(190, 92, 92);
	public static final Color RED_DARK = new Color(105, 52, 52);
	public static final Color BLUE = new Color(105, 145, 180);
	public static final Color BLUE_DARK = new Color(62, 78, 105);
	
	private ControlCenterTheme()
	{
	}
	
	public static void install()
	{
		DarkTheme.activate();
	}
	
	public static Font font(int style, int size)
	{
		return new Font(FACE, style, size);
	}
	
	public static void styleScrollPane(JScrollPane scrollPane)
	{
		scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
		scrollPane.getViewport().setBackground(SURFACE);
		scrollPane.getVerticalScrollBar().setBackground(BACKGROUND);
		scrollPane.getHorizontalScrollBar().setBackground(BACKGROUND);
		scrollPane.getVerticalScrollBar().setUnitIncrement(18);
	}
}
