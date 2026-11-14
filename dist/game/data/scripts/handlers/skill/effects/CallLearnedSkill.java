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
package handlers.skill.effects;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class implements the {@code CallLearnedSkill} effect.<br>
 * It handles the logic for triggering a learned skill based on the character's level.
 * @author Kazumi
 */
public class CallLearnedSkill extends AbstractEffect
{
	private final int _skillId;
	
	/**
	 * Creates a new instance of the {@code CallLearnedSkill} effect.<br>
	 * This constructor initializes the skill ID from the provided parameters.
	 * @param params The {@code StatSet} containing the required skill data.
	 */
	public CallLearnedSkill(StatSet params)
	{
		_skillId = params.getInt("skillId");
	}
	
	/**
	 * Checks if the effect occurs instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Triggers an instant skill effect between two creatures.<br>
	 * This method checks if the {@code effector} knows the required skill.<br>
	 * If the skill is known, it triggers the cast on the {@code effected} target.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The specific {@code Skill} being used.
	 * @param item The {@code Item} associated with the effect.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Skill knownSkill = effector.getKnownSkill(_skillId);
		if (knownSkill != null)
		{
			SkillCaster.triggerCast(effector, effected, knownSkill);
		}
	}
}
