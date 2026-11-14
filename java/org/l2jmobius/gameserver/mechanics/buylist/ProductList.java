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
package org.l2jmobius.gameserver.mechanics.buylist;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a collection of products available in the buy list.<br>
 * This class manages the mapping between product identifiers and their corresponding data.<br>
 * It is used to handle items that can be purchased by players.
 * @author NosBit
 */
public class ProductList
{
	private final int _listId;
	private final Map<Integer, Product> _products = new LinkedHashMap<>();
	private Set<Integer> _allowedNpcs = null;
	
	/**
	 * Creates a new {@code ProductList} instance.<br>
	 * This constructor initializes the list with a specific identifier.
	 * @param listId The unique ID for this product list.
	 */
	public ProductList(int listId)
	{
		_listId = listId;
	}
	
	/**
	 * Retrieves the unique identifier for this product list.<br>
	 * This value was set during the creation of the {@code ProductList}.
	 * @return The {@code int} ID of the list.
	 */
	public int getListId()
	{
		return _listId;
	}
	
	/**
	 * Retrieves all products in the current list.<br>
	 * This method returns a {@code Collection} of {@code Product} objects.
	 * @return A collection containing all {@code Product} items.
	 */
	public Collection<Product> getProducts()
	{
		return _products.values();
	}
	
	/**
	 * Retrieves a {@code Product} based on its unique item ID.<br>
	 * This method searches the internal product map for the given {@code itemId}.<br>
	 * It returns {@code null} if no matching product is found.
	 * @param itemId The unique identifier of the item to find.
	 * @return The corresponding {@code Product} object or {@code null}.
	 */
	public Product getProductByItemId(int itemId)
	{
		return _products.get(itemId);
	}
	
	/**
	 * Adds a new {@code Product} to the internal list.<br>
	 * This method uses the item ID as the unique key.<br>
	 * If an item with the same ID exists, it will be overwritten.
	 * @param product The {@code Product} object to add to the collection.
	 */
	public void addProduct(Product product)
	{
		_products.put(product.getItemId(), product);
	}
	
	/**
	 * Adds a specific NPC to the list of allowed NPCs.<br>
	 * This method ensures that the {@code npcId} is included in the internal set.<br>
	 * If the set does not exist yet, it will be created automatically.
	 * @param npcId The unique identifier of the NPC to allow.
	 */
	public void addAllowedNpc(int npcId)
	{
		if (_allowedNpcs == null)
		{
			_allowedNpcs = new HashSet<>();
		}
		
		_allowedNpcs.add(npcId);
	}
	
	/**
	 * Checks if a specific NPC is permitted to interact with this list.<br>
	 * It returns {@code true} if the ID exists in the allowed set.<br>
	 * It returns {@code false} if the ID is missing or the set is null.
	 * @param npcId The unique identifier of the NPC to check.
	 * @return {@code true} if the NPC is allowed, otherwise {@code false}.
	 */
	public boolean isNpcAllowed(int npcId)
	{
		return (_allowedNpcs != null) && _allowedNpcs.contains(npcId);
	}
	
	/**
	 * Retrieves the set of NPC IDs that are allowed to access this list.<br>
	 * This method returns all IDs added via {@code addAllowedNpc}.
	 * @return a {@code Set<Integer>} containing the allowed NPC IDs.
	 */
	public Set<Integer> getNpcsAllowed()
	{
		return _allowedNpcs;
	}
}
