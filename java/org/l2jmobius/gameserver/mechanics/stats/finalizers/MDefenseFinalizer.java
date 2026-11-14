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
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.actor.stat.CreatureStat;
import org.l2jmobius.gameserver.entity.actor.transform.Transform;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.mechanics.stats.IStatFunction;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

/**
 * This class handles the final calculation of the Magic Defense stat.<br>
 * It applies specific modifiers to determine the final value used by {@code Creature}.
 * @author UnAfraid, Mobius
 */
public class MDefenseFinalizer implements IStatFunction
{
	private static final int[] SLOTS =
	{
		Inventory.PAPERDOLL_LFINGER,
		Inventory.PAPERDOLL_RFINGER,
		Inventory.PAPERDOLL_LEAR,
		Inventory.PAPERDOLL_REAR,
		Inventory.PAPERDOLL_NECK
	};
	
	/**
	 * Calculates the final Magic Defense value for a {@code Creature}.<br>
	 * This method accounts for base values, enchanted items, and equipment.<br>
	 * It also applies specific modifiers for players, pets, and raids.
	 * @param creature The {@code Creature} to calculate the stat for.
	 * @param base An {@code OptionalDouble} representing the base value.
	 * @param stat The {@code Stat} type being calculated.
	 * @return The final calculated double value for the statistic.
	 */
	@Override
	public double calc(Creature creature, OptionalDouble base, Stat stat)
	{
		throwIfPresent(base);
		double baseValue = creature.getTemplate().getBaseValue(stat, 0);
		if (creature.isPet())
		{
			final Pet pet = creature.asPet();
			baseValue = pet.getPetLevelData().getPetMDef();
		}
		
		baseValue += calcEnchantedItemBonus(creature, stat);
		
		final Inventory inv = creature.getInventory();
		if (inv != null)
		{
			for (Item item : inv.getPaperdollItems())
			{
				baseValue += item.getTemplate().getStats(stat, 0);
			}
		}
		
		if (creature.isPlayer())
		{
			final double accessoryMagicalDefence = creature.getStat().getValue(Stat.ARMOR_BONUS_MAGICAL_DEFENCE, 0) / 5;
			final Player player = creature.asPlayer();
			final Transform transform = creature.getTransformation();
			for (int slot : SLOTS)
			{
				if (!player.getInventory().isPaperdollSlotEmpty(slot))
				{
					baseValue -= transform == null ? player.getTemplate().getBaseDefBySlot(slot) : transform.getBaseDefBySlot(player, slot);
					if (accessoryMagicalDefence > 0)
					{
						baseValue += accessoryMagicalDefence;
					}
				}
			}
			
			baseValue *= BaseStat.CHA.calcBonus(creature);
		}
		else if (creature.isPet() && (creature.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_NECK) != 0))
		{
			baseValue -= 13;
		}
		
		if (creature.isRaid())
		{
			baseValue *= NpcConfig.RAID_MDEFENCE_MULTIPLIER;
		}
		
		final double bonus = creature.getMEN() > 0 ? BaseStat.MEN.calcBonus(creature) : 1;
		baseValue *= bonus * creature.getLevelMod();
		return defaultValue(creature, stat, baseValue);
	}
	
	/**
	 * Calculates the default value for a specific stat.<br>
	 * This method applies multipliers and additions from the {@code Creature}.<br>
	 * It also considers move type values and template base values.
	 * @param creature The {@code Creature} to check.
	 * @param stat The {@code Stat} being calculated.
	 * @param baseValue The initial {@code double} value for the stat.
	 * @return The final calculated {@code double} value.
	 */
	private double defaultValue(Creature creature, Stat stat, double baseValue)
	{
		final CreatureStat creatureStat = creature.getStat();
		final double mul = Math.max(creatureStat.getMul(stat), 0.5);
		final double add = creatureStat.getAdd(stat);
		return Math.max((baseValue * mul) + add + creatureStat.getMoveTypeValue(stat, creature.getMoveType()), creature.getTemplate().getBaseValue(stat, 0) * 0.2);
	}
}
