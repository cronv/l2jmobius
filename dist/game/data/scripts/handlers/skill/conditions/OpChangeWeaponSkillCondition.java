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

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if a skill can be used based on whether the player has changed their equipped weapon.<br>
 * This condition validates specific weapon requirements for {@code Skill} execution.
 * @author Sdw
 */
public class OpChangeWeaponSkillCondition implements ISkillCondition
{
	/**
	 * Creates a new condition for checking weapon skills.<br>
	 * This constructor initializes the requirements using a {@code StatSet}.
	 * @param params The set of statistics used to define the condition.
	 */
	public OpChangeWeaponSkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks if the caster can use a specific skill based on their current weapon.<br>
	 * The caster must have an active {@code Weapon}.<br>
	 * The weapon must have a valid change ID.<br>
	 * The caster cannot be currently requesting an item.
	 * @param caster The creature attempting to use the skill.
	 * @param skill The skill being used.
	 * @param target The object targeted by the skill.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		final Weapon weaponItem = caster.getActiveWeaponItem();
		if (weaponItem == null)
		{
			return false;
		}
		
		if (weaponItem.getChangeWeaponId() == 0)
		{
			return false;
		}
		
		if (caster.asPlayer().hasItemRequest())
		{
			return false;
		}
		
		return true;
	}
}
