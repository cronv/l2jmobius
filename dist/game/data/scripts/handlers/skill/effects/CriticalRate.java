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
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the application of a critical rate effect to an actor.<br>
 * It modifies the {@code Stat} values associated with critical hit chances.
 * @author Sdw, Mobius
 */
public class CriticalRate extends AbstractConditionalHpEffect
{
	/**
	 * Creates a new {@code CriticalRate} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the values for this effect.
	 */
	public CriticalRate(StatSet params)
	{
		super(params, Stat.CRITICAL_RATE);
	}
	
	/**
	 * Applies the critical rate effect to a specific creature.<br>
	 * This method checks all required conditions before modifying stats.<br>
	 * It updates either an additive or multiplicative stat based on the current mode.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used to trigger the effect.
	 */
	@Override
	public void pump(Creature effected, Skill skill)
	{
		if (!_conditions.isEmpty())
		{
			for (Condition cond : _conditions)
			{
				if (!cond.test(effected, effected, skill))
				{
					return;
				}
			}
		}
		
		switch (_mode)
		{
			case DIFF:
			{
				effected.getStat().mergeAdd(_addStat, _amount);
				break;
			}
			case PER:
			{
				effected.getStat().mergeMul(_mulStat, (_amount / 100));
				break;
			}
		}
	}
}
