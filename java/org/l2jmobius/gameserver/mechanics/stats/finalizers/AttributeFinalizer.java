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

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.entity.item.enchant.attribute.AttributeHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.stats.IStatFunction;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

/**
 * This class handles the final calculation of attributes for a {@code Creature}.<br>
 * It ensures that all modifiers from items and other sources are correctly applied to the final stats.
 * @author UnAfraid
 */
public class AttributeFinalizer implements IStatFunction
{
	private final AttributeType _type;
	private final boolean _isWeapon;
	
	/**
	 * Creates a new instance of {@code AttributeFinalizer}.<br>
	 * This constructor initializes the attribute type and weapon status.
	 * @param type The {@code AttributeType} to be processed.
	 * @param isWeapon Set to {@code true} if the attribute belongs to a weapon, otherwise {@code false}.
	 */
	public AttributeFinalizer(AttributeType type, boolean isWeapon)
	{
		_type = type;
		_isWeapon = isWeapon;
	}
	
	/**
	 * Calculates the final value of a specific statistic for a {@code Creature}.<br>
	 * This method adds attributes from equipment based on whether it is a weapon or armor.<br>
	 * It ensures that both instance and template values are included in the calculation.
	 * @param creature The {@code Creature} whose stats are being calculated.
	 * @param base An {@code OptionalDouble} representing the base value of the stat.
	 * @param stat The {@code Stat} type to be processed.
	 * @return The final calculated value as a {@code double}.
	 */
	@Override
	public double calc(Creature creature, OptionalDouble base, Stat stat)
	{
		throwIfPresent(base);
		
		double baseValue = creature.getTemplate().getBaseValue(stat, 0);
		if (creature.isPlayable())
		{
			if (_isWeapon)
			{
				final Item weapon = creature.getActiveWeaponInstance();
				if (weapon != null)
				{
					final AttributeHolder weaponInstanceHolder = weapon.getAttribute(_type);
					if (weaponInstanceHolder != null)
					{
						baseValue += weaponInstanceHolder.getValue();
					}
					
					final AttributeHolder weaponHolder = weapon.getTemplate().getAttribute(_type);
					if (weaponHolder != null)
					{
						baseValue += weaponHolder.getValue();
					}
				}
			}
			else
			{
				final Inventory inventory = creature.getInventory();
				if (inventory != null)
				{
					for (Item item : inventory.getPaperdollItems(Item::isArmor))
					{
						final AttributeHolder weaponInstanceHolder = item.getAttribute(_type);
						if (weaponInstanceHolder != null)
						{
							baseValue += weaponInstanceHolder.getValue();
						}
						
						final AttributeHolder weaponHolder = item.getTemplate().getAttribute(_type);
						if (weaponHolder != null)
						{
							baseValue += weaponHolder.getValue();
						}
					}
				}
			}
		}
		
		return Stat.defaultValue(creature, stat, baseValue);
	}
}
