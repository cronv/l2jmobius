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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.mechanics.stats.IStatFunction;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

/**
 * This class serves as a base implementation for finalising {@code Stat} calculations.<br>
 * It provides the foundation for applying final modifications to various character statistics.
 * @author UnAfraid, Mobius
 */
public class BaseStatFinalizer implements IStatFunction
{
	/**
	 * Calculates the final value of a specific {@code Stat} for a given {@code Creature}.<br>
	 * This method applies template values and additional bonuses like armor sets or henna.<br>
	 * It ensures that only {@code Player} entities receive certain equipment-based bonuses.
	 * @param creature The {@code Creature} for which the stat is being calculated.
	 * @param base The optional base value of the stat.
	 * @param stat The specific {@code Stat} to calculate.
	 * @return The final calculated value as a {@code double}.
	 */
	@Override
	public double calc(Creature creature, OptionalDouble base, Stat stat)
	{
		throwIfPresent(base);
		
		// Apply template value
		double baseValue = creature.getTemplate().getBaseValue(stat, 0);
		
		// Should not apply armor set and henna bonus to summons.
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			
			// Armor sets calculation
			baseValue += player.getInventory().getPaperdollCache().getBaseStatValue(player, BaseStat.valueOf(stat));
			
			// Henna calculation
			baseValue += player.getHennaList().getValue(BaseStat.valueOf(stat));
		}
		
		return validateValue(creature, Stat.defaultValue(creature, stat, baseValue), 1, BaseStat.MAX_STAT_VALUE - 1);
	}
}
