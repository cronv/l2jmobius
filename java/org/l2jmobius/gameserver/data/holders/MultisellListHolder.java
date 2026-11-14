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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class serves as a static container for all multisell entries within a specific list.<br>
 * It provides a centralized way to manage and access these entries throughout the server.
 * @author Nik
 */
public class MultisellListHolder
{
	private final int _listId;
	private final boolean _isChanceMultisell;
	private final boolean _applyTaxes;
	private final boolean _maintainEnchantment;
	private final double _ingredientMultiplier;
	private final double _productMultiplier;
	
	protected List<MultisellEntryHolder> _entries;
	protected final Set<Integer> _npcsAllowed;
	
	/**
	 * Creates a new {@code MultisellListHolder} instance.<br>
	 * This constructor initializes all properties for a multisell list.
	 * @param listId The unique identifier for the multisell list.
	 * @param isChanceMultisell Determines if the multisell has a random chance to trigger.
	 * @param applyTaxes Determines if taxes should be applied to the transaction.
	 * @param maintainEnchantment Determines if enchantments are kept after the sale.
	 * @param ingredientMultiplier The multiplier used for ingredients.
	 * @param productMultiplier The multiplier used for products.
	 * @param entries A list of {@code MultisellEntryHolder} objects belonging to this list.
	 * @param npcsAllowed A set of NPC IDs that are allowed to use this multisell.
	 */
	public MultisellListHolder(int listId, boolean isChanceMultisell, boolean applyTaxes, boolean maintainEnchantment, double ingredientMultiplier, double productMultiplier, List<MultisellEntryHolder> entries, Set<Integer> npcsAllowed)
	{
		_listId = listId;
		_isChanceMultisell = isChanceMultisell;
		_applyTaxes = applyTaxes;
		_maintainEnchantment = maintainEnchantment;
		_ingredientMultiplier = ingredientMultiplier;
		_productMultiplier = productMultiplier;
		_entries = entries;
		_npcsAllowed = npcsAllowed;
	}
	
	/**
	 * Creates a new {@code MultisellListHolder} from a {@code StatSet}.<br>
	 * This constructor initializes all fields using the values stored in the provided set.
	 * @param set The {@code StatSet} containing the configuration data.
	 */
	@SuppressWarnings("unchecked")
	public MultisellListHolder(StatSet set)
	{
		_listId = set.getInt("listId");
		_isChanceMultisell = set.getBoolean("isChanceMultisell", false);
		_applyTaxes = set.getBoolean("applyTaxes", false);
		_maintainEnchantment = set.getBoolean("maintainEnchantment", false);
		_ingredientMultiplier = set.getDouble("ingredientMultiplier", 1.0);
		_productMultiplier = set.getDouble("productMultiplier", 1.0);
		_entries = Collections.unmodifiableList(set.getList("entries", MultisellEntryHolder.class, Collections.emptyList()));
		_npcsAllowed = set.getObject("allowNpc", Set.class);
	}
	
	/**
	 * Retrieves the list of multisell entries. <br>
	 * This method returns all items associated with this holder.
	 * @return a {@code List} of {@code MultisellEntryHolder} objects.
	 */
	public List<MultisellEntryHolder> getEntries()
	{
		return _entries;
	}
	
	/**
	 * Retrieves the unique identifier for this multisell list.<br>
	 * This value corresponds to the {@code listId} provided during construction.
	 * @return The integer ID of the multisell list.
	 */
	public int getId()
	{
		return _listId;
	}
	
	/**
	 * Checks if the multisell list uses a chance-based system.<br>
	 * Returns {@code true} if it is a chance multisell.<br>
	 * Returns {@code false} otherwise.
	 * @return The status of the chance multisell.
	 */
	public boolean isChanceMultisell()
	{
		return _isChanceMultisell;
	}
	
	/**
	 * Checks if taxes are applied to this multisell list.<br>
	 * Returns {@code true} if taxes are active.<br>
	 * Returns {@code false} otherwise.
	 * @return the tax status of the multisell list.
	 */
	public boolean isApplyTaxes()
	{
		return _applyTaxes;
	}
	
	/**
	 * Checks if the enchantment level of an item is kept during a multisell.<br>
	 * Returns {@code true} if the enchantment is maintained.<br>
	 * Returns {@code false} if the enchantment is removed.
	 * @return The status of enchantment maintenance.
	 */
	public boolean isMaintainEnchantment()
	{
		return _maintainEnchantment;
	}
	
	/**
	 * Returns the multiplier used for ingredients.<br>
	 * This value determines how many items are consumed during a multisell.
	 * @return the {@code double} ingredient multiplier.
	 */
	public double getIngredientMultiplier()
	{
		return _ingredientMultiplier;
	}
	
	/**
	 * Retrieves the multiplier used for products.<br>
	 * This value determines how much a product is scaled during a multisell operation.
	 * @return The {@code double} value of the product multiplier.
	 */
	public double getProductMultiplier()
	{
		return _productMultiplier;
	}
	
	/**
	 * Checks if a specific NPC is allowed in the multisell list.<br>
	 * This method verifies if the {@code npcId} exists within the internal set.
	 * @param npcId The unique identifier of the NPC to check.
	 * @return {@code true} if the NPC is allowed, otherwise {@code false}.
	 */
	public boolean isNpcAllowed(int npcId)
	{
		return (_npcsAllowed != null) && _npcsAllowed.contains(npcId);
	}
}
