/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This condition checks if a {@code Creature} is currently using a specific {@code Skill}.<br>
 * It allows for mechanics to be triggered based on the active skill of an entity.
 * @author mkizub
 */
public class ConditionUsingSkill extends Condition
{
	private final int _skillId;
	
	/**
	 * Creates a new instance of {@code ConditionUsingSkill}.<br>
	 * This constructor sets the required skill ID for the condition.
	 * @param skillId The unique identifier for the skill.
	 */
	public ConditionUsingSkill(int skillId)
	{
		_skillId = skillId;
	}
	
	/**
	 * Checks if the provided {@code Skill} matches the required ID.<br>
	 * Returns {@code false} if the {@code Skill} is {@code null}.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used to check the condition.
	 * @param item The item template associated with the action.
	 * @return {@code true} if the skill ID matches, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		if (skill == null)
		{
			return false;
		}
		
		return skill.getId() == _skillId;
	}
}
