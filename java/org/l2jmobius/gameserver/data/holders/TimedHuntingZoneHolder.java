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
package org.l2jmobius.gameserver.data.holders;

import org.l2jmobius.gameserver.entity.Location;

/**
 * Holds the configuration and timing data for a timed hunting zone.
 * @author Mobius
 */
public class TimedHuntingZoneHolder
{
	private final int _id;
	private final String _name;
	private final int _initialTime;
	private final int _maximumAddedTime;
	private final int _resetDelay;
	private final int _entryItemId;
	private final int _entryFee;
	private final int _minLevel;
	private final int _maxLevel;
	private final int _remainRefillTime;
	private final int _refillTimeMax;
	private final boolean _pvpZone;
	private final boolean _noPvpZone;
	private final int _instanceId;
	private final boolean _soloInstance;
	private final boolean _weekly;
	private final boolean _useWorldPrefix;
	private final boolean _zonePremiumUserOnly;
	private final Location _enterLocation;
	private final Location _exitLocation;
	private final boolean _isEvenWeek;
	private final boolean _isSwapWeek;
	
	/**
	 * Creates a {@code TimedHuntingZoneHolder} from the given configuration.
	 * @param id The zone id.
	 * @param name The zone name.
	 * @param initialTime The base time duration.
	 * @param maximumAddedTime The maximum extra time.
	 * @param resetDelay The reset delay.
	 * @param entryItemId The entry item id.
	 * @param entryFee The entry fee.
	 * @param minLevel The minimum level.
	 * @param maxLevel The maximum level.
	 * @param remainRefillTime The remaining refill time.
	 * @param refillTimeMax The maximum refill time.
	 * @param pvpZone {@code true} if PvP is allowed.
	 * @param noPvpZone {@code true} if PvP is forbidden.
	 * @param instanceId The instance id.
	 * @param soloInstance {@code true} if a solo instance.
	 * @param weekly {@code true} if a weekly zone.
	 * @param useWorldPrefix {@code true} to use the world prefix.
	 * @param zonePremiumUserOnly {@code true} if premium-only.
	 * @param enterLocation The entry {@code Location}.
	 * @param exitLocation The exit {@code Location}.
	 * @param isEvenWeek {@code true} if active on an even week.
	 * @param isSwapWeek {@code true} if a swap week.
	 */
	public TimedHuntingZoneHolder(int id, String name, int initialTime, int maximumAddedTime, int resetDelay, int entryItemId, int entryFee, int minLevel, int maxLevel, int remainRefillTime, int refillTimeMax, boolean pvpZone, boolean noPvpZone, int instanceId, boolean soloInstance, boolean weekly, boolean useWorldPrefix, boolean zonePremiumUserOnly, Location enterLocation, Location exitLocation, boolean isEvenWeek, boolean isSwapWeek)
	{
		_id = id;
		_name = name;
		_initialTime = initialTime;
		_maximumAddedTime = maximumAddedTime;
		_resetDelay = resetDelay;
		_entryItemId = entryItemId;
		_entryFee = entryFee;
		_minLevel = minLevel;
		_maxLevel = maxLevel;
		_remainRefillTime = remainRefillTime;
		_refillTimeMax = refillTimeMax;
		_pvpZone = pvpZone;
		_noPvpZone = noPvpZone;
		_instanceId = instanceId;
		_soloInstance = soloInstance;
		_weekly = weekly;
		_useWorldPrefix = useWorldPrefix;
		_zonePremiumUserOnly = zonePremiumUserOnly;
		_enterLocation = enterLocation;
		_exitLocation = exitLocation;
		_isEvenWeek = isEvenWeek;
		_isSwapWeek = isSwapWeek;
	}
	
	/**
	 * Retrieves the zone id.
	 * @return The zone id as an {@code int}.
	 */
	public int getZoneId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the zone name.
	 * @return The zone name as a {@code String}.
	 */
	public String getZoneName()
	{
		return _name;
	}
	
	/**
	 * Retrieves the base time duration.
	 * @return The initial time as an {@code int}.
	 */
	public int getInitialTime()
	{
		return _initialTime;
	}
	
	/**
	 * Retrieves the maximum added time.
	 * @return The maximum added time as an {@code int}.
	 */
	public int getMaximumAddedTime()
	{
		return _maximumAddedTime;
	}
	
	/**
	 * Retrieves the reset delay.
	 * @return The reset delay as an {@code int}.
	 */
	public int getResetDelay()
	{
		return _resetDelay;
	}
	
	/**
	 * Retrieves the entry item id.
	 * @return The entry item id as an {@code int}.
	 */
	public int getEntryItemId()
	{
		return _entryItemId;
	}
	
	/**
	 * Retrieves the entry fee.
	 * @return The entry fee as an {@code int}.
	 */
	public int getEntryFee()
	{
		return _entryFee;
	}
	
	/**
	 * Retrieves the minimum level.
	 * @return The minimum level as an {@code int}.
	 */
	public int getMinLevel()
	{
		return _minLevel;
	}
	
	/**
	 * Retrieves the maximum level.
	 * @return The maximum level as an {@code int}.
	 */
	public int getMaxLevel()
	{
		return _maxLevel;
	}
	
	/**
	 * Retrieves the remaining refill time.
	 * @return The remaining refill time as an {@code int}.
	 */
	public int getRemainRefillTime()
	{
		return _remainRefillTime;
	}
	
	/**
	 * Retrieves the maximum refill time.
	 * @return The maximum refill time as an {@code int}.
	 */
	public int getRefillTimeMax()
	{
		return _refillTimeMax;
	}
	
	/**
	 * Indicates whether PvP is allowed.
	 * @return {@code true} if a PvP zone, {@code false} otherwise.
	 */
	public boolean isPvpZone()
	{
		return _pvpZone;
	}
	
	/**
	 * Indicates whether PvP is forbidden.
	 * @return {@code true} if a no-PvP zone, {@code false} otherwise.
	 */
	public boolean isNoPvpZone()
	{
		return _noPvpZone;
	}
	
	/**
	 * Retrieves the instance id.
	 * @return The instance id as an {@code int}.
	 */
	public int getInstanceId()
	{
		return _instanceId;
	}
	
	/**
	 * Indicates whether this is a solo instance.
	 * @return {@code true} if a solo instance, {@code false} otherwise.
	 */
	public boolean isSoloInstance()
	{
		return _soloInstance;
	}
	
	/**
	 * Indicates whether this is a weekly zone.
	 * @return {@code true} if weekly, {@code false} otherwise.
	 */
	public boolean isWeekly()
	{
		return _weekly;
	}
	
	/**
	 * Indicates whether the world prefix is used.
	 * @return {@code true} if enabled, {@code false} otherwise.
	 */
	public boolean useWorldPrefix()
	{
		return _useWorldPrefix;
	}
	
	/**
	 * Indicates whether the zone is premium-only.
	 * @return {@code true} if premium-only, {@code false} otherwise.
	 */
	public boolean zonePremiumUserOnly()
	{
		return _zonePremiumUserOnly;
	}
	
	/**
	 * Retrieves the entry location.
	 * @return The entry {@code Location}.
	 */
	public Location getEnterLocation()
	{
		return _enterLocation;
	}
	
	/**
	 * Retrieves the exit location.
	 * @return The exit {@code Location}.
	 */
	public Location getExitLocation()
	{
		return _exitLocation;
	}
	
	/**
	 * Indicates whether the zone is active on an even week.
	 * @return {@code true} if an even week, {@code false} otherwise.
	 */
	public boolean isEvenWeek()
	{
		return _isEvenWeek;
	}
	
	/**
	 * Indicates whether the current week is a swap week.
	 * @return {@code true} if a swap week, {@code false} otherwise.
	 */
	public boolean isSwapWeek()
	{
		return _isSwapWeek;
	}
}
