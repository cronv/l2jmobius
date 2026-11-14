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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;

import org.l2jmobius.tools.dashboard.runtime.ServiceSupervisor;
import org.l2jmobius.tools.dashboard.settings.ControlCenterSettings;

/**
 * Displays the Login Server and Game Server summaries side by side on the Dashboard home page.
 * @author BazookaRpm
 */
public final class HomePage extends JPanel implements DashboardPageLifecycle
{
	private final ServiceSummaryPanel _loginSummary;
	private final ServiceSummaryPanel _gameSummary;
	
	public HomePage(ControlCenterSettings settings, ServiceSupervisor login, ServiceSupervisor game, Runnable startLogin, Runnable startGame, Runnable openLogin, Runnable openGame)
	{
		setOpaque(false);
		setLayout(new GridBagLayout());
		_loginSummary = new ServiceSummaryPanel(settings, login, startLogin, true, openLogin);
		_gameSummary = new ServiceSummaryPanel(settings, game, startGame, true, openGame);
		place(_loginSummary, 0);
		place(_gameSummary, 1);
	}
	
	@Override
	public void pageShown()
	{
		_loginSummary.pageShown();
		_gameSummary.pageShown();
	}
	
	@Override
	public void pageHidden()
	{
		_loginSummary.pageHidden();
		_gameSummary.pageHidden();
	}
	
	private void place(ServiceSummaryPanel summary, int column)
	{
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = column;
		constraints.gridy = 0;
		constraints.weightx = 1;
		constraints.weighty = 1;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.insets = column == 0 ? new Insets(0, 0, 0, 4) : new Insets(0, 4, 0, 0);
		add(summary, constraints);
	}
}
