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

import java.util.Set;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This condition checks if a target is a specific NPC based on its unique ID.<br>
 * It allows for precise filtering of NPCs in game mechanics.
 */
public class ConditionTargetNpcId extends Condition
{
	private final Set<Integer> _npcIds;
	
	/**
	 * Creates a new instance of {@code ConditionTargetNpcId}.<br>
	 * This condition checks if an entity matches specific NPC IDs.
	 * @param npcIds A {@code Set} of unique integer IDs for the target NPCs.
	 */
	public ConditionTargetNpcId(Set<Integer> npcIds)
	{
		_npcIds = npcIds;
	}
	
	/**
	 * Checks if the target creature matches a specific NPC ID.<br>
	 * This method verifies if the {@code effected} entity is an NPC or door.<br>
	 * It then checks if its ID exists within the allowed set.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item involved in the action.
	 * @return {@code true} if the target is a valid NPC from the list, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		if ((effected != null) && (effected.isNpc() || effected.isDoor()))
		{
			return _npcIds.contains(effected.getId());
		}
		
		return false;
	}
}
