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
package org.l2jmobius.gameserver.entity.actor.stat;

import org.l2jmobius.gameserver.entity.actor.instance.Door;

/**
 * Represents the statistics specific to a {@code Door} entity.<br>
 * This class handles attributes and properties unique to doors within the game world.
 */
public class DoorStat extends CreatureStat
{
	private int _upgradeHpRatio = 1;
	
	/**
	 * Creates a new {@code DoorStat} instance.<br>
	 * This constructor initializes the statistics for a specific door.
	 * @param activeChar The {@code Door} object associated with these stats.
	 */
	public DoorStat(Door activeChar)
	{
		super(activeChar);
	}
	
	/**
	 * Retrieves the {@code Door} associated with this stat.<br>
	 * It converts the underlying actor to a {@code Door} object.
	 * @return The active {@code Door} instance.
	 */
	@Override
	public Door getActiveChar()
	{
		return super.getActiveChar().asDoor();
	}
	
	/**
	 * Returns the maximum health points of the door.<br>
	 * This value accounts for the {@code _upgradeHpRatio}.
	 * @return The total maximum health as a {@code long}.
	 */
	@Override
	public long getMaxHp()
	{
		return super.getMaxHp() * _upgradeHpRatio;
	}
	
	/**
	 * Sets the health point multiplier for upgraded doors.<br>
	 * This value determines how much extra health is added during an upgrade.
	 * @param ratio The new {@code int} ratio to apply.
	 */
	public void setUpgradeHpRatio(int ratio)
	{
		_upgradeHpRatio = ratio;
	}
	
	/**
	 * Retrieves the health point multiplier for upgrades.<br>
	 * This value determines how much extra HP is added during an upgrade.
	 * @return The current {@code _upgradeHpRatio} as an {@code int}.
	 */
	public int getUpgradeHpRatio()
	{
		return _upgradeHpRatio;
	}
}
