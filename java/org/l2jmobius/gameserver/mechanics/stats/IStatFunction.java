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
package org.l2jmobius.gameserver.mechanics.stats;

import java.util.OptionalDouble;

import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.actor.transform.Transform;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.CrystalType;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;

/**
 * Represents a functional interface for calculating or modifying character statistics.<br>
 * It allows for dynamic computation of stats based on various game mechanics and conditions.
 * @author UnAfraid, Mobius
 */
@FunctionalInterface
public interface IStatFunction
{
	/**
	 * Checks if the provided {@code OptionalDouble} contains a value.<br>
	 * Throws an {@code IllegalArgumentException} if a value is present.
	 * @param base The {@code OptionalDouble} to check.
	 */
	default void throwIfPresent(OptionalDouble base)
	{
		if (base.isPresent())
		{
			throw new IllegalArgumentException("base should not be set for " + getClass().getSimpleName());
		}
	}
	
	/**
	 * Calculates the total enchantment bonus from specific body parts.<br>
	 * It checks for items with an enchant level of 4 or higher and a red crystal type.<br>
	 * The sum is based on the individual bonuses of each valid item found.
	 * @param creature The {@code Creature} to check for equipped items.
	 * @param bodyParts The array of {@code BodyPart} types to evaluate.
	 * @return The total calculated enchantment bonus as a {@code double}.
	 */
	default double calcEnchantBodyPart(Creature creature, BodyPart... bodyParts)
	{
		double value = 0;
		for (BodyPart bodyPart : bodyParts)
		{
			final Item item = creature.getInventory().getPaperdollItemByBodyPart(bodyPart);
			if ((item != null) && (item.getEnchantLevel() >= 4) && ((item.getTemplate().getCrystalTypePlus() == CrystalType.R) || (item.getTemplate().getCrystalTypePlus() == CrystalType.L)))
			{
				value += calcEnchantBodyPartBonus(item.getEnchantLevel(), item.getTemplate().isBlessed());
			}
		}
		
		return value;
	}
	
	/**
	 * Calculates the bonus for a body part based on enchantment. <br>
	 * This method uses the provided level and blessed status.
	 * @param enchantLevel The current level of the enchantment.
	 * @param isBlessed Whether the item has the blessed property.
	 * @return The calculated bonus value as a {@code double}.
	 */
	default double calcEnchantBodyPartBonus(int enchantLevel, boolean isBlessed)
	{
		return 0;
	}
	
	/**
	 * Calculates the base value of a specific {@code Stat} for a given {@code Creature}.<br>
	 * This method accounts for transformations, pet levels, and active weapons.
	 * @param creature The {@code Creature} to calculate the stat for.
	 * @param stat The {@code Stat} type to retrieve.
	 * @return The calculated base value as a {@code double}.
	 */
	default double calcWeaponBaseValue(Creature creature, Stat stat)
	{
		final double baseTemplateValue = creature.getTemplate().getBaseValue(stat, 0);
		final Transform transform = creature.getTransformation();
		double baseValue = transform == null ? baseTemplateValue : transform.getStats(creature, stat, baseTemplateValue);
		if (creature.isPet())
		{
			final Pet pet = creature.asPet();
			final Item weapon = pet.getActiveWeaponInstance();
			final double baseVal = stat == Stat.PHYSICAL_ATTACK ? pet.getPetLevelData().getPetPAtk() : stat == Stat.MAGIC_ATTACK ? pet.getPetLevelData().getPetMAtk() : baseTemplateValue;
			baseValue = baseVal + (weapon != null ? weapon.getTemplate().getStats(stat, baseVal) : 0);
		}
		else if (creature.isPlayer() && ((transform == null) || transform.canUseWeaponStats()))
		{
			final Item weapon = creature.getActiveWeaponInstance();
			baseValue = ((weapon != null) && !weapon.isDamaged() ? weapon.getTemplate().getStats(stat, baseTemplateValue) : baseTemplateValue);
		}
		
		return baseValue;
	}
	
	/**
	 * Calculates the total base value for a specific {@code Stat}.<br>
	 * This includes values from the creature template and transformations.<br>
	 * It also adds equipment bonuses if the creature is playable.
	 * @param creature The {@code Creature} to calculate stats for.
	 * @param stat The {@code Stat} type to evaluate.
	 * @return The final calculated base value as a {@code double}.
	 */
	default double calcWeaponPlusBaseValue(Creature creature, Stat stat)
	{
		final double baseTemplateValue = creature.getTemplate().getBaseValue(stat, 0);
		final Transform transform = creature.getTransformation();
		double baseValue = (transform != null) && !transform.isStance() ? transform.getStats(creature, stat, baseTemplateValue) : baseTemplateValue;
		if (creature.isPlayable())
		{
			final Inventory inv = creature.getInventory();
			if (inv != null)
			{
				baseValue += inv.getPaperdollCache().getStats(stat);
			}
		}
		
		return baseValue;
	}
	
	/**
	 * Calculates the total bonus from enchanted items for a specific stat.<br>
	 * This method only applies to {@code Creature} objects that are players.<br>
	 * It iterates through all equipped enchanted items in the player's inventory.<br>
	 * The calculation accounts for blessing bonuses and olympiad enchant limits.
	 * @param creature The {@code Creature} whose stats are being calculated.
	 * @param stat The specific {@code Stat} to calculate the bonus for.
	 * @return The total calculated bonus as a {@code double}.
	 */
	default double calcEnchantedItemBonus(Creature creature, Stat stat)
	{
		if (!creature.isPlayer())
		{
			return 0;
		}
		
		double value = 0;
		for (Item equippedItem : creature.getInventory().getPaperdollItems(Item::isEnchanted))
		{
			if (equippedItem.isDamaged())
			{
				continue;
			}
			
			final ItemTemplate item = equippedItem.getTemplate();
			final BodyPart bodyPart = item.getBodyPart();
			if ((bodyPart == BodyPart.HAIR) || //
				(bodyPart == BodyPart.HAIR2) || //
				(bodyPart == BodyPart.HAIRALL))
			{
				// TODO: Item after enchant shows pDef, but scroll says mDef increase.
				if ((stat != Stat.PHYSICAL_DEFENCE) && (stat != Stat.MAGICAL_DEFENCE))
				{
					continue;
				}
			}
			else if (item.getStats(stat, 0) <= 0)
			{
				continue;
			}
			
			final double blessedBonus = item.isBlessed() ? 1.5 : 1;
			int enchant = equippedItem.getEnchantLevel();
			
			if (creature.asPlayer().isInOlympiadMode())
			{
				if (item.isWeapon())
				{
					if ((OlympiadConfig.OLYMPIAD_WEAPON_ENCHANT_LIMIT >= 0) && (enchant > OlympiadConfig.OLYMPIAD_WEAPON_ENCHANT_LIMIT))
					{
						enchant = OlympiadConfig.OLYMPIAD_WEAPON_ENCHANT_LIMIT;
					}
				}
				else
				{
					if ((OlympiadConfig.OLYMPIAD_ARMOR_ENCHANT_LIMIT >= 0) && (enchant > OlympiadConfig.OLYMPIAD_ARMOR_ENCHANT_LIMIT))
					{
						enchant = OlympiadConfig.OLYMPIAD_ARMOR_ENCHANT_LIMIT;
					}
				}
			}
			
			if ((stat == Stat.MAGICAL_DEFENCE) || (stat == Stat.PHYSICAL_DEFENCE))
			{
				value += calcEnchantDefBonus(equippedItem, blessedBonus, enchant);
			}
			else if (stat == Stat.MAGIC_ATTACK)
			{
				value += calcEnchantMatkBonus(equippedItem, blessedBonus, enchant);
			}
			else if ((stat == Stat.PHYSICAL_ATTACK) && equippedItem.isWeapon())
			{
				value += calcEnchantedPAtkBonus(equippedItem, blessedBonus, enchant);
			}
		}
		
		return value;
	}
	
	/**
	 * Calculates the defense bonus for an enchanted item.<br>
	 * The calculation depends on the item's crystal type and enchantment level.<br>
	 * It applies a specific formula if the item is of type {@code R}.<br>
	 * Otherwise, it uses a standard linear growth formula.
	 * @param item The {@code Item} being checked for its properties.
	 * @param blessedBonus The bonus value provided by the blessed status.
	 * @param enchant The current enchantment level of the item.
	 * @return The calculated defense bonus as a {@code double}.
	 */
	static double calcEnchantDefBonus(Item item, double blessedBonus, int enchant)
	{
		switch (item.getTemplate().getCrystalTypePlus())
		{
			case L:
			case R:
			{
				return ((2 * blessedBonus * enchant) + (6 * blessedBonus * Math.max(0, enchant - 3)));
			}
			default:
			{
				return enchant + (2 * Math.max(0, enchant - 3));
			}
		}
	}
	
	/**
	 * Calculates the Magic Attack bonus for an enchanted item.<br>
	 * The result depends on the crystal type of the item template.<br>
	 * It considers the enchantment level and the blessed bonus.
	 * @param item The {@code Item} instance to check.
	 * @param blessedBonus The multiplier for the blessed status.
	 * @param enchant The current enchantment level.
	 * @return The calculated Magic Attack bonus as a {@code double}.
	 */
	static double calcEnchantMatkBonus(Item item, double blessedBonus, int enchant)
	{
		switch (item.getTemplate().getCrystalTypePlus())
		{
			case L:
			case R:
			{
				return ((5 * blessedBonus * enchant) + (10 * blessedBonus * Math.max(0, enchant - 3)));
			}
			case S:
			{
				// M. Atk. increases by 4 for all weapons.
				// Starting at +4, M. Atk. bonus double.
				return (4 * enchant) + (4 * Math.max(0, enchant - 3));
			}
			case A:
			case B:
			case C:
			{
				// M. Atk. increases by 3 for all weapons.
				// Starting at +4, M. Atk. bonus double.
				return (3 * enchant) + (3 * Math.max(0, enchant - 3));
			}
			default:
			{
				// M. Atk. increases by 2 for all weapons. Starting at +4, M. Atk. bonus double.
				// Starting at +4, M. Atk. bonus double.
				return (2 * enchant) + (2 * Math.max(0, enchant - 3));
			}
		}
	}
	
	/**
	 * Calculates the Physical Attack bonus for an enchanted item.<br>
	 * The calculation depends on the crystal type and weapon category.<br>
	 * It applies a multiplier based on the enchantment level.
	 * @param item The {@code Item} being evaluated.
	 * @param blessedBonus The bonus value from the blessed status.
	 * @param enchant The current enchantment level of the item.
	 * @return The calculated Physical Attack bonus as a {@code double}.
	 */
	static double calcEnchantedPAtkBonus(Item item, double blessedBonus, int enchant)
	{
		switch (item.getTemplate().getCrystalTypePlus())
		{
			case L:
			case R:
			{
				if ((item.getWeaponItem().getBodyPart() == BodyPart.LR_HAND) && (item.getWeaponItem().getItemType() != WeaponType.POLE))
				{
					if (item.getWeaponItem().getItemType().isRanged())
					{
						return (12 * blessedBonus * enchant) + (24 * blessedBonus * Math.max(0, enchant - 3));
					}
					
					return (7 * blessedBonus * enchant) + (14 * blessedBonus * Math.max(0, enchant - 3));
				}
				
				return (6 * blessedBonus * enchant) + (12 * blessedBonus * Math.max(0, enchant - 3));
			}
			case S:
			{
				if ((item.getWeaponItem().getBodyPart() == BodyPart.LR_HAND) && (item.getWeaponItem().getItemType() != WeaponType.POLE))
				{
					if (item.getWeaponItem().getItemType().isRanged())
					{
						// P. Atk. increases by 10 for bows.
						// Starting at +4, P. Atk. bonus double.
						return (10 * enchant) + (10 * Math.max(0, enchant - 3));
					}
					
					// P. Atk. increases by 6 for two-handed swords, two-handed blunts, dualswords, and two-handed combat weapons.
					// Starting at +4, P. Atk. bonus double.
					return (6 * enchant) + (6 * Math.max(0, enchant - 3));
				}
				
				// P. Atk. increases by 5 for one-handed swords, one-handed blunts, daggers, spears, and other weapons.
				// Starting at +4, P. Atk. bonus double.
				return (5 * enchant) + (5 * Math.max(0, enchant - 3));
			}
			case A:
			{
				if ((item.getWeaponItem().getBodyPart() == BodyPart.LR_HAND) && (item.getWeaponItem().getItemType() != WeaponType.POLE))
				{
					if (item.getWeaponItem().getItemType().isRanged())
					{
						// P. Atk. increases by 8 for bows.
						// Starting at +4, P. Atk. bonus double.
						return (8 * enchant) + (8 * Math.max(0, enchant - 3));
					}
					
					// P. Atk. increases by 5 for two-handed swords, two-handed blunts, dualswords, and two-handed combat weapons.
					// Starting at +4, P. Atk. bonus double.
					return (5 * enchant) + (5 * Math.max(0, enchant - 3));
				}
				
				// P. Atk. increases by 4 for one-handed swords, one-handed blunts, daggers, spears, and other weapons.
				// Starting at +4, P. Atk. bonus double.
				return (4 * enchant) + (4 * Math.max(0, enchant - 3));
			}
			case B:
			case C:
			{
				if ((item.getWeaponItem().getBodyPart() == BodyPart.LR_HAND) && (item.getWeaponItem().getItemType() != WeaponType.POLE))
				{
					if (item.getWeaponItem().getItemType().isRanged())
					{
						// P. Atk. increases by 6 for bows.
						// Starting at +4, P. Atk. bonus double.
						return (6 * enchant) + (6 * Math.max(0, enchant - 3));
					}
					
					// P. Atk. increases by 4 for two-handed swords, two-handed blunts, dualswords, and two-handed combat weapons.
					// Starting at +4, P. Atk. bonus double.
					return (4 * enchant) + (4 * Math.max(0, enchant - 3));
				}
				
				// P. Atk. increases by 3 for one-handed swords, one-handed blunts, daggers, spears, and other weapons.
				// Starting at +4, P. Atk. bonus double.
				return (3 * enchant) + (3 * Math.max(0, enchant - 3));
			}
			default:
			{
				if (item.getWeaponItem().getItemType().isRanged())
				{
					// Bows increase by 4.
					// Starting at +4, P. Atk. bonus double.
					return (4 * enchant) + (4 * Math.max(0, enchant - 3));
				}
				
				// P. Atk. increases by 2 for all weapons with the exception of bows.
				// Starting at +4, P. Atk. bonus double.
				return (2 * enchant) + (2 * Math.max(0, enchant - 3));
			}
		}
	}
	
	/**
	 * Checks if a given value is within the allowed range.<br>
	 * It caps the result between {@code minValue} and {@code maxValue}.<br>
	 * This method ensures that stats do not exceed defined limits.
	 * @param creature The {@code Creature} being checked.
	 * @param value The raw value to validate.
	 * @param minValue The minimum allowed value.
	 * @param maxValue The maximum allowed value.
	 * @return The validated double value.
	 */
	default double validateValue(Creature creature, double value, double minValue, double maxValue)
	{
		if (value > maxValue) // && !creature.isGM()
		{
			return maxValue;
		}
		
		return Math.max(minValue, value);
	}
	
	double calc(Creature creature, OptionalDouble base, Stat stat);
}
