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
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Represents the status effects and states specific to a {@code Pet}.<br>
 * This class handles pet-related logic inherited from {@code SummonStatus}.
 */
public class PetStatus extends SummonStatus
{
	private int _currentFed = 0; // Current Fed of the Pet
	
	/**
	 * Creates a new {@code PetStatus} instance.<br>
	 * This constructor initializes the status for a specific pet.
	 * @param activeChar The {@code Pet} associated with this status.
	 */
	public PetStatus(Pet activeChar)
	{
		super(activeChar);
	}
	
	/**
	 * Reduces the health of a {@code Creature}.<br>
	 * This method handles damage calculation and updates.
	 * @param value The amount of health to subtract.
	 * @param attacker The {@code Creature} that dealt the damage.
	 */
	@Override
	public void reduceHp(double value, Creature attacker)
	{
		reduceHp(value, attacker, true, false, false);
	}
	
	/**
	 * Reduces the health of the pet associated with this status.<br>
	 * This method handles damage logic and notifies the owner if applicable.<br>
	 * It also triggers AI reactions based on the attacker information.
	 * @param value The amount of health to subtract from the pet.
	 * @param attacker The {@code Creature} that dealt the damage.
	 * @param awake Whether the pet is currently in an awake state.
	 * @param isDOT Whether the damage is from a Damage Over Time effect.
	 * @param isHpConsumption Whether the damage is caused by health consumption.
	 */
	@Override
	public void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHpConsumption)
	{
		final Pet pet = getActiveChar();
		if (pet.isDead())
		{
			return;
		}
		
		super.reduceHp(value, attacker, awake, isDOT, isHpConsumption);
		
		if (attacker != null)
		{
			if (!isDOT && (pet.getOwner() != null))
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_DEALT_S2_DAMAGE_TO_YOUR_PET);
				sm.addString(attacker.getName());
				sm.addInt((int) value);
				pet.sendPacket(sm);
			}
			
			pet.getAI().notifyActionAttacked(attacker);
		}
	}
	
	/**
	 * Retrieves the current fed value of the pet.<br>
	 * This value represents how much food the pet has currently consumed.
	 * @return The current fed amount as an {@code int}.
	 */
	public int getCurrentFed()
	{
		return _currentFed;
	}
	
	/**
	 * Updates the current fed value for the pet.<br>
	 * This method sets the internal {@code _currentFed} field to a new value.
	 * @param value The new integer value to set.
	 */
	public void setCurrentFed(int value)
	{
		_currentFed = value;
	}
	
	/**
	 * Retrieves the {@code Pet} currently associated with this status.<br>
	 * This method calls the parent class to get the active character and converts it.
	 * @return the current {@code Pet} object.
	 */
	@Override
	public Pet getActiveChar()
	{
		return super.getActiveChar().asPet();
	}
}
