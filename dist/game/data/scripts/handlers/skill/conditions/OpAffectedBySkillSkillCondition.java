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
 * Checks if a target creature is currently affected by a specific skill.<br>
 * This condition validates the presence of a {@code BuffInfo} associated with a {@code Skill}.
 * @author Mobius
 */
public class OpAffectedBySkillSkillCondition implements ISkillCondition
{
	private final int _skillId;
	private final int _skillLevel;
	
	/**
	 * Creates a new condition to check if an entity is affected by a specific skill.<br>
	 * This constructor extracts the required skill data from a {@code StatSet}.
	 * @param params The {@code StatSet} containing the {@code skillId} and {@code skillLevel}.
	 */
	public OpAffectedBySkillSkillCondition(StatSet params)
	{
		_skillId = params.getInt("skillId", -1);
		_skillLevel = params.getInt("skillLevel", -1);
	}
	
	/**
	 * Checks if the {@code caster} is currently affected by a specific skill.<br>
	 * It verifies if the skill exists and meets the required level.
	 * @param caster The creature attempting to use the skill.
	 * @param skill The skill being cast.
	 * @param target The object being targeted.
	 * @return {@code true} if the condition is met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		final BuffInfo buffInfo = caster.getEffectList().getBuffInfoBySkillId(_skillId);
		if (_skillLevel > 0)
		{
			return (buffInfo != null) && (buffInfo.getSkill().getLevel() >= _skillLevel);
		}
		
		return buffInfo != null;
	}
}
