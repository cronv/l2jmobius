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
import org.l2jmobius.gameserver.data.holders.PetLevelData;
import org.l2jmobius.gameserver.data.xml.PetData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.type.SwampZone;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.mechanics.stats.IStatFunction;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

/**
 * This class handles the final calculation of a creature's speed stat.<br>
 * It applies specific modifiers such as zone effects and pet bonuses to determine the final value.
 * @author UnAfraid, Mobius
 */
public class SpeedFinalizer implements IStatFunction
{
	/**
	 * Calculates the final speed value for a given {@code Creature}.<br>
	 * This method applies bonuses from enchanted body parts and specific stats.<br>
	 * It also ensures the result stays within the allowed limits for players and summons.
	 * @param creature The {@code Creature} to calculate the stat for.
	 * @param base The {@code OptionalDouble} containing the base value of the stat.
	 * @param stat The {@code Stat} type being calculated.
	 * @return The final calculated speed as a {@code double}.
	 */
	@Override
	public double calc(Creature creature, OptionalDouble base, Stat stat)
	{
		throwIfPresent(base);
		
		final double staticSpeed = creature.getStat().getValue(Stat.STATIC_SPEED, 0);
		if (staticSpeed > 0)
		{
			return staticSpeed;
		}
		
		double baseValue = getBaseSpeed(creature, stat);
		
		// A ridden mount moves at its own speed, the rider's worn items do not count.
		if (creature.isPlayer() && creature.asPlayer().isMounted())
		{
			return Math.max(1, baseValue);
		}
		
		if (creature.isPlayer())
		{
			// Enchanted feet bonus
			baseValue += calcEnchantBodyPart(creature, BodyPart.FEET);
			
			// Match retail speed value.
			// Verified on 4game EU server, protocol 286.
			// Does not apply for Essence client.
			baseValue += 50;
		}
		
		final byte speedStat = (byte) creature.getStat().getAdd(Stat.STAT_BONUS_SPEED, -1);
		if ((speedStat >= 0) && (speedStat < BaseStat.values().length))
		{
			final BaseStat baseStat = BaseStat.values()[speedStat];
			final double bonusDex = Math.max(0, baseStat.calcValue(creature) - 55);
			baseValue += bonusDex;
		}
		
		final double maxSpeed;
		if (creature.isPlayer())
		{
			maxSpeed = creature.isGM() ? 10000 : PlayerConfig.MAX_RUN_SPEED + creature.getStat().getValue(Stat.SPEED_LIMIT, 0);
		}
		else if (creature.isSummon())
		{
			maxSpeed = PlayerConfig.MAX_RUN_SPEED_SUMMON + creature.getStat().getValue(Stat.SPEED_LIMIT, 0);
		}
		else
		{
			maxSpeed = Double.MAX_VALUE;
		}
		
		return validateValue(creature, Stat.defaultValue(creature, stat, baseValue), 1, maxSpeed);
	}
	
	/**
	 * Calculates the speed bonus from an enchanted body part.<br>
	 * This method considers both the enchantment level and whether the item is blessed.<br>
	 * It returns a {@code double} representing the final calculated bonus.
	 * @param enchantLevel The current level of the enchantment.
	 * @param isBlessed A boolean indicating if the item has the blessed property.
	 * @return The total speed bonus value.
	 */
	@Override
	public double calcEnchantBodyPartBonus(int enchantLevel, boolean isBlessed)
	{
		if (isBlessed)
		{
			return Math.max(enchantLevel - 3, 0) + Math.max(enchantLevel - 6, 0);
		}
		
		return (0.6 * Math.max(enchantLevel - 3, 0)) + (0.6 * Math.max(enchantLevel - 6, 0));
	}
	
	/**
	 * Calculates the base speed value for a specific creature.<br>
	 * This method accounts for weapon bonuses, mount stats, and player configurations.<br>
	 * It also applies penalties for hungry mounts or high level differences.<br>
	 * Finally, it adjusts the value based on swamp zone modifiers.
	 * @param creature The {@code Creature} to calculate speed for.
	 * @param stat The {@code Stat} type being queried.
	 * @return The final calculated base speed as a {@code double}.
	 */
	private double getBaseSpeed(Creature creature, Stat stat)
	{
		double baseValue = calcWeaponPlusBaseValue(creature, stat);
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			if (player.isMounted())
			{
				final int mountLevel = player.getMountLevel();
				final PetLevelData data = PetData.getInstance().getPetLevelData(player.getMountNpcId(), mountLevel);
				if (data != null)
				{
					baseValue = data.getSpeedOnRide(stat);
					
					// If level diff with mount >= 10, it decreases move speed by 50%.
					if ((mountLevel - creature.getLevel()) >= 10)
					{
						baseValue /= 2;
					}
					
					// If mount is hungry, it decreases move speed by 50%.
					if (player.isHungry())
					{
						baseValue /= 2;
					}
				}
			}
			
			baseValue += PlayerConfig.RUN_SPD_BOOST;
		}
		
		if (creature.isPlayable() && creature.isInsideZone(ZoneId.SWAMP))
		{
			final SwampZone zone = ZoneManager.getInstance().getZone(creature, SwampZone.class);
			if (zone != null)
			{
				baseValue *= zone.getMoveBonus();
			}
		}
		
		return baseValue;
	}
}
