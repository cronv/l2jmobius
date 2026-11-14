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
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.type.ArmorType;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the character is currently equipped with a shield.<br>
 * This condition ensures that a {@code Skill} can only be used when a valid shield is active.
 * @author UnAfraid
 */
public class EquipShieldSkillCondition implements ISkillCondition
{
	/**
	 * Creates a new {@code EquipShieldSkillCondition} instance.<br>
	 * This constructor initializes the condition using the provided stats.
	 * @param params The {@code StatSet} containing the required statistics.
	 */
	public EquipShieldSkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks if the {@code caster} is currently holding a valid shield.<br>
	 * This method verifies that the secondary weapon item exists and is of type {@code SHIELD}.
	 * @param caster The creature attempting to use the skill.
	 * @param skill The skill being cast.
	 * @param target The object targeted by the skill.
	 * @return {@code true} if a shield is equipped, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		final ItemTemplate shield = caster.getSecondaryWeaponItem();
		return (shield != null) && (shield.getItemType() == ArmorType.SHIELD);
	}
}
