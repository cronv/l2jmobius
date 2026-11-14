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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;

import javax.swing.JComponent;

/**
 * Draws a compact radial metric using the dark-gray Interlude palette.
 * @author BazookaRpm
 */
public final class RadialMeter extends JComponent
{
	private final String _caption;
	private double _fraction = -1;
	private String _reading = "N/A";
	private String _detail = "Waiting for telemetry";
	
	public RadialMeter(String caption)
	{
		_caption = caption;
		setOpaque(false);
	}
	
	public void update(double fraction, String reading, String detail)
	{
		_fraction = clamp(fraction);
		_reading = (reading == null) || reading.isBlank() ? "N/A" : reading;
		_detail = detail == null ? "" : detail;
		repaint(0, 0, getWidth(), getHeight());
	}
	
	@Override
	protected void paintComponent(Graphics graphics)
	{
		final Graphics2D canvas = (Graphics2D) graphics.create();
		try
		{
			canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			// The stroke is centered on the arc path, so the circle gives up one stroke width to stay inside the component.
			final int width = getWidth();
			final int size = Math.min(width - 22, getHeight() - 45);
			final float stroke = Math.max(8f, size * 0.075f);
			final int diameter = Math.max(70, Math.round(size - stroke));
			final int left = (width - diameter) / 2;
			final int top = 8 + Math.round(stroke / 2);
			canvas.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			paintArc(canvas, ControlCenterTheme.GAUGE_TRACK, left, top, diameter, 225);
			if (_fraction >= 0)
			{
				paintArc(canvas, meterColor(_fraction), left, top, diameter, 225 * _fraction);
			}
			
			// Text and spacing grow with the circle, so a large window does not bunch the labels in the middle.
			final float scale = Math.max(1f, Math.min(2f, diameter / 160f));
			final int center = top + (diameter / 2);
			paintCentered(canvas, _caption.toUpperCase(), center - Math.round(16 * scale), ControlCenterTheme.MUTED, Font.BOLD, Math.round(9 * scale));
			paintCentered(canvas, _reading, center + Math.round(7 * scale), ControlCenterTheme.TEXT, Font.BOLD, Math.round(17 * scale));
			paintCentered(canvas, _detail, top + diameter + Math.round(24 * scale), ControlCenterTheme.MUTED, Font.PLAIN, Math.round(9 * scale));
		}
		finally
		{
			canvas.dispose();
		}
	}
	
	private void paintArc(Graphics2D canvas, Color color, int x, int y, int diameter, double extent)
	{
		canvas.setColor(color);
		canvas.draw(new Arc2D.Double(x, y, diameter, diameter, 225, -extent, Arc2D.OPEN));
	}
	
	private void paintCentered(Graphics2D canvas, String text, int baseline, Color color, int style, int size)
	{
		canvas.setFont(ControlCenterTheme.font(style, size));
		canvas.setColor(color);
		final int x = Math.max(4, (getWidth() - canvas.getFontMetrics().stringWidth(text)) / 2);
		canvas.drawString(text, x, baseline);
	}
	
	private static double clamp(double value)
	{
		if (value < 0)
		{
			return -1;
		}
		
		return Math.min(1, value);
	}
	
	private static Color meterColor(double value)
	{
		if (value >= 0.85)
		{
			return ControlCenterTheme.RED;
		}
		
		if (value >= 0.65)
		{
			return ControlCenterTheme.YELLOW;
		}
		
		return ControlCenterTheme.METRIC;
	}
}
