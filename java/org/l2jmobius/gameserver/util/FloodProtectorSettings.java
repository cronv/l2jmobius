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
package org.l2jmobius.gameserver.util;

/**
 * Holds configurable limits and punishment rules for a single flood protector type.
 * @author fordfrog, Mobius
 */
public class FloodProtectorSettings
{
	private final String _floodProtectorType;
	private int _protectionInterval;
	private boolean _logFlooding;
	private int _punishmentLimit;
	private String _punishmentType;
	private long _punishmentTime;
	
	/**
	 * Creates settings for the given flood protector type.
	 * @param floodProtectorType type of flood protection
	 */
	public FloodProtectorSettings(String floodProtectorType)
	{
		_floodProtectorType = floodProtectorType;
	}
	
	/**
	 * Returns the flood protector type.
	 * @return the flood protector type
	 */
	public String getFloodProtectorType()
	{
		return _floodProtectorType;
	}
	
	/**
	 * Returns the protection interval.
	 * @return the protection interval
	 */
	public int getProtectionInterval()
	{
		return _protectionInterval;
	}
	
	/**
	 * Sets the protection interval.
	 * @param protectionInterval interval to set
	 */
	public void setProtectionInterval(int protectionInterval)
	{
		_protectionInterval = protectionInterval;
	}
	
	/**
	 * Sets whether flooding events are logged.
	 * @param logFlooding {@code true} to enable logging, {@code false} to disable
	 */
	public void setLogFlooding(boolean logFlooding)
	{
		_logFlooding = logFlooding;
	}
	
	/**
	 * Indicates whether flooding events are logged.
	 * @return {@code true} if logging is enabled, {@code false} otherwise
	 */
	public boolean isLogFlooding()
	{
		return _logFlooding;
	}
	
	/**
	 * Sets the violation count that triggers punishment.
	 * @param punishmentLimit limit to set
	 */
	public void setPunishmentLimit(int punishmentLimit)
	{
		_punishmentLimit = punishmentLimit;
	}
	
	/**
	 * Returns the violation count that triggers punishment.
	 * @return the punishment limit
	 */
	public int getPunishmentLimit()
	{
		return _punishmentLimit;
	}
	
	/**
	 * Sets the punishment type applied for flooding.
	 * @param punishmentType punishment type to set
	 */
	public void setPunishmentType(String punishmentType)
	{
		_punishmentType = punishmentType;
	}
	
	/**
	 * Returns the punishment type applied for flooding.
	 * @return the punishment type
	 */
	public String getPunishmentType()
	{
		return _punishmentType;
	}
	
	/**
	 * Sets the punishment duration.
	 * @param punishmentTime duration in milliseconds
	 */
	public void setPunishmentTime(long punishmentTime)
	{
		_punishmentTime = punishmentTime;
	}
	
	/**
	 * Returns the punishment duration.
	 * @return the punishment time
	 */
	public long getPunishmentTime()
	{
		return _punishmentTime;
	}
}
