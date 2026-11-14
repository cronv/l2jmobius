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
package org.l2jmobius.gameserver.entity.item;

import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.type.ArmorType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class is dedicated to the management of armors.<br>
 * It handles all logic and properties specific to armor items in the game world.
 */
public class Armor extends ItemTemplate
{
	private ArmorType _type;
	
	/**
	 * Creates a new {@code Armor} instance.<br>
	 * This constructor initializes the armor with a specific set of statistics.
	 * @param set The {@code StatSet} containing the attributes for this armor.
	 */
	public Armor(StatSet set)
	{
		super(set);
	}
	
	/**
	 * Updates the armor with a new set of statistics.<br>
	 * This method assigns the provided {@code StatSet} to the item.<br>
	 * It also determines the correct {@code ArmorType} based on the data.
	 * @param set The {@code StatSet} containing the armor properties.
	 */
	@Override
	public void set(StatSet set)
	{
		super.set(set);
		_type = set.getEnum("armor_type", ArmorType.class, ArmorType.NONE);
		
		final BodyPart bodyPart = getBodyPart();
		switch (bodyPart)
		{
			case ARTIFACT:
			case AGATHION:
			{
				_type1 = ItemTemplate.TYPE1_SHIELD_ARMOR;
				_type2 = ItemTemplate.TYPE2_ACCESSORY;
				break;
			}
			case NECK:
			case L_EAR:
			case R_EAR:
			case LR_EAR:
			case L_FINGER:
			case R_FINGER:
			case LR_FINGER:
			case R_BRACELET:
			case L_BRACELET:
			case ARTIFACT_BOOK:
			{
				_type1 = ItemTemplate.TYPE1_WEAPON_RING_EARRING_NECKLACE;
				_type2 = ItemTemplate.TYPE2_ACCESSORY;
				break;
			}
			default:
			{
				if ((_type == ArmorType.NONE) && (bodyPart == BodyPart.L_HAND))
				{
					_type = ArmorType.SHIELD;
				}
				
				_type1 = ItemTemplate.TYPE1_SHIELD_ARMOR;
				_type2 = ItemTemplate.TYPE2_SHIELD_ARMOR;
				break;
			}
		}
	}
	
	/**
	 * Retrieves the specific type of armor for this item.<br>
	 * This method returns the {@code ArmorType} associated with the current instance.
	 * @return the {@code ArmorType} of the item.
	 */
	@Override
	public ArmorType getItemType()
	{
		return _type;
	}
	
	/**
	 * Retrieves the bitmask associated with this armor type.<br>
	 * This value is used to identify specific categories of items.
	 * @return the integer mask for the current {@code ArmorType}.
	 */
	@Override
	public int getItemMask()
	{
		return _type.mask();
	}
	
	/**
	 * Checks if the item is an armor type.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the item is armor.
	 */
	@Override
	public boolean isArmor()
	{
		return true;
	}
}
