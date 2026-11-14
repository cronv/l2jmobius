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
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.l2jmobius.tools.dashboard.bridge.DashboardProfile;
import org.l2jmobius.tools.dashboard.runtime.ServiceObserver;
import org.l2jmobius.tools.dashboard.runtime.ServicePhase;
import org.l2jmobius.tools.dashboard.runtime.ServiceSupervisor;

/**
 * Displays the Control Center context header with Interlude identity and dark-gray styling.
 * @author BazookaRpm
 */
public final class TopBarPanel extends JPanel
{
	private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private final JLabel _clockChip = createChip("DATE & TIME | --");
	private final JLabel _systemChip = createChip("SYSTEMS | OFFLINE");
	private ServicePhase _login = ServicePhase.OFFLINE;
	private ServicePhase _game = ServicePhase.OFFLINE;
	
	public TopBarPanel(ServiceSupervisor login, ServiceSupervisor game)
	{
		setBackground(ControlCenterTheme.TOP_BAR);
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ControlCenterTheme.BORDER), BorderFactory.createEmptyBorder(8, 18, 8, 14)));
		add(buildIdentity());
		add(Box.createHorizontalGlue());
		add(buildEnvironment());
		add(Box.createHorizontalStrut(14));
		add(buildRuntimeStatus());
		login.addObserver(observer(true));
		game.addObserver(observer(false));
		final Timer clock = new Timer(1000, event -> refreshClock());
		clock.setRepeats(true);
		clock.start();
		refreshClock();
	}
	
	private JPanel buildIdentity()
	{
		final JPanel group = DashboardUiFactory.panel(new BorderLayout(9, 0));
		group.setAlignmentY(Component.CENTER_ALIGNMENT);
		final JLabel mark = DashboardUiFactory.label("L2J", Font.BOLD, 25, ControlCenterTheme.ACCENT);
		final JPanel words = DashboardUiFactory.panel(new BorderLayout(0, 1));
		words.add(DashboardUiFactory.label("L2J MOBIUS", Font.BOLD, 14, ControlCenterTheme.TEXT), BorderLayout.CENTER);
		words.add(DashboardUiFactory.label(DashboardProfile.ADMINISTRATION_DESCRIPTION, Font.PLAIN, 9, ControlCenterTheme.MUTED), BorderLayout.SOUTH);
		group.add(mark, BorderLayout.LINE_START);
		group.add(words, BorderLayout.CENTER);
		return group;
	}
	
	private JPanel buildEnvironment()
	{
		final JPanel group = DashboardUiFactory.flow(FlowLayout.CENTER, 6);
		DashboardUiFactory.addAll(group, createChip("LANGUAGE | EN"), createChip("ENVIRONMENT | PRODUCTION"), createChip("HOST | " + resolveHost()));
		group.setAlignmentY(Component.CENTER_ALIGNMENT);
		return group;
	}
	
	private JPanel buildRuntimeStatus()
	{
		final JPanel group = DashboardUiFactory.flow(FlowLayout.RIGHT, 6);
		DashboardUiFactory.addAll(group, _clockChip, _systemChip);
		group.setAlignmentY(Component.CENTER_ALIGNMENT);
		return group;
	}
	
	private ServiceObserver observer(boolean login)
	{
		return new ServiceObserver()
		{
			@Override
			public void phaseChanged(ServicePhase phase, String detail)
			{
				SwingUtilities.invokeLater(() -> recordPhase(login, phase));
			}
		};
	}
	
	private void recordPhase(boolean login, ServicePhase phase)
	{
		if (login)
		{
			_login = phase;
		}
		else
		{
			_game = phase;
		}
		
		final int active = (_login == ServicePhase.ONLINE ? 1 : 0) + (_game == ServicePhase.ONLINE ? 1 : 0);
		final String state = active == 2 ? "ONLINE" : active == 0 ? "OFFLINE" : active + "/2 ONLINE";
		_systemChip.setText("SYSTEMS | " + state);
		_systemChip.setForeground(active == 2 ? ControlCenterTheme.GREEN : active == 0 ? ControlCenterTheme.MUTED : ControlCenterTheme.YELLOW);
	}
	
	private void refreshClock()
	{
		_clockChip.setText("DATE & TIME | " + CLOCK_FORMAT.format(LocalDateTime.now()));
	}
	
	private static JLabel createChip(String text)
	{
		final JLabel chip = DashboardUiFactory.label(text, Font.PLAIN, 9, ControlCenterTheme.TEXT);
		chip.setOpaque(true);
		chip.setBackground(ControlCenterTheme.SURFACE);
		chip.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ControlCenterTheme.BORDER), BorderFactory.createEmptyBorder(6, 9, 6, 9)));
		return chip;
	}
	
	private static String resolveHost()
	{
		try
		{
			return InetAddress.getLocalHost().getHostName().toUpperCase();
		}
		catch (Exception e)
		{
			return "LOCALHOST";
		}
	}
}
