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
 * Checks if a player is currently inside an instance.<br>
 * This condition returns {@code true} if the actor is within a specific instance zone.
 * @author Sdw
 */
public class ConditionPlayerInInstance extends Condition
{
	public boolean _inInstance;
	
	/**
	 * Creates a new {@code ConditionPlayerInInstance} object.<br>
	 * This constructor sets the instance status for the condition.
	 * @param inInstance The boolean value indicating if the player is inside an instance.
	 */
	public ConditionPlayerInInstance(boolean inInstance)
	{
		_inInstance = inInstance;
	}
	
	/**
	 * Checks if the condition is met based on whether the player is in an instance.<br>
	 * This method returns {@code false} if the {@code effector} is not a player.<br>
	 * It compares the current instance status against the required state.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item involved in the action.
	 * @return {@code true} if the condition is satisfied, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		if (effector.asPlayer() == null)
		{
			return false;
		}
		
		return (effector.getInstanceId() == 0) ? !_inInstance : _inInstance;
	}
}
