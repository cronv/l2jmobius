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
package org.l2jmobius.tools.editor.item.ui;

import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Lightweight line-number gutter for Item Editor text areas.
 * @author BazookaRpm
 */
final class ItemEditorLineNumbers extends JTextArea
{
	private static final long serialVersionUID = 1L;
	private final JTextArea _source;
	private int _lines;
	
	ItemEditorLineNumbers(JTextArea source)
	{
		_source = source;
		setEditable(false);
		setFocusable(false);
		setRows(1);
		setColumns(4);
		setFont(new Font(Font.MONOSPACED, Font.PLAIN, source.getFont().getSize()));
		setForeground(ItemEditorPalette.muted());
		setBackground(ItemEditorPalette.surfaceAlt());
		setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 6));
		_source.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent event)
			{
				refresh();
			}
			
			@Override
			public void removeUpdate(DocumentEvent event)
			{
				refresh();
			}
			
			@Override
			public void changedUpdate(DocumentEvent event)
			{
				// Attribute changes never alter the line count.
			}
		});
		refresh();
	}
	
	private void refresh()
	{
		final int lines = Math.max(1, _source.getLineCount());
		if (lines == _lines)
		{
			return;
		}
		
		_lines = lines;
		final StringBuilder text = new StringBuilder(lines * 4);
		for (int line = 1; line <= lines; line++)
		{
			text.append(line);
			if (line < lines)
			{
				text.append('\n');
			}
		}
		
		setText(text.toString());
	}
}
