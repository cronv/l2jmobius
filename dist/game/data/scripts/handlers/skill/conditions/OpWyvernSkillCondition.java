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
import org.l2jmobius.gameserver.entity.actor.enums.player.MountType;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class checks if a specific condition is met for using skills related to the Wyvern.<br>
 * It ensures that only valid actions can be performed based on the {@code OpWyvern} mechanics.
 * @author UnAfraid
 */
public class OpWyvernSkillCondition implements ISkillCondition
{
	/**
	 * Creates a new {@code OpWyvernSkillCondition} instance.<br>
	 * This constructor initializes the condition using the provided stats.
	 * @param params The {@code StatSet} containing the required values.
	 */
	public OpWyvernSkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks if the {@code caster} is allowed to use a specific {@code skill}.<br>
	 * This condition requires the {@code caster} to be a player.<br>
	 * The player must also be riding a {@code MountType.WYVERN}.
	 * @param caster The entity attempting to cast the skill.
	 * @param skill The skill being attempted.
	 * @param target The object being targeted by the skill.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		return caster.isPlayer() && (caster.asPlayer().getMountType() == MountType.WYVERN);
	}
}
