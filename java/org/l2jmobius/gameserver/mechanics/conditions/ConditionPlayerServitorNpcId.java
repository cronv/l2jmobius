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

import java.util.List;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * Checks if a player's servitor matches a specific {@code ItemTemplate} ID.<br>
 * This condition is used to verify the identity of a summoned creature.
 */
public class ConditionPlayerServitorNpcId extends Condition
{
	private final List<Integer> _npcIds;
	
	/**
	 * Creates a new instance of {@code ConditionPlayerServitorNpcId}.<br>
	 * This condition checks for specific NPC IDs associated with a player's servitor.<br>
	 * If the provided list contains only the value {@code 0}, the internal list is set to {@code null}.
	 * @param npcIds The list of NPC IDs to check.
	 */
	public ConditionPlayerServitorNpcId(List<Integer> npcIds)
	{
		if ((npcIds.size() == 1) && (npcIds.get(0) == 0))
		{
			_npcIds = null;
		}
		else
		{
			_npcIds = npcIds;
		}
	}
	
	/**
	 * Checks if the {@code effector} has a specific servitor NPC ID.<br>
	 * This method verifies that the {@code effector} is a {@code Player} with an active summon.<br>
	 * It returns {@code true} if any of the player's servitors match the IDs in this condition.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item involved in the action.
	 * @return {@code true} if a matching servitor is found, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		final Player player = effector.asPlayer();
		if ((player == null) || !player.hasSummon())
		{
			return false;
		}
		
		if (_npcIds == null)
		{
			return true;
		}
		
		for (Summon summon : effector.getServitors().values())
		{
			if (_npcIds.contains(summon.getId()))
			{
				return true;
			}
		}
		
		return false;
	}
}
