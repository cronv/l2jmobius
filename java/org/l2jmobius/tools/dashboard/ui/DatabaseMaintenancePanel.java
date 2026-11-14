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
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.l2jmobius.tools.dashboard.bridge.DashboardProfile;
import org.l2jmobius.tools.dashboard.database.DatabaseConfigurationService;
import org.l2jmobius.tools.dashboard.database.DatabaseEndpoint;
import org.l2jmobius.tools.dashboard.database.DatabaseInstallationEngine;
import org.l2jmobius.tools.dashboard.database.MySqlJdbcAddress;
import org.l2jmobius.tools.dashboard.database.SqlBackupCoordinator;
import org.l2jmobius.tools.dashboard.runtime.DashboardWorkloadCoordinator;
import org.l2jmobius.tools.dashboard.runtime.DashboardWorkloadCoordinator.WorkloadPermit;
import org.l2jmobius.tools.dashboard.runtime.ServiceRole;
import org.l2jmobius.tools.dashboard.runtime.ServiceSupervisor;
import org.l2jmobius.tools.dashboard.settings.ControlCenterSettings;

/**
 * Provides guarded database installation, connection configuration, backup, restore, and local distribution tools. <br>
 * Database mutations and other heavy database work are available only while both managed services are offline and no other heavy task is active.
 * @author BazookaRpm
 */
public final class DatabaseMaintenancePanel extends JPanel implements DashboardPageLifecycle
{
	private record DatabaseInput(String host, int port, String database, String user, String password)
	{
	}
	
	private record DatabaseView(DatabaseEndpoint endpoint, Path dumpTool)
	{
	}
	
	private final ControlCenterSettings _settings;
	private final DatabaseConfigurationService _database;
	private final SqlBackupCoordinator _backups;
	private final DatabaseInstallationEngine _installer;
	private final Runnable _backupChanged;
	private final ServiceSupervisor _login;
	private final ServiceSupervisor _game;
	private final DashboardWorkloadCoordinator _workloads;
	private final JTextField _host = new JTextField();
	private final JTextField _port = new JTextField();
	private final JTextField _databaseName = new JTextField();
	private final JTextField _user = new JTextField();
	private final JPasswordField _password = new JPasswordField();
	private final JTextField _mysqlPath = new JTextField();
	private final JTextField _backupPath = new JTextField();
	private final JLabel _status = new JLabel("Ready.");
	private final DashboardActionButton _install = new DashboardActionButton("Install Database", ControlCenterIcons.create(GlyphIcon.Kind.DATABASE, ControlCenterTheme.TEXT), ControlCenterTheme.ACTION_PRIMARY, true);
	private final DashboardActionButton _test = new DashboardActionButton("Test connection", ControlCenterIcons.create(GlyphIcon.Kind.DIAGNOSTICS, ControlCenterTheme.TEXT), ControlCenterTheme.ACCENT, false);
	private final DashboardActionButton _backup = new DashboardActionButton("Create backup", ControlCenterIcons.create(GlyphIcon.Kind.SAVE, ControlCenterTheme.TEXT), ControlCenterTheme.ACTION_PRIMARY, true);
	private final DashboardActionButton _restore = new DashboardActionButton("Restore SQL", ControlCenterIcons.create(GlyphIcon.Kind.DATABASE, ControlCenterTheme.TEXT), ControlCenterTheme.RED, false);
	private final DashboardActionButton _apply = new DashboardActionButton("Apply to Login/Game", ControlCenterIcons.create(GlyphIcon.Kind.SETTINGS, ControlCenterTheme.TEXT), ControlCenterTheme.ACCENT, false);
	private final Runnable _availabilityListener = () -> SwingUtilities.invokeLater(this::refreshAvailability);
	
	public DatabaseMaintenancePanel(ControlCenterSettings settings, DatabaseConfigurationService database, SqlBackupCoordinator backups, Runnable backupChanged, ServiceSupervisor login, ServiceSupervisor game, DashboardWorkloadCoordinator workloads)
	{
		_settings = settings;
		_database = database;
		_backups = backups;
		_installer = new DatabaseInstallationEngine(settings.getDistributionRoot().resolve("db_installer"));
		_backupChanged = backupChanged;
		_login = login;
		_game = game;
		_workloads = workloads;
		setOpaque(false);
		setLayout(new BorderLayout(0, 10));
		add(createTitle(), BorderLayout.NORTH);
		final JPanel content = new JPanel(new BorderLayout(0, 10));
		content.setOpaque(false);
		content.add(createLocalTools(), BorderLayout.NORTH);
		final JPanel databaseArea = new JPanel(new BorderLayout(0, 10));
		databaseArea.setOpaque(false);
		databaseArea.add(createInstallerSection(), BorderLayout.NORTH);
		databaseArea.add(createDatabaseForm(), BorderLayout.CENTER);
		content.add(databaseArea, BorderLayout.CENTER);
		content.add(createActions(), BorderLayout.SOUTH);
		add(content, BorderLayout.CENTER);
		_status.setForeground(ControlCenterTheme.MUTED);
		_status.setFont(ControlCenterTheme.font(Font.PLAIN, 10));
		add(_status, BorderLayout.SOUTH);
		loadConfiguration();
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
	
	private JPanel createTitle()
	{
		final JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		final JLabel title = new JLabel("Database Tools");
		title.setFont(ControlCenterTheme.font(Font.BOLD, 17));
		final JLabel subtitle = new JLabel("Database installation, connection configuration, backup, restore, and local distribution shortcuts");
		subtitle.setFont(ControlCenterTheme.font(Font.PLAIN, 10));
		subtitle.setForeground(ControlCenterTheme.MUTED);
		panel.add(title, BorderLayout.NORTH);
		panel.add(subtitle, BorderLayout.SOUTH);
		return panel;
	}
	
	private SurfacePanel createLocalTools()
	{
		final SurfacePanel panel = section("Local folders");
		final JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		actions.setOpaque(false);
		actions.add(folderButton("Distribution", _settings.getDistributionRoot()));
		actions.add(folderButton("Login Server", _settings.getServiceDirectory(ServiceRole.LOGIN)));
		actions.add(folderButton("Game Server", _settings.getServiceDirectory(ServiceRole.GAME)));
		actions.add(folderButton("Backups", _backups.backupDirectory()));
		actions.add(folderButton("DB Installer", _installer.installerDirectory()));
		panel.add(actions, BorderLayout.CENTER);
		return panel;
	}
	
	private SurfacePanel createInstallerSection()
	{
		final SurfacePanel panel = section("Database installation");
		final JPanel content = new JPanel(new BorderLayout(12, 0));
		content.setOpaque(false);
		final JLabel description = new JLabel("<html>Open the integrated " + DashboardProfile.CHRONICLE_NAME + " database installer.<br>It uses the official Login/Game SQL files and is available only while both managed services are offline.</html>");
		description.setFont(ControlCenterTheme.font(Font.PLAIN, 10));
		description.setForeground(ControlCenterTheme.MUTED);
		content.add(description, BorderLayout.CENTER);
		_install.addActionListener(event -> openDatabaseInstaller());
		content.add(_install, BorderLayout.EAST);
		panel.add(content, BorderLayout.CENTER);
		return panel;
	}
	
	private SurfacePanel createDatabaseForm()
	{
		final SurfacePanel panel = section("Database connection");
		final JPanel form = new JPanel(new GridBagLayout());
		form.setOpaque(false);
		addField(form, 0, "Host", _host);
		addField(form, 1, "Port", _port);
		addField(form, 2, "Database", _databaseName);
		addField(form, 3, "User", _user);
		addField(form, 4, "Password", _password);
		_mysqlPath.setEditable(false);
		_backupPath.setEditable(false);
		addField(form, 5, "MySQL tools", _mysqlPath);
		addField(form, 6, "Backup directory", _backupPath);
		panel.add(form, BorderLayout.CENTER);
		return panel;
	}
	
	private JPanel createActions()
	{
		final JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		actions.setOpaque(false);
		final DashboardActionButton detect = new DashboardActionButton("Detect MySQL", ControlCenterIcons.create(GlyphIcon.Kind.SEARCH, ControlCenterTheme.TEXT), ControlCenterTheme.ACCENT, false);
		detect.addActionListener(event -> detectMySql());
		_test.addActionListener(event -> testConnection());
		_backup.addActionListener(event -> createBackup());
		_restore.addActionListener(event -> chooseRestore());
		_apply.addActionListener(event -> applySettings());
		actions.add(detect);
		actions.add(_test);
		actions.add(_backup);
		actions.add(_restore);
		actions.add(_apply);
		return actions;
	}
	
	private DashboardActionButton folderButton(String text, Path path)
	{
		final DashboardActionButton button = new DashboardActionButton(text, ControlCenterIcons.create(GlyphIcon.Kind.FOLDER, ControlCenterTheme.TEXT), ControlCenterTheme.ACCENT, false);
		button.addActionListener(event -> openFolder(path));
		return button;
	}
	
	private void loadConfiguration()
	{
		_backupPath.setText(_backups.backupDirectory().toAbsolutePath().normalize().toString());
		_status.setText("Loading database configuration...");
		new SwingWorker<DatabaseView, Void>()
		{
			@Override
			protected DatabaseView doInBackground() throws Exception
			{
				final DatabaseEndpoint endpoint = _database.loadPrimary();
				return new DatabaseView(endpoint, _backups.locateDumpTool(endpoint));
			}
			
			@Override
			protected void done()
			{
				try
				{
					final DatabaseView view = get();
					final DatabaseEndpoint endpoint = view.endpoint();
					final MySqlJdbcAddress address = endpoint.address();
					_host.setText(address.host());
					_port.setText(Integer.toString(address.port()));
					_databaseName.setText(address.database());
					_user.setText(endpoint.user());
					_password.setText(endpoint.password());
					_mysqlPath.setText(view.dumpTool() == null ? "Not detected" : view.dumpTool().getParent().toString());
					_status.setText("Database configuration loaded.");
				}
				catch (Exception e)
				{
					_status.setText("Database configuration could not be loaded: " + rootMessage(e));
				}
			}
		}.execute();
	}
	
	private void openDatabaseInstaller()
	{
		final DatabaseInput input;
		try
		{
			input = captureInput();
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, "Invalid database settings: " + rootMessage(e), "Database Installer", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		final WorkloadPermit permit = acquireHeavy("Database installation");
		if (permit == null)
		{
			return;
		}
		
		_status.setText("Preparing integrated database installer...");
		new SwingWorker<DatabaseEndpoint, Void>()
		{
			@Override
			protected DatabaseEndpoint doInBackground() throws Exception
			{
				final DatabaseEndpoint endpoint = endpointFromInput(input);
				_installer.validateResources();
				return endpoint;
			}
			
			@Override
			protected void done()
			{
				try
				{
					final DatabaseEndpoint endpoint = get();
					final Window owner = SwingUtilities.getWindowAncestor(DatabaseMaintenancePanel.this);
					final DatabaseInstallerDialog dialog = new DatabaseInstallerDialog(owner, endpoint, _database, _installer, _backups, _login, _game, () ->
					{
						loadConfiguration();
						_backupChanged.run();
					});
					dialog.open();
					_status.setText("Integrated database installer closed.");
				}
				catch (Exception e)
				{
					_status.setText("Integrated database installer is unavailable.");
					JOptionPane.showMessageDialog(DatabaseMaintenancePanel.this, "Unable to open the integrated database installer: " + rootMessage(e), "Database Installer", JOptionPane.ERROR_MESSAGE);
				}
				finally
				{
					permit.close();
				}
			}
		}.execute();
	}
	
	private void detectMySql()
	{
		final DatabaseInput input;
		try
		{
			input = captureInput();
		}
		catch (Exception e)
		{
			_status.setText("MySQL detection failed: " + rootMessage(e));
			return;
		}
		
		_status.setText("Detecting MySQL tools...");
		new SwingWorker<Path, Void>()
		{
			@Override
			protected Path doInBackground() throws Exception
			{
				return _backups.locateDumpTool(endpointFromInput(input));
			}
			
			@Override
			protected void done()
			{
				try
				{
					final Path dumpTool = get();
					_mysqlPath.setText(dumpTool == null ? "Not detected" : dumpTool.getParent().toString());
					_status.setText(dumpTool == null ? "MySQL tools were not detected." : "MySQL tools detected.");
				}
				catch (Exception e)
				{
					_status.setText("MySQL detection failed: " + rootMessage(e));
				}
			}
		}.execute();
	}
	
	private void testConnection()
	{
		final DatabaseInput input;
		try
		{
			input = captureInput();
		}
		catch (Exception e)
		{
			_status.setText("Database connection settings are invalid: " + rootMessage(e));
			return;
		}
		
		final WorkloadPermit permit = acquireHeavy("Database connection test");
		if (permit == null)
		{
			return;
		}
		
		_status.setText("Testing database connection...");
		new SwingWorker<Void, Void>()
		{
			@Override
			protected Void doInBackground() throws Exception
			{
				_database.test(endpointFromInput(input));
				return null;
			}
			
			@Override
			protected void done()
			{
				try
				{
					get();
					_status.setText("Database connection succeeded.");
				}
				catch (Exception e)
				{
					_status.setText("Database connection failed.");
					JOptionPane.showMessageDialog(DatabaseMaintenancePanel.this, "Database connection failed: " + rootMessage(e), "Database connection", JOptionPane.ERROR_MESSAGE);
				}
				finally
				{
					permit.close();
				}
			}
		}.execute();
	}
	
	private void createBackup()
	{
		final DatabaseInput input;
		try
		{
			input = captureInput();
		}
		catch (Exception e)
		{
			_status.setText("Database backup settings are invalid: " + rootMessage(e));
			return;
		}
		
		final WorkloadPermit permit = acquireHeavy("Database backup");
		if (permit == null)
		{
			return;
		}
		
		_status.setText("Creating database backup...");
		new SwingWorker<Path, Void>()
		{
			@Override
			protected Path doInBackground() throws Exception
			{
				return _backups.create(endpointFromInput(input));
			}
			
			@Override
			protected void done()
			{
				try
				{
					final Path file = get();
					_status.setText("Backup created: " + file.getFileName());
					_backupChanged.run();
				}
				catch (Exception e)
				{
					_status.setText("Database backup failed.");
					JOptionPane.showMessageDialog(DatabaseMaintenancePanel.this, "Database backup failed: " + rootMessage(e), "Database backup", JOptionPane.ERROR_MESSAGE);
				}
				finally
				{
					permit.close();
				}
			}
		}.execute();
	}
	
	private void chooseRestore()
	{
		if (!_workloads.isMaintenanceAvailable())
		{
			showMaintenanceBlocked("Database restore");
			return;
		}
		
		final JFileChooser chooser = new JFileChooser(_backups.backupDirectory().toFile());
		chooser.setDialogTitle("Select SQL backup");
		chooser.setFileFilter(new FileNameExtensionFilter("SQL backup (*.sql)", "sql"));
		if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}
		
		final Path selected = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
		final int answer = JOptionPane.showConfirmDialog(this, "Restore " + selected.getFileName() + " into the configured database? A preventive backup will be created first.", "Confirm database restore", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (answer != JOptionPane.YES_OPTION)
		{
			return;
		}
		
		restore(selected);
	}
	
	private void restore(Path backup)
	{
		final DatabaseInput input;
		try
		{
			input = captureInput();
		}
		catch (Exception e)
		{
			_status.setText("Database restore settings are invalid: " + rootMessage(e));
			return;
		}
		
		final WorkloadPermit permit = acquireHeavy("Database restore");
		if (permit == null)
		{
			return;
		}
		
		_status.setText("Restoring database...");
		new SwingWorker<Void, Void>()
		{
			@Override
			protected Void doInBackground() throws Exception
			{
				_backups.restore(endpointFromInput(input), backup);
				return null;
			}
			
			@Override
			protected void done()
			{
				try
				{
					get();
					_status.setText("Database restore completed.");
					_backupChanged.run();
				}
				catch (Exception e)
				{
					_status.setText("Database restore failed.");
					JOptionPane.showMessageDialog(DatabaseMaintenancePanel.this, "Database restore failed: " + rootMessage(e), "Database restore", JOptionPane.ERROR_MESSAGE);
				}
				finally
				{
					permit.close();
				}
			}
		}.execute();
	}
	
	private void applySettings()
	{
		final DatabaseInput input;
		try
		{
			input = captureInput();
		}
		catch (Exception e)
		{
			_status.setText("Database settings are invalid: " + rootMessage(e));
			return;
		}
		
		final WorkloadPermit permit = acquireHeavy("Database configuration update");
		if (permit == null)
		{
			return;
		}
		
		_status.setText("Applying database settings...");
		new SwingWorker<Void, Void>()
		{
			@Override
			protected Void doInBackground() throws Exception
			{
				_database.applyShared(input.host(), input.port(), input.database(), input.user(), input.password());
				return null;
			}
			
			@Override
			protected void done()
			{
				try
				{
					get();
					_status.setText("Database settings saved to Login Server and Game Server.");
				}
				catch (Exception e)
				{
					_status.setText("Database settings update failed.");
					JOptionPane.showMessageDialog(DatabaseMaintenancePanel.this, "Unable to save database settings: " + rootMessage(e), "Database settings", JOptionPane.ERROR_MESSAGE);
				}
				finally
				{
					permit.close();
				}
			}
		}.execute();
	}
	
	private WorkloadPermit acquireHeavy(String operation)
	{
		final WorkloadPermit permit = _workloads.tryAcquireHeavy(operation);
		if (permit == null)
		{
			showMaintenanceBlocked(operation);
		}
		
		return permit;
	}
	
	private void showMaintenanceBlocked(String operation)
	{
		final String reason = _workloads.maintenanceBlockReason();
		_status.setText(operation + " is unavailable: " + reason);
		JOptionPane.showMessageDialog(this, reason, operation + " unavailable", JOptionPane.WARNING_MESSAGE);
	}
	
	private void refreshAvailability()
	{
		final boolean available = _workloads.isMaintenanceAvailable();
		final String reason = _workloads.maintenanceBlockReason();
		_install.setEnabled(available);
		_test.setEnabled(available);
		_backup.setEnabled(available);
		_restore.setEnabled(available);
		_apply.setEnabled(available);
		_install.setToolTipText(available ? null : reason);
		_test.setToolTipText(available ? null : reason);
		_backup.setToolTipText(available ? null : reason);
		_restore.setToolTipText(available ? null : reason);
		_apply.setToolTipText(available ? null : reason);
	}
	
	private DatabaseInput captureInput()
	{
		return new DatabaseInput(_host.getText().trim(), parsePort(_port.getText()), _databaseName.getText().trim(), _user.getText().trim(), new String(_password.getPassword()));
	}
	
	private DatabaseEndpoint endpointFromInput(DatabaseInput input) throws Exception
	{
		final DatabaseEndpoint current = _database.loadPrimary();
		final String url = current.address().rebuild(input.host(), input.port(), input.database());
		return new DatabaseEndpoint(current.sourceFile(), current.driver(), url, input.user(), input.password(), current.mysqlBinDirectory());
	}
	
	private void openFolder(Path path)
	{
		try
		{
			Files.createDirectories(path);
			if (!Desktop.isDesktopSupported())
			{
				throw new IllegalStateException("Desktop integration is not available.");
			}
			
			Desktop.getDesktop().open(path.toFile());
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, "Unable to open folder: " + rootMessage(e), "Local folder", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private static int parsePort(String text)
	{
		final int value = Integer.parseInt(text.trim());
		if ((value < 1) || (value > 65535))
		{
			throw new IllegalArgumentException("Port must be between 1 and 65535.");
		}
		
		return value;
	}
	
	private static SurfacePanel section(String titleText)
	{
		final SurfacePanel panel = new SurfacePanel(12, ControlCenterTheme.SURFACE, ControlCenterTheme.BORDER);
		panel.setLayout(new BorderLayout(0, 9));
		panel.setBorder(BorderFactory.createCompoundBorder(panel.getBorder(), BorderFactory.createEmptyBorder(10, 12, 12, 12)));
		final JLabel title = new JLabel(titleText);
		title.setFont(ControlCenterTheme.font(Font.BOLD, 12));
		panel.add(title, BorderLayout.NORTH);
		return panel;
	}
	
	private static void addField(JPanel panel, int row, String labelText, JComponent field)
	{
		final GridBagConstraints label = new GridBagConstraints();
		label.gridx = 0;
		label.gridy = row;
		label.anchor = GridBagConstraints.WEST;
		label.insets = new Insets(4, 0, 4, 12);
		final JLabel text = new JLabel(labelText);
		text.setForeground(ControlCenterTheme.MUTED);
		text.setFont(ControlCenterTheme.font(Font.PLAIN, 10));
		panel.add(text, label);
		final GridBagConstraints value = new GridBagConstraints();
		value.gridx = 1;
		value.gridy = row;
		value.weightx = 1;
		value.fill = GridBagConstraints.HORIZONTAL;
		value.insets = new Insets(4, 0, 4, 0);
		panel.add(field, value);
	}
	
	private static String rootMessage(Exception exception)
	{
		Throwable current = exception;
		while ((current.getCause() != null) && (current.getCause() != current))
		{
			current = current.getCause();
		}
		
		return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
	}
}
