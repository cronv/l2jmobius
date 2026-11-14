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
import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.l2jmobius.tools.dashboard.database.DatabaseConfigurationService;
import org.l2jmobius.tools.dashboard.database.SqlBackupCoordinator;
import org.l2jmobius.tools.dashboard.runtime.DashboardWorkloadCoordinator;
import org.l2jmobius.tools.dashboard.runtime.ServiceSupervisor;
import org.l2jmobius.tools.dashboard.settings.ControlCenterSettings;

/**
 * Creates Dashboard pages on first use and coordinates page visibility lifecycle callbacks during navigation. <br>
 * Hidden pages remain cached but release continuous observers, timers, and other avoidable UI work.
 * @author BazookaRpm
 */
public final class DashboardWorkspace extends JPanel
{
	private final CardLayout _pageLayout = new CardLayout();
	private final JPanel _pages = DashboardUiFactory.panel(_pageLayout);
	private final Map<String, Supplier<Component>> _pageFactories = new LinkedHashMap<>();
	private final Map<String, Component> _pageInstances = new LinkedHashMap<>();
	private final DatabaseConfigurationService _database;
	private final SqlBackupCoordinator _backups;
	private final ServiceSupervisor _login;
	private final ServiceSupervisor _game;
	private final DashboardWorkloadCoordinator _workloads;
	private final NavigationRail _navigation;
	private BackupArchivePanel _backupHistory;
	private Component _currentPage;
	private boolean _responsiveCollapse;
	
	public DashboardWorkspace(ControlCenterSettings settings, ServiceSupervisor login, ServiceSupervisor game, Runnable startGame, DashboardWorkloadCoordinator workloads)
	{
		_login = login;
		_game = game;
		_workloads = workloads;
		_database = new DatabaseConfigurationService(settings);
		_backups = new SqlBackupCoordinator(settings.getBackupDirectory(), settings.getDatabaseAuditLog());
		setBackground(ControlCenterTheme.BACKGROUND);
		setLayout(new BorderLayout());
		_pages.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));
		register("dashboard", () -> new HomePage(settings, login, game, login::startAsync, startGame, () -> open("login"), () -> open("game")));
		register("login", () -> new ServiceSummaryPanel(settings, login, login::startAsync));
		register("game", () -> new ServiceSummaryPanel(settings, game, startGame));
		register("observability", () -> new TelemetryOverviewPanel(settings, login, game));
		register("diagnostics", () -> new RuntimeDiagnosticsPanel(settings, login, game));
		register("logs", () -> new UnifiedLogPanel(settings, login, game));
		register("settings", () -> new RuntimeSettingsPanel(settings));
		register("tools", () -> new DatabaseMaintenancePanel(settings, _database, _backups, () -> getBackupHistory().refresh(), login, game, workloads));
		register("mapExtractor", () -> new MapExtractorPage(settings, workloads));
		register("npcEditor", () -> new NpcEditorPage(settings, workloads, this::openDropEditor));
		register("itemEditor", () -> new ItemEditorPage(settings, workloads));
		register("dropEditor", () -> new DropEditorPage(settings, workloads));
		register("search", () -> new SearchPage(settings, workloads));
		register("accountManager", () -> new AccountManagerPage(_database));
		register("gameServerRegister", () -> new GameServerRegisterPage(settings, _database, login, game, workloads));
		register("backups", this::getBackupHistory);
		register("preferences", () -> new GeneralPreferencesPanel(settings));
		_navigation = new NavigationRail(settings.getNavigationWidth(), this::showPage, login, game);
		final JPanel main = DashboardUiFactory.panel(new BorderLayout());
		main.add(new TopBarPanel(login, game), BorderLayout.PAGE_START);
		main.add(_pages, BorderLayout.CENTER);
		add(_navigation, BorderLayout.LINE_START);
		add(main, BorderLayout.CENTER);
		addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent event)
			{
				updateNavigationMode();
			}
		});
	}
	
	public void open(String page)
	{
		_navigation.selectPage(page);
		showPage(page);
	}
	
	private void openDropEditor(int npcId)
	{
		final Component page = getOrCreatePage("dropEditor");
		if (page instanceof DropEditorPage)
		{
			((DropEditorPage) page).selectNpc(npcId);
		}
		
		open("dropEditor");
	}
	
	public boolean prepareForClose()
	{
		for (Component page : _pageInstances.values())
		{
			if ((page instanceof DashboardPageLifecycle) && !((DashboardPageLifecycle) page).prepareForClose())
			{
				return false;
			}
		}
		
		return true;
	}
	
	private void register(String key, Supplier<Component> factory)
	{
		_pageFactories.put(key, factory);
	}
	
	private Component getOrCreatePage(String key)
	{
		Component page = _pageInstances.get(key);
		if (page != null)
		{
			return page;
		}
		
		final Supplier<Component> factory = _pageFactories.get(key);
		if (factory == null)
		{
			return null;
		}
		
		page = factory.get();
		_pageInstances.put(key, page);
		_pages.add(page, key);
		return page;
	}
	
	private BackupArchivePanel getBackupHistory()
	{
		if (_backupHistory == null)
		{
			_backupHistory = new BackupArchivePanel(_database, _backups, _login, _game, _workloads);
		}
		
		return _backupHistory;
	}
	
	private void showPage(String pageKey)
	{
		final Component page = getOrCreatePage(pageKey);
		if (page == null)
		{
			return;
		}
		
		if (_currentPage == page)
		{
			return;
		}
		
		if (_currentPage instanceof DashboardPageLifecycle)
		{
			((DashboardPageLifecycle) _currentPage).pageHidden();
		}
		
		_pageLayout.show(_pages, pageKey);
		_currentPage = page;
		if (page instanceof DashboardPageLifecycle)
		{
			((DashboardPageLifecycle) page).pageShown();
		}
	}
	
	private void updateNavigationMode()
	{
		final boolean compact = getWidth() < 1180;
		if (compact && !_navigation.isCollapsed())
		{
			_navigation.setCollapsed(true);
			_responsiveCollapse = true;
		}
		else if (!compact && _responsiveCollapse)
		{
			_navigation.setCollapsed(false);
			_responsiveCollapse = false;
		}
	}
}
