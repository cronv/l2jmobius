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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import org.l2jmobius.tools.dashboard.database.DatabaseConfigurationService;
import org.l2jmobius.tools.dashboard.database.DatabaseEndpoint;
import org.l2jmobius.tools.dashboard.tools.AccountManagerService;

/**
 * Provides account creation and administration through the configured Login Server database.
 * @author BazookaRpm
 */
public final class AccountManagerPage extends JPanel
{
	private static final String[] ACCESS_LEVELS =
	{
		"-1 - Banned",
		"0 - User",
		"10 - Chat Moderator",
		"20 - Test GM",
		"30 - General GM",
		"40 - Support GM",
		"50 - Event GM",
		"60 - Head GM",
		"70 - Admin",
		"100 - Master"
	};
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
	private final DatabaseConfigurationService _database;
	private final AccountManagerService _service = new AccountManagerService();
	private final JLabel _connection = DashboardUiFactory.label("Not tested", Font.BOLD, 10, ControlCenterTheme.MUTED);
	private final DefaultTableModel _model = new DefaultTableModel(new String[]
	{
		"Login",
		"Access",
		"Email",
		"Last IP",
		"Created",
		"Last Active"
	}, 0)
	{
		@Override
		public boolean isCellEditable(int row, int column)
		{
			return false;
		}
	};
	private final JTable _table = new JTable(_model);
	private final JTextField _filter = new JTextField();
	private final JLabel _count = DashboardUiFactory.label("0 accounts", Font.PLAIN, 10, ControlCenterTheme.MUTED);
	
	public AccountManagerPage(DatabaseConfigurationService database)
	{
		_database = database;
		setOpaque(false);
		setLayout(new BorderLayout(0, 8));
		add(DashboardUiFactory.title("Account Manager", "Create, search, update and remove Login Server accounts from the Control Center."), BorderLayout.NORTH);
		final JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Accounts", buildAccountsTab());
		tabs.addTab("Create Account", buildCreateTab());
		add(tabs, BorderLayout.CENTER);
		_count.setText("Press Refresh or Search to load accounts.");
	}
	
	private JPanel buildAccountsTab()
	{
		final JPanel panel = DashboardUiFactory.panel(new BorderLayout(0, 8));
		final SurfacePanel actions = DashboardUiFactory.section("Account administration");
		final JPanel bar = DashboardUiFactory.panel(new BorderLayout(6, 0));
		_filter.setToolTipText("Partial account name.");
		bar.add(_filter, BorderLayout.CENTER);
		final JPanel buttons = DashboardUiFactory.panel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		final JButton search = button("Search", GlyphIcon.Kind.SEARCH, false);
		final JButton refresh = button("Refresh", GlyphIcon.Kind.DETAILS, false);
		final JButton update = button("Update Selected", GlyphIcon.Kind.SETTINGS, false);
		final JButton delete = button("Delete Selected", GlyphIcon.Kind.LOG, false);
		final JButton test = button("Test DB", GlyphIcon.Kind.DATABASE, false);
		search.addActionListener(event -> refresh(_filter.getText().trim()));
		refresh.addActionListener(event -> refresh(""));
		update.addActionListener(event -> updateSelected());
		delete.addActionListener(event -> deleteSelected());
		test.addActionListener(event -> testConnection());
		buttons.add(search);
		buttons.add(refresh);
		buttons.add(update);
		buttons.add(delete);
		buttons.add(test);
		buttons.add(_connection);
		bar.add(buttons, BorderLayout.EAST);
		actions.add(bar, BorderLayout.CENTER);
		panel.add(actions, BorderLayout.NORTH);
		_table.setAutoCreateRowSorter(true);
		final JScrollPane scroll = DashboardUiFactory.scroll(_table);
		panel.add(scroll, BorderLayout.CENTER);
		final JPanel footer = DashboardUiFactory.panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		footer.add(_count);
		panel.add(footer, BorderLayout.SOUTH);
		return panel;
	}
	
	private JPanel buildCreateTab()
	{
		final JPanel panel = DashboardUiFactory.panel(new BorderLayout());
		final SurfacePanel section = DashboardUiFactory.section("Create account");
		final JPanel form = DashboardUiFactory.panel(new GridBagLayout());
		final JTextField username = new JTextField();
		final JPasswordField password = new JPasswordField();
		final JComboBox<String> access = new JComboBox<>(ACCESS_LEVELS);
		access.setSelectedItem("0 - User");
		addFormRow(form, 0, "Username", username);
		addFormRow(form, 1, "Password", password);
		addFormRow(form, 2, "Access Level", access);
		final JButton create = button("Create Account", GlyphIcon.Kind.DETAILS, true);
		form.add(create, constraints(1, 3, 1));
		create.addActionListener(event ->
		{
			final char[] secret = password.getPassword();
			runOperation("Create account", () ->
			{
				_service.create(_database.loadLogin(), username.getText().trim(), secret, accessValue((String) access.getSelectedItem()));
				return "Account created successfully.";
			}, () ->
			{
				username.setText("");
				password.setText("");
				refresh("");
			});
		});
		section.add(form, BorderLayout.CENTER);
		panel.add(section, BorderLayout.NORTH);
		return panel;
	}
	
	private void refresh(String filter)
	{
		new SwingWorker<List<AccountManagerService.AccountRecord>, Void>()
		{
			private int _total;
			
			@Override
			protected List<AccountManagerService.AccountRecord> doInBackground() throws Exception
			{
				final DatabaseEndpoint endpoint = _database.loadLogin();
				_total = _service.count(endpoint);
				return filter.isBlank() ? _service.list(endpoint, 1000, 0) : _service.search(endpoint, filter, 1000);
			}
			
			@Override
			protected void done()
			{
				try
				{
					final List<AccountManagerService.AccountRecord> records = get();
					_model.setRowCount(0);
					for (AccountManagerService.AccountRecord record : records)
					{
						_model.addRow(new Object[]
						{
							record.login(),
							record.accessLevel(),
							text(record.email()),
							text(record.lastIp()),
							timestamp(record.created()),
							lastActive(record.lastActive())
						});
					}
					
					_count.setText(records.size() + " shown / " + _total + " total accounts");
				}
				catch (Exception e)
				{
					showError("Account list", e);
				}
			}
		}.execute();
	}
	
	private void testConnection()
	{
		_connection.setText("Testing...");
		_connection.setForeground(ControlCenterTheme.YELLOW);
		runOperation("Database connection", () ->
		{
			_service.test(_database.loadLogin());
			return "Login database connection is valid.";
		}, () ->
		{
			_connection.setText("Connected");
			_connection.setForeground(ControlCenterTheme.GREEN);
		});
	}
	
	private void updateSelected()
	{
		final int viewRow = _table.getSelectedRow();
		if (viewRow < 0)
		{
			JOptionPane.showMessageDialog(this, "Select an account first.", "Account Manager", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		final int row = _table.convertRowIndexToModel(viewRow);
		final String username = String.valueOf(_model.getValueAt(row, 0));
		final JPasswordField password = new JPasswordField();
		final JCheckBox updatePassword = new JCheckBox("Change password", false);
		final JComboBox<String> access = new JComboBox<>(ACCESS_LEVELS);
		selectAccess(access, ((Number) _model.getValueAt(row, 1)).intValue());
		final Object[] message =
		{
			"Account: " + username,
			"Access level:",
			access,
			updatePassword,
			"New password:",
			password
		};
		if (JOptionPane.showConfirmDialog(this, message, "Update Account", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
		{
			return;
		}
		
		final char[] secret = password.getPassword();
		runOperation("Update account", () ->
		{
			_service.update(_database.loadLogin(), username, secret, accessValue((String) access.getSelectedItem()), updatePassword.isSelected());
			return "Account updated successfully.";
		}, () -> refresh(_filter.getText().trim()));
	}
	
	private void deleteSelected()
	{
		final int viewRow = _table.getSelectedRow();
		if (viewRow < 0)
		{
			JOptionPane.showMessageDialog(this, "Select an account first.", "Account Manager", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		final int row = _table.convertRowIndexToModel(viewRow);
		final String username = String.valueOf(_model.getValueAt(row, 0));
		if (JOptionPane.showConfirmDialog(this, "Delete account '" + username + "'?", "Confirm account deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION)
		{
			return;
		}
		
		runOperation("Delete account", () ->
		{
			_service.delete(_database.loadLogin(), username);
			return "Account deleted successfully.";
		}, () -> refresh(_filter.getText().trim()));
	}
	
	private void runOperation(String title, Operation operation, Runnable successAction)
	{
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
					final String message = get();
					successAction.run();
					JOptionPane.showMessageDialog(AccountManagerPage.this, message, title, JOptionPane.INFORMATION_MESSAGE);
				}
				catch (Exception e)
				{
					showError(title, e);
				}
			}
		}.execute();
	}
	
	private JButton button(String text, GlyphIcon.Kind icon, boolean primary)
	{
		return new DashboardActionButton(text, ControlCenterIcons.create(icon, 13, ControlCenterTheme.TEXT), ControlCenterTheme.ACTION_PRIMARY, primary);
	}
	
	private void addFormRow(JPanel panel, int row, String label, Component component)
	{
		panel.add(DashboardUiFactory.label(label, Font.BOLD, 11, ControlCenterTheme.MUTED), constraints(0, row, 0));
		panel.add(component, constraints(1, row, 1));
	}
	
	private static GridBagConstraints constraints(int column, int row, int weight)
	{
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = column;
		constraints.gridy = row;
		constraints.weightx = weight;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.insets = new Insets(5, 5, 5, 5);
		return constraints;
	}
	
	private static int accessValue(String value)
	{
		return Integer.parseInt(value.substring(0, value.indexOf(" - ")));
	}
	
	private static void selectAccess(JComboBox<String> combo, int accessLevel)
	{
		for (int index = 0; index < combo.getItemCount(); index++)
		{
			if (accessValue(combo.getItemAt(index)) == accessLevel)
			{
				combo.setSelectedIndex(index);
				return;
			}
		}
		
		combo.setSelectedItem("0 - User");
	}
	
	private static String text(String value)
	{
		return (value == null) || value.isBlank() ? "N/A" : value;
	}
	
	private static String timestamp(Timestamp value)
	{
		return value == null ? "Unknown" : DATE_TIME.format(value.toInstant());
	}
	
	private static String lastActive(long value)
	{
		return value <= 0 ? "Never" : DATE_TIME.format(Instant.ofEpochMilli(value));
	}
	
	private void showError(String title, Exception error)
	{
		final Throwable cause = error.getCause() == null ? error : error.getCause();
		JOptionPane.showMessageDialog(this, cause.getMessage(), title, JOptionPane.ERROR_MESSAGE);
	}
	
	@FunctionalInterface
	private interface Operation
	{
		String run() throws Exception;
	}
}
