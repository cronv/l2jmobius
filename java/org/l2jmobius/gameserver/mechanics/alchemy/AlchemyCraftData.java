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
package org.l2jmobius.gameserver.mechanics.alchemy;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class stores the configuration data for alchemy crafting recipes.<br>
 * It defines the required items and materials needed to create specific products.<br>
 * Use this class to manage how {@code AlchemyCraft} mechanics behave in the game.
 * @author Sdw
 */
public class AlchemyCraftData
{
	private final int _id;
	private final int _level;
	private final int _grade;
	private final int _category;
	private final float _chance;
	private Set<ItemHolder> _ingredients;
	private ItemHolder _productionSuccess;
	private ItemHolder _productionFailure;
	
	/**
	 * Creates a new {@code AlchemyCraftData} instance.<br>
	 * This constructor initializes the craft data using values from a {@code StatSet}.
	 * @param set The {@code StatSet} containing the required crafting properties.
	 */
	public AlchemyCraftData(StatSet set)
	{
		_id = set.getInt("id");
		_level = set.getInt("level");
		_grade = set.getInt("grade");
		_category = set.getInt("category");
		_chance = set.getFloat("chance", 0);
	}
	
	/**
	 * Retrieves the unique identifier for this alchemy craft.<br>
	 * This value is used to identify the specific recipe.
	 * @return The {@code int} ID of the alchemy craft.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the level of the alchemy craft.<br>
	 * This value represents the difficulty or tier of the recipe.
	 * @return The current level as an {@code int}.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Retrieves the grade of the alchemy craft.<br>
	 * This value represents the quality level of the production.
	 * @return The current {@code int} grade.
	 */
	public int getGrade()
	{
		return _grade;
	}
	
	/**
	 * Retrieves the success chance for this alchemy craft.<br>
	 * The value is represented as a {@code float}.
	 * @return The success chance of the craft.
	 */
	public float getChance()
	{
		return _chance;
	}
	
	/**
	 * Retrieves the category of the alchemy craft.<br>
	 * This value identifies which group the craft belongs to.
	 * @return The {@code int} category identifier.
	 */
	public int getCategory()
	{
		return _category;
	}
	
	/**
	 * Adds a new item to the list of required ingredients.<br>
	 * This method initializes the ingredient set if it is currently {@code null}.
	 * @param ingredient The {@code ItemHolder} object to be added.
	 */
	public void addIngredient(ItemHolder ingredient)
	{
		if (_ingredients == null)
		{
			_ingredients = new HashSet<>();
		}
		
		_ingredients.add(ingredient);
	}
	
	/**
	 * Retrieves the list of items required for this alchemy craft.<br>
	 * Returns an empty {@code Set} if no ingredients are defined.
	 * @return a {@code Set} of {@code ItemHolder} objects representing the ingredients.
	 */
	public Set<ItemHolder> getIngredients()
	{
		return _ingredients != null ? _ingredients : Collections.emptySet();
	}
	
	/**
	 * Sets the item produced when an alchemy craft is successful.<br>
	 * This updates the {@code _productionSuccess} field with the provided {@code ItemHolder}.
	 * @param item The {@code ItemHolder} to be awarded on success.
	 */
	public void setProductionSuccess(ItemHolder item)
	{
		_productionSuccess = item;
	}
	
	/**
	 * Retrieves the item produced upon a successful alchemy craft.<br>
	 * This method returns the {@code ItemHolder} associated with success.
	 * @return the {@code ItemHolder} of the successful production result.
	 */
	public ItemHolder getProductionSuccess()
	{
		return _productionSuccess;
	}
	
	/**
	 * Sets the item that is produced when an alchemy craft fails.<br>
	 * This updates the {@code _productionFailure} field with the provided {@code ItemHolder}.
	 * @param item The {@code ItemHolder} to be assigned as the failure result.
	 */
	public void setProductionFailure(ItemHolder item)
	{
		_productionFailure = item;
	}
	
	/**
	 * Retrieves the item received when an alchemy craft fails.<br>
	 * This method returns the {@code ItemHolder} associated with a failed production.
	 * @return the {@code ItemHolder} representing the failure result.
	 */
	public ItemHolder getProductionFailure()
	{
		return _productionFailure;
	}
}
