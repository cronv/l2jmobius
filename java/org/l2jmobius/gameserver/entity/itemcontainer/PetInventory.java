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
package org.l2jmobius.gameserver.entity.itemcontainer;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;

/**
 * Manages the inventory system specifically for {@code Pet} entities.<br>
 * It handles how items are stored and accessed within a pet's personal storage.
 */
public class PetInventory extends Inventory
{
	private final Pet _owner;
	
	/**
	 * Creates a new {@code PetInventory} for a specific pet.<br>
	 * This constructor links the inventory to its owner.
	 * @param owner The {@code Pet} that owns this inventory.
	 */
	public PetInventory(Pet owner)
	{
		_owner = owner;
	}
	
	/**
	 * Retrieves the owner of this inventory.<br>
	 * This method returns the {@code Pet} associated with the current inventory instance.
	 * @return the {@code Pet} owner of the inventory
	 */
	@Override
	public Pet getOwner()
	{
		return _owner;
	}
	
	/**
	 * Retrieves the unique identifier of the owner.<br>
	 * This method returns the {@code objectId} from the {@code Pet} owner.<br>
	 * It returns {@code 0} if the owner is null.
	 * @return The unique ID of the owner as an {@code int}.
	 */
	@Override
	public int getOwnerId()
	{
		// gets the Player-owner's ID
		int id;
		try
		{
			id = _owner.getOwner().getObjectId();
		}
		catch (NullPointerException e)
		{
			return 0;
		}
		
		return id;
	}
	
	/**
	 * Updates the total weight of the inventory.<br>
	 * This method also notifies the {@code Pet} owner of the change.
	 */
	@Override
	protected void refreshWeight()
	{
		super.refreshWeight();
		_owner.updateAndBroadcastStatus(1);
	}
	
	/**
	 * Checks if there is enough space in the inventory for a specific {@code Item}.<br>
	 * It calculates how many slots the item requires based on its properties.<br>
	 * This method returns {@code true} if the item can be added.
	 * @param item The {@code Item} to check against the current capacity.
	 * @return {@code true} if there is enough space, otherwise {@code false}.
	 */
	public boolean validateCapacity(Item item)
	{
		int slots = 0;
		if (!(item.isStackable() && (getItemByItemId(item.getId()) != null)) && !item.getTemplate().hasExImmediateEffect())
		{
			slots++;
		}
		
		return validateCapacity(slots);
	}
	
	/**
	 * Checks if adding a specific number of items will exceed the inventory limit.<br>
	 * This method calculates the total size after adding {@code slots}.<br>
	 * It compares the result against the owner's maximum capacity.
	 * @param slots The number of additional slots to check.
	 * @return {@code true} if the new total is within limits, {@code false} otherwise.
	 */
	@Override
	public boolean validateCapacity(long slots)
	{
		return ((_items.size() + slots) <= _owner.getInventoryLimit());
	}
	
	/**
	 * Checks if adding a specific amount of an item exceeds the weight limit.<br>
	 * This method calculates the total weight based on the {@code Item} template.<br>
	 * It returns {@code false} if the {@code ItemTemplate} is not found.
	 * @param item The {@code Item} to be checked.
	 * @param count The quantity of the item to add.
	 * @return {@code true} if the weight is valid, otherwise {@code false}.
	 */
	public boolean validateWeight(Item item, long count)
	{
		int weight = 0;
		final ItemTemplate template = ItemData.getInstance().getTemplate(item.getId());
		if (template == null)
		{
			return false;
		}
		
		weight += count * template.getWeight();
		return validateWeight(weight);
	}
	
	/**
	 * Checks if adding a specific weight will exceed the owner's limit.<br>
	 * This method compares the current total weight plus the new {@code weight} against the maximum load.
	 * @param weight The amount of weight to add.
	 * @return {@code true} if the weight is within the limit, otherwise {@code false}.
	 */
	@Override
	public boolean validateWeight(long weight)
	{
		return ((_totalWeight + weight) <= _owner.getMaxLoad());
	}
	
	/**
	 * Returns the default location for items in this inventory.<br>
	 * This method identifies where the pet's primary storage is located.
	 * @return the {@code ItemLocation} representing the pet's base storage.
	 */
	@Override
	protected ItemLocation getBaseLocation()
	{
		return ItemLocation.PET;
	}
	
	/**
	 * Returns the location where equipment is stored for this inventory.<br>
	 * This method identifies the specific slot type used for pet gear.
	 * @return the {@code ItemLocation} representing the pet equipment area.
	 */
	@Override
	protected ItemLocation getEquipLocation()
	{
		return ItemLocation.PET_EQUIP;
	}
	
	/**
	 * Restores the inventory state for this {@code PetInventory}.<br>
	 * It calls the parent restoration logic.<br>
	 * It also unequips items that do not meet the owner's requirements.
	 */
	@Override
	public void restore()
	{
		super.restore();
		
		// check for equipped items from other pets
		for (Item item : _items)
		{
			if (item.isEquipped() && !item.getTemplate().checkCondition(_owner, _owner, false))
			{
				unEquipItemInSlot(item.getLocationSlot());
			}
		}
	}
	
	/**
	 * Moves all items from this inventory to the owner's main inventory.<br>
	 * This method iterates through every {@code Item} in the current collection.<br>
	 * It uses {@code ItemProcessType.TRANSFER} to move each item to the owner's primary storage.
	 */
	public void transferItemsToOwner()
	{
		final Pet owner = getOwner();
		final Player ownerOwner = owner.getOwner();
		for (Item item : _items)
		{
			owner.transferItem(ItemProcessType.TRANSFER, item.getObjectId(), item.getCount(), ownerOwner.getInventory(), ownerOwner, owner);
		}
	}
}
