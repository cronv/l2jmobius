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
 * Checks if the target is currently free from a specific skill.<br>
 * This condition returns {@code true} only if the target is neither affected by nor knows the specified {@code Skill}.
 * @author Mobius
 */
public class TargetNotAffectedBySkillSkillCondition implements ISkillCondition
{
	private final int _skillId;
	private final int _skillLevel;
	
	/**
	 * Creates a new condition to check if a target is not affected by a specific skill.<br>
	 * This constructor initializes the required {@code skillId} and {@code skillLevel}.
	 * @param params A {@code StatSet} containing the configuration values.
	 */
	public TargetNotAffectedBySkillSkillCondition(StatSet params)
	{
		_skillId = params.getInt("skillId", -1);
		_skillLevel = params.getInt("skillLevel", -1);
	}
	
	/**
	 * Checks if the target is eligible to receive a skill.<br>
	 * This method verifies that the target is not currently affected by and does not know a specific skill level.<br>
	 * It returns {@code false} if the target is {@code null} or not a creature.
	 * @param caster The {@code Creature} attempting to use the skill.
	 * @param skill The {@code Skill} being cast.
	 * @param target The {@code WorldObject} targeted by the skill.
	 * @return {@code true} if the condition is met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		if ((target == null) || !target.isCreature())
		{
			return false;
		}
		
		final Creature targetCreature = target.asCreature();
		final BuffInfo buffInfo = targetCreature.getEffectList().getBuffInfoBySkillId(_skillId);
		final Skill knownSkill = targetCreature.getKnownSkill(_skillId);
		if (_skillLevel > 0)
		{
			if ((buffInfo != null) && (buffInfo.getSkill().getLevel() >= _skillLevel))
			{
				return false;
			}
			
			return (knownSkill == null) || (knownSkill.getLevel() < _skillLevel);
		}
		
		return (buffInfo == null) && (knownSkill == null);
	}
}
