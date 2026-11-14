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
import java.awt.Font;
import java.nio.file.FileStore;
import java.nio.file.Files;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import org.l2jmobius.tools.dashboard.runtime.ServiceSupervisor;
import org.l2jmobius.tools.dashboard.settings.ControlCenterSettings;

/**
 * Presents read-only Login Server and Game Server observability while keeping telemetry and disk polling inactive when hidden.
 * @author BazookaRpm
 */
public final class TelemetryOverviewPanel extends JPanel implements DashboardPageLifecycle
{
	private final ControlCenterSettings _settings;
	private final ServiceTelemetryPanel _loginTelemetry;
	private final ServiceTelemetryPanel _gameTelemetry;
	private final JLabel _storage = DashboardUiFactory.label("Disk: N/A", Font.PLAIN, 10, ControlCenterTheme.MUTED);
	private final Timer _diskPoll = new Timer(5000, event -> updateStorageAsync());
	private boolean _storageLoading;
	
	public TelemetryOverviewPanel(ControlCenterSettings settings, ServiceSupervisor login, ServiceSupervisor game)
	{
		_settings = settings;
		_loginTelemetry = new ServiceTelemetryPanel(settings, login);
		_gameTelemetry = new ServiceTelemetryPanel(settings, game);
		_diskPoll.setRepeats(true);
		setOpaque(false);
		setLayout(new BorderLayout());
		add(buildBanner(), BorderLayout.PAGE_START);
		final JSplitPane services = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _loginTelemetry, _gameTelemetry);
		services.setResizeWeight(0.5);
		services.setContinuousLayout(true);
		services.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
		services.setOpaque(false);
		final JPanel canvas = DashboardUiFactory.panel(new BorderLayout());
		canvas.add(services, BorderLayout.CENTER);
		final JScrollPane scroll = DashboardUiFactory.scroll(canvas);
		scroll.setBorder(null);
		scroll.getViewport().setOpaque(false);
		scroll.setOpaque(false);
		add(scroll, BorderLayout.CENTER);
	}
	
	@Override
	public void pageShown()
	{
		_loginTelemetry.pageShown();
		_gameTelemetry.pageShown();
		updateStorageAsync();
		_diskPoll.start();
	}
	
	@Override
	public void pageHidden()
	{
		_diskPoll.stop();
		_loginTelemetry.pageHidden();
		_gameTelemetry.pageHidden();
	}
	
	private SurfacePanel buildBanner()
	{
		final SurfacePanel banner = new SurfacePanel(10, ControlCenterTheme.SURFACE, ControlCenterTheme.BORDER);
		banner.setPreferredSize(new Dimension(0, 52));
		banner.setLayout(new BorderLayout());
		banner.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
		final JPanel text = DashboardUiFactory.panel(new BorderLayout(0, 1));
		text.add(DashboardUiFactory.label("Safe observability", Font.BOLD, 15, ControlCenterTheme.TEXT), BorderLayout.CENTER);
		text.add(DashboardUiFactory.label("READ ONLY · NO AUTOMATIC ACTIONS", Font.PLAIN, 9, ControlCenterTheme.MUTED), BorderLayout.SOUTH);
		banner.add(text, BorderLayout.LINE_START);
		banner.add(_storage, BorderLayout.LINE_END);
		return banner;
	}
	
	private void updateStorageAsync()
	{
		if (_storageLoading)
		{
			return;
		}
		
		_storageLoading = true;
		new SwingWorker<long[], Void>()
		{
			@Override
			protected long[] doInBackground() throws Exception
			{
				final FileStore disk = Files.getFileStore(_settings.getDistributionRoot());
				return new long[]
				{
					disk.getUsableSpace(),
					disk.getTotalSpace()
				};
			}
			
			@Override
			protected void done()
			{
				_storageLoading = false;
				try
				{
					final long[] values = get();
					applyStorage(values[0], values[1]);
				}
				catch (Exception e)
				{
					_storage.setText("Disk: N/A");
					_storage.setForeground(ControlCenterTheme.MUTED);
					_storage.setToolTipText(e.getMessage());
				}
			}
		}.execute();
	}
	
	private void applyStorage(long available, long capacity)
	{
		final double percent = capacity <= 0 ? -1 : (available * 100d) / capacity;
		_storage.setText("Free disk: " + readableSize(available) + " of " + readableSize(capacity));
		final boolean warning = (percent >= 0) && (percent < _settings.getDiskFreeWarningPercent());
		_storage.setForeground(warning ? ControlCenterTheme.YELLOW : ControlCenterTheme.GREEN);
		_storage.setToolTipText(warning ? "Free disk space is below the informational threshold." : "Disk state observed locally by the Control Center.");
	}
	
	private static String readableSize(long value)
	{
		if (value < 0)
		{
			return "N/A";
		}
		
		final double divisor = value >= 1073741824L ? 1073741824d : 1048576d;
		final String unit = value >= 1073741824L ? "GB" : "MB";
		return String.format("%.1f %s", value / divisor, unit);
	}
}
