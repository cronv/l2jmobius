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
import java.util.Collections;
import java.util.List;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the execution of a {@code Skill} after a specific time delay.<br>
 * It allows for delayed skill triggers to be added as an effect on a target or caster.
 * @author Mobius
 */
public class TriggerSkillWithDelay extends AbstractEffect
{
	private final List<SkillHolder> _triggerSkill;
	private final List<Integer> _delay;
	
	/**
	 * Creates a new effect that triggers skills after a specific delay.<br>
	 * This constructor parses skill IDs and timing from the provided {@code StatSet}.<br>
	 * It handles multiple skills separated by semicolons.
	 * @param params The {@code StatSet} containing "triggerSkill" and "delay" values.
	 */
	public TriggerSkillWithDelay(StatSet params)
	{
		final String triggerSkills = params.getString("triggerSkill", "");
		if (triggerSkills.isEmpty())
		{
			_triggerSkill = Collections.emptyList();
		}
		else
		{
			final String[] split = triggerSkills.split(";");
			_triggerSkill = new ArrayList<>(split.length);
			
			for (String skill : split)
			{
				final String[] splitSkill = skill.split(",");
				if (splitSkill.length == 1)
				{
					_triggerSkill.add(new SkillHolder(Integer.parseInt(splitSkill[0]), 1));
				}
				else
				{
					_triggerSkill.add(new SkillHolder(Integer.parseInt(splitSkill[0]), Integer.parseInt(splitSkill[1])));
				}
			}
		}
		
		final String delays = params.getString("delay", "");
		if (triggerSkills.isEmpty())
		{
			final int size = _triggerSkill.size();
			_delay = new ArrayList<>(size);
			for (int i = 0; i < size; i++)
			{
				_delay.add(1000);
			}
		}
		else
		{
			final String[] split = delays.split(";");
			if (split.length == 1)
			{
				final int size = _triggerSkill.size();
				_delay = new ArrayList<>(size);
				
				final int delay = Integer.parseInt(split[0]);
				for (int i = 0; i < size; i++)
				{
					_delay.add(delay);
				}
			}
			else
			{
				_delay = new ArrayList<>(split.length);
				for (String delay : split)
				{
					_delay.add(Integer.parseInt(delay));
				}
			}
		}
	}
	
	/**
	 * Checks if the effect occurs immediately.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Triggers a sequence of skills immediately.<br>
	 * This method schedules the skills from {@code _triggerSkill} to be cast.<br>
	 * It applies the cumulative delays defined in {@code _delay}.
	 * @param effector The creature that performs the skill.
	 * @param effected The creature that receives the skill.
	 * @param skill The primary skill being used for this effect.
	 * @param item The item associated with the skill execution.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		int delay = 0;
		int index = 0;
		for (SkillHolder holder : _triggerSkill)
		{
			delay += _delay.get(index++);
			ThreadPool.schedule(() -> SkillCaster.triggerCast(effector, effected, holder.getSkill()), delay);
		}
	}
}
