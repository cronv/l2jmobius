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
 * This class checks if a {@code Player} meets specific weight requirements.<br>
 * It is used to determine if an actor can perform actions based on their current inventory load.
 * @author Kerberos, Mobius
 */
public class ConditionPlayerWeight extends Condition
{
	private final int _weight;
	
	/**
	 * Creates a new instance of {@code ConditionPlayerWeight}.<br>
	 * This condition checks the weight of a player.
	 * @param weight The weight value to use for this condition.
	 */
	public ConditionPlayerWeight(int weight)
	{
		_weight = weight;
	}
	
	/**
	 * Checks if the {@code effector} meets the weight requirements.<br>
	 * This method calculates the current load percentage of a {@code Player}.<br>
	 * It returns {@code true} if the player is in diet mode or below the weight threshold.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} receiving the action.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code ItemTemplate} involved in the action.
	 * @return {@code true} if the condition is met, {@code false} otherwise.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		final Player player = effector.asPlayer();
		if ((player != null) && (player.getMaxLoad() > 0))
		{
			final long weightproc = ((player.getCurrentLoad() - player.getBonusWeightPenalty()) * 100L) / player.getMaxLoad();
			return (weightproc < _weight) || player.getDietMode();
		}
		
		return true;
	}
}
