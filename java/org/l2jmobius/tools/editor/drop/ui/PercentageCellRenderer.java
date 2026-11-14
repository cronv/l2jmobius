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
package org.l2jmobius.tools.editor.drop.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Renders numeric percentage cells as semantic color badges while preserving the original XML precision.
 * @author BazookaRpm
 */
final class PercentageCellRenderer extends DefaultTableCellRenderer
{
	private static final long serialVersionUID = 1L;
	
	private Color _badgeColor;
	private boolean _paintBadge;
	
	PercentageCellRenderer()
	{
		setHorizontalAlignment(SwingConstants.CENTER);
		setFont(getFont().deriveFont(Font.BOLD));
	}
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column)
	{
		super.getTableCellRendererComponent(table, value, selected, focused, row, column);
		final String raw = value == null ? "" : value.toString().trim();
		_paintBadge = false;
		setText(raw);
		setForeground(selected ? table.getSelectionForeground() : table.getForeground());
		setBackground(selected ? table.getSelectionBackground() : table.getBackground());
		setOpaque(true);
		if (raw.isEmpty() || "-".equals(raw))
		{
			return this;
		}
		
		try
		{
			final String numeric = raw.endsWith("%") ? raw.substring(0, raw.length() - 1).trim() : raw;
			final double percentage = Double.parseDouble(numeric);
			if (Double.isFinite(percentage))
			{
				_badgeColor = DropEditorPalette.percentageFill(percentage);
				_paintBadge = true;
				setText(raw.endsWith("%") ? raw : raw + "%");
				setForeground(DropEditorPalette.badgeForeground(_badgeColor));
			}
		}
		catch (NumberFormatException e)
		{
			// Preserve non-numeric values as plain text.
		}
		
		return this;
	}
	
	@Override
	protected void paintComponent(Graphics graphics)
	{
		if (_paintBadge)
		{
			final Graphics2D canvas = (Graphics2D) graphics.create();
			canvas.setColor(getBackground());
			canvas.fillRect(0, 0, getWidth(), getHeight());
			canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			canvas.setColor(_badgeColor);
			final int horizontalInset = Math.max(5, Math.min(10, getWidth() / 10));
			final int verticalInset = 3;
			canvas.fillRoundRect(horizontalInset, verticalInset, Math.max(0, getWidth() - (horizontalInset * 2)), Math.max(0, getHeight() - (verticalInset * 2)), 12, 12);
			canvas.dispose();
			final boolean opaque = isOpaque();
			setOpaque(false);
			super.paintComponent(graphics);
			setOpaque(opaque);
			return;
		}
		
		super.paintComponent(graphics);
	}
}
