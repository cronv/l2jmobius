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
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the player has a specific sub-job active.<br>
 * This condition is used to restrict skill usage based on the character's current job status.
 * @author UnAfraid
 */
public class OpSubjobSkillCondition implements ISkillCondition
{
	/**
	 * Creates a new instance of {@code OpSubjobSkillCondition}.<br>
	 * This constructor initializes the condition using the provided statistics.
	 * @param params The {@code StatSet} containing the required parameters.
	 */
	public OpSubjobSkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks if the {@code caster} is allowed to use a specific {@code skill}.<br>
	 * This method verifies that the {@code caster} is a player and has an active sub-class.
	 * @param caster The {@code Creature} attempting to cast the skill.
	 * @param skill The {@code Skill} being used.
	 * @param target The {@code WorldObject} targeted by the skill.
	 * @return {@code true} if the player has an active sub-class, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		return caster.isPlayer() && caster.asPlayer().isSubClassActive();
	}
}
