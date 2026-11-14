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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.mechanics.siege.CastleSide;
import org.l2jmobius.gameserver.mechanics.siege.CastleSpawnHolder;
import org.l2jmobius.gameserver.mechanics.siege.SiegeGuardHolder;
import org.l2jmobius.gameserver.mechanics.siege.SiegeGuardType;

/**
 * This class handles the loading and parsing of castle data from XML files.<br>
 * It provides the necessary configuration for siege mechanics within the game world.
 * @author St3eT, BazookaRpm
 */
public class CastleData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(CastleData.class.getName());
	
	private final Map<Integer, List<CastleSpawnHolder>> _spawns = new ConcurrentHashMap<>();
	private final Map<Integer, List<SiegeGuardHolder>> _siegeGuards = new ConcurrentHashMap<>();
	
	/**
	 * Initializes a new instance of the {@code CastleData} class.<br>
	 * This constructor automatically calls the {@code load()} method to populate data.
	 */
	protected CastleData()
	{
		load();
	}
	
	/**
	 * Loads the castle data from the configuration files.<br>
	 * This method clears existing data and parses the directory.<br>
	 * It populates the internal maps with spawn and guard information.
	 */
	@Override
	public void load()
	{
		_spawns.clear();
		_siegeGuards.clear();
		parseDatapackDirectory("data/residences/castles", true);
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _spawns.size() + " castle spawn lists and " + _siegeGuards.size() + " siege guard lists.");
	}
	
	/**
	 * Parses the provided {@code Document} to populate castle data.<br>
	 * This method reads spawn and siege guard information from the XML structure.<br>
	 * It maps these values into internal collections based on the castle ID.
	 * @param document The {@code Document} object containing the XML data.
	 * @param file The {@code File} associated with the parsed document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node listNode = document.getFirstChild(); listNode != null; listNode = listNode.getNextSibling())
		{
			if ("list".equals(listNode.getNodeName()))
			{
				for (Node castleNode = listNode.getFirstChild(); castleNode != null; castleNode = castleNode.getNextSibling())
				{
					if ("castle".equals(castleNode.getNodeName()))
					{
						final int castleId = parseInteger(castleNode.getAttributes(), "id");
						for (Node tpNode = castleNode.getFirstChild(); tpNode != null; tpNode = tpNode.getNextSibling())
						{
							final String tpName = tpNode.getNodeName();
							if ("spawns".equals(tpName))
							{
								final List<CastleSpawnHolder> spawns = new ArrayList<>();
								for (Node npcNode = tpNode.getFirstChild(); npcNode != null; npcNode = npcNode.getNextSibling())
								{
									if ("npc".equals(npcNode.getNodeName()))
									{
										final NamedNodeMap np = npcNode.getAttributes();
										final int npcId = parseInteger(np, "id");
										final CastleSide side = parseEnum(np, CastleSide.class, "castleSide", CastleSide.NEUTRAL);
										final int x = parseInteger(np, "x");
										final int y = parseInteger(np, "y");
										final int z = parseInteger(np, "z");
										final int heading = parseInteger(np, "heading");
										spawns.add(new CastleSpawnHolder(npcId, side, x, y, z, heading));
									}
								}
								
								_spawns.put(castleId, spawns);
							}
							else if ("siegeGuards".equals(tpName))
							{
								final List<SiegeGuardHolder> guards = new ArrayList<>();
								final SiegeGuardType type = parseEnum(tpNode.getAttributes(), SiegeGuardType.class, "type");
								for (Node npcNode = tpNode.getFirstChild(); npcNode != null; npcNode = npcNode.getNextSibling())
								{
									if ("guard".equals(npcNode.getNodeName()))
									{
										final NamedNodeMap np = npcNode.getAttributes();
										final int itemId = parseInteger(np, "itemId");
										final boolean stationary = parseBoolean(np, "stationary", false);
										final int npcId = parseInteger(np, "npcId");
										final int npcMaxAmount = parseInteger(np, "npcMaxAmount");
										guards.add(new SiegeGuardHolder(castleId, itemId, type, stationary, npcId, npcMaxAmount));
									}
								}
								
								_siegeGuards.put(castleId, guards);
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves a list of spawn points for a specific castle and side.<br>
	 * It filters the spawns based on the provided {@code CastleSide}.
	 * @param castleId The unique identifier for the castle.
	 * @param side The side of the castle to filter by.
	 * @return A list of {@code CastleSpawnHolder} objects matching the criteria.
	 */
	public List<CastleSpawnHolder> getSpawnsForSide(int castleId, CastleSide side)
	{
		final List<CastleSpawnHolder> result = new ArrayList<>();
		final List<CastleSpawnHolder> spawns = _spawns.get(castleId);
		if (spawns != null)
		{
			for (CastleSpawnHolder spawn : spawns)
			{
				if (spawn.getSide() == side)
				{
					result.add(spawn);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves the list of siege guards for a specific castle.<br>
	 * It uses the provided {@code castleId} to look up the data.<br>
	 * If no guards are found, it returns an empty list.
	 * @param castleId The unique identifier of the castle.
	 * @return A {@code List} of {@code SiegeGuardHolder} objects.
	 */
	public List<SiegeGuardHolder> getSiegeGuardsForCastle(int castleId)
	{
		return _siegeGuards.getOrDefault(castleId, Collections.emptyList());
	}
	
	/**
	 * Retrieves the global instance of {@code CastleData}.<br>
	 * This method follows the singleton pattern.<br>
	 * Use this to access castle data from anywhere in the code.
	 * @return The single instance of {@code CastleData}.
	 */
	public static CastleData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CastleData INSTANCE = new CastleData();
	}
}
