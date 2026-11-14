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
 * This class calculates the final value of a {@code Stat} based on the player's range.<br>
 * It ensures that statistics are correctly adjusted according to distance-based mechanics.
 * @author UnAfraid
 */
public class PRangeFinalizer implements IStatFunction
{
	/**
	 * Calculates the final value for a specific {@code Stat}.<br>
	 * This method uses the weapon base value if it is available.<br>
	 * It returns the default value if no valid calculation can be performed.
	 * @param creature The {@code Creature} being evaluated.
	 * @param base The optional base value for the stat.
	 * @param stat The {@code Stat} type to calculate.
	 * @return The calculated double value of the stat.
	 */
	@Override
	public double calc(Creature creature, OptionalDouble base, Stat stat)
	{
		throwIfPresent(base);
		
		return Stat.defaultValue(creature, stat, calcWeaponBaseValue(creature, stat));
	}
}
