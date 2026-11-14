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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.l2jmobius.tools.dashboard.bridge.DashboardProfile;
import org.l2jmobius.tools.dashboard.database.DatabaseConfigurationService;
import org.l2jmobius.tools.dashboard.database.DatabaseEndpoint;
import org.l2jmobius.tools.dashboard.database.DatabaseInstallationEngine;
import org.l2jmobius.tools.dashboard.database.DatabaseInstallationEngine.ConnectionSettings;
import org.l2jmobius.tools.dashboard.database.DatabaseInstallationEngine.ExistingDatabaseAction;
import org.l2jmobius.tools.dashboard.database.DatabaseInstallationEngine.ExistingDatabaseDecision;
import org.l2jmobius.tools.dashboard.database.DatabaseInstallationEngine.InstallationResult;
import org.l2jmobius.tools.dashboard.database.DatabaseInstallationEngine.ProgressEvent;
import org.l2jmobius.tools.dashboard.database.DatabaseInstallationEngine.ProgressLevel;
import org.l2jmobius.tools.dashboard.database.DatabaseInstallationEngine.ResultStatus;
import org.l2jmobius.tools.dashboard.database.DatabaseInstallationEngine.ScriptErrorAction;
import org.l2jmobius.tools.dashboard.database.DatabaseInstallationEngine.ScriptErrorDecision;
import org.l2jmobius.tools.dashboard.database.MySqlJdbcAddress;
import org.l2jmobius.tools.dashboard.database.SqlBackupCoordinator;
import org.l2jmobius.tools.dashboard.runtime.ServiceSupervisor;

/**
 * Provides an integrated dark-gray database installer owned by the Dashboard window.
 * @author BazookaRpm
 */
public final class DatabaseInstallerDialog extends JDialog
{
	private final DatabaseConfigurationService _databaseConfiguration;
	private final DatabaseInstallationEngine _installer;
	private final SqlBackupCoordinator _backups;
	private final ServiceSupervisor _login;
	private final ServiceSupervisor _game;
	private final Runnable _installationCompleted;
	private final JTextField _host = new JTextField();
	private final JTextField _port = new JTextField();
	private final JTextField _databaseName = new JTextField();
	private final JTextField _user = new JTextField();
	private final JPasswordField _password = new JPasswordField();
	private final JCheckBox _installLogin = new JCheckBox("Install Login database");
	private final JCheckBox _installGame = new JCheckBox("Install Game database");
	private final JCheckBox _saveConnection = new JCheckBox("Apply connection to LoginServer and GameServer after successful installation");
	private final JLabel _resourceSummary = new JLabel("Checking official SQL resources...");
	private final JLabel _status = new JLabel("Ready.");
	private final JProgressBar _progress = new JProgressBar(0, 100);
	private final JTextPane _log = new JTextPane();
	private final DashboardActionButton _testButton = new DashboardActionButton("Test Connection", ControlCenterIcons.create(GlyphIcon.Kind.DIAGNOSTICS, ControlCenterTheme.TEXT), ControlCenterTheme.ACCENT, false);
	private final DashboardActionButton _installButton = new DashboardActionButton("Install Database", ControlCenterIcons.create(GlyphIcon.Kind.DATABASE, ControlCenterTheme.TEXT), ControlCenterTheme.ACTION_PRIMARY, true);
	private final DashboardActionButton _cancelButton = new DashboardActionButton("Cancel", ControlCenterIcons.create(GlyphIcon.Kind.STOP, ControlCenterTheme.TEXT), ControlCenterTheme.RED, false);
	private final DashboardActionButton _closeButton = new DashboardActionButton("Close", null, ControlCenterTheme.ACCENT, false);
	private final AtomicBoolean _cancelRequested = new AtomicBoolean(false);
	private SwingWorker<?, ?> _activeWorker;
	private DatabaseEndpoint _templateEndpoint;
	
	/**
	 * Creates the integrated database installation dialog.
	 * @param owner owning Dashboard window
	 * @param endpoint initial database connection settings
	 * @param databaseConfiguration service used to persist successful connection settings
	 * @param installer neutral database installation engine
	 * @param backups backup coordinator used before destructive recreation
	 * @param login managed LoginServer supervisor
	 * @param game managed GameServer supervisor
	 * @param installationCompleted callback invoked after a successful installation
	 */
	public DatabaseInstallerDialog(Window owner, DatabaseEndpoint endpoint, DatabaseConfigurationService databaseConfiguration, DatabaseInstallationEngine installer, SqlBackupCoordinator backups, ServiceSupervisor login, ServiceSupervisor game, Runnable installationCompleted)
	{
		super(owner, "Database Installation - L2J Mobius Control Dashboard", ModalityType.APPLICATION_MODAL);
		_templateEndpoint = endpoint;
		_databaseConfiguration = databaseConfiguration;
		_installer = installer;
		_backups = backups;
		_login = login;
		_game = game;
		_installationCompleted = installationCompleted;
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		setMinimumSize(new Dimension(900, 620));
		setPreferredSize(new Dimension(980, 680));
		setContentPane(createContent());
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent event)
			{
				closeRequested();
			}
		});
		loadEndpoint(endpoint);
		configureActions();
		refreshResourceSummary();
		pack();
		setLocationRelativeTo(owner);
	}
	
	/** Opens the modal installer window. */
	public void open()
	{
		setVisible(true);
	}
	
	private JPanel createContent()
	{
		final JPanel root = new JPanel(new BorderLayout(12, 12));
		root.setBackground(ControlCenterTheme.BACKGROUND);
		root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
		root.add(createHeader(), BorderLayout.NORTH);
		final JPanel center = new JPanel(new BorderLayout(12, 12));
		center.setOpaque(false);
		final JPanel top = new JPanel(new GridBagLayout());
		top.setOpaque(false);
		final GridBagConstraints left = new GridBagConstraints();
		left.gridx = 0;
		left.gridy = 0;
		left.weightx = 0.54;
		left.weighty = 1;
		left.fill = GridBagConstraints.BOTH;
		left.insets = new Insets(0, 0, 0, 6);
		top.add(createConnectionPanel(), left);
		final GridBagConstraints right = new GridBagConstraints();
		right.gridx = 1;
		right.gridy = 0;
		right.weightx = 0.46;
		right.weighty = 1;
		right.fill = GridBagConstraints.BOTH;
		right.insets = new Insets(0, 6, 0, 0);
		top.add(createSelectionPanel(), right);
		center.add(top, BorderLayout.NORTH);
		center.add(createProgressPanel(), BorderLayout.CENTER);
		root.add(center, BorderLayout.CENTER);
		root.add(createFooter(), BorderLayout.SOUTH);
		return root;
	}
	
	private JPanel createHeader()
	{
		final JPanel panel = new JPanel();
		panel.setOpaque(false);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		final JLabel title = new JLabel("DATABASE INSTALLATION");
		title.setFont(ControlCenterTheme.font(Font.BOLD, 18));
		title.setForeground(ControlCenterTheme.TEXT_BRIGHT);
		final JLabel description = new JLabel("Install the " + DashboardProfile.CHRONICLE_NAME + " Login/Game schema directly from the official dist/db_installer SQL files.");
		description.setFont(ControlCenterTheme.font(Font.PLAIN, 11));
		description.setForeground(ControlCenterTheme.MUTED);
		panel.add(title);
		panel.add(Box.createVerticalStrut(4));
		panel.add(description);
		return panel;
	}
	
	private SurfacePanel createConnectionPanel()
	{
		final SurfacePanel panel = section("Connection");
		final JPanel form = new JPanel(new GridBagLayout());
		form.setOpaque(false);
		addField(form, 0, "Host", _host);
		addField(form, 1, "Port", _port);
		addField(form, 2, "Database", _databaseName);
		addField(form, 3, "Username", _user);
		addField(form, 4, "Password", _password);
		panel.add(form, BorderLayout.CENTER);
		return panel;
	}
	
	private SurfacePanel createSelectionPanel()
	{
		final SurfacePanel panel = section("Installation scope");
		final JPanel content = new JPanel();
		content.setOpaque(false);
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		styleCheckBox(_installLogin);
		styleCheckBox(_installGame);
		styleCheckBox(_saveConnection);
		_installLogin.setSelected(true);
		_installGame.setSelected(true);
		_saveConnection.setSelected(true);
		content.add(_installLogin);
		content.add(Box.createVerticalStrut(6));
		content.add(_installGame);
		content.add(Box.createVerticalStrut(12));
		content.add(_saveConnection);
		content.add(Box.createVerticalStrut(14));
		_resourceSummary.setForeground(ControlCenterTheme.MUTED);
		_resourceSummary.setFont(ControlCenterTheme.font(Font.PLAIN, 10));
		content.add(_resourceSummary);
		content.add(Box.createVerticalStrut(8));
		final JLabel safety = new JLabel("<html>Safety: LoginServer and GameServer must be offline.<br>Recreate always creates a preventive backup first.</html>");
		safety.setForeground(ControlCenterTheme.YELLOW);
		safety.setFont(ControlCenterTheme.font(Font.PLAIN, 10));
		content.add(safety);
		panel.add(content, BorderLayout.CENTER);
		return panel;
	}
	
	private SurfacePanel createProgressPanel()
	{
		final SurfacePanel panel = section("Installation progress");
		final JPanel body = new JPanel(new BorderLayout(0, 8));
		body.setOpaque(false);
		_progress.setStringPainted(true);
		_progress.setString("Ready");
		_progress.setForeground(ControlCenterTheme.ACCENT);
		_progress.setBackground(ControlCenterTheme.SURFACE_ALT);
		body.add(_progress, BorderLayout.NORTH);
		_log.setEditable(false);
		_log.setBackground(ControlCenterTheme.CONSOLE);
		_log.setForeground(ControlCenterTheme.TEXT);
		_log.setFont(ControlCenterTheme.font(Font.PLAIN, 11));
		final JScrollPane scroll = new JScrollPane(_log);
		ControlCenterTheme.styleScrollPane(scroll);
		scroll.setPreferredSize(new Dimension(720, 280));
		body.add(scroll, BorderLayout.CENTER);
		panel.add(body, BorderLayout.CENTER);
		return panel;
	}
	
	private JPanel createFooter()
	{
		final JPanel panel = new JPanel(new BorderLayout(8, 0));
		panel.setOpaque(false);
		_status.setForeground(ControlCenterTheme.MUTED);
		_status.setFont(ControlCenterTheme.font(Font.PLAIN, 10));
		panel.add(_status, BorderLayout.CENTER);
		final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		actions.setOpaque(false);
		actions.add(_testButton);
		actions.add(_installButton);
		_cancelButton.setEnabled(false);
		actions.add(_cancelButton);
		actions.add(_closeButton);
		panel.add(actions, BorderLayout.EAST);
		return panel;
	}
	
	private void configureActions()
	{
		_testButton.addActionListener(event -> testConnection());
		_installButton.addActionListener(event -> installDatabase());
		_cancelButton.addActionListener(event -> requestCancellation());
		_closeButton.addActionListener(event -> closeRequested());
		_installLogin.addActionListener(event -> updateInstallEnabled());
		_installGame.addActionListener(event -> updateInstallEnabled());
	}
	
	private void loadEndpoint(DatabaseEndpoint endpoint)
	{
		final MySqlJdbcAddress address = endpoint.address();
		_host.setText(address.host());
		_port.setText(Integer.toString(address.port()));
		_databaseName.setText(address.database());
		_user.setText(endpoint.user());
		_password.setText(endpoint.password());
	}
	
	private void refreshResourceSummary()
	{
		try
		{
			_installer.validateResources();
			_resourceSummary.setText("Official SQL: " + _installer.loginScriptCount() + " Login + " + _installer.gameScriptCount() + " Game scripts");
		}
		catch (Exception e)
		{
			_resourceSummary.setText("Official SQL resources unavailable: " + rootMessage(e));
			_resourceSummary.setForeground(ControlCenterTheme.RED);
			_installButton.setEnabled(false);
		}
	}
	
	private void testConnection()
	{
		final DatabaseEndpoint endpoint;
		try
		{
			endpoint = endpointFromFields();
		}
		catch (Exception e)
		{
			showError("Database connection", rootMessage(e));
			return;
		}
		
		beginOperation("Testing MySQL connection...");
		_activeWorker = new SwingWorker<Void, Void>()
		{
			@Override
			protected Void doInBackground() throws Exception
			{
				_installer.testServerConnection(toConnectionSettings(endpoint));
				return null;
			}
			
			@Override
			protected void done()
			{
				try
				{
					get();
					appendLog(ProgressLevel.SUCCESS, "Connection successful.");
					_status.setText("MySQL server connection succeeded.");
				}
				catch (Exception e)
				{
					appendLog(ProgressLevel.ERROR, "Connection failed: " + rootMessage(e));
					_status.setText("MySQL server connection failed.");
				}
				
				endOperation();
			}
		};
		_activeWorker.execute();
	}
	
	private void installDatabase()
	{
		if (!ensureServicesOffline("installing the database"))
		{
			return;
		}
		
		if (!_installLogin.isSelected() && !_installGame.isSelected())
		{
			showError("Database installation", "Select Login, Game, or both database groups.");
			return;
		}
		
		final DatabaseEndpoint endpoint;
		try
		{
			_installer.validateResources();
			endpoint = endpointFromFields();
		}
		catch (Exception e)
		{
			showError("Database installation", rootMessage(e));
			return;
		}
		
		if (JOptionPane.showConfirmDialog(this, installationSummary(endpoint), "Confirm Database Installation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION)
		{
			return;
		}
		
		final boolean installLogin = _installLogin.isSelected();
		final boolean installGame = _installGame.isSelected();
		final ExistingDatabaseDecision existingDatabaseDecision = this::chooseExistingDatabaseAction;
		final ScriptErrorDecision scriptErrorDecision = this::chooseScriptErrorAction;
		_cancelRequested.set(false);
		_log.setText("");
		beginOperation("Installing database...");
		_activeWorker = new SwingWorker<InstallationResult, ProgressEvent>()
		{
			@Override
			protected InstallationResult doInBackground() throws Exception
			{
				return _installer.install(toConnectionSettings(endpoint), installLogin, installGame, existingDatabaseDecision, scriptErrorDecision, () -> _backups.create(endpoint), _cancelRequested::get, this::publish);
			}
			
			@Override
			protected void process(List<ProgressEvent> events)
			{
				for (ProgressEvent event : events)
				{
					appendLog(event.level(), event.message());
					updateProgress(event.completedScripts(), event.totalScripts());
				}
			}
			
			@Override
			protected void done()
			{
				try
				{
					handleInstallationResult(endpoint, get());
				}
				catch (Exception e)
				{
					appendLog(ProgressLevel.ERROR, "Installation failed: " + rootMessage(e));
					_status.setText("Database installation failed.");
					showError("Database installation", rootMessage(e));
				}
				
				endOperation();
			}
		};
		_activeWorker.execute();
	}
	
	private ExistingDatabaseAction chooseExistingDatabaseAction(String databaseName) throws Exception
	{
		return callOnEventThread(() ->
		{
			final Object[] options =
			{
				"Recreate Database",
				"Install on Existing Database",
				"Cancel"
			};
			final String message = "Database '" + databaseName + "' already exists.\n\n" + "Recreate Database:\nCreates a preventive backup, deletes the existing schema, then creates it again.\n\n" + "Install on Existing Database:\nKeeps current data and executes the selected official SQL scripts.\nSQL conflicts may require operator decisions.\n";
			final int selected = JOptionPane.showOptionDialog(DatabaseInstallerDialog.this, message, "Database Already Exists", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[2]);
			if (selected == 0)
			{
				return ExistingDatabaseAction.RECREATE;
			}
			
			if (selected == 1)
			{
				return ExistingDatabaseAction.USE_EXISTING;
			}
			
			return ExistingDatabaseAction.CANCEL;
		});
	}
	
	private ScriptErrorAction chooseScriptErrorAction(Path script, SQLException exception) throws Exception
	{
		return callOnEventThread(() ->
		{
			final Object[] options =
			{
				"Continue",
				"Abort Installation"
			};
			final String message = "SQL error while installing " + script.getFileName() + ".\n\n" + sanitize(exception.getMessage()) + "\n\nContinue with the remaining statements?";
			return JOptionPane.showOptionDialog(DatabaseInstallerDialog.this, message, "SQL Installation Error", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[1]) == 0 ? ScriptErrorAction.CONTINUE : ScriptErrorAction.ABORT;
		});
	}
	
	private void handleInstallationResult(DatabaseEndpoint endpoint, InstallationResult result) throws Exception
	{
		if (result.status() == ResultStatus.SUCCESS)
		{
			_progress.setValue(100);
			_progress.setString("100% - Complete");
			if (_saveConnection.isSelected())
			{
				final MySqlJdbcAddress address = endpoint.address();
				_databaseConfiguration.applyShared(address.host(), address.port(), address.database(), endpoint.user(), endpoint.password());
				appendLog(ProgressLevel.SUCCESS, "Database connection applied to LoginServer and GameServer configuration.");
			}
			
			_status.setText("Database installation completed successfully.");
			_installationCompleted.run();
			JOptionPane.showMessageDialog(this, "Database installation completed successfully.", "Database Installation", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		if (result.status() == ResultStatus.COMPLETED_WITH_ERRORS)
		{
			_status.setText("Installation completed with accepted SQL errors. Review the log before starting the servers.");
			JOptionPane.showMessageDialog(this, "Installation completed with " + result.statementErrors() + " accepted SQL error(s).\nReview the installation log before starting LoginServer or GameServer.", "Database Installation", JOptionPane.WARNING_MESSAGE);
			return;
		}
		
		if (result.status() == ResultStatus.CANCELLED)
		{
			_status.setText("Database installation cancelled.");
			return;
		}
		
		_status.setText("Database installation aborted after an SQL error.");
	}
	
	private DatabaseEndpoint endpointFromFields()
	{
		final int port = parsePort(_port.getText());
		final String host = _host.getText().trim();
		final String databaseName = _databaseName.getText().trim();
		final String user = _user.getText().trim();
		if (host.isEmpty() || databaseName.isEmpty() || user.isEmpty())
		{
			throw new IllegalArgumentException("Host, database, and username are required.");
		}
		
		final String url = _templateEndpoint.address().rebuild(host, port, databaseName);
		return new DatabaseEndpoint(_templateEndpoint.sourceFile(), _templateEndpoint.driver(), url, user, new String(_password.getPassword()), _templateEndpoint.mysqlBinDirectory());
	}
	
	private static ConnectionSettings toConnectionSettings(DatabaseEndpoint endpoint)
	{
		return new ConnectionSettings(endpoint.driver(), endpoint.jdbcUrl(), endpoint.user(), endpoint.password());
	}
	
	private boolean ensureServicesOffline(String operation)
	{
		if (_login.isRunning() || _game.isRunning())
		{
			JOptionPane.showMessageDialog(this, "Stop both LoginServer and GameServer before " + operation + ".", "Database Operation Blocked", JOptionPane.WARNING_MESSAGE);
			return false;
		}
		
		return true;
	}
	
	private String installationSummary(DatabaseEndpoint endpoint)
	{
		final MySqlJdbcAddress address = endpoint.address();
		final String scope;
		if (_installLogin.isSelected() && _installGame.isSelected())
		{
			scope = "Login + Game";
		}
		else
		{
			scope = _installLogin.isSelected() ? "Login" : "Game";
		}
		
		return "Host: " + address.host() + ":" + address.port() + "\nDatabase: " + address.database() + "\nUser: " + endpoint.user() + "\nScope: " + scope + "\n\nThe installer uses the official " + DashboardProfile.CHRONICLE_NAME + " SQL files. Continue?";
	}
	
	private void beginOperation(String status)
	{
		_cancelRequested.set(false);
		_status.setText(status);
		_progress.setValue(0);
		_progress.setString("Working...");
		setControlsBusy(true);
	}
	
	private void endOperation()
	{
		_activeWorker = null;
		setControlsBusy(false);
		_cancelRequested.set(false);
		updateInstallEnabled();
	}
	
	private void setControlsBusy(boolean busy)
	{
		_host.setEnabled(!busy);
		_port.setEnabled(!busy);
		_databaseName.setEnabled(!busy);
		_user.setEnabled(!busy);
		_password.setEnabled(!busy);
		_installLogin.setEnabled(!busy);
		_installGame.setEnabled(!busy);
		_saveConnection.setEnabled(!busy);
		_testButton.setEnabled(!busy);
		_installButton.setEnabled(!busy);
		_cancelButton.setEnabled(busy);
		_closeButton.setEnabled(!busy);
	}
	
	private void updateInstallEnabled()
	{
		if (_activeWorker == null)
		{
			_installButton.setEnabled(_installLogin.isSelected() || _installGame.isSelected());
		}
	}
	
	private void requestCancellation()
	{
		if (_activeWorker == null)
		{
			return;
		}
		
		_cancelRequested.set(true);
		_cancelButton.setEnabled(false);
		_status.setText("Cancellation requested. The current SQL statement will be allowed to finish safely.");
		appendLog(ProgressLevel.WARNING, "Cancellation requested by operator.");
	}
	
	private void closeRequested()
	{
		if (_activeWorker != null)
		{
			JOptionPane.showMessageDialog(this, "A database operation is still running. Cancel it and wait for the current SQL statement to finish before closing this window.", "Database Operation Running", JOptionPane.WARNING_MESSAGE);
			return;
		}
		
		dispose();
	}
	
	private void updateProgress(int completed, int total)
	{
		if (total <= 0)
		{
			_progress.setValue(0);
			_progress.setString("Working...");
			return;
		}
		
		final int percent = Math.min(100, Math.max(0, (int) Math.round((completed * 100.0) / total)));
		_progress.setValue(percent);
		_progress.setString(completed + " / " + total + " scripts (" + percent + "%)");
	}
	
	private void appendLog(ProgressLevel level, String message)
	{
		final StyledDocument document = _log.getStyledDocument();
		final Style style = _log.addStyle(level.name(), null);
		switch (level)
		{
			case ERROR:
			{
				StyleConstants.setForeground(style, ControlCenterTheme.RED);
				break;
			}
			case WARNING:
			{
				StyleConstants.setForeground(style, ControlCenterTheme.YELLOW);
				break;
			}
			case SUCCESS:
			{
				StyleConstants.setForeground(style, ControlCenterTheme.GREEN);
				break;
			}
			default:
			{
				StyleConstants.setForeground(style, ControlCenterTheme.TEXT);
				break;
			}
		}
		
		try
		{
			document.insertString(document.getLength(), "[" + level.name() + "] " + message + System.lineSeparator(), style);
			_log.setCaretPosition(document.getLength());
		}
		catch (BadLocationException e)
		{
			_status.setText("Unable to append an installation log message.");
		}
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
	
	private static void addField(JPanel panel, int row, String labelText, Component field)
	{
		final GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.gridx = 0;
		labelConstraints.gridy = row;
		labelConstraints.anchor = GridBagConstraints.WEST;
		labelConstraints.insets = new Insets(4, 0, 4, 12);
		final JLabel label = new JLabel(labelText);
		label.setForeground(ControlCenterTheme.MUTED);
		label.setFont(ControlCenterTheme.font(Font.PLAIN, 10));
		panel.add(label, labelConstraints);
		final GridBagConstraints fieldConstraints = new GridBagConstraints();
		fieldConstraints.gridx = 1;
		fieldConstraints.gridy = row;
		fieldConstraints.weightx = 1;
		fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
		fieldConstraints.insets = new Insets(4, 0, 4, 0);
		panel.add(field, fieldConstraints);
	}
	
	private static void styleCheckBox(JCheckBox checkBox)
	{
		checkBox.setOpaque(false);
		checkBox.setForeground(ControlCenterTheme.TEXT);
		checkBox.setFont(ControlCenterTheme.font(Font.PLAIN, 11));
		checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);
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
	
	private static <T> T callOnEventThread(Callable<T> operation) throws Exception
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			return operation.call();
		}
		
		final FutureTask<T> task = new FutureTask<>(operation);
		SwingUtilities.invokeAndWait(task);
		return task.get();
	}
	
	private void showError(String title, String message)
	{
		JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
	}
	
	private static String rootMessage(Exception exception)
	{
		Throwable current = exception;
		while ((current.getCause() != null) && (current.getCause() != current))
		{
			current = current.getCause();
		}
		
		return sanitize(current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage());
	}
	
	private static String sanitize(String message)
	{
		return message == null ? "Unknown error." : message.replace('\r', ' ').replace('\n', ' ').trim();
	}
}
