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
package org.l2jmobius.gameserver.mechanics.stats.finalizers;

import java.util.OptionalDouble;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.stat.CreatureStat;
import org.l2jmobius.gameserver.mechanics.stats.IStatFunction;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

/**
 * This class calculates the final value for the MP Vampiric Chance statistic.<br>
 * It ensures that the resulting percentage is correctly processed as a {@code double}.
 * @author Mobius
 */
public class MpVampiricChanceFinalizer implements IStatFunction
{
	/**
	 * Calculates the final value for a specific statistic.<br>
	 * This method uses the creature's stats to determine the result.<br>
	 * It handles mana absorption and vampiric sum logic.
	 * @param creature The {@code Creature} being evaluated.
	 * @param base The {@code OptionalDouble} representing the base value.
	 * @param stat The {@code Stat} type to calculate.
	 * @return The calculated result as a {@code double}.
	 */
	@Override
	public double calc(Creature creature, OptionalDouble base, Stat stat)
	{
		throwIfPresent(base);
		
		final CreatureStat creatureStat = creature.getStat();
		final double amount = creatureStat.getValue(Stat.ABSORB_MANA_DAMAGE_PERCENT, 0) * 100;
		final double mpVampiricSum = creatureStat.getMpVampiricSum();
		return amount > 0 ? Stat.defaultValue(creature, stat, Math.min(1, mpVampiricSum / amount / 100)) : 0;
	}
}
