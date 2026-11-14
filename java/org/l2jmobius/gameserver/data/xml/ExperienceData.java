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

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.PlayerConfig;

/**
 * This class manages the experience point requirements for player and pet levels.<br>
 * It loads this data from an XML file and provides methods to retrieve required experience for specific levels.<br>
 * It also stores the maximum allowed levels for both players and pets.
 * @author Mobius
 */
public class ExperienceData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ExperienceData.class.getName());
	
	private long[] _expTable;
	private double[] _trainingRateTable;
	
	private int _maxLevel;
	private int _maxPetLevel;
	
	/**
	 * This is the default constructor for {@code ExperienceData}.<br>
	 * It initializes the object by calling the {@code load()} method.<br>
	 * This ensures that experience data is loaded when a new instance is created.
	 */
	protected ExperienceData()
	{
		load();
	}
	
	/**
	 * Loads the experience data from the XML file.<br>
	 * This method reads the configuration from {@code data/stats/players/experience.xml}.<br>
	 * It populates the internal tables and logs the loaded levels to the console.
	 */
	@Override
	public void load()
	{
		parseDatapackFile("data/stats/players/experience.xml");
		final String simpleName = getClass().getSimpleName();
		LOGGER.info(simpleName + ": Loaded " + (_expTable.length - 1) + " levels.");
		LOGGER.info(simpleName + ": Max Player Level is " + (_maxLevel - 1) + ".");
		LOGGER.info(simpleName + ": Max Pet Level is " + (_maxPetLevel - 1) + ".");
	}
	
	/**
	 * This method parses the XML data from a {@code Document}.<br>
	 * It populates the experience tables and sets the maximum levels.<br>
	 * The values are validated against {@code PlayerConfig}.
	 * @param document The {@code Document} object containing the XML content.
	 * @param file The {@code File} object representing the source of the data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, tableNode ->
		{
			final NamedNodeMap tableAttr = tableNode.getAttributes();
			_maxLevel = Integer.parseInt(tableAttr.getNamedItem("maxLevel").getNodeValue()) + 1;
			_maxPetLevel = Integer.parseInt(tableAttr.getNamedItem("maxPetLevel").getNodeValue()) + 1;
			if (_maxLevel > PlayerConfig.PLAYER_MAXIMUM_LEVEL)
			{
				_maxLevel = PlayerConfig.PLAYER_MAXIMUM_LEVEL;
			}
			
			if (_maxPetLevel > (_maxLevel + 1))
			{
				_maxPetLevel = _maxLevel + 1; // Pet level should not exceed owner level.
			}
			
			// Initialize arrays with size _maxLevel + 1 (level 0 is unused).
			final int maxLevelPlusOne = _maxLevel + 1;
			_expTable = new long[maxLevelPlusOne];
			_trainingRateTable = new double[maxLevelPlusOne];
			
			forEach(tableNode, "experience", experienceNode ->
			{
				final NamedNodeMap attrs = experienceNode.getAttributes();
				final int level = parseInteger(attrs, "level");
				if ((level > PlayerConfig.PLAYER_MAXIMUM_LEVEL) || (level > _maxLevel))
				{
					return;
				}
				
				_expTable[level] = parseLong(attrs, "tolevel");
				_trainingRateTable[level] = parseDouble(attrs, "trainingRate");
			});
		});
	}
	
	/**
	 * Retrieves the experience points required to reach a specific level.<br>
	 * If the requested {@code level} exceeds {@code PlayerConfig.PLAYER_MAXIMUM_LEVEL}, it returns the maximum possible experience.
	 * @param level The level to check for experience requirements.
	 * @return The amount of experience needed for the given level as a {@code long}.
	 */
	public long getExpForLevel(int level)
	{
		if (level > PlayerConfig.PLAYER_MAXIMUM_LEVEL)
		{
			return _expTable[PlayerConfig.PLAYER_MAXIMUM_LEVEL];
		}
		
		return _expTable[level];
	}
	
	/**
	 * Retrieves the training rate for a specific level.<br>
	 * If the requested {@code level} exceeds {@code PlayerConfig.PLAYER_MAXIMUM_LEVEL}, it returns the rate for the maximum level.
	 * @param level The level to check.
	 * @return The training rate as a {@code double}.
	 */
	public double getTrainingRate(int level)
	{
		if (level > PlayerConfig.PLAYER_MAXIMUM_LEVEL)
		{
			return _trainingRateTable[PlayerConfig.PLAYER_MAXIMUM_LEVEL];
		}
		
		return _trainingRateTable[level];
	}
	
	/**
	 * Retrieves the maximum level allowed for players.<br>
	 * This value is loaded from the configuration data.
	 * @return The maximum player level as an {@code int}.
	 */
	public int getMaxLevel()
	{
		return _maxLevel;
	}
	
	/**
	 * Retrieves the maximum level allowed for pets.<br>
	 * This value is loaded from the configuration data.
	 * @return The maximum pet level as an {@code int}.
	 */
	public int getMaxPetLevel()
	{
		return _maxPetLevel;
	}
	
	/**
	 * Retrieves the singleton instance of {@code ExperienceData}.<br>
	 * This method provides a global access point to the experience data.
	 * @return The single instance of {@code ExperienceData}.
	 */
	public static ExperienceData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ExperienceData INSTANCE = new ExperienceData();
	}
}