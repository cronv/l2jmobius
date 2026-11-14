/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.mechanics.stats.finalizers;

import java.util.OptionalDouble;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.mechanics.stats.IStatFunction;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

/**
 * This class handles the final calculation for the maximum CP stat.<br>
 * It ensures that the {@code Stat} value is correctly capped or adjusted based on specific rules.
 * @author UnAfraid
 */
public class MaxCpFinalizer implements IStatFunction
{
	/**
	 * Calculates the final value for a specific {@code Stat}.<br>
	 * This method determines the base value based on the {@code Creature} type.<br>
	 * It applies bonuses from {@code CON} and {@code CHA} stats.
	 * @param creature The {@code Creature} to calculate the stat for.
	 * @param base The {@code OptionalDouble} representing the base value.
	 * @param stat The {@code Stat} type being calculated.
	 * @return The final calculated double value.
	 */
	@Override
	public double calc(Creature creature, OptionalDouble base, Stat stat)
	{
		throwIfPresent(base);
		
		double baseValue = creature.getTemplate().getBaseValue(stat, 0);
		final Player player = creature.asPlayer();
		if (player != null)
		{
			baseValue = player.getTemplate().getBaseCpMax(player.getLevel());
		}
		
		final double chaBonus = creature.isPlayer() ? BaseStat.CHA.calcBonus(creature) : 1;
		final double conBonus = creature.getCON() > 0 ? BaseStat.CON.calcBonus(creature) : 1;
		baseValue *= conBonus * chaBonus;
		return Stat.defaultValue(creature, stat, baseValue);
	}
}
