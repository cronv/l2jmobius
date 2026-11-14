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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.holders.player.ClassInfoHolder;

/**
 * This class loads and manages the list of player classes and their associated information.<br>
 * It parses data from XML files to populate {@code ClassInfoHolder} objects.
 * @author Zoey76, Mobius
 */
public class ClassListData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ClassListData.class.getName());
	
	private final Map<PlayerClass, ClassInfoHolder> _classData = new ConcurrentHashMap<>();
	
	/**
	 * Initializes a new instance of the {@code ClassListData} class.<br>
	 * This constructor automatically calls the {@code load()} method to populate data.
	 */
	protected ClassListData()
	{
		load();
	}
	
	/**
	 * Loads the player class data from the XML file.<br>
	 * This method clears existing data before parsing the new configuration.<br>
	 * It logs the total number of classes loaded into the {@code _classData} map.
	 */
	@Override
	public void load()
	{
		_classData.clear();
		parseDatapackFile("data/stats/players/classList.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _classData.size() + " class data.");
	}
	
	/**
	 * Parses the provided {@code Document} to populate class data.<br>
	 * This method reads information from an XML file and stores it in a map.<br>
	 * It identifies each player class by its unique ID.
	 * @param document The {@code Document} object containing the XML content.
	 * @param file The {@code File} source of the XML data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "class", classNode ->
		{
			final NamedNodeMap attrs = classNode.getAttributes();
			final PlayerClass playerClass = PlayerClass.getPlayerClass(parseInteger(attrs, "classId"));
			final PlayerClass parentPlayerClass = parseInteger(attrs, "parentClassId", -1) > 0 ? PlayerClass.getPlayerClass(parseInteger(attrs, "parentClassId")) : null;
			final String className = parseString(attrs, "name");
			_classData.put(playerClass, new ClassInfoHolder(playerClass, parentPlayerClass, className));
		}));
	}
	
	/**
	 * Retrieves the complete list of loaded player classes.<br>
	 * Each {@code PlayerClass} is mapped to its corresponding {@code ClassInfoHolder}.
	 * @return A {@code Map} containing all available class data.
	 */
	public Map<PlayerClass, ClassInfoHolder> getClassList()
	{
		return _classData;
	}
	
	/**
	 * Retrieves the information holder for a specific player class.<br>
	 * This method looks up the data based on the provided {@code PlayerClass}.
	 * @param playerClass The {@code PlayerClass} to look up.
	 * @return The {@code ClassInfoHolder} associated with the class, or {@code null} if not found.
	 */
	public ClassInfoHolder getClass(PlayerClass playerClass)
	{
		return _classData.get(playerClass);
	}
	
	/**
	 * Retrieves the information for a specific player class.<br>
	 * It uses the provided {@code classId} to find the correct data.
	 * @param classId The unique identifier of the class.
	 * @return The {@code ClassInfoHolder} associated with the ID, or {@code null} if not found.
	 */
	public ClassInfoHolder getClass(int classId)
	{
		final PlayerClass id = PlayerClass.getPlayerClass(classId);
		return (id != null) ? _classData.get(id) : null;
	}
	
	/**
	 * Provides the global instance of {@code ClassListData}.<br>
	 * This method follows the singleton pattern.<br>
	 * Use this to access class data from anywhere in the application.
	 * @return The single instance of {@code ClassListData}.
	 */
	public static ClassListData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ClassListData INSTANCE = new ClassListData();
	}
}
