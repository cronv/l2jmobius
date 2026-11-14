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
 * Checks if a creature possesses the required skill to perform a summon.<br>
 * This condition is used to validate whether an actor can execute specific summoning abilities.
 * @author UnAfraid
 */
public class OpHaveSummonSkillCondition implements ISkillCondition
{
	/**
	 * Creates a new condition to check if a player has a summon skill.<br>
	 * It uses the provided {@code StatSet} to define the required criteria.
	 * @param params The {@code StatSet} containing the necessary requirements.
	 */
	public OpHaveSummonSkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks if the {@code caster} is allowed to use a specific {@code skill}.<br>
	 * This method verifies if the {@code caster} currently has any active summons.
	 * @param caster The {@code Creature} attempting to use the skill.
	 * @param skill The {@code Skill} being attempted.
	 * @param target The {@code WorldObject} targeted by the skill.
	 * @return {@code true} if the {@code caster} has summons, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		return caster.hasServitors();
	}
}
