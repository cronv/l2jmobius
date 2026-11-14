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
package org.l2jmobius.gameserver.entity.actor.status;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.instance.SiegeFlag;

/**
 * Represents the status effects and properties specific to a {@code SiegeFlag}.<br>
 * This class handles how siege flags behave within the game world.<br>
 * It extends {@code NpcStatus} to provide specialized logic for these entities.
 */
public class SiegeFlagStatus extends NpcStatus
{
	/**
	 * Creates a new {@code SiegeFlagStatus} instance.<br>
	 * This constructor initializes the status using an active character.
	 * @param activeChar The {@code SiegeFlag} associated with this status.
	 */
	public SiegeFlagStatus(SiegeFlag activeChar)
	{
		super(activeChar);
	}
	
	/**
	 * Reduces the health points of this status.<br>
	 * It takes a specific amount to subtract from the current total.<br>
	 * The method also records which creature caused the damage.
	 * @param value The amount of health to remove.
	 * @param attacker The {@code Creature} that performed the action.
	 */
	@Override
	public void reduceHp(double value, Creature attacker)
	{
		reduceHp(value, attacker, true, false, false);
	}
	
	/**
	 * Reduces the health points of the {@code SiegeFlag}.<br>
	 * This method handles special damage logic for advanced headquarters.<br>
	 * It applies a reduction based on the provided parameters.
	 * @param value The amount of damage to apply.
	 * @param attacker The {@code Creature} that initiated the attack.
	 * @param awake Whether the target is currently awake.
	 * @param isDOT Whether the damage is from a Damage Over Time effect.
	 * @param isHpConsumption Whether the damage is caused by health consumption.
	 */
	@Override
	public void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHpConsumption)
	{
		if (getActiveChar().isAdvancedHeadquarter())
		{
			super.reduceHp(value / 2, attacker, awake, isDOT, isHpConsumption);
		}
		else
		{
			super.reduceHp(value, attacker, awake, isDOT, isHpConsumption);
		}
	}
	
	/**
	 * Retrieves the character currently associated with this siege flag.<br>
	 * This method returns the {@code SiegeFlag} object.
	 * @return the active {@code SiegeFlag}
	 */
	@Override
	public SiegeFlag getActiveChar()
	{
		return (SiegeFlag) super.getActiveChar();
	}
}
