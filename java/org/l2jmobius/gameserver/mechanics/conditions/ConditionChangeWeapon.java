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
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This class handles conditions related to weapon changes for an actor.<br>
 * It is used to check if a {@code Creature} has equipped or changed their current weapon.
 * @author nBd
 */
public class ConditionChangeWeapon extends Condition
{
	private final boolean _required;
	
	/**
	 * Creates a new instance of {@code ConditionChangeWeapon}.<br>
	 * This constructor sets the requirement status for the condition.
	 * @param required The value to determine if the weapon is mandatory.
	 */
	public ConditionChangeWeapon(boolean required)
	{
		_required = required;
	}
	
	/**
	 * Checks if the weapon requirements for a skill are met.<br>
	 * This method validates the {@code effector} and their active equipment.<br>
	 * It returns {@code false} if any conditions fail to be satisfied.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The template of the item involved.
	 * @return {@code true} if all requirements are met, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		final Player player = effector.asPlayer();
		if (player == null)
		{
			return false;
		}
		
		if (_required)
		{
			final Weapon weaponItem = effector.getActiveWeaponItem();
			if (weaponItem == null)
			{
				return false;
			}
			
			if (weaponItem.getChangeWeaponId() == 0)
			{
				return false;
			}
			
			if (player.hasItemRequest())
			{
				return false;
			}
		}
		
		return true;
	}
}
