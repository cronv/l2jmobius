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
package org.l2jmobius.gameserver.entity.actor.holders.creature;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This class acts as a data holder for the {@code DelayedPump} effect.<br>
 * It stores necessary information to manage the delay and execution of the pump mechanic.
 * @author Mobius
 */
public class DelayedPumpHolder
{
	private final AbstractEffect _effect;
	private final Creature _effected;
	private final Skill _skill;
	
	/**
	 * Creates a new instance of {@code DelayedPumpHolder}.<br>
	 * This constructor initializes the holder with an effect, a target creature, and a skill.
	 * @param effect The {@code AbstractEffect} to be applied.
	 * @param effected The {@code Creature} that is receiving the effect.
	 * @param skill The {@code Skill} associated with this action.
	 */
	public DelayedPumpHolder(AbstractEffect effect, Creature effected, Skill skill)
	{
		_effect = effect;
		_effected = effected;
		_skill = skill;
	}
	
	/**
	 * Retrieves the {@code AbstractEffect} associated with this holder.<br>
	 * This method returns the effect that is being delayed.
	 * @return the {@code AbstractEffect} instance.
	 */
	public AbstractEffect getEffect()
	{
		return _effect;
	}
	
	/**
	 * Retrieves the {@code Creature} that is currently affected by this holder.<br>
	 * This method returns the target of the effect.
	 * @return the {@code Creature} being affected
	 */
	public Creature getEffected()
	{
		return _effected;
	}
	
	/**
	 * Retrieves the {@code Skill} associated with this holder.
	 * @return the {@code Skill} object.
	 */
	public Skill getSkill()
	{
		return _skill;
	}
}
