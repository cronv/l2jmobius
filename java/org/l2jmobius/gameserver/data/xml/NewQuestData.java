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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.mechanics.script.newquestdata.NewQuest;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the parsing of quest data from XML files.<br>
 * It maps raw XML elements into {@code NewQuest} objects for use in the game server.
 * @author Magik
 */
public class NewQuestData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(NewQuestData.class.getName());
	
	private final Map<Integer, NewQuest> _newQuestData = new LinkedHashMap<>();
	
	/**
	 * Initializes a new instance of the {@code NewQuestData} class.<br>
	 * This constructor calls the {@code load()} method to populate quest data.
	 */
	protected NewQuestData()
	{
		load();
	}
	
	/**
	 * Loads the new quest data from the XML file.<br>
	 * This method clears existing data before parsing the file.<br>
	 * It logs the total number of quests loaded to the console.
	 */
	@Override
	public void load()
	{
		_newQuestData.clear();
		parseDatapackFile("data/NewQuestData.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _newQuestData.size() + " new quest data.");
	}
	
	/**
	 * Parses the provided {@code Document} to populate quest data.<br>
	 * This method reads locations, conditions, rewards, and goals from the XML file.<br>
	 * The parsed information is stored in the internal quest map.
	 * @param document The {@code Document} object containing the XML content.
	 * @param file The {@code File} associated with the loaded data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "quest", questNode ->
		{
			final StatSet set = new StatSet(parseAttributes(questNode));
			
			// Parse locations.
			forEach(questNode, "locations", locationsNode ->
			{
				forEach(locationsNode, "param", paramNode -> set.set(parseString(paramNode.getAttributes(), "name"), paramNode.getTextContent()));
			});
			
			// Parse conditions.
			forEach(questNode, "conditions", conditionsNode ->
			{
				forEach(conditionsNode, "param", paramNode -> set.set(parseString(paramNode.getAttributes(), "name"), paramNode.getTextContent()));
			});
			
			// Parse rewards.
			forEach(questNode, "rewards", rewardsNode ->
			{
				final List<ItemHolder> rewardItems = new ArrayList<>();
				forEach(rewardsNode, "items", itemsNode -> forEach(itemsNode, "item", itemNode ->
				{
					final NamedNodeMap itemAttrs = itemNode.getAttributes();
					final int itemId = parseInteger(itemAttrs, "id");
					final int itemCount = parseInteger(itemAttrs, "count");
					final ItemHolder rewardItem = new ItemHolder(itemId, itemCount);
					rewardItems.add(rewardItem);
				}));
				
				set.set("rewardItems", rewardItems);
				forEach(rewardsNode, "param", paramNode -> set.set(parseString(paramNode.getAttributes(), "name"), paramNode.getTextContent()));
			});
			
			// Parse goals.
			forEach(questNode, "goals", goalsNode ->
			{
				forEach(goalsNode, "param", paramNode -> set.set(parseString(paramNode.getAttributes(), "name"), paramNode.getTextContent()));
			});
			
			final NewQuest holder = new NewQuest(set);
			_newQuestData.put(holder.getId(), holder);
		}));
	}
	
	/**
	 * Retrieves a specific quest based on its unique identifier.<br>
	 * This method searches the internal data map for the provided {@code id}.<br>
	 * It returns {@code null} if no quest is found with that ID.
	 * @param id The unique identification number of the quest to find.
	 * @return The corresponding {@code NewQuest} object, or {@code null} if not found.
	 */
	public NewQuest getQuestById(int id)
	{
		return _newQuestData.get(id);
	}
	
	/**
	 * Retrieves all loaded quest data.<br>
	 * This method returns a collection of {@code NewQuest} objects.
	 * @return A {@code Collection} containing all {@code NewQuest} entries.
	 */
	public Collection<NewQuest> getQuests()
	{
		return _newQuestData.values();
	}
	
	/**
	 * Retrieves the singleton instance of {@code NewQuestData}.<br>
	 * This method provides a global access point to the quest data.
	 * @return The single instance of {@code NewQuestData}.
	 */
	public static NewQuestData getInstance()
	{
		return NewQuestData.SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final NewQuestData INSTANCE = new NewQuestData();
	}
}
