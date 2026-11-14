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
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.ClanShopProductHolder;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading and parsing of clan shop data from XML files.<br>
 * It populates {@code ClanShopProductHolder} objects with item templates and pricing information.
 * @author Mobius
 */
public class ClanShopData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ClanShopData.class.getName());
	
	private final List<ClanShopProductHolder> _clanShopProducts = new ArrayList<>();
	
	/**
	 * Initializes a new instance of {@code ClanShopData}.<br>
	 * This constructor calls the {@code load()} method to populate data.
	 */
	protected ClanShopData()
	{
		load();
	}
	
	/**
	 * Loads the clan shop data from the data file.<br>
	 * This method clears existing products and parses {@code data/ClanShop.xml}.<br>
	 * It logs the total number of products loaded to the logger.
	 */
	@Override
	public void load()
	{
		_clanShopProducts.clear();
		parseDatapackFile("data/ClanShop.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _clanShopProducts.size() + " clan shop products.");
	}
	
	/**
	 * This method parses a {@code Document} to populate the clan shop products.<br>
	 * It reads data from the provided {@code File} and creates new product holders.<br>
	 * Each valid item found in the XML is added to the internal list.
	 * @param document The {@code Document} object containing the XML data.
	 * @param file The {@code File} source of the XML data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "clan", productNode ->
		{
			// Parse attributes into StatSet.
			final StatSet set = new StatSet(parseAttributes(productNode));
			
			// Extract clan shop product attributes.
			final int clanLevel = set.getInt("level");
			final int itemId = set.getInt("item");
			final int count = set.getInt("count");
			final long adena = set.getLong("adena");
			final int fame = set.getInt("fame");
			
			// Retrieve the item template.
			final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
			if (item == null)
			{
				LOGGER.info(getClass().getSimpleName() + ": Could not create clan shop item " + itemId + ", it does not exist.");
			}
			else // Add the ClanShopProductHolder to the list.
			{
				_clanShopProducts.add(new ClanShopProductHolder(clanLevel, item, count, adena, fame));
			}
		}));
	}
	
	/**
	 * Retrieves a specific product from the clan shop based on its item ID.<br>
	 * This method searches through all loaded products to find a match.
	 * @param itemId The unique identifier of the item to search for.
	 * @return The {@code ClanShopProductHolder} if found, or {@code null} if no match exists.
	 */
	public ClanShopProductHolder getProduct(int itemId)
	{
		for (ClanShopProductHolder product : _clanShopProducts)
		{
			if (product.getTradeItem().getItem().getId() == itemId)
			{
				return product;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves all products available in the clan shop.<br>
	 * This method returns a list of {@code ClanShopProductHolder} objects.
	 * @return A {@code List} containing all loaded {@code ClanShopProductHolder} items.
	 */
	public List<ClanShopProductHolder> getProducts()
	{
		return _clanShopProducts;
	}
	
	/**
	 * Retrieves the singleton instance of {@code ClanShopData}.<br>
	 * This method provides a global access point to the shop data.
	 * @return The single instance of {@code ClanShopData}.
	 */
	public static ClanShopData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ClanShopData INSTANCE = new ClanShopData();
	}
}
