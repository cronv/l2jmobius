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
package org.l2jmobius.gameserver.entity.actor.holders.player;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.player.DamageTakenType;

/**
 * This class stores information regarding damage received by a player.<br>
 * It tracks various types of damage to facilitate calculations and logging.<br>
 * Use this holder to access specific {@code DamageTakenType} data for an actor.
 * @author Mobius
 */
public class DamageTakenHolder
{
	private final Creature _creature;
	private final int _skillId;
	private final double _damage;
	private final DamageTakenType _damageTakenType;
	
	/**
	 * Creates a new instance of {@code DamageTakenHolder}.<br>
	 * This constructor initializes the damage data and determines the {@code DamageTakenType}.
	 * @param creature The {@code Creature} that received the damage.
	 * @param skillId The unique identifier for the skill used.
	 * @param damage The amount of damage dealt.
	 * @param isDOT Set to {@code true} if the damage is a Damage Over Time effect.
	 * @param reflect Set to {@code true} if the damage was reflected back at the attacker.
	 */
	public DamageTakenHolder(Creature creature, int skillId, double damage, boolean isDOT, boolean reflect)
	{
		_creature = creature;
		_skillId = skillId;
		if (isDOT)
		{
			_damageTakenType = DamageTakenType.POISON_FIELD;
		}
		else if (reflect)
		{
			_damageTakenType = DamageTakenType.REFLECTED_DAMAGE;
		}
		else if (skillId > 0)
		{
			_damageTakenType = DamageTakenType.OTHER_DAMAGE;
		}
		else
		{
			_damageTakenType = DamageTakenType.NORMAL_DAMAGE;
		}
		
		_damage = damage;
	}
	
	/**
	 * Retrieves the {@code Creature} associated with this holder.<br>
	 * This method returns the entity that took the damage.
	 * @return the {@code Creature} object.
	 */
	public Creature getCreature()
	{
		return _creature;
	}
	
	/**
	 * Retrieves the unique identifier for the skill used.<br>
	 * This value is stored within the {@code DamageTakenHolder}.
	 * @return The {@code int} ID of the skill.
	 */
	public int getSkillId()
	{
		return _skillId;
	}
	
	/**
	 * Retrieves the amount of damage taken.<br>
	 * This value is stored as a {@code double}.
	 * @return The total damage value.
	 */
	public double getDamage()
	{
		return _damage;
	}
	
	/**
	 * Retrieves the unique identifier for the damage type.<br>
	 * This ID is used to identify the specific client-side effect.
	 * @return The {@code int} client ID associated with this damage type.
	 */
	public int getClientId()
	{
		return _damageTakenType.getClientId();
	}
}
