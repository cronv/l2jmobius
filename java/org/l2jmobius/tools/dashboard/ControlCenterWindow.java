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

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.l2jmobius.tools.dashboard.bridge.DashboardProfile;
import org.l2jmobius.tools.dashboard.runtime.DashboardWorkloadCoordinator;
import org.l2jmobius.tools.dashboard.runtime.ServiceRole;
import org.l2jmobius.tools.dashboard.runtime.ServiceSupervisor;
import org.l2jmobius.tools.dashboard.settings.ControlCenterSettings;
import org.l2jmobius.tools.dashboard.ui.ApplicationIconSet;
import org.l2jmobius.tools.dashboard.ui.DashboardWorkspace;

/**
 * Provides the top-level Control Center window and wires managed services to the Dashboard workspace.
 * @author BazookaRpm
 */
public final class ControlCenterWindow extends JFrame
{
	private final ControlCenterSettings _settings;
	private final ServiceSupervisor _login;
	private final ServiceSupervisor _game;
	private final DashboardWorkloadCoordinator _workloads;
	private final DashboardLifecycle _lifecycle;
	
	public ControlCenterWindow(ControlCenterSettings settings)
	{
		super(DashboardProfile.WINDOW_TITLE);
		_settings = settings;
		_login = new ServiceSupervisor(settings, ServiceRole.LOGIN);
		_game = new ServiceSupervisor(settings, ServiceRole.GAME);
		_workloads = new DashboardWorkloadCoordinator();
		_login.setWorkloadCoordinator(_workloads);
		_game.setWorkloadCoordinator(_workloads);
		_workloads.bindServices(_login, _game);
		_lifecycle = new DashboardLifecycle(this, settings, _login, _game, _workloads);
		applyFrameSettings();
		final DashboardWorkspace workspace = new DashboardWorkspace(settings, _login, _game, _lifecycle::startGame, _workloads);
		_lifecycle.setCloseGuard(workspace::prepareForClose);
		setContentPane(workspace);
		workspace.open("dashboard");
	}
	
	private void applyFrameSettings()
	{
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		setMinimumSize(new Dimension(960, 640));
		setIconImages(ApplicationIconSet.create());
		final Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		final int width = Math.min(_settings.getWindowWidth(), screen.width);
		final int height = Math.min(_settings.getWindowHeight(), screen.height);
		setSize(width, height);
		if ((_settings.getWindowX() < 0) || (_settings.getWindowY() < 0))
		{
			setLocationRelativeTo(null);
		}
		else
		{
			setLocation(_settings.getWindowX(), _settings.getWindowY());
		}
		
		if (_settings.isWindowMaximized())
		{
			setExtendedState(getExtendedState() | Frame.MAXIMIZED_BOTH);
		}
		
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent event)
			{
				_lifecycle.requestClose();
			}
		});
	}
}
