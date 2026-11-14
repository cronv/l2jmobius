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
 * This condition checks if a target belongs to a specific set of allowed classes.<br>
 * It restricts the execution of a mechanic based on the {@code ClassId} of the target creature.
 */
public class ConditionTargetClassIdRestriction extends Condition
{
	private final Set<Integer> _classIds;
	
	/**
	 * Creates a new restriction based on specific class IDs.<br>
	 * This condition checks if the target belongs to the allowed classes.
	 * @param classId A {@code Set} of {@code Integer} values representing the allowed class IDs.
	 */
	public ConditionTargetClassIdRestriction(Set<Integer> classId)
	{
		_classIds = classId;
	}
	
	/**
	 * Checks if the target creature meets the class ID requirements.<br>
	 * This method verifies if the {@code effected} creature is a player.<br>
	 * It also checks if their class ID exists in the allowed set.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item involved in the action.
	 * @return {@code true} if the target is a valid player with an allowed class ID, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		return effected.isPlayer() && _classIds.contains(effected.asPlayer().getPlayerClass().getId());
	}
}
