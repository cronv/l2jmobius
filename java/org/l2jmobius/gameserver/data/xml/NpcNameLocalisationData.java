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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.custom.MultilingualSupportConfig;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading and storage of localized NPC names from XML files.<br>
 * It allows for multilingual support by mapping unique identifiers to specific translated strings.
 * @author Mobius
 */
public class NpcNameLocalisationData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(NpcNameLocalisationData.class.getName());
	
	private static final Map<String, Map<Integer, String[]>> NPC_NAME_LOCALISATIONS = new ConcurrentHashMap<>();
	private static String _lang;
	
	/**
	 * Default constructor for the {@code NpcNameLocalisationData} class.<br>
	 * This constructor initializes the data by calling the {@code load()} method.
	 */
	protected NpcNameLocalisationData()
	{
		load();
	}
	
	/**
	 * Loads the NPC name localisation data from XML files.<br>
	 * This method clears existing data before reading new values.<br>
	 * It checks if multilingual support is enabled in {@code MultilingualSupportConfig}.<br>
	 * Files are loaded for every language defined in {@code MultilingualSupportConfig.MULTILANG_ALLOWED}.
	 */
	@Override
	public void load()
	{
		NPC_NAME_LOCALISATIONS.clear();
		
		if (MultilingualSupportConfig.MULTILANG_ENABLE)
		{
			for (String lang : MultilingualSupportConfig.MULTILANG_ALLOWED)
			{
				final File file = new File("data/lang/" + lang + "/NpcNameLocalisation.xml");
				if (!file.isFile())
				{
					continue;
				}
				
				NPC_NAME_LOCALISATIONS.put(lang, new ConcurrentHashMap<>());
				_lang = lang;
				parseDatapackFile("data/lang/" + lang + "/NpcNameLocalisation.xml");
				final int count = NPC_NAME_LOCALISATIONS.get(lang).values().size();
				if (count == 0)
				{
					NPC_NAME_LOCALISATIONS.remove(lang);
				}
				else
				{
					LOGGER.log(Level.INFO, getClass().getSimpleName() + ": Loaded localisations for [" + lang + "].");
				}
			}
		}
	}
	
	/**
	 * Parses the provided {@code Document} to load NPC name localisations.<br>
	 * This method reads data from the XML file and stores it in a map.
	 * @param document The {@code Document} object containing the XML content.
	 * @param file The {@code File} associated with the parsed document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "localisation", localisationNode ->
		{
			final StatSet set = new StatSet(parseAttributes(localisationNode));
			NPC_NAME_LOCALISATIONS.get(_lang).put(set.getInt("id"), new String[]
			{
				set.getString("name"),
				set.getString("title")
			});
		}));
	}
	
	/**
	 * Retrieves the localized name array for a specific NPC.<br>
	 * It looks up the data based on the provided language and ID.
	 * @param lang The language code to use for the lookup.
	 * @param id The unique identifier of the NPC.
	 * @return An array of strings containing the names, or {@code null} if not found.
	 */
	public String[] getLocalisation(String lang, int id)
	{
		final Map<Integer, String[]> localisations = NPC_NAME_LOCALISATIONS.get(lang);
		if (localisations != null)
		{
			return localisations.get(id);
		}
		
		return null;
	}
	
	/**
	 * Checks if a specific NPC has any localization data available.<br>
	 * It searches through all loaded languages for the given {@code id}.
	 * @param id The unique identifier of the NPC to check.
	 * @return {@code true} if the ID exists in any language, {@code false} otherwise.
	 */
	public boolean hasLocalisation(int id)
	{
		for (Map<Integer, String[]> data : NPC_NAME_LOCALISATIONS.values())
		{
			if (data.containsKey(id))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Provides access to the singleton instance of {@code NpcNameLocalisationData}.<br>
	 * Use this method to retrieve the global data holder for NPC name localisations.
	 * @return The single instance of {@code NpcNameLocalisationData}.
	 */
	public static NpcNameLocalisationData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final NpcNameLocalisationData INSTANCE = new NpcNameLocalisationData();
	}
}
