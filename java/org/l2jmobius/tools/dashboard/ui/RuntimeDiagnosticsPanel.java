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
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import org.l2jmobius.tools.dashboard.bridge.DashboardProfile;
import org.l2jmobius.tools.dashboard.runtime.ServiceObserver;
import org.l2jmobius.tools.dashboard.runtime.ServicePhase;
import org.l2jmobius.tools.dashboard.runtime.ServiceRole;
import org.l2jmobius.tools.dashboard.runtime.ServiceSupervisor;
import org.l2jmobius.tools.dashboard.settings.ControlCenterSettings;
import org.l2jmobius.tools.dashboard.telemetry.RuntimeSnapshot;

/**
 * Provides read-only runtime snapshots and thread dumps without exposing credentials or management tokens.
 * @author BazookaRpm
 */
public final class RuntimeDiagnosticsPanel extends JPanel implements DashboardPageLifecycle
{
	private static final DateTimeFormatter SNAPSHOT_FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
	private final ControlCenterSettings _settings;
	private final ServiceSupervisor _loginService;
	private final ServiceSupervisor _gameService;
	private final JTextArea _viewer = new JTextArea();
	private final DiagnosticRecord _loginRecord = new DiagnosticRecord(ServiceRole.LOGIN);
	private final DiagnosticRecord _gameRecord = new DiagnosticRecord(ServiceRole.GAME);
	private final SnapshotObserver _loginObserver = new SnapshotObserver(_loginRecord);
	private final SnapshotObserver _gameObserver = new SnapshotObserver(_gameRecord);
	private boolean _active;
	
	public RuntimeDiagnosticsPanel(ControlCenterSettings settings, ServiceSupervisor login, ServiceSupervisor game)
	{
		_settings = settings;
		_loginService = login;
		_gameService = game;
		setOpaque(false);
		setLayout(new BorderLayout());
		add(buildCommandStrip(), BorderLayout.PAGE_START);
		configureViewer();
		final JScrollPane scroll = DashboardUiFactory.scroll(_viewer);
		scroll.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
		add(scroll, BorderLayout.CENTER);
		showCurrentSnapshot();
	}
	
	@Override
	public void pageShown()
	{
		if (_active)
		{
			return;
		}
		
		_active = true;
		_loginService.addObserver(_loginObserver);
		_gameService.addObserver(_gameObserver);
		showCurrentSnapshot();
	}
	
	@Override
	public void pageHidden()
	{
		if (!_active)
		{
			return;
		}
		
		_active = false;
		_loginService.removeObserver(_loginObserver);
		_gameService.removeObserver(_gameObserver);
	}
	
	private SurfacePanel buildCommandStrip()
	{
		final SurfacePanel strip = new SurfacePanel(10, ControlCenterTheme.SURFACE, ControlCenterTheme.BORDER);
		strip.setLayout(new BorderLayout());
		strip.setBorder(BorderFactory.createEmptyBorder(9, 10, 9, 10));
		strip.add(DashboardUiFactory.label("Local diagnostics · read-only operations", Font.BOLD, 13, ControlCenterTheme.TEXT), BorderLayout.LINE_START);
		final JPanel actions = DashboardUiFactory.flow(FlowLayout.RIGHT, 6);
		final DashboardActionButton refresh = button("Snapshot", GlyphIcon.Kind.REFRESH, ControlCenterTheme.ACCENT);
		final DashboardActionButton save = button("Save snapshot", GlyphIcon.Kind.SAVE, ControlCenterTheme.ACCENT);
		final DashboardActionButton loginDump = button("Login thread dump", GlyphIcon.Kind.DIAGNOSTICS, ControlCenterTheme.MUTED);
		final DashboardActionButton gameDump = button("Game thread dump", GlyphIcon.Kind.DIAGNOSTICS, ControlCenterTheme.MUTED);
		refresh.addActionListener(event -> showCurrentSnapshot());
		save.addActionListener(event -> persistSnapshot());
		loginDump.addActionListener(event -> requestThreadDump(_loginService));
		gameDump.addActionListener(event -> requestThreadDump(_gameService));
		DashboardUiFactory.addAll(actions, refresh, save, loginDump, gameDump);
		strip.add(actions, BorderLayout.LINE_END);
		return strip;
	}
	
	private DashboardActionButton button(String text, GlyphIcon.Kind icon, Color accent)
	{
		return new DashboardActionButton(text, ControlCenterIcons.create(icon, 14, accent == ControlCenterTheme.MUTED ? ControlCenterTheme.MUTED : ControlCenterTheme.TEXT_BRIGHT), accent, false);
	}
	
	private void configureViewer()
	{
		_viewer.setEditable(false);
		_viewer.setWrapStyleWord(false);
		_viewer.setLineWrap(false);
		_viewer.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
		_viewer.setBackground(ControlCenterTheme.CONSOLE);
		_viewer.setForeground(ControlCenterTheme.TEXT);
		_viewer.setCaretColor(ControlCenterTheme.TEXT_BRIGHT);
	}
	
	private void showCurrentSnapshot()
	{
		showText(createSnapshotText());
	}
	
	private String createSnapshotText()
	{
		final List<String> lines = new ArrayList<>();
		lines.add(DashboardProfile.WINDOW_TITLE + " - Safe observability snapshot");
		lines.add("Date: " + LocalDateTime.now());
		lines.add("This snapshot excludes credentials, control tokens and Database.ini content.");
		lines.add("");
		appendService(lines, _loginRecord);
		lines.add("");
		appendService(lines, _gameRecord);
		return String.join(System.lineSeparator(), lines) + System.lineSeparator();
	}
	
	private void appendService(List<String> lines, DiagnosticRecord record)
	{
		final RuntimeSnapshot telemetry = record.snapshot();
		lines.add("[ " + record.role().label() + " ]");
		lines.add("State: " + record.phase().getLabel());
		lines.add("PID: " + (record.pid() > 0 ? record.pid() : "N/A"));
		lines.add("Port: " + (record.port() > 0 ? record.port() : "N/A"));
		lines.add("Supervisor uptime: " + duration(record.uptime()));
		lines.add("JMX: " + (telemetry.available() ? "CONNECTED" : "DISCONNECTED"));
		if (!telemetry.available())
		{
			return;
		}
		
		lines.add("Java: " + telemetry.javaVersion());
		lines.add("Heap: " + bytes(telemetry.heapUsedBytes()) + " / " + bytes(telemetry.heapMaximumBytes()));
		lines.add("Non-Heap: " + bytes(telemetry.nonHeapUsedBytes()));
		lines.add("Metaspace: " + bytes(telemetry.metaspaceUsedBytes()));
		lines.add("Process CPU: " + percent(telemetry.processCpuRatio()));
		lines.add("System CPU: " + percent(telemetry.systemCpuRatio()));
		lines.add("Threads: " + telemetry.liveThreads() + " · peak " + telemetry.peakThreads() + " · daemon " + telemetry.daemonThreads());
		lines.add("Loaded classes: " + telemetry.loadedClasses());
		lines.add("GC: " + telemetry.garbageCollectionCount() + " collections · " + telemetry.garbageCollectionMillis() + " ms");
		if (record.role() == ServiceRole.GAME)
		{
			lines.add("Players: " + telemetry.onlinePlayers() + " / " + telemetry.maximumPlayers());
		}
		else
		{
			lines.add("Connected Game Servers: " + telemetry.connectedGameServers());
			lines.add("Active connections: " + telemetry.authenticatedClients());
		}
	}
	
	private void requestThreadDump(ServiceSupervisor service)
	{
		showText("Requesting read-only thread dump from " + service.getRole().label() + "..." + System.lineSeparator());
		new SwingWorker<String, Void>()
		{
			@Override
			protected String doInBackground()
			{
				return service.readThreadDump();
			}
			
			@Override
			protected void done()
			{
				try
				{
					showText(get());
				}
				catch (Exception e)
				{
					showText("Thread dump failed: " + rootMessage(e));
				}
			}
		}.execute();
	}
	
	private void persistSnapshot()
	{
		try
		{
			final Path folder = _settings.getDistributionRoot().resolve("diagnostics");
			Files.createDirectories(folder);
			final Path destination = folder.resolve("snapshot-" + SNAPSHOT_FILE_TIME.format(LocalDateTime.now()) + ".txt");
			final String snapshot = createSnapshotText();
			Files.writeString(destination, snapshot, StandardCharsets.UTF_8);
			showText(snapshot + System.lineSeparator() + "Saved to: " + destination + System.lineSeparator());
		}
		catch (Exception e)
		{
			showText("Unable to save snapshot: " + rootMessage(e));
		}
	}
	
	private void showText(String text)
	{
		_viewer.setText(text == null ? "" : text);
		_viewer.setCaretPosition(0);
	}
	
	private static String bytes(long value)
	{
		if (value < 0)
		{
			return "N/A";
		}
		
		return value >= 1073741824L ? String.format("%.2f GB", value / 1073741824d) : String.format("%.1f MB", value / 1048576d);
	}
	
	private static String percent(double value)
	{
		return value < 0 ? "N/A" : String.format("%.1f%%", value * 100d);
	}
	
	private static String duration(long millis)
	{
		if (millis <= 0)
		{
			return "N/A";
		}
		
		final long total = millis / 1000;
		return String.format("%02d:%02d:%02d", total / 3600, (total % 3600) / 60, total % 60);
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
	
	private static final class DiagnosticRecord
	{
		private final ServiceRole _role;
		private volatile ServicePhase _phase = ServicePhase.OFFLINE;
		private volatile RuntimeSnapshot _snapshot = RuntimeSnapshot.unavailable();
		private volatile long _pid = -1;
		private volatile long _uptime;
		private volatile int _port;
		
		private DiagnosticRecord(ServiceRole role)
		{
			_role = role;
		}
		
		private ServiceRole role()
		{
			return _role;
		}
		
		private ServicePhase phase()
		{
			return _phase;
		}
		
		private RuntimeSnapshot snapshot()
		{
			return _snapshot;
		}
		
		private long pid()
		{
			return _pid;
		}
		
		private long uptime()
		{
			return _uptime;
		}
		
		private int port()
		{
			return _port;
		}
	}
	
	private static final class SnapshotObserver implements ServiceObserver
	{
		private final DiagnosticRecord _record;
		
		private SnapshotObserver(DiagnosticRecord record)
		{
			_record = record;
		}
		
		@Override
		public void phaseChanged(ServicePhase phase, String detail)
		{
			_record._phase = phase;
		}
		
		@Override
		public void runtimeChanged(long processId, long uptimeMillis, int servicePort)
		{
			_record._pid = processId;
			_record._uptime = uptimeMillis;
			_record._port = servicePort;
		}
		
		@Override
		public void telemetryChanged(RuntimeSnapshot snapshot)
		{
			_record._snapshot = snapshot;
		}
		
		@Override
		public boolean requiresTelemetry()
		{
			return true;
		}
	}
}
