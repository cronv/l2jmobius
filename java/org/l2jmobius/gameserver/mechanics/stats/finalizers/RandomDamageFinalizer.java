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
import org.l2jmobius.gameserver.mechanics.stats.IStatFunction;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

/**
 * This class applies a random multiplier to the final damage value of an actor.<br>
 * It implements {@code IStatFunction} to modify stats during the calculation phase.
 * @author UnAfraid
 */
public class RandomDamageFinalizer implements IStatFunction
{
	/**
	 * Calculates the final value for a specific {@code Stat}.<br>
	 * This method uses the weapon base value if it is available.<br>
	 * It returns the default value if no base value exists.
	 * @param creature The {@code Creature} being processed.
	 * @param base The {@code OptionalDouble} representing the base value.
	 * @param stat The {@code Stat} type to calculate.
	 * @return The calculated double value for the statistic.
	 */
	@Override
	public double calc(Creature creature, OptionalDouble base, Stat stat)
	{
		throwIfPresent(base);
		
		return Stat.defaultValue(creature, stat, calcWeaponBaseValue(creature, stat));
	}
}
