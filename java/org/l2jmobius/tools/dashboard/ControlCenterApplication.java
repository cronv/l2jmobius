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
package org.l2jmobius.tools.dashboard;

import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.l2jmobius.tools.dashboard.bridge.DashboardProfile;
import org.l2jmobius.tools.dashboard.logging.ControlCenterLog;
import org.l2jmobius.tools.dashboard.settings.ControlCenterSettings;
import org.l2jmobius.tools.dashboard.settings.SettingsRepository;
import org.l2jmobius.tools.dashboard.ui.ControlCenterTheme;

/**
 * Starts the standalone L2J Mobius Control Dashboard from the distribution dashboard directory.
 * @author BazookaRpm
 */
public final class ControlCenterApplication
{
	private ControlCenterApplication()
	{
	}
	
	public static void main(String[] args)
	{
		if (GraphicsEnvironment.isHeadless())
		{
			System.err.println("The " + DashboardProfile.EDITION_LABEL + " requires a graphical environment.");
			return;
		}
		
		try
		{
			final ControlCenterSettings settings = SettingsRepository.open(locateDistributionRoot(args));
			ControlCenterLog.initialize(settings.getDashboardLogFile());
			ControlCenterLog.write(DashboardProfile.EDITION_LABEL + " started.");
			ControlCenterTheme.install();
			SwingUtilities.invokeLater(() ->
			{
				new ControlCenterWindow(settings).setVisible(true);
			});
		}
		catch (Exception e)
		{
			// Dialogs belong on the event dispatch thread, and startup still runs on main here.
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Dashboard startup failed: " + e.getMessage(), DashboardProfile.EDITION_LABEL, JOptionPane.ERROR_MESSAGE));
		}
	}
	
	private static Path locateDistributionRoot(String[] args)
	{
		if ((args != null) && (args.length > 0) && (args[0] != null) && !args[0].isBlank())
		{
			return Paths.get(args[0]).toAbsolutePath().normalize();
		}
		
		final Path current = Paths.get("").toAbsolutePath().normalize();
		if (isDistributionRoot(current))
		{
			return current;
		}
		
		final Path parent = current.getParent();
		if ((parent != null) && isDistributionRoot(parent))
		{
			return parent;
		}
		
		return current;
	}
	
	private static boolean isDistributionRoot(Path path)
	{
		return Files.isDirectory(path.resolve("login")) && Files.isDirectory(path.resolve("game")) && Files.isDirectory(path.resolve("libs"));
	}
}
