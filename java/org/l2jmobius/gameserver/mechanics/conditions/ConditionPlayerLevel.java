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
 * This class checks if a player meets a specific level requirement.<br>
 * It is used to validate the character's level against a required value.
 * @author mkizub
 */
public class ConditionPlayerLevel extends Condition
{
	private final int _level;
	
	/**
	 * Creates a new instance of {@code ConditionPlayerLevel}.<br>
	 * This condition checks if an actor meets a specific level requirement.
	 * @param level The minimum level required to satisfy the condition.
	 */
	public ConditionPlayerLevel(int level)
	{
		_level = level;
	}
	
	/**
	 * Checks if the {@code effector} meets the required level.<br>
	 * This method compares the {@code effector} level against the internal requirement.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item involved in the action.
	 * @return {@code true} if the level requirement is met, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		return effector.getLevel() >= _level;
	}
}
