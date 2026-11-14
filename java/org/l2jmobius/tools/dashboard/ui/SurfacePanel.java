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

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * Supplies a lightweight opaque dashboard surface using standard Swing border composition.
 * @author BazookaRpm
 */
public class SurfacePanel extends JPanel
{
	private Color _outlineColor;
	
	public SurfacePanel()
	{
		this(16, ControlCenterTheme.SURFACE, ControlCenterTheme.BORDER);
	}
	
	public SurfacePanel(int cornerRadius, Color fill, Color border)
	{
		_outlineColor = border;
		setOpaque(true);
		setBackground(fill);
		setBorder(createSurfaceBorder(cornerRadius, border));
	}
	
	public void setSurfaceColor(Color fill)
	{
		setBackground(fill);
	}
	
	public void setOutlineColor(Color border)
	{
		_outlineColor = border;
		setBorder(createSurfaceBorder(16, border));
	}
	
	public Color getOutlineColor()
	{
		return _outlineColor;
	}
	
	private static CompoundBorder createSurfaceBorder(int cornerRadius, Color border)
	{
		final int inset = Math.max(1, cornerRadius / 8);
		return BorderFactory.createCompoundBorder(new LineBorder(border, 1, true), new EmptyBorder(inset, inset, inset, inset));
	}
}
