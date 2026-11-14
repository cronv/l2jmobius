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
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.item.holders.InitialEquipment;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading of initial equipment data from XML files.<br>
 * It maps specific items to different {@code PlayerClass} types for new characters.<br>
 * The loaded data is used to populate the {@code InitialEquipment} of a player.
 * @author Mobius
 */
public class InitialEquipmentData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(InitialEquipmentData.class.getName());
	
	private final Map<PlayerClass, List<InitialEquipment>> _classEquipment = new EnumMap<>(PlayerClass.class);
	
	/**
	 * This is the default constructor for {@code InitialEquipmentData}.<br>
	 * It initializes the data by calling the {@code load()} method.
	 */
	protected InitialEquipmentData()
	{
		load();
	}
	
	/**
	 * Loads the initial equipment data from the XML file.<br>
	 * This method clears existing data before parsing the configuration.<br>
	 * It logs a message once the loading process is complete.
	 */
	@Override
	public void load()
	{
		_classEquipment.clear();
		parseDatapackFile("data/stats/players/initialEquipment.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _classEquipment.size() + " initial equipment data.");
	}
	
	/**
	 * This method reads data from an XML file to populate the initial equipment list.<br>
	 * It iterates through all classes and their corresponding items in the {@code Document}.<br>
	 * The parsed information is stored in the internal class equipment map.
	 * @param document The {@code Document} object containing the XML content.
	 * @param file The {@code File} source of the XML data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "equipment", equipmentNode ->
		{
			final Map<String, Object> attributes = parseAttributes(equipmentNode);
			final List<InitialEquipment> equipment = new ArrayList<>();
			forEach(equipmentNode, "item", itemNode ->
			{
				final StatSet set = new StatSet();
				parseAttributes(itemNode).forEach(set::set);
				equipment.add(new InitialEquipment(set));
			});
			
			final PlayerClass playerClass = PlayerClass.getPlayerClass(Integer.parseInt((String) attributes.get("classId")));
			_classEquipment.put(playerClass, equipment);
		}));
	}
	
	/**
	 * Retrieves the starting equipment for a specific character class.<br>
	 * This method looks up the list of items associated with the provided {@code PlayerClass}.
	 * @param playerClass The type of character class to check.
	 * @return A {@code Collection} of {@code InitialEquipment} objects assigned to that class.
	 */
	public Collection<InitialEquipment> getClassEquipment(PlayerClass playerClass)
	{
		return _classEquipment.get(playerClass);
	}
	
	/**
	 * Retrieves the singleton instance of {@code InitialEquipmentData}.<br>
	 * This method provides a global access point to the data.
	 * @return The single instance of {@code InitialEquipmentData}.
	 */
	public static InitialEquipmentData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final InitialEquipmentData INSTANCE = new InitialEquipmentData();
	}
}
