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
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the character's {@code maxSkill} stat is greater than or equal to a specific value.<br>
 * This condition is used to verify skill requirements based on the {@code Stat} system.
 * @author UnAfraid
 */
public class OpSoulMaxSkillCondition implements ISkillCondition
{
	/**
	 * Creates a new {@code OpSoulMaxSkillCondition} instance.<br>
	 * It uses the provided statistics to define the condition requirements.
	 * @param params The {@code StatSet} containing the required stats.
	 */
	public OpSoulMaxSkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks if the {@code caster} can use a specific {@code skill}.<br>
	 * This method verifies that the player is playable.<br>
	 * It also checks if the current charged souls are below the maximum limit.
	 * @param caster The {@code Creature} attempting to use the skill.
	 * @param skill The {@code Skill} being cast.
	 * @param target The {@code WorldObject} being targeted.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		final int maxSouls = (int) caster.getStat().getValue(Stat.MAX_SOULS);
		return caster.isPlayable() && (caster.asPlayer().getChargedSouls() < maxSouls);
	}
}
