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
import org.l2jmobius.gameserver.entity.actor.instance.Chest;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if a skill can be unlocked based on specific requirements.<br>
 * This condition validates whether the player meets the criteria to learn a new {@code Skill}.
 * @author Sdw
 */
public class OpUnlockSkillCondition implements ISkillCondition
{
	/**
	 * Creates a new {@code OpUnlockSkillCondition} instance.<br>
	 * This constructor initializes the condition using specific statistics.
	 * @param params The {@code StatSet} containing the required values.
	 */
	public OpUnlockSkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks if a {@code Skill} can be used on a specific {@code WorldObject}.<br>
	 * The target must not be {@code null}.<br>
	 * The target must be a door or an instance of {@code Chest}.
	 * @param caster The creature casting the skill.
	 * @param skill The skill being attempted.
	 * @param target The object being targeted by the skill.
	 * @return {@code true} if the conditions are met, {@code false} otherwise.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		return (target != null) && (target.isDoor() || (target instanceof Chest));
	}
}
