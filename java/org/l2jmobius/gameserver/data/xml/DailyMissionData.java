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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.holders.player.DailyMissionDataHolder;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading and management of daily mission data from XML files.<br>
 * It provides the necessary structure to define requirements and rewards for player missions.
 * @author Sdw, Mobius
 */
public class DailyMissionData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(DailyMissionData.class.getName());
	
	private final Map<Integer, List<DailyMissionDataHolder>> _dailyMissionRewards = new LinkedHashMap<>();
	private final List<DailyMissionDataHolder> _dailyMissionData = new ArrayList<>();
	private boolean _isAvailable;
	
	/**
	 * Initializes a new instance of {@code DailyMissionData}.<br>
	 * This constructor calls the {@code load()} method to populate data.
	 */
	protected DailyMissionData()
	{
		load();
	}
	
	/**
	 * This method initializes the daily mission data.<br>
	 * It clears existing records and reads data from {@code data/DailyMission.xml}.<br>
	 * It populates the internal lists with reward information.<br>
	 * The availability status is updated based on whether any rewards were loaded.
	 */
	@Override
	public void load()
	{
		_dailyMissionRewards.clear();
		parseDatapackFile("data/DailyMission.xml");
		
		_dailyMissionData.clear();
		for (List<DailyMissionDataHolder> missionList : _dailyMissionRewards.values())
		{
			_dailyMissionData.addAll(missionList);
		}
		
		_isAvailable = !_dailyMissionRewards.isEmpty();
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _dailyMissionRewards.size() + " one day rewards.");
	}
	
	/**
	 * Parses the provided {@code Document} to populate daily mission rewards.<br>
	 * This method reads reward data from the XML structure and stores it in internal lists.
	 * @param document The {@code Document} object containing the XML content.
	 * @param file The {@code File} associated with the parsed document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "reward", rewardNode ->
		{
			final StatSet set = new StatSet(parseAttributes(rewardNode));
			final List<ItemHolder> items = new ArrayList<>(1);
			forEach(rewardNode, "items", itemsNode -> forEach(itemsNode, "item", itemNode ->
			{
				final NamedNodeMap itemAttrs = itemNode.getAttributes();
				final int itemId = parseInteger(itemAttrs, "id");
				final int itemCount = parseInteger(itemAttrs, "count");
				items.add(new ItemHolder(itemId, itemCount));
			}));
			
			set.set("items", items);
			
			final List<PlayerClass> classRestriction = new ArrayList<>(1);
			forEach(rewardNode, "classId", classRestrictionNode -> classRestriction.add(PlayerClass.getPlayerClass(Integer.parseInt(classRestrictionNode.getTextContent()))));
			set.set("classRestriction", classRestriction);
			
			// Initial values in case handler doesn't exists.
			set.set("handler", "");
			set.set("params", StatSet.EMPTY_STATSET);
			
			// Parse handler and parameters.
			forEach(rewardNode, "handler", handlerNode ->
			{
				set.set("handler", parseString(handlerNode.getAttributes(), "name"));
				
				final StatSet params = new StatSet();
				set.set("params", params);
				forEach(handlerNode, "param", paramNode -> params.set(parseString(paramNode.getAttributes(), "name"), paramNode.getTextContent()));
			});
			
			final DailyMissionDataHolder holder = new DailyMissionDataHolder(set);
			_dailyMissionRewards.computeIfAbsent(holder.getId(), _ -> new ArrayList<>()).add(holder);
		}));
	}
	
	/**
	 * Retrieves the list of all daily missions.<br>
	 * This method returns the internal collection of {@code DailyMissionDataHolder} objects.
	 * @return A {@code Collection} containing all {@code DailyMissionDataHolder} instances.
	 */
	public Collection<DailyMissionDataHolder> getDailyMissionData()
	{
		return _dailyMissionData;
	}
	
	/**
	 * Retrieves the daily missions for a specific player.<br>
	 * This method filters out missions that are not visible to the {@code Player}.
	 * @param player The {@code Player} object used to check mission visibility.
	 * @return A {@code Collection} of {@code DailyMissionDataHolder} objects that the player can see.
	 */
	public Collection<DailyMissionDataHolder> getDailyMissionData(Player player)
	{
		final List<DailyMissionDataHolder> missionData = new ArrayList<>();
		for (DailyMissionDataHolder mission : _dailyMissionData)
		{
			if (mission.isDisplayable(player))
			{
				missionData.add(mission);
			}
		}
		
		return missionData;
	}
	
	/**
	 * Retrieves the list of daily mission rewards for a specific ID.<br>
	 * This method looks up the data in the internal reward map.
	 * @param id The unique identifier for the daily mission.
	 * @return A {@code Collection} of {@code DailyMissionDataHolder} objects associated with the given {@code id}.
	 */
	public Collection<DailyMissionDataHolder> getDailyMissionData(int id)
	{
		return _dailyMissionRewards.get(id);
	}
	
	/**
	 * Checks if the daily mission data is currently available.<br>
	 * This method returns the current state of the {@code _isAvailable} flag.
	 * @return {@code true} if the data is available, {@code false} otherwise.
	 */
	public boolean isAvailable()
	{
		return _isAvailable;
	}
	
	/**
	 * Retrieves the global instance of {@code DailyMissionData}.<br>
	 * This method follows the singleton pattern.<br>
	 * Use this to access the shared mission data across the server.
	 * @return The single instance of {@code DailyMissionData}.
	 */
	public static DailyMissionData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final DailyMissionData INSTANCE = new DailyMissionData();
	}
}
