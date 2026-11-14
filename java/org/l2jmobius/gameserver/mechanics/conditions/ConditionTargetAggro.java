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
 * This condition checks if a specific target is currently aggressive towards the actor.<br>
 * It evaluates whether the {@code Creature} has an active aggro state.
 * @author mkizub
 */
public class ConditionTargetAggro extends Condition
{
	private final boolean _isAggro;
	
	/**
	 * Creates a new instance of {@code ConditionTargetAggro}.<br>
	 * This constructor sets the aggro state for the condition.
	 * @param isAggro The value to set for the aggro status. Use {@code true} or {@code false}.
	 */
	public ConditionTargetAggro(boolean isAggro)
	{
		_isAggro = isAggro;
	}
	
	/**
	 * Checks if the target meets specific aggression requirements.<br>
	 * It verifies if a monster is aggressive or if a player has a negative reputation.
	 * @param effector The creature performing the action.
	 * @param effected The creature being targeted.
	 * @param skill The skill being used.
	 * @param item The item template involved.
	 * @return {@code true} if the target meets the aggression condition, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		if (effected != null)
		{
			if (effected.isMonster())
			{
				return effected.asMonster().isAggressive() == _isAggro;
			}
			
			if (effected.isPlayer())
			{
				return effected.asPlayer().getReputation() < 0;
			}
		}
		
		return false;
	}
}
