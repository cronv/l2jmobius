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
package handlers.skill.conditions;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the target's weapon matches a specific {@code WeaponType}.<br>
 * This condition is used to restrict skill usage based on the equipment of the opponent.
 * @author UnAfraid
 */
public class OpTargetWeaponAttackTypeSkillCondition implements ISkillCondition
{
	private final Set<WeaponType> _weaponTypes = EnumSet.noneOf(WeaponType.class);
	
	/**
	 * Creates a new condition based on specific weapon types.<br>
	 * This constructor extracts the required types from the provided {@code StatSet}.<br>
	 * It populates the internal set of allowed {@code WeaponType} values.
	 * @param params The {@code StatSet} containing the "weaponType" list.
	 */
	public OpTargetWeaponAttackTypeSkillCondition(StatSet params)
	{
		final List<String> weaponTypes = params.getList("weaponType", String.class);
		if (weaponTypes != null)
		{
			for (String type : weaponTypes)
			{
				_weaponTypes.add(WeaponType.valueOf(type));
			}
		}
	}
	
	/**
	 * Checks if the target is holding a weapon of a specific type.<br>
	 * This method validates the requirements for using a skill based on the target's equipment.
	 * @param caster The {@code Creature} casting the skill.
	 * @param skill The {@code Skill} being attempted.
	 * @param target The {@code WorldObject} targeted by the skill.
	 * @return {@code true} if the target is a creature holding an allowed weapon type, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		if ((target == null) || !target.isCreature())
		{
			return false;
		}
		
		final Creature targetCreature = target.asCreature();
		final Weapon weapon = targetCreature.getActiveWeaponItem();
		if (weapon == null)
		{
			return false;
		}
		
		final WeaponType equippedType = weapon.getItemType();
		for (WeaponType weaponType : _weaponTypes)
		{
			if (weaponType == equippedType)
			{
				return true;
			}
		}
		
		return false;
	}
}
