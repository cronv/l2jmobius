/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package handlers.skill.conditions;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillConditionAffectType;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillConditionPercentType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if a {@code Creature} has a minimum amount of health remaining.<br>
 * This condition can be evaluated as a fixed value or a percentage of the maximum health.
 * @author UnAfraid, Mobius
 */
public class RemainHpPerSkillCondition implements ISkillCondition
{
	private final int _amount;
	private final SkillConditionPercentType _percentType;
	private final SkillConditionAffectType _affectType;
	
	/**
	 * Creates a new condition based on remaining health.<br>
	 * It initializes the required amount and types from a {@code StatSet}.
	 * @param params The {@code StatSet} containing configuration values.
	 */
	public RemainHpPerSkillCondition(StatSet params)
	{
		_amount = params.getInt("amount");
		_percentType = params.getEnum("percentType", SkillConditionPercentType.class);
		_affectType = params.getEnum("affectType", SkillConditionAffectType.class);
	}
	
	/**
	 * Checks if the skill can be used based on health requirements.<br>
	 * It validates the current HP percentage of either the {@code caster} or the {@code target}.<br>
	 * The check depends on the configured affect type and amount.
	 * @param caster The creature casting the skill.
	 * @param skill The skill being attempted.
	 * @param target The object being targeted by the skill.
	 * @return {@code true} if the health conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		switch (_affectType)
		{
			case CASTER:
			{
				return _percentType.test(caster.getCurrentHpPercent(), _amount);
			}
			case TARGET:
			{
				if ((target != null) && target.isCreature())
				{
					return _percentType.test(target.asCreature().getCurrentHpPercent(), _amount);
				}
				break;
			}
			case SUMMON:
			{
				if (caster.hasServitors())
				{
					return _percentType.test(caster.asPlayer().getAnyServitor().getCurrentHpPercent(), _amount);
				}
				break;
			}
		}
		
		return false;
	}
}
