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
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.mechanics.stats.IStatFunction;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

/**
 * This class handles the final calculations for the {@code MAttack} stat.<br>
 * It applies specific modifiers and logic to determine the final attack value of an actor.
 * @author UnAfraid, Mobius
 */
public class MAttackFinalizer implements IStatFunction
{
	/**
	 * Calculates the final Magic Attack value for a {@code Creature}.<br>
	 * This method applies bonuses from weapons, enchantments, and champion status.<br>
	 * It also factors in character stats like Intelligence and Charisma.
	 * @param creature The {@code Creature} to calculate the stat for.
	 * @param base The {@code OptionalDouble} representing the base value.
	 * @param stat The {@code Stat} type being calculated.
	 * @return The final calculated Magic Attack as a {@code double}.
	 */
	@Override
	public double calc(Creature creature, OptionalDouble base, Stat stat)
	{
		throwIfPresent(base);
		
		// A ridden mount attacks with its own stats, the rider's worn items do not count.
		if (creature.isPlayer())
		{
			final double rideValue = creature.asPlayer().getRideMAtk();
			if (rideValue > 0)
			{
				return rideValue;
			}
		}
		
		double baseValue = calcWeaponBaseValue(creature, stat);
		if (creature.getActiveWeaponInstance() != null)
		{
			final CreatureStat creatureStat = creature.getStat();
			baseValue += creatureStat.getWeaponBonusMAtk();
			baseValue *= creatureStat.getMul(Stat.WEAPON_BONUS_MAGIC_ATTACK_MULTIPIER, 1);
		}
		
		baseValue += calcEnchantedItemBonus(creature, stat);
		
		if (creature.isPlayer())
		{
			// Enchanted chest bonus
			baseValue += calcEnchantBodyPart(creature, BodyPart.CHEST, BodyPart.FULL_ARMOR);
		}
		
		if (ChampionMonstersConfig.CHAMPION_ENABLE && creature.isChampion())
		{
			baseValue *= ChampionMonstersConfig.CHAMPION_ATK;
		}
		
		if (creature.isRaid())
		{
			baseValue *= NpcConfig.RAID_MATTACK_MULTIPLIER;
		}
		
		// Calculate modifiers Magic Attack.
		final double chaBonus = creature.isPlayer() ? BaseStat.CHA.calcBonus(creature) : 1;
		baseValue *= Math.pow(BaseStat.INT.calcBonus(creature) * creature.getLevelMod() * chaBonus, 2.2072);
		return validateValue(creature, Stat.defaultValue(creature, stat, baseValue), 0, creature.isPlayable() ? PlayerConfig.MAX_MATK : Double.MAX_VALUE);
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
			return (2 * Math.max(enchantLevel - 3, 0)) + (2 * Math.max(enchantLevel - 6, 0));
		}
		
		return (1.4 * Math.max(enchantLevel - 3, 0)) + (1.4 * Math.max(enchantLevel - 6, 0));
	}
}
