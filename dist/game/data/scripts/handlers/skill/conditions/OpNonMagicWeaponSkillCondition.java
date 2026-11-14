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

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the skill can be used with a non-magic weapon.<br>
 * This condition ensures that only physical weapons satisfy the requirement for {@code Skill}.
 * @author Mobius
 */
public class OpNonMagicWeaponSkillCondition implements ISkillCondition
{
	/**
	 * Creates a new condition for skills that require non-magic weapons.<br>
	 * This constructor initializes the condition using a set of statistics.
	 * @param params The {@code StatSet} containing the required parameters.
	 */
	public OpNonMagicWeaponSkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks if the {@code caster} can use a specific skill based on their current weapon.<br>
	 * The method verifies that the {@code caster} is holding a non-magic weapon.
	 * @param caster The creature attempting to use the skill.
	 * @param skill The skill being attempted.
	 * @param target The object targeted by the skill.
	 * @return {@code true} if the caster holds a valid non-magic weapon, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		final Weapon weapon = caster.getActiveWeaponItem();
		return (weapon != null) && !weapon.isMagicWeapon();
	}
}
