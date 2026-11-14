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
import java.awt.Cursor;
import java.awt.Font;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * Configures a native Swing action button with centralized dark-theme state transitions.
 * @author BazookaRpm
 */
public final class DashboardActionButton extends JButton
{
	private final Color _normalBackground;
	private final Color _rolloverBackground;
	private final Color _pressedBackground;
	
	public DashboardActionButton(String text, Icon icon, Color accent, boolean primary)
	{
		super(text, icon);
		_normalBackground = primary ? accent : ControlCenterTheme.SURFACE_ALT;
		_rolloverBackground = primary ? accent.brighter() : ControlCenterTheme.SURFACE_HOVER;
		_pressedBackground = primary ? accent.darker() : ControlCenterTheme.BACKGROUND;
		setFont(ControlCenterTheme.font(Font.BOLD, 12));
		setForeground(ControlCenterTheme.TEXT_BRIGHT);
		setBackground(_normalBackground);
		setOpaque(true);
		setContentAreaFilled(true);
		setFocusPainted(false);
		setRolloverEnabled(true);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setIconTextGap(8);
		setBorder(new CompoundBorder(new LineBorder(primary ? accent.brighter() : ControlCenterTheme.BORDER_BRIGHT, 1, true), new EmptyBorder(7, 12, 7, 12)));
		getModel().addChangeListener(event -> updateVisualState());
	}
	
	private void updateVisualState()
	{
		if (!isEnabled())
		{
			setBackground(ControlCenterTheme.SURFACE);
			return;
		}
		
		if (getModel().isPressed())
		{
			setBackground(_pressedBackground);
			return;
		}
		
		setBackground(getModel().isRollover() ? _rolloverBackground : _normalBackground);
	}
}
