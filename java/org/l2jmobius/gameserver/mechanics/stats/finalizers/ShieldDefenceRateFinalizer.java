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
 * This class calculates the final shield defense rate for a {@code Creature}.<br>
 * It applies specific logic to determine how much damage is mitigated by shields.<br>
 * It implements the {@code IStatFunction} interface to integrate with the statistics system.
 * @author Sdw
 */
public class ShieldDefenceRateFinalizer implements IStatFunction
{
	/**
	 * Calculates the final value for a specific statistic.<br>
	 * This method uses the weapon plus base value of the {@code Creature}.<br>
	 * It returns the default value if the calculation is not applicable.
	 * @param creature The {@code Creature} being processed.
	 * @param base The optional base value for the statistic.
	 * @param stat The {@code Stat} type to calculate.
	 * @return The calculated double value of the statistic.
	 */
	@Override
	public double calc(Creature creature, OptionalDouble base, Stat stat)
	{
		throwIfPresent(base);
		
		return Stat.defaultValue(creature, stat, calcWeaponPlusBaseValue(creature, stat));
	}
}
