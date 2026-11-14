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
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;

/**
 * This class handles the parsing of clan level data from XML files.<br>
 * It maps configuration values to internal data structures for use by the game server.
 * @author Mobius
 */
public class ClanLevelData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ClanLevelData.class.getName());
	
	private static final int EXPECTED_CLAN_LEVEL_DATA = 16; // Level 0 included.
	
	private int[] CLAN_EXP;
	private int[] CLAN_COMMON_MEMBERS;
	private int[] CLAN_ELITE_MEMBERS;
	private int MAX_CLAN_LEVEL = 0;
	
	/**
	 * Constructs a new {@code ClanLevelData} object.<br>
	 * This constructor automatically calls the {@code load()} method.<br>
	 * It initializes the data required for clan levels.
	 */
	protected ClanLevelData()
	{
		load();
	}
	
	/**
	 * Initializes the internal arrays for clan data.<br>
	 * This method reads the configuration from {@code data/ClanLevelData.xml}.<br>
	 * It sets up the experience and member limits for all levels.
	 */
	@Override
	public synchronized void load()
	{
		CLAN_EXP = new int[EXPECTED_CLAN_LEVEL_DATA];
		CLAN_COMMON_MEMBERS = new int[EXPECTED_CLAN_LEVEL_DATA];
		CLAN_ELITE_MEMBERS = new int[EXPECTED_CLAN_LEVEL_DATA];
		MAX_CLAN_LEVEL = 0;
		
		parseDatapackFile("data/ClanLevelData.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + (EXPECTED_CLAN_LEVEL_DATA - 1) /* level 0 excluded */ + " clan level data.");
	}
	
	/**
	 * This method reads clan level data from an XML file.<br>
	 * It populates the internal arrays with experience and member limits.<br>
	 * The {@code document} object must contain a valid list of clan entries.
	 * @param document The {@code Document} object containing the XML data.
	 * @param file The {@code File} object representing the source on disk.
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
					if ("clan".equals(d.getNodeName()))
					{
						NamedNodeMap attrs = d.getAttributes();
						
						final int level = parseInteger(attrs, "level");
						final int exp = parseInteger(attrs, "exp");
						final int commonMembers = parseInteger(attrs, "commonMembers");
						final int eliteMembers = parseInteger(attrs, "eliteMembers");
						
						if (MAX_CLAN_LEVEL < level)
						{
							MAX_CLAN_LEVEL = level;
						}
						
						CLAN_EXP[level] = exp;
						CLAN_COMMON_MEMBERS[level] = commonMembers;
						CLAN_ELITE_MEMBERS[level] = eliteMembers;
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves the experience required to reach a specific clan level.<br>
	 * This method looks up the value in the {@code CLAN_EXP} array.
	 * @param clanLevel The level of the clan to check.
	 * @return The amount of experience needed for the given level.
	 */
	public int getLevelExp(int clanLevel)
	{
		return CLAN_EXP[clanLevel];
	}
	
	/**
	 * Retrieves the maximum number of common members allowed for a specific clan level.<br>
	 * This value is pulled from the internal {@code CLAN_COMMON_MEMBERS} array.
	 * @param clanLevel The level of the clan to check.
	 * @return The limit of common members as an {@code int}.
	 */
	public int getCommonMemberLimit(int clanLevel)
	{
		return CLAN_COMMON_MEMBERS[clanLevel];
	}
	
	/**
	 * Retrieves the maximum number of elite members allowed for a specific clan level.<br>
	 * This value is pulled from the internal {@code CLAN_ELITE_MEMBERS} array.
	 * @param clanLevel The current level of the clan.
	 * @return The limit of elite members as an {@code int}.
	 */
	public int getEliteMemberLimit(int clanLevel)
	{
		return CLAN_ELITE_MEMBERS[clanLevel];
	}
	
	/**
	 * Retrieves the maximum level allowed for a clan.<br>
	 * This value is loaded from the configuration data.
	 * @return The highest possible clan level as an {@code int}.
	 */
	public int getMaxLevel()
	{
		return MAX_CLAN_LEVEL;
	}
	
	/**
	 * Retrieves the singleton instance of {@code ClanLevelData}.<br>
	 * This method provides global access to the loaded clan level configuration.
	 * @return The single instance of {@code ClanLevelData}.
	 */
	public static ClanLevelData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ClanLevelData INSTANCE = new ClanLevelData();
	}
}
