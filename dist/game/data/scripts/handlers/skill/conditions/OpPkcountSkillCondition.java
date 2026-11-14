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
 * Checks if the target's PK count meets a specific requirement.<br>
 * This condition is used to restrict skill usage based on the {@code Creature}'s PK status.
 * @author UnAfraid
 */
public class OpPkcountSkillCondition implements ISkillCondition
{
	private final SkillConditionAffectType _affectType;
	
	/**
	 * Creates a new {@code OpPkcountSkillCondition} instance.<br>
	 * It initializes the condition using the provided stats.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public OpPkcountSkillCondition(StatSet params)
	{
		_affectType = params.getEnum("affectType", SkillConditionAffectType.class);
	}
	
	/**
	 * Checks if the skill can be used based on PK kill counts.<br>
	 * It verifies if either the {@code caster} or the {@code target} has at least 1 PK kill.<br>
	 * This check depends on the specific {@code SkillConditionAffectType} configured for this condition.
	 * @param caster The creature attempting to use the skill.
	 * @param skill The skill being cast.
	 * @param target The object being targeted by the skill.
	 * @return {@code true} if the conditions are met, {@code false} otherwise.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		switch (_affectType)
		{
			case CASTER:
			{
				return caster.isPlayer() && (caster.asPlayer().getPkKills() > 0);
			}
			case TARGET:
			{
				if ((target != null) && target.isPlayer())
				{
					return target.asPlayer().getPkKills() > 0;
				}
				break;
			}
		}
		
		return false;
	}
}
