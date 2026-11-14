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

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

/**
 * Loads the application window icons from bundled L2JMobius image resources. <br>
 * The existing vector glyph remains as a fallback if an image resource cannot be loaded.
 * @author BazookaRpm
 */
public final class ApplicationIconSet
{
	private ApplicationIconSet()
	{
	}
	
	public static List<Image> create()
	{
		final int[] sizes =
		{
			16,
			32,
			64,
			128
		};
		final List<Image> images = new ArrayList<>(sizes.length);
		for (int size : sizes)
		{
			final URL resource = ApplicationIconSet.class.getResource("/org/l2jmobius/tools/dashboard/ui/images/l2jmobius_" + size + "x" + size + ".png");
			if (resource != null)
			{
				images.add(new ImageIcon(resource).getImage());
				continue;
			}
			
			final BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D graphics = image.createGraphics();
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setColor(ControlCenterTheme.ACCENT_DARK);
			graphics.fillRoundRect(0, 0, size, size, size / 3, size / 3);
			new GlyphIcon(GlyphIcon.Kind.SERVICE, Math.max(12, size - 8), ControlCenterTheme.TEXT_BRIGHT).paintIcon(null, graphics, 4, 4);
			graphics.dispose();
			images.add(image);
		}
		
		return images;
	}
}
