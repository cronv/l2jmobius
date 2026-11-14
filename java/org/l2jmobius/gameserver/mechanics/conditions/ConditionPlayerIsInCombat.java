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
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;

/**
 * Checks if a player is currently engaged in combat.<br>
 * This condition returns {@code true} if the actor is actively fighting an enemy.
 * @author Sdw
 */
public class ConditionPlayerIsInCombat extends Condition
{
	private final boolean _value;
	
	/**
	 * Creates a new condition to check if a player is in combat.<br>
	 * This determines whether the {@code Creature} is currently engaged in fighting.
	 * @param value The required state for the combat check. Use {@code true} to require combat and {@code false} otherwise.
	 */
	public ConditionPlayerIsInCombat(boolean value)
	{
		_value = value;
	}
	
	/**
	 * Checks if the {@code effector} is currently in combat.<br>
	 * This method compares the current status against a predefined value.<br>
	 * It returns {@code true} if the condition matches.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item template involved in the action.
	 * @return {@code true} if the combat status matches the required value, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		final boolean isInCombat = !AttackStanceTaskManager.getInstance().hasAttackStanceTask(effector);
		return _value == isInCombat;
	}
}
