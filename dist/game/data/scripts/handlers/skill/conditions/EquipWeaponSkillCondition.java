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

import java.util.List;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if a player is currently wielding a weapon of a specific type.<br>
 * This condition ensures that a {@code Skill} can only be used when the required equipment is equipped.
 * @author Sdw
 */
public class EquipWeaponSkillCondition implements ISkillCondition
{
	private int _weaponTypesMask = 0;
	
	/**
	 * Creates a new condition to check if a specific weapon is equipped.<br>
	 * It uses the provided {@code StatSet} to determine which {@code WeaponType} masks are required.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public EquipWeaponSkillCondition(StatSet params)
	{
		final List<WeaponType> weaponTypes = params.getEnumList("weaponType", WeaponType.class);
		if (weaponTypes != null)
		{
			for (WeaponType weaponType : weaponTypes)
			{
				_weaponTypesMask |= weaponType.mask();
			}
		}
	}
	
	/**
	 * Checks if the {@code caster} is holding a valid weapon.<br>
	 * The weapon must match the required types defined in this condition.
	 * @param caster The creature attempting to use the skill.
	 * @param skill The skill being cast.
	 * @param target The object being targeted by the skill.
	 * @return {@code true} if the requirements are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		final ItemTemplate weapon = caster.getActiveWeaponItem();
		return (weapon != null) && ((weapon.getItemMask() & _weaponTypesMask) != 0);
	}
}
