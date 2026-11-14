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

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.StatModifierType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the application of abnormal time effects based on specific skill IDs.<br>
 * This class allows for custom time manipulation logic tied to unique {@code Skill} identifiers.
 * @author Mobius
 */
public class AbnormalTimeChangeBySkillId extends AbstractEffect
{
	private final double _time;
	private final StatModifierType _mode;
	private final Set<Integer> _skillIds = new HashSet<>();
	
	/**
	 * Creates a new instance of {@code AbnormalTimeChangeBySkillId}.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It sets the time, mode, and specific skill IDs for the effect.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public AbnormalTimeChangeBySkillId(StatSet params)
	{
		_time = params.getDouble("time", -1);
		_mode = params.getEnum("mode", StatModifierType.class, StatModifierType.PER);
		final String skillIds = params.getString("ids", null);
		if ((skillIds != null) && !skillIds.isEmpty())
		{
			for (String id : skillIds.split(","))
			{
				_skillIds.add(Integer.valueOf(id));
			}
		}
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It applies abnormal time modifiers to the {@code effected} creature.<br>
	 * The logic checks if the mode is {@code StatModifierType.PER} or a difference.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to trigger the effect.
	 * @param item The {@code Item} used to trigger the effect.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		for (int skillId : _skillIds)
		{
			if (_mode == StatModifierType.PER)
			{
				effected.addMultipliedAbnormalTime(skillId, _time);
			}
			else // DIFF
			{
				effected.addAddedAbnormalTime(skillId, (int) _time);
			}
		}
	}
	
	/**
	 * This method is called when an effect ends.<br>
	 * It removes the abnormal time modification from the {@code effected} creature.<br>
	 * The removal depends on whether the mode was a multiplier or an addition.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill used to apply the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		for (int skillId : _skillIds)
		{
			if (_mode == StatModifierType.PER)
			{
				effected.removeMultipliedAbnormalTime(skillId, _time);
			}
			else // DIFF
			{
				effected.removeAddedAbnormalTime(skillId, (int) _time);
			}
		}
	}
}
