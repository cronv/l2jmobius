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
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Checks if a {@code Player} has any available free summon points.<br>
 * This condition is used to verify resource availability before performing specific actions.
 * @author Sdw, Mobius
 */
public class ConditionPlayerHasFreeSummonPoints extends Condition
{
	private final int _summonPoints;
	
	/**
	 * Creates a new condition to check for available summon points.<br>
	 * This condition verifies if a player has enough points to perform an action.
	 * @param summonPoints The required amount of summon points needed.
	 */
	public ConditionPlayerHasFreeSummonPoints(int summonPoints)
	{
		_summonPoints = summonPoints;
	}
	
	/**
	 * Checks if the player has enough summon points to perform an action.<br>
	 * It validates the current points against the maximum limit.<br>
	 * It also checks for specific servitor requirements when points are zero.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code ItemTemplate} associated with the action.
	 * @return {@code true} if the player can perform the action, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		final Player player = effector.asPlayer();
		if (player == null)
		{
			return false;
		}
		
		boolean canSummon = true;
		if ((_summonPoints == 0) && player.hasServitors())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_USE_THE_S1_SKILL_DUE_TO_INSUFFICIENT_SUMMON_POINTS).addSkillName(skill));
			canSummon = false;
		}
		else if ((player.getSummonPoints() + _summonPoints) > player.getMaxSummonPoints())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_CANNOT_USE_THE_S1_SKILL_DUE_TO_INSUFFICIENT_SUMMON_POINTS);
			sm.addSkillName(skill);
			player.sendPacket(sm);
			canSummon = false;
		}
		
		return canSummon;
	}
}
