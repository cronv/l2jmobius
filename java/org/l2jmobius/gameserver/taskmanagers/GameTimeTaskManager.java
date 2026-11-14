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
package org.l2jmobius.gameserver.taskmanagers;

import java.util.Calendar;

import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.OnDayNightChange;

/**
 * Manages the progression of in-game time.<br>
 * This {@code GameTimeTaskManager} handles periodic updates to the game world's clock.
 * @author Forsaiken, Mobius
 */
public class GameTimeTaskManager extends Thread
{
	public static final int TICKS_PER_SECOND = 10; // Not able to change this without checking through code.
	public static final int MILLIS_IN_TICK = 1000 / TICKS_PER_SECOND;
	public static final int IG_DAYS_PER_DAY = 6;
	public static final int MILLIS_PER_IG_DAY = (3600000 * 24) / IG_DAYS_PER_DAY;
	public static final int SECONDS_PER_IG_DAY = MILLIS_PER_IG_DAY / 1000;
	public static final int TICKS_PER_IG_DAY = SECONDS_PER_IG_DAY * TICKS_PER_SECOND;
	
	private final long _referenceTime;
	private boolean _isNight;
	private int _gameTicks;
	private int _gameTime;
	private int _gameHour;
	
	/**
	 * Initializes the {@code GameTimeTaskManager}.<br>
	 * Sets up the thread as a daemon with high priority.<br>
	 * Establishes the initial reference time for game calculations.<br>
	 * Starts the background execution of the task manager.
	 */
	protected GameTimeTaskManager()
	{
		super("GameTimeTaskManager");
		super.setDaemon(true);
		super.setPriority(MAX_PRIORITY);
		
		final Calendar c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		_referenceTime = c.getTimeInMillis();
		
		super.start();
	}
	
	@Override
	public void run()
	{
		while (true)
		{
			_gameTicks = (int) ((System.currentTimeMillis() - _referenceTime) / MILLIS_IN_TICK);
			_gameTime = (_gameTicks % TICKS_PER_IG_DAY) / MILLIS_IN_TICK;
			_gameHour = _gameTime / 60;
			
			if ((_gameHour < 6) != _isNight)
			{
				_isNight = !_isNight;
				
				if (EventDispatcher.getInstance().hasListener(EventType.ON_DAY_NIGHT_CHANGE))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnDayNightChange(_isNight));
				}
			}
			
			try
			{
				Thread.sleep(MILLIS_IN_TICK);
			}
			catch (InterruptedException e)
			{
				// Ignore.
			}
		}
	}
	
	/**
	 * Checks if the current game time is set to night.<br>
	 * This method returns the state of the {@code _isNight} variable.
	 * @return {@code true} if it is currently night, or {@code false} otherwise.
	 */
	public boolean isNight()
	{
		return _isNight;
	}
	
	/**
	 * Retrieves the current number of game ticks.<br>
	 * This value represents the total elapsed time in ticks since the server started.
	 * @return The current count of {@code _gameTicks}.
	 */
	public int getGameTicks()
	{
		return _gameTicks;
	}
	
	/**
	 * Retrieves the current time in the game world.<br>
	 * This value represents the total elapsed time since the start of the game day.
	 * @return The current {@code int} value for the game time.
	 */
	public int getGameTime()
	{
		return _gameTime;
	}
	
	/**
	 * Retrieves the current hour of the game world.<br>
	 * This value is based on the internal game time clock.
	 * @return The current game hour as an {@code int}.
	 */
	public int getGameHour()
	{
		return _gameHour;
	}
	
	/**
	 * Returns the current minute of the game time.<br>
	 * This value is calculated based on the total {@code _gameTime}.
	 * @return The current game minute as an {@code int}.
	 */
	public int getGameMinute()
	{
		return _gameTime % 60;
	}
	
	/**
	 * Retrieves the singleton instance of the {@code GameTimeTaskManager}.<br>
	 * Use this method to access the global game time manager.
	 * @return The single instance of {@code GameTimeTaskManager}.
	 */
	public static GameTimeTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final GameTimeTaskManager INSTANCE = new GameTimeTaskManager();
	}
}
