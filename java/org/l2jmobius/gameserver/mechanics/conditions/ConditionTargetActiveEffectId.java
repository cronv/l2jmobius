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
package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This condition checks if a specific effect ID is currently active on the target.<br>
 * It is used to verify the presence of certain buffs or debuffs during mechanic execution.
 */
public class ConditionTargetActiveEffectId extends Condition
{
	private final int _effectId;
	private final int _effectLvl;
	
	/**
	 * Creates a new instance of {@code ConditionTargetActiveEffectId}.<br>
	 * This constructor sets the target effect ID.<br>
	 * The default level is set to {@code -1}.
	 * @param effectId The unique identifier for the active effect.
	 */
	public ConditionTargetActiveEffectId(int effectId)
	{
		_effectId = effectId;
		_effectLvl = -1;
	}
	
	/**
	 * Creates a new instance of {@code ConditionTargetActiveEffectId}.<br>
	 * This constructor sets the specific effect and its level.
	 * @param effectId The unique identifier for the effect.
	 * @param effectLevel The level associated with the effect.
	 */
	public ConditionTargetActiveEffectId(int effectId, int effectLevel)
	{
		_effectId = effectId;
		_effectLvl = effectLevel;
	}
	
	/**
	 * Checks if the target creature has a specific active effect.<br>
	 * It verifies that the effect exists and meets the required level.
	 * @param effector The creature performing the action.
	 * @param effected The creature being checked for the effect.
	 * @param skill The skill associated with the condition.
	 * @param item The item template involved in the action.
	 * @return {@code true} if the effect is active and meets the level requirements, {@code false} otherwise.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		if (effected == null)
		{
			return false;
		}
		
		final BuffInfo info = effected.getEffectList().getBuffInfoBySkillId(_effectId);
		return (info != null) && ((_effectLvl == -1) || (_effectLvl <= info.getSkill().getLevel()));
	}
}
