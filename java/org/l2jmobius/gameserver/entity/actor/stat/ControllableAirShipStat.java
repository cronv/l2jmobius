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

import org.l2jmobius.gameserver.entity.actor.instance.ControllableAirShip;

/**
 * Represents the statistics for a {@code ControllableAirShip}.<br>
 * This class defines specific attributes and behaviors unique to controllable airships.<br>
 * It extends {@code VehicleStat} to provide base vehicle functionality.
 */
public class ControllableAirShipStat extends VehicleStat
{
	/**
	 * Creates a new {@code ControllableAirShipStat} instance.<br>
	 * This constructor initializes the statistics for a specific air ship.<br>
	 * It links the stats to the provided {@code activeChar}.
	 * @param activeChar The {@code ControllableAirShip} character associated with these stats.
	 */
	public ControllableAirShipStat(ControllableAirShip activeChar)
	{
		super(activeChar);
	}
	
	/**
	 * Retrieves the {@code ControllableAirShip} associated with this statistic.<br>
	 * This method returns the active character instance.
	 * @return the current {@code ControllableAirShip} object.
	 */
	@Override
	public ControllableAirShip getActiveChar()
	{
		return (ControllableAirShip) super.getActiveChar();
	}
	
	/**
	 * Returns the current movement speed of the airship.<br>
	 * The speed is reduced if the ship has no fuel and is not in a dock.
	 * @return the calculated move speed as a {@code double}.
	 */
	@Override
	public double getMoveSpeed()
	{
		if (getActiveChar().isInDock() || (getActiveChar().getFuel() > 0))
		{
			return super.getMoveSpeed();
		}
		
		return super.getMoveSpeed() * 0.05f;
	}
}
