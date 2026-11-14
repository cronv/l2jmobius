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
package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * Checks if a player character has the dual-class system enabled.<br>
 * This condition is used to verify specific requirements related to multi-classing.<br>
 * It returns {@code true} if the actor meets the criteria for being a dual-class character.
 */
public class ConditionPlayerDualclass extends Condition
{
	private final boolean _value;
	
	/**
	 * Creates a new {@code ConditionPlayerDualclass} instance.<br>
	 * This condition checks if the player has a dual class.
	 * @param value The boolean state to check for this condition.
	 */
	public ConditionPlayerDualclass(boolean value)
	{
		_value = value;
	}
	
	/**
	 * Checks if the {@code effector} meets the dual class condition.<br>
	 * If the {@code effector} is not a player, it returns {@code true}.<br>
	 * Otherwise, it compares the active dual class status against the stored value.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item template involved in the action.
	 * @return {@code true} if the condition is met, {@code false} otherwise.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		if (!effector.isPlayer())
		{
			return true;
		}
		
		return effector.asPlayer().isDualClassActive() == _value;
	}
}
