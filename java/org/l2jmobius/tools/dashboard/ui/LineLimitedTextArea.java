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

import java.awt.Font;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.Element;

/**
 * Keeps a text area within a fixed line budget using the Swing document line tree instead of rescanning its full text.
 * @author BazookaRpm
 */
public final class LineLimitedTextArea extends JTextArea
{
	public LineLimitedTextArea(int maximumLines)
	{
		setEditable(false);
		setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		setBackground(ControlCenterTheme.CONSOLE);
		setForeground(ControlCenterTheme.TEXT);
		setCaretColor(ControlCenterTheme.TEXT_BRIGHT);
		setLineWrap(false);
		if (getDocument() instanceof AbstractDocument)
		{
			((AbstractDocument) getDocument()).setDocumentFilter(new LineLimitFilter(Math.max(100, maximumLines)));
		}
	}
	
	public void appendLine(String line)
	{
		appendLines((line == null ? "" : line) + System.lineSeparator());
	}
	
	public void appendLines(String text)
	{
		if (text == null)
		{
			return;
		}
		
		if (SwingUtilities.isEventDispatchThread())
		{
			append(text);
			setCaretPosition(getDocument().getLength());
			return;
		}
		
		SwingUtilities.invokeLater(() -> appendLines(text));
	}
	
	private static final class LineLimitFilter extends DocumentFilter
	{
		private final int _maximumLines;
		
		private LineLimitFilter(int maximumLines)
		{
			_maximumLines = maximumLines;
		}
		
		@Override
		public void insertString(FilterBypass bypass, int offset, String string, AttributeSet attributes) throws BadLocationException
		{
			super.insertString(bypass, offset, string, attributes);
			trim(bypass);
		}
		
		@Override
		public void replace(FilterBypass bypass, int offset, int length, String text, AttributeSet attributes) throws BadLocationException
		{
			super.replace(bypass, offset, length, text, attributes);
			trim(bypass);
		}
		
		private void trim(FilterBypass bypass) throws BadLocationException
		{
			final Document document = bypass.getDocument();
			final int length = document.getLength();
			if (length == 0)
			{
				return;
			}
			
			final Element root = document.getDefaultRootElement();
			int lineCount = root.getElementCount();
			if ("\n".equals(document.getText(length - 1, 1)))
			{
				lineCount--;
			}
			
			final int linesToRemove = lineCount - _maximumLines;
			if (linesToRemove <= 0)
			{
				return;
			}
			
			final Element lastRemovedLine = root.getElement(linesToRemove - 1);
			if (lastRemovedLine != null)
			{
				bypass.remove(0, Math.min(lastRemovedLine.getEndOffset(), document.getLength()));
			}
		}
	}
}
