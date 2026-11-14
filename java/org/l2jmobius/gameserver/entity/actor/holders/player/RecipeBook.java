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
package org.l2jmobius.gameserver.entity.actor.holders.player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.holders.RecipeHolder;
import org.l2jmobius.gameserver.data.xml.RecipeData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.ShortcutType;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

/**
 * Manages the recipe book and the manufacture shop of a {@code Player}.<br>
 * It handles registering recipes, the crafting limits and the private workshop list.<br>
 * This class interacts with the database to persist the recipe information.
 * @author Mobius
 */
public class RecipeBook
{
	private static final Logger LOGGER = Logger.getLogger(RecipeBook.class.getName());
	
	private static final String DELETE_CHAR_RECIPE_SHOP = "DELETE FROM character_recipeshoplist WHERE charId=?";
	private static final String INSERT_CHAR_RECIPE_SHOP = "REPLACE INTO character_recipeshoplist (`charId`, `recipeId`, `price`, `index`) VALUES (?, ?, ?, ?)";
	private static final String RESTORE_CHAR_RECIPE_SHOP = "SELECT * FROM character_recipeshoplist WHERE charId=? ORDER BY `index`";
	
	private final Player _player;
	private final Map<Integer, RecipeHolder> _dwarven = new ConcurrentSkipListMap<>();
	private final Map<Integer, RecipeHolder> _common = new ConcurrentSkipListMap<>();
	private Map<Integer, Long> _shopItems;
	
	/**
	 * Creates a new {@code RecipeBook} for a specific player.
	 * @param player The {@code Player} object associated with this recipe book.
	 */
	public RecipeBook(Player player)
	{
		_player = player;
	}
	
	/**
	 * Restores the character's recipe book data from the database.<br>
	 * It populates the internal recipe lists based on the provided flag.
	 * @param loadCommon Determines if common recipes should be loaded along with class-specific ones.
	 */
	public void restore(boolean loadCommon)
	{
		final String sql = loadCommon ? "SELECT id, type, classIndex FROM character_recipebook WHERE charId=?" : "SELECT id FROM character_recipebook WHERE charId=? AND classIndex=? AND type = 1";
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(sql))
		{
			statement.setInt(1, _player.getObjectId());
			if (!loadCommon)
			{
				statement.setInt(2, _player.getClassIndex());
			}
			
			try (ResultSet rset = statement.executeQuery())
			{
				_dwarven.clear();
				
				RecipeHolder recipe;
				final RecipeData rd = RecipeData.getInstance();
				while (rset.next())
				{
					recipe = rd.getRecipe(rset.getInt("id"));
					if (loadCommon)
					{
						if (rset.getInt(2) == 1)
						{
							if (rset.getInt(3) == _player.getClassIndex())
							{
								registerDwarven(recipe, false);
							}
						}
						else
						{
							registerCommon(recipe, false);
						}
					}
					else
					{
						registerDwarven(recipe, false);
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not restore recipe book data:" + e.getMessage(), e);
		}
	}
	
	/**
	 * Registers a new dwarven recipe into the system.<br>
	 * This method adds the recipe to the internal collection.<br>
	 * It optionally saves the new data to the database.
	 * @param recipe The {@code RecipeHolder} object containing the recipe details.
	 * @param saveToDb Set to {@code true} to persist the recipe in the database, or {@code false} to skip saving.
	 */
	public void registerDwarven(RecipeHolder recipe, boolean saveToDb)
	{
		final int recipeId = recipe.getId();
		_dwarven.put(recipeId, recipe);
		
		if (saveToDb)
		{
			insertData(recipeId, true);
		}
	}
	
	/**
	 * Registers a new recipe into the common recipe book.<br>
	 * This method adds the recipe to the internal list.<br>
	 * It optionally saves the new recipe data to the database.
	 * @param recipe The {@code RecipeHolder} object containing the recipe details.
	 * @param saveToDb Set to {@code true} to persist the recipe in the database, or {@code false} to skip saving.
	 */
	public void registerCommon(RecipeHolder recipe, boolean saveToDb)
	{
		final int recipeId = recipe.getId();
		_common.put(recipeId, recipe);
		
		if (saveToDb)
		{
			insertData(recipeId, false);
		}
	}
	
	/**
	 * Removes a specific recipe list from the player's records.<br>
	 * This method deletes the data for the given {@code recipeId}.<br>
	 * It also removes any associated shortcuts from the player's shortcut bars.
	 * @param recipeId The unique identifier of the recipe list to remove.
	 */
	public void unregister(int recipeId)
	{
		if (_dwarven.remove(recipeId) != null)
		{
			deleteData(recipeId, true);
		}
		else if (_common.remove(recipeId) != null)
		{
			deleteData(recipeId, false);
		}
		else
		{
			LOGGER.warning("Attempted to remove unknown RecipeList: " + recipeId);
		}
		
		for (Shortcut sc : _player.getAllShortcuts())
		{
			if ((sc != null) && (sc.getId() == recipeId) && (sc.getType() == ShortcutType.RECIPE))
			{
				_player.deleteShortcut(sc.getSlot(), sc.getPage());
			}
		}
	}
	
	/**
	 * Checks if a specific recipe exists in the available books.<br>
	 * It searches both the dwarven and common recipe collections.
	 * @param recipeId The unique identifier of the recipe to check.
	 * @return {@code true} if the recipe is found, {@code false} otherwise.
	 */
	public boolean has(int recipeId)
	{
		return _dwarven.containsKey(recipeId) || _common.containsKey(recipeId);
	}
	
	/**
	 * Retrieves the collection of recipes from the Dwarven recipe book.
	 * @return A {@code Collection} of {@code RecipeHolder} objects.
	 */
	public Collection<RecipeHolder> getDwarven()
	{
		return _dwarven.values();
	}
	
	/**
	 * Retrieves the collection of common recipes.<br>
	 * This method returns all entries from the internal recipe book.
	 * @return a {@code Collection} of {@code RecipeHolder} objects.
	 */
	public Collection<RecipeHolder> getCommon()
	{
		return _common.values();
	}
	
	/**
	 * Gets the total limit for dwarf recipes.<br>
	 * This value is calculated by adding the base limit from {@code PlayerConfig} to the player's specific stat.
	 * @return The total number of allowed dwarf recipes.
	 */
	public int getDwarfLimit()
	{
		int recdlim = PlayerConfig.DWARF_RECIPE_LIMIT;
		recdlim += (int) _player.getStat().getValue(Stat.RECIPE_DWARVEN, 0);
		return recdlim;
	}
	
	/**
	 * Retrieves the total limit for common recipes.<br>
	 * This value is calculated by adding the base limit from {@code PlayerConfig} to the player's specific stat.
	 * @return The total number of common recipes allowed.
	 */
	public int getCommonLimit()
	{
		int recclim = PlayerConfig.COMMON_RECIPE_LIMIT;
		recclim += (int) _player.getStat().getValue(Stat.RECIPE_COMMON, 0);
		return recclim;
	}
	
	/**
	 * Saves the current manufacture shop items to the database.<br>
	 * This method checks if the player has a manufacture shop active.<br>
	 * It deletes existing records for the character and inserts new ones.<br>
	 * Errors during the database operation are logged as severe errors.
	 */
	public void storeShop()
	{
		if (hasShop())
		{
			final int objectId = _player.getObjectId();
			try (Connection con = DatabaseFactory.getConnection())
			{
				try (PreparedStatement st = con.prepareStatement(DELETE_CHAR_RECIPE_SHOP))
				{
					st.setInt(1, objectId);
					st.execute();
				}
				
				try (PreparedStatement st = con.prepareStatement(INSERT_CHAR_RECIPE_SHOP))
				{
					final AtomicInteger slot = new AtomicInteger(1);
					for (Entry<Integer, Long> entry : _shopItems.entrySet())
					{
						st.setInt(1, objectId);
						st.setInt(2, entry.getKey());
						st.setLong(3, entry.getValue());
						st.setInt(4, slot.getAndIncrement());
						st.addBatch();
					}
					
					st.executeBatch();
					// No need to call con.commit() as HikariCP autocommit is true.
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Could not store recipe shop for playerId " + objectId + ": ", e);
			}
		}
	}
	
	/**
	 * Loads the character's recipe shop items from the database.<br>
	 * This method clears existing manufacture items and populates them with saved data.<br>
	 * It uses the {@code RESTORE_CHAR_RECIPE_SHOP} query to fetch the records.
	 */
	public void restoreShop()
	{
		final Map<Integer, Long> manufactureItems = new HashMap<>();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_RECIPE_SHOP))
		{
			statement.setInt(1, _player.getObjectId());
			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					manufactureItems.put(rset.getInt("recipeId"), rset.getLong("price"));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not restore recipe shop list data for playerId: " + _player.getObjectId(), e);
		}
		
		_shopItems = manufactureItems;
	}
	
	/**
	 * Checks if the player has any items for manufacturing.<br>
	 * It returns {@code true} if the {@code manufactureItems} list is not empty.<br>
	 * It returns {@code false} if the list is null or contains no items.
	 * @return {@code true} if there are manufacture items, {@code false} otherwise.
	 */
	public boolean hasShop()
	{
		return (_shopItems != null) && !_shopItems.isEmpty();
	}
	
	/**
	 * Retrieves the map of manufacture items.<br>
	 * Returns an empty {@code Map} if no items are registered.
	 * @return a {@code Map} where keys are item IDs and values are quantities.
	 */
	public Map<Integer, Long> getShopItems()
	{
		if (_shopItems == null)
		{
			return Collections.emptyMap();
		}
		
		return _shopItems;
	}
	
	/**
	 * Updates the list of items that can be manufactured.<br>
	 * This method sets the internal map of item IDs and their quantities.
	 * @param manufactureItems A {@code Map<Integer, Long>} containing the item data.
	 */
	public void setShopItems(Map<Integer, Long> manufactureItems)
	{
		_shopItems = manufactureItems;
	}
	
	/**
	 * Saves a new recipe to the database for the current character.<br>
	 * It uses the {@code recipeId} and checks if the character is a dwarf.<br>
	 * If {@code isDwarf} is {@code true}, it sets specific class indices.
	 * @param recipeId The unique identifier of the recipe to insert.
	 * @param isDwarf A boolean flag indicating if the character belongs to the dwarf race.
	 */
	private void insertData(int recipeId, boolean isDwarf)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO character_recipebook (charId, id, classIndex, type) values(?,?,?,?)"))
		{
			statement.setInt(1, _player.getObjectId());
			statement.setInt(2, recipeId);
			statement.setInt(3, isDwarf ? _player.getClassIndex() : 0);
			statement.setInt(4, isDwarf ? 1 : 0);
			statement.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "SQL exception while inserting recipe: " + recipeId + " from character " + _player.getObjectId(), e);
		}
	}
	
	/**
	 * Removes a specific recipe from the character's recipe book.<br>
	 * This method updates the database by deleting the entry matching the provided ID.
	 * @param recipeId The unique identifier of the recipe to remove.
	 * @param isDwarf A boolean flag used to determine the correct class index for the deletion.
	 */
	private void deleteData(int recipeId, boolean isDwarf)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM character_recipebook WHERE charId=? AND id=? AND classIndex=?"))
		{
			statement.setInt(1, _player.getObjectId());
			statement.setInt(2, recipeId);
			statement.setInt(3, isDwarf ? _player.getClassIndex() : 0);
			statement.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "SQL exception while deleting recipe: " + recipeId + " from character " + _player.getObjectId(), e);
		}
	}
}
