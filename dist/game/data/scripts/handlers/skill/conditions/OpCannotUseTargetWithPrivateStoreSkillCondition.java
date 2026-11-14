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
 * This condition prevents a skill from being used on targets that have a private store.<br>
 * It checks if the target {@code Creature} possesses a private store before allowing the action.
 * @author Sdw
 */
public class OpCannotUseTargetWithPrivateStoreSkillCondition implements ISkillCondition
{
	/**
	 * This constructor initializes the condition with specific statistics.<br>
	 * It uses a {@code StatSet} to define the required parameters.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public OpCannotUseTargetWithPrivateStoreSkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks if the {@code caster} can use a specific {@code skill} on a {@code target}.<br>
	 * This condition prevents using skills on players who are currently in store mode.<br>
	 * It returns {@code true} if the {@code target} is null or not a player.
	 * @param caster The creature attempting to use the skill.
	 * @param skill The skill being cast.
	 * @param target The object being targeted by the skill.
	 * @return {@code true} if the skill can be used, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		return (target == null) || !target.isPlayer() || !target.asPlayer().isInStoreMode();
	}
}
