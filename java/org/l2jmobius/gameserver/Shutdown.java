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
package org.l2jmobius.gameserver;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.config.DatabaseConfig;
import org.l2jmobius.commons.database.DatabaseBackup;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.config.WorldExchangeConfig;
import org.l2jmobius.gameserver.config.custom.OfflinePlayConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.data.BotReportTable;
import org.l2jmobius.gameserver.data.SchemeBufferTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.sql.OfflinePlayTable;
import org.l2jmobius.gameserver.data.sql.OfflineTraderTable;
import org.l2jmobius.gameserver.data.sql.PartyMatchingHistoryTable;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.AbstractDailyMissionHandler;
import org.l2jmobius.gameserver.managers.CastleManorManager;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.managers.DatabaseSpawnManager;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.managers.ItemAuctionManager;
import org.l2jmobius.gameserver.managers.ItemsOnGroundManager;
import org.l2jmobius.gameserver.managers.PrecautionaryRestartManager;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.managers.WorldExchangeManager;
import org.l2jmobius.gameserver.mechanics.olympiad.Hero;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.loginserverpackets.send.ServerStatus;
import org.l2jmobius.gameserver.network.serverpackets.ServerClose;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

/**
 * This class provides the functions for shutting down and restarting the server.<br>
 * It closes all open client connections and saves all data.
 */
public class Shutdown extends Thread
{
	private static final Logger LOGGER = Logger.getLogger(Shutdown.class.getName());
	
	private static final int SIGTERM = 0;
	private static final int GM_SHUTDOWN = 1;
	private static final int GM_RESTART = 2;
	private static final int ABORT = 3;
	private static final String[] MODE_TEXT =
	{
		"SIGTERM",
		"shutting down",
		"restarting",
		"aborting"
	};
	
	private static volatile Shutdown _counterInstance;
	private static volatile boolean _countdownFinished;
	
	private static volatile int _secondsShut;
	private static volatile int _shutdownMode;
	
	/**
	 * Sends a broadcast message to all online players.<br>
	 * This message informs them that the server is shutting down.<br>
	 * It includes the remaining time in seconds.
	 * @param seconds The number of seconds until the server shuts down.
	 */
	private void sendServerQuit(int seconds)
	{
		final SystemMessage sysm = new SystemMessage(SystemMessageId.THE_SERVER_WILL_BE_SHUT_DOWN_IN_S1_SEC_PLEASE_FIND_A_SAFE_PLACE_TO_LOG_OUT);
		sysm.addInt(seconds);
		World.broadcastToAllOnlinePlayers(sysm);
	}
	
	/**
	 * Initializes the {@code Shutdown} instance with default values.<br>
	 * Sets the shutdown mode to {@code SIGTERM}.<br>
	 * Sets the countdown timer to {@code -1}.
	 */
	protected Shutdown()
	{
		_secondsShut = -1;
		_shutdownMode = SIGTERM;
	}
	
	/**
	 * Initializes the shutdown process with a specific delay and mode.<br>
	 * This method sets the countdown time and determines if the server will restart.
	 * @param seconds The number of seconds to wait before shutting down.
	 * @param restart Set to {@code true} to restart the server, or {@code false} to shut it down completely.
	 */
	public Shutdown(int seconds, boolean restart)
	{
		_secondsShut = Math.max(0, seconds);
		_shutdownMode = restart ? GM_RESTART : GM_SHUTDOWN;
	}
	
	@Override
	public void run()
	{
		if (this == getInstance())
		{
			startShutdownActions();
			return;
		}
		
		if (_countdownFinished)
		{
			return;
		}
		
		// Send warnings and then call exit to start shutdown sequence.
		countdown();
		
		final int mode;
		synchronized (Shutdown.class)
		{
			// A newer countdown replaced this one and carries on with the shutdown.
			if (_counterInstance != this)
			{
				return;
			}
			
			// From here on an abort finds no pending countdown.
			_counterInstance = null;
			mode = _shutdownMode;
		}
		
		// Last point where logging is operational.
		LOGGER.warning("GM shutdown countdown is over. " + MODE_TEXT[mode] + " NOW!");
		
		switch (mode)
		{
			case GM_SHUTDOWN:
			{
				getInstance().setMode(GM_SHUTDOWN);
				startShutdownActions();
				System.exit(0);
				break;
			}
			case GM_RESTART:
			{
				getInstance().setMode(GM_RESTART);
				startShutdownActions();
				System.exit(2);
				break;
			}
			case ABORT:
			{
				LoginServerThread.getInstance().setServerStatus(ServerStatus.STATUS_AUTO);
				break;
			}
			default:
			{
				break;
			}
		}
	}
	
	/**
	 * Initiates the server shutdown or restart process.<br>
	 * This method schedules the action based on the provided time and type.<br>
	 * It logs the event if a {@code Player} is provided.
	 * @param player The {@code Player} who issued the command, or {@code null}.
	 * @param seconds The number of seconds to wait before starting the action.
	 * @param restart A boolean indicating if the server should restart or shut down completely.
	 */
	public void startShutdown(Player player, int seconds, boolean restart)
	{
		// The shared mode is only written under the class lock, where the new counter sets it.
		final int mode = restart ? GM_RESTART : GM_SHUTDOWN;
		
		if (player != null)
		{
			LOGGER.warning("GM: " + player.getName() + "(" + player.getObjectId() + ") issued shutdown command. " + MODE_TEXT[mode] + " in " + seconds + " seconds!");
		}
		else
		{
			LOGGER.warning("Server scheduled restart issued shutdown command. " + (restart ? "Restart" : "Shutdown") + " in " + seconds + " seconds!");
		}
		
		if (mode > 0)
		{
			switch (seconds)
			{
				case 540:
				case 480:
				case 420:
				case 360:
				case 300:
				case 240:
				case 180:
				case 120:
				case 60:
				case 30:
				case 10:
				case 5:
				case 4:
				case 3:
				case 2:
				case 1:
				{
					break;
				}
				default:
				{
					sendServerQuit(seconds);
					break;
				}
			}
		}
		
		synchronized (Shutdown.class)
		{
			if (_counterInstance != null)
			{
				_counterInstance.abort();
			}
			
			if (ServerConfig.PRECAUTIONARY_RESTART_ENABLED)
			{
				PrecautionaryRestartManager.getInstance().restartEnabled();
			}
			
			// The main instance should only run for shutdown hook, so we start a new instance.
			_counterInstance = new Shutdown(seconds, restart);
			_counterInstance.start();
		}
	}
	
	/**
	 * Cancels the current server shutdown process.<br>
	 * This method stops the countdown and resumes normal operations.<br>
	 * It will fail if the countdown has already finished.
	 * @param player The {@code Player} who issued the abort command.
	 * @return {@code true} if a pending countdown was stopped, {@code false} if none was pending or it had already finished.
	 */
	public boolean abort(Player player)
	{
		final String modeText;
		synchronized (Shutdown.class)
		{
			if (_countdownFinished)
			{
				LOGGER.warning("GM: " + (player != null ? player.getName() + "(" + player.getObjectId() + ") " : "") + "shutdown ABORT failed because countdown has finished.");
				return false;
			}
			
			if ((_counterInstance == null) || (_shutdownMode == ABORT))
			{
				return false;
			}
			
			modeText = MODE_TEXT[_shutdownMode]; // Capture before abort() overwrites the mode to ABORT.
			_counterInstance.abort();
		}
		
		LOGGER.warning("GM: " + (player != null ? player.getName() + "(" + player.getObjectId() + ") " : "") + "issued shutdown ABORT. " + modeText + " has been stopped!");
		
		if (ServerConfig.PRECAUTIONARY_RESTART_ENABLED)
		{
			PrecautionaryRestartManager.getInstance().restartAborted();
		}
		
		World.broadcastToAllOnlinePlayers("Server aborts " + modeText + " and continues normal operation!", false);
		return true;
	}
	
	/**
	 * Updates the current shutdown mode.<br>
	 * This method sets the internal {@code _shutdownMode} variable.
	 * @param mode The new mode to set for the shutdown process.
	 */
	private void setMode(int mode)
	{
		_shutdownMode = mode;
	}
	
	/**
	 * Immediately stops the current shutdown process.<br>
	 * This method sets the {@code _shutdownMode} to {@code ABORT}.<br>
	 * It is used to cancel any pending shutdown actions.
	 */
	private void abort()
	{
		_shutdownMode = ABORT;
	}
	
	/**
	 * Manages the server shutdown countdown process.<br>
	 * It sends notifications to players at specific intervals.<br>
	 * It updates the {@code ServerStatus} to prevent new logins when time is low.<br>
	 * This method runs until {@code _secondsShut} reaches 0.
	 */
	private void countdown()
	{
		try
		{
			while (true)
			{
				synchronized (Shutdown.class)
				{
					// A newer countdown replaced this one; the static mode it resets can no longer signal the abort.
					if ((_counterInstance != this) || (_secondsShut <= 0))
					{
						break;
					}
					
					// Rehabilitate previous server status if shutdown is aborted.
					if (_shutdownMode == ABORT)
					{
						if (LoginServerThread.getInstance().getServerStatus() == ServerStatus.STATUS_DOWN)
						{
							LoginServerThread.getInstance().setServerStatus((GeneralConfig.SERVER_GMONLY) ? ServerStatus.STATUS_GM_ONLY : ServerStatus.STATUS_AUTO);
						}
						break;
					}
					
					switch (_secondsShut)
					{
						case 540:
						case 480:
						case 420:
						case 360:
						case 300:
						case 240:
						case 180:
						case 120:
						case 60:
						case 30:
						case 10:
						case 5:
						case 4:
						case 3:
						case 2:
						case 1:
						{
							sendServerQuit(_secondsShut);
							break;
						}
						default:
						{
							break;
						}
					}
					
					// Prevent players from logging in.
					if ((_secondsShut <= 60) && (LoginServerThread.getInstance().getServerStatus() != ServerStatus.STATUS_DOWN))
					{
						LoginServerThread.getInstance().setServerStatus(ServerStatus.STATUS_DOWN);
					}
					
					_secondsShut--;
				}
				
				Thread.sleep(1000);
			}
		}
		catch (Exception e)
		{
			// This will never happen.
		}
	}
	
	/**
	 * Executes the final sequence of actions to shut down the server.<br>
	 * This method saves offline data and disconnects all players.<br>
	 * It also stops thread pools, interrupts tasks, and closes database connections.<br>
	 * Finally, it performs a database backup if configured.
	 */
	private void startShutdownActions()
	{
		if (_countdownFinished)
		{
			return;
		}
		
		_countdownFinished = true;
		
		final TimeCounter tc = new TimeCounter();
		final TimeCounter tc1 = new TimeCounter();
		
		try
		{
			if ((OfflineTradeConfig.OFFLINE_TRADE_ENABLE || OfflineTradeConfig.OFFLINE_CRAFT_ENABLE) && OfflineTradeConfig.RESTORE_OFFLINERS && !OfflineTradeConfig.STORE_OFFLINE_TRADE_IN_REALTIME)
			{
				OfflineTraderTable.getInstance().storeOffliners();
				LOGGER.info("Offline Traders Table: Offline shops stored(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
			}
		}
		catch (Throwable t)
		{
			LOGGER.log(Level.WARNING, "Error saving offline shops.", t);
		}
		
		try
		{
			if (OfflinePlayConfig.RESTORE_AUTO_PLAY_OFFLINERS && GeneralConfig.ENABLE_AUTO_ASSIST)
			{
				OfflinePlayTable.getInstance().storeOfflineGroups();
				LOGGER.info("Offline Play Table: Offline play groups stored(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
			}
		}
		catch (Throwable t)
		{
			LOGGER.log(Level.WARNING, "Error saving offline play groups.", t);
		}
		
		try
		{
			disconnectAllCharacters();
			LOGGER.info("All players disconnected and saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		}
		catch (Throwable t)
		{
			// ignore
		}
		
		// ensure all services are stopped
		
		try
		{
			GameTimeTaskManager.getInstance().interrupt();
			LOGGER.info("Game Time Task Manager: Thread interruped(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		}
		catch (Throwable t)
		{
			// ignore
		}
		
		// stop all thread pools
		try
		{
			ThreadPool.shutdown();
			LOGGER.info("Thread Pool Manager: Manager has been shut down(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		}
		catch (Throwable t)
		{
			// ignore
		}
		
		try
		{
			LoginServerThread.getInstance().interrupt();
			LOGGER.info("Login Server Thread: Thread interruped(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		}
		catch (Throwable t)
		{
			// ignore
		}
		
		// last byebye, save all data and quit this server
		saveData();
		tc.restartCounter();
		
		// commit data, last chance
		try
		{
			DatabaseFactory.close();
			LOGGER.info("Database Factory: Database connection has been shut down(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		}
		catch (Throwable t)
		{
			// ignore
		}
		
		// Backup database.
		if (DatabaseConfig.BACKUP_DATABASE)
		{
			DatabaseBackup.performBackup("game");
		}
		
		LOGGER.info("The server has been successfully shut down in " + (tc1.getEstimatedTime() / 1000) + " seconds.");
	}
	
	/**
	 * Saves all persistent game data to the database during shutdown.<br>
	 * This method handles various managers including bosses, clans, and global variables.<br>
	 * It also processes items on the ground and bot reports based on configuration settings.
	 */
	private void saveData()
	{
		switch (_shutdownMode)
		{
			case SIGTERM:
			{
				LOGGER.info("SIGTERM received. Shutting down NOW!");
				break;
			}
			case GM_SHUTDOWN:
			{
				LOGGER.info("GM shutdown received. Shutting down NOW!");
				break;
			}
			case GM_RESTART:
			{
				LOGGER.info("GM restart received. Restarting NOW!");
				break;
			}
		}
		
		final TimeCounter tc = new TimeCounter();
		
		// Save all raidboss and GrandBoss status ^_^.
		DatabaseSpawnManager.getInstance().cleanUp();
		LOGGER.info("RaidBossSpawnManager: All raidboss info saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		GrandBossManager.getInstance().cleanUp();
		LOGGER.info("GrandBossManager: All Grand Boss info saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		ItemAuctionManager.getInstance().shutdown();
		LOGGER.info("Item Auction Manager: All tasks stopped(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		Olympiad.getInstance().saveOlympiadStatus();
		LOGGER.info("Olympiad System: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		
		Hero.getInstance().shutdown();
		LOGGER.info("Hero System: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		ClanTable.getInstance().shutdown();
		LOGGER.info("Clan System: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		
		// Save queued daily mission progress before closing.
		AbstractDailyMissionHandler.flushPendingEntries();
		LOGGER.info("Daily Mission Handler: Queued entries saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		
		// Save Cursed Weapons data before closing..
		CursedWeaponsManager.getInstance().saveData();
		LOGGER.info("Cursed Weapons Manager: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		
		// Save all manor data..
		if (!GeneralConfig.ALT_MANOR_SAVE_ALL_ACTIONS)
		{
			CastleManorManager.getInstance().storeMe();
			LOGGER.info("Castle Manor Manager: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		}
		
		// Save all global (non-player specific) Quest data that needs to persist after reboot..
		ScriptManager.getInstance().save();
		LOGGER.info("Script Manager: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		
		// Save all global variables data..
		GlobalVariablesManager.getInstance().storeMe();
		LOGGER.info("Global Variables Manager: Variables saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		
		// Save all party matching history data..
		PartyMatchingHistoryTable.getInstance().storeMe();
		LOGGER.info("Party Matching History Table: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		
		// Save World Exchange..
		if (WorldExchangeConfig.ENABLE_WORLD_EXCHANGE)
		{
			WorldExchangeManager.getInstance().storeMe();
			LOGGER.info("World Exchange Manager: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		}
		
		// Schemes save..
		SchemeBufferTable.getInstance().saveSchemes();
		LOGGER.info("SchemeBufferTable: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		
		// Save items on ground before closing..
		if (GeneralConfig.SAVE_DROPPED_ITEM)
		{
			ItemsOnGroundManager.getInstance().saveInDb();
			LOGGER.info("Items On Ground Manager: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
			ItemsOnGroundManager.getInstance().cleanUp();
			LOGGER.info("Items On Ground Manager: Cleaned up(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		}
		
		// Save bot reports to database..
		if (GeneralConfig.BOTREPORT_ENABLE)
		{
			BotReportTable.getInstance().saveReportedCharData();
			LOGGER.info("Bot Report Table: Successfully saved reports to database!");
		}
		
		try
		{
			Thread.sleep(5000);
		}
		catch (Exception e)
		{
			// This will never happen.
		}
	}
	
	/**
	 * Iterates through all active players in the {@code World}.<br>
	 * Disconnects each {@code Player} from the server.<br>
	 * Sends a {@code ServerClose.STATIC_PACKET} during the disconnection process.
	 */
	private void disconnectAllCharacters()
	{
		for (Player player : World.getPlayers())
		{
			Disconnection.of(player).storeAndDeleteWith(ServerClose.STATIC_PACKET);
		}
	}
	
	/**
	 * A simple class used to track down the estimated time of method executions.<br>
	 * Once this class is created, it saves the start time, and when you want to get the estimated time, use the getEstimatedTime() method.
	 */
	private static class TimeCounter
	{
		private long _startTime;
		
		protected TimeCounter()
		{
			restartCounter();
		}
		
		public void restartCounter()
		{
			_startTime = System.currentTimeMillis();
		}
		
		public long getEstimatedTimeAndRestartCounter()
		{
			final long toReturn = System.currentTimeMillis() - _startTime;
			restartCounter();
			return toReturn;
		}
		
		public long getEstimatedTime()
		{
			return System.currentTimeMillis() - _startTime;
		}
	}
	
	/**
	 * Retrieves the singleton instance of the {@code Shutdown} class.<br>
	 * This method provides a global access point to manage server shutdown procedures.
	 * @return The singleton instance of {@code Shutdown}.
	 */
	public static Shutdown getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final Shutdown INSTANCE = new Shutdown();
	}
}
