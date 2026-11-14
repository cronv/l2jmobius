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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.item.appearance.AppearanceHolder;
import org.l2jmobius.gameserver.entity.item.appearance.AppearanceStone;
import org.l2jmobius.gameserver.entity.item.appearance.AppearanceTargetType;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.type.CrystalType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading and storage of appearance item data from XML files.<br>
 * It maps specific items to their visual properties and body parts for use in the game world.<br>
 * The lookup table is published only after the whole file parses, so a failed reload keeps the active stones and an empty datapack leaves an empty table rather than an absent one.
 * @author BazookaRpm
 */
public class AppearanceItemData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(AppearanceItemData.class.getName());
	
	// Stone State.
	private volatile AppearanceStone[] _stones = new AppearanceStone[0];
	
	/**
	 * Initializes a new instance of {@code AppearanceItemData}.<br>
	 * This constructor automatically calls the {@code load()} method.<br>
	 * It prepares the data for use by the game server.
	 */
	protected AppearanceItemData()
	{
		load();
	}
	
	/**
	 * Loads the appearance stones from the data files.<br>
	 * The active table is replaced only when the file parses successfully.
	 */
	@Override
	public void load()
	{
		final AppearanceStone[] previous = _stones;
		parseDatapackFile("data/AppearanceStones.xml");
		if (_stones == previous)
		{
			LOGGER.warning("AppearanceItemData: Could not load appearance stones. Keeping the active table.");
		}
	}
	
	/**
	 * Parses the provided {@code Document} and publishes the appearance stone table.<br>
	 * Stones without an item template are skipped, as they belong to another datapack.
	 * @param document The {@code Document} object containing the XML data.
	 * @param file The {@code File} associated with the parsed document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		if (document == null)
		{
			LOGGER.warning("AppearanceItemData: Cannot parse a null XML document.");
			return;
		}
		
		final Node rootNode = document.getDocumentElement();
		if ((rootNode == null) || !"list".equalsIgnoreCase(rootNode.getNodeName()))
		{
			throw new IllegalArgumentException("AppearanceItemData: Invalid root node in " + getFilePath(file) + ". Expected list.");
		}
		
		final Map<Integer, AppearanceStone> stoneMap = new HashMap<>();
		int highestStoneId = -1;
		for (Node stoneNode = rootNode.getFirstChild(); stoneNode != null; stoneNode = stoneNode.getNextSibling())
		{
			if (!"appearance_stone".equalsIgnoreCase(stoneNode.getNodeName()))
			{
				continue;
			}
			
			final AppearanceStone stone = new AppearanceStone(new StatSet(parseAttributes(stoneNode)));
			for (Node c = stoneNode.getFirstChild(); c != null; c = c.getNextSibling())
			{
				switch (c.getNodeName())
				{
					case "grade":
					{
						final CrystalType type = CrystalType.valueOf(c.getTextContent());
						stone.addCrystalType(type);
						break;
					}
					case "targetType":
					{
						final AppearanceTargetType type = AppearanceTargetType.valueOf(c.getTextContent());
						stone.addTargetType(type);
						break;
					}
					case "bodyPart":
					{
						final BodyPart bodyPart = BodyPart.fromName(c.getTextContent());
						stone.addBodyPart(bodyPart);
						break;
					}
					case "race":
					{
						final Race race = Race.valueOf(c.getTextContent());
						stone.addRace(race);
						break;
					}
					case "raceNot":
					{
						final Race raceNot = Race.valueOf(c.getTextContent());
						stone.addRaceNot(raceNot);
						break;
					}
					case "visual":
					{
						stone.addVisualId(new AppearanceHolder(new StatSet(parseAttributes(c))));
						break;
					}
				}
			}
			
			final Integer stoneId = stone.getId();
			if (stoneId.intValue() < 0)
			{
				LOGGER.warning("AppearanceItemData: Skipping negative appearance stone id " + stoneId + " in " + getFilePath(file) + ".");
				continue;
			}
			
			if (ItemData.getInstance().getTemplate(stoneId) == null)
			{
				LOGGER.info("AppearanceItemData: Could not find appearance stone item " + stoneId);
				continue;
			}
			
			if (stoneId.intValue() > highestStoneId)
			{
				highestStoneId = stoneId.intValue();
			}
			
			stoneMap.put(stoneId, stone);
		}
		
		final AppearanceStone[] stones = new AppearanceStone[highestStoneId + 1];
		for (Entry<Integer, AppearanceStone> stone : stoneMap.entrySet())
		{
			stones[stone.getKey().intValue()] = stone.getValue();
		}
		
		_stones = stones;
		LOGGER.info("AppearanceItemData: Loaded " + stoneMap.size() + " stones.");
	}
	
	/**
	 * Retrieves an {@code AppearanceStone} based on its index.<br>
	 * This method looks up the stone in the internal array.<br>
	 * It returns {@code null} if the provided index is out of bounds.
	 * @param stone The index of the stone to retrieve.
	 * @return The {@code AppearanceStone} at the specified index, or {@code null}.
	 */
	public AppearanceStone getStone(int stone)
	{
		final AppearanceStone[] stones = _stones;
		return (stone >= 0) && (stone < stones.length) ? stones[stone] : null;
	}
	
	/**
	 * Converts a {@code File} object into its absolute path string.<br>
	 * Returns the string {@code <unknown>} if the input is {@code null}.
	 * @param file The {@code File} to describe.
	 * @return The absolute path, or {@code <unknown>} when the file is not available.
	 */
	private String getFilePath(File file)
	{
		return file != null ? file.getAbsolutePath() : "<unknown>";
	}
	
	/**
	 * Retrieves the global instance of {@code AppearanceItemData}.<br>
	 * This method follows the singleton pattern.<br>
	 * Use this to access the loaded appearance data from anywhere in the code.
	 * @return The single instance of {@code AppearanceItemData}.
	 */
	public static AppearanceItemData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AppearanceItemData INSTANCE = new AppearanceItemData();
	}
}
