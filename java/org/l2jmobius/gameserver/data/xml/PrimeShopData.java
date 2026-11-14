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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.primeshop.PrimeShopGroup;
import org.l2jmobius.gameserver.mechanics.primeshop.PrimeShopItem;
import org.l2jmobius.gameserver.network.serverpackets.primeshop.ExBRProductInfo;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading and parsing of {@code PrimeShop} data from XML files.<br>
 * It maps configuration entries to {@code PrimeShopItem} objects for use in the game server.
 * @author Gnacik, UnAfraid, BazookaRpm
 */
public class PrimeShopData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(PrimeShopData.class.getName());
	
	private final Map<Integer, PrimeShopGroup> _primeItems = new LinkedHashMap<>();
	
	/**
	 * Initializes a new instance of the {@code PrimeShopData} class.<br>
	 * This constructor automatically calls the {@code load()} method to populate data.
	 */
	protected PrimeShopData()
	{
		load();
	}
	
	/**
	 * Loads the prime shop data from the configuration file.<br>
	 * This method clears existing items and parses {@code data/PrimeShop.xml}.<br>
	 * It logs a message indicating how many items were loaded or if the system is disabled.
	 */
	@Override
	public void load()
	{
		_primeItems.clear();
		parseDatapackFile("data/PrimeShop.xml");
		
		if (!_primeItems.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _primeItems.size() + " items.");
		}
		else
		{
			LOGGER.info(getClass().getSimpleName() + ": System is disabled.");
		}
	}
	
	/**
	 * Parses the provided {@code Document} to load shop data.<br>
	 * This method reads XML nodes and populates the internal item map.<br>
	 * It checks for enabled lists and processes each item entry.
	 * @param document The {@code Document} object containing the XML data.
	 * @param file The {@code File} associated with the parsed document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				final Node attribute = n.getAttributes().getNamedItem("enabled");
				if ((attribute != null) && Boolean.parseBoolean(attribute.getNodeValue()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("item".equalsIgnoreCase(d.getNodeName()))
						{
							final StatSet set = new StatSet(parseAttributes(d));
							final Integer productId = set.getInt("id");
							final List<PrimeShopItem> items = new ArrayList<>();
							boolean valid = true;
							for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
							{
								if ("item".equalsIgnoreCase(b.getNodeName()))
								{
									final NamedNodeMap attrs = b.getAttributes();
									final int itemId = parseInteger(attrs, "itemId");
									final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
									if (item == null)
									{
										LOGGER.severe(getClass().getSimpleName() + ": Item template null for itemId: " + itemId + " brId: " + productId);
										valid = false;
										break;
									}
									
									final int count = parseInteger(attrs, "count");
									items.add(new PrimeShopItem(itemId, count, item.getWeight(), item.isTradeable() ? 1 : 0));
								}
							}
							
							if (!valid)
							{
								continue;
							}
							
							_primeItems.put(productId, new PrimeShopGroup(set, items));
						}
					}
				}
			}
		}
	}
	
	/**
	 * Displays the information for a specific product to a player.<br>
	 * This method sends an {@code ExBRProductInfo} packet to the user.<br>
	 * It checks if both the {@code Player} and the item exist before sending.
	 * @param player The {@code Player} who will receive the information.
	 * @param productId The unique ID of the product to display.
	 */
	public void showProductInfo(Player player, int productId)
	{
		final PrimeShopGroup item = _primeItems.get(productId);
		if ((player == null) || (item == null))
		{
			return;
		}
		
		player.sendPacket(new ExBRProductInfo(item, player));
	}
	
	/**
	 * Retrieves a {@code PrimeShopGroup} based on the provided ID.<br>
	 * This method looks up the item in the internal data map.<br>
	 * It returns {@code null} if no matching ID is found.
	 * @param productId The unique identifier of the product to retrieve.
	 * @return The {@code PrimeShopGroup} associated with the ID, or {@code null}.
	 */
	public PrimeShopGroup getItem(int productId)
	{
		return _primeItems.get(productId);
	}
	
	/**
	 * Retrieves all items available in the prime shop.<br>
	 * This method returns a map of product IDs to their corresponding groups.
	 * @return A {@code Map} containing all {@code PrimeShopGroup} objects.
	 */
	public Map<Integer, PrimeShopGroup> getPrimeItems()
	{
		return _primeItems;
	}
	
	/**
	 * Provides access to the singleton instance of {@code PrimeShopData}.<br>
	 * This method ensures that only one instance of the data is used throughout the application.
	 * @return The global instance of {@code PrimeShopData}.
	 */
	public static PrimeShopData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PrimeShopData INSTANCE = new PrimeShopData();
	}
}
