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

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportType;
import org.l2jmobius.gameserver.entity.teleporter.TeleportHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading and storage of teleporter data from XML files.<br>
 * It maps specific locations to their corresponding {@code TeleportHolder} entities.<br>
 * The catalog is published only once the whole directory parses, so a failed reload keeps the active teleporters.
 * @author BazookaRpm
 */
public class TeleporterData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(TeleporterData.class.getName());
	
	private volatile Map<Integer, Map<String, TeleportHolder>> _teleporters = Collections.emptyMap();
	private volatile Map<Integer, Map<String, TeleportHolder>> _loadingTeleporters;
	
	/**
	 * Initializes a new instance of the {@code TeleporterData} class.<br>
	 * This constructor automatically calls the {@code load()} method to populate data.
	 */
	protected TeleporterData()
	{
		load();
	}
	
	/**
	 * Loads all teleporter data from the configuration files.<br>
	 * The active catalog is replaced only once the whole directory parses, so a failed reload keeps the teleporters already in use.
	 */
	@Override
	public synchronized void load()
	{
		final Map<Integer, Map<String, TeleportHolder>> loadedTeleporters = new ConcurrentHashMap<>();
		_loadingTeleporters = loadedTeleporters;
		try
		{
			parseDatapackDirectory("data/teleporters", true);
			if (loadedTeleporters.isEmpty())
			{
				LOGGER.warning(getClass().getSimpleName() + ": No teleporters were loaded. Keeping previous data.");
				return;
			}
			
			_teleporters = loadedTeleporters;
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + loadedTeleporters.size() + " npc teleporters.");
		}
		finally
		{
			_loadingTeleporters = null;
		}
	}
	
	/**
	 * Parses the XML content from a {@code Document} to populate teleport data.<br>
	 * This method reads NPC IDs and their associated teleport locations.<br>
	 * It handles multiple teleport lists for each NPC entry.
	 * @param document The {@code Document} object containing the XML data.
	 * @param file The {@code File} source of the XML data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", list -> forEach(list, "npc", npc ->
		{
			final Map<String, TeleportHolder> teleList = new HashMap<>();
			final List<Integer> sharingNpcIds = new ArrayList<>();
			
			// Parse npc node child.
			final int npcId = parseInteger(npc.getAttributes(), "id");
			forEach(npc, node ->
			{
				switch (node.getNodeName())
				{
					case "teleport":
					{
						final NamedNodeMap nodeAttrs = node.getAttributes();
						
						// Parse attributes
						final TeleportType type = parseEnum(nodeAttrs, TeleportType.class, "type");
						final String name = parseString(nodeAttrs, "name", type.name());
						
						// Parse locations
						final TeleportHolder holder = new TeleportHolder(name, type);
						forEach(node, "location", location -> holder.registerLocation(new StatSet(parseAttributes(location))));
						
						// Register holder
						if (teleList.putIfAbsent(name, holder) != null)
						{
							LOGGER.warning("Duplicate teleport list (" + name + ") has been found for NPC: " + npcId);
						}
						break;
					}
					case "npcs":
					{
						// Collected here and registered below; the node usually precedes the teleport lists.
						forEach(node, "npc", npcNode -> sharingNpcIds.add(parseInteger(npcNode.getAttributes(), "id")));
						break;
					}
				}
			});
			
			registerTeleportList(npcId, teleList);
			for (Integer sharingNpcId : sharingNpcIds)
			{
				registerTeleportList(sharingNpcId.intValue(), teleList);
			}
		}));
	}
	
	/**
	 * Gets the total number of teleporters currently loaded.<br>
	 * This method counts all entries in the internal map.
	 * @return The total count of teleporters as an {@code int}.
	 */
	public int getTeleporterCount()
	{
		return _teleporters.size();
	}
	
	/**
	 * Registers a map of teleport holders for a specific NPC.<br>
	 * This method stores the list in the catalog being loaded.
	 * @param npcId The unique identifier for the NPC.
	 * @param teleList A map containing the teleport data associated with this NPC.
	 */
	private void registerTeleportList(int npcId, Map<String, TeleportHolder> teleList)
	{
		final Map<Integer, Map<String, TeleportHolder>> loadingTeleporters = _loadingTeleporters;
		if (loadingTeleporters == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Skipping teleport list outside loading context. npcId: " + npcId);
			return;
		}
		
		loadingTeleporters.computeIfAbsent(npcId, k -> new ConcurrentHashMap<>()).putAll(teleList);
	}
	
	/**
	 * Retrieves a specific {@code TeleportHolder} based on the NPC ID and list name.<br>
	 * This method looks up the data within the internal teleporter map.
	 * @param npcId The unique identifier for the NPC.
	 * @param listName The name of the teleport list to search in.
	 * @return The {@code TeleportHolder} if found, or {@code null} if it does not exist.
	 */
	public TeleportHolder getHolder(int npcId, String listName)
	{
		return _teleporters.getOrDefault(npcId, Collections.emptyMap()).get(listName);
	}
	
	/**
	 * Provides access to the singleton instance of {@code TeleporterData}.<br>
	 * Use this method to retrieve the global data manager for teleporters.
	 * @return The single instance of {@code TeleporterData}.
	 */
	public static TeleporterData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final TeleporterData INSTANCE = new TeleporterData();
	}
}
