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

import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.ChampionMonstersConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.stat.CreatureStat;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.mechanics.stats.IStatFunction;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

/**
 * This class handles the final calculation of the {@code attackSpeed} stat.<br>
 * It ensures that the speed is correctly applied to a {@code Creature}.
 * @author UnAfraid, Mobius
 */
public class MAttackSpeedFinalizer implements IStatFunction
{
	/**
	 * Calculates the final attack speed for a given {@code Creature}.<br>
	 * This method applies bonuses from stats and champion configurations.<br>
	 * It ensures the result stays within valid limits for players and monsters.
	 * @param creature The {@code Creature} to calculate the stat for.
	 * @param base The optional base value of the stat.
	 * @param stat The {@code Stat} being calculated.
	 * @return The final calculated attack speed as a {@code double}.
	 */
	@Override
	public double calc(Creature creature, OptionalDouble base, Stat stat)
	{
		throwIfPresent(base);
		
		final double staticMAtkSpeed = creature.getStat().getValue(Stat.STATIC_MAGICAL_ATTACK_SPEED, 0);
		if (staticMAtkSpeed > 0)
		{
			return staticMAtkSpeed;
		}
		
		double baseValue = calcWeaponBaseValue(creature, stat);
		if (ChampionMonstersConfig.CHAMPION_ENABLE && creature.isChampion())
		{
			baseValue *= ChampionMonstersConfig.CHAMPION_SPD_ATK;
		}
		
		final double chaBonus = creature.isPlayer() ? BaseStat.CHA.calcBonus(creature) : 1;
		// NPC casting speed is used as defined, unless the WIT bonus is enabled for them.
		final double witBonus = ((creature.getWIT() > 0) && (!creature.isNpc() || !NpcConfig.NPC_SPEEDS_WITHOUT_STAT_BONUS)) ? BaseStat.WIT.calcBonus(creature) : 1;
		baseValue *= witBonus * chaBonus;
		return validateValue(creature, defaultValue(creature, stat, baseValue), 1, creature.isPlayable() ? PlayerConfig.MAX_MATK_SPEED : Double.MAX_VALUE);
	}
	
	/**
	 * Calculates the final value for a specific stat.<br>
	 * This method applies multipliers and additions to a base value.<br>
	 * It also includes move type bonuses for the {@code Creature}.
	 * @param creature The {@code Creature} entity being processed.
	 * @param stat The {@code Stat} type to calculate.
	 * @param baseValue The initial base value of the stat.
	 * @return The final calculated double value.
	 */
	private double defaultValue(Creature creature, Stat stat, double baseValue)
	{
		final CreatureStat creatureStat = creature.getStat();
		final double mul = Math.max(creatureStat.getMul(stat), 0.7);
		final double add = creatureStat.getAdd(stat);
		return (baseValue * mul) + add + creatureStat.getMoveTypeValue(stat, creature.getMoveType());
	}
}
