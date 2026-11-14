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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * Checks if a {@code Player} is the leader of their current clan.<br>
 * This condition returns {@code true} if the player holds the leader rank.<br>
 * It is used to restrict actions or requirements to clan leaders only.
 */
public class ConditionPlayerIsClanLeader extends Condition
{
	private final boolean _value;
	
	/**
	 * Creates a new instance of {@code ConditionPlayerIsClanLeader}.<br>
	 * This condition checks if a player is the leader of their clan.<br>
	 * The required state is determined by the provided boolean value.
	 * @param value the boolean value to set for this condition
	 */
	public ConditionPlayerIsClanLeader(boolean value)
	{
		_value = value;
	}
	
	/**
	 * Checks if the {@code effector} is a clan leader.<br>
	 * This method verifies the condition against the internal value of this instance.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item template involved in the action.
	 * @return {@code true} if the condition is met, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		final Player player = effector.asPlayer();
		if (player == null)
		{
			return false;
		}
		
		return player.isClanLeader() == _value;
	}
}
