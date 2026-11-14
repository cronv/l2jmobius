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
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if a target is currently affected by a specific skill.<br>
 * This condition validates whether the {@code Creature} has an active buff or debuff from the specified {@code Skill}.
 * @author Mobius
 */
public class TargetAffectedBySkillSkillCondition implements ISkillCondition
{
	private final int _skillId;
	private final int _skillLevel;
	
	/**
	 * Creates a new condition to check if a target is affected by a specific skill.<br>
	 * This constructor initializes the required skill ID and level from a {@code StatSet}.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public TargetAffectedBySkillSkillCondition(StatSet params)
	{
		_skillId = params.getInt("skillId", -1);
		_skillLevel = params.getInt("skillLevel", -1);
	}
	
	/**
	 * Checks if the target is currently affected by a specific skill.<br>
	 * It verifies that the target is a {@code Creature}.<br>
	 * The check also ensures the skill level meets the required minimum.
	 * @param caster The {@code Creature} attempting to use the skill.
	 * @param skill The {@code Skill} being cast.
	 * @param target The {@code WorldObject} that is the potential target.
	 * @return {@code true} if the conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		if ((target == null) || !target.isCreature())
		{
			return false;
		}
		
		final BuffInfo buffInfo = target.asCreature().getEffectList().getBuffInfoBySkillId(_skillId);
		if (_skillLevel > 0)
		{
			return (buffInfo != null) && (buffInfo.getSkill().getLevel() >= _skillLevel);
		}
		
		return buffInfo != null;
	}
}
