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
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.Sex;
import org.l2jmobius.gameserver.mechanics.beautyshop.BeautyData;
import org.l2jmobius.gameserver.mechanics.beautyshop.BeautyItem;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading and parsing of beauty shop data from XML files.<br>
 * It populates {@code BeautyItem} objects to define available cosmetics in the game.<br>
 * Each race owns its own sex map, and the whole catalog is published only after the file parses.
 * @author BazookaRpm
 */
public class BeautyShopData implements IXmlReader
{
	// Served for races and sexes the datapack does not configure, such as Kamael female.
	private static final BeautyData EMPTY_BEAUTY_DATA = new BeautyData();
	
	private volatile Map<Race, Map<Sex, BeautyData>> _beautyList = Collections.emptyMap();
	
	/**
	 * Initializes a new instance of the {@code BeautyShopData} class.<br>
	 * This constructor automatically calls the {@code load()} method to populate data.
	 */
	protected BeautyShopData()
	{
		load();
	}
	
	/**
	 * Loads the beauty shop data from the configuration file.<br>
	 * The active data is replaced only when the file parses successfully.
	 */
	@Override
	public synchronized void load()
	{
		final Map<Race, Map<Sex, BeautyData>> previous = _beautyList;
		parseDatapackFile("data/BeautyShop.xml");
		if (_beautyList == previous)
		{
			LOGGER.warning("BeautyShopData: Could not load beauty shop data. Keeping the active data.");
			return;
		}
		
		LOGGER.info("BeautyShopData: Loaded beauty shop data for " + _beautyList.size() + " races.");
	}
	
	/**
	 * Parses the provided {@code Document} to populate beauty data.<br>
	 * This method reads XML nodes to identify hair and face attributes.<br>
	 * It organizes the data into maps based on {@code Race} and {@code Sex}.
	 * @param document The {@code Document} object containing the XML content.
	 * @param file The {@code File} associated with the parsed document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		if (document == null)
		{
			LOGGER.warning("BeautyShopData: Cannot parse a null XML document.");
			return;
		}
		
		final Node rootNode = document.getDocumentElement();
		if ((rootNode == null) || !"list".equalsIgnoreCase(rootNode.getNodeName()))
		{
			throw new IllegalArgumentException("BeautyShopData: Invalid root node in " + getFilePath(file) + ". Expected list.");
		}
		
		final Map<Race, Map<Sex, BeautyData>> beautyList = new EnumMap<>(Race.class);
		for (Node raceNode = rootNode.getFirstChild(); raceNode != null; raceNode = raceNode.getNextSibling())
		{
			if (!"race".equalsIgnoreCase(raceNode.getNodeName()))
			{
				continue;
			}
			
			final Race race = parseEnum(raceNode.getAttributes(), Race.class, "type");
			if (race == null)
			{
				throw new IllegalArgumentException("BeautyShopData: Missing or unknown race type in " + getFilePath(file) + ".");
			}
			
			// Every race owns its map. Sharing one made each race resolve to the last race parsed.
			final Map<Sex, BeautyData> sexData = new EnumMap<>(Sex.class);
			for (Node sexNode = raceNode.getFirstChild(); sexNode != null; sexNode = sexNode.getNextSibling())
			{
				if (!"sex".equalsIgnoreCase(sexNode.getNodeName()))
				{
					continue;
				}
				
				final Sex sex = parseEnum(sexNode.getAttributes(), Sex.class, "type");
				if (sex == null)
				{
					throw new IllegalArgumentException("BeautyShopData: Missing or unknown sex type for race " + race + " in " + getFilePath(file) + ".");
				}
				
				sexData.put(sex, parseBeautyData(sexNode));
			}
			
			if (beautyList.put(race, sexData) != null)
			{
				throw new IllegalArgumentException("BeautyShopData: Duplicate race " + race + " in " + getFilePath(file) + ".");
			}
		}
		
		if (beautyList.isEmpty())
		{
			throw new IllegalArgumentException("BeautyShopData: No beauty shop races were found in " + getFilePath(file) + ".");
		}
		
		_beautyList = beautyList;
	}
	
	/**
	 * Checks if beauty data exists for a specific combination of {@code Race} and {@code Sex}.<br>
	 * This method verifies if the requested data is loaded in the internal map.
	 * @param race The {@code Race} to check.
	 * @param sex The {@code Sex} to check.
	 * @return {@code true} if the data exists, otherwise {@code false}.
	 */
	public boolean hasBeautyData(Race race, Sex sex)
	{
		final Map<Sex, BeautyData> raceMap = _beautyList.get(race);
		return (raceMap != null) && raceMap.containsKey(sex);
	}
	
	/**
	 * Retrieves the beauty data for a specific race and sex.<br>
	 * This method looks up the configuration in the internal list.<br>
	 * Combinations the datapack does not configure return empty hair and face lists, so callers take their regular not found branch instead of dereferencing {@code null}.
	 * @param race The {@code Race} of the character.
	 * @param sex The {@code Sex} of the character.
	 * @return The corresponding {@code BeautyData} object, never {@code null}.
	 */
	public BeautyData getBeautyData(Race race, Sex sex)
	{
		return _beautyList.getOrDefault(race, Collections.emptyMap()).getOrDefault(sex, EMPTY_BEAUTY_DATA);
	}
	
	/**
	 * Reads the hair and face entries of one race and sex section.
	 * @param sexNode The XML node holding the cosmetics of a single sex.
	 * @return The parsed {@code BeautyData}.
	 */
	private BeautyData parseBeautyData(Node sexNode)
	{
		final BeautyData beautyData = new BeautyData();
		for (Node itemNode = sexNode.getFirstChild(); itemNode != null; itemNode = itemNode.getNextSibling())
		{
			final String itemName = itemNode.getNodeName();
			if ("hair".equalsIgnoreCase(itemName))
			{
				final BeautyItem hair = new BeautyItem(new StatSet(parseAttributes(itemNode)));
				for (Node colorNode = itemNode.getFirstChild(); colorNode != null; colorNode = colorNode.getNextSibling())
				{
					if ("color".equalsIgnoreCase(colorNode.getNodeName()))
					{
						hair.addColor(new StatSet(parseAttributes(colorNode)));
					}
				}
				
				beautyData.addHair(hair);
			}
			else if ("face".equalsIgnoreCase(itemName))
			{
				beautyData.addFace(new BeautyItem(new StatSet(parseAttributes(itemNode))));
			}
		}
		
		return beautyData;
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
	 * Provides access to the singleton instance of {@code BeautyShopData}.<br>
	 * Use this method to get the global data container for beauty shop items.
	 * @return The single instance of {@code BeautyShopData}.
	 */
	public static BeautyShopData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final BeautyShopData INSTANCE = new BeautyShopData();
	}
}
