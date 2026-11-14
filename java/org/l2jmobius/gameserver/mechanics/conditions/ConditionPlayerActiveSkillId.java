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
 * Checks if a player is currently using a specific skill ID.<br>
 * This condition verifies that the {@code Skill} being executed matches the required identifier.
 * @author DrHouse
 */
public class ConditionPlayerActiveSkillId extends Condition
{
	private final int _skillId;
	private final int _skillLevel;
	
	/**
	 * Creates a new condition based on an active skill ID.<br>
	 * This constructor sets the {@code _skillLevel} to -1 by default.
	 * @param skillId The unique identifier for the skill.
	 */
	public ConditionPlayerActiveSkillId(int skillId)
	{
		_skillId = skillId;
		_skillLevel = -1;
	}
	
	/**
	 * Creates a new condition to check for a specific active skill.<br>
	 * This constructor sets both the required skill ID and its level.
	 * @param skillId The unique identifier for the skill.
	 * @param skillLevel The required level of the skill.
	 */
	public ConditionPlayerActiveSkillId(int skillId, int skillLevel)
	{
		_skillId = skillId;
		_skillLevel = skillLevel;
	}
	
	/**
	 * Checks if the {@code effector} knows a specific skill.<br>
	 * It verifies if the skill level meets the required criteria.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being checked.
	 * @param item The item involved in the action.
	 * @return {@code true} if the condition is met, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		final Skill knownSkill = effector.getKnownSkill(_skillId);
		if (knownSkill != null)
		{
			return (_skillLevel == -1) || (_skillLevel <= knownSkill.getLevel());
		}
		
		return false;
	}
}
