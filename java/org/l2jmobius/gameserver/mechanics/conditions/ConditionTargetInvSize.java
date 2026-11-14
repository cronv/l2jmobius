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
 * Checks if the target's inventory size meets a specific requirement.<br>
 * This condition is used to verify the capacity of an actor's inventory.
 * @author Zoey76
 */
public class ConditionTargetInvSize extends Condition
{
	private final int _size;
	
	/**
	 * Creates a new {@code ConditionTargetInvSize} instance.<br>
	 * This condition checks if an inventory meets a specific size requirement.
	 * @param size The required inventory size to check against.
	 */
	public ConditionTargetInvSize(int size)
	{
		_size = size;
	}
	
	/**
	 * Checks if the target player has enough inventory space.<br>
	 * It verifies if the current non-quest items plus the required size fit within the limit.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used.
	 * @param item The item template involved in the check.
	 * @return {@code true} if there is enough space, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		if ((effected != null) && effected.isPlayer())
		{
			final Player target = effected.asPlayer();
			return target.getInventory().getNonQuestSize() <= (target.getInventoryLimit() - _size);
		}
		
		return false;
	}
}
