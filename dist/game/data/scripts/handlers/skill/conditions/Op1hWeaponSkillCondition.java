/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package handlers.skill.conditions;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the player is wielding a one-handed weapon.<br>
 * This condition ensures that a skill can only be used when a {@code Weapon} of type {@code WeaponType.ONE_HANDED} is equipped.
 * @author Mobius
 */
public class Op1hWeaponSkillCondition implements ISkillCondition
{
	private final Set<WeaponType> _weaponTypes = EnumSet.noneOf(WeaponType.class);
	
	/**
	 * Creates a new condition for one-handed weapons.<br>
	 * This constructor initializes the allowed {@code WeaponType} values.<br>
	 * It reads these types from the provided {@code StatSet}.
	 * @param params The {@code StatSet} containing the required weapon type strings.
	 */
	public Op1hWeaponSkillCondition(StatSet params)
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
	 * Checks if the caster can use a specific skill based on their equipped weapon.<br>
	 * This method verifies that the active weapon is of an allowed type.<br>
	 * It also ensures the weapon is not held in the primary hand for certain types.
	 * @param caster The {@code Creature} attempting to use the skill.
	 * @param skill The {@code Skill} being cast.
	 * @param target The {@code WorldObject} targeted by the skill.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		final Weapon weapon = caster.getActiveWeaponItem();
		if (weapon == null)
		{
			return false;
		}
		
		// An empty type list leaves the slot as the only requirement, the skill declares its weapon types separately.
		if (!_weaponTypes.isEmpty() && !_weaponTypes.contains(weapon.getItemType()))
		{
			return false;
		}
		
		return weapon.getBodyPart() != BodyPart.LR_HAND;
	}
}
