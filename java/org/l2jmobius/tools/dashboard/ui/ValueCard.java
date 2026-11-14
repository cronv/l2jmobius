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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;

/**
 * Renders a compact metric card with a muted caption and highlighted value.
 * @author BazookaRpm
 */
public final class ValueCard extends SurfacePanel
{
	private final JLabel _valueLabel;
	
	public ValueCard(String caption, String initialValue)
	{
		this(caption);
		setValue(initialValue);
	}
	
	public ValueCard(String caption)
	{
		setLayout(new GridBagLayout());
		final JLabel captionLabel = DashboardUiFactory.label(caption.toUpperCase(), Font.BOLD, 9, ControlCenterTheme.MUTED);
		_valueLabel = DashboardUiFactory.label("-", Font.BOLD, 14, ControlCenterTheme.METRIC);
		final GridBagConstraints top = new GridBagConstraints();
		top.gridx = 0;
		top.gridy = 0;
		top.anchor = GridBagConstraints.SOUTHWEST;
		top.weightx = 1;
		top.weighty = 0.35;
		top.insets = new Insets(5, 8, 0, 8);
		add(captionLabel, top);
		final GridBagConstraints bottom = new GridBagConstraints();
		bottom.gridx = 0;
		bottom.gridy = 1;
		bottom.anchor = GridBagConstraints.NORTHWEST;
		bottom.weightx = 1;
		bottom.weighty = 0.65;
		bottom.insets = new Insets(1, 8, 6, 8);
		add(_valueLabel, bottom);
	}
	
	public void setValue(String value)
	{
		_valueLabel.setText((value == null) || value.isBlank() ? "-" : value);
	}
	
	public void setAccent(Color color)
	{
		_valueLabel.setForeground(color == null ? ControlCenterTheme.METRIC : color);
	}
}
