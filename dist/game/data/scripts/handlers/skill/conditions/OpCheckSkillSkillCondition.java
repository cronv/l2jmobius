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
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillConditionAffectType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class checks if a specific skill is currently active or available for use.<br>
 * It implements the {@code ISkillCondition} interface to validate skill requirements.<br>
 * Use this condition when a skill's execution depends on another skill's state.
 * @author Sdw
 */
public class OpCheckSkillSkillCondition implements ISkillCondition
{
	private final int _skillId;
	private final SkillConditionAffectType _affectType;
	
	/**
	 * Creates a new {@code OpCheckSkillSkillCondition} instance.<br>
	 * This constructor initializes the condition using values from a {@code StatSet}.<br>
	 * It extracts the required skill ID and affect type for validation.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public OpCheckSkillSkillCondition(StatSet params)
	{
		_skillId = params.getInt("skillId");
		_affectType = params.getEnum("affectType", SkillConditionAffectType.class);
	}
	
	/**
	 * Checks if the skill can be used based on specific requirements.<br>
	 * It verifies if either the {@code caster} or the {@code target} has a certain skill level.
	 * @param caster The creature casting the skill.
	 * @param skill The skill being attempted.
	 * @param target The object being targeted by the skill.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		switch (_affectType)
		{
			case CASTER:
			{
				return caster.getSkillLevel(_skillId) > 0;
			}
			case TARGET:
			{
				if ((target != null) && !target.isPlayer())
				{
					return target.asPlayer().getSkillLevel(_skillId) > 0;
				}
				break;
			}
		}
		
		return false;
	}
}
