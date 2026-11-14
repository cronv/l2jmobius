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
package org.l2jmobius.tools.dashboard;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.util.List;
import java.util.function.BooleanSupplier;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import org.l2jmobius.tools.dashboard.logging.ControlCenterLog;
import org.l2jmobius.tools.dashboard.runtime.DashboardWorkloadCoordinator;
import org.l2jmobius.tools.dashboard.runtime.ServicePhase;
import org.l2jmobius.tools.dashboard.runtime.ServiceSupervisor;
import org.l2jmobius.tools.dashboard.settings.ControlCenterSettings;
import org.l2jmobius.tools.dashboard.settings.SettingsRepository;
import org.l2jmobius.tools.dashboard.ui.ControlCenterTheme;

/**
 * Coordinates service dependency startup, safe shutdown, and persisted window geometry.
 * @author BazookaRpm
 */
final class DashboardLifecycle
{
	private final ControlCenterWindow _window;
	private final ControlCenterSettings _settings;
	private final ServiceSupervisor _login;
	private final ServiceSupervisor _game;
	private final DashboardWorkloadCoordinator _workloads;
	private BooleanSupplier _closeGuard = () -> true;
	private boolean _closing;
	
	DashboardLifecycle(ControlCenterWindow window, ControlCenterSettings settings, ServiceSupervisor login, ServiceSupervisor game, DashboardWorkloadCoordinator workloads)
	{
		_window = window;
		_settings = settings;
		_login = login;
		_game = game;
		_workloads = workloads;
	}
	
	void setCloseGuard(BooleanSupplier closeGuard)
	{
		_closeGuard = closeGuard == null ? () -> true : closeGuard;
	}
	
	void startGame()
	{
		if (!_workloads.serviceStartBlockReason().isEmpty())
		{
			JOptionPane.showMessageDialog(_window, _workloads.serviceStartBlockReason(), "Start Game Server", JOptionPane.WARNING_MESSAGE);
			return;
		}
		
		if (_game.isRunning())
		{
			return;
		}
		
		if (_login.getPhase() == ServicePhase.ONLINE)
		{
			_game.startAsync();
			return;
		}
		
		if (!_settings.isLoginAutoStartEnabled())
		{
			final int answer = JOptionPane.showConfirmDialog(_window, "Game Server requires Login Server. Start Login Server first?", "Start Game Server", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (answer != JOptionPane.YES_OPTION)
			{
				return;
			}
		}
		
		_login.startAsync();
		final Timer dependencyCheck = new Timer(1000, event ->
		{
			final ServicePhase state = _login.getPhase();
			if (state == ServicePhase.ONLINE)
			{
				((Timer) event.getSource()).stop();
				_game.startAsync();
			}
			else if ((state == ServicePhase.FAILED) || ((state == ServicePhase.OFFLINE) && !_login.isRunning()))
			{
				((Timer) event.getSource()).stop();
			}
		});
		dependencyCheck.setInitialDelay(500);
		dependencyCheck.start();
	}
	
	void requestClose()
	{
		if (_closing)
		{
			return;
		}
		
		if (!_closeGuard.getAsBoolean())
		{
			return;
		}
		
		final boolean loginActive = _login.isRunning();
		final boolean gameActive = _game.isRunning();
		if (!loginActive && !gameActive)
		{
			finishClose();
			return;
		}
		
		final String active = loginActive && gameActive ? "Login Server and Game Server are running." : gameActive ? "Game Server is running." : "Login Server is running.";
		final String message = "<html><b>" + active + "</b><br><br>Closing the dashboard will stop all active managed services.<br>Connected players will be disconnected and server access will close.<br><br>Stop services and close the dashboard?</html>";
		final Object[] choices =
		{
			"Stop services and close",
			"Cancel"
		};
		if (JOptionPane.showOptionDialog(_window, message, "Close Dashboard", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, choices, choices[1]) != 0)
		{
			return;
		}
		
		_closing = true;
		stopServices();
	}
	
	private void stopServices()
	{
		_window.setEnabled(false);
		final JLabel status = new JLabel("Preparing safe shutdown...", SwingConstants.CENTER);
		status.setForeground(ControlCenterTheme.TEXT);
		status.setFont(ControlCenterTheme.font(Font.BOLD, 12));
		final JProgressBar progress = new JProgressBar();
		progress.setIndeterminate(true);
		final JPanel body = new JPanel(new BorderLayout(0, 12));
		body.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
		body.add(status, BorderLayout.CENTER);
		body.add(progress, BorderLayout.PAGE_END);
		final JDialog dialog = new JDialog(_window, "Stopping services", true);
		dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		dialog.setResizable(false);
		dialog.setContentPane(body);
		dialog.setSize(460, 130);
		dialog.setLocationRelativeTo(_window);
		shutdownWorker(dialog, status).execute();
		dialog.setVisible(true);
	}
	
	private SwingWorker<String, String> shutdownWorker(JDialog dialog, JLabel status)
	{
		return new SwingWorker<>()
		{
			@Override
			protected String doInBackground()
			{
				if (_game.isRunning())
				{
					publish("Stopping Game Server safely...");
					if (!_game.stopAndWaitForExit())
					{
						return "Game Server did not stop. The dashboard will remain open.";
					}
				}
				
				if (_login.isRunning())
				{
					publish("Stopping Login Server safely...");
					if (!_login.stopAndWaitForExit())
					{
						return "Login Server did not stop. The dashboard will remain open.";
					}
				}
				
				publish("Closing dashboard resources...");
				return null;
			}
			
			@Override
			protected void process(List<String> messages)
			{
				if (!messages.isEmpty())
				{
					status.setText(messages.get(messages.size() - 1));
				}
			}
			
			@Override
			protected void done()
			{
				String error = null;
				try
				{
					error = get();
				}
				catch (Exception e)
				{
					error = "Safe shutdown failed: " + e.getMessage();
				}
				
				dialog.dispose();
				if (error == null)
				{
					finishClose();
				}
				else
				{
					_closing = false;
					_window.setEnabled(true);
					_window.toFront();
					JOptionPane.showMessageDialog(_window, error, "Shutdown cancelled", JOptionPane.ERROR_MESSAGE);
				}
			}
		};
	}
	
	private void finishClose()
	{
		persistGeometry();
		_game.close();
		_login.close();
		_window.dispose();
		System.exit(0);
	}
	
	private void persistGeometry()
	{
		if ((_window.getExtendedState() & Frame.MAXIMIZED_BOTH) == 0)
		{
			_settings.set(ControlCenterSettings.WINDOW_X, _window.getX());
			_settings.set(ControlCenterSettings.WINDOW_Y, _window.getY());
			_settings.set(ControlCenterSettings.WINDOW_WIDTH, _window.getWidth());
			_settings.set(ControlCenterSettings.WINDOW_HEIGHT, _window.getHeight());
			_settings.set(ControlCenterSettings.WINDOW_MAXIMIZED, false);
		}
		else
		{
			_settings.set(ControlCenterSettings.WINDOW_MAXIMIZED, true);
		}
		
		try
		{
			SettingsRepository.save(_settings);
		}
		catch (Exception e)
		{
			ControlCenterLog.write("Unable to save dashboard window geometry: " + e.getMessage());
		}
	}
}
