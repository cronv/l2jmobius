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
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import org.l2jmobius.tools.dashboard.logging.ConsoleMessage;
import org.l2jmobius.tools.dashboard.logging.ConsoleMessageBuffer.ConsoleEntry;
import org.l2jmobius.tools.dashboard.logging.MessageSeverity;
import org.l2jmobius.tools.dashboard.runtime.ServiceObserver;
import org.l2jmobius.tools.dashboard.runtime.ServiceSupervisor;

/**
 * Renders one service console from the central history buffer and coalesces live messages into bounded EDT batches.
 * @author BazookaRpm
 */
public final class LogStreamPanel extends SurfacePanel implements ServiceObserver, DashboardPageLifecycle
{
	private record SnapshotView(String text, long latestSequence)
	{
	}
	
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
	private static final int FLUSH_INTERVAL_MILLIS = 50;
	private static final int MAXIMUM_PENDING_MESSAGES = 512;
	private static final String[] FILTER_NAMES =
	{
		"All",
		"Information",
		"Warning",
		"Error",
		"System"
	};
	private final ServiceSupervisor _service;
	private final LineLimitedTextArea _console;
	private final JTextField _query = new JTextField(10);
	private final JComboBox<String> _filter = new JComboBox<>(FILTER_NAMES);
	private final ArrayDeque<ConsoleEntry> _pending = new ArrayDeque<>();
	private final Timer _flushTimer;
	private volatile boolean _active;
	private volatile boolean _frozen;
	private volatile boolean _snapshotRequired;
	private long _lastSequence;
	private int _snapshotGeneration;
	private boolean _snapshotInProgress;
	private SwingWorker<SnapshotView, Void> _snapshotWorker;
	
	public LogStreamPanel(String title, int maximumLines, ServiceSupervisor service)
	{
		_service = service;
		_console = new LineLimitedTextArea(maximumLines);
		_flushTimer = new Timer(FLUSH_INTERVAL_MILLIS, event -> flushPending());
		_flushTimer.setRepeats(true);
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));
		add(buildToolbar(title), BorderLayout.PAGE_START);
		final JScrollPane scroll = DashboardUiFactory.scroll(_console);
		scroll.setPreferredSize(new Dimension(0, 132));
		add(scroll, BorderLayout.CENTER);
	}
	
	private JPanel buildToolbar(String title)
	{
		final JPanel bar = DashboardUiFactory.panel(new BorderLayout());
		bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
		bar.add(DashboardUiFactory.label(title.toUpperCase(), Font.BOLD, 10, ControlCenterTheme.MUTED), BorderLayout.LINE_START);
		final JPanel controls = DashboardUiFactory.flow(FlowLayout.RIGHT, 5);
		_query.setPreferredSize(new Dimension(110, 25));
		final JLabel search = new JLabel(ControlCenterIcons.create(GlyphIcon.Kind.SEARCH, 13, ControlCenterTheme.MUTED));
		final DashboardActionButton pause = new DashboardActionButton("Pause", ControlCenterIcons.create(GlyphIcon.Kind.PAUSE, 12, ControlCenterTheme.TEXT), ControlCenterTheme.ACCENT, false);
		final DashboardActionButton clear = new DashboardActionButton("Clear", ControlCenterIcons.create(GlyphIcon.Kind.TRASH, 12, ControlCenterTheme.TEXT), ControlCenterTheme.ACCENT, false);
		pause.addActionListener(event ->
		{
			_frozen = !_frozen;
			pause.setText(_frozen ? "Resume" : "Pause");
			if (!_frozen)
			{
				refreshSnapshot();
			}
		});
		clear.addActionListener(event -> clear());
		_filter.addActionListener(event -> refreshSnapshot());
		_query.addActionListener(event -> refreshSnapshot());
		DashboardUiFactory.addAll(controls, search, _query, _filter, pause, clear);
		bar.add(controls, BorderLayout.LINE_END);
		return bar;
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
		refreshSnapshot();
		_flushTimer.start();
	}
	
	@Override
	public void pageHidden()
	{
		if (!_active)
		{
			return;
		}
		
		_active = false;
		_snapshotGeneration++;
		_snapshotInProgress = false;
		if (_snapshotWorker != null)
		{
			_snapshotWorker.cancel(true);
			_snapshotWorker = null;
		}
		
		_service.removeObserver(this);
		_flushTimer.stop();
		synchronized (_pending)
		{
			_pending.clear();
			_snapshotRequired = false;
		}
	}
	
	@Override
	public void consoleMessage(ConsoleEntry entry)
	{
		final ConsoleMessage message = entry.message();
		if (!_active || (message == null))
		{
			return;
		}
		
		synchronized (_pending)
		{
			if (_pending.size() >= MAXIMUM_PENDING_MESSAGES)
			{
				_pending.clear();
				_snapshotRequired = true;
				return;
			}
			
			_pending.addLast(entry);
		}
	}
	
	public void append(ConsoleMessage message)
	{
		if (message == null)
		{
			return;
		}
		
		_console.appendLine(format(message));
	}
	
	public void clear()
	{
		_snapshotGeneration++;
		_snapshotInProgress = false;
		if (_snapshotWorker != null)
		{
			_snapshotWorker.cancel(true);
			_snapshotWorker = null;
		}
		
		_service.clearConsoleHistory();
		synchronized (_pending)
		{
			_pending.clear();
			_snapshotRequired = false;
		}
		
		_lastSequence = _service.getLatestConsoleSequence();
		_console.setText("");
	}
	
	private void flushPending()
	{
		if (!_active || _frozen || _snapshotInProgress)
		{
			return;
		}
		
		boolean snapshotRequired;
		final ArrayDeque<ConsoleEntry> batch = new ArrayDeque<>();
		synchronized (_pending)
		{
			snapshotRequired = _snapshotRequired;
			_snapshotRequired = false;
			while (!_pending.isEmpty())
			{
				batch.addLast(_pending.removeFirst());
			}
		}
		
		if (snapshotRequired)
		{
			refreshSnapshot();
			return;
		}
		
		if (batch.isEmpty())
		{
			return;
		}
		
		final StringBuilder text = new StringBuilder(batch.size() * 96);
		final int selectedFilter = _filter.getSelectedIndex();
		final String query = _query.getText().trim().toLowerCase(Locale.ROOT);
		for (ConsoleEntry entry : batch)
		{
			if (entry.sequence() <= _lastSequence)
			{
				continue;
			}
			
			_lastSequence = entry.sequence();
			final ConsoleMessage message = entry.message();
			if ((message == null) || !acceptSeverity(message.severity(), selectedFilter))
			{
				continue;
			}
			
			final String formatted = format(message);
			if (query.isEmpty() || formatted.toLowerCase(Locale.ROOT).contains(query))
			{
				text.append(formatted).append(System.lineSeparator());
			}
		}
		
		if (text.length() > 0)
		{
			_console.appendLines(text.toString());
		}
	}
	
	private void refreshSnapshot()
	{
		if (!_active || _frozen)
		{
			return;
		}
		
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(this::refreshSnapshot);
			return;
		}
		
		final int selectedFilter = _filter.getSelectedIndex();
		final String query = _query.getText().trim().toLowerCase(Locale.ROOT);
		final int generation = ++_snapshotGeneration;
		_snapshotInProgress = true;
		if (_snapshotWorker != null)
		{
			_snapshotWorker.cancel(true);
		}
		
		_snapshotWorker = new SwingWorker<>()
		{
			@Override
			protected SnapshotView doInBackground()
			{
				final List<ConsoleEntry> snapshot = _service.getConsoleSnapshot();
				final StringBuilder text = new StringBuilder(snapshot.size() * 96);
				long latest = 0;
				int index = 0;
				for (ConsoleEntry entry : snapshot)
				{
					if (((index++ & 127) == 0) && isCancelled())
					{
						return new SnapshotView("", 0);
					}
					
					latest = Math.max(latest, entry.sequence());
					if (accept(entry.message(), selectedFilter, query))
					{
						text.append(format(entry.message())).append(System.lineSeparator());
					}
				}
				
				return new SnapshotView(text.toString(), latest);
			}
			
			@Override
			protected void done()
			{
				if (_snapshotWorker == this)
				{
					_snapshotWorker = null;
				}
				
				if (generation != _snapshotGeneration)
				{
					return;
				}
				
				_snapshotInProgress = false;
				if (!_active || _frozen)
				{
					return;
				}
				
				try
				{
					final SnapshotView view = get();
					_lastSequence = view.latestSequence();
					_console.setText(view.text());
					_console.setCaretPosition(_console.getDocument().getLength());
					flushPending();
				}
				catch (Exception e)
				{
					_snapshotRequired = true;
				}
			}
		};
		_snapshotWorker.execute();
	}
	
	private boolean accept(ConsoleMessage message, int selectedFilter, String query)
	{
		if ((message == null) || !acceptSeverity(message.severity(), selectedFilter))
		{
			return false;
		}
		
		return query.isEmpty() || format(message).toLowerCase(Locale.ROOT).contains(query);
	}
	
	private String format(ConsoleMessage message)
	{
		return "[" + TIME.format(message.timestamp()) + "] [" + message.severity().name() + "] " + message.text();
	}
	
	private static boolean acceptSeverity(MessageSeverity severity, int selected)
	{
		if (selected == 0)
		{
			return true;
		}
		
		if (selected == 1)
		{
			return severity == MessageSeverity.INFO;
		}
		
		if (selected == 2)
		{
			return severity == MessageSeverity.WARNING;
		}
		
		if (selected == 3)
		{
			return severity == MessageSeverity.ERROR;
		}
		
		return severity == MessageSeverity.TRACE;
	}
}
