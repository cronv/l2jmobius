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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.l2jmobius.tools.dashboard.logging.ConsoleMessage;
import org.l2jmobius.tools.dashboard.runtime.ServiceObserver;
import org.l2jmobius.tools.dashboard.runtime.ServicePhase;
import org.l2jmobius.tools.dashboard.runtime.ServiceSupervisor;

/**
 * Presents direct lifecycle controls and process identity details for one service.
 * @author BazookaRpm
 */
public final class ServiceControlPanel extends SurfacePanel implements ServiceObserver, DashboardPageLifecycle
{
	private final ServiceSupervisor _supervisor;
	private final StatePill _state = new StatePill();
	private final JLabel _runtime = new JLabel("PID: - | Port: - | Uptime: 00:00:00");
	private final DashboardActionButton _start;
	private final DashboardActionButton _stop;
	private final DashboardActionButton _restart;
	
	public ServiceControlPanel(ServiceSupervisor supervisor)
	{
		_supervisor = supervisor;
		setLayout(new BorderLayout(12, 12));
		setBorder(new EmptyBorder(16, 16, 16, 16));
		final JPanel title = new JPanel(new BorderLayout());
		title.setOpaque(false);
		final JLabel name = new JLabel(supervisor.getRole().label());
		name.setFont(ControlCenterTheme.font(Font.BOLD, 18));
		title.add(name, BorderLayout.WEST);
		title.add(_state, BorderLayout.EAST);
		_runtime.setForeground(ControlCenterTheme.MUTED);
		_runtime.setFont(ControlCenterTheme.font(Font.PLAIN, 11));
		final JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		actions.setOpaque(false);
		_start = new DashboardActionButton("Start", ControlCenterIcons.create(GlyphIcon.Kind.PLAY, ControlCenterTheme.TEXT), ControlCenterTheme.ACTION_PRIMARY, true);
		_stop = new DashboardActionButton("Stop", ControlCenterIcons.create(GlyphIcon.Kind.STOP, ControlCenterTheme.TEXT), ControlCenterTheme.RED, false);
		_restart = new DashboardActionButton("Restart", ControlCenterIcons.create(GlyphIcon.Kind.RESTART, ControlCenterTheme.TEXT), ControlCenterTheme.ACCENT, false);
		_start.addActionListener(event -> _supervisor.startAsync());
		_stop.addActionListener(event -> ServiceLifecycleDialog.show(this, _supervisor, false));
		_restart.addActionListener(event -> ServiceLifecycleDialog.show(this, _supervisor, true));
		actions.add(_start);
		actions.add(_stop);
		actions.add(_restart);
		final JPanel center = new JPanel(new GridLayout(2, 1, 0, 8));
		center.setOpaque(false);
		center.add(_runtime);
		center.add(actions);
		add(title, BorderLayout.NORTH);
		add(center, BorderLayout.CENTER);
	}
	
	@Override
	public void pageShown()
	{
		_supervisor.addObserver(this);
	}
	
	@Override
	public void pageHidden()
	{
		_supervisor.removeObserver(this);
	}
	
	@Override
	public void phaseChanged(ServicePhase phase, String detail)
	{
		SwingUtilities.invokeLater(() ->
		{
			_state.setPhase(phase);
			final boolean running = phase.isProcessExpected();
			_start.setEnabled(!running || (phase == ServicePhase.FAILED));
			_stop.setEnabled(running);
			_restart.setEnabled(running);
		});
	}
	
	@Override
	public void lifecycleActionChanged(boolean scheduled, boolean restart, int secondsRemaining)
	{
		SwingUtilities.invokeLater(() ->
		{
			_stop.setText(scheduled && !restart ? "Stop " + secondsRemaining + "s" : "Stop");
			_restart.setText(scheduled && restart ? "Restart " + secondsRemaining + "s" : "Restart");
		});
	}
	
	@Override
	public void runtimeChanged(long processId, long uptimeMillis, int servicePort)
	{
		SwingUtilities.invokeLater(() -> _runtime.setText("PID: " + ((processId > 0) ? processId : "-") + " | Port: " + servicePort + " | Uptime: " + formatUptime(uptimeMillis)));
	}
	
	@Override
	public void consoleMessage(ConsoleMessage message)
	{
		// Console messages are handled by the dedicated log view.
	}
	
	private static String formatUptime(long millis)
	{
		final long seconds = Math.max(0, millis / 1000);
		return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
	}
}
