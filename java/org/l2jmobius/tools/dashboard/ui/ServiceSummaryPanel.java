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
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.l2jmobius.tools.dashboard.logging.ConsoleMessage;
import org.l2jmobius.tools.dashboard.logging.MessageSeverity;
import org.l2jmobius.tools.dashboard.runtime.ServiceObserver;
import org.l2jmobius.tools.dashboard.runtime.ServicePhase;
import org.l2jmobius.tools.dashboard.runtime.ServiceRole;
import org.l2jmobius.tools.dashboard.runtime.ServiceSupervisor;
import org.l2jmobius.tools.dashboard.settings.ControlCenterSettings;
import org.l2jmobius.tools.dashboard.telemetry.RuntimeSnapshot;

/**
 * Displays lifecycle controls, runtime metrics, and the central live console for one managed server process.
 * @author BazookaRpm
 */
public final class ServiceSummaryPanel extends SurfacePanel implements ServiceObserver, DashboardPageLifecycle
{
	private final ControlCenterSettings _settings;
	private final ServiceSupervisor _service;
	private final Runnable _startRequest;
	private final StatePill _phasePill = new StatePill();
	private final ValueCard[] _summaryCards;
	private final RadialMeter[] _meters =
	{
		new RadialMeter("JVM Heap"),
		new RadialMeter("CPU"),
		new RadialMeter("System RAM")
	};
	private final LogStreamPanel _log;
	private final DashboardActionButton _startButton;
	private final DashboardActionButton _stopButton;
	private final DashboardActionButton _restartButton;
	private final CardLayout _viewLayout = new CardLayout();
	private final JPanel _viewDeck = DashboardUiFactory.panel(_viewLayout);
	private final JLabel[] _detailValues = new JLabel[9];
	private volatile long _managedPid = -1;
	private volatile int _servicePort;
	private volatile ServicePhase _currentPhase = ServicePhase.OFFLINE;
	
	public ServiceSummaryPanel(ControlCenterSettings settings, ServiceSupervisor supervisor, Runnable startAction, boolean summary, Runnable openDetails)
	{
		super(12, ControlCenterTheme.SURFACE, ControlCenterTheme.BORDER);
		_settings = settings;
		_service = supervisor;
		_startRequest = startAction;
		final boolean login = supervisor.getRole() == ServiceRole.LOGIN;
		_summaryCards = new ValueCard[]
		{
			new ValueCard("Uptime", "N/A"),
			new ValueCard("Java", "N/A"),
			new ValueCard("Client port", "N/A"),
			new ValueCard(login ? "Connected Game Servers" : "Maximum players", "N/A"),
			new ValueCard(login ? "Active connections" : "Online players", "N/A"),
			new ValueCard("PID", "N/A")
		};
		_log = new LogStreamPanel("Live log output", settings.getConsoleLineLimit(), supervisor);
		_startButton = actionButton("Start", GlyphIcon.Kind.PLAY, ControlCenterTheme.TEXT_BRIGHT);
		_stopButton = actionButton("Stop", GlyphIcon.Kind.STOP, ControlCenterTheme.MUTED);
		_restartButton = actionButton("Restart", GlyphIcon.Kind.RESTART, ControlCenterTheme.MUTED);
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		setLayout(new BorderLayout(0, 7));
		add(buildHeader(summary, openDetails), BorderLayout.PAGE_START);
		add(buildCenter(), BorderLayout.CENTER);
		add(buildFooter(), BorderLayout.PAGE_END);
		setPreferredSize(new Dimension(summary ? 520 : 860, summary ? 620 : 680));
	}
	
	public ServiceSummaryPanel(ControlCenterSettings settings, ServiceSupervisor supervisor, Runnable startAction)
	{
		this(settings, supervisor, startAction, false, null);
	}
	
	private JPanel buildHeader(boolean summary, Runnable openDetails)
	{
		final JPanel header = DashboardUiFactory.panel(new GridBagLayout());
		final JPanel identity = DashboardUiFactory.flow(FlowLayout.LEFT, 9);
		DashboardUiFactory.addAll(identity, new JLabel(ControlCenterIcons.create(GlyphIcon.Kind.SERVICE, 20, ControlCenterTheme.TEXT)), DashboardUiFactory.label(_service.getRole().label().toUpperCase(), Font.BOLD, 18, ControlCenterTheme.TEXT));
		final GridBagConstraints left = new GridBagConstraints();
		left.gridx = 0;
		left.gridy = 0;
		left.weightx = 1;
		left.anchor = GridBagConstraints.WEST;
		header.add(identity, left);
		final JPanel controls = DashboardUiFactory.flow(FlowLayout.RIGHT, 7);
		if (summary && (openDetails != null))
		{
			final DashboardActionButton detailButton = actionButton("Details", GlyphIcon.Kind.DETAILS, ControlCenterTheme.TEXT_BRIGHT);
			detailButton.addActionListener(event -> openDetails.run());
			controls.add(detailButton);
		}
		
		controls.add(_phasePill);
		final GridBagConstraints right = new GridBagConstraints();
		right.gridx = 1;
		right.gridy = 0;
		right.anchor = GridBagConstraints.EAST;
		header.add(controls, right);
		return header;
	}
	
	private JPanel buildCenter()
	{
		final JPanel center = DashboardUiFactory.panel(new BorderLayout(0, 6));
		center.add(DashboardUiFactory.grid(3, 6, _summaryCards), BorderLayout.PAGE_START);
		_viewDeck.add(buildMetricView(), "metrics");
		_viewDeck.add(buildDetailView(), "details");
		final JPanel switcher = DashboardUiFactory.panel(new BorderLayout(0, 5));
		switcher.add(buildViewTabs(), BorderLayout.PAGE_START);
		switcher.add(_viewDeck, BorderLayout.CENTER);
		center.add(switcher, BorderLayout.CENTER);
		return center;
	}
	
	private JPanel buildViewTabs()
	{
		final JToggleButton metrics = createTab("Metrics", true);
		final JToggleButton details = createTab("Details", false);
		final ButtonGroup group = new ButtonGroup();
		group.add(metrics);
		group.add(details);
		metrics.addActionListener(event -> _viewLayout.show(_viewDeck, "metrics"));
		details.addActionListener(event -> _viewLayout.show(_viewDeck, "details"));
		return DashboardUiFactory.grid(2, 4, metrics, details);
	}
	
	private JToggleButton createTab(String text, boolean selected)
	{
		final JToggleButton tab = new JToggleButton(text, selected);
		tab.setFont(ControlCenterTheme.font(Font.PLAIN, 11));
		tab.setFocusPainted(false);
		tab.setOpaque(true);
		tab.setContentAreaFilled(true);
		tab.setBorder(BorderFactory.createLineBorder(ControlCenterTheme.BORDER));
		final Runnable refresh = () ->
		{
			tab.setForeground(tab.isSelected() ? ControlCenterTheme.TEXT_BRIGHT : ControlCenterTheme.TEXT);
			tab.setBackground(tab.isSelected() ? ControlCenterTheme.ACCENT_DARK : ControlCenterTheme.SURFACE_ALT);
		};
		tab.addItemListener(event -> refresh.run());
		refresh.run();
		return tab;
	}
	
	private SurfacePanel buildMetricView()
	{
		final SurfacePanel panel = new SurfacePanel(10, ControlCenterTheme.SURFACE, ControlCenterTheme.BORDER);
		panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		panel.setLayout(new BorderLayout(0, 3));
		panel.add(DashboardUiFactory.label("SYSTEM METRICS", Font.PLAIN, 10, ControlCenterTheme.MUTED), BorderLayout.PAGE_START);
		panel.add(DashboardUiFactory.grid(3, 7, _meters), BorderLayout.CENTER);
		return panel;
	}
	
	private SurfacePanel buildDetailView()
	{
		final String[] names =
		{
			"Overall state",
			"Process",
			"Managed PID",
			"Service port",
			"JMX telemetry",
			"Uptime",
			"Working directory",
			"Server JAR",
			"JVM configuration"
		};
		final String[] initial =
		{
			"N/A",
			"STOPPED",
			"N/A",
			"N/A",
			"DISCONNECTED",
			"N/A",
			_settings.getServiceDirectory(_service.getRole()).toString(),
			_settings.getServiceJar(_service.getRole()).toString(),
			_settings.getJavaOptionsFile(_service.getRole()).toString()
		};
		final SurfacePanel panel = new SurfacePanel(10, ControlCenterTheme.SURFACE, ControlCenterTheme.BORDER);
		panel.setLayout(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
		for (int index = 0; index < names.length; index++)
		{
			_detailValues[index] = DashboardUiFactory.label(initial[index], Font.PLAIN, 10, ControlCenterTheme.TEXT);
			final GridBagConstraints nameConstraints = detailConstraints(0, index, 0);
			panel.add(DashboardUiFactory.label(names[index] + ":", Font.PLAIN, 10, ControlCenterTheme.MUTED), nameConstraints);
			panel.add(_detailValues[index], detailConstraints(1, index, 1));
		}
		
		return panel;
	}
	
	private GridBagConstraints detailConstraints(int column, int row, int weight)
	{
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = column;
		constraints.gridy = row;
		constraints.weightx = weight;
		constraints.fill = column == 1 ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = new Insets(3, 0, 3, column == 0 ? 12 : 0);
		return constraints;
	}
	
	private JPanel buildFooter()
	{
		final JPanel footer = DashboardUiFactory.panel(new BorderLayout(0, 6));
		footer.add(buildActions(), BorderLayout.PAGE_START);
		footer.add(_log, BorderLayout.CENTER);
		return footer;
	}
	
	private JPanel buildActions()
	{
		_startButton.addActionListener(event -> _startRequest.run());
		_stopButton.addActionListener(event -> ServiceLifecycleDialog.show(this, _service, false));
		_restartButton.addActionListener(event -> ServiceLifecycleDialog.show(this, _service, true));
		final DashboardActionButton logs = actionButton("Logs", GlyphIcon.Kind.LOG, ControlCenterTheme.TEXT_BRIGHT);
		final DashboardActionButton configuration = actionButton("Configuration", GlyphIcon.Kind.SETTINGS, ControlCenterTheme.TEXT);
		logs.addActionListener(event -> openLocation(_settings.getServerLogDirectory(_service.getRole())));
		configuration.addActionListener(event -> openLocation(_settings.getServiceDirectory(_service.getRole()).resolve("config")));
		return DashboardUiFactory.grid(5, 5, _startButton, _stopButton, _restartButton, logs, configuration);
	}
	
	private DashboardActionButton actionButton(String text, GlyphIcon.Kind icon, Color accent)
	{
		return new DashboardActionButton(text, ControlCenterIcons.create(icon, 14, accent), ControlCenterTheme.ACCENT, false);
	}
	
	private void openLocation(Path location)
	{
		try
		{
			if (!Desktop.isDesktopSupported() || !Files.exists(location))
			{
				throw new IllegalStateException("Desktop location is not available: " + location);
			}
			
			Desktop.getDesktop().open(location.toFile());
		}
		catch (Exception e)
		{
			_log.append(new ConsoleMessage(Instant.now(), _service.getRole(), MessageSeverity.WARNING, "Unable to open " + location + ": " + rootMessage(e)));
		}
	}
	
	@Override
	public void pageShown()
	{
		_service.addObserver(this);
		_log.pageShown();
	}
	
	@Override
	public void pageHidden()
	{
		_log.pageHidden();
		_service.removeObserver(this);
	}
	
	@Override
	public boolean requiresTelemetry()
	{
		return true;
	}
	
	@Override
	public void phaseChanged(ServicePhase phase, String detail)
	{
		_currentPhase = phase;
		EventQueue.invokeLater(() -> applyPhase(phase, detail));
	}
	
	private void applyPhase(ServicePhase phase, String detail)
	{
		_phasePill.setPhase(phase);
		_phasePill.setToolTipText(detail);
		_detailValues[0].setText(phase.getLabel() + " - " + detail);
		refreshProcessStatus();
		final boolean active = phase.isProcessExpected();
		_startButton.setEnabled(!active && (phase != ServicePhase.STOPPING));
		_stopButton.setEnabled(active);
		_restartButton.setEnabled(phase != ServicePhase.STOPPING);
	}
	
	@Override
	public void lifecycleActionChanged(boolean scheduled, boolean restart, int secondsRemaining)
	{
		EventQueue.invokeLater(() ->
		{
			_stopButton.setText(scheduled && !restart ? "Stop " + secondsRemaining + "s" : "Stop");
			_restartButton.setText(scheduled && restart ? "Restart " + secondsRemaining + "s" : "Restart");
		});
	}
	
	@Override
	public void runtimeChanged(long processId, long uptimeMillis, int servicePort)
	{
		_managedPid = processId;
		_servicePort = servicePort;
		EventQueue.invokeLater(() -> applyRuntime(processId, uptimeMillis, servicePort));
	}
	
	private void applyRuntime(long processId, long uptimeMillis, int servicePort)
	{
		final String uptime = processId > 0 ? elapsedTime(uptimeMillis) : "N/A";
		final String pid = processId > 0 ? Long.toString(processId) : "N/A";
		_summaryCards[0].setValue(uptime);
		_summaryCards[2].setValue(servicePort > 0 ? Integer.toString(servicePort) : "N/A");
		_summaryCards[5].setValue(pid);
		_detailValues[2].setText(pid);
		_detailValues[5].setText(uptime);
		refreshProcessStatus();
		if (_currentPhase != ServicePhase.ONLINE)
		{
			resetTelemetry("Waiting for server");
		}
	}
	
	@Override
	public void telemetryChanged(RuntimeSnapshot snapshot)
	{
		EventQueue.invokeLater(() -> applyTelemetry(snapshot));
	}
	
	private void applyTelemetry(RuntimeSnapshot snapshot)
	{
		if (!snapshot.available())
		{
			_detailValues[4].setText("DISCONNECTED");
			_summaryCards[1].setValue("N/A");
			resetTelemetry(_currentPhase == ServicePhase.ONLINE ? "JMX unavailable" : "Waiting for server");
			return;
		}
		
		_detailValues[4].setText("CONNECTED");
		_summaryCards[1].setValue(snapshot.javaVersion());
		_meters[0].update(snapshot.heapRatio(), readableBytes(snapshot.heapUsedBytes()), "of " + readableBytes(snapshot.heapMaximumBytes()));
		_meters[1].update(snapshot.processCpuRatio(), percent(snapshot.processCpuRatio()), snapshot.liveThreads() >= 0 ? snapshot.liveThreads() + " threads" : "Threads N/A");
		_meters[2].update(memoryFraction(snapshot), readableBytes(snapshot.systemMemoryUsedBytes()), "of " + readableBytes(snapshot.systemMemoryTotalBytes()));
		if (_service.getRole() == ServiceRole.LOGIN)
		{
			_summaryCards[3].setValue(number(snapshot.connectedGameServers()));
			_summaryCards[4].setValue(number(snapshot.authenticatedClients()));
		}
		else
		{
			_summaryCards[3].setValue(number(snapshot.maximumPlayers()));
			_summaryCards[4].setValue(number(snapshot.onlinePlayers()));
		}
	}
	
	private void refreshProcessStatus()
	{
		final boolean alive = _managedPid > 0;
		_detailValues[1].setText(alive ? "RUNNING" : "STOPPED");
		String label = "CLOSED";
		if (_currentPhase == ServicePhase.ONLINE)
		{
			label = "OPEN";
		}
		else if ((_currentPhase == ServicePhase.EXTERNAL) && !alive)
		{
			label = "OPEN WITHOUT MANAGED PID";
		}
		else if (_currentPhase == ServicePhase.LAUNCHING)
		{
			label = "WAITING FOR PORT";
		}
		
		_detailValues[3].setText((_servicePort > 0 ? _servicePort + " - " : "") + label);
	}
	
	private void resetTelemetry(String reason)
	{
		for (RadialMeter meter : _meters)
		{
			meter.update(-1, "N/A", reason);
		}
		
		_summaryCards[3].setValue("N/A");
		_summaryCards[4].setValue("N/A");
	}
	
	private static double memoryFraction(RuntimeSnapshot snapshot)
	{
		final long total = snapshot.systemMemoryTotalBytes();
		final long used = snapshot.systemMemoryUsedBytes();
		return (used < 0) || (total <= 0) ? -1 : Math.min(1d, (double) used / total);
	}
	
	private static String number(int value)
	{
		return value < 0 ? "N/A" : Integer.toString(value);
	}
	
	private static String percent(double value)
	{
		return value < 0 ? "N/A" : String.format("%.1f%%", value * 100d);
	}
	
	private static String readableBytes(long value)
	{
		if (value < 0)
		{
			return "N/A";
		}
		
		return value >= 1073741824L ? String.format("%.2f GB", value / 1073741824d) : String.format("%.1f MB", value / 1048576d);
	}
	
	private static String elapsedTime(long millis)
	{
		final long seconds = Math.max(0, millis / 1000);
		return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
	}
	
	private static String rootMessage(Exception exception)
	{
		Throwable current = exception;
		while ((current.getCause() != null) && (current.getCause() != current))
		{
			current = current.getCause();
		}
		
		return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
	}
}
