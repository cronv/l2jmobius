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
package org.l2jmobius.tools.editor.npc.ui;

import java.awt.Color;

import javax.swing.UIManager;

/**
 * Provides theme-aware colors shared by the Dashboard and standalone NPC/Mob Editor surfaces.
 * @author BazookaRpm
 */
final class NpcEditorPalette
{
	static final Color ACCENT = new Color(104, 93, 156);
	static final Color GREEN = new Color(48, 126, 75);
	static final Color ORANGE = new Color(174, 92, 40);
	static final Color RED = new Color(161, 62, 68);
	static final Color BLUE = new Color(62, 105, 150);
	
	private NpcEditorPalette()
	{
	}
	
	static Color panel()
	{
		return uiColor("Panel.background", new Color(60, 63, 65));
	}
	
	static Color surface()
	{
		return uiColor("Table.background", new Color(43, 43, 43));
	}
	
	static Color surfaceAlt()
	{
		final Color base = panel();
		return isDark(base) ? blend(base, Color.BLACK, 0.18f) : blend(base, Color.WHITE, 0.34f);
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
		return uiColor("Label.foreground", new Color(187, 187, 187));
	}
	
	static Color muted()
	{
		return blend(text(), panel(), 0.48f);
	}
	
	static Color changed()
	{
		return blend(uiColor("TextField.background", surface()), ACCENT, isDark(panel()) ? 0.22f : 0.12f);
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
	
	private static boolean isDark(Color color)
	{
		return luminance(color) < 0.5d;
	}
	
	private static double luminance(Color color)
	{
		return ((0.2126d * color.getRed()) + (0.7152d * color.getGreen()) + (0.0722d * color.getBlue())) / 255d;
	}
}
