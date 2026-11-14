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
 * This class handles the loading and parsing of karma loss configuration from XML files.<br>
 * It defines how much karma is lost during specific game events or actions.
 * @author Mobius
 */
public class KarmaLossData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(KarmaLossData.class.getName());
	
	private final double[] _levelModifiers = new double[PlayerConfig.PLAYER_MAXIMUM_LEVEL];
	
	/**
	 * Constructs a new {@code KarmaLossData} object.<br>
	 * This constructor automatically calls the {@code load()} method.<br>
	 * It initializes the data required for karma loss calculations.
	 */
	protected KarmaLossData()
	{
		load();
	}
	
	/**
	 * Loads the karma loss data from the configuration file.<br>
	 * This method initializes the {@code _levelModifiers} array with a default value of {@code 1d}.<br>
	 * It then parses the specific XML file located at {@code data/stats/players/karmaLoss.xml}.
	 */
	@Override
	public void load()
	{
		Arrays.fill(_levelModifiers, 1d);
		parseDatapackFile("data/stats/players/karmaLoss.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + (_levelModifiers.length - 1) + " modifiers.");
	}
	
	/**
	 * This method reads karma loss modifiers from an XML file.<br>
	 * It populates the internal level modifier array based on the provided data.
	 * @param document The {@code Document} object containing the XML structure.
	 * @param file The {@code File} object representing the source of the XML data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "modifier", modifierNode ->
		{
			final NamedNodeMap attrs = modifierNode.getAttributes();
			final int level = parseInteger(attrs, "level");
			if (level >= PlayerConfig.PLAYER_MAXIMUM_LEVEL)
			{
				return;
			}
			
			_levelModifiers[level] = parseDouble(attrs, "val");
		}));
	}
	
	/**
	 * Retrieves the karma loss modifier for a specific player level.<br>
	 * If the requested {@code level} exceeds {@code PlayerConfig.PLAYER_MAXIMUM_LEVEL},<br>
	 * it returns the modifier for the highest possible level.
	 * @param level The character level to check.
	 * @return The double value of the karma loss modifier.
	 */
	public double getModifier(int level)
	{
		if (level >= PlayerConfig.PLAYER_MAXIMUM_LEVEL)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Requested too high level (" + level + ").");
			return _levelModifiers[_levelModifiers.length - 1];
		}
		
		return _levelModifiers[level];
	}
	
	/**
	 * Retrieves the singleton instance of {@code KarmaLossData}.<br>
	 * Use this method to access the shared data for karma loss.
	 * @return The global instance of {@code KarmaLossData}.
	 */
	public static KarmaLossData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final KarmaLossData INSTANCE = new KarmaLossData();
	}
}
