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
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import org.l2jmobius.tools.dashboard.database.DatabaseConfigurationService;
import org.l2jmobius.tools.dashboard.runtime.DashboardWorkloadCoordinator;
import org.l2jmobius.tools.dashboard.runtime.DashboardWorkloadCoordinator.WorkloadPermit;
import org.l2jmobius.tools.dashboard.runtime.ServiceSupervisor;
import org.l2jmobius.tools.dashboard.settings.ControlCenterSettings;
import org.l2jmobius.tools.dashboard.tools.GameServerRegistryService;

/**
 * Lists GameServer registry entries and performs guarded offline registration changes. <br>
 * Registry refresh remains read-only and available while services are online; register and unregister operations follow the global maintenance policy.
 * @author BazookaRpm
 */
public final class GameServerRegisterPage extends JPanel implements DashboardPageLifecycle
{
	private final DatabaseConfigurationService _database;
	private final GameServerRegistryService _service;
	private final DashboardWorkloadCoordinator _workloads;
	private final DefaultTableModel _model = new DefaultTableModel(new String[]
	{
		"Server ID",
		"Server Name",
		"Status"
	}, 0)
	{
		@Override
		public boolean isCellEditable(int row, int column)
		{
			return false;
		}
	};
	private final JTable _table = new JTable(_model);
	private final JLabel _status = DashboardUiFactory.label("Ready.", Font.PLAIN, 10, ControlCenterTheme.MUTED);
	private final JButton _register = button("Register Selected", GlyphIcon.Kind.SERVICE, true);
	private final JButton _unregister = button("Unregister Selected", GlyphIcon.Kind.LOG, false);
	private final JButton _unregisterAll = button("Unregister All", GlyphIcon.Kind.LOG, false);
	private final Runnable _availabilityListener = () -> SwingUtilities.invokeLater(this::refreshAvailability);
	
	public GameServerRegisterPage(ControlCenterSettings settings, DatabaseConfigurationService database, ServiceSupervisor login, ServiceSupervisor game, DashboardWorkloadCoordinator workloads)
	{
		_database = database;
		_service = new GameServerRegistryService(settings);
		_workloads = workloads;
		setOpaque(false);
		setLayout(new BorderLayout(0, 8));
		add(DashboardUiFactory.title("GameServer Register", "Review GameServer IDs and write game/config/hexid.txt through guarded offline registry operations."), BorderLayout.NORTH);
		final JPanel body = DashboardUiFactory.panel(new BorderLayout(0, 8));
		body.add(buildActions(), BorderLayout.NORTH);
		_table.setAutoCreateRowSorter(true);
		_table.getColumnModel().getColumn(0).setMaxWidth(90);
		_table.getColumnModel().getColumn(2).setMaxWidth(120);
		final JScrollPane scroll = DashboardUiFactory.scroll(_table);
		body.add(scroll, BorderLayout.CENTER);
		body.add(_status, BorderLayout.SOUTH);
		add(body, BorderLayout.CENTER);
		_status.setText("Press Refresh to load the GameServer registry.");
		refreshAvailability();
	}
	
	@Override
	public void pageShown()
	{
		_workloads.addListener(_availabilityListener);
		refreshAvailability();
	}
	
	@Override
	public void pageHidden()
	{
		_workloads.removeListener(_availabilityListener);
	}
	
	private JPanel buildActions()
	{
		final SurfacePanel section = DashboardUiFactory.section("GameServer registry");
		final JPanel buttons = DashboardUiFactory.panel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		final JButton refresh = button("Refresh", GlyphIcon.Kind.DETAILS, false);
		refresh.addActionListener(event -> refresh());
		_register.addActionListener(event -> registerSelected());
		_unregister.addActionListener(event -> unregisterSelected());
		_unregisterAll.addActionListener(event -> unregisterAll());
		buttons.add(refresh);
		buttons.add(_register);
		buttons.add(_unregister);
		buttons.add(_unregisterAll);
		section.add(buttons, BorderLayout.CENTER);
		return section;
	}
	
	private void refresh()
	{
		_status.setText("Loading server registry...");
		new SwingWorker<List<GameServerRegistryService.ServerRecord>, Void>()
		{
			@Override
			protected List<GameServerRegistryService.ServerRecord> doInBackground() throws Exception
			{
				return _service.list(_database.loadLogin());
			}
			
			@Override
			protected void done()
			{
				try
				{
					final List<GameServerRegistryService.ServerRecord> records = get();
					_model.setRowCount(0);
					int registered = 0;
					for (GameServerRegistryService.ServerRecord record : records)
					{
						if (record.registered())
						{
							registered++;
						}
						
						_model.addRow(new Object[]
						{
							record.id(),
							record.name(),
							record.registered() ? "In Use" : "Free"
						});
					}
					
					_status.setText(records.size() + " server names loaded; " + registered + " registered.");
				}
				catch (Exception e)
				{
					showError("GameServer registry", e);
				}
			}
		}.execute();
	}
	
	private void registerSelected()
	{
		if (!maintenanceAvailable("Register GameServer"))
		{
			return;
		}
		
		final Integer id = selectedId();
		if (id == null)
		{
			return;
		}
		
		if (JOptionPane.showConfirmDialog(this, "Register GameServer ID " + id + "?\nThe current game/config/hexid.txt will be replaced.", "Confirm registration", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION)
		{
			return;
		}
		
		runOperation("Register GameServer", () ->
		{
			return "GameServer registered. HexID written to " + _service.register(_database.loadLogin(), id.intValue()) + ".";
		});
	}
	
	private void unregisterSelected()
	{
		if (!maintenanceAvailable("Unregister GameServer"))
		{
			return;
		}
		
		final Integer id = selectedId();
		if (id == null)
		{
			return;
		}
		
		if (JOptionPane.showConfirmDialog(this, "Unregister GameServer ID " + id + "?", "Confirm removal", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION)
		{
			return;
		}
		
		runOperation("Unregister GameServer", () ->
		{
			_service.unregister(_database.loadLogin(), id.intValue());
			return "GameServer ID " + id + " unregistered.";
		});
	}
	
	private void unregisterAll()
	{
		if (!maintenanceAvailable("Unregister all GameServers"))
		{
			return;
		}
		
		if (JOptionPane.showConfirmDialog(this, "Unregister every GameServer entry?", "Confirm removal", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION)
		{
			return;
		}
		
		runOperation("Unregister all", () -> "Removed " + _service.unregisterAll(_database.loadLogin()) + " registration entries.");
	}
	
	private void runOperation(String title, Operation operation)
	{
		final WorkloadPermit permit = _workloads.tryAcquireHeavy(title);
		if (permit == null)
		{
			showBlocked(title);
			return;
		}
		
		_status.setText(title + "...");
		new SwingWorker<String, Void>()
		{
			@Override
			protected String doInBackground() throws Exception
			{
				return operation.run();
			}
			
			@Override
			protected void done()
			{
				try
				{
					final String result = get();
					_status.setText(result);
					refresh();
					JOptionPane.showMessageDialog(GameServerRegisterPage.this, result, title, JOptionPane.INFORMATION_MESSAGE);
				}
				catch (Exception e)
				{
					showError(title, e);
				}
				finally
				{
					permit.close();
				}
			}
		}.execute();
	}
	
	private boolean maintenanceAvailable(String operation)
	{
		if (_workloads.isMaintenanceAvailable())
		{
			return true;
		}
		
		showBlocked(operation);
		return false;
	}
	
	private void showBlocked(String operation)
	{
		final String reason = _workloads.maintenanceBlockReason();
		_status.setText(operation + " is unavailable: " + reason);
		JOptionPane.showMessageDialog(this, reason, operation + " unavailable", JOptionPane.WARNING_MESSAGE);
	}
	
	private void refreshAvailability()
	{
		final boolean available = _workloads.isMaintenanceAvailable();
		final String reason = _workloads.maintenanceBlockReason();
		_register.setEnabled(available);
		_unregister.setEnabled(available);
		_unregisterAll.setEnabled(available);
		_register.setToolTipText(available ? null : reason);
		_unregister.setToolTipText(available ? null : reason);
		_unregisterAll.setToolTipText(available ? null : reason);
	}
	
	private Integer selectedId()
	{
		final int viewRow = _table.getSelectedRow();
		if (viewRow < 0)
		{
			JOptionPane.showMessageDialog(this, "Select a server first.", "GameServer Register", JOptionPane.INFORMATION_MESSAGE);
			return null;
		}
		
		final int row = _table.convertRowIndexToModel(viewRow);
		return (Integer) _model.getValueAt(row, 0);
	}
	
	private static JButton button(String text, GlyphIcon.Kind icon, boolean primary)
	{
		return new DashboardActionButton(text, ControlCenterIcons.create(icon, 13, ControlCenterTheme.TEXT), ControlCenterTheme.ACTION_PRIMARY, primary);
	}
	
	private void showError(String title, Exception error)
	{
		final Throwable cause = error.getCause() == null ? error : error.getCause();
		_status.setText(title + " failed: " + cause.getMessage());
		JOptionPane.showMessageDialog(this, cause.getMessage(), title, JOptionPane.ERROR_MESSAGE);
	}
	
	@FunctionalInterface
	private interface Operation
	{
		String run() throws Exception;
	}
}
