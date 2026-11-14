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
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * Checks if a creature is currently using an item of a specific slot type.<br>
 * This condition verifies that the {@code Creature} has an active {@code Weapon} or other equipment in the required category.
 * @author NosBit
 */
public class ConditionUsingSlotType extends Condition
{
	private final int _mask;
	
	/**
	 * Creates a new {@code ConditionUsingSlotType} instance.<br>
	 * This constructor sets the required slot type bitmask.
	 * @param mask The integer bitmask representing the allowed slot types.
	 */
	public ConditionUsingSlotType(int mask)
	{
		_mask = mask;
	}
	
	/**
	 * Checks if the effector's active weapon matches a specific slot type.<br>
	 * This method verifies that the {@code effector} is a player and has an active weapon.<br>
	 * It then compares the weapon's body part mask against the internal {@code _mask}.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item template associated with the action.
	 * @return {@code true} if the weapon matches the required mask, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		if ((effector == null) || !effector.isPlayer())
		{
			return false;
		}
		
		final Weapon activeWeapon = effector.getActiveWeaponItem();
		if (activeWeapon == null)
		{
			return false;
		}
		
		return (activeWeapon.getBodyPart().getMask() & _mask) != 0;
	}
}
