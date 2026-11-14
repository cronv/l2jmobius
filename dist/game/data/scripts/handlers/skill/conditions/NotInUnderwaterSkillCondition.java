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
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if a creature is not currently in an underwater state.<br>
 * This condition returns {@code true} if the target is above water.
 * @author UnAfraid
 */
public class NotInUnderwaterSkillCondition implements ISkillCondition
{
	/**
	 * Creates a new condition to check if the user is not underwater.<br>
	 * This constructor initializes the condition using provided statistics.
	 * @param params The {@code StatSet} containing the required parameters.
	 */
	public NotInUnderwaterSkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks if the {@code caster} is allowed to use a specific {@code skill}.<br>
	 * This method returns {@code true} only if the {@code caster} is not in an underwater zone.
	 * @param caster The {@code Creature} attempting to cast the skill.
	 * @param skill The {@code Skill} being used.
	 * @param target The {@code WorldObject} targeted by the skill.
	 * @return {@code true} if the caster is not underwater, {@code false} otherwise.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		return !caster.isInsideZone(ZoneId.WATER);
	}
}
