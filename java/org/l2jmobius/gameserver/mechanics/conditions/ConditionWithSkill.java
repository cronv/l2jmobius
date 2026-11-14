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
 * This class represents a condition that is tied to a specific {@code Skill}.<br>
 * It allows for checking requirements or effects based on skill usage.<br>
 * Use this when a mechanic needs to evaluate whether a creature possesses or can use a particular skill.
 * @author Steuf
 */
public class ConditionWithSkill extends Condition
{
	private final boolean _skill;
	
	/**
	 * Creates a new instance of {@code ConditionWithSkill}.<br>
	 * This constructor sets whether the condition requires a skill.
	 * @param skill The boolean value indicating if a skill is required.
	 */
	public ConditionWithSkill(boolean skill)
	{
		_skill = skill;
	}
	
	/**
	 * Checks if the current condition matches the provided {@code Skill}.<br>
	 * It compares whether a {@code Skill} object is present against the internal requirement.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The {@code Skill} being checked.
	 * @param item The {@code ItemTemplate} involved in the action.
	 * @return {@code true} if the condition is met, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		return (skill != null) == _skill;
	}
}
