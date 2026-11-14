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
 * This class represents a condition that checks the weight of a target.<br>
 * It is used to verify if an entity meets specific weight requirements during mechanic execution.
 * @author Zoey76, Mobius
 */
public class ConditionTargetWeight extends Condition
{
	private final int _weight;
	
	/**
	 * Creates a new {@code ConditionTargetWeight} instance.<br>
	 * This sets the required weight for the condition.
	 * @param weight The numeric value of the weight to check.
	 */
	public ConditionTargetWeight(int weight)
	{
		_weight = weight;
	}
	
	/**
	 * Checks if the target player's current load is below a specific weight threshold.<br>
	 * This check only applies to players who are not in diet mode and have a positive maximum load.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used.
	 * @param item The item template involved in the condition.
	 * @return {@code true} if the player's weight percentage is less than the required threshold, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		if ((effected != null) && effected.isPlayer())
		{
			final Player target = effected.asPlayer();
			final int maxLoad = target.getMaxLoad();
			if (!target.getDietMode() && (maxLoad > 0))
			{
				return (((target.getCurrentLoad() - target.getBonusWeightPenalty()) * 100L) / maxLoad) < _weight;
			}
		}
		
		return false;
	}
}
