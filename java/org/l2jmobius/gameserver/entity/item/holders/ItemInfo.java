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
package org.l2jmobius.gameserver.entity.item.holders;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.WarehouseItem;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.buylist.Product;
import org.l2jmobius.gameserver.mechanics.ensoul.EnsoulOption;
import org.l2jmobius.gameserver.mechanics.options.AugmentationInstance;
import org.l2jmobius.gameserver.network.holders.TradeItem;

/**
 * This class serves as a data container that aggregates all relevant properties of an {@code Item}.<br>
 * It is used to simplify the retrieval and handling of item information across different game systems.
 */
public class ItemInfo
{
	/** Identifier of the Item */
	private int _objectId;
	
	/** The Item template of the Item */
	private ItemTemplate _item;
	
	/** The level of enchant on the Item */
	private int _enchantLevel;
	
	/** The augmentation of the item */
	private AugmentationInstance _augmentation;
	
	/** The quantity of Item */
	private long _count;
	
	/** The price of the Item */
	private int _price;
	
	/** The custom Item types (used loto, race tickets) */
	private int _type1;
	private int _type2;
	
	/** If True the Item is equipped */
	private int _equipped;
	private boolean _virtual = false;
	
	/** The action to do clientside (1=ADD, 2=MODIFY, 3=REMOVE) */
	private int _change;
	
	/** The mana of this item */
	private int _mana;
	private int _time;
	
	private boolean _available = true;
	
	private boolean _damaged = false;
	
	private int _location;
	
	private byte _elemAtkType = -2;
	private int _elemAtkPower = 0;
	private final int[] _attributeDefence =
	{
		0,
		0,
		0,
		0,
		0,
		0
	};
	
	private int[] _option;
	private Collection<EnsoulOption> _soulCrystalOptions;
	private Collection<EnsoulOption> _soulCrystalSpecialOptions;
	private int _visualId;
	private long _visualExpiration;
	
	private int _reuseDelay;
	private Player _owner;
	
	/**
	 * Creates a new {@code ItemInfo} instance from an {@code Item}.<br>
	 * This constructor copies all relevant data from the provided {@code Item} object.<br>
	 * The input {@code item} parameter must not be {@code null}.
	 * @param item The {@code Item} to extract information from.
	 */
	public ItemInfo(Item item)
	{
		Objects.requireNonNull(item);
		
		// Get the Identifier of the Item.
		_objectId = item.getObjectId();
		
		// Get the Item of the Item.
		_item = item.getTemplate();
		
		// Get the enchant level of the Item.
		_enchantLevel = item.getEnchantLevel();
		
		// Get the augmentation bonus.
		_augmentation = item.getAugmentation();
		
		// Get the quantity of the Item.
		_count = item.getCount();
		
		// Get custom item types (used loto, race tickets).
		_type1 = item.getCustomType1();
		_type2 = item.getCustomType2();
		
		// Verify if the Item is equipped.
		_equipped = item.isEquipped() ? 1 : 0;
		_virtual = item.isVirtual();
		
		// Verify if the item is damaged.
		_damaged = item.isDamaged();
		
		// Get the action to do clientside.
		switch (item.getLastChange())
		{
			case Item.ADDED:
			{
				_change = 1;
				break;
			}
			case Item.MODIFIED:
			{
				_change = 2;
				break;
			}
			case Item.REMOVED:
			{
				_change = 3;
				break;
			}
		}
		
		// Get shadow item mana.
		_mana = item.getMana();
		final long visualLifeTime = item.getVisualLifeTime();
		_time = item.isTimeLimitedItem() ? (int) (item.getRemainingTime() / 1000) : -9999;
		_available = item.isAvailable();
		_location = item.getLocationSlot();
		_elemAtkType = item.getAttackAttributeType().getClientId();
		_elemAtkPower = item.getAttackAttributePower();
		for (AttributeType type : AttributeType.ATTRIBUTE_TYPES)
		{
			_attributeDefence[type.getClientId()] = item.getDefenceAttribute(type);
		}
		
		_option = item.getEnchantOptions();
		_soulCrystalOptions = item.getSpecialAbilities();
		_soulCrystalSpecialOptions = item.getAdditionalSpecialAbilities();
		_visualId = item.getVisualId();
		_visualExpiration = visualLifeTime > 0 ? (int) ((visualLifeTime - System.currentTimeMillis()) / 1000) : 0;
		_reuseDelay = item.getReuseDelay();
		_owner = item.asPlayer();
	}
	
	/**
	 * Creates a new {@code ItemInfo} object from an {@code Item}.<br>
	 * This constructor also sets the specific change action.<br>
	 * It calculates the visual expiration based on the item's life time.
	 * @param item The {@code Item} to use for creating the information.
	 * @param change The integer value representing the client-side action.
	 */
	public ItemInfo(Item item, int change)
	{
		this(item);
		_change = change;
		final long visualLifeTime = item.getVisualLifeTime();
		_visualExpiration = visualLifeTime > 0 ? (visualLifeTime - System.currentTimeMillis()) / 1000 : 0;
	}
	
	/**
	 * Creates a new {@code ItemInfo} instance from a {@code TradeItem}.<br>
	 * This constructor extracts all necessary data from the provided {@code TradeItem}.<br>
	 * It handles null checks and populates fields like quantity, price, and attributes.
	 * @param item The {@code TradeItem} to copy information from.
	 */
	public ItemInfo(TradeItem item)
	{
		if (item == null)
		{
			return;
		}
		
		// Get the Identifier of the Item.
		_objectId = item.getObjectId();
		
		// Get the Item of the Item.
		_item = item.getItem();
		
		// Get the enchant level of the Item.
		_enchantLevel = item.getEnchant();
		
		// Get the augmentation bonus.
		final int augOption = item.getAugmentationOption1();
		final int augOption2 = item.getAugmentationOption2();
		final int augOption3 = item.getAugmentationOption3();
		if ((augOption > 0) || (augOption2 > 0) || (augOption3 > 0))
		{
			_augmentation = new AugmentationInstance(0, augOption, augOption2, augOption3);
		}
		
		// Get the quantity of the Item.
		_count = item.getCount();
		
		// Get custom item types (used loto, race tickets).
		_type1 = item.getCustomType1();
		_type2 = item.getCustomType2();
		
		// Verify if the Item is equipped.
		_equipped = 0;
		
		// Get the action to do clientside.
		_change = 0;
		
		// Get shadow item mana.
		_mana = -1;
		_time = -9999;
		_location = item.getLocationSlot();
		_elemAtkType = item.getAttackElementType();
		_elemAtkPower = item.getAttackElementPower();
		for (byte i = 0; i < 6; i++)
		{
			_attributeDefence[i] = item.getElementDefAttr(i);
		}
		
		_option = item.getEnchantOptions();
		_soulCrystalOptions = item.getSoulCrystalOptions();
		_soulCrystalSpecialOptions = item.getSoulCrystalSpecialOptions();
		_visualId = item.getVisualId();
	}
	
	/**
	 * Creates a new {@code ItemInfo} instance from a {@code Product}.<br>
	 * This constructor extracts data from the provided {@code Product} object.<br>
	 * It initializes fields like count and custom types.<br>
	 * If the input is {@code null}, no action is taken.
	 * @param item The {@code Product} to use for initializing the information.
	 */
	public ItemInfo(Product item)
	{
		if (item == null)
		{
			return;
		}
		
		// Get the Identifier of the Item.
		_objectId = 0;
		
		// Get the Item of the Item.
		_item = item.getItem();
		
		// Get the enchant level of the Item.
		_enchantLevel = 0;
		
		// Get the augmentation bonus.
		_augmentation = null;
		
		// Get the quantity of the Item.
		_count = item.getCount();
		
		// Get custom item types (used loto, race tickets).
		_type1 = _item.getType1();
		_type2 = _item.getType2();
		
		// Verify if the Item is equipped.
		_equipped = 0;
		
		// Get the action to do clientside.
		_change = 0;
		
		// Get shadow item mana.
		_mana = -1;
		_time = -9999;
		_location = 0;
		_soulCrystalOptions = Collections.emptyList();
		_soulCrystalSpecialOptions = Collections.emptyList();
	}
	
	/**
	 * Creates a new {@code ItemInfo} instance from a {@code WarehouseItem}.<br>
	 * This constructor copies all relevant data from the provided warehouse item.<br>
	 * It handles null checks to ensure the operation is safe.
	 * @param item The {@code WarehouseItem} to copy information from.
	 */
	public ItemInfo(WarehouseItem item)
	{
		if (item == null)
		{
			return;
		}
		
		// Get the Identifier of the Item.
		_objectId = item.getObjectId();
		
		// Get the Item of the Item.
		_item = item.getItem();
		
		// Get the enchant level of the Item.
		_enchantLevel = item.getEnchantLevel();
		
		// Get the augmentation bonus.
		_augmentation = item.getAugmentation();
		
		// Get the quantity of the Item.
		_count = item.getCount();
		
		// Get custom item types (used loto, race tickets).
		_type1 = item.getCustomType1();
		_type2 = item.getCustomType2();
		
		// Verify if the Item is equipped.
		_equipped = 0;
		
		// Get shadow item mana.
		_mana = item.getMana();
		_time = item.getTime();
		_location = item.getLocationSlot();
		_elemAtkType = item.getAttackElementType();
		_elemAtkPower = item.getAttackElementPower();
		for (byte i = 0; i < 6; i++)
		{
			_attributeDefence[i] = item.getElementDefAttr(i);
		}
		
		_option = item.getEnchantOptions();
		_soulCrystalOptions = item.getSoulCrystalOptions();
		_soulCrystalSpecialOptions = item.getSoulCrystalSpecialOptions();
	}
	
	/**
	 * Retrieves the unique identifier of the item.<br>
	 * This value corresponds to the {@code objectId} field.
	 * @return The integer ID of the item.
	 */
	public int getObjectId()
	{
		return _objectId;
	}
	
	/**
	 * Retrieves the {@code ItemTemplate} associated with this item.<br>
	 * This provides access to the base properties of the item.
	 * @return the {@code ItemTemplate} object.
	 */
	public ItemTemplate getItem()
	{
		return _item;
	}
	
	/**
	 * Retrieves the current enchantment level of the item.<br>
	 * This value represents how many times the item has been enchanted.
	 * @return The integer level of the enchantment.
	 */
	public int getEnchantLevel()
	{
		return _enchantLevel;
	}
	
	public boolean isDamaged()
	{
		return _damaged;
	}
	
	/**
	 * Retrieves the augmentation associated with this item.
	 * @return the {@code AugmentationInstance} of the augmentation.
	 */
	public AugmentationInstance getAugmentation()
	{
		return _augmentation;
	}
	
	/**
	 * Retrieves the total quantity of the item.
	 * @return The number of items as a {@code long}.
	 */
	public long getCount()
	{
		return _count;
	}
	
	/**
	 * Retrieves the price of the item.<br>
	 * This value is stored in the {@code _price} field.
	 * @return The current price as an {@code int}.
	 */
	public int getPrice()
	{
		return _price;
	}
	
	/**
	 * Retrieves the first custom type identifier for the item.<br>
	 * This value is used for specific items like loto or race tickets.
	 * @return The integer value of {@code _type1}.
	 */
	public int getCustomType1()
	{
		return _type1;
	}
	
	/**
	 * Retrieves the second custom type of the item.<br>
	 * This value is used for specific items like race tickets.
	 * @return the value of {@code _type2} as an {@code int}.
	 */
	public int getCustomType2()
	{
		return _type2;
	}
	
	/**
	 * Checks if the item is currently equipped.
	 * @return The equipment status of the item as an {@code int}.
	 */
	/**
	 * Checks whether this item is a virtual one, granted by the illusory equipment system.
	 * @return {@code true} when the item is virtual, {@code false} otherwise.
	 */
	public boolean isVirtual()
	{
		return _virtual;
	}
	
	public int getEquipped()
	{
		return _equipped;
	}
	
	/**
	 * Retrieves the client-side action type for this item.<br>
	 * The value represents an action such as {@code 1} for ADD, {@code 2} for MODIFY, or {@code 3} for REMOVE.
	 * @return the integer value of the change action.
	 */
	public int getChange()
	{
		return _change;
	}
	
	/**
	 * Retrieves the mana value of this item.
	 * @return the current {@code int} mana value.
	 */
	public int getMana()
	{
		return _mana;
	}
	
	/**
	 * Retrieves the time value associated with this item.<br>
	 * It checks for a positive {@code _time} first.<br>
	 * If that is not available, it returns the {@code _visualExpiration}.<br>
	 * If neither are valid, it returns -9999.
	 * @return The calculated time as an {@code int}.
	 */
	public int getTime()
	{
		return _time > 0 ? _time : _visualExpiration > 0 ? (int) _visualExpiration : -9999;
	}
	
	/**
	 * Checks if the item is currently available.
	 * @return {@code true} if the item is available, {@code false} otherwise.
	 */
	public boolean isAvailable()
	{
		return _available;
	}
	
	/**
	 * Retrieves the location of the item.<br>
	 * This value is stored in the {@code _location} field.
	 * @return The integer location of the item.
	 */
	public int getLocation()
	{
		return _location;
	}
	
	/**
	 * Retrieves the attack element type of the item.<br>
	 * This value is used to determine the elemental properties of an attack.
	 * @return The integer representation of the attack element type.
	 */
	public int getAttackElementType()
	{
		return _elemAtkType;
	}
	
	/**
	 * Retrieves the power of the attack element.<br>
	 * This value is used to calculate elemental damage.
	 * @return The integer value of the attack element power.
	 */
	public int getAttackElementPower()
	{
		return _elemAtkPower;
	}
	
	/**
	 * Retrieves the defense value for a specific attribute.<br>
	 * This method looks up the value based on the {@code AttributeType}.
	 * @param attribute The type of attribute to check.
	 * @return The integer defense value associated with the given attribute.
	 */
	public int getAttributeDefence(AttributeType attribute)
	{
		return _attributeDefence[attribute.getClientId()];
	}
	
	/**
	 * Retrieves the enchantment options for this item.<br>
	 * These values are used to determine how the item appears in the game.
	 * @return an {@code int[]} array containing the enchantment options.
	 */
	public int[] getEnchantOptions()
	{
		return _option;
	}
	
	/**
	 * Retrieves the visual identifier for this item.<br>
	 * This ID is used to determine which 3D model to display.
	 * @return the unique visual identifier as an {@code int}
	 */
	public int getVisualId()
	{
		return _visualId;
	}
	
	/**
	 * Retrieves the list of soul crystal options for this item.<br>
	 * Returns an empty collection if no options are set.
	 * @return A {@code Collection} of {@code EnsoulOption} objects.
	 */
	public Collection<EnsoulOption> getSoulCrystalOptions()
	{
		return _soulCrystalOptions != null ? _soulCrystalOptions : Collections.emptyList();
	}
	
	/**
	 * Checks if the item contains all required soul crystal options.<br>
	 * It compares the internal options against the provided array.
	 * @param soulCrystalOptions The array of {@code EnsoulOption} to check against.
	 * @return {@code true} if all required options are present, otherwise {@code false}.
	 */
	public boolean soulCrystalOptionsMatch(EnsoulOption[] soulCrystalOptions)
	{
		if (_soulCrystalOptions == null)
		{
			return false;
		}
		
		for (EnsoulOption soulCrystalOption1 : _soulCrystalOptions)
		{
			boolean found = false;
			SEARCH: for (EnsoulOption soulCrystalOption2 : soulCrystalOptions)
			{
				if (soulCrystalOption1.equals(soulCrystalOption2))
				{
					found = true;
					break SEARCH;
				}
			}
			
			if (!found)
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Retrieves the special options for a Soul Crystal.<br>
	 * Returns an empty list if no special options exist.
	 * @return A {@code Collection} of {@code EnsoulOption} objects.
	 */
	public Collection<EnsoulOption> getSoulCrystalSpecialOptions()
	{
		return _soulCrystalSpecialOptions != null ? _soulCrystalSpecialOptions : Collections.emptyList();
	}
	
	/**
	 * Checks if the special options of this item match a provided list.<br>
	 * It verifies that every option in the internal collection exists within the input array.
	 * @param soulCrystalSpecialOptions The array of {@code EnsoulOption} to compare against.
	 * @return {@code true} if all internal special options are found in the input, otherwise {@code false}.
	 */
	public boolean soulCrystalSpecialOptionsMatch(EnsoulOption[] soulCrystalSpecialOptions)
	{
		if (_soulCrystalSpecialOptions == null)
		{
			return false;
		}
		
		for (EnsoulOption soulCrystalSpecialOption1 : _soulCrystalSpecialOptions)
		{
			boolean found = false;
			SEARCH: for (EnsoulOption soulCrystalSpecialOption2 : soulCrystalSpecialOptions)
			{
				if (soulCrystalSpecialOption1.equals(soulCrystalSpecialOption2))
				{
					found = true;
					break SEARCH;
				}
			}
			
			if (!found)
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Retrieves the visual expiration time of the item.<br>
	 * This value is used to determine when a visual effect should end.
	 * @return The expiration time as a {@code long}.
	 */
	public long getVisualExpiration()
	{
		return _visualExpiration;
	}
	
	/**
	 * Retrieves the reuse delay of the item.<br>
	 * This value represents the time required before the item can be used again.
	 * @return The reuse delay as an {@code int}.
	 */
	public int getReuseDelay()
	{
		return _reuseDelay;
	}
	
	/**
	 * Retrieves the owner of this item.
	 * @return the {@code Player} who owns the item.
	 */
	public Player getOwner()
	{
		return _owner;
	}
	
	/**
	 * Returns a string representation of the {@code ItemInfo}.<br>
	 * This includes the item template and basic details like object ID and count.
	 * @return A string describing the current state of this {@code ItemInfo} instance.
	 */
	@Override
	public String toString()
	{
		return String.valueOf(_item) + "[objId: " + _objectId + ", count: " + _count + "]";
	}
}
