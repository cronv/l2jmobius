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
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.file.Path;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.l2jmobius.tools.dashboard.runtime.DashboardWorkloadCoordinator;
import org.l2jmobius.tools.dashboard.runtime.DashboardWorkloadCoordinator.WorkloadPermit;
import org.l2jmobius.tools.dashboard.runtime.ServiceSupervisor;
import org.l2jmobius.tools.dashboard.tools.OfficialTool;
import org.l2jmobius.tools.dashboard.tools.OfficialToolLauncher;
import org.l2jmobius.tools.dashboard.tools.ToolAvailability;

/**
 * Presents one official L2J Mobius utility as a dedicated Dashboard navigation page.
 * @author BazookaRpm
 */
public final class OfficialToolPage extends JPanel
{
	private final OfficialTool _tool;
	private final OfficialToolLauncher _launcher;
	private final ServiceSupervisor _login;
	private final ServiceSupervisor _game;
	private final DashboardWorkloadCoordinator _workloads;
	private WorkloadPermit _workloadPermit;
	private final JLabel _availability = DashboardUiFactory.label("Checking...", Font.BOLD, 11, ControlCenterTheme.MUTED);
	private final JLabel _status = DashboardUiFactory.label("Ready.", Font.PLAIN, 10, ControlCenterTheme.MUTED);
	private final DashboardActionButton _open;
	
	public OfficialToolPage(OfficialTool tool, OfficialToolLauncher launcher, ServiceSupervisor login, ServiceSupervisor game, DashboardWorkloadCoordinator workloads)
	{
		_tool = tool;
		_launcher = launcher;
		_login = login;
		_game = game;
		_workloads = workloads;
		setOpaque(false);
		setLayout(new BorderLayout(0, 12));
		add(createTitle(), BorderLayout.NORTH);
		final JPanel content = new JPanel(new BorderLayout(0, 12));
		content.setOpaque(false);
		content.add(createDescription(), BorderLayout.NORTH);
		content.add(createRuntimeDetails(), BorderLayout.CENTER);
		_open = new DashboardActionButton("Open " + tool.label(), ControlCenterIcons.create(iconFor(tool), 14, ControlCenterTheme.TEXT_BRIGHT), ControlCenterTheme.ACTION_PRIMARY, true);
		final JPanel actions = new JPanel(new BorderLayout(8, 0));
		actions.setOpaque(false);
		actions.add(_status, BorderLayout.CENTER);
		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		buttons.setOpaque(false);
		final DashboardActionButton folder = new DashboardActionButton("Open folder", ControlCenterIcons.create(GlyphIcon.Kind.FOLDER, 14, ControlCenterTheme.TEXT), ControlCenterTheme.ACCENT, false);
		final DashboardActionButton refresh = new DashboardActionButton("Refresh", ControlCenterIcons.create(GlyphIcon.Kind.REFRESH, 14, ControlCenterTheme.TEXT), ControlCenterTheme.ACCENT, false);
		folder.addActionListener(event -> openFolder());
		refresh.addActionListener(event -> refreshAvailability());
		_open.addActionListener(event -> launch());
		buttons.add(folder);
		buttons.add(refresh);
		buttons.add(_open);
		actions.add(buttons, BorderLayout.EAST);
		content.add(actions, BorderLayout.SOUTH);
		add(content, BorderLayout.CENTER);
		refreshAvailability();
	}
	
	private JPanel createTitle()
	{
		final JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		final JLabel title = DashboardUiFactory.label(_tool.label(), Font.BOLD, 18, ControlCenterTheme.TEXT_BRIGHT);
		final JLabel subtitle = DashboardUiFactory.label("Official L2J Mobius utility", Font.PLAIN, 10, ControlCenterTheme.MUTED);
		panel.add(title, BorderLayout.NORTH);
		panel.add(subtitle, BorderLayout.SOUTH);
		return panel;
	}
	
	private SurfacePanel createDescription()
	{
		final SurfacePanel panel = new SurfacePanel(8, ControlCenterTheme.SURFACE, ControlCenterTheme.BORDER);
		panel.setLayout(new BorderLayout(8, 8));
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ControlCenterTheme.BORDER), BorderFactory.createEmptyBorder(14, 14, 14, 14)));
		final JLabel description = new JLabel("<html>" + _tool.description() + "<br><br>The Dashboard starts the source-provided launcher and the utility opens in its own official DarkTheme window.</html>");
		description.setFont(ControlCenterTheme.font(Font.PLAIN, 11));
		description.setForeground(ControlCenterTheme.TEXT);
		panel.add(description, BorderLayout.CENTER);
		return panel;
	}
	
	private SurfacePanel createRuntimeDetails()
	{
		final SurfacePanel panel = new SurfacePanel(8, ControlCenterTheme.SURFACE, ControlCenterTheme.BORDER);
		panel.setLayout(new BorderLayout(0, 10));
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ControlCenterTheme.BORDER), BorderFactory.createEmptyBorder(14, 14, 14, 14)));
		panel.add(DashboardUiFactory.label("Runtime", Font.BOLD, 12, ControlCenterTheme.TEXT_BRIGHT), BorderLayout.NORTH);
		final JPanel details = new JPanel(new GridBagLayout());
		details.setOpaque(false);
		addDetail(details, 0, "Status", _availability);
		addDetail(details, 1, "Working directory", DashboardUiFactory.label(_launcher.workingDirectory(_tool).toString(), Font.PLAIN, 10, ControlCenterTheme.TEXT));
		addDetail(details, 2, "Launcher", DashboardUiFactory.label(_launcher.launcherScript(_tool).toString(), Font.PLAIN, 10, ControlCenterTheme.TEXT));
		addDetail(details, 3, "Runtime jar", DashboardUiFactory.label(_tool.jarName(), Font.PLAIN, 10, ControlCenterTheme.TEXT));
		addDetail(details, 4, "Main class", DashboardUiFactory.label(_tool.mainClass(), Font.PLAIN, 10, ControlCenterTheme.TEXT));
		if (_tool.servicesMustBeOffline())
		{
			addDetail(details, 5, "Safety", DashboardUiFactory.label("LoginServer and GameServer must be offline before launch.", Font.PLAIN, 10, ControlCenterTheme.YELLOW));
		}
		
		panel.add(details, BorderLayout.CENTER);
		return panel;
	}
	
	private void addDetail(JPanel panel, int row, String label, JLabel value)
	{
		final GridBagConstraints left = new GridBagConstraints();
		left.gridx = 0;
		left.gridy = row;
		left.anchor = GridBagConstraints.NORTHWEST;
		left.insets = new Insets(4, 0, 4, 16);
		panel.add(DashboardUiFactory.label(label, Font.BOLD, 10, ControlCenterTheme.MUTED), left);
		final GridBagConstraints right = new GridBagConstraints();
		right.gridx = 1;
		right.gridy = row;
		right.weightx = 1;
		right.fill = GridBagConstraints.HORIZONTAL;
		right.anchor = GridBagConstraints.NORTHWEST;
		right.insets = new Insets(4, 0, 4, 0);
		panel.add(value, right);
	}
	
	private void refreshAvailability()
	{
		final ToolAvailability availability = _launcher.inspect(_tool);
		_availability.setText(availability.available() ? "AVAILABLE" : "UNAVAILABLE");
		_availability.setForeground(availability.available() ? ControlCenterTheme.GREEN : ControlCenterTheme.RED);
		_status.setText(availability.detail());
		_open.setEnabled(availability.available() && !_launcher.isRunning(_tool));
	}
	
	private void launch()
	{
		if (_tool.servicesMustBeOffline() && (_login.isRunning() || _game.isRunning()))
		{
			JOptionPane.showMessageDialog(this, "Stop both LoginServer and GameServer before opening " + _tool.label() + ".", "Tool launch blocked", JOptionPane.WARNING_MESSAGE);
			return;
		}
		
		if (_launcher.isRunning(_tool))
		{
			JOptionPane.showMessageDialog(this, _tool.label() + " is already running.", "Tool already running", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		final ToolAvailability availability = _launcher.inspect(_tool);
		if (!availability.available())
		{
			refreshAvailability();
			JOptionPane.showMessageDialog(this, availability.detail(), _tool.label(), JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		_workloadPermit = _workloads.tryAcquireHeavy(_tool.label());
		if (_workloadPermit == null)
		{
			final String reason = _workloads.maintenanceBlockReason();
			_status.setText(reason);
			JOptionPane.showMessageDialog(this, reason, _tool.label(), JOptionPane.WARNING_MESSAGE);
			return;
		}
		
		try
		{
			final Process process = _launcher.launch(_tool);
			_open.setEnabled(false);
			_open.setText("Running");
			_availability.setText("RUNNING");
			_availability.setForeground(ControlCenterTheme.GREEN);
			_status.setText(_tool.label() + " started through " + _launcher.launcherScript(_tool).getFileName() + ".");
			waitForExit(process);
		}
		catch (IOException e)
		{
			releaseWorkloadPermit();
			_status.setText(_tool.label() + " could not be launched: " + e.getMessage());
			JOptionPane.showMessageDialog(this, e.getMessage() + "\n\nLauncher log: " + _launcher.logFile(_tool), _tool.label(), JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void waitForExit(Process process)
	{
		new SwingWorker<Integer, Void>()
		{
			@Override
			protected Integer doInBackground() throws InterruptedException
			{
				return process.waitFor();
			}
			
			@Override
			protected void done()
			{
				_launcher.release(_tool, process);
				releaseWorkloadPermit();
				_open.setText("Open " + _tool.label());
				try
				{
					final int exitCode = get();
					_status.setText(_tool.label() + " closed. Launcher exit code: " + exitCode + ". Log: " + _launcher.logFile(_tool));
				}
				catch (Exception e)
				{
					_status.setText(_tool.label() + " closed; exit status could not be read.");
				}
				
				refreshAvailability();
			}
		}.execute();
	}
	
	private void releaseWorkloadPermit()
	{
		if (_workloadPermit != null)
		{
			_workloadPermit.close();
			_workloadPermit = null;
		}
	}
	
	private void openFolder()
	{
		final Path folder = _launcher.workingDirectory(_tool);
		try
		{
			if (!Desktop.isDesktopSupported())
			{
				throw new IOException("Desktop integration is unavailable.");
			}
			
			Desktop.getDesktop().open(folder.toFile());
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this), "Unable to open " + folder + ": " + e.getMessage(), _tool.label(), JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private static GlyphIcon.Kind iconFor(OfficialTool tool)
	{
		switch (tool)
		{
			case MAP_EXTRACTOR:
			{
				return GlyphIcon.Kind.TOOLS;
			}
			case SEARCH:
			{
				return GlyphIcon.Kind.SEARCH;
			}
			case ACCOUNT_MANAGER:
			{
				return GlyphIcon.Kind.DETAILS;
			}
			case GAME_SERVER_REGISTER:
			{
				return GlyphIcon.Kind.SERVICE;
			}
			default:
			{
				return GlyphIcon.Kind.TOOLS;
			}
		}
	}
}
