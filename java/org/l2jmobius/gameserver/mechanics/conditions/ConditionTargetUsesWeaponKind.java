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
 * Checks if the target of a condition is currently using a specific kind of weapon.<br>
 * This {@code Condition} evaluates whether the target's equipped weapon matches the required type.
 * @author mkizub
 */
public class ConditionTargetUsesWeaponKind extends Condition
{
	private final int _weaponMask;
	
	/**
	 * Creates a new condition that checks for specific weapon types.<br>
	 * It uses a bitmask to determine which weapons are allowed.
	 * @param weaponMask the integer mask representing allowed weapon kinds
	 */
	public ConditionTargetUsesWeaponKind(int weaponMask)
	{
		_weaponMask = weaponMask;
	}
	
	/**
	 * Checks if the target is using a specific type of weapon.<br>
	 * This method verifies the active weapon of the {@code effected} creature.<br>
	 * It compares the weapon's mask against the internal {@code _weaponMask}.
	 * @param effector The creature performing the action.
	 * @param effected The creature being targeted by the action.
	 * @param skill The skill being executed.
	 * @param item The item template associated with the action.
	 * @return {@code true} if the target's weapon matches the required mask, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		if (effected == null)
		{
			return false;
		}
		
		final Weapon weapon = effected.getActiveWeaponItem();
		if (weapon == null)
		{
			return false;
		}
		
		return (weapon.getItemType().mask() & _weaponMask) != 0;
	}
}
