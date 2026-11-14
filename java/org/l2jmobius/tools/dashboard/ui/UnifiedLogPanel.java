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

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.l2jmobius.tools.dashboard.runtime.ServiceSupervisor;
import org.l2jmobius.tools.dashboard.settings.ControlCenterSettings;

/**
 * Displays Login Server and Game Server console streams side by side from their shared service histories.
 * @author BazookaRpm
 */
public final class UnifiedLogPanel extends JPanel implements DashboardPageLifecycle
{
	private final LogStreamPanel _loginLog;
	private final LogStreamPanel _gameLog;
	
	public UnifiedLogPanel(ControlCenterSettings settings, ServiceSupervisor login, ServiceSupervisor game)
	{
		setOpaque(false);
		setLayout(new BorderLayout());
		_loginLog = new LogStreamPanel("Login Server", settings.getConsoleLineLimit(), login);
		_gameLog = new LogStreamPanel("Game Server", settings.getConsoleLineLimit(), game);
		final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _loginLog, _gameLog);
		split.setBorder(null);
		split.setOpaque(false);
		split.setResizeWeight(0.5);
		split.setContinuousLayout(true);
		add(split, BorderLayout.CENTER);
	}
	
	@Override
	public void pageShown()
	{
		_loginLog.pageShown();
		_gameLog.pageShown();
	}
	
	@Override
	public void pageHidden()
	{
		_loginLog.pageHidden();
		_gameLog.pageHidden();
	}
}
