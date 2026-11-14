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

import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;

/**
 * Represents a status effect that can be applied to {@code Attackable} entities.<br>
 * This class serves as a base for statuses affecting creatures and other attackable actors.
 */
public class AttackableStatus extends NpcStatus
{
	/**
	 * Creates a new {@code AttackableStatus} instance.<br>
	 * This constructor initializes the status for a specific character.
	 * @param activeChar The {@code Attackable} entity this status belongs to.
	 */
	public AttackableStatus(Attackable activeChar)
	{
		super(activeChar);
	}
	
	/**
	 * Reduces the current health of the entity.<br>
	 * This method updates the {@code Creature} health based on a specific amount.
	 * @param value The amount of health to subtract.
	 * @param attacker The {@code Creature} that performed the attack.
	 */
	@Override
	public void reduceHp(double value, Creature attacker)
	{
		reduceHp(value, attacker, true, false, false);
	}
	
	/**
	 * Reduces the health points of the target creature.<br>
	 * This method handles overhit logic and updates the {@code Attackable} state.<br>
	 * It calls the parent reduction logic based on the provided flags.
	 * @param value The amount of health to subtract from the target.
	 * @param attacker The {@code Creature} performing the action.
	 * @param awake Whether the target is currently awake.
	 * @param isDOT Indicates if the damage is a Damage Over Time effect.
	 * @param isHpConsumption Indicates if the reduction is due to HP consumption.
	 */
	@Override
	public void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHpConsumption)
	{
		final Attackable attackable = getActiveChar();
		if (attackable.isDead())
		{
			return;
		}
		
		if (value > 0)
		{
			if (attackable.isOverhit())
			{
				attackable.setOverhitValues(attacker, value);
			}
			else
			{
				attackable.overhitEnabled(false);
			}
		}
		else
		{
			attackable.overhitEnabled(false);
		}
		
		super.reduceHp(value, attacker, awake, isDOT, isHpConsumption);
		
		if (!attackable.isDead())
		{
			// And the attacker's hit didn't kill the mob, clear the over-hit flag.
			attackable.overhitEnabled(false);
		}
	}
	
	/**
	 * Updates the current health points of the entity.<br>
	 * This method sets the {@code newHp} value.<br>
	 * It determines whether to send a network packet based on {@code broadcastPacket}.
	 * @param newHp The new health point value to set.
	 * @param broadcastPacket Set to {@code true} to send a packet, or {@code false} to skip it.
	 * @return {@code true} if the update was successful, otherwise {@code false}.
	 */
	@Override
	public boolean setCurrentHp(double newHp, boolean broadcastPacket)
	{
		return super.setCurrentHp(newHp, true);
	}
	
	/**
	 * Retrieves the current {@code Attackable} entity associated with this status.<br>
	 * This method converts the internal actor to an {@code Attackable} type.
	 * @return The {@code Attackable} character currently affected by this status.
	 */
	@Override
	public Attackable getActiveChar()
	{
		return super.getActiveChar().asAttackable();
	}
}
