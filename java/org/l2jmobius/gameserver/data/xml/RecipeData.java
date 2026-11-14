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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.RecipeHolder;
import org.l2jmobius.gameserver.entity.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.enums.StatusUpdateType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading and parsing of recipe data from XML files.<br>
 * It maps raw configuration data into {@code RecipeHolder} objects for use in the game server.
 * @author Nik, BazookaRpm
 */
public class RecipeData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(RecipeData.class.getName());
	
	private final Map<Integer, RecipeHolder> _recipes = new ConcurrentHashMap<>();
	
	/**
	 * Creates a new instance of the {@code RecipeData} class.<br>
	 * This constructor automatically calls the {@code load()} method to initialize data.
	 */
	protected RecipeData()
	{
		load();
	}
	
	/**
	 * Loads the recipe data from the XML file.<br>
	 * This method clears existing data and parses {@code data/Recipes.xml}.<br>
	 * It logs the total number of loaded recipes to the console.
	 */
	@Override
	public void load()
	{
		_recipes.clear();
		parseDatapackFile("data/Recipes.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _recipes.size() + " recipes.");
	}
	
	/**
	 * Parses the provided {@code Document} to populate the internal recipe data.<br>
	 * This method reads XML nodes to create and store {@code RecipeHolder} objects.<br>
	 * It handles ingredients, production stats, and success rates from the file.
	 * @param document The {@code Document} object containing the XML content.
	 * @param file The {@code File} source of the parsed data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("recipe".equalsIgnoreCase(d.getNodeName()))
					{
						final StatSet set = new StatSet(parseAttributes(d));
						final int recipeId = set.getInt("id");
						List<ItemHolder> materials = Collections.emptyList();
						List<ItemChanceHolder> productGroup = Collections.emptyList();
						List<ItemHolder> npcFee = Collections.emptyList();
						final Map<StatusUpdateType, Double> statUse = new HashMap<>();
						
						for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
						{
							final String cName = c.getNodeName();
							if ("materials".equalsIgnoreCase(cName))
							{
								materials = getItemList(c);
							}
							else if ("product".equalsIgnoreCase(cName))
							{
								final List<ItemHolder> products = getItemList(c);
								productGroup = new ArrayList<>(products.size());
								for (ItemHolder product : products)
								{
									productGroup.add((ItemChanceHolder) product);
								}
							}
							else if ("npcFee".equalsIgnoreCase(cName))
							{
								npcFee = getItemList(c);
							}
							else if ("statUse".equalsIgnoreCase(cName))
							{
								for (Node b = c.getFirstChild(); b != null; b = b.getNextSibling())
								{
									if ("stat".equalsIgnoreCase(b.getNodeName()))
									{
										final NamedNodeMap bAttrs = b.getAttributes();
										final StatusUpdateType stat = StatusUpdateType.valueOf(bAttrs.getNamedItem("name").getNodeValue());
										final double value = Double.parseDouble(bAttrs.getNamedItem("val").getNodeValue());
										statUse.put(stat, value);
									}
								}
							}
						}
						
						_recipes.put(recipeId, new RecipeHolder(set, materials, productGroup, npcFee, statUse));
					}
				}
			}
		}
	}
	
	/**
	 * This method parses a list of items from an XML node.<br>
	 * It iterates through all child nodes to find elements named "item".<br>
	 * Each item is converted into either an {@code ItemHolder} or an {@code ItemChanceHolder}.
	 * @param c The {@code Node} containing the item definitions.
	 * @return A {@code List} of populated {@code ItemHolder} objects.
	 */
	private List<ItemHolder> getItemList(Node c)
	{
		final List<ItemHolder> items = new ArrayList<>();
		for (Node b = c.getFirstChild(); b != null; b = b.getNextSibling())
		{
			if ("item".equalsIgnoreCase(b.getNodeName()))
			{
				final NamedNodeMap bAttrs = b.getAttributes();
				final int itemId = Integer.parseInt(bAttrs.getNamedItem("id").getNodeValue());
				final long itemCount = Long.parseLong(bAttrs.getNamedItem("count").getNodeValue());
				
				if (bAttrs.getNamedItem("chance") != null)
				{
					final double chance = Double.parseDouble(bAttrs.getNamedItem("chance").getNodeValue());
					items.add(new ItemChanceHolder(itemId, chance, itemCount));
				}
				else
				{
					items.add(new ItemHolder(itemId, itemCount));
				}
			}
		}
		
		return items;
	}
	
	/**
	 * Retrieves a {@code RecipeHolder} based on the provided item ID.<br>
	 * This method searches through all loaded recipes to find a match.<br>
	 * It returns {@code null} if no recipe is found for that ID.
	 * @param itemId The unique identifier of the item to search for.
	 * @return The matching {@code RecipeHolder} or {@code null}.
	 */
	public RecipeHolder getRecipeByRecipeItemId(int itemId)
	{
		for (RecipeHolder recipe : _recipes.values())
		{
			if (recipe.getItemId() == itemId)
			{
				return recipe;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves a specific recipe based on its unique identifier.<br>
	 * This method looks up the data in the internal map.
	 * @param recipeId The unique ID of the recipe to find.
	 * @return The {@code RecipeHolder} associated with the given ID, or {@code null} if not found.
	 */
	public RecipeHolder getRecipe(int recipeId)
	{
		return _recipes.get(recipeId);
	}
	
	/**
	 * Provides access to the singleton instance of {@code RecipeData}.<br>
	 * Use this method to get the global data container for recipes.
	 * @return The single shared instance of {@code RecipeData}.
	 */
	public static RecipeData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RecipeData INSTANCE = new RecipeData();
	}
}
