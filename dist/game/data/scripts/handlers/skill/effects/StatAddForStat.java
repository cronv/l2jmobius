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
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.StatModifierType;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles effects that modify a specific {@code Stat} based on the value of another {@code Stat}.<br>
 * It allows for dynamic stat scaling where one attribute influences the growth or total of another.<br>
 * The effect is applied to a {@code Creature} when triggered by a {@code Skill}.
 * @author Mobius
 */
public class StatAddForStat extends AbstractEffect
{
	private final Stat _stat;
	private final int _min;
	private final int _max;
	private final Stat _addStat;
	private final double _amount;
	
	/**
	 * Creates a new {@code StatAddForStat} effect.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It requires the mode to be set to {@code StatModifierType.DIFF}.
	 * @param params The {@code StatSet} containing the configuration data for this effect.
	 */
	public StatAddForStat(StatSet params)
	{
		_stat = params.getEnum("stat", Stat.class);
		_min = params.getInt("min", 0);
		_max = params.getInt("max", 2147483647);
		_addStat = params.getEnum("addStat", Stat.class);
		_amount = params.getDouble("amount", 0);
		
		if (params.getEnum("mode", StatModifierType.class, StatModifierType.DIFF) != StatModifierType.DIFF)
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " can only use DIFF mode.");
		}
	}
	
	/**
	 * Checks if the effect should be delayed before pumping.<br>
	 * This method currently always returns {@code true}.
	 * @return {@code true} if the pump action is allowed to proceed.
	 */
	@Override
	public boolean delayPump()
	{
		return true;
	}
	
	/**
	 * Applies a stat modification to a target creature.<br>
	 * This method checks if the current stat value is within a specific range.<br>
	 * If the condition is met, it merges an additional amount into the specified stat.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} that triggered this effect.
	 */
	@Override
	public void pump(Creature effected, Skill skill)
	{
		final int currentValue = (int) effected.getStat().getValue(_stat);
		if ((currentValue >= _min) && (currentValue <= _max))
		{
			effected.getStat().mergeAdd(_addStat, _amount);
		}
	}
}
