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
package org.l2jmobius.gameserver.entity.actor.holders.player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.tasks.player.RecoGiveTask;

/**
 * Manages the recommendation state of a {@code Player}.<br>
 * It tracks how many recommendations the player has received and can still give, and drives the periodic task that refills the giveable amount.
 * @author Mobius
 */
public class Recommendations
{
	private static final Logger LOGGER = Logger.getLogger(Recommendations.class.getName());
	
	private final Player _player;
	private int _have; // How much I was recommended by others.
	private int _left; // How many recommendations I can give to others.
	private ScheduledFuture<?> _giveTask;
	private boolean _twoHoursGiven = false;
	
	/**
	 * Creates a new {@code Recommendations} for a specific player.
	 * @param player The {@code Player} object associated with this instance.
	 */
	public Recommendations(Player player)
	{
		_player = player;
	}
	
	/**
	 * Retrieves the current recommendation count.
	 * @return The number of recommendations held.
	 */
	public int getHave()
	{
		return _have;
	}
	
	/**
	 * Increments the recommendation count.<br>
	 * The value is capped at a maximum of {@code 255}.
	 */
	protected void increaseHave()
	{
		if (_have < 255)
		{
			_have++;
		}
	}
	
	/**
	 * Sets the recommendation value for the actor.<br>
	 * The input {@code value} is clamped between {@code 0} and {@code 255}.
	 * @param value The new recommendation value to set.
	 */
	public void setHave(int value)
	{
		_have = Math.clamp(value, 0, 255);
	}
	
	/**
	 * Retrieves the current amount of left recommendations.
	 * @return The number of remaining recommendations as an {@code int}.
	 */
	public int getLeft()
	{
		return _left;
	}
	
	/**
	 * Sets the left recommendation value.<br>
	 * The input is clamped between {@code 0} and {@code 255}.
	 * @param value The new recommendation value to set.
	 */
	public void setLeft(int value)
	{
		_left = Math.clamp(value, 0, 255);
	}
	
	/**
	 * Decreases the remaining recommendation count.<br>
	 * This method reduces {@code _left} by 1 if it is greater than 0.
	 */
	protected void decreaseLeft()
	{
		if (_left > 0)
		{
			_left--;
		}
	}
	
	/**
	 * Gives a recommendation to the target player.<br>
	 * This method increases the recommendation count for the {@code target}.<br>
	 * It also decreases the remaining recommendations for the current actor.
	 * @param target The {@code Player} who receives the recommendation.
	 */
	public void give(Player target)
	{
		target.getRecommendations().increaseHave();
		decreaseLeft();
	}
	
	/**
	 * Loads the recommendation data from the database.<br>
	 * It retrieves the {@code rec_have} and {@code rec_left} values for the current character.<br>
	 * If an error occurs, it logs a severe message.
	 */
	public void restore()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT rec_have, rec_left FROM character_reco_bonus WHERE charId = ?"))
		{
			statement.setInt(1, _player.getObjectId());
			try (ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
				{
					setHave(rset.getInt("rec_have"));
					setLeft(rset.getInt("rec_left"));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not restore Recommendations for player: " + _player.getObjectId(), e);
		}
	}
	
	/**
	 * Saves the current recommendation data to the database.<br>
	 * This method updates the {@code character_reco_bonus} table.<br>
	 * It uses the unique identifier from {@code getObjectId()} to identify the player.
	 */
	public void store()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("REPLACE INTO character_reco_bonus (charId,rec_have,rec_left,time_left) VALUES (?,?,?,?)"))
		{
			ps.setInt(1, _player.getObjectId());
			ps.setInt(2, _have);
			ps.setInt(3, _left);
			ps.setLong(4, 0);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not update Recommendations for player: " + _player.getObjectId(), e);
		}
	}
	
	/**
	 * Starts the recurring task to provide new recommendations.<br>
	 * This method schedules a {@code RecoGiveTask} using the {@code ThreadPool}.<br>
	 * It also triggers an initial call to {@code storeRecommendations()}.
	 */
	public void startGiveTask()
	{
		// Create task to give new recommendations.
		_giveTask = ThreadPool.scheduleAtFixedRate(new RecoGiveTask(_player), 7200000, 3600000);
		
		// Store new data.
		store();
	}
	
	/**
	 * Stops the recommendation reward task.<br>
	 * This method cancels the {@code _giveTask} if it is currently running.<br>
	 * It then sets the {@code _giveTask} reference to {@code null}.
	 */
	public void stopGiveTask()
	{
		if (_giveTask != null)
		{
			_giveTask.cancel(false);
			_giveTask = null;
		}
	}
	
	/**
	 * Checks if the two-hour recommendation has been granted.<br>
	 * This method returns the current state of the {@code _twoHoursGiven} flag.
	 * @return {@code true} if the recommendation was given, {@code false} otherwise.
	 */
	public boolean isTwoHoursGiven()
	{
		return _twoHoursGiven;
	}
	
	/**
	 * Sets whether the two-hour recommendation has been given.<br>
	 * This updates the {@code _twoHoursGiven} flag.
	 * @param value The boolean value to set for the recommendation status.
	 */
	public void setTwoHoursGiven(boolean value)
	{
		_twoHoursGiven = value;
	}
}
