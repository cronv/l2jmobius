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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;

/**
 * Manages the premium status and related features for players.<br>
 * It handles the registration, verification, and expiration of premium accounts.<br>
 * This class ensures that premium benefits are correctly applied to {@code Player} entities.
 * @author Mobius
 */
public class PremiumManager
{
	private static final Logger LOGGER = Logger.getLogger(PremiumManager.class.getName());
	
	private static final String LOAD_SQL = "SELECT account_name,enddate FROM account_premium WHERE account_name = ?";
	private static final String UPDATE_SQL = "REPLACE INTO account_premium (account_name,enddate) VALUE (?,?)";
	private static final String DELETE_SQL = "DELETE FROM account_premium WHERE account_name = ?";
	
	class PremiumExpireTask implements Runnable
	{
		final Player _player;
		
		PremiumExpireTask(Player player)
		{
			_player = player;
		}
		
		@Override
		public void run()
		{
			_player.setPremiumStatus(false);
		}
	}
	
	// Data Cache
	private final Map<String, Long> _premiumData = new ConcurrentHashMap<>();
	
	// expireTasks
	private final Map<String, ScheduledFuture<?>> _expiretasks = new ConcurrentHashMap<>();
	
	// Listeners
	private final ListenersContainer _listenerContainer = Containers.Players();
	
	private final Consumer<OnPlayerLogin> _playerLoginEvent = event ->
	{
		final Player player = event.getPlayer();
		final String accountName = PremiumSystemConfig.ACCOUNT_WIDE_PREMIUM ? player.getAccountName() : player.getName();
		loadPremiumData(accountName);
		final long now = System.currentTimeMillis();
		final long premiumExpiration = getPremiumExpiration(accountName);
		player.setPremiumStatus(premiumExpiration > now);
		if (player.hasPremiumStatus())
		{
			startExpireTask(player, premiumExpiration - now);
		}
		else if (premiumExpiration > 0)
		{
			removePremiumStatus(accountName, false);
		}
	};
	
	private final Consumer<OnPlayerLogout> _playerLogoutEvent = event ->
	{
		stopExpireTask(event.getPlayer());
	};
	
	/**
	 * Initializes the {@code PremiumManager} with necessary event listeners.<br>
	 * It registers listeners for both login and logout events.
	 */
	protected PremiumManager()
	{
		_listenerContainer.addListener(new ConsumerEventListener(_listenerContainer, EventType.ON_PLAYER_LOGIN, _playerLoginEvent, this));
		_listenerContainer.addListener(new ConsumerEventListener(_listenerContainer, EventType.ON_PLAYER_LOGOUT, _playerLogoutEvent, this));
	}
	
	/**
	 * Schedules a task to handle premium expiration for a specific player.<br>
	 * This method uses the {@code ThreadPool} to run the task after a set delay.<br>
	 * It stores the scheduled future in the internal task map.
	 * @param player The {@code Player} object for whom the task is being scheduled.
	 * @param delay The amount of time to wait before executing the task in milliseconds.
	 */
	private void startExpireTask(Player player, long delay)
	{
		_expiretasks.put(PremiumSystemConfig.ACCOUNT_WIDE_PREMIUM ? player.getAccountName().toLowerCase() : player.getName().toLowerCase(), ThreadPool.schedule(new PremiumExpireTask(player), delay));
	}
	
	/**
	 * Stops the scheduled expiration task for a specific {@code Player}.<br>
	 * This method removes the task from the internal tracking map.<br>
	 * If the task exists, it is cancelled without interrupting it.
	 * @param player The {@code Player} whose expiration task should be stopped.
	 */
	private void stopExpireTask(Player player)
	{
		final ScheduledFuture<?> task = _expiretasks.remove(PremiumSystemConfig.ACCOUNT_WIDE_PREMIUM ? player.getAccountName().toLowerCase() : player.getName().toLowerCase());
		if (task != null)
		{
			task.cancel(false);
		}
	}
	
	/**
	 * Loads the premium data for a specific account from the database.<br>
	 * It updates the internal cache with the expiration date.<br>
	 * If an error occurs, it logs a warning to the logger.
	 * @param accountName The name of the account to load.
	 */
	public void loadPremiumData(String accountName)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement stmt = con.prepareStatement(LOAD_SQL))
		{
			stmt.setString(1, accountName.toLowerCase());
			try (ResultSet rset = stmt.executeQuery())
			{
				while (rset.next())
				{
					_premiumData.put(rset.getString(1).toLowerCase(), rset.getLong(2));
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning("Problem with PremiumManager: " + e.getMessage());
		}
	}
	
	/**
	 * Retrieves the expiration timestamp for a specific account.<br>
	 * It looks up the data using the provided {@code accountName}.<br>
	 * If no premium data exists, it returns {@code 0L}.
	 * @param accountName The name of the account to check.
	 * @return The expiration time as a {@code long} value.
	 */
	public long getPremiumExpiration(String accountName)
	{
		return _premiumData.getOrDefault(accountName.toLowerCase(), 0L);
	}
	
	/**
	 * Adds premium time to a specific account.<br>
	 * This method updates the database and the internal cache.<br>
	 * It also refreshes the status for any online players.
	 * @param accountName The name of the account to update.
	 * @param timeValue The amount of time to add.
	 * @param timeUnit The unit of measurement for the time value.
	 */
	public void addPremiumTime(String accountName, int timeValue, TimeUnit timeUnit)
	{
		final long addTime = timeUnit.toMillis(timeValue);
		final long now = System.currentTimeMillis();
		
		// New premium task at least from now.
		final long oldPremiumExpiration = Math.max(now, getPremiumExpiration(accountName));
		final long newPremiumExpiration = oldPremiumExpiration + addTime;
		
		// UPDATE DATABASE
		final String lowerAccountName = accountName.toLowerCase();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement stmt = con.prepareStatement(UPDATE_SQL))
		{
			stmt.setString(1, lowerAccountName);
			stmt.setLong(2, newPremiumExpiration);
			stmt.execute();
		}
		catch (SQLException e)
		{
			LOGGER.warning("Problem with PremiumManager: " + e.getMessage());
		}
		
		// UPDATE CACHE
		_premiumData.put(lowerAccountName, newPremiumExpiration);
		
		// UPDATE PlAYER PREMIUMSTATUS
		for (Player player : World.getPlayers())
		{
			if (accountName.equalsIgnoreCase(player.getAccountName()))
			{
				stopExpireTask(player);
				startExpireTask(player, newPremiumExpiration - now);
				if (!player.hasPremiumStatus())
				{
					player.setPremiumStatus(true);
				}
				break;
			}
		}
	}
	
	/**
	 * Removes the premium status from a specific account.<br>
	 * This method updates both the database and the internal cache.<br>
	 * It can also update the live status of an online player.
	 * @param accountName The name of the account to modify.
	 * @param checkOnline Set to {@code true} to update the status for currently logged-in players.
	 */
	public void removePremiumStatus(String accountName, boolean checkOnline)
	{
		if (checkOnline)
		{
			for (Player player : World.getPlayers())
			{
				if (accountName.equalsIgnoreCase(player.getAccountName()) && player.hasPremiumStatus())
				{
					player.setPremiumStatus(false);
					stopExpireTask(player);
					break;
				}
			}
		}
		
		// UPDATE CACHE
		final String lowerAccountName = accountName.toLowerCase();
		_premiumData.remove(lowerAccountName);
		
		// UPDATE DATABASE
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement stmt = con.prepareStatement(DELETE_SQL))
		{
			stmt.setString(1, lowerAccountName);
			stmt.execute();
		}
		catch (SQLException e)
		{
			LOGGER.warning("Problem with PremiumManager: " + e.getMessage());
		}
	}
	
	/**
	 * Retrieves the singleton instance of the {@code PremiumManager}.<br>
	 * Use this method to access the manager from anywhere in your code.
	 * @return The single shared instance of {@code PremiumManager}.
	 */
	public static PremiumManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PremiumManager INSTANCE = new PremiumManager();
	}
}
