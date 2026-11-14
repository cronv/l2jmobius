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
import org.l2jmobius.gameserver.entity.actor.instance.AirShip;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if a skill can be used while the player is on an {@code AirShip}.<br>
 * This condition prevents players from using specific skills during airship travel.<br>
 * It returns {@code false} if the player is currently riding an {@code AirShip}.
 * @author UnAfraid
 */
public class OpCanNotUseAirshipSkillCondition implements ISkillCondition
{
	/**
	 * Creates a new condition to check if an airship skill can be used.<br>
	 * This constructor initializes the condition using the provided stats.
	 * @param params The {@code StatSet} containing the required parameters.
	 */
	public OpCanNotUseAirshipSkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks if the {@code caster} is allowed to use a specific {@code skill}.<br>
	 * This condition returns {@code true} only if the {@code caster} is a player.<br>
	 * It also ensures the player is not currently riding an {@code AirShip}.
	 * @param caster The entity attempting to cast the skill.
	 * @param skill The skill being attempted.
	 * @param target The object targeted by the skill.
	 * @return {@code true} if the conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		return caster.isPlayer() && !(caster.asPlayer().getVehicle() instanceof AirShip);
	}
}
