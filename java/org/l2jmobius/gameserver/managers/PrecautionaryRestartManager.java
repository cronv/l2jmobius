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
package org.l2jmobius.gameserver.managers;

import java.lang.management.ManagementFactory;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.Shutdown;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.entity.actor.instance.RaidBoss;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;

/**
 * Manages the server's behavior during a planned restart.<br>
 * It ensures that active game events and entities are handled safely before the {@code Shutdown} occurs.
 * @author Mobius
 */
public class PrecautionaryRestartManager
{
	private static final Logger LOGGER = Logger.getLogger(PrecautionaryRestartManager.class.getName());
	
	private static final String SYSTEM_CPU_LOAD_VAR = "SystemCpuLoad";
	private static final String PROCESS_CPU_LOAD_VAR = "ProcessCpuLoad";
	
	private static volatile boolean _restarting = false;
	
	/**
	 * Initializes the {@code PrecautionaryRestartManager}.<br>
	 * This constructor sets up a scheduled task to monitor system resources.<br>
	 * It checks for high CPU or memory usage to trigger a safe server restart.
	 */
	protected PrecautionaryRestartManager()
	{
		ThreadPool.scheduleAtFixedRate(() ->
		{
			if (_restarting)
			{
				return;
			}
			
			if (ServerConfig.PRECAUTIONARY_RESTART_CPU && (getCpuLoad(SYSTEM_CPU_LOAD_VAR) > ServerConfig.PRECAUTIONARY_RESTART_PERCENTAGE))
			{
				if (serverBizzy())
				{
					return;
				}
				
				LOGGER.info("PrecautionaryRestartManager: CPU usage over " + ServerConfig.PRECAUTIONARY_RESTART_PERCENTAGE + "%.");
				LOGGER.info("PrecautionaryRestartManager: Server is using " + getCpuLoad(PROCESS_CPU_LOAD_VAR) + "%.");
				World.broadcastToAllOnlinePlayers("Server will restart in 10 minutes.", false);
				Shutdown.getInstance().startShutdown(null, 600, true);
			}
			
			if (ServerConfig.PRECAUTIONARY_RESTART_MEMORY && (getProcessRamLoad() > ServerConfig.PRECAUTIONARY_RESTART_PERCENTAGE))
			{
				if (serverBizzy())
				{
					return;
				}
				
				LOGGER.info("PrecautionaryRestartManager: Memory usage over " + ServerConfig.PRECAUTIONARY_RESTART_PERCENTAGE + "%.");
				World.broadcastToAllOnlinePlayers("Server will restart in 10 minutes.", false);
				Shutdown.getInstance().startShutdown(null, 600, true);
			}
		}, ServerConfig.PRECAUTIONARY_RESTART_DELAY, ServerConfig.PRECAUTIONARY_RESTART_DELAY);
	}
	
	/**
	 * Retrieves the CPU load from the operating system.<br>
	 * It uses the provided variable name to fetch the specific metric.<br>
	 * If the value is unavailable or an error occurs, it returns {@code 0}.
	 * @param var The name of the attribute to retrieve.
	 * @return The calculated CPU load as a {@code double}.
	 */
	private static double getCpuLoad(String var)
	{
		try
		{
			final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			final ObjectName name = ObjectName.getInstance("java.lang:type=OperatingSystem");
			final AttributeList list = mbs.getAttributes(name, new String[]
			{
				var
			});
			
			if (list.isEmpty())
			{
				return 0;
			}
			
			final Attribute att = (Attribute) list.get(0);
			final Double value = (Double) att.getValue();
			if (value == -1)
			{
				return 0;
			}
			
			return (value * 1000) / 10d;
		}
		catch (Exception e)
		{
		}
		
		return 0;
	}
	
	/**
	 * Calculates the current memory usage of the Java process.<br>
	 * It returns the percentage of memory used relative to the maximum available memory.
	 * @return The memory load as a {@code double} value between 0.0 and 100.0.
	 */
	private static double getProcessRamLoad()
	{
		final Runtime runTime = Runtime.getRuntime();
		final long totalMemory = runTime.maxMemory();
		final long usedMemory = totalMemory - ((totalMemory - runTime.totalMemory()) + runTime.freeMemory());
		return (usedMemory * 100) / totalMemory;
	}
	
	/**
	 * Checks if the server is currently busy with important activities.<br>
	 * It looks for active sieges, olympiads, events, or boss fights.
	 * @return {@code true} if any major activity is in progress, {@code false} otherwise.
	 */
	private boolean serverBizzy()
	{
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			if ((castle != null) && castle.getSiege().isInProgress())
			{
				return true;
			}
		}
		
		for (Fort fort : FortManager.getInstance().getForts())
		{
			if ((fort != null) && fort.getSiege().isInProgress())
			{
				return true;
			}
		}
		
		for (Player player : World.getPlayers())
		{
			if ((player == null) || player.getOfflineMode().isEnabled())
			{
				continue;
			}
			
			if (player.isInOlympiadMode())
			{
				return true;
			}
			
			if (player.isOnEvent())
			{
				return true;
			}
			
			if (player.isInInstance())
			{
				return true;
			}
			
			final WorldObject target = player.getTarget();
			if ((target instanceof RaidBoss) || (target instanceof GrandBoss))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Enables the precautionary restart process.<br>
	 * This method sets the {@code _restarting} flag to {@code true}.<br>
	 * It signals that the server should begin its shutdown sequence.
	 */
	public void restartEnabled()
	{
		_restarting = true;
	}
	
	/**
	 * Cancels the current precautionary restart process.<br>
	 * This method sets the {@code _restarting} flag to {@code false}.<br>
	 * Use this when a restart should no longer occur.
	 */
	public void restartAborted()
	{
		_restarting = false;
	}
	
	/**
	 * Retrieves the singleton instance of the {@code PrecautionaryRestartManager}.<br>
	 * Use this method to access the manager from anywhere in your code.
	 * @return The active {@code PrecautionaryRestartManager} instance.
	 */
	public static PrecautionaryRestartManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PrecautionaryRestartManager INSTANCE = new PrecautionaryRestartManager();
	}
}
