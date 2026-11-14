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
package org.l2jmobius.gameserver.data.holders;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.StatusUpdateType;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class represents a single entry within a {@code RecipeList}.<br>
 * It stores the specific item and quantity required for a crafting recipe.
 * @author Nik
 */
public class RecipeHolder
{
	/** List of materials required to craft this recipe. */
	private final List<ItemHolder> _materials;
	
	/** Group of products where a single product will randomly be selected upon crafting. */
	private final List<ItemChanceHolder> _productGroup;
	
	private final List<ItemHolder> _npcFee;
	
	/** Stats and amount required to perform the craft. */
	private final Map<StatusUpdateType, Double> _statUse;
	
	private final int _id;
	private final int _level;
	private final int _itemId;
	private final String _name;
	private final double _successRate;
	private final boolean _isCommonRecipe;
	private final double _maxOfferingBonus;
	private final long _maxOffering;
	
	/**
	 * Creates a new {@code RecipeHolder} instance using the provided data.<br>
	 * This constructor initializes all recipe properties including requirements and products.
	 * @param set The {@code StatSet} containing the raw configuration values.
	 * @param ingredients The list of required items to craft this recipe.
	 * @param productGroup The group of possible items that can result from crafting.
	 * @param npcFee The list of items required as a fee for NPC crafting.
	 * @param statUse A map defining the stats and amounts needed to perform the craft.
	 */
	public RecipeHolder(StatSet set, List<ItemHolder> ingredients, List<ItemChanceHolder> productGroup, List<ItemHolder> npcFee, Map<StatusUpdateType, Double> statUse)
	{
		_id = set.getInt("id");
		_level = set.getInt("level");
		_itemId = set.getInt("itemId");
		_name = set.getString("name");
		_successRate = set.getDouble("successRate");
		_isCommonRecipe = set.getBoolean("isCommonRecipe");
		_maxOfferingBonus = set.getDouble("maxOfferingBonus", Math.max(0, 100 - _successRate));
		_maxOffering = set.getLong("maxOffering", 0);
		_materials = Collections.unmodifiableList(ingredients);
		_productGroup = Collections.unmodifiableList(productGroup);
		_npcFee = Collections.unmodifiableList(npcFee);
		_statUse = Collections.unmodifiableMap(statUse);
	}
	
	/**
	 * Retrieves the unique identifier for this recipe.
	 * @return The {@code int} ID of the recipe.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the level required for this recipe.
	 * @return The integer value of the recipe level.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Retrieves the unique identifier for the item.<br>
	 * This value corresponds to the {@code _itemId} field.
	 * @return The integer ID of the item.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Retrieves the display name of this recipe.
	 * @return The {@code String} name of the recipe.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Retrieves the success rate for this recipe.<br>
	 * The value is returned as a {@code double}.
	 * @return The success rate of the recipe.
	 */
	public double getSuccessRate()
	{
		return _successRate;
	}
	
	/**
	 * Checks if the recipe is a Dwarven recipe.<br>
	 * It returns {@code true} if it is not a common recipe.
	 * @return {@code true} if this is a Dwarven recipe, {@code false} otherwise.
	 */
	public boolean isDwarvenRecipe()
	{
		return !_isCommonRecipe;
	}
	
	/**
	 * Retrieves the list of materials required for this recipe.
	 * @return a {@code List} of {@code ItemHolder} objects.
	 */
	public List<ItemHolder> getMaterials()
	{
		return _materials;
	}
	
	/**
	 * Retrieves the list of possible products for this recipe.<br>
	 * A single product from this group is selected randomly during crafting.
	 * @return a {@code List} of {@code ItemChanceHolder} objects.
	 */
	public List<ItemChanceHolder> getProductGroup()
	{
		return _productGroup;
	}
	
	/**
	 * Retrieves the list of items required as a fee for this recipe.<br>
	 * These items are typically consumed by an NPC during the crafting process.
	 * @return A {@code List} of {@code ItemHolder} objects representing the fees.
	 */
	public List<ItemHolder> getNpcFee()
	{
		return _npcFee;
	}
	
	/**
	 * Retrieves the statistics required to perform this craft.<br>
	 * The map contains {@code StatusUpdateType} keys and their corresponding double values.
	 * @return A {@code Map<StatusUpdateType, Double>} of the required stats.
	 */
	public Map<StatusUpdateType, Double> getStatUse()
	{
		return _statUse;
	}
	
	/**
	 * Retrieves the maximum offering bonus for this recipe.<br>
	 * This value represents the highest possible bonus a player can receive.
	 * @return The maximum offering bonus as a {@code double}.
	 */
	public double getMaxOfferingBonus()
	{
		return _maxOfferingBonus;
	}
	
	/**
	 * Retrieves the maximum offering value for this recipe.<br>
	 * This value is used to determine limits during the crafting process.
	 * @return The maximum offering as a {@code long}.
	 */
	public long getMaxOffering()
	{
		return _maxOffering;
	}
	
	/**
	 * Selects a random product from the recipe group.<br>
	 * This method uses a weighted random selection based on individual chances.<br>
	 * It returns {@code null} if no product is selected.
	 * @return The selected {@code ItemChanceHolder} or {@code null}.
	 */
	public ItemChanceHolder getRandomProduct()
	{
		double random = Rnd.get(100);
		for (ItemChanceHolder product : _productGroup)
		{
			final double chance = product.getChance();
			if (chance > random)
			{
				return product;
			}
			
			random -= chance;
		}
		
		return null;
	}
	
	/**
	 * Checks if the manufacturer has enough stats to perform the craft.<br>
	 * It compares the required values against the current stats of the {@code manufacturer}.<br>
	 * If a stat is missing, it may send a message to the {@code player} if {@code sendMessage} is {@code true}.
	 * @param player The player who will receive any error messages.
	 * @param manufacturer The player attempting to perform the craft.
	 * @param sendMessage Whether to notify the player of missing stats.
	 * @return {@code true} if all requirements are met, {@code false} otherwise.
	 */
	public boolean checkNecessaryStats(Player player, Player manufacturer, boolean sendMessage)
	{
		for (Entry<StatusUpdateType, Double> entry : _statUse.entrySet())
		{
			final StatusUpdateType stat = entry.getKey();
			final double requiredAmount = entry.getValue();
			
			// Less than or equals to because some stats bad interraction - like HP could kill the player if it is taken all.
			if (stat.getValue(manufacturer) <= requiredAmount)
			{
				if (sendMessage)
				{
					switch (stat)
					{
						case CUR_HP:
						{
							player.sendPacket(SystemMessageId.NOT_ENOUGH_HP);
							break;
						}
						case CUR_MP:
						{
							player.sendPacket(SystemMessageId.NOT_ENOUGH_MP);
							break;
						}
						default:
						{
							player.sendMessage("You need " + requiredAmount + " " + stat.toString().toLowerCase() + " to perform this craft.");
							break;
						}
					}
				}
				
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Checks if the {@code Player} has enough materials to craft this recipe.<br>
	 * If an ingredient is missing, it sends a message if {@code sendMessage} is {@code true}.
	 * @param player The {@code Player} whose inventory will be checked.
	 * @param sendMessage Whether to notify the player of missing items.
	 * @return {@code true} if all ingredients are present, {@code false} otherwise.
	 */
	public boolean checkNecessaryIngredients(Player player, boolean sendMessage)
	{
		for (ItemHolder ingredient : _materials)
		{
			final int ingredientId = ingredient.getId();
			final long ingredientCount = ingredient.getCount();
			final long count = player.getInventory().getInventoryItemCount(ingredientId, -1);
			if (count < ingredientCount)
			{
				if (sendMessage)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_NEED_S2_MORE_S1_S);
					sm.addItemName(ingredientId);
					sm.addLong(ingredientCount - count);
					player.sendPacket(sm);
				}
				
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Executes the crafting process for a recipe.<br>
	 * This method validates requirements and consumes resources from the players.<br>
	 * It then awards the resulting item based on the success status.
	 * @param player The player providing the materials.
	 * @param manufacturer The player performing the craft.
	 * @param success Whether the crafting attempt was successful.
	 * @param craftingCritical Whether a critical success occurred to award extra items.
	 * @param sendMessage Whether to send messages during the process.
	 * @return The resulting {@code ItemHolder} or {@code null} if requirements are not met.
	 */
	public ItemHolder doCraft(Player player, Player manufacturer, boolean success, boolean craftingCritical, boolean sendMessage)
	{
		if (!checkNecessaryStats(player, manufacturer, sendMessage))
		{
			return null;
		}
		
		if (!checkNecessaryIngredients(player, sendMessage))
		{
			return null;
		}
		
		// Take necessary stats.
		for (Entry<StatusUpdateType, Double> entry : _statUse.entrySet())
		{
			final StatusUpdateType stat = entry.getKey();
			final double requiredAmount = entry.getValue();
			
			switch (stat)
			{
				case CUR_HP:
				{
					manufacturer.reduceCurrentHp(requiredAmount, manufacturer, null);
					break;
				}
				case CUR_MP:
				{
					manufacturer.reduceCurrentMp(requiredAmount);
					break;
				}
				case CUR_CP:
				{
					manufacturer.getStatus().reduceCp((int) requiredAmount);
					break;
				}
				case EXP:
				{
					manufacturer.getStat().removeExp((long) requiredAmount);
					break;
				}
				case REPUTATION:
				{
					manufacturer.setReputation((int) (manufacturer.getReputation() - requiredAmount));
					break;
				}
			}
		}
		
		// Take necessary ingredients. If there was problem destroying item, return null to indicate that process didn't go well.
		for (ItemHolder material : _materials)
		{
			if (!player.destroyItemByItemId(ItemProcessType.CRAFT, material.getId(), material.getCount(), manufacturer, sendMessage))
			{
				return null;
			}
		}
		
		// Check if success. Luck triggers no matter the success rate - even with 100% craft. Luck chance is taken from your stat and not manufacturer's stat.
		final ItemHolder result = getRandomProduct();
		if (success)
		{
			player.addItem(ItemProcessType.CRAFT, result, manufacturer, true);
			
			// Award another item if it is crafting critical. Double blessed items is very, very rare, but still possible.
			if (craftingCritical)
			{
				player.addItem(ItemProcessType.CRAFT, result, manufacturer, true);
			}
		}
		
		return result;
	}
}
