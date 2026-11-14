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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;

import org.l2jmobius.tools.dashboard.runtime.ServicePhase;

/**
 * Displays a compact service phase label with a phase-specific background.
 * @author BazookaRpm
 */
public final class StatePill extends JLabel
{
	private Color _background = ControlCenterTheme.SURFACE_ALT;
	
	public StatePill()
	{
		setFont(ControlCenterTheme.font(Font.BOLD, 11));
		setForeground(ControlCenterTheme.TEXT);
		setBorder(new EmptyBorder(5, 9, 5, 9));
		setOpaque(false);
		setPhase(ServicePhase.OFFLINE);
	}
	
	public void setPhase(ServicePhase phase)
	{
		setText(phase.getLabel().toUpperCase());
		_background = phaseColor(phase);
		repaint();
	}
	
	public static Color phaseColor(ServicePhase phase)
	{
		switch (phase)
		{
			case ONLINE:
			{
				return ControlCenterTheme.GREEN_DARK;
			}
			case LAUNCHING:
			case STOPPING:
			{
				return ControlCenterTheme.YELLOW_DARK;
			}
			case FAILED:
			{
				return ControlCenterTheme.RED_DARK;
			}
			case EXTERNAL:
			{
				return ControlCenterTheme.BLUE_DARK;
			}
			case OFFLINE:
			default:
			{
				return ControlCenterTheme.SURFACE_ALT;
			}
		}
	}
	
	@Override
	protected void paintComponent(Graphics graphics)
	{
		final Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(_background);
		g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
		g.dispose();
		super.paintComponent(graphics);
	}
	
	@Override
	public Insets getInsets()
	{
		return new Insets(5, 9, 5, 9);
	}
}
