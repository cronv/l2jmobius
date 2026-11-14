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
 * Checks if a player belongs to a specific set of allowed class IDs.<br>
 * This condition restricts actions or mechanics based on the character's class.<br>
 * It is used to ensure only certain classes can perform specific activities.
 */
public class ConditionPlayerClassIdRestriction extends Condition
{
	private final Set<Integer> _classIds;
	
	/**
	 * Creates a new restriction based on allowed player class IDs.<br>
	 * This condition checks if a player belongs to a specific set of classes.
	 * @param classId A {@code Set<Integer>} containing the permitted class IDs.
	 */
	public ConditionPlayerClassIdRestriction(Set<Integer> classId)
	{
		_classIds = classId;
	}
	
	/**
	 * Checks if the {@code effector} is a player with a restricted class ID.<br>
	 * This method validates the character's class against the allowed list.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item involved in the action.
	 * @return {@code true} if the player belongs to an allowed class, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		return effector.isPlayer() && _classIds.contains(effector.asPlayer().getPlayerClass().getId());
	}
}
