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
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Files;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.l2jmobius.tools.dashboard.bridge.DashboardProfile;
import org.l2jmobius.tools.dashboard.settings.ControlCenterSettings;
import org.l2jmobius.tools.dashboard.settings.SettingsRepository;

/**
 * Provides local Control Center preferences for configuration access, persisted window state, and product information.
 * @author BazookaRpm
 */
public final class GeneralPreferencesPanel extends JPanel
{
	private final ControlCenterSettings _settings;
	
	public GeneralPreferencesPanel(ControlCenterSettings settings)
	{
		_settings = settings;
		setOpaque(false);
		setLayout(new BorderLayout(0, 8));
		add(DashboardUiFactory.title("Preferences", "Local Control Center configuration and persisted window behavior"), BorderLayout.PAGE_START);
		final JPanel cards = DashboardUiFactory.panel(new GridBagLayout());
		place(cards, preferenceCard("Dashboard configuration", "Open the local Dashboard.ini configuration file.", "Open configuration", GlyphIcon.Kind.SETTINGS, this::openConfiguration), 0);
		place(cards, preferenceCard("Window position", "Forget the saved position and size for the next dashboard launch.", "Reset window", GlyphIcon.Kind.RESTART, this::resetWindow), 1);
		place(cards, preferenceCard("About", "Product information, author, managed services, metrics, and observability.", "Show information", GlyphIcon.Kind.INFO, this::showAbout), 2);
		add(cards, BorderLayout.CENTER);
	}
	
	private SurfacePanel preferenceCard(String heading, String description, String actionText, GlyphIcon.Kind icon, Runnable action)
	{
		final SurfacePanel card = new SurfacePanel(12, ControlCenterTheme.SURFACE, ControlCenterTheme.BORDER);
		card.setLayout(new GridBagLayout());
		final GridBagConstraints text = new GridBagConstraints();
		text.gridx = 0;
		text.gridy = 0;
		text.weightx = 1;
		text.fill = GridBagConstraints.HORIZONTAL;
		text.anchor = GridBagConstraints.WEST;
		text.insets = new Insets(12, 14, 12, 10);
		card.add(DashboardUiFactory.title(heading, description), text);
		final DashboardActionButton button = new DashboardActionButton(actionText, ControlCenterIcons.create(icon, 14, ControlCenterTheme.TEXT_BRIGHT), ControlCenterTheme.ACCENT, false);
		button.addActionListener(event -> action.run());
		final GridBagConstraints buttonConstraints = new GridBagConstraints();
		buttonConstraints.gridx = 1;
		buttonConstraints.gridy = 0;
		buttonConstraints.insets = new Insets(12, 0, 12, 12);
		card.add(button, buttonConstraints);
		return card;
	}
	
	private void place(JPanel parent, SurfacePanel card, int row)
	{
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = row;
		constraints.weightx = 1;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.anchor = GridBagConstraints.NORTH;
		constraints.insets = new Insets(row == 0 ? 0 : 4, 0, 4, 0);
		parent.add(card, constraints);
		if (row == 2)
		{
			constraints.gridy = 3;
			constraints.weighty = 1;
			constraints.fill = GridBagConstraints.BOTH;
			parent.add(new JPanel(), constraints);
		}
	}
	
	private void openConfiguration()
	{
		runDesktopAction("Dashboard configuration", () ->
		{
			SettingsRepository.save(_settings);
			if (!Files.isRegularFile(_settings.getSettingsFile()))
			{
				throw new IllegalStateException("Dashboard.ini was not created.");
			}
			
			Desktop.getDesktop().open(_settings.getSettingsFile().toFile());
		});
	}
	
	private void resetWindow()
	{
		try
		{
			final Object[][] defaults =
			{
				{
					ControlCenterSettings.WINDOW_X,
					-1
				},
				{
					ControlCenterSettings.WINDOW_Y,
					-1
				},
				{
					ControlCenterSettings.WINDOW_WIDTH,
					1440
				},
				{
					ControlCenterSettings.WINDOW_HEIGHT,
					860
				},
				{
					ControlCenterSettings.WINDOW_MAXIMIZED,
					false
				}
			};
			for (Object[] entry : defaults)
			{
				_settings.set((String) entry[0], entry[1]);
			}
			
			SettingsRepository.save(_settings);
			JOptionPane.showMessageDialog(this, "Saved window geometry was reset for the next launch.", "Preferences", JOptionPane.INFORMATION_MESSAGE);
		}
		catch (Exception e)
		{
			showError("Preferences", e);
		}
	}
	
	private void showAbout()
	{
		final SurfacePanel about = new SurfacePanel(12, ControlCenterTheme.SURFACE, ControlCenterTheme.BORDER);
		about.setLayout(new BoxLayout(about, BoxLayout.Y_AXIS));
		about.setBorder(BorderFactory.createCompoundBorder(about.getBorder(), BorderFactory.createEmptyBorder(14, 18, 14, 18)));
		about.add(aboutLabel(DashboardProfile.PRODUCT_NAME.toUpperCase(), Font.BOLD, 15, ControlCenterTheme.TEXT_BRIGHT));
		about.add(Box.createVerticalStrut(10));
		about.add(aboutLabel("Author: BazookaRpm", Font.BOLD, 11, ControlCenterTheme.TEXT));
		about.add(aboutLabel(DashboardProfile.DISTRIBUTION_NAME, Font.PLAIN, 11, ControlCenterTheme.MUTED));
		about.add(Box.createVerticalStrut(10));
		about.add(aboutLabel("Managed GameServer and LoginServer", Font.PLAIN, 11, ControlCenterTheme.TEXT));
		about.add(aboutLabel("Runtime metrics and observability", Font.PLAIN, 11, ControlCenterTheme.TEXT));
		about.add(Box.createVerticalStrut(10));
		about.add(aboutLabel("2026", Font.PLAIN, 10, ControlCenterTheme.MUTED));
		JOptionPane.showMessageDialog(this, about, "About " + DashboardProfile.PRODUCT_NAME, JOptionPane.PLAIN_MESSAGE);
	}
	
	private static JLabel aboutLabel(String text, int style, int size, Color color)
	{
		final JLabel label = new JLabel(text);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		label.setFont(ControlCenterTheme.font(style, size));
		label.setForeground(color);
		return label;
	}
	
	private void runDesktopAction(String title, CheckedAction action)
	{
		try
		{
			if (!Desktop.isDesktopSupported())
			{
				throw new IllegalStateException("Desktop integration is not available.");
			}
			
			action.run();
		}
		catch (Exception e)
		{
			showError(title, e);
		}
	}
	
	private void showError(String title, Exception exception)
	{
		Throwable cause = exception;
		while ((cause.getCause() != null) && (cause.getCause() != cause))
		{
			cause = cause.getCause();
		}
		
		JOptionPane.showMessageDialog(this, cause.getMessage(), title, JOptionPane.ERROR_MESSAGE);
	}
	
	@FunctionalInterface
	private interface CheckedAction
	{
		void run() throws Exception;
	}
}
