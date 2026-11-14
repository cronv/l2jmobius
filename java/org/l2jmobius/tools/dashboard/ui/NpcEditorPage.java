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
import java.awt.CardLayout;
import java.awt.Font;
import java.util.function.IntConsumer;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.l2jmobius.tools.dashboard.runtime.DashboardWorkloadCoordinator;
import org.l2jmobius.tools.dashboard.runtime.DashboardWorkloadCoordinator.WorkloadPermit;
import org.l2jmobius.tools.dashboard.settings.ControlCenterSettings;
import org.l2jmobius.tools.editor.npc.ui.NpcEditorPanel;

/**
 * Embeds the NPC/Mob Editor directly in the Control Dashboard workspace.
 * @author BazookaRpm
 */
public final class NpcEditorPage extends JPanel implements DashboardPageLifecycle
{
	private static final long serialVersionUID = 1L;
	private static final String EDITOR_CARD = "editor";
	private static final String BLOCKED_CARD = "blocked";
	
	private final DashboardWorkloadCoordinator _workloads;
	private final CardLayout _contentLayout = new CardLayout();
	private final JPanel _content = DashboardUiFactory.panel(_contentLayout);
	private final NpcEditorPanel _editor;
	private final JLabel _blockedMessage = DashboardUiFactory.label("NPC / Mob Editor is unavailable.", Font.PLAIN, 11, ControlCenterTheme.YELLOW);
	private WorkloadPermit _workloadPermit;
	private boolean _visible;
	private boolean _releaseWhenIdle;
	
	public NpcEditorPage(ControlCenterSettings settings, DashboardWorkloadCoordinator workloads)
	{
		this(settings, workloads, null);
	}
	
	public NpcEditorPage(ControlCenterSettings settings, DashboardWorkloadCoordinator workloads, IntConsumer openDropsAction)
	{
		_workloads = workloads;
		_editor = new NpcEditorPanel(settings.getDistributionRoot());
		_editor.setActivityListener(this::activityChanged);
		_editor.setOpenDropsAction(openDropsAction);
		setOpaque(false);
		setLayout(new BorderLayout());
		_content.add(_editor, EDITOR_CARD);
		_content.add(createBlockedPanel(), BLOCKED_CARD);
		add(_content, BorderLayout.CENTER);
		_workloads.addListener(this::workloadStateChanged);
	}
	
	@Override
	public void pageShown()
	{
		_visible = true;
		_releaseWhenIdle = false;
		refreshAccess();
	}
	
	@Override
	public void pageHidden()
	{
		_visible = false;
		if (_editor.isBusy())
		{
			_releaseWhenIdle = true;
			return;
		}
		
		releasePermit();
	}
	
	@Override
	public boolean prepareForClose()
	{
		if (!_editor.hasUnsavedChanges())
		{
			return true;
		}
		
		boolean temporaryPermit = false;
		if (_workloadPermit == null)
		{
			_workloadPermit = _workloads.tryAcquireHeavy("NPC / Mob Editor");
			temporaryPermit = _workloadPermit != null;
		}
		
		if (_workloadPermit == null)
		{
			final Object[] choices =
			{
				"Discard changes",
				"Cancel"
			};
			final String reason = _workloads.maintenanceBlockReason();
			final String detail = reason.isEmpty() ? "Datapack maintenance is unavailable." : reason;
			if (JOptionPane.showOptionDialog(this, "The NPC / Mob Editor has unsaved changes. " + detail + " XML cannot be saved safely during dashboard shutdown.", "Unsaved NPC changes", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, choices, choices[1]) == 0)
			{
				_editor.discardUnsavedChanges();
				return true;
			}
			
			return false;
		}
		
		try
		{
			return _editor.confirmDiscardOrSave();
		}
		finally
		{
			if (temporaryPermit)
			{
				releasePermit();
			}
		}
	}
	
	private JPanel createBlockedPanel()
	{
		final SurfacePanel panel = new SurfacePanel(12, ControlCenterTheme.SURFACE, ControlCenterTheme.BORDER);
		panel.setLayout(new BorderLayout(0, 10));
		panel.setBorder(BorderFactory.createCompoundBorder(panel.getBorder(), BorderFactory.createEmptyBorder(24, 24, 24, 24)));
		final JLabel title = DashboardUiFactory.label("NPC / Mob Editor locked", Font.BOLD, 16, ControlCenterTheme.TEXT_BRIGHT);
		_blockedMessage.setHorizontalAlignment(SwingConstants.LEFT);
		panel.add(title, BorderLayout.NORTH);
		panel.add(_blockedMessage, BorderLayout.CENTER);
		return panel;
	}
	
	private void workloadStateChanged()
	{
		if (_visible)
		{
			SwingUtilities.invokeLater(this::refreshAccess);
		}
	}
	
	private void refreshAccess()
	{
		if (!_visible)
		{
			return;
		}
		
		if (_workloadPermit != null)
		{
			_contentLayout.show(_content, EDITOR_CARD);
			_editor.start();
			return;
		}
		
		_workloadPermit = _workloads.tryAcquireHeavy("NPC / Mob Editor");
		if (_workloadPermit == null)
		{
			final String reason = _workloads.maintenanceBlockReason();
			_blockedMessage.setText(reason.isEmpty() ? "Stop Login Server and Game Server before editing datapack XML." : reason);
			_contentLayout.show(_content, BLOCKED_CARD);
			return;
		}
		
		_contentLayout.show(_content, EDITOR_CARD);
		_editor.start();
	}
	
	private void activityChanged(boolean busy)
	{
		if (!busy && _releaseWhenIdle && !_visible)
		{
			_releaseWhenIdle = false;
			releasePermit();
		}
	}
	
	private void releasePermit()
	{
		if (_workloadPermit != null)
		{
			_workloadPermit.close();
			_workloadPermit = null;
		}
	}
}
