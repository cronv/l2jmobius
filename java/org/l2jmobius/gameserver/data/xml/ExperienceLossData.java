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
import java.util.Arrays;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.PlayerConfig;

/**
 * This class handles the loading and parsing of experience loss configuration from XML files.<br>
 * It allows for defining specific rules regarding how much experience players lose during certain events.
 * @author Mobius
 */
public class ExperienceLossData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ExperienceLossData.class.getName());
	
	private final double[] _levelPercentLost = new double[PlayerConfig.PLAYER_MAXIMUM_LEVEL];
	
	/**
	 * Initializes a new instance of the {@code ExperienceLossData} class.<br>
	 * This constructor automatically calls the {@code load()} method to populate data.
	 */
	protected ExperienceLossData()
	{
		load();
	}
	
	/**
	 * Loads the experience loss data from the configuration file.<br>
	 * This method initializes the {@code _levelPercentLost} array with a default value of {@code 1d}.<br>
	 * It then parses the XML file located at {@code data/stats/players/experienceLoss.xml}.
	 */
	@Override
	public void load()
	{
		Arrays.fill(_levelPercentLost, 1d);
		parseDatapackFile("data/stats/players/experienceLoss.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + (_levelPercentLost.length - 1) + " levels.");
	}
	
	/**
	 * Parses experience loss data from an XML file.<br>
	 * This method populates the internal level percentage array.<br>
	 * It iterates through all experience nodes in the {@code Document}.
	 * @param document The {@code Document} object containing the XML content.
	 * @param file The {@code File} source of the XML data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "experience", experienceNode ->
		{
			final NamedNodeMap attrs = experienceNode.getAttributes();
			final int level = parseInteger(attrs, "level");
			if (level >= PlayerConfig.PLAYER_MAXIMUM_LEVEL)
			{
				return;
			}
			
			_levelPercentLost[level] = parseDouble(attrs, "val");
		}));
	}
	
	/**
	 * Calculates the percentage of experience lost for a specific level.<br>
	 * This method retrieves the value from the internal data array.<br>
	 * If the provided {@code level} is too high, it returns the maximum possible loss.
	 * @param level The character level to check.
	 * @return The percentage of experience lost as a {@code double}.
	 */
	public double getPercentLost(int level)
	{
		if (level >= PlayerConfig.PLAYER_MAXIMUM_LEVEL)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Requested too high level (" + level + ").");
			return _levelPercentLost[_levelPercentLost.length - 1];
		}
		
		return _levelPercentLost[level];
	}
	
	/**
	 * Retrieves the singleton instance of {@code ExperienceLossData}.<br>
	 * This method provides a global access point to the shared data.
	 * @return The single instance of {@code ExperienceLossData}.
	 */
	public static ExperienceLossData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ExperienceLossData INSTANCE = new ExperienceLossData();
	}
}
