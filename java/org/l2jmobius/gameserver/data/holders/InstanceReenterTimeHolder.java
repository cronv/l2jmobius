/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.data.holders;

import java.time.DayOfWeek;
import java.util.concurrent.TimeUnit;

/**
 * This class serves as a simple data holder for instance re-entry information.<br>
 * It stores the specific timing data required to manage when an instance can be entered again.
 * @author FallenAngel
 */
public class InstanceReenterTimeHolder
{
	private DayOfWeek _day = null;
	private int _hour = -1;
	private int _minute = -1;
	private long _time = -1;
	
	/**
	 * Creates a new {@code InstanceReenterTimeHolder} using a time value.<br>
	 * This constructor converts the input from minutes to milliseconds.
	 * @param time The time in minutes to be stored.
	 */
	public InstanceReenterTimeHolder(long time)
	{
		_time = TimeUnit.MINUTES.toMillis(time);
	}
	
	/**
	 * Creates a new {@code InstanceReenterTimeHolder} with a specific time.<br>
	 * This constructor sets the day, hour, and minute for instance re-entry.
	 * @param day The {@code DayOfWeek} for the re-entry.
	 * @param hour The hour of the day as an integer.
	 * @param minute The minute of the hour as an integer.
	 */
	public InstanceReenterTimeHolder(DayOfWeek day, int hour, int minute)
	{
		_day = day;
		_hour = hour;
		_minute = minute;
	}
	
	/**
	 * Retrieves the stored time value.<br>
	 * This method returns the internal {@code long} timestamp.
	 * @return The current time as a {@code long}.
	 */
	public long getTime()
	{
		return _time;
	}
	
	/**
	 * Retrieves the day of the week for this instance.<br>
	 * This method returns the {@code DayOfWeek} value stored in the holder.
	 * @return The {@code DayOfWeek} associated with the reenter time.
	 */
	public DayOfWeek getDay()
	{
		return _day;
	}
	
	/**
	 * Retrieves the current hour stored in this holder.<br>
	 * This value is part of the instance reenter time configuration.
	 * @return The hour as an {@code int}.
	 */
	public int getHour()
	{
		return _hour;
	}
	
	/**
	 * Retrieves the current minute value.<br>
	 * This method returns the minute stored in this {@code InstanceReenterTimeHolder}.
	 * @return The minute as an {@code int}.
	 */
	public int getMinute()
	{
		return _minute;
	}
}
