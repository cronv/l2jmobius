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
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillConditionPercentType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the remaining CP of a skill meets a specific percentage requirement.<br>
 * This condition is used to validate skill usage based on the current {@code Skill} progress.
 * @author UnAfraid
 */
public class RemainCpPerSkillCondition implements ISkillCondition
{
	private final int _amount;
	private final SkillConditionPercentType _percentType;
	
	/**
	 * Creates a new condition based on remaining CP.<br>
	 * It extracts the required amount and type from the provided {@code StatSet}.
	 * @param params The {@code StatSet} containing configuration values.
	 */
	public RemainCpPerSkillCondition(StatSet params)
	{
		_amount = params.getInt("amount");
		_percentType = params.getEnum("percentType", SkillConditionPercentType.class);
	}
	
	/**
	 * Checks if the {@code caster} meets the required CP requirements.<br>
	 * It compares the current CP percentage against a specific amount.
	 * @param caster The creature attempting to use the skill.
	 * @param skill The skill being cast.
	 * @param target The object targeted by the skill.
	 * @return {@code true} if the condition is met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		return _percentType.test(caster.getCurrentCpPercent(), _amount);
	}
}
