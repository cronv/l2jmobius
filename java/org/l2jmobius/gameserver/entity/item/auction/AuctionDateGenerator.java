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
package org.l2jmobius.gameserver.entity.item.auction;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class provides utility methods to calculate auction expiration dates.<br>
 * It handles the logic for determining when an auction should end based on a specific duration.<br>
 * It is used by {@code Auction} objects to manage time-based lifecycle events.
 * @author Forsaiken
 */
public class AuctionDateGenerator
{
	public static final String FIELD_INTERVAL = "interval";
	public static final String FIELD_DAY_OF_WEEK = "day_of_week";
	public static final String FIELD_HOUR_OF_DAY = "hour_of_day";
	public static final String FIELD_MINUTE_OF_HOUR = "minute_of_hour";
	
	private static final long MILLIS_IN_WEEK = TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS);
	
	private final Calendar _calendar;
	
	private final int _interval;
	private int _day_of_week;
	private int _hour_of_day;
	private int _minute_of_hour;
	
	/**
	 * Creates a new instance of {@code AuctionDateGenerator}.<br>
	 * It initializes the generator using settings from a {@code StatSet}.<br>
	 * The configuration values determine the auction intervals and times.
	 * @param config The {@code StatSet} containing the configuration data.
	 */
	public AuctionDateGenerator(StatSet config)
	{
		_calendar = Calendar.getInstance();
		_interval = config.getInt(FIELD_INTERVAL, -1);
		
		// NC week start in Monday.
		final int fixedDayWeek = config.getInt(FIELD_DAY_OF_WEEK, -1) + 1;
		_day_of_week = (fixedDayWeek > 7) ? 1 : fixedDayWeek;
		_hour_of_day = config.getInt(FIELD_HOUR_OF_DAY, -1);
		_minute_of_hour = config.getInt(FIELD_MINUTE_OF_HOUR, -1);
		checkDayOfWeek(-1);
		checkHourOfDay(-1);
		checkMinuteOfHour(0);
	}
	
	/**
	 * Calculates the next valid auction date based on a given timestamp.<br>
	 * This method adjusts the time to match the configured interval or day of week.<br>
	 * It ensures the returned value is in the future relative to the input.
	 * @param date The starting timestamp in milliseconds.
	 * @return The next valid auction timestamp in milliseconds.
	 */
	public synchronized long nextDate(long date)
	{
		_calendar.setTimeInMillis(date);
		_calendar.set(Calendar.MILLISECOND, 0);
		_calendar.set(Calendar.SECOND, 0);
		_calendar.set(Calendar.MINUTE, _minute_of_hour);
		_calendar.set(Calendar.HOUR_OF_DAY, _hour_of_day);
		if (_day_of_week > 0)
		{
			_calendar.set(Calendar.DAY_OF_WEEK, _day_of_week);
			return calcDestTime(_calendar.getTimeInMillis(), date, MILLIS_IN_WEEK);
		}
		
		return calcDestTime(_calendar.getTimeInMillis(), date, TimeUnit.MILLISECONDS.convert(_interval, TimeUnit.DAYS));
	}
	
	/**
	 * Calculates the destination time based on a given value and interval.<br>
	 * It ensures the result is not earlier than the specified date.<br>
	 * The calculation rounds the time up to the nearest multiple of {@code add}.
	 * @param timeValue The initial time value to process.
	 * @param date The minimum allowed date for the destination.
	 * @param add The interval used to round the time.
	 * @return The calculated destination time as a {@code long}.
	 */
	private long calcDestTime(long timeValue, long date, long add)
	{
		long time = timeValue;
		if (time < date)
		{
			time += ((date - time) / add) * add;
			if (time < date)
			{
				time += add;
			}
		}
		
		return time;
	}
	
	/**
	 * Validates the current day of the week value.<br>
	 * It ensures the value is between 1 and 7.<br>
	 * If the value is invalid, it assigns the provided {@code defaultValue}.<br>
	 * This method also checks for conflicts between the interval and the day of the week.
	 * @param defaultValue The value to use if the current day of the week is out of range.
	 */
	private void checkDayOfWeek(int defaultValue)
	{
		if ((_day_of_week < 1) || (_day_of_week > 7))
		{
			if ((defaultValue == -1) && (_interval < 1))
			{
				throw new IllegalArgumentException("Illegal params for '" + FIELD_DAY_OF_WEEK + "': " + (_day_of_week == -1 ? "not found" : _day_of_week));
			}
			
			_day_of_week = defaultValue;
		}
		else if (_interval > 1)
		{
			throw new IllegalArgumentException("Illegal params for '" + FIELD_INTERVAL + "' and '" + FIELD_DAY_OF_WEEK + "': you can use only one, not both");
		}
	}
	
	/**
	 * Validates the current {@code _hour_of_day} value.<br>
	 * It checks if the hour is between 0 and 23.<br>
	 * If the value is invalid, it assigns the provided {@code defaultValue}.<br>
	 * An {@code IllegalArgumentException} is thrown if the value is out of range and {@code defaultValue} is -1.
	 * @param defaultValue The value to use if the current hour is invalid.
	 */
	private void checkHourOfDay(int defaultValue)
	{
		if ((_hour_of_day < 0) || (_hour_of_day > 23))
		{
			if (defaultValue == -1)
			{
				throw new IllegalArgumentException("Illegal params for '" + FIELD_HOUR_OF_DAY + "': " + (_hour_of_day == -1 ? "not found" : _hour_of_day));
			}
			
			_hour_of_day = defaultValue;
		}
	}
	
	/**
	 * Validates the current minute value.<br>
	 * It checks if {@code _minute_of_hour} is between 0 and 59.<br>
	 * If the value is invalid, it assigns the provided {@code defaultValue}.<br>
	 * An exception is thrown if the value is out of range and {@code defaultValue} is -1.
	 * @param defaultValue The value to use if the current minute is invalid.
	 */
	private void checkMinuteOfHour(int defaultValue)
	{
		if ((_minute_of_hour < 0) || (_minute_of_hour > 59))
		{
			if (defaultValue == -1)
			{
				throw new IllegalArgumentException("Illegal params for '" + FIELD_MINUTE_OF_HOUR + "': " + (_minute_of_hour == -1 ? "not found" : _minute_of_hour));
			}
			
			_minute_of_hour = defaultValue;
		}
	}
}
