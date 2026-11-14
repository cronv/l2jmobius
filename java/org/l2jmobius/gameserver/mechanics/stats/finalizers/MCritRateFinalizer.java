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

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.mechanics.stats.IStatFunction;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

/**
 * This class handles the final calculation of the {@code MCritRate} stat.<br>
 * It ensures that the critical rate is correctly processed before being applied to a {@code Creature}.
 * @author UnAfraid
 */
public class MCritRateFinalizer implements IStatFunction
{
	/**
	 * Calculates the final value for a specific statistic.<br>
	 * This method considers weapon bonuses and character attributes.<br>
	 * It also applies special logic for players and magical critical rates.
	 * @param creature The {@code Creature} being evaluated.
	 * @param base The {@code OptionalDouble} representing the base value.
	 * @param stat The {@code Stat} type to calculate.
	 * @return The final calculated double value.
	 */
	@Override
	public double calc(Creature creature, OptionalDouble base, Stat stat)
	{
		throwIfPresent(base);
		
		double baseValue = calcWeaponPlusBaseValue(creature, stat);
		if (creature.isPlayer())
		{
			// Enchanted legs bonus
			baseValue += calcEnchantBodyPart(creature, BodyPart.LEGS);
		}
		
		final double witBonus = creature.getWIT() > 0 ? BaseStat.WIT.calcBonus(creature) : 1;
		
		final double maxMagicalCritRate;
		if (creature.isPlayable())
		{
			maxMagicalCritRate = PlayerConfig.MAX_MCRIT_RATE + creature.getStat().getValue(Stat.ADD_MAX_MAGIC_CRITICAL_RATE, 0);
		}
		else
		{
			maxMagicalCritRate = Double.MAX_VALUE;
		}
		
		return validateValue(creature, Stat.defaultValue(creature, stat, baseValue * witBonus * 10), 0, maxMagicalCritRate);
	}
	
	/**
	 * Calculates the bonus value for a body part based on its enchantment.<br>
	 * This method considers both the level and whether the item is blessed.
	 * @param enchantLevel The current level of the enchantment.
	 * @param isBlessed A boolean indicating if the item has the blessed status.
	 * @return The calculated bonus as a {@code double}.
	 */
	@Override
	public double calcEnchantBodyPartBonus(int enchantLevel, boolean isBlessed)
	{
		if (isBlessed)
		{
			return (0.5 * Math.max(enchantLevel - 3, 0)) + (0.5 * Math.max(enchantLevel - 6, 0));
		}
		
		return (0.34 * Math.max(enchantLevel - 3, 0)) + (0.34 * Math.max(enchantLevel - 6, 0));
	}
}
