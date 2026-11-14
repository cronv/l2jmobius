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

import java.util.Collection;
import java.util.Objects;

import org.l2jmobius.gameserver.entity.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.CrystalType;
import org.l2jmobius.gameserver.entity.item.type.ItemType;
import org.l2jmobius.gameserver.mechanics.ensoul.EnsoulOption;
import org.l2jmobius.gameserver.mechanics.options.AugmentationInstance;

/**
 * This class represents an {@code Item} stored within a warehouse.<br>
 * It is used to categorize and sort specific types such as Armor, Weapons, and EtcItems.
 */
public class WarehouseItem
{
	private final ItemTemplate _item;
	private final int _object;
	private final long _count;
	private final int _owner;
	private final int _locationSlot;
	private final int _enchant;
	private final CrystalType _grade;
	private final AugmentationInstance _augmentation;
	private final int _customType1;
	private final int _customType2;
	private final int _mana;
	
	private byte _elemAtkType = -2;
	private int _elemAtkPower = 0;
	
	private final int[] _elemDefAttr =
	{
		0,
		0,
		0,
		0,
		0,
		0
	};
	
	private final int[] _enchantOptions;
	private final Collection<EnsoulOption> _soulCrystalOptions;
	private final Collection<EnsoulOption> _soulCrystalSpecialOptions;
	
	private final int _time;
	
	/**
	 * Creates a new {@code WarehouseItem} from an existing {@code Item}.<br>
	 * This constructor copies all relevant data from the provided {@code Item} object.<br>
	 * The input {@code item} parameter must not be {@code null}.
	 * @param item The {@code Item} to copy data from.
	 */
	public WarehouseItem(Item item)
	{
		Objects.requireNonNull(item);
		final ItemTemplate template = item.getTemplate();
		_item = template;
		_object = item.getObjectId();
		_count = item.getCount();
		_owner = item.getOwnerId();
		_locationSlot = item.getLocationSlot();
		_enchant = item.getEnchantLevel();
		_customType1 = item.getCustomType1();
		_customType2 = item.getCustomType2();
		_grade = template.getCrystalType();
		_augmentation = item.getAugmentation();
		_mana = item.getMana();
		_time = item.isTimeLimitedItem() ? (int) (item.getRemainingTime() / 1000) : -1;
		_elemAtkType = item.getAttackAttributeType().getClientId();
		_elemAtkPower = item.getAttackAttributePower();
		for (AttributeType type : AttributeType.ATTRIBUTE_TYPES)
		{
			_elemDefAttr[type.getClientId()] = item.getDefenceAttribute(type);
		}
		
		_enchantOptions = item.getEnchantOptions();
		_soulCrystalOptions = item.getSpecialAbilities();
		_soulCrystalSpecialOptions = item.getAdditionalSpecialAbilities();
	}
	
	/**
	 * Retrieves the {@code ItemTemplate} associated with this warehouse item.<br>
	 * This provides access to the base data of the item.
	 * @return the {@code ItemTemplate} object.
	 */
	public ItemTemplate getItem()
	{
		return _item;
	}
	
	/**
	 * Retrieves the unique object identifier for this item.<br>
	 * This value is used to identify specific instances in the warehouse.
	 * @return The {@code int} object ID of the item.
	 */
	public int getObjectId()
	{
		return _object;
	}
	
	/**
	 * Retrieves the unique identifier of the owner.<br>
	 * This ID belongs to the player who owns this {@code WarehouseItem}.
	 * @return The {@code int} value representing the owner's ID.
	 */
	public int getOwnerId()
	{
		return _owner;
	}
	
	/**
	 * Retrieves the specific slot location of the item.<br>
	 * This value identifies where the item is stored in the warehouse.
	 * @return The integer value representing the location slot.
	 */
	public int getLocationSlot()
	{
		return _locationSlot;
	}
	
	/**
	 * Retrieves the total quantity of this item.<br>
	 * This value represents how many units are stored in the warehouse.
	 * @return The number of items as a {@code long}.
	 */
	public long getCount()
	{
		return _count;
	}
	
	/**
	 * Retrieves the first type value from the associated {@code ItemTemplate}.<br>
	 * This value is used to categorize specific item properties.
	 * @return the integer value of {@code getType1} from the template.
	 */
	public int getType1()
	{
		return _item.getType1();
	}
	
	/**
	 * Retrieves the secondary type of the item.<br>
	 * This value is fetched from the underlying {@code ItemTemplate}.
	 * @return the integer value of the second type.
	 */
	public int getType2()
	{
		return _item.getType2();
	}
	
	/**
	 * Retrieves the {@code ItemType} of this item.<br>
	 * This value is obtained from the underlying {@code ItemTemplate}.
	 * @return the {@code ItemType} of the item.
	 */
	public ItemType getItemType()
	{
		return _item.getItemType();
	}
	
	/**
	 * Retrieves the unique identifier for this item.<br>
	 * This value is obtained from the underlying {@code ItemTemplate}.
	 * @return The {@code int} ID of the item.
	 */
	public int getItemId()
	{
		return _item.getId();
	}
	
	/**
	 * Retrieves the body part associated with this item.<br>
	 * This value is taken from the underlying {@code ItemTemplate}.
	 * @return the {@code BodyPart} of the item.
	 */
	public BodyPart getBodyPart()
	{
		return _item.getBodyPart();
	}
	
	/**
	 * Returns the current enchantment level of the item.<br>
	 * This value is stored in the {@code _enchant} field.
	 * @return The integer level of the enchantment.
	 */
	public int getEnchantLevel()
	{
		return _enchant;
	}
	
	/**
	 * Retrieves the crystal grade of the item.<br>
	 * This value represents the quality level assigned to the object.
	 * @return the {@code CrystalType} of the item.
	 */
	public CrystalType getItemGrade()
	{
		return _grade;
	}
	
	/**
	 * Checks if the item is a weapon.<br>
	 * This method returns {@code true} if the item belongs to the {@code Weapon} class.<br>
	 * It returns {@code false} otherwise.
	 * @return {@code true} if the item is a weapon, {@code false} otherwise.
	 */
	public boolean isWeapon()
	{
		return _item instanceof Weapon;
	}
	
	/**
	 * Checks if the item is an instance of {@code Armor}.
	 * @return {@code true} if the item is armor, {@code false} otherwise.
	 */
	public boolean isArmor()
	{
		return _item instanceof Armor;
	}
	
	/**
	 * Checks if the item is an instance of {@code EtcItem}.
	 * @return {@code true} if the item is an {@code EtcItem}, otherwise {@code false}.
	 */
	public boolean isEtcItem()
	{
		return _item instanceof EtcItem;
	}
	
	/**
	 * Retrieves the display name of the item.<br>
	 * This method calls {@code getName()} on the internal {@code ItemTemplate}.
	 * @return The name of the item as a {@code String}.
	 */
	public String getItemName()
	{
		return _item.getName();
	}
	
	/**
	 * Retrieves the augmentation data for this item.<br>
	 * This returns a {@code AugmentationInstance} object.<br>
	 * It may return {@code null} if no augmentation exists.
	 * @return The {@code AugmentationInstance} associated with this item.
	 */
	public AugmentationInstance getAugmentation()
	{
		return _augmentation;
	}
	
	/**
	 * Retrieves the display name of the item.<br>
	 * This method returns the name from the underlying {@code ItemTemplate}.
	 * @return The name of the item as a {@code String}.
	 */
	public String getName()
	{
		return _item.getName();
	}
	
	/**
	 * Retrieves the first custom type value for this item.<br>
	 * This value is used to store additional specific data.
	 * @return the value of {@code _customType1} as an {@code int}.
	 */
	public int getCustomType1()
	{
		return _customType1;
	}
	
	/**
	 * Retrieves the second custom type value for this item.<br>
	 * This value is used to store additional metadata.
	 * @return the value of {@code _customType2} as an {@code int}.
	 */
	public int getCustomType2()
	{
		return _customType2;
	}
	
	/**
	 * Retrieves the mana value of the item.<br>
	 * This value is stored in the {@code _mana} field.
	 * @return The current mana amount as an {@code int}.
	 */
	public int getMana()
	{
		return _mana;
	}
	
	/**
	 * Retrieves the elemental type of the attack.<br>
	 * This value is used to determine which element the item uses for its damage.
	 * @return The {@code byte} representing the attack element type.
	 */
	public byte getAttackElementType()
	{
		return _elemAtkType;
	}
	
	/**
	 * Retrieves the power level of the attack element.<br>
	 * This value represents the strength of the elemental effect.
	 * @return the current {@code int} value of the attack element power.
	 */
	public int getAttackElementPower()
	{
		return _elemAtkPower;
	}
	
	/**
	 * Retrieves the defense attribute value for a specific element.<br>
	 * This method accesses the internal array using the provided index.
	 * @param i The index of the element to check.
	 * @return The integer value of the defense attribute.
	 */
	public int getElementDefAttr(byte i)
	{
		return _elemDefAttr[i];
	}
	
	/**
	 * Retrieves the enchantment options for this item.<br>
	 * This returns an array of integers representing specific enchant details.
	 * @return an {@code int[]} containing the enchantment options.
	 */
	public int[] getEnchantOptions()
	{
		return _enchantOptions;
	}
	
	/**
	 * Retrieves the soul crystal options for this item.<br>
	 * These options define the specific properties granted by a soul crystal.
	 * @return A {@code Collection} of {@code EnsoulOption} objects.
	 */
	public Collection<EnsoulOption> getSoulCrystalOptions()
	{
		return _soulCrystalOptions;
	}
	
	/**
	 * Retrieves the special options for soul crystals.<br>
	 * This method returns a collection of {@code EnsoulOption} objects.
	 * @return A {@code Collection} containing all special soul crystal options.
	 */
	public Collection<EnsoulOption> getSoulCrystalSpecialOptions()
	{
		return _soulCrystalSpecialOptions;
	}
	
	/**
	 * Retrieves the time associated with this warehouse item.<br>
	 * This value is stored in the internal {@code _time} field.
	 * @return The current time as an {@code int}.
	 */
	public int getTime()
	{
		return _time;
	}
	
	/**
	 * Returns the string representation of the underlying {@code ItemTemplate}.<br>
	 * This method delegates the call to the internal item template.
	 * @return The string representation of the item.
	 */
	@Override
	public String toString()
	{
		return _item.toString();
	}
}
