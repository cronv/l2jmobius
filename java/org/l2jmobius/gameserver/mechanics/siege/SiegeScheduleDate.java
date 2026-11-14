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
package org.l2jmobius.gameserver.mechanics.siege;

import java.util.Calendar;

import org.l2jmobius.gameserver.util.StatSet;

/**
 * Represents a specific date and time for a siege event.<br>
 * This class is used to manage the scheduling of siege mechanics within the game world.
 * @author UnAfraid
 */
public class SiegeScheduleDate
{
	private final int _day;
	private final int _hour;
	private final int _maxConcurrent;
	private final boolean _siegeEnabled;
	
	/**
	 * Creates a new {@code SiegeScheduleDate} instance using values from a {@code StatSet}.<br>
	 * It initializes the schedule with default values if specific keys are missing.
	 * @param set The {@code StatSet} containing the configuration data.
	 */
	public SiegeScheduleDate(StatSet set)
	{
		_day = set.getInt("day", Calendar.SUNDAY);
		_hour = set.getInt("hour", 16);
		_maxConcurrent = set.getInt("maxConcurrent", 5);
		_siegeEnabled = set.getBoolean("siegeEnabled", false);
	}
	
	/**
	 * Retrieves the day of the month for this schedule.<br>
	 * This value is stored in the {@code _day} field.
	 * @return The day as an {@code int}.
	 */
	public int getDay()
	{
		return _day;
	}
	
	/**
	 * Retrieves the hour for this siege schedule.<br>
	 * The value is stored as an {@code int}.
	 * @return The current hour of the siege.
	 */
	public int getHour()
	{
		return _hour;
	}
	
	/**
	 * Retrieves the maximum number of concurrent players allowed.<br>
	 * This value is used to limit participation in a siege event.
	 * @return The maximum number of concurrent participants as an {@code int}.
	 */
	public int getMaxConcurrent()
	{
		return _maxConcurrent;
	}
	
	/**
	 * Checks if the siege is currently enabled.<br>
	 * Returns {@code true} if it is active.<br>
	 * Returns {@code false} if it is disabled.
	 * @return The status of the siege enablement.
	 */
	public boolean siegeEnabled()
	{
		return _siegeEnabled;
	}
}
