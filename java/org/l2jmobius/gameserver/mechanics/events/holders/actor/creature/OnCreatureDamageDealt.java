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
 * This event is triggered instantly when a {@code Creature} deals damage to another {@code Creature}.<br>
 * It allows for the execution of custom logic immediately following a successful damage calculation.
 * @author UnAfraid, Mobius
 */
public class OnCreatureDamageDealt implements IBaseEvent
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
	public OnCreatureDamageDealt(Creature attacker, Creature target, double damage, Skill skill, boolean critical, boolean damageOverTime, boolean reflect)
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
	 * This method returns the {@code Creature} object responsible for dealing damage.
	 * @return the {@code Creature} who performed the attack.
	 */
	public Creature getAttacker()
	{
		return _attacker;
	}
	
	/**
	 * Retrieves the creature that was hit by an attack.<br>
	 * This method returns the target associated with this event.
	 * @return the {@code Creature} that received the damage.
	 */
	public Creature getTarget()
	{
		return _target;
	}
	
	/**
	 * Retrieves the amount of damage dealt.<br>
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
	 * @return the {@code Skill} instance.
	 */
	public Skill getSkill()
	{
		return _skill;
	}
	
	/**
	 * Checks if the damage dealt was a critical hit.
	 * @return {@code true} if the attack was critical, {@code false} otherwise.
	 */
	public boolean isCritical()
	{
		return _crit;
	}
	
	/**
	 * Checks if the damage dealt is a damage-over-time effect.<br>
	 * This helps identify skills that apply damage gradually over time.
	 * @return {@code true} if the event represents damage-over-time, {@code false} otherwise.
	 */
	public boolean isDamageOverTime()
	{
		return _damageOverTime;
	}
	
	/**
	 * Checks if the damage dealt by this event is reflected.<br>
	 * Returns {@code true} if reflection is active.<br>
	 * Returns {@code false} otherwise.
	 * @return a boolean indicating if the damage is reflected.
	 */
	public boolean isReflect()
	{
		return _reflect;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with the damage dealt.
	 * @return The {@code EventType} for this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_CREATURE_DAMAGE_DEALT;
	}
}
