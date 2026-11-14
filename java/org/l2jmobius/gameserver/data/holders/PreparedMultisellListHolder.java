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
package org.l2jmobius.gameserver.data.holders;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.holders.ItemInfo;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.itemcontainer.ItemContainer;
import org.l2jmobius.gameserver.mechanics.siege.CastleTaxType;

/**
 * This class is a modified version of {@code MultisellListHolder}.<br>
 * It stores altered data from the original list along with dynamic data resulting from player interactions.
 * @author Nik
 */
public class PreparedMultisellListHolder extends MultisellListHolder
{
	private int _npcObjectId;
	private final boolean _inventoryOnly;
	private double _taxRate;
	private List<ItemInfo> _itemInfos;
	
	/**
	 * Creates a new {@code PreparedMultisellListHolder} with specific configuration.<br>
	 * This constructor initializes the holder and calculates dynamic data like tax rates.<br>
	 * It also filters items from the inventory if {@code inventoryOnly} is set to {@code true}.
	 * @param list The base {@code MultisellListHolder} to copy data from.
	 * @param inventoryOnly Whether to only include equippable items from the player's inventory.
	 * @param inventory The {@code ItemContainer} containing the items to check.
	 * @param npc The {@code Npc} associated with this multisell list.
	 * @param ingredientMultiplier The multiplier applied to ingredients.
	 * @param productMultiplier The multiplier applied to products.
	 */
	public PreparedMultisellListHolder(MultisellListHolder list, boolean inventoryOnly, ItemContainer inventory, Npc npc, double ingredientMultiplier, double productMultiplier)
	{
		super(list.getId(), list.isChanceMultisell(), list.isApplyTaxes(), list.isMaintainEnchantment(), list.getIngredientMultiplier(), list.getProductMultiplier(), list._entries, list._npcsAllowed);
		
		_inventoryOnly = inventoryOnly;
		if (npc != null)
		{
			_npcObjectId = npc.getObjectId();
			_taxRate = npc.getCastleTaxRate(CastleTaxType.BUY);
		}
		
		// Display items from inventory that are available for exchange.
		if (inventoryOnly)
		{
			_entries = new ArrayList<>();
			_itemInfos = new ArrayList<>();
			
			// Only do the match up on equippable items that are not currently equipped. For each appropriate item, produce a set of entries for the multisell list.
			for (Item item : inventory.getItems())
			{
				if (!item.isEquipped() && (item.isArmor() || item.isWeapon()))
				{
					final int itemId = item.getId();
					// Check ingredients of each entry to see if it's an entry we'd like to include.
					for (MultisellEntryHolder entry : list.getEntries())
					{
						for (ItemChanceHolder holder : entry.getIngredients())
						{
							if (holder.getId() == itemId)
							{
								_entries.add(entry);
								_itemInfos.add(new ItemInfo(item));
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves the enchantment information for a specific item.<br>
	 * This method looks up the data based on the provided position in the list.
	 * @param index The position of the item in the list.
	 * @return The {@code ItemInfo} object or {@code null} if no item exists at that index.
	 */
	public ItemInfo getItemEnchantment(int index)
	{
		return _itemInfos != null ? _itemInfos.get(index) : null;
	}
	
	/**
	 * Retrieves the current tax rate for this multisell list.<br>
	 * It returns {@code 0.0} if taxes are not enabled.
	 * @return The applicable tax rate as a {@code double}.
	 */
	public double getTaxRate()
	{
		return isApplyTaxes() ? _taxRate : 0;
	}
	
	/**
	 * Checks if the multisell list is restricted to inventory items.<br>
	 * Returns {@code true} if it only applies to the player's inventory.<br>
	 * Returns {@code false} otherwise.
	 * @return a boolean indicating if the list is inventory-only.
	 */
	public boolean isInventoryOnly()
	{
		return _inventoryOnly;
	}
	
	/**
	 * Checks if the provided {@code npcObjectId} matches the current NPC.<br>
	 * It returns {@code true} if the ID is {@code 0} or matches exactly.
	 * @param npcObjectId The unique identifier of the NPC to check.
	 * @return {@code true} if the IDs match or are zero, otherwise {@code false}.
	 */
	public boolean checkNpcObjectId(int npcObjectId)
	{
		return (_npcObjectId == 0) || (_npcObjectId == npcObjectId);
	}
	
	/**
	 * Calculates the total count of a specific ingredient.<br>
	 * This method applies multipliers and tax rates based on the item type.<br>
	 * It returns a rounded {@code long} value.
	 * @param ingredient The {@code ItemHolder} to check for its count.
	 * @return The calculated amount of the ingredient.
	 */
	public long getIngredientCount(ItemHolder ingredient)
	{
		return (ingredient.getId() == Inventory.ADENA_ID) ? Math.round(ingredient.getCount() * getIngredientMultiplier() * (1 + getTaxRate())) : Math.round(ingredient.getCount() * getIngredientMultiplier());
	}
	
	/**
	 * Calculates the total count of a specific product.<br>
	 * This method multiplies the base count by the product multiplier.<br>
	 * The result is rounded to the nearest long value.
	 * @param product The {@code ItemChanceHolder} representing the product to count.
	 * @return The final calculated count as a {@code long}.
	 */
	public long getProductCount(ItemChanceHolder product)
	{
		return Math.round(product.getCount() * getProductMultiplier());
	}
}
