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
package ai.areas.Conquest;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.ConquestConfig;

/**
 * This class parses point data for the {@code Conquest} system.<br>
 * It handles the loading of configuration data from XML files.
 * @author Zoey76, CostyKiller
 */
public class ConquestPointData implements IXmlReader
{
	private final Map<Integer, int[]> _pointsInfo = new HashMap<>();
	private final Map<Integer, int[]> _pvpPointsInfo = new HashMap<>();
	
	/**
	 * Creates a new instance of the {@code ConquestPointData} class.<br>
	 * This constructor checks if the conquest system is enabled in {@code ConquestConfig}.<br>
	 * If it is enabled, it automatically calls the {@code load()} method to initialize data.
	 */
	public ConquestPointData()
	{
		if (ConquestConfig.CONQUEST_SYSTEM_ENABLED)
		{
			load();
		}
	}
	
	/**
	 * Loads the conquest point data from the configuration file.<br>
	 * This method clears existing maps and parses the XML file.<br>
	 * It logs the number of rewards loaded to the console.
	 */
	@Override
	public void load()
	{
		_pointsInfo.clear();
		_pvpPointsInfo.clear();
		parseDatapackFile("data/scripts/ai/areas/Conquest/conquestPoints.xml");
		printSection("Conquest");
		
		final String className = getClass().getSimpleName();
		LOGGER.info(className + ": Loaded " + _pointsInfo.size() + " conquest points reward data.");
		LOGGER.info(className + ": Loaded " + _pvpPointsInfo.size() + " conquest pvp points reward data.");
	}
	
	/**
	 * Prints a formatted header to the log.<br>
	 * The header includes the provided {@code section} name.<br>
	 * It pads the string with dashes to reach a specific length.
	 * @param section The name of the section to display in the logs.
	 */
	private void printSection(String section)
	{
		String s = "=[ " + section + " ]";
		while (s.length() < 61)
		{
			s = "-" + s;
		}
		
		LOGGER.info(s);
	}
	
	/**
	 * Parses the XML content from a file into point data.<br>
	 * This method iterates through all list elements in the {@code Document}.<br>
	 * It calls {@code parsePoint} for each entry found.
	 * @param document The {@code Document} object containing the XML structure.
	 * @param file The {@code File} source of the XML data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equals(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					parsePoint(d);
				}
			}
		}
	}
	
	/**
	 * Parses a specific XML node to extract point data.<br>
	 * It handles both {@code npc} and {@code player} tags.<br>
	 * Data is stored in internal maps based on the unique identifier.
	 * @param d The {@code Node} to be parsed.
	 */
	private void parsePoint(Node d)
	{
		final String nodeName = d.getNodeName();
		if ("npc".equals(nodeName))
		{
			final NamedNodeMap attrs = d.getAttributes();
			Node att = attrs.getNamedItem("id");
			if (att == null)
			{
				LOGGER.severe(getClass().getSimpleName() + ": Missing NPC ID, skipping record!");
				return;
			}
			
			final int npcId = Integer.parseInt(att.getNodeValue());
			att = attrs.getNamedItem("personalPoints");
			if (att == null)
			{
				LOGGER.severe("[Conquest Points Info] Missing personal points info for NPC ID " + npcId + ", skipping record");
				return;
			}
			
			att = attrs.getNamedItem("serverPoints");
			if (att == null)
			{
				LOGGER.severe("[Conquest Points Info] Missing server points info for NPC ID " + npcId + ", skipping record");
				return;
			}
			
			final int personalPoints = Integer.parseInt(att.getNodeValue());
			final int serverPoints = Integer.parseInt(att.getNodeValue());
			att = attrs.getNamedItem("zonePoints");
			final int zonePoints = (att == null) ? 0 : Integer.parseInt(att.getNodeValue());
			att = attrs.getNamedItem("zoneId");
			final int zoneId = (att == null) ? 0 : Integer.parseInt(att.getNodeValue());
			_pointsInfo.put(npcId, new int[]
			{
				personalPoints,
				serverPoints,
				zonePoints,
				zoneId,
			});
		}
		
		if ("player".equals(nodeName))
		{
			final NamedNodeMap attrs = d.getAttributes();
			Node att = attrs.getNamedItem("level");
			if (att == null)
			{
				LOGGER.severe(getClass().getSimpleName() + ": Missing Player level, skipping record!");
				return;
			}
			
			final int level = Integer.parseInt(att.getNodeValue());
			att = attrs.getNamedItem("personalPoints");
			if (att == null)
			{
				LOGGER.severe("[Conquest Points Info] Missing personal points info for PLAYER LEVEL " + level + ", skipping record");
				return;
			}
			
			att = attrs.getNamedItem("serverPoints");
			if (att == null)
			{
				LOGGER.severe("[Conquest Points Info] Missing server points info for PLAYER LEVEL " + level + ", skipping record");
				return;
			}
			
			final int personalPoints = Integer.parseInt(att.getNodeValue());
			final int serverPoints = Integer.parseInt(att.getNodeValue());
			final int bloodyCoins = Integer.parseInt(att.getNodeValue());
			_pvpPointsInfo.put(level, new int[]
			{
				personalPoints,
				serverPoints,
				bloodyCoins,
			});
		}
	}
	
	/**
	 * Retrieves the map of point information.<br>
	 * This method returns all points data loaded from the configuration.
	 * @return a {@code Map<Integer, int[]>} containing the point information.
	 */
	public Map<Integer, int[]> getPointsInfo()
	{
		return _pointsInfo;
	}
	
	/**
	 * Retrieves the information for PVP points.<br>
	 * This map contains point data associated with specific levels.
	 * @return a {@code Map<Integer, int[]>} containing the PVP points info.
	 */
	public Map<Integer, int[]> getPvpPointsInfo()
	{
		return _pvpPointsInfo;
	}
	
	/**
	 * Retrieves the personal points for a specific NPC.<br>
	 * This value is fetched from the internal point information map.
	 * @param npcId The unique identifier of the NPC.
	 * @return The amount of personal points as an {@code int}.
	 */
	public int getPersonalPointsAmount(int npcId)
	{
		return _pointsInfo.get(npcId)[0];
	}
	
	/**
	 * Retrieves the total server points for a specific NPC.<br>
	 * This value is stored in the internal point information map.
	 * @param npcId The unique identifier of the NPC to check.
	 * @return The amount of server points associated with the given {@code npcId}.
	 */
	public int getServerPointsAmount(int npcId)
	{
		return _pointsInfo.get(npcId)[1];
	}
	
	/**
	 * Retrieves the total zone points for a specific NPC.<br>
	 * This value is stored in the third index of the {@code _pointsInfo} map.
	 * @param npcId The unique identifier of the NPC.
	 * @return The amount of zone points as an {@code int}.
	 */
	public int getZonePointsAmount(int npcId)
	{
		return _pointsInfo.get(npcId)[2];
	}
	
	/**
	 * Retrieves the zone identifier for a specific NPC.<br>
	 * This method looks up the data associated with the provided {@code npcId}.
	 * @param npcId The unique identifier of the NPC.
	 * @return The integer ID of the zone where the NPC is located.
	 */
	public int getZoneId(int npcId)
	{
		return _pointsInfo.get(npcId)[3];
	}
	
	/**
	 * Retrieves the amount of personal PVP points for a specific level.<br>
	 * This value is fetched from the internal {@code _pvpPointsInfo} map.
	 * @param level The level used to look up the point data.
	 * @return The number of personal PVP points as an {@code int}.
	 */
	public int getPvpPersonalPointsAmount(int level)
	{
		return _pvpPointsInfo.get(level)[0];
	}
	
	/**
	 * Retrieves the amount of PVP server points for a specific level.<br>
	 * This value is fetched from the internal {@code _pvpPointsInfo} map.
	 * @param level The character level to check.
	 * @return The number of PVP server points as an {@code int}.
	 */
	public int getPvpServerPointsAmount(int level)
	{
		return _pvpPointsInfo.get(level)[1];
	}
	
	/**
	 * Retrieves the amount of coins for a specific level.<br>
	 * This value is pulled from the {@code _pvpPointsInfo} map.
	 * @param level The level to check for coin amounts.
	 * @return The number of coins associated with the given {@code level}.
	 */
	public int getCoinsAmount(int level)
	{
		return _pvpPointsInfo.get(level)[2];
	}
	
	/**
	 * Retrieves the singleton instance of {@code ConquestPointData}.<br>
	 * This method provides a global access point to the data.
	 * @return The single instance of {@code ConquestPointData}.
	 */
	public static ConquestPointData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ConquestPointData INSTANCE = new ConquestPointData();
	}
}
