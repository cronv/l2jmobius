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
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This class defines a condition based on the type of an {@code NPC} target.<br>
 * It is used to check if a specific creature matches a required template or category.
 */
public class ConditionTargetNpcType extends Condition
{
	private final InstanceType[] _npcType;
	
	/**
	 * Creates a new instance of {@code ConditionTargetNpcType}.<br>
	 * This constructor sets the allowed NPC types for the condition.
	 * @param type An array of {@code InstanceType} values to include.
	 */
	public ConditionTargetNpcType(InstanceType[] type)
	{
		_npcType = type;
	}
	
	/**
	 * Checks if the target creature matches the required NPC types.<br>
	 * This method validates the {@code InstanceType} of the {@code effected} entity.<br>
	 * It returns {@code false} if the {@code effected} parameter is {@code null}.
	 * @param effector The creature performing the action.
	 * @param effected The creature being targeted by the action.
	 * @param skill The skill being used.
	 * @param item The item template associated with the action.
	 * @return {@code true} if the target matches the required types, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		if (effected == null)
		{
			return false;
		}
		
		return effected.getInstanceType().isTypes(_npcType);
	}
}
