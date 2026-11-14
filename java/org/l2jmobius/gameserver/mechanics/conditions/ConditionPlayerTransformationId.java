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
 * Checks if a player is currently in a transformed state.<br>
 * It returns {@code true} if the player's transformation ID matches the provided parameter.<br>
 * If the parameter is {@code -1}, it returns {@code true} for any active transformation.
 * @author Zoey76
 */
public class ConditionPlayerTransformationId extends Condition
{
	private final int _id;
	
	/**
	 * Creates a new instance of {@code ConditionPlayerTransformationId}.<br>
	 * This condition checks if a player is in a specific transformation.<br>
	 * It uses the provided {@code id} to validate the transformation type.
	 * @param id The unique identifier for the transformation.
	 */
	public ConditionPlayerTransformationId(int id)
	{
		_id = id;
	}
	
	/**
	 * Checks if the {@code effector} is in a specific transformation.<br>
	 * It returns {@code true} if the ID matches or if the required ID is -1.<br>
	 * This method evaluates the current state of the {@code Creature}.
	 * @param effector The creature being checked for the transformation.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used.
	 * @param item The item template involved in the action.
	 * @return {@code true} if the condition is met, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		return _id == -1 ? effector.isTransformed() : effector.getTransformationId() == _id;
	}
}
