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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.TeleportListHolder;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading and parsing of teleport data from XML files.<br>
 * It populates a list of {@code TeleportListHolder} objects to define world travel points.
 * @author NviX, Mobius
 */
public class TeleportListData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(TeleportListData.class.getName());
	
	private final Map<Integer, TeleportListHolder> _teleports = new HashMap<>();
	private int _teleportCount = 0;
	
	/**
	 * Initializes a new instance of the {@code TeleportListData} class.<br>
	 * This constructor calls the {@code load()} method to populate data.
	 */
	protected TeleportListData()
	{
		load();
	}
	
	/**
	 * Loads the teleport data from the XML file.<br>
	 * This method clears existing data and populates the internal map.<br>
	 * It updates the {@code _teleportCount} based on the loaded entries.
	 */
	@Override
	public void load()
	{
		_teleports.clear();
		parseDatapackFile("data/TeleportListData.xml");
		_teleportCount = _teleports.size();
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _teleportCount + " teleports.");
	}
	
	/**
	 * Parses a {@code Document} from a specific {@code File}.<br>
	 * This method populates the teleport data from an XML structure.<br>
	 * It extracts IDs, prices, and locations to create {@code TeleportListHolder} objects.
	 * @param document The {@code Document} object containing the XML data.
	 * @param file The {@code File} source of the XML data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "teleport", teleportNode ->
		{
			// Parse attributes for the teleport node.
			final StatSet set = new StatSet(parseAttributes(teleportNode));
			final int tpId = set.getInt("id");
			final int tpPrice = set.getInt("price");
			
			// Initialize locations list.
			final List<Location> locations = new ArrayList<>();
			
			// Parse each "location" element within the "teleport" node.
			forEach(teleportNode, "location", locationsNode ->
			{
				final StatSet locationSet = new StatSet(parseAttributes(locationsNode));
				locations.add(new Location(locationSet.getInt("x"), locationSet.getInt("y"), locationSet.getInt("z")));
			});
			
			// If no locations were added, add a default location from teleport attributes.
			if (locations.isEmpty())
			{
				locations.add(new Location(set.getInt("x"), set.getInt("y"), set.getInt("z")));
			}
			
			// Add the teleport entry to _teleports.
			_teleports.put(tpId, new TeleportListHolder(tpId, locations, tpPrice));
		}));
	}
	
	/**
	 * Retrieves a specific teleport based on its unique ID.<br>
	 * This method looks up the data in the internal map.
	 * @param teleportId The unique identifier for the teleport to find.
	 * @return The {@code TeleportListHolder} associated with the ID, or {@code null} if not found.
	 */
	public TeleportListHolder getTeleport(int teleportId)
	{
		return _teleports.get(teleportId);
	}
	
	/**
	 * Retrieves the total number of teleports loaded.<br>
	 * This count is stored in the {@code _teleportCount} variable.
	 * @return The total number of teleports.
	 */
	public int getTeleportCount()
	{
		return _teleportCount;
	}
	
	/**
	 * Retrieves the singleton instance of {@code TeleportListData}.<br>
	 * This method provides a global access point to the teleport data.
	 * @return The single instance of {@code TeleportListData}.
	 */
	public static TeleportListData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final TeleportListData INSTANCE = new TeleportListData();
	}
}
