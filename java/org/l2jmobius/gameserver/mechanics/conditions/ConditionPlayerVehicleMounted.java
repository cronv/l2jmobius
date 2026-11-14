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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * Checks if a {@code Player} is currently mounted on a vehicle.<br>
 * This condition returns {@code true} if the player is riding a mount or vehicle.<br>
 * It is used to verify movement states for various game mechanics.
 * @author Nyaran
 */
public class ConditionPlayerVehicleMounted extends Condition
{
	private final boolean _value;
	
	/**
	 * Checks if a player is currently mounted on a vehicle.<br>
	 * This condition determines the required mounting state.
	 * @param value The required mounting status, such as {@code true} or {@code false}.
	 */
	public ConditionPlayerVehicleMounted(boolean value)
	{
		_value = value;
	}
	
	/**
	 * Checks if the {@code effector} is currently mounted on a vehicle.<br>
	 * This method compares the current state against the stored {@code _value}.<br>
	 * It returns {@code true} if the player is not a {@code Player} instance.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item template involved in the action.
	 * @return {@code true} if the condition is met, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		final Player player = effector.asPlayer();
		if (player == null)
		{
			return true;
		}
		
		return player.isInVehicle() == _value;
	}
}
