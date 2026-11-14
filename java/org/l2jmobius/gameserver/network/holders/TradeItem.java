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
package org.l2jmobius.gameserver.network.holders;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.l2jmobius.gameserver.entity.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.ensoul.EnsoulOption;
import org.l2jmobius.gameserver.mechanics.options.AugmentationInstance;

/**
 * Represents an item instance that is currently being held within a trade window.<br>
 * This class acts as a wrapper for {@code Item} objects to manage their state during the trading process.
 */
public class TradeItem
{
	private int _objectId;
	private final ItemTemplate _item;
	private final int _location;
	private int _enchant;
	private final int _type1;
	private final int _type2;
	private long _count;
	private long _storeCount;
	private long _price;
	private byte _elemAtkType;
	private int _elemAtkPower;
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
	private Collection<EnsoulOption> _soulCrystalOptions;
	private Collection<EnsoulOption> _soulCrystalSpecialOptions;
	private int _visualId;
	private int _augmentationOption1 = 0;
	private int _augmentationOption2 = 0;
	private int _augmentationOption3 = 0;
	
	/**
	 * Creates a new {@code TradeItem} based on an existing {@code Item}.<br>
	 * This constructor copies all relevant properties from the provided {@code Item} object.
	 * @param item The source {@code Item} to copy data from.
	 * @param count The quantity of the item for the trade.
	 * @param price The cost of the item in the trade.
	 */
	public TradeItem(Item item, long count, long price)
	{
		Objects.requireNonNull(item);
		_objectId = item.getObjectId();
		_item = item.getTemplate();
		_location = item.getLocationSlot();
		_enchant = item.getEnchantLevel();
		_type1 = item.getCustomType1();
		_type2 = item.getCustomType2();
		_count = count;
		_price = price;
		_elemAtkType = item.getAttackAttributeType().getClientId();
		_elemAtkPower = item.getAttackAttributePower();
		for (AttributeType type : AttributeType.ATTRIBUTE_TYPES)
		{
			_elemDefAttr[type.getClientId()] = item.getDefenceAttribute(type);
		}
		
		_enchantOptions = item.getEnchantOptions();
		_soulCrystalOptions = item.getSpecialAbilities();
		_soulCrystalSpecialOptions = item.getAdditionalSpecialAbilities();
		_visualId = item.getVisualId();
		
		final AugmentationInstance augment = item.getAugmentation();
		if (augment != null)
		{
			_augmentationOption1 = augment.getOption1Id();
			_augmentationOption2 = augment.getOption2Id();
			_augmentationOption3 = augment.getOption3Id();
		}
	}
	
	/**
	 * Creates a new {@code TradeItem} instance using an item template.<br>
	 * This constructor initializes the default values for trade properties.
	 * @param item The {@code ItemTemplate} to use for this trade item.
	 * @param count The initial quantity of the item.
	 * @param price The cost of the item in the trade.
	 */
	public TradeItem(ItemTemplate item, long count, long price)
	{
		Objects.requireNonNull(item);
		_objectId = 0;
		_item = item;
		_location = 0;
		_enchant = 0;
		_type1 = 0;
		_type2 = 0;
		_count = count;
		_storeCount = count;
		_price = price;
		_elemAtkType = AttributeType.NONE.getClientId();
		_elemAtkPower = 0;
		_enchantOptions = Item.DEFAULT_ENCHANT_OPTIONS;
		_soulCrystalOptions = Collections.emptyList();
		_soulCrystalSpecialOptions = Collections.emptyList();
	}
	
	/**
	 * Creates a new {@code TradeItem} based on an existing {@code TradeItem}.<br>
	 * This constructor copies the properties of the provided {@code item}.<br>
	 * It also updates the count and price to the specified values.
	 * @param item The source {@code TradeItem} to copy data from.
	 * @param count The new quantity for this trade item.
	 * @param price The new price for this trade item.
	 */
	public TradeItem(TradeItem item, long count, long price)
	{
		Objects.requireNonNull(item);
		_objectId = item.getObjectId();
		_item = item.getItem();
		_location = item.getLocationSlot();
		_enchant = item.getEnchant();
		_type1 = item.getCustomType1();
		_type2 = item.getCustomType2();
		_count = count;
		_storeCount = count;
		_price = price;
		_elemAtkType = item.getAttackElementType();
		_elemAtkPower = item.getAttackElementPower();
		for (byte i = 0; i < 6; i++)
		{
			_elemDefAttr[i] = item.getElementDefAttr(i);
		}
		
		_enchantOptions = item.getEnchantOptions();
		_soulCrystalOptions = item.getSoulCrystalOptions();
		_soulCrystalSpecialOptions = item.getSoulCrystalSpecialOptions();
		_visualId = item.getVisualId();
	}
	
	/**
	 * Sets the unique identifier for this trade item.<br>
	 * This value is used to identify the specific instance in the game world.
	 * @param objectId The new {@code int} ID to assign to the object.
	 */
	public void setObjectId(int objectId)
	{
		_objectId = objectId;
	}
	
	/**
	 * Retrieves the unique identifier for this trade item.<br>
	 * This value is used to distinguish between different items in a trade.
	 * @return The {@code int} object ID.
	 */
	public int getObjectId()
	{
		return _objectId;
	}
	
	/**
	 * Retrieves the {@code ItemTemplate} associated with this trade item.<br>
	 * This method returns the base template used for the item.
	 * @return The {@code ItemTemplate} of the item.
	 */
	public ItemTemplate getItem()
	{
		return _item;
	}
	
	/**
	 * Retrieves the location slot of the trade item.<br>
	 * This value represents where the item is positioned in a trade window.
	 * @return The integer value of the location slot.
	 */
	public int getLocationSlot()
	{
		return _location;
	}
	
	/**
	 * Sets the enchantment level for this trade item.<br>
	 * This updates the internal {@code _enchant} value.
	 * @param enchant The new enchantment level to set.
	 */
	public void setEnchant(int enchant)
	{
		_enchant = enchant;
	}
	
	/**
	 * Retrieves the current enchantment level of the item.<br>
	 * This value represents the numeric strength of the enchant.
	 * @return The {@code int} value of the enchantment.
	 */
	public int getEnchant()
	{
		return _enchant;
	}
	
	/**
	 * Retrieves the first custom type value.<br>
	 * This value is stored in the {@code _type1} field.
	 * @return The integer value of {@code _type1}.
	 */
	public int getCustomType1()
	{
		return _type1;
	}
	
	/**
	 * Retrieves the second custom type value.<br>
	 * This value is stored in the {@code _type2} field.
	 * @return The integer value of the second custom type.
	 */
	public int getCustomType2()
	{
		return _type2;
	}
	
	/**
	 * Updates the quantity of the item.<br>
	 * This method sets the {@code _count} field to a new value.
	 * @param count The new amount for the item.
	 */
	public void setCount(long count)
	{
		_count = count;
	}
	
	/**
	 * Returns the current quantity of the item.<br>
	 * This value represents how many units are in this {@code TradeItem}.
	 * @return The total count of the items.
	 */
	public long getCount()
	{
		return _count;
	}
	
	/**
	 * Retrieves the quantity of items currently in the store.<br>
	 * This value is stored as a {@code long}.
	 * @return The total count of items for the store.
	 */
	public long getStoreCount()
	{
		return _storeCount;
	}
	
	/**
	 * Updates the price of the trade item.<br>
	 * This method sets the internal {@code _price} field to a new value.
	 * @param price The new price to assign to the item.
	 */
	public void setPrice(long price)
	{
		_price = price;
	}
	
	/**
	 * Retrieves the price of the trade item.<br>
	 * This value is returned as a {@code long}.
	 * @return The current price of the item.
	 */
	public long getPrice()
	{
		return _price;
	}
	
	/**
	 * Sets the element type for attacks.<br>
	 * This method updates the internal attribute based on the provided {@code AttributeType}.
	 * @param attackElement The new {@code AttributeType} to assign.
	 */
	public void setAttackElementType(AttributeType attackElement)
	{
		_elemAtkType = attackElement.getClientId();
	}
	
	/**
	 * Retrieves the element type of the attack.<br>
	 * This value is stored as a {@code byte}.
	 * @return The attack element type.
	 */
	public byte getAttackElementType()
	{
		return _elemAtkType;
	}
	
	/**
	 * Sets the power level of the attack element.<br>
	 * This value determines how much damage is dealt by the specific element.
	 * @param attackElementPower The new power value for the attack element.
	 */
	public void setAttackElementPower(int attackElementPower)
	{
		_elemAtkPower = attackElementPower;
	}
	
	/**
	 * Retrieves the power level of the attack element.<br>
	 * This value is used to calculate elemental damage.
	 * @return the current {@code int} value of the attack element power.
	 */
	public int getAttackElementPower()
	{
		return _elemAtkPower;
	}
	
	/**
	 * Sets the defense attribute value for a specific element.<br>
	 * This method updates the internal array using the client ID of the {@code AttributeType}.
	 * @param element The {@code AttributeType} to modify.
	 * @param value The new integer value to assign.
	 */
	public void setElementDefAttr(AttributeType element, int value)
	{
		_elemDefAttr[element.getClientId()] = value;
	}
	
	/**
	 * Retrieves the defense attribute value for a specific element.<br>
	 * This method accesses the internal array using the provided index.
	 * @param i The index of the element to retrieve.
	 * @return The integer value of the defense attribute.
	 */
	public int getElementDefAttr(byte i)
	{
		return _elemDefAttr[i];
	}
	
	/**
	 * Retrieves the enchantment options for this trade item.<br>
	 * This method returns the internal array of enchant values.
	 * @return an {@code int[]} containing the enchantment options.
	 */
	public int[] getEnchantOptions()
	{
		return _enchantOptions;
	}
	
	/**
	 * Sets the collection of {@code EnsoulOption} for this trade item.<br>
	 * This method updates the internal list of soul crystal options.
	 * @param soulCrystalOptions The collection of {@code EnsoulOption} to set.
	 */
	public void setSoulCrystalOptions(Collection<EnsoulOption> soulCrystalOptions)
	{
		_soulCrystalOptions = soulCrystalOptions;
	}
	
	/**
	 * Retrieves the list of soul crystal options for this trade item.<br>
	 * Returns an empty list if no options are set.
	 * @return a {@code Collection} of {@code EnsoulOption} objects.
	 */
	public Collection<EnsoulOption> getSoulCrystalOptions()
	{
		return _soulCrystalOptions == null ? Collections.emptyList() : _soulCrystalOptions;
	}
	
	/**
	 * Sets the special options for a {@code SoulCrystal}.<br>
	 * This method updates the internal collection of {@code EnsoulOption} objects.
	 * @param soulCrystalSpecialOptions The collection of special options to assign.
	 */
	public void setSoulCrystalSpecialOptions(Collection<EnsoulOption> soulCrystalSpecialOptions)
	{
		_soulCrystalSpecialOptions = soulCrystalSpecialOptions;
	}
	
	/**
	 * Retrieves the special options for soul crystals.<br>
	 * Returns an empty list if no special options are set.
	 * @return a {@code Collection} of {@code EnsoulOption} objects.
	 */
	public Collection<EnsoulOption> getSoulCrystalSpecialOptions()
	{
		return _soulCrystalSpecialOptions == null ? Collections.emptyList() : _soulCrystalSpecialOptions;
	}
	
	/**
	 * Updates the augmentation options for this item.<br>
	 * This method sets three specific integer values for the item's augmentations.
	 * @param option1 The first augmentation value.
	 * @param option2 The second augmentation value.
	 * @param option3 The third augmentation value.
	 */
	public void setAugmentation(int option1, int option2, int option3)
	{
		_augmentationOption1 = option1;
		_augmentationOption2 = option2;
		_augmentationOption3 = option3;
	}
	
	/**
	 * Retrieves the first augmentation option for this item.<br>
	 * This value is stored as an {@code int}.
	 * @return The value of the first augmentation option.
	 */
	public int getAugmentationOption1()
	{
		return _augmentationOption1;
	}
	
	/**
	 * Retrieves the second augmentation option for this item.<br>
	 * This value is stored as an {@code int}.
	 * @return The value of the second augmentation option.
	 */
	public int getAugmentationOption2()
	{
		return _augmentationOption2;
	}
	
	/**
	 * Retrieves the third augmentation option for this item.<br>
	 * This value is stored as an {@code int}.
	 * @return The value of the third augmentation option.
	 */
	public int getAugmentationOption3()
	{
		return _augmentationOption3;
	}
	
	/**
	 * Sets the unique identifier for the item's visual representation.<br>
	 * This value determines how the item appears to players in the game world.
	 * @param visualItemId The {@code int} ID of the visual model to use.
	 */
	public void setVisualId(int visualItemId)
	{
		_visualId = visualItemId;
	}
	
	/**
	 * Retrieves the unique visual identifier for this trade item.<br>
	 * This ID is used to determine how the item appears in the game world.
	 * @return The {@code int} value of the visual ID.
	 */
	public int getVisualId()
	{
		return _visualId;
	}
}
