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
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.PlayerConfig;

/**
 * This class handles the loading and parsing of dynamic experience rate configurations from XML files.<br>
 * It allows for easy modification of experience multipliers across different game zones or conditions.
 * @author MrNiceGuy
 */
public class DynamicExpRateData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(DynamicExpRateData.class.getName());
	
	private static float[] _expRates = new float[PlayerConfig.PLAYER_MAXIMUM_LEVEL + 1];
	private static float[] _spRates = new float[PlayerConfig.PLAYER_MAXIMUM_LEVEL + 1];
	private static boolean _enabled = false;
	
	/**
	 * Initializes a new instance of the {@code DynamicExpRateData} class.<br>
	 * This constructor automatically calls the {@code load()} method to populate data.
	 */
	protected DynamicExpRateData()
	{
		load();
	}
	
	/**
	 * Loads the dynamic experience rate data from the configuration file.<br>
	 * This method calls {@code parseDatapackFile} to read the XML content.
	 */
	@Override
	public void load()
	{
		parseDatapackFile("config/DynamicExpRates.xml");
	}
	
	/**
	 * Parses the XML data from a file to set experience and skill point rates.<br>
	 * It populates internal arrays based on the values found in the {@code Document}.<br>
	 * The system is enabled only if at least one custom rate is defined.
	 * @param document The {@code Document} object containing the XML data.
	 * @param file The {@code File} source of the XML data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		int count = 0;
		Arrays.fill(_expRates, 1f);
		Arrays.fill(_spRates, 1f);
		
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equals(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("dynamic".equals(d.getNodeName()))
					{
						final NamedNodeMap attrs = d.getAttributes();
						final int level = parseInteger(attrs, "level");
						final float exp = parseFloat(attrs, "exp");
						final float sp = parseFloat(attrs, "sp");
						if ((exp != 1) || (sp != 1))
						{
							_expRates[level] = exp;
							_spRates[level] = sp;
							count++;
						}
					}
				}
			}
		}
		
		_enabled = count > 0;
		if (_enabled)
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded dynamic rates for " + count + " levels.");
		}
	}
	
	/**
	 * Retrieves the experience rate for a specific character level.<br>
	 * This value is used to calculate dynamic experience gains.
	 * @param level The character level to check.
	 * @return The experience rate as a {@code float}.
	 */
	public float getDynamicExpRate(int level)
	{
		return _expRates[level];
	}
	
	/**
	 * Retrieves the special point rate for a specific character level.<br>
	 * This value is pulled from the internal {@code _spRates} array.
	 * @param level The character level to check.
	 * @return The special point rate as a {@code float}.
	 */
	public float getDynamicSpRate(int level)
	{
		return _spRates[level];
	}
	
	/**
	 * Checks if the dynamic experience rate system is currently active.<br>
	 * Returns {@code true} if enabled, and {@code false} otherwise.
	 * @return The current status of the dynamic experience rate system.
	 */
	public boolean isEnabled()
	{
		return _enabled;
	}
	
	/**
	 * Retrieves the singleton instance of {@code DynamicExpRateData}.<br>
	 * This method provides a global access point to the data.<br>
	 * Use this instead of creating a new object.
	 * @return The single instance of {@code DynamicExpRateData}.
	 */
	public static DynamicExpRateData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final DynamicExpRateData INSTANCE = new DynamicExpRateData();
	}
}
