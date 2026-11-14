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
 * Checks if a player is currently unable to move.<br>
 * This condition returns {@code true} when the actor's movement is restricted.
 * @author Sdw
 */
public class ConditionPlayerImmobile extends Condition
{
	private final boolean _value;
	
	/**
	 * Creates a new {@code ConditionPlayerImmobile} instance.<br>
	 * This condition checks if a player is unable to move.
	 * @param value The state of the immobility condition. Use {@code true} for immobile and {@code false} for mobile.
	 */
	public ConditionPlayerImmobile(boolean value)
	{
		_value = value;
	}
	
	/**
	 * Checks if the movement state of a creature matches a specific condition.<br>
	 * This method compares whether the {@code effector} can move against the stored value.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item involved in the action.
	 * @return {@code true} if the movement state matches the condition, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		final boolean isImmobile = !effector.isMovementDisabled();
		return _value == isImmobile;
	}
}
