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

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;

import javax.swing.JFrame;

/**
 * Standalone Drop List Editor window backed by the reusable {@link org.l2jmobius.tools.editor.drop.ui.DropEditorPanel}.
 * @author BazookaRpm
 */
public final class DropEditorFrame extends JFrame
{
	private static final long serialVersionUID = 1L;
	
	private final DropEditorPanel _editor;
	
	public DropEditorFrame(Path startPath)
	{
		super("Mobius - Drop Editor");
		_editor = new DropEditorPanel(startPath);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setMinimumSize(new Dimension(1280, 760));
		setSize(1600, 900);
		setContentPane(_editor);
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				if (_editor.confirmDiscardOrSave())
				{
					dispose();
				}
			}
		});
		_editor.start();
	}
	
	public void selectNpc(int npcId)
	{
		_editor.selectNpc(npcId);
	}
}
