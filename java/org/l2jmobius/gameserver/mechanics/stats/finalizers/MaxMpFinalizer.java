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
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.actor.stat.CreatureStat;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.mechanics.stats.IStatFunction;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

/**
 * This class handles the final calculation of the maximum MP stat.<br>
 * It ensures that the {@code maxMp} value is correctly applied to a {@code Creature}.
 * @author UnAfraid, Mobius
 */
public class MaxMpFinalizer implements IStatFunction
{
	/**
	 * Calculates the final maximum MP value for a {@code Creature}.<br>
	 * This method applies bonuses from stats like {@code MEN} and {@code CHA}.<br>
	 * It handles specific logic for both players and pets.
	 * @param creature The {@code Creature} to calculate the stat for.
	 * @param base An {@code OptionalDouble} containing the base value.
	 * @param stat The {@code Stat} being calculated.
	 * @return The final calculated double value.
	 */
	@Override
	public double calc(Creature creature, OptionalDouble base, Stat stat)
	{
		throwIfPresent(base);
		
		double baseValue = creature.getTemplate().getBaseValue(stat, 0);
		if (creature.isPet())
		{
			final Pet pet = creature.asPet();
			baseValue = pet.getPetLevelData().getPetMaxMP();
		}
		else if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			if (player != null)
			{
				baseValue = player.getTemplate().getBaseMpMax(player.getLevel());
			}
		}
		
		final double chaBonus = creature.isPlayer() ? BaseStat.CHA.calcBonus(creature) : 1;
		final double menBonus = creature.getMEN() > 0 ? BaseStat.MEN.calcBonus(creature) : 1;
		baseValue *= menBonus * chaBonus;
		
		return defaultValue(creature, stat, baseValue);
	}
	
	/**
	 * Calculates the final value for a specific {@code Stat}.<br>
	 * This method combines multipliers, additions, and item bonuses.<br>
	 * It also accounts for move type values.
	 * @param creature The {@code Creature} whose stats are being calculated.
	 * @param stat The {@code Stat} type to calculate.
	 * @param baseValue The initial {@code double} value of the stat.
	 * @return The final calculated {@code double} value.
	 */
	private static double defaultValue(Creature creature, Stat stat, double baseValue)
	{
		final CreatureStat creatureStat = creature.getStat();
		final double mul = creatureStat.getMul(stat);
		final double add = creatureStat.getAdd(stat);
		double addItem = 0;
		
		final Inventory inv = creature.getInventory();
		if (inv != null)
		{
			// Add maxMP bonus from items.
			for (Item item : inv.getPaperdollItems())
			{
				addItem += item.getTemplate().getStats(stat, 0);
			}
		}
		
		return (mul * baseValue) + add + addItem + creatureStat.getMoveTypeValue(stat, creature.getMoveType());
	}
}
