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

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.mechanics.stats.IStatFunction;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

/**
 * This class handles the final calculation of the evasion rate stat.<br>
 * It ensures that the {@code Stat} value is correctly processed before being applied to a {@code Creature}.
 * @author UnAfraid, Atronic
 */
public class MEvasionRateFinalizer implements IStatFunction
{
	/**
	 * Calculates the final evasion rate for a specific {@code Creature}.<br>
	 * This method applies bonuses based on level and attributes.<br>
	 * It handles different logic for players and non-player entities.
	 * @param creature The {@code Creature} to calculate the stat for.
	 * @param base The {@code OptionalDouble} containing the base value.
	 * @param stat The {@code Stat} type being calculated.
	 * @return The final calculated evasion rate as a {@code double}.
	 */
	@Override
	public double calc(Creature creature, OptionalDouble base, Stat stat)
	{
		throwIfPresent(base);
		
		double baseValue = calcWeaponPlusBaseValue(creature, stat);
		
		if (creature.isPlayer())
		{
			// Enchanted helm bonus
			baseValue += calcEnchantBodyPart(creature, BodyPart.HEAD);
		}
		
		return validateValue(creature, Stat.defaultValue(creature, stat, baseValue), Double.NEGATIVE_INFINITY, creature.isPlayable() ? PlayerConfig.MAX_EVASION : Double.MAX_VALUE);
	}
	
	/**
	 * Calculates the bonus value for a body part based on its enchantment.<br>
	 * This method considers both the level and whether the item is blessed.
	 * @param enchantLevel The current level of the enchantment.
	 * @param isBlessed Whether the item has the blessed property.
	 * @return The calculated bonus as a {@code double}.
	 */
	@Override
	public double calcEnchantBodyPartBonus(int enchantLevel, boolean isBlessed)
	{
		if (isBlessed)
		{
			return (0.3 * Math.max(enchantLevel - 3, 0)) + (0.3 * Math.max(enchantLevel - 6, 0));
		}
		
		return (0.2 * Math.max(enchantLevel - 3, 0)) + (0.2 * Math.max(enchantLevel - 6, 0));
	}
}
