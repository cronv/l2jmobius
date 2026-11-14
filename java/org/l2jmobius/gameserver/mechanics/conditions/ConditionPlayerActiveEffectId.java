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
 * Checks if a player currently has a specific active effect or buff.<br>
 * This condition validates the presence of an effect ID on a {@code Creature}.
 */
public class ConditionPlayerActiveEffectId extends Condition
{
	private final int _effectId;
	private final int _effectLvl;
	
	/**
	 * Creates a new {@code ConditionPlayerActiveEffectId} instance.<br>
	 * This constructor sets the required effect ID and defaults the level to -1.
	 * @param effectId The unique identifier for the active effect.
	 */
	public ConditionPlayerActiveEffectId(int effectId)
	{
		_effectId = effectId;
		_effectLvl = -1;
	}
	
	/**
	 * Creates a new instance of {@code ConditionPlayerActiveEffectId}.<br>
	 * This condition checks for a specific active effect and its level.
	 * @param effectId The unique identifier for the effect.
	 * @param effectLevel The required level of the effect.
	 */
	public ConditionPlayerActiveEffectId(int effectId, int effectLevel)
	{
		_effectId = effectId;
		_effectLvl = effectLevel;
	}
	
	/**
	 * Checks if the {@code effector} has a specific active effect.<br>
	 * It verifies that the effect level meets the required criteria.
	 * @param effector The creature who performs the action.
	 * @param effected The creature who receives the action.
	 * @param skill The skill being used.
	 * @param item The item being used.
	 * @return {@code true} if the condition is met, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		final BuffInfo info = effector.getEffectList().getBuffInfoBySkillId(_effectId);
		return (info != null) && ((_effectLvl == -1) || (_effectLvl <= info.getSkill().getLevel()));
	}
}
