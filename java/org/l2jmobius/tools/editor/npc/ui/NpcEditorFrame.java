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

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;

import javax.swing.JFrame;

import org.l2jmobius.tools.editor.drop.ui.DropEditorFrame;

/**
 * Standalone NPC/Mob Editor window backed by the reusable {@link org.l2jmobius.tools.editor.npc.ui.NpcEditorPanel}.
 * @author BazookaRpm
 */
public final class NpcEditorFrame extends JFrame
{
	private static final long serialVersionUID = 1L;
	
	private final Path _startPath;
	private final NpcEditorPanel _editor;
	
	public NpcEditorFrame(Path startPath)
	{
		super("Mobius - NPC / Mob Editor");
		_startPath = startPath.toAbsolutePath().normalize();
		_editor = new NpcEditorPanel(_startPath);
		_editor.setOpenDropsAction(this::openDrops);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setMinimumSize(new Dimension(1280, 760));
		setSize(1600, 900);
		setContentPane(_editor);
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent event)
			{
				if (_editor.confirmDiscardOrSave())
				{
					dispose();
				}
			}
		});
		_editor.start();
	}
	
	private void openDrops(int npcId)
	{
		final DropEditorFrame frame = new DropEditorFrame(_startPath);
		frame.selectNpc(npcId);
		frame.setLocationRelativeTo(this);
		frame.setVisible(true);
	}
}
