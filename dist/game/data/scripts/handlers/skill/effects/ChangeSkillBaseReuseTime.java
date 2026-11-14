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
import org.l2jmobius.gameserver.entity.actor.stat.CreatureStat;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Changes the base reuse time of specific skills to a fixed value while this effect is active.
 * @author Atronic
 */
public class ChangeSkillBaseReuseTime extends AbstractEffect
{
	private final Set<Integer> _skillIds;
	private final int _time;
	
	/**
	 * Creates a new {@code ChangeSkillBaseReuseTime} effect.<br>
	 * This constructor initializes the affected skills from the provided parameters.<br>
	 * It parses skill IDs separated by semicolons in the "skillIds" string.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public ChangeSkillBaseReuseTime(StatSet params)
	{
		final String skillIds = params.getString("skillIds", null);
		if ((skillIds != null) && !skillIds.isEmpty())
		{
			_skillIds = new HashSet<>();
			for (String skillId : skillIds.split(";"))
			{
				_skillIds.add(Integer.parseInt(skillId));
			}
		}
		else
		{
			_skillIds = Collections.emptySet();
		}
		
		_time = params.getInt("amount", 0);
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It changes the base reuse time of the configured skills.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill used to trigger this effect.
	 * @param item The item used to trigger this effect.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		final CreatureStat creatureStat = effected.getStat();
		for (Integer skillId : _skillIds)
		{
			creatureStat.addChangedSkillBaseReuseTime(skillId, _time);
		}
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It restores the base reuse time of the configured skills.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill used to apply the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		final CreatureStat creatureStat = effected.getStat();
		for (Integer skillId : _skillIds)
		{
			creatureStat.removeChangedSkillBaseReuseTime(skillId);
		}
	}
}
