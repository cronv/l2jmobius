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
import org.l2jmobius.gameserver.config.custom.ChampionMonstersConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.stat.CreatureStat;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.mechanics.stats.IStatFunction;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

/**
 * This class handles the final calculation of the {@code PAttackSpeed} stat.<br>
 * It ensures that attack speed values are correctly processed before being applied to a {@code Creature}.
 * @author UnAfraid, Mobius
 */
public class PAttackSpeedFinalizer implements IStatFunction
{
	/**
	 * Calculates the final attack speed for a given {@code Creature}.<br>
	 * This method applies bonuses from stats and champion configurations.<br>
	 * It ensures the result stays within valid limits.
	 * @param creature The {@code Creature} to calculate the stat for.
	 * @param base The {@code OptionalDouble} representing the base value.
	 * @param stat The {@code Stat} type being calculated.
	 * @return The final calculated attack speed as a {@code double}.
	 */
	@Override
	public double calc(Creature creature, OptionalDouble base, Stat stat)
	{
		throwIfPresent(base);
		
		// A ridden mount attacks with its own stats, the rider's worn items do not count.
		if (creature.isPlayer())
		{
			final double rideValue = creature.asPlayer().getRideAttackSpeed();
			if (rideValue > 0)
			{
				return rideValue;
			}
		}
		
		final double staticPAtkSpeed = creature.getStat().getValue(Stat.STATIC_PHYSICAL_ATTACK_SPEED, 0);
		if (staticPAtkSpeed > 0)
		{
			return staticPAtkSpeed;
		}
		
		double baseValue = calcWeaponBaseValue(creature, stat);
		if (ChampionMonstersConfig.CHAMPION_ENABLE && creature.isChampion())
		{
			baseValue *= ChampionMonstersConfig.CHAMPION_SPD_ATK;
		}
		
		final double chaBonus = creature.isPlayer() ? BaseStat.CHA.calcBonus(creature) : 1;
		final double dexBonus = creature.getDEX() > 0 ? BaseStat.DEX.calcBonus(creature) : 1;
		baseValue *= dexBonus * chaBonus;
		return validateValue(creature, defaultValue(creature, stat, baseValue), 1, creature.isPlayable() ? PlayerConfig.MAX_PATK_SPEED : Double.MAX_VALUE);
	}
	
	/**
	 * Calculates the default value for a specific stat.<br>
	 * This method applies multipliers and additions from the {@code Creature}.<br>
	 * It also includes the move type value for the given {@code Stat}.
	 * @param creature The {@code Creature} providing the stat modifiers.
	 * @param stat The {@code Stat} being calculated.
	 * @param baseValue The initial {@code double} value before modifiers.
	 * @return The final calculated {@code double} value.
	 */
	private double defaultValue(Creature creature, Stat stat, double baseValue)
	{
		final CreatureStat creatureStat = creature.getStat();
		final double mul = Math.max(creatureStat.getMul(stat), 0.7);
		final double add = creatureStat.getAdd(stat);
		return (baseValue * mul) + add + creatureStat.getMoveTypeValue(stat, creature.getMoveType());
	}
}
