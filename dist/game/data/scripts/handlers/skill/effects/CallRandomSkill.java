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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for triggering a random skill from a predefined list.<br>
 * It selects one {@code Skill} randomly and executes it on the target.<br>
 * Use this effect when you want to create unpredictable combat outcomes.
 * @author Mobius
 */
public class CallRandomSkill extends AbstractEffect
{
	private final List<SkillHolder> _skills = new ArrayList<>();
	
	/**
	 * Creates a new {@code CallRandomSkill} effect.<br>
	 * This constructor parses skill data from the provided {@code StatSet}.<br>
	 * It looks for a key named "skills" containing semicolon-separated values.
	 * @param params The {@code StatSet} containing the skill configuration strings.
	 */
	public CallRandomSkill(StatSet params)
	{
		final String skills = params.getString("skills", null);
		if (skills != null)
		{
			for (String skill : skills.split(";"))
			{
				_skills.add(new SkillHolder(Integer.parseInt(skill.split(",")[0]), Integer.parseInt(skill.split(",")[1])));
			}
		}
	}
	
	/**
	 * Checks if the effect is triggered instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Triggers a random skill from the effect's list.<br>
	 * This method selects one {@code Skill} at random and executes it.
	 * @param effector The {@code Creature} who casts the skill.
	 * @param effected The {@code Creature} who receives the skill.
	 * @param skill The specific {@code Skill} being used for this instance.
	 * @param item The {@code Item} associated with the action.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		SkillCaster.triggerCast(effector, effected, _skills.get(Rnd.get(_skills.size())).getSkill());
	}
}
