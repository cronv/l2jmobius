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
package org.l2jmobius.tools.dashboard.tools;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Describes a tile-aligned visual layer that can be displayed by the Map Extractor preview.
 * @param image the rendered image for this preview layer
 * @param minimumX the minimum map region X coordinate covered by this layer
 * @param maximumX the maximum map region X coordinate covered by this layer
 * @param minimumY the minimum map region Y coordinate covered by this layer
 * @param maximumY the maximum map region Y coordinate covered by this layer
 * @param source the source description associated with this preview layer
 * @author BazookaRpm
 */
public record MapPreviewLayer(BufferedImage image, int minimumX, int maximumX, int minimumY, int maximumY, String source)
{
	public MapPreviewLayer
	{
		Objects.requireNonNull(image, "image");
		Objects.requireNonNull(source, "source");
		if ((maximumX < minimumX) || (maximumY < minimumY))
		{
			throw new IllegalArgumentException("Invalid map preview bounds.");
		}
	}
	
	public int columns()
	{
		return (maximumX - minimumX) + 1;
	}
	
	public int rows()
	{
		return (maximumY - minimumY) + 1;
	}
}
