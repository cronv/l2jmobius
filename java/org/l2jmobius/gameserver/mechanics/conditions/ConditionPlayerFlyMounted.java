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
 * Checks if a player is currently flying while mounted on another creature.<br>
 * This condition returns {@code true} only when both flight and mounting requirements are met.
 * @author Kerberos
 */
public class ConditionPlayerFlyMounted extends Condition
{
	private final boolean _value;
	
	/**
	 * Creates a new instance of {@code ConditionPlayerFlyMounted}.<br>
	 * This condition checks if a player is currently flying while mounted.
	 * @param value the boolean state to set for this condition.
	 */
	public ConditionPlayerFlyMounted(boolean value)
	{
		_value = value;
	}
	
	/**
	 * Checks if the condition is met based on the effector's flying status.<br>
	 * This method returns {@code true} if the effector is not a player.<br>
	 * If the effector is a player, it compares their flying mounted state to the stored value.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item template involved in the action.
	 * @return {@code true} if the condition is satisfied, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		return !effector.isPlayer() || (effector.asPlayer().isFlyingMounted() == _value);
	}
}
