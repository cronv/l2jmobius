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
package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This condition verifies the {@code AbnormalType} and level of a player.<br>
 * It is used to check if a creature meets specific abnormal status requirements.
 * @author Zoey76
 */
public class ConditionPlayerCheckAbnormal extends Condition
{
	private final AbnormalType _type;
	private final int _level;
	
	/**
	 * Creates a new condition to check for an abnormal status.<br>
	 * This constructor sets the required {@code AbnormalType}.<br>
	 * The level is initialized to {@code -1} by default.
	 * @param type the specific {@code AbnormalType} to check for
	 */
	public ConditionPlayerCheckAbnormal(AbnormalType type)
	{
		_type = type;
		_level = -1;
	}
	
	/**
	 * Creates a new condition to check for specific abnormal types.<br>
	 * This method sets the required {@code AbnormalType} and its minimum {@code level}.
	 * @param type The {@code AbnormalType} to verify.
	 * @param level The minimum {@code level} required for the abnormal effect.
	 */
	public ConditionPlayerCheckAbnormal(AbnormalType type, int level)
	{
		_type = type;
		_level = level;
	}
	
	/**
	 * Checks if the {@code effector} has a specific abnormal type.<br>
	 * It also verifies if the level of that effect meets the required minimum.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item involved in the action.
	 * @return {@code true} if the condition is met, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		if (_level == -1)
		{
			return effector.getEffectList().hasAbnormalType(_type);
		}
		
		return effector.getEffectList().hasAbnormalType(_type, info -> _level >= info.getSkill().getAbnormalLevel());
	}
}
