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
package org.l2jmobius.tools.editor.item.ui;

import java.awt.Color;

import javax.swing.UIManager;

/**
 * Provides theme-aware colors shared by the Dashboard and standalone Item Editor surfaces.
 * @author BazookaRpm
 */
final class ItemEditorPalette
{
	static final Color ACCENT = uiColor("L2JDashboard.accent", new Color(104, 93, 156));
	static final Color GREEN = uiColor("L2JDashboard.success", new Color(113, 214, 140));
	static final Color ORANGE = uiColor("L2JDashboard.warning", new Color(232, 201, 106));
	static final Color RED = uiColor("L2JDashboard.error", new Color(240, 125, 125));
	static final Color BLUE = uiColor("L2JDashboard.info", new Color(121, 184, 232));
	
	private ItemEditorPalette()
	{
	}
	
	static Color panel()
	{
		return uiColor("L2JDashboard.background", uiColor("Panel.background", new Color(32, 34, 37)));
	}
	
	static Color surface()
	{
		return uiColor("L2JDashboard.surface", uiColor("Table.background", new Color(41, 44, 48)));
	}
	
	static Color surfaceAlt()
	{
		return uiColor("L2JDashboard.surfaceAlt", isDark(panel()) ? blend(panel(), Color.WHITE, 0.10f) : blend(panel(), Color.BLACK, 0.10f));
	}
	
	static Color border()
	{
		final Color color = UIManager.getColor("Separator.foreground");
		if (color != null)
		{
			return color;
		}
		
		final Color base = panel();
		return isDark(base) ? blend(base, Color.WHITE, 0.18f) : blend(base, Color.BLACK, 0.18f);
	}
	
	static Color text()
	{
		return uiColor("L2JDashboard.text", contrastingText(panel()));
	}
	
	static Color muted()
	{
		return uiColor("L2JDashboard.textMuted", blend(text(), panel(), 0.25f));
	}
	
	static Color surfaceText()
	{
		return contrastingText(surface());
	}
	
	static Color inputBackground()
	{
		return uiColor("L2JDashboard.fieldBackground", uiColor("TextField.background", surface()));
	}
	
	static Color inputText()
	{
		return contrastingText(inputBackground());
	}
	
	static Color selectionBackground()
	{
		return uiColor("Table.selectionBackground", ACCENT);
	}
	
	static Color selectionText()
	{
		return uiColor("Table.selectionForeground", contrastingText(selectionBackground()));
	}
	
	static Color headerBackground()
	{
		return uiColor("TableHeader.background", surfaceAlt());
	}
	
	static Color headerText()
	{
		return uiColor("TableHeader.foreground", contrastingText(headerBackground()));
	}
	
	static Color grid()
	{
		return uiColor("Table.gridColor", isDark(surface()) ? blend(surface(), Color.WHITE, 0.18f) : blend(surface(), Color.BLACK, 0.18f));
	}
	
	static Color badgeForeground(Color fill)
	{
		return luminance(fill) > 0.62d ? new Color(35, 35, 35) : Color.WHITE;
	}
	
	static Color blend(Color first, Color second, float ratio)
	{
		final float safeRatio = Math.max(0f, Math.min(1f, ratio));
		final float inverse = 1f - safeRatio;
		return new Color(Math.round((first.getRed() * inverse) + (second.getRed() * safeRatio)), Math.round((first.getGreen() * inverse) + (second.getGreen() * safeRatio)), Math.round((first.getBlue() * inverse) + (second.getBlue() * safeRatio)));
	}
	
	private static Color uiColor(String key, Color fallback)
	{
		final Color color = UIManager.getColor(key);
		return color == null ? fallback : color;
	}
	
	private static Color contrastingText(Color background)
	{
		return isDark(background) ? new Color(238, 238, 238) : new Color(32, 32, 32);
	}
	
	private static boolean isDark(Color color)
	{
		return luminance(color) < 0.5d;
	}
	
	private static double luminance(Color color)
	{
		return ((0.2126d * color.getRed()) + (0.7152d * color.getGreen()) + (0.0722d * color.getBlue())) / 255d;
	}
}
