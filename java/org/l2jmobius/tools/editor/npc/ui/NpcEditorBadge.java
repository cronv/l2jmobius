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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * Displays a compact rounded status label used by the NPC/Mob Editor header.
 * @author BazookaRpm
 */
final class NpcEditorBadge extends JLabel
{
	private static final long serialVersionUID = 1L;
	
	private Color _fill;
	
	NpcEditorBadge(String text, Color fill)
	{
		super(text, SwingConstants.CENTER);
		_fill = fill;
		setOpaque(false);
		setFont(getFont().deriveFont(Font.BOLD, Math.max(10f, getFont().getSize2D() - 1f)));
		setBorder(new EmptyBorder(4, 9, 4, 9));
		setForeground(NpcEditorPalette.badgeForeground(fill));
	}
	
	void setBadge(String text, Color fill)
	{
		setText(text);
		_fill = fill;
		setForeground(NpcEditorPalette.badgeForeground(fill));
		repaint();
	}
	
	@Override
	protected void paintComponent(Graphics graphics)
	{
		final Graphics2D canvas = (Graphics2D) graphics.create();
		canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		canvas.setColor(_fill);
		canvas.fillRoundRect(0, 0, getWidth(), getHeight(), Math.max(10, getHeight()), Math.max(10, getHeight()));
		canvas.dispose();
		super.paintComponent(graphics);
	}
	
	@Override
	public Insets getInsets()
	{
		return new Insets(4, 9, 4, 9);
	}
}
