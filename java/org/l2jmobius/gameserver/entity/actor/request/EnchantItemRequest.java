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
package org.l2jmobius.gameserver.entity.actor.request;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;

/**
 * Handles the request sent by a player to enchant an item.<br>
 * It validates the action and initiates the enchantment process for the specified {@code Item}.
 * @author UnAfraid
 */
public class EnchantItemRequest extends AbstractRequest
{
	private volatile int _enchantingItemObjectId;
	private volatile int _enchantingScrollObjectId;
	private volatile int _supportItemObjectId;
	private volatile int _enchantingItemCurrentEnchantLevel;
	private final Map<Integer, Integer> _multiEnchantingItems = new ConcurrentHashMap<>();
	private final Map<Integer, ItemHolder> _multiFailRewardItems = new ConcurrentHashMap<>();
	private final Map<Integer, int[]> _successEnchant = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> _failureEnchant = new ConcurrentHashMap<>();
	
	/**
	 * Creates a new {@code EnchantItemRequest} for a specific player.<br>
	 * This constructor initializes the request with the required scroll ID.
	 * @param player The {@code Player} who is performing the action.
	 * @param enchantingScrollObjectId The unique identifier for the enchanting scroll.
	 */
	public EnchantItemRequest(Player player, int enchantingScrollObjectId)
	{
		super(player);
		_enchantingScrollObjectId = enchantingScrollObjectId;
	}
	
	/**
	 * Sets the list of successful enchantments for multiple items.<br>
	 * This method updates the internal success map with all entries from the provided collection.
	 * @param list A {@code Map<Integer, int[]>} containing the enchantment data.
	 */
	public void setMultiSuccessEnchantList(Map<Integer, int[]> list)
	{
		_successEnchant.putAll(list);
	}
	
	/**
	 * Sets the list of enchant levels for multiple failures.<br>
	 * This method updates the internal failure map with the provided data.
	 * @param list A {@code Map<Integer, Integer>} containing the enchantment levels.
	 */
	public void setMultiFailureEnchantList(Map<Integer, Integer> list)
	{
		_failureEnchant.putAll(list);
	}
	
	/**
	 * Clears all entries from the multi-success enchantment list.<br>
	 * This method empties the {@code _successEnchant} map.
	 */
	public void clearMultiSuccessEnchantList()
	{
		_successEnchant.clear();
	}
	
	/**
	 * Clears all entries from the multi-failure enchantment list.<br>
	 * This method empties the {@code _failureEnchant} map.
	 */
	public void clearMultiFailureEnchantList()
	{
		_failureEnchant.clear();
	}
	
	/**
	 * Retrieves the list of successful enchantments.<br>
	 * This map contains enchantment data for multiple items.
	 * @return a {@code Map<Integer, int[]>} containing the success enchantations.
	 */
	public Map<Integer, int[]> getMultiSuccessEnchantList()
	{
		return _successEnchant;
	}
	
	/**
	 * Retrieves the list of enchant levels for multiple failures.<br>
	 * This map links item IDs to their corresponding failure enchantment values.
	 * @return a {@code Map<Integer, Integer>} containing the failure enchant data.
	 */
	public Map<Integer, Integer> getMultiFailureEnchantList()
	{
		return _failureEnchant;
	}
	
	/**
	 * Retrieves the item being enchanted.<br>
	 * This method looks up the {@code Item} using the stored object ID.
	 * @return the {@code Item} instance or {@code null} if not found.
	 */
	public Item getEnchantingItem()
	{
		return getPlayer().getInventory().getItemByObjectId(_enchantingItemObjectId);
	}
	
	/**
	 * Retrieves the unique identifier for the enchanting scroll.<br>
	 * This value is used to identify which scroll was selected for the request.
	 * @return The {@code int} object ID of the enchanting scroll.
	 */
	public int getEnchantingScrollObjectId()
	{
		return _enchantingScrollObjectId;
	}
	
	/**
	 * Sets the unique identifier for the item being enchanted.<br>
	 * This value is used to identify which {@code Item} instance is targeted.
	 * @param objectId The unique ID of the enchanting item.
	 */
	public void setEnchantingItem(int objectId)
	{
		_enchantingItemObjectId = objectId;
	}
	
	/**
	 * Retrieves the enchanting scroll used in this request.<br>
	 * It looks up the item from the player's inventory.
	 * @return the {@code Item} object for the enchanting scroll, or {@code null} if not found.
	 */
	public Item getEnchantingScroll()
	{
		return getPlayer().getInventory().getItemByObjectId(_enchantingScrollObjectId);
	}
	
	/**
	 * Sets the unique identifier for the enchanting scroll.<br>
	 * This value is used to determine which scroll is being applied.
	 * @param objectId The {@code int} ID of the enchanting scroll.
	 */
	public void setEnchantingScroll(int objectId)
	{
		_enchantingScrollObjectId = objectId;
	}
	
	/**
	 * Retrieves the support item used for this enchantment.<br>
	 * This method looks up the {@code Item} based on the stored object ID.
	 * @return The {@code Item} associated with the support slot, or {@code null} if not found.
	 */
	public Item getSupportItem()
	{
		return getPlayer().getInventory().getItemByObjectId(_supportItemObjectId);
	}
	
	/**
	 * Sets the unique identifier for the support item.<br>
	 * This value is used to identify which item provides extra effects during enchanting.
	 * @param objectId The {@code int} ID of the support item.
	 */
	public void setSupportItem(int objectId)
	{
		_supportItemObjectId = objectId;
	}
	
	/**
	 * Sets the current enchantment level for the item.<br>
	 * This updates the {@code _enchantingItemCurrentEnchantLevel} field.
	 * @param enchantLevel The new level to assign to the item.
	 */
	public void setEnchantLevel(int enchantLevel)
	{
		_enchantingItemCurrentEnchantLevel = enchantLevel;
	}
	
	/**
	 * Retrieves the current enchantment level of the item.<br>
	 * This value is used to determine the success rate of an enchantment.
	 * @return The current enchantment level as an {@code int}.
	 */
	public int getEnchantLevel()
	{
		return _enchantingItemCurrentEnchantLevel;
	}
	
	/**
	 * Adds a multi-enchanting item to the request.<br>
	 * It maps a specific slot to an {@code objectId}.
	 * @param slot The inventory slot index.
	 * @param objectId The unique identifier of the item.
	 */
	public void addMultiEnchantingItems(int slot, int objectId)
	{
		_multiEnchantingItems.put(slot, objectId);
	}
	
	/**
	 * Retrieves the item ID for a multi-enchanting item at a specific slot.<br>
	 * It returns the value associated with the {@code slot} key in the internal map.<br>
	 * If no item exists for that slot, it returns -1.
	 * @param slot The index of the inventory slot to check.
	 * @return The object ID of the item or -1 if not found.
	 */
	public int getMultiEnchantingItemsBySlot(int slot)
	{
		return _multiEnchantingItems.getOrDefault(slot, -1);
	}
	
	/**
	 * Updates the {@code objectId} for a specific slot in the multi-enchanting items map.<br>
	 * This method replaces any existing value at the given {@code slot}.
	 * @param slot The inventory slot index to update.
	 * @param objectId The new item {@code objectId} to assign to the slot.
	 */
	public void changeMultiEnchantingItemsBySlot(int slot, int objectId)
	{
		_multiEnchantingItems.replace(slot, objectId);
	}
	
	/**
	 * Checks if a specific item is currently part of the multi-enchanting process.<br>
	 * It looks for the {@code objectId} within the internal collection of items.
	 * @param objectId The unique identifier of the item to check.
	 * @return {@code true} if the item exists in the list, otherwise {@code false}.
	 */
	public boolean checkMultiEnchantingItemsByObjectId(int objectId)
	{
		return _multiEnchantingItems.containsValue(objectId);
	}
	
	/**
	 * Gets the total number of items currently in the multi-enchanting list.<br>
	 * This method returns the size of the {@code _multiEnchantingItems} map.
	 * @return The count of multi-enchanting items.
	 */
	public int getMultiEnchantingItemsCount()
	{
		return _multiEnchantingItems.size();
	}
	
	/**
	 * Clears all items from the multi-enchanting collection.<br>
	 * This method empties the {@code _multiEnchantingItems} map.<br>
	 * It resets the data for all slots.
	 */
	public void clearMultiEnchantingItemsBySlot()
	{
		_multiEnchantingItems.clear();
	}
	
	/**
	 * Returns a string representation of the multi-enchanting items map.<br>
	 * This method converts the internal {@code Map} to its default {@code String} format.
	 * @return A {@code String} containing the contents of the multi-enchanting items map.
	 */
	public String getMultiEnchantingItemsLits()
	{
		return _multiEnchantingItems.toString();
	}
	
	/**
	 * Adds an {@code ItemHolder} to the list of multi-enchant failure rewards.<br>
	 * This method stores the reward based on the current count of failed items.
	 * @param itemHolder The {@code ItemHolder} object to be added as a reward.
	 */
	public void addMultiEnchantFailItems(ItemHolder itemHolder)
	{
		_multiFailRewardItems.put(getMultiFailItemsCount() + 1, itemHolder);
	}
	
	/**
	 * Returns the total number of items that failed multiple enchantments.<br>
	 * This count is based on the size of the {@code _multiFailRewardItems} map.
	 * @return The number of multi-fail reward items.
	 */
	public int getMultiFailItemsCount()
	{
		return _multiFailRewardItems.size();
	}
	
	/**
	 * Clears all items from the multi-failure reward map.<br>
	 * This resets the rewards for failed enchanting attempts.
	 */
	public void clearMultiFailReward()
	{
		_multiFailRewardItems.clear();
	}
	
	/**
	 * Retrieves the items received as rewards from failed multi-enchant attempts.<br>
	 * The map uses slot indices as keys.
	 * @return A {@code Map} containing the slot index and its corresponding {@code ItemHolder}.
	 */
	public Map<Integer, ItemHolder> getMultiEnchantFailItems()
	{
		return _multiFailRewardItems;
	}
	
	/**
	 * Checks if this request involves an item.<br>
	 * This method returns {@code true} for all {@code EnchantItemRequest} instances.
	 * @return {@code true} if the request is related to an item, otherwise {@code false}.
	 */
	@Override
	public boolean isItemRequest()
	{
		return true;
	}
	
	/**
	 * Checks if this request can be processed alongside another request.<br>
	 * It returns {@code true} if the provided request is not an item request.
	 * @param request The {@code AbstractRequest} to check.
	 * @return {@code true} if the request is compatible, otherwise {@code false}.
	 */
	@Override
	public boolean canWorkWith(AbstractRequest request)
	{
		return !request.isItemRequest();
	}
	
	/**
	 * Checks if the provided object ID belongs to an item involved in this request.<br>
	 * It returns {@code true} if the ID matches the enchanting item, scroll, or support item.
	 * @param objectId The unique identifier of the object to check.
	 * @return {@code true} if the ID is valid and matches any requested item; {@code false} otherwise.
	 */
	@Override
	public boolean isUsing(int objectId)
	{
		return (objectId > 0) && ((objectId == _enchantingItemObjectId) || (objectId == _enchantingScrollObjectId) || (objectId == _supportItemObjectId));
	}
}
