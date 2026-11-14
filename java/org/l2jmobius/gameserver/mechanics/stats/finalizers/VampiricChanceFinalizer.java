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
 * This class handles the final calculation for the vampiric chance statistic.<br>
 * It ensures that the {@code Stat} value is correctly processed before being applied to a {@code Creature}.
 * @author Sdw, Mobius
 */
public class VampiricChanceFinalizer implements IStatFunction
{
	/**
	 * Calculates the final value for a specific {@code Stat}.<br>
	 * This method determines the percentage of damage absorbed based on the creature's stats.<br>
	 * It ensures the result does not exceed the maximum allowed limit.
	 * @param creature The {@code Creature} instance to check.
	 * @param base The {@code OptionalDouble} containing the base value.
	 * @param stat The {@code Stat} type being calculated.
	 * @return The final calculated double value for the stat.
	 */
	@Override
	public double calc(Creature creature, OptionalDouble base, Stat stat)
	{
		throwIfPresent(base);
		
		final CreatureStat creatureStat = creature.getStat();
		final double amount = creatureStat.getValue(Stat.ABSORB_DAMAGE_PERCENT, 0) * 100;
		final double vampiricSum = creatureStat.getVampiricSum();
		return amount > 0 ? Stat.defaultValue(creature, stat, Math.min(1, vampiricSum / amount / 100)) : 0;
	}
}
