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
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillConditionAlignment;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if a skill can be cast based on the alignment of the target.<br>
 * This condition validates whether the {@code Skill} requirements match the {@code SkillConditionAlignment}.<br>
 * It ensures that only compatible alignments are allowed to trigger the effect.
 * @author Sdw
 */
public class OpAlignmentSkillCondition implements ISkillCondition
{
	private final SkillConditionAffectType _affectType;
	private final SkillConditionAlignment _alignment;
	
	/**
	 * Creates a new {@code OpAlignmentSkillCondition} using the provided statistics.<br>
	 * This constructor initializes the affect type and alignment from the {@code StatSet}.
	 * @param params The {@code StatSet} containing the required configuration values.
	 */
	public OpAlignmentSkillCondition(StatSet params)
	{
		_affectType = params.getEnum("affectType", SkillConditionAffectType.class);
		_alignment = params.getEnum("alignment", SkillConditionAlignment.class);
	}
	
	/**
	 * Checks if the skill can be used based on alignment requirements.<br>
	 * It validates the {@code Creature} or {@code WorldObject} against specific rules.
	 * @param caster The entity casting the skill.
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
				return _alignment.test(caster.asPlayer());
			}
			case TARGET:
			{
				if ((target != null) && target.isPlayer())
				{
					return _alignment.test(target.asPlayer());
				}
				break;
			}
		}
		
		return false;
	}
}
