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
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import org.l2jmobius.tools.dashboard.health.HealthFinding;
import org.l2jmobius.tools.dashboard.health.HealthPolicy;
import org.l2jmobius.tools.dashboard.health.SampleSeries;
import org.l2jmobius.tools.dashboard.logging.ConsoleMessageBuffer.ConsoleCounts;
import org.l2jmobius.tools.dashboard.runtime.ServiceObserver;
import org.l2jmobius.tools.dashboard.runtime.ServicePhase;
import org.l2jmobius.tools.dashboard.runtime.ServiceRole;
import org.l2jmobius.tools.dashboard.runtime.ServiceSupervisor;
import org.l2jmobius.tools.dashboard.settings.ControlCenterSettings;
import org.l2jmobius.tools.dashboard.telemetry.RuntimeSnapshot;

/**
 * Displays current JVM telemetry, bounded history charts, health findings, and coalesced console counters for one service. <br>
 * Telemetry observation and counter rendering remain active only while the containing page is visible.
 * @author BazookaRpm
 */
public final class ServiceTelemetryPanel extends SurfacePanel implements ServiceObserver, DashboardPageLifecycle
{
	private final ControlCenterSettings _settings;
	private final ServiceSupervisor _service;
	private final SampleSeries _samples;
	private final StatePill _statePill = new StatePill();
	private final ValueCard[] _metricCards =
	{
		new ValueCard("JVM Heap", "N/A"),
		new ValueCard("Non-Heap", "N/A"),
		new ValueCard("Metaspace", "N/A"),
		new ValueCard("Process CPU", "N/A"),
		new ValueCard("System CPU", "N/A"),
		new ValueCard("Threads", "N/A"),
		new ValueCard("GC", "N/A"),
		new ValueCard("Loaded classes", "N/A")
	};
	private final JLabel _healthLine = DashboardUiFactory.label("No active alerts.", Font.PLAIN, 10, ControlCenterTheme.GREEN);
	private final JLabel _messageCounts = DashboardUiFactory.label("INFO 0   WARNING 0   ERROR 0", Font.PLAIN, 10, ControlCenterTheme.MUTED);
	private final TelemetryChart[] _charts;
	private final Timer _counterRefresh = new Timer(1000, event -> renderMessageCounts());
	private volatile ServicePhase _servicePhase = ServicePhase.OFFLINE;
	private volatile RuntimeSnapshot _latest = RuntimeSnapshot.unavailable();
	private boolean _active;
	
	public ServiceTelemetryPanel(ControlCenterSettings settings, ServiceSupervisor supervisor)
	{
		super(12, ControlCenterTheme.SURFACE, ControlCenterTheme.BORDER);
		_settings = settings;
		_service = supervisor;
		_samples = new SampleSeries(settings.getHistoryCapacity(), settings.getTelemetryIntervalMillis());
		final ServiceRole role = supervisor.getRole();
		_charts = new TelemetryChart[]
		{
			new TelemetryChart("Process CPU", _samples, TelemetryChart.Metric.CPU, role),
			new TelemetryChart("JVM Heap", _samples, TelemetryChart.Metric.HEAP, role),
			new TelemetryChart("Active threads", _samples, TelemetryChart.Metric.THREADS, role),
			new TelemetryChart(role == ServiceRole.GAME ? "Players" : "Connections", _samples, TelemetryChart.Metric.ACTIVITY, role)
		};
		_counterRefresh.setRepeats(true);
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout(0, 8));
		add(buildHeading(), BorderLayout.PAGE_START);
		add(buildTelemetryBody(), BorderLayout.CENTER);
		add(buildStatusStrip(), BorderLayout.PAGE_END);
		setPreferredSize(new Dimension(540, 590));
	}
	
	private JPanel buildHeading()
	{
		final JPanel heading = DashboardUiFactory.panel(new BorderLayout());
		heading.add(DashboardUiFactory.label(_service.getRole().label().toUpperCase() + " · OBSERVABILITY", Font.BOLD, 15, ControlCenterTheme.TEXT), BorderLayout.LINE_START);
		heading.add(_statePill, BorderLayout.LINE_END);
		return heading;
	}
	
	private JPanel buildTelemetryBody()
	{
		final JPanel body = DashboardUiFactory.panel(new BorderLayout(0, 7));
		body.add(DashboardUiFactory.grid(4, 6, _metricCards), BorderLayout.PAGE_START);
		body.add(DashboardUiFactory.grid(2, 6, _charts), BorderLayout.CENTER);
		return body;
	}
	
	private SurfacePanel buildStatusStrip()
	{
		final SurfacePanel status = new SurfacePanel(9, ControlCenterTheme.SURFACE_ALT, ControlCenterTheme.BORDER);
		status.setLayout(new GridBagLayout());
		status.setBorder(BorderFactory.createEmptyBorder(7, 9, 7, 9));
		final JComponent[] lines =
		{
			_healthLine,
			_messageCounts
		};
		for (int index = 0; index < lines.length; index++)
		{
			final GridBagConstraints constraints = new GridBagConstraints();
			constraints.gridx = 0;
			constraints.gridy = index;
			constraints.weightx = 1;
			constraints.fill = GridBagConstraints.HORIZONTAL;
			constraints.insets = new Insets(1, 1, 1, 1);
			status.add(lines[index], constraints);
		}
		
		return status;
	}
	
	@Override
	public void pageShown()
	{
		if (_active)
		{
			return;
		}
		
		_active = true;
		_service.addObserver(this);
		renderMessageCounts();
		_counterRefresh.start();
	}
	
	@Override
	public void pageHidden()
	{
		if (!_active)
		{
			return;
		}
		
		_active = false;
		_counterRefresh.stop();
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
		_servicePhase = phase;
		EventQueue.invokeLater(() ->
		{
			_statePill.setPhase(phase);
			_statePill.setToolTipText(detail);
			applyHealthFindings();
		});
	}
	
	@Override
	public void telemetryChanged(RuntimeSnapshot snapshot)
	{
		_latest = snapshot;
		final boolean sampled = _samples.offer(snapshot);
		EventQueue.invokeLater(() ->
		{
			renderMetrics(snapshot);
			applyHealthFindings();
			if (sampled)
			{
				for (TelemetryChart chart : _charts)
				{
					chart.repaint();
				}
			}
		});
	}
	
	private void renderMessageCounts()
	{
		final ConsoleCounts counts = _service.getConsoleCounts();
		_messageCounts.setText("INFO " + counts.information() + "   WARNING " + counts.warnings() + "   ERROR " + counts.errors());
	}
	
	private void renderMetrics(RuntimeSnapshot snapshot)
	{
		final String[] values;
		if (!snapshot.available())
		{
			values = new String[]
			{
				"N/A",
				"N/A",
				"N/A",
				"N/A",
				"N/A",
				"N/A",
				"N/A",
				"N/A"
			};
		}
		else
		{
			values = new String[]
			{
				readableBytes(snapshot.heapUsedBytes()) + " / " + readableBytes(snapshot.heapMaximumBytes()),
				readableBytes(snapshot.nonHeapUsedBytes()),
				readableBytes(snapshot.metaspaceUsedBytes()),
				percentage(snapshot.processCpuRatio()),
				percentage(snapshot.systemCpuRatio()),
				snapshot.liveThreads() < 0 ? "N/A" : snapshot.liveThreads() + " / peak " + snapshot.peakThreads(),
				snapshot.garbageCollectionCount() < 0 ? "N/A" : snapshot.garbageCollectionCount() + " · " + elapsed(snapshot.garbageCollectionMillis()),
				snapshot.loadedClasses() < 0 ? "N/A" : Integer.toString(snapshot.loadedClasses())
			};
		}
		
		for (int index = 0; index < _metricCards.length; index++)
		{
			_metricCards[index].setValue(values[index]);
		}
	}
	
	private void applyHealthFindings()
	{
		final List<HealthFinding> findings = HealthPolicy.evaluate(_servicePhase, _latest, _settings.getCpuWarningPercent(), _settings.getHeapWarningPercent());
		if (findings.isEmpty())
		{
			_healthLine.setText("No active alerts. Thresholds are informational only.");
			_healthLine.setForeground(ControlCenterTheme.GREEN);
			return;
		}
		
		final List<String> messages = new ArrayList<>();
		boolean warning = false;
		boolean critical = false;
		for (HealthFinding finding : findings)
		{
			messages.add(finding.message());
			if (finding.level() == HealthFinding.Level.CRITICAL)
			{
				critical = true;
			}
			else if (finding.level() == HealthFinding.Level.WARNING)
			{
				warning = true;
			}
		}
		
		_healthLine.setText(String.join("  |  ", messages));
		if (critical)
		{
			_healthLine.setForeground(ControlCenterTheme.RED);
		}
		else if (warning)
		{
			_healthLine.setForeground(ControlCenterTheme.YELLOW);
		}
		else
		{
			_healthLine.setForeground(ControlCenterTheme.GREEN);
		}
	}
	
	private static String readableBytes(long value)
	{
		if (value < 0)
		{
			return "N/A";
		}
		
		if (value >= 1073741824L)
		{
			return String.format("%.2f GB", value / 1073741824d);
		}
		
		return String.format("%.1f MB", value / 1048576d);
	}
	
	private static String percentage(double value)
	{
		return value < 0 ? "N/A" : String.format("%.1f%%", value * 100d);
	}
	
	private static String elapsed(long millis)
	{
		if (millis < 0)
		{
			return "N/A";
		}
		
		return millis >= 60000 ? String.format("%.1f min", millis / 60000d) : String.format("%.1f s", millis / 1000d);
	}
}
