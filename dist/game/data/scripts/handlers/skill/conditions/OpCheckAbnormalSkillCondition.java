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
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillConditionAffectType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if a skill can be cast based on the {@code AbnormalType} of the target.<br>
 * This condition validates whether the current state allows for specific abnormal effects.
 * @author UnAfraid
 */
public class OpCheckAbnormalSkillCondition implements ISkillCondition
{
	private final AbnormalType _type;
	private final int _level;
	private final boolean _hasAbnormal;
	private final SkillConditionAffectType _affectType;
	
	/**
	 * Creates a new {@code OpCheckAbnormalSkillCondition} using the provided parameters.<br>
	 * This constructor initializes the condition type, level, and affect type from a {@code StatSet}.
	 * @param params The {@code StatSet} containing all required configuration values.
	 */
	public OpCheckAbnormalSkillCondition(StatSet params)
	{
		_type = params.getEnum("type", AbnormalType.class);
		_level = params.getInt("level");
		_hasAbnormal = params.getBoolean("hasAbnormal");
		_affectType = params.getEnum("affectType", SkillConditionAffectType.class, SkillConditionAffectType.TARGET);
	}
	
	/**
	 * Checks if the skill can be used based on abnormal status conditions.<br>
	 * It verifies if the {@code caster} or {@code target} has a specific abnormal type.<br>
	 * The check depends on the required level and the expected presence of the effect.
	 * @param caster The creature casting the skill.
	 * @param skill The skill being attempted.
	 * @param target The object targeted by the skill.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		switch (_affectType)
		{
			case CASTER:
			{
				return caster.getEffectList().hasAbnormalType(_type, info -> (info.getSkill().getAbnormalLevel() >= _level)) == _hasAbnormal;
			}
			case TARGET:
			{
				if ((target != null) && target.isCreature())
				{
					return target.asCreature().getEffectList().hasAbnormalType(_type, info -> (info.getSkill().getAbnormalLevel() >= _level)) == _hasAbnormal;
				}
			}
		}
		
		return false;
	}
}
