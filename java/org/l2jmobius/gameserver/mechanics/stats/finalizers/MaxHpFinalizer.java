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
import org.l2jmobius.gameserver.data.xml.EnchantItemHPBonusData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.actor.stat.CreatureStat;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.mechanics.stats.IStatFunction;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

/**
 * This class calculates the final {@code maxHp} value for a creature.<br>
 * It applies various modifiers and bonuses from equipment and other sources to determine the total health capacity.
 * @author UnAfraid, Mobius
 */
public class MaxHpFinalizer implements IStatFunction
{
	/**
	 * Calculates the final maximum HP value for a given creature.<br>
	 * This method applies bonuses from stats like {@code CON} and {@code CHA}.<br>
	 * It handles specific logic for both players and pets.
	 * @param creature The {@code Creature} to calculate the stat for.
	 * @param base An {@code OptionalDouble} containing the base value.
	 * @param stat The {@code Stat} type being calculated.
	 * @return The final calculated double value for the maximum HP.
	 */
	@Override
	public double calc(Creature creature, OptionalDouble base, Stat stat)
	{
		throwIfPresent(base);
		
		double baseValue = creature.getTemplate().getBaseValue(stat, 0);
		if (creature.isPet())
		{
			final Pet pet = creature.asPet();
			baseValue = pet.getPetLevelData().getPetMaxHP();
		}
		else if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			if (player != null)
			{
				baseValue = player.getTemplate().getBaseHpMax(player.getLevel());
			}
		}
		
		final double chaBonus = creature.isPlayer() ? BaseStat.CHA.calcBonus(creature) : 1;
		final double conBonus = creature.getCON() > 0 ? BaseStat.CON.calcBonus(creature) : 1;
		baseValue *= conBonus * chaBonus;
		
		return defaultValue(creature, stat, baseValue);
	}
	
	/**
	 * Calculates the default value for a specific stat.<br>
	 * This method accounts for multipliers, additions, and equipment bonuses.<br>
	 * It also enforces maximum limits based on player configuration where applicable.
	 * @param creature The {@code Creature} whose stats are being calculated.
	 * @param stat The {@code Stat} type to calculate.
	 * @param baseValue The initial {@code double} value for the stat.
	 * @return The final calculated {@code double} value for the stat.
	 */
	private static double defaultValue(Creature creature, Stat stat, double baseValue)
	{
		final CreatureStat creatureStat = creature.getStat();
		double mul = creatureStat.getMul(stat);
		double add = creatureStat.getAdd(stat);
		
		double maxHp = (mul * baseValue) + add + creatureStat.getMoveTypeValue(stat, creature.getMoveType());
		final boolean isPlayer = creature.isPlayer();
		
		final Inventory inv = creature.getInventory();
		if (inv == null)
		{
			if (isPlayer)
			{
				if (creature.asPlayer().isCursedWeaponEquipped())
				{
					return Double.MAX_VALUE;
				}
				
				mul = creatureStat.getMul(Stat.HP_LIMIT);
				add = creatureStat.getAdd(Stat.HP_LIMIT);
				return Math.min(maxHp, (PlayerConfig.MAX_HP * mul) + add);
			}
			
			return maxHp;
		}
		
		boolean shouldLiftLimit = false;
		
		// Add maxHP bonus from items.
		for (Item item : inv.getPaperdollItems())
		{
			final ItemTemplate template = item.getTemplate();
			maxHp += template.getStats(stat, 0);
			
			// Apply enchanted item bonus HP.
			if (item.isArmor() && item.isEnchanted())
			{
				final BodyPart bodyPart = template.getBodyPart();
				if ((bodyPart != BodyPart.NECK) && (bodyPart != BodyPart.LR_EAR) && (bodyPart != BodyPart.LR_FINGER))
				{
					maxHp += EnchantItemHPBonusData.getInstance().getHPBonus(item);
				}
			}
			
			if (item.isWeapon() && item.getWeaponItem().isDragonWeapon())
			{
				shouldLiftLimit = true;
			}
		}
		
		final double hpLimit;
		if (isPlayer && !shouldLiftLimit && !creature.asPlayer().isCursedWeaponEquipped())
		{
			mul = creatureStat.getMul(Stat.HP_LIMIT);
			add = creatureStat.getAdd(Stat.HP_LIMIT);
			hpLimit = (PlayerConfig.MAX_HP * mul) + add;
		}
		else
		{
			hpLimit = Double.MAX_VALUE;
		}
		
		if (isPlayer)
		{
			return Math.min(Math.min(maxHp, hpLimit), Integer.MAX_VALUE);
		}
		
		return Math.min(maxHp, hpLimit);
	}
}
