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

import java.awt.Component;

import javax.swing.JOptionPane;

import org.l2jmobius.tools.dashboard.runtime.ServicePhase;
import org.l2jmobius.tools.dashboard.runtime.ServiceSupervisor;
import org.l2jmobius.tools.dashboard.runtime.ServiceSupervisor.LifecycleCountdown;

/**
 * Collects a lifecycle delay in seconds and exposes cancellation for an existing scheduled action.
 * @author BazookaRpm
 */
public final class ServiceLifecycleDialog
{
	private ServiceLifecycleDialog()
	{
	}
	
	public static void show(Component parent, ServiceSupervisor service, boolean restart)
	{
		final LifecycleCountdown countdown = service.getLifecycleCountdown();
		if (countdown != null)
		{
			final boolean scheduledRestart = countdown.isRestart();
			if (JOptionPane.showConfirmDialog(parent, service.getRole().label() + " has a " + (scheduledRestart ? "restart" : "shutdown") + " scheduled in " + countdown.getSecondsRemaining() + " second(s).\nCancel the scheduled action?", scheduledRestart ? "Scheduled Restart" : "Scheduled Shutdown", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)
			{
				service.cancelScheduledLifecycleActionAsync();
			}
			return;
		}
		
		// Nothing to delay: an offline restart starts the service and a stop during shutdown resends the request.
		if (!service.isRunning() || (service.getPhase() == ServicePhase.STOPPING))
		{
			service.scheduleLifecycleActionAsync(restart, 0);
			return;
		}
		
		final String title = (restart ? "Restart " : "Stop ") + service.getRole().label();
		while (true)
		{
			// The default keeps Stop and Restart immediate, as they were before the delay prompt existed.
			final String input = (String) JOptionPane.showInputDialog(parent, "Enter the delay in seconds.\n0 = immediate.", title, JOptionPane.QUESTION_MESSAGE, null, null, "0");
			if (input == null)
			{
				return;
			}
			
			try
			{
				final int seconds = Integer.parseInt(input.trim());
				if (seconds < 0)
				{
					throw new NumberFormatException();
				}
				
				service.scheduleLifecycleActionAsync(restart, seconds);
				return;
			}
			catch (NumberFormatException e)
			{
				JOptionPane.showMessageDialog(parent, "Enter a whole number of seconds greater than or equal to 0.", "Invalid Delay", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
}
