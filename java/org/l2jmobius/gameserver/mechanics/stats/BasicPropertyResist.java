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
package org.l2jmobius.gameserver.mechanics.stats;

import java.time.Duration;
import java.time.Instant;

/**
 * This class represents the basic property resistance for mesmerizing debuffs.<br>
 * It handles how characters resist specific types of mesmerization effects.
 * @author Nik
 */
public class BasicPropertyResist
{
	private static final Duration RESIST_DURATION = Duration.ofSeconds(15); // The resistance stays no longer than 15 seconds after last mesmerizing debuff.
	
	private volatile Instant _resistanceEndTime = Instant.MIN;
	private volatile int _resistanceLevel;
	
	/**
	 * Checks if the resistance effect has ended.<br>
	 * It compares the current time against the stored end time.
	 * @return {@code true} if the resistance is no longer active, {@code false} otherwise.
	 */
	public boolean isExpired()
	{
		return Instant.now().isAfter(_resistanceEndTime);
	}
	
	/**
	 * Calculates the time remaining until the resistance expires.<br>
	 * It compares the current time with the stored end time.
	 * @return the {@code Duration} between now and the expiration instant
	 */
	public Duration getRemainTime()
	{
		return Duration.between(Instant.now(), _resistanceEndTime);
	}
	
	/**
	 * Retrieves the current level of resistance.<br>
	 * It returns {@code 0} if the resistance has expired.
	 * @return The current {@code int} resist level.
	 */
	public int getResistLevel()
	{
		return !isExpired() ? _resistanceLevel : 0;
	}
	
	/**
	 * Increases the current resistance level.<br>
	 * This method checks if the resistance has expired.<br>
	 * If it is expired, it resets the level to {@code 1}.<br>
	 * Otherwise, it increments the existing level by {@code 1}.
	 */
	public synchronized void increaseResistLevel()
	{
		// Check if the level needs to be reset due to timer warn off.
		if (isExpired())
		{
			_resistanceLevel = 1;
			_resistanceEndTime = Instant.now().plus(RESIST_DURATION);
		}
		else
		{
			_resistanceLevel++;
		}
	}
}
