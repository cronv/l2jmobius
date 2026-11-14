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
package org.l2jmobius.gameserver.mechanics.events.holders.actor.creature;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This class represents an instantly executed event triggered when a {@code Creature} is attacked by another {@code Creature}.<br>
 * It provides the necessary context for handling damage logic between two entities.
 * @author UnAfraid, Mobius
 */
public class OnCreatureDamageReceived implements IBaseEvent
{
	private final Creature _attacker;
	private final Creature _target;
	private final double _damage;
	private final Skill _skill;
	private final boolean _crit;
	private final boolean _damageOverTime;
	private final boolean _reflect;
	
	/**
	 * Creates the event for one hit.
	 * @param attacker the creature that dealt the damage
	 * @param target the creature that received the damage
	 * @param damage the damage amount
	 * @param skill the skill that dealt the damage, {@code null} for a normal attack
	 * @param critical whether the hit was critical
	 * @param damageOverTime whether the damage came from a damage over time effect
	 * @param reflect whether the damage was reflected
	 */
	public OnCreatureDamageReceived(Creature attacker, Creature target, double damage, Skill skill, boolean critical, boolean damageOverTime, boolean reflect)
	{
		_attacker = attacker;
		_target = target;
		_damage = damage;
		_skill = skill;
		_crit = critical;
		_damageOverTime = damageOverTime;
		_reflect = reflect;
	}
	
	/**
	 * Retrieves the creature that initiated the attack.<br>
	 * This method returns the {@code Creature} object responsible for the damage event.
	 * @return the {@code Creature} who performed the attack.
	 */
	public Creature getAttacker()
	{
		return _attacker;
	}
	
	/**
	 * Retrieves the creature that was targeted by the attack.
	 * @return the {@code Creature} object representing the target.
	 */
	public Creature getTarget()
	{
		return _target;
	}
	
	/**
	 * Retrieves the amount of damage dealt in this event.<br>
	 * This value is stored as a {@code double}.
	 * @return The total damage amount.
	 */
	public double getDamage()
	{
		return _damage;
	}
	
	/**
	 * Retrieves the {@code Skill} used to deal damage.<br>
	 * This method returns the skill object associated with this event.
	 * @return the {@code Skill} instance, or {@code null} if no skill is assigned.
	 */
	public Skill getSkill()
	{
		return _skill;
	}
	
	/**
	 * Checks if the damage was a critical hit.<br>
	 * Returns {@code true} if it was a critical hit.<br>
	 * Returns {@code false} otherwise.
	 * @return The critical hit status.
	 */
	public boolean isCritical()
	{
		return _crit;
	}
	
	/**
	 * Checks if the damage was dealt over time.<br>
	 * This is used to identify effects like poison or burns.
	 * @return {@code true} if it is a damage over time effect, {@code false} otherwise.
	 */
	public boolean isDamageOverTime()
	{
		return _damageOverTime;
	}
	
	/**
	 * Checks if the damage event includes a reflection effect.<br>
	 * This determines if the damage is reflected back to the attacker.
	 * @return {@code true} if the damage is reflected, {@code false} otherwise.
	 */
	public boolean isReflect()
	{
		return _reflect;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * It returns a constant value from {@code EventType}.
	 * @return The {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_CREATURE_DAMAGE_RECEIVED;
	}
}
