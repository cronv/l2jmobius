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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Centralizes reusable Swing composition primitives for the L2J Mobius Control Dashboard.
 * @author BazookaRpm
 */
public final class DashboardUiFactory
{
	private DashboardUiFactory()
	{
	}
	
	public static JPanel panel(LayoutManager layout)
	{
		final JPanel panel = new JPanel(layout);
		panel.setOpaque(false);
		return panel;
	}
	
	public static JPanel flow(int alignment, int horizontalGap)
	{
		return panel(new FlowLayout(alignment, horizontalGap, 0));
	}
	
	public static JPanel title(String heading, String description)
	{
		final JPanel text = panel(new BorderLayout(0, 1));
		text.add(label(heading, Font.BOLD, 17, ControlCenterTheme.TEXT), BorderLayout.CENTER);
		text.add(label(description, Font.PLAIN, 10, ControlCenterTheme.MUTED), BorderLayout.SOUTH);
		return text;
	}
	
	public static JLabel label(String text, int style, int size, Color color)
	{
		final JLabel label = new JLabel(text);
		label.setFont(ControlCenterTheme.font(style, size));
		label.setForeground(color);
		return label;
	}
	
	public static SurfacePanel section(String heading)
	{
		final SurfacePanel panel = new SurfacePanel(12, ControlCenterTheme.SURFACE, ControlCenterTheme.BORDER);
		panel.setLayout(new BorderLayout(0, 8));
		panel.setBorder(BorderFactory.createCompoundBorder(panel.getBorder(), BorderFactory.createEmptyBorder(10, 12, 12, 12)));
		panel.add(label(heading, Font.BOLD, 12, ControlCenterTheme.TEXT), BorderLayout.NORTH);
		return panel;
	}
	
	public static JScrollPane scroll(JComponent content)
	{
		final JScrollPane scroll = new JScrollPane(content);
		ControlCenterTheme.styleScrollPane(scroll);
		return scroll;
	}
	
	public static JPanel grid(int columns, int gap, Component... components)
	{
		final JPanel panel = panel(new GridBagLayout());
		for (int index = 0; index < components.length; index++)
		{
			final GridBagConstraints constraints = new GridBagConstraints();
			constraints.gridx = index % columns;
			constraints.gridy = index / columns;
			constraints.weightx = 1;
			constraints.weighty = 1;
			constraints.fill = GridBagConstraints.BOTH;
			constraints.insets = new Insets(gap / 2, gap / 2, gap / 2, gap / 2);
			panel.add(components[index], constraints);
		}
		
		return panel;
	}
	
	public static void addAll(Container target, Component... components)
	{
		for (Component component : components)
		{
			target.add(component);
		}
	}
}
