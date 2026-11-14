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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.l2jmobius.tools.dashboard.bridge.DashboardProfile;
import org.l2jmobius.tools.dashboard.runtime.ServiceRole;
import org.l2jmobius.tools.dashboard.settings.ControlCenterSettings;
import org.l2jmobius.tools.dashboard.settings.SettingsRepository;

/**
 * Edits the Control Center execution, monitoring, console retention, and service startup settings. <br>
 * Persisted values use the flat PascalCase setting names documented in {@code Dashboard.ini}.
 * @author BazookaRpm
 */
public final class RuntimeSettingsPanel extends JPanel
{
	private final ControlCenterSettings _settings;
	private final JTextField _javaPath = new JTextField();
	private final JTextField _healthCheck = new JTextField();
	private final JTextField _telemetry = new JTextField();
	private final JTextField _maximumLines = new JTextField();
	private final JTextField _historySamples = new JTextField();
	private final JTextField _shutdownTimeout = new JTextField();
	private final JCheckBox _autoStartLogin = new JCheckBox("Start Login Server automatically before Game Server when required");
	
	public RuntimeSettingsPanel(ControlCenterSettings settings)
	{
		_settings = settings;
		setOpaque(false);
		setLayout(new BorderLayout(0, 10));
		add(createTitle(), BorderLayout.NORTH);
		final JPanel content = new JPanel(new BorderLayout(0, 10));
		content.setOpaque(false);
		content.add(createExecutionSection(), BorderLayout.NORTH);
		content.add(createDistributionSection(), BorderLayout.CENTER);
		content.add(createActions(), BorderLayout.SOUTH);
		add(content, BorderLayout.CENTER);
		loadValues();
	}
	
	private JPanel createTitle()
	{
		final JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		final JLabel title = new JLabel("Settings");
		title.setFont(ControlCenterTheme.font(Font.BOLD, 17));
		final JLabel subtitle = new JLabel("Managed execution, monitoring cadence, console retention, and " + DashboardProfile.CHRONICLE_NAME + " distribution paths");
		subtitle.setFont(ControlCenterTheme.font(Font.PLAIN, 10));
		subtitle.setForeground(ControlCenterTheme.MUTED);
		panel.add(title, BorderLayout.NORTH);
		panel.add(subtitle, BorderLayout.SOUTH);
		return panel;
	}
	
	private SurfacePanel createExecutionSection()
	{
		final SurfacePanel section = section("Execution and monitoring");
		final JPanel form = new JPanel(new GridBagLayout());
		form.setOpaque(false);
		addField(form, 0, "Java executable override", _javaPath);
		addField(form, 1, "Health check interval (ms)", _healthCheck);
		addField(form, 2, "Telemetry interval (ms)", _telemetry);
		addField(form, 3, "Maximum console lines", _maximumLines);
		addField(form, 4, "Telemetry history samples", _historySamples);
		addField(form, 5, "Shutdown timeout (seconds)", _shutdownTimeout);
		addReadOnly(form, 6, "Managed mode", "Login Server and Game Server are launched as independent JVM processes.");
		addReadOnly(form, 7, "Maintenance policy", "Heavy maintenance work is mutually exclusive with managed service startup/runtime.");
		_autoStartLogin.setOpaque(false);
		_autoStartLogin.setForeground(ControlCenterTheme.TEXT);
		final GridBagConstraints check = new GridBagConstraints();
		check.gridx = 0;
		check.gridy = 8;
		check.gridwidth = 2;
		check.anchor = GridBagConstraints.WEST;
		check.insets = new Insets(7, 0, 3, 0);
		form.add(_autoStartLogin, check);
		section.add(form, BorderLayout.CENTER);
		return section;
	}
	
	private SurfacePanel createDistributionSection()
	{
		final SurfacePanel section = section("Distribution paths");
		final JPanel form = new JPanel(new GridBagLayout());
		form.setOpaque(false);
		addReadOnly(form, 0, "Distribution root", _settings.getDistributionRoot().toString());
		addReadOnly(form, 1, "Login java.cfg", _settings.getJavaOptionsFile(ServiceRole.LOGIN).toString());
		addReadOnly(form, 2, "Login JAR", _settings.getServiceJar(ServiceRole.LOGIN).toString());
		addReadOnly(form, 3, "Game java.cfg", _settings.getJavaOptionsFile(ServiceRole.GAME).toString());
		addReadOnly(form, 4, "Game JAR", _settings.getServiceJar(ServiceRole.GAME).toString());
		section.add(form, BorderLayout.CENTER);
		return section;
	}
	
	private JPanel createActions()
	{
		final JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		actions.setOpaque(false);
		final DashboardActionButton defaults = new DashboardActionButton("Restore defaults", ControlCenterIcons.create(GlyphIcon.Kind.RESTART, ControlCenterTheme.TEXT), ControlCenterTheme.ACCENT, false);
		final DashboardActionButton save = new DashboardActionButton("Save", ControlCenterIcons.create(GlyphIcon.Kind.SAVE, ControlCenterTheme.TEXT), ControlCenterTheme.ACTION_PRIMARY, true);
		defaults.addActionListener(event -> restoreDefaults());
		save.addActionListener(event -> save());
		actions.add(defaults);
		actions.add(save);
		return actions;
	}
	
	private void loadValues()
	{
		_javaPath.setText(_settings.getJavaExecutableOverride());
		_healthCheck.setText(Integer.toString(_settings.getHealthCheckIntervalMillis()));
		_telemetry.setText(Integer.toString(_settings.getTelemetryIntervalMillis()));
		_maximumLines.setText(Integer.toString(_settings.getConsoleLineLimit()));
		_historySamples.setText(Integer.toString(_settings.getHistoryCapacity()));
		_shutdownTimeout.setText(Integer.toString(_settings.getShutdownTimeoutSeconds()));
		_autoStartLogin.setSelected(_settings.isLoginAutoStartEnabled());
	}
	
	private void save()
	{
		try
		{
			final int healthCheck = boundedInt(_healthCheck.getText(), 250, 10000, "Health check interval");
			final int telemetry = boundedInt(_telemetry.getText(), 500, 60000, "Telemetry interval");
			final int lines = boundedInt(_maximumLines.getText(), 1000, 100000, "Maximum console lines");
			final int history = boundedInt(_historySamples.getText(), 60, 20000, "Telemetry history samples");
			final int timeout = boundedInt(_shutdownTimeout.getText(), 5, 300, "Shutdown timeout");
			_settings.set(ControlCenterSettings.SERVICE_JAVA_PATH, _javaPath.getText().trim());
			_settings.set(ControlCenterSettings.MONITOR_HEALTH_CHECK_MS, healthCheck);
			_settings.set(ControlCenterSettings.MONITOR_TELEMETRY_MS, telemetry);
			_settings.set(ControlCenterSettings.CONSOLE_MAX_LINES, lines);
			_settings.set(ControlCenterSettings.TELEMETRY_HISTORY_SAMPLES, history);
			_settings.set(ControlCenterSettings.SERVICE_SHUTDOWN_TIMEOUT_SECONDS, timeout);
			_settings.set(ControlCenterSettings.SERVICE_AUTO_START_LOGIN, _autoStartLogin.isSelected());
			SettingsRepository.save(_settings);
			JOptionPane.showMessageDialog(this, "Settings saved. Monitoring interval changes apply after the Control Center is restarted; service launch settings apply to subsequent starts.", "Settings", JOptionPane.INFORMATION_MESSAGE);
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, "Unable to save settings: " + rootMessage(e), "Settings", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void restoreDefaults()
	{
		_javaPath.setText("");
		_healthCheck.setText("1000");
		_telemetry.setText("2000");
		_maximumLines.setText("10000");
		_historySamples.setText("1800");
		_shutdownTimeout.setText("30");
		_autoStartLogin.setSelected(true);
	}
	
	private static int boundedInt(String text, int minimum, int maximum, String name)
	{
		final int value = Integer.parseInt(text.trim());
		if ((value < minimum) || (value > maximum))
		{
			throw new IllegalArgumentException(name + " must be between " + minimum + " and " + maximum + ".");
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
	
	private static void addField(JPanel panel, int row, String labelText, JTextField field)
	{
		final GridBagConstraints label = constraints(0, row, 0);
		panel.add(label(labelText), label);
		final GridBagConstraints value = constraints(1, row, 1);
		value.weightx = 1;
		panel.add(field, value);
	}
	
	private static void addReadOnly(JPanel panel, int row, String labelText, String valueText)
	{
		final JTextField field = new JTextField(valueText);
		field.setEditable(false);
		addField(panel, row, labelText, field);
	}
	
	private static JLabel label(String text)
	{
		final JLabel label = new JLabel(text);
		label.setForeground(ControlCenterTheme.MUTED);
		label.setFont(ControlCenterTheme.font(Font.PLAIN, 10));
		return label;
	}
	
	private static GridBagConstraints constraints(int x, int y, int fill)
	{
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = x;
		constraints.gridy = y;
		constraints.fill = fill == 1 ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = new Insets(4, 0, 4, x == 0 ? 12 : 0);
		return constraints;
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
