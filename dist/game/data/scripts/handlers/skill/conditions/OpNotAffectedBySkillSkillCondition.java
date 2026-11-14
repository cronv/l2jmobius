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
 * Checks if a target is currently not affected by a specific skill.<br>
 * This condition returns {@code true} only when the specified skill's effect is absent from the target.
 * @author Mobius
 */
public class OpNotAffectedBySkillSkillCondition implements ISkillCondition
{
	private final int _skillId;
	private final int _skillLevel;
	
	/**
	 * Creates a new condition to check if an actor is not affected by a specific skill.<br>
	 * This constructor initializes the required skill ID and level from a {@code StatSet}.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public OpNotAffectedBySkillSkillCondition(StatSet params)
	{
		_skillId = params.getInt("skillId", -1);
		_skillLevel = params.getInt("skillLevel", -1);
	}
	
	/**
	 * Checks if the {@code caster} is allowed to use a skill based on specific conditions.<br>
	 * This method verifies that the {@code caster} is not currently affected by a certain skill.<br>
	 * It returns {@code true} if the condition is met and {@code false} otherwise.
	 * @param caster The creature attempting to cast the skill.
	 * @param skill The skill being used.
	 * @param target The object being targeted by the skill.
	 * @return {@code true} if the caster can use the skill, {@code false} otherwise.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		final BuffInfo buffInfo = caster.getEffectList().getBuffInfoBySkillId(_skillId);
		if (_skillLevel > 0)
		{
			return (buffInfo == null) || (buffInfo.getSkill().getLevel() < _skillLevel);
		}
		
		return buffInfo == null;
	}
}
