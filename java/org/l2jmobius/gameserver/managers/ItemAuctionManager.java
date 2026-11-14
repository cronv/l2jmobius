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
package org.l2jmobius.gameserver.managers;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.item.auction.ItemAuctionInstance;

/**
 * Manages the lifecycle and operations of item auctions within the game world.<br>
 * This class handles auction data, including creation, listing, and retrieval from the database.
 * @author Forsaiken
 */
public class ItemAuctionManager implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ItemAuctionManager.class.getName());
	
	private final Map<Integer, ItemAuctionInstance> _managerInstances = new HashMap<>();
	private final AtomicInteger _auctionIds = new AtomicInteger(1);
	
	/**
	 * Initializes the {@code ItemAuctionManager}.<br>
	 * This constructor checks if item auctions are enabled in the configuration.<br>
	 * It retrieves the last auction ID from the database to set the starting point for new IDs.<br>
	 * Finally, it calls the {@code load()} method to prepare the manager.
	 */
	protected ItemAuctionManager()
	{
		if (!GeneralConfig.ALT_ITEM_AUCTION_ENABLED)
		{
			LOGGER.info(getClass().getSimpleName() + ": Disabled.");
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			Statement statement = con.createStatement();
			ResultSet rset = statement.executeQuery("SELECT auctionId FROM item_auction ORDER BY auctionId DESC LIMIT 0, 1"))
		{
			if (rset.next())
			{
				_auctionIds.set(rset.getInt(1) + 1);
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Failed loading auctions.", e);
		}
		
		load();
	}
	
	/**
	 * This method initializes the auction data.<br>
	 * It clears existing instances from the manager.<br>
	 * It loads new data from the {@code data/ItemAuctions.xml} file.
	 */
	@Override
	public void load()
	{
		_managerInstances.clear();
		parseDatapackFile("data/ItemAuctions.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _managerInstances.size() + " instances.");
	}
	
	/**
	 * Parses the provided {@code Document} to load auction instances.<br>
	 * It reads data from the XML file and stores it in the manager.<br>
	 * If a duplicate ID is found, an error will be logged.
	 * @param document The {@code Document} object containing the XML data.
	 * @param file The {@code File} source of the configuration.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		try
		{
			for (Node na = document.getFirstChild(); na != null; na = na.getNextSibling())
			{
				if ("list".equalsIgnoreCase(na.getNodeName()))
				{
					for (Node nb = na.getFirstChild(); nb != null; nb = nb.getNextSibling())
					{
						if ("instance".equalsIgnoreCase(nb.getNodeName()))
						{
							final NamedNodeMap nab = nb.getAttributes();
							final int instanceId = Integer.parseInt(nab.getNamedItem("id").getNodeValue());
							if (_managerInstances.containsKey(instanceId))
							{
								throw new Exception("Dublicated instanceId " + instanceId);
							}
							
							final ItemAuctionInstance instance = new ItemAuctionInstance(instanceId, _auctionIds, nb);
							_managerInstances.put(instanceId, instance);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Failed loading auctions from xml.", e);
		}
	}
	
	/**
	 * Shuts down all active auction instances.<br>
	 * This method iterates through the {@code _managerInstances} map.<br>
	 * It calls the {@code shutdown()} method on every {@code ItemAuctionInstance}.
	 */
	public void shutdown()
	{
		for (ItemAuctionInstance instance : _managerInstances.values())
		{
			instance.shutdown();
		}
	}
	
	/**
	 * Retrieves a specific {@code ItemAuctionInstance} based on its unique ID.<br>
	 * This method looks up the instance in the internal manager map.
	 * @param instanceId The unique identifier of the auction instance to retrieve.
	 * @return The {@code ItemAuctionInstance} associated with the provided ID, or {@code null} if not found.
	 */
	public ItemAuctionInstance getManagerInstance(int instanceId)
	{
		return _managerInstances.get(instanceId);
	}
	
	/**
	 * Retrieves the next unique identifier for an auction.<br>
	 * This method increments the internal counter every time it is called.
	 * @return The next available {@code int} ID for a new auction.
	 */
	public int getNextAuctionId()
	{
		return _auctionIds.getAndIncrement();
	}
	
	/**
	 * Removes an auction from the database.<br>
	 * This method deletes all records associated with a specific {@code auctionId}.<br>
	 * It removes data from both the {@code item_auction} and {@code item_auction_bid} tables.
	 * @param auctionId The unique identifier of the auction to delete.
	 */
	public static void deleteAuction(int auctionId)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			try (PreparedStatement statement = con.prepareStatement("DELETE FROM item_auction WHERE auctionId=?"))
			{
				statement.setInt(1, auctionId);
				statement.execute();
			}
			
			try (PreparedStatement statement = con.prepareStatement("DELETE FROM item_auction_bid WHERE auctionId=?"))
			{
				statement.setInt(1, auctionId);
				statement.execute();
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "ItemAuctionManagerInstance: Failed deleting auction: " + auctionId, e);
		}
	}
	
	/**
	 * Retrieves the singleton instance of {@code ItemAuctionManager}.<br>
	 * This method provides a global access point to the manager.
	 * @return The active {@code ItemAuctionManager} instance.
	 */
	public static ItemAuctionManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ItemAuctionManager INSTANCE = new ItemAuctionManager();
	}
}
