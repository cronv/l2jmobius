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
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.AttendanceRewardsConfig;
import org.l2jmobius.gameserver.data.holders.AttendanceItemHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading and storage of attendance reward data from XML files.<br>
 * It maps configuration values to {@code AttendanceRewardsConfig} for use in the game server.
 * @author Mobius
 */
public class AttendanceRewardData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(AttendanceRewardData.class.getName());
	private final List<AttendanceItemHolder> _rewards = new ArrayList<>();
	private int _rewardsCount = 0;
	
	/**
	 * Initializes a new instance of {@code AttendanceRewardData}.<br>
	 * This constructor automatically calls the {@code load()} method.<br>
	 * It prepares the data for use by the game server.
	 */
	protected AttendanceRewardData()
	{
		load();
	}
	
	/**
	 * Loads the attendance reward data from the configuration file.<br>
	 * This method checks if rewards are enabled in {@code AttendanceRewardsConfig}.<br>
	 * It clears existing rewards and parses the XML file if enabled.
	 */
	@Override
	public void load()
	{
		if (AttendanceRewardsConfig.ENABLE_ATTENDANCE_REWARDS)
		{
			_rewards.clear();
			parseDatapackFile("data/AttendanceRewards.xml");
			_rewardsCount = _rewards.size();
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _rewardsCount + " rewards.");
		}
		else
		{
			LOGGER.info(getClass().getSimpleName() + ": Disabled.");
		}
	}
	
	/**
	 * Parses the provided {@code Document} to populate the reward list.<br>
	 * This method reads items from the XML file and validates their IDs.<br>
	 * Valid rewards are added to the internal collection.
	 * @param document The {@code Document} object containing the XML data.
	 * @param file The {@code File} source of the XML data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "item", rewardNode ->
		{
			// Parse item attributes into StatSet.
			final StatSet set = new StatSet(parseAttributes(rewardNode));
			
			// Extract item ID and count.
			final int itemId = set.getInt("id");
			final int itemCount = set.getInt("count");
			final int highlight = set.getInt("highlight", 0);
			
			// Validate item existence.
			if (ItemData.getInstance().getTemplate(itemId) == null)
			{
				LOGGER.info(getClass().getSimpleName() + ": Item with id " + itemId + " does not exist.");
			}
			else // Add valid item to rewards.
			{
				_rewards.add(new AttendanceItemHolder(itemId, itemCount, highlight));
			}
		}));
	}
	
	/**
	 * Retrieves the list of all attendance rewards.<br>
	 * This method returns the internal collection of {@code AttendanceItemHolder} objects.
	 * @return a {@code List} containing all loaded {@code AttendanceItemHolder} items.
	 */
	public List<AttendanceItemHolder> getRewards()
	{
		return _rewards;
	}
	
	/**
	 * Retrieves the total number of rewards.<br>
	 * This value represents the size of the reward list.
	 * @return The count of rewards as an {@code int}.
	 */
	public int getRewardsCount()
	{
		return _rewardsCount;
	}
	
	/**
	 * Retrieves the global instance of {@code AttendanceRewardData}.<br>
	 * This method follows the singleton pattern.<br>
	 * Use this to access the loaded attendance reward data from anywhere in the code.
	 * @return The single instance of {@code AttendanceRewardData}.
	 */
	public static AttendanceRewardData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AttendanceRewardData INSTANCE = new AttendanceRewardData();
	}
}
