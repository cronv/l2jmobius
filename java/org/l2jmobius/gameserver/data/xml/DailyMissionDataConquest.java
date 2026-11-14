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
 * This class handles the loading and parsing of conquest-related daily mission data from XML files.<br>
 * It provides the necessary structure to initialize specific rewards and requirements for these missions.
 * @author Sdw, Mobius
 */
public class DailyMissionDataConquest implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(DailyMissionDataConquest.class.getName());
	
	private final Map<Integer, List<DailyMissionDataHolder>> _dailyMissionRewards = new LinkedHashMap<>();
	private final List<DailyMissionDataHolder> _dailyMissionData = new ArrayList<>();
	private boolean _isAvailable;
	
	/**
	 * Initializes a new instance of the {@code DailyMissionDataConquest} class.<br>
	 * This constructor automatically calls the {@code load()} method to populate data.
	 */
	protected DailyMissionDataConquest()
	{
		load();
	}
	
	/**
	 * Loads the daily mission conquest data from the XML file.<br>
	 * This method clears existing data and populates the internal lists.<br>
	 * It also updates the {@code _isAvailable} flag based on the loaded rewards.
	 */
	@Override
	public void load()
	{
		_dailyMissionRewards.clear();
		parseDatapackFile("data/DailyMissionConquest.xml");
		
		_dailyMissionData.clear();
		for (List<DailyMissionDataHolder> missionList : _dailyMissionRewards.values())
		{
			_dailyMissionData.addAll(missionList);
		}
		
		_isAvailable = !_dailyMissionRewards.isEmpty();
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _dailyMissionRewards.size() + " one day conquest rewards.");
	}
	
	/**
	 * Parses the XML document to populate daily mission rewards.<br>
	 * This method reads reward data, items, and class restrictions from the provided file.
	 * @param document The {@code Document} object containing the parsed XML content.
	 * @param file The {@code File} source of the XML data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "reward", rewardNode ->
		{
			final StatSet set = new StatSet(parseAttributes(rewardNode));
			final List<ItemHolder> items = new ArrayList<>(1);
			
			// Process items.
			forEach(rewardNode, "items", itemsNode -> forEach(itemsNode, "item", itemNode ->
			{
				final NamedNodeMap itemAttrs = itemNode.getAttributes();
				final int itemId = parseInteger(itemAttrs, "id");
				final int itemCount = parseInteger(itemAttrs, "count");
				items.add(new ItemHolder(itemId, itemCount));
			}));
			
			set.set("items", items);
			
			// Process class restrictions.
			final List<PlayerClass> classRestriction = new ArrayList<>(1);
			forEach(rewardNode, "classId", classRestrictionNode -> classRestriction.add(PlayerClass.getPlayerClass(Integer.parseInt(classRestrictionNode.getTextContent()))));
			set.set("classRestriction", classRestriction);
			
			// Initial values in case handler doesn't exist.
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
	 * Retrieves the list of all daily mission data.<br>
	 * This method returns the internal collection of {@code DailyMissionDataHolder} objects.
	 * @return a {@code Collection} containing all {@code DailyMissionDataHolder} instances.
	 */
	public Collection<DailyMissionDataHolder> getDailyMissionData()
	{
		return _dailyMissionData;
	}
	
	/**
	 * Retrieves the daily missions for a specific player.<br>
	 * This method filters out missions that are not displayable to the {@code Player}.
	 * @param player The {@code Player} object used to check mission visibility.
	 * @return A {@code Collection} of {@code DailyMissionDataHolder} objects available to the player.
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
	 * Retrieves the collection of daily mission rewards for a specific ID.<br>
	 * This method looks up data based on the provided {@code id}.
	 * @param id The unique identifier for the daily mission reward set.
	 * @return A {@code Collection} of {@code DailyMissionDataHolder} objects, or {@code null} if not found.
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
	 * Provides access to the singleton instance of {@code DailyMissionDataConquest}.<br>
	 * Use this method to get the global data manager for daily missions.
	 * @return The single instance of {@code DailyMissionDataConquest}.
	 */
	public static DailyMissionDataConquest getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final DailyMissionDataConquest INSTANCE = new DailyMissionDataConquest();
	}
}
