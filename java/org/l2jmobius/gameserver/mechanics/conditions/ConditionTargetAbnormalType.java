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
 * This class represents a condition that checks for specific abnormal types on a target.<br>
 * It is used to verify if a {@code Creature} possesses an {@code AbnormalType} defined by a {@link org.l2jmobius.gameserver.mechanics.skill.Skill}.
 * @author janiii
 */
public class ConditionTargetAbnormalType extends Condition
{
	private final AbnormalType _abnormalType;
	
	/**
	 * Creates a new instance of {@code ConditionTargetAbnormalType}.<br>
	 * This constructor sets the required {@code AbnormalType} for the condition.
	 * @param abnormalType The specific type of abnormality to check.
	 */
	public ConditionTargetAbnormalType(AbnormalType abnormalType)
	{
		_abnormalType = abnormalType;
	}
	
	/**
	 * Checks if the target creature has a specific abnormal type.<br>
	 * This method validates the condition for a skill execution.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used.
	 * @param item The item associated with the skill.
	 * @return {@code true} if the condition is met, {@code false} otherwise.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		return effected.hasAbnormalType(_abnormalType);
	}
}
