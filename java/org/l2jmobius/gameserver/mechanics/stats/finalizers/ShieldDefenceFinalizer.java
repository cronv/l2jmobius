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
 * This class handles the final calculation of the shield defense statistic.<br>
 * It ensures that shield defense values are correctly processed before being applied to a {@code Creature}.
 * @author Sdw
 */
public class ShieldDefenceFinalizer implements IStatFunction
{
	/**
	 * Calculates the final value for a specific statistic.<br>
	 * This method uses the weapon plus base value calculation.<br>
	 * It ensures that the {@code base} value is present before proceeding.
	 * @param creature The {@code Creature} instance to calculate stats for.
	 * @param base The {@code OptionalDouble} containing the base value.
	 * @param stat The {@code Stat} type being calculated.
	 * @return The final calculated double value.
	 */
	@Override
	public double calc(Creature creature, OptionalDouble base, Stat stat)
	{
		throwIfPresent(base);
		
		return Stat.defaultValue(creature, stat, calcWeaponPlusBaseValue(creature, stat));
	}
}
