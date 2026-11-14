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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the effect of disabling a specific skill for an actor.<br>
 * It prevents the target from using the designated {@code Skill} for a set duration.
 * @author Ofelin
 */
public class DisableSkill extends AbstractEffect
{
	private final Set<Integer> _disabledSkills;
	
	/**
	 * Creates a new {@code DisableSkill} effect.<br>
	 * This constructor initializes the set of disabled skills from the provided parameters.<br>
	 * It parses skill IDs separated by semicolons in the "disable" string.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public DisableSkill(StatSet params)
	{
		final String disable = params.getString("disable");
		if ((disable != null) && !disable.isEmpty())
		{
			_disabledSkills = new HashSet<>();
			for (String slot : disable.split(";"))
			{
				_disabledSkills.add(Integer.parseInt(slot));
			}
		}
		else
		{
			_disabledSkills = Collections.emptySet();
		}
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It disables specific skills for the target creature.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill used to trigger this effect.
	 * @param item The item used to trigger this effect.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		Skill knownSkill;
		for (int disableSkillId : _disabledSkills)
		{
			knownSkill = effected.getKnownSkill(disableSkillId);
			if (knownSkill != null)
			{
				effected.disableSkill(knownSkill, 0);
			}
		}
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It re-enables all skills that were previously disabled by this effect.
	 * @param effector The {@code Creature} who cast the skill.
	 * @param effected The {@code Creature} who was affected by the skill.
	 * @param skill The {@code Skill} used to apply the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		Skill knownSkill;
		for (int enableSkillId : _disabledSkills)
		{
			knownSkill = effected.getKnownSkill(enableSkillId);
			if (knownSkill != null)
			{
				if (effected.isPlayer())
				{
					effected.asPlayer().enableSkill(knownSkill, false);
				}
				else
				{
					effected.enableSkill(knownSkill);
				}
			}
		}
	}
}
