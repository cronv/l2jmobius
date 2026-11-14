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
package org.l2jmobius.commons.ui;

import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;

/**
 * A {@code DocumentListener} that enforces a maximum line count on a {@code Document}.<br>
 * It automatically removes excess lines from the start of the document.
 * @author Mobius
 */
public class LineLimitListener implements DocumentListener
{
	private final int _maxLines;
	
	/**
	 * Creates a new {@code LineLimitListener} with a specific line limit.<br>
	 * Excess lines are removed from the start of the document.
	 * @param maxLines The maximum number of lines allowed in the document.
	 */
	public LineLimitListener(int maxLines)
	{
		_maxLines = maxLines;
	}
	
	/**
	 * This method handles the removal of excess lines from a {@code Document}.<br>
	 * It checks if the current line count exceeds the allowed limit.<br>
	 * If it does, it calls {@code removeFromStart} until the limit is met.
	 * @param event the {@code DocumentEvent} containing the document information
	 */
	private void removeLines(DocumentEvent event)
	{
		// The root Element of the Document will tell us the total number of line in the Document.
		final Document document = event.getDocument();
		final Element root = document.getDefaultRootElement();
		
		while (root.getElementCount() > _maxLines)
		{
			removeFromStart(document, root);
		}
	}
	
	/**
	 * Removes the first line from the {@code Document}.<br>
	 * This method identifies the end offset of the first element.<br>
	 * It then deletes everything from index 0 to that offset.
	 * @param document The {@code Document} to modify.
	 * @param root The {@code Element} representing the content of the document.
	 */
	private void removeFromStart(Document document, Element root)
	{
		final Element line = root.getElement(0);
		try
		{
			document.remove(0, line.getEndOffset());
		}
		catch (BadLocationException ble)
		{
			System.out.println(ble);
		}
	}
	
	/**
	 * Handles the insertion of text into the document.<br>
	 * This method ensures that line limits are enforced after an insert operation.<br>
	 * It schedules the {@code removeLines} task to run on the event dispatch thread.
	 * @param event the {@code DocumentEvent} containing details about the change
	 */
	@Override
	public void insertUpdate(DocumentEvent event)
	{
		SwingUtilities.invokeLater(() -> removeLines(event));
	}
	
	/**
	 * This method handles the removal of text from a {@code Document}.<br>
	 * It is currently a placeholder and performs no actions.
	 * @param event the {@code DocumentEvent} containing information about the change
	 */
	@Override
	public void removeUpdate(DocumentEvent event)
	{
		// No action required.
	}
	
	/**
	 * Handles updates to the {@code Document} when text is modified.<br>
	 * This method is currently a placeholder and performs no actions.
	 * @param event the {@code DocumentEvent} containing details about the change
	 */
	@Override
	public void changedUpdate(DocumentEvent event)
	{
		// No action required.
	}
}
