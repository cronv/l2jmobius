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
import org.l2jmobius.gameserver.config.HuntPassConfig;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading and storage of data for the {@code HuntPass} system from XML files.<br>
 * It provides a structured way to access configuration values defined in the game's data folders.
 * @author Serenitty
 */
public class HuntPassData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(HuntPassData.class.getName());
	
	private final List<ItemHolder> _rewards = new ArrayList<>();
	private final List<ItemHolder> _premiumRewards = new ArrayList<>();
	private int _rewardCount = 0;
	private int _premiumRewardCount = 0;
	
	/**
	 * Constructs a new {@code HuntPassData} object.<br>
	 * This constructor automatically calls the {@code load()} method.<br>
	 * It initializes the data required for hunt passes.
	 */
	protected HuntPassData()
	{
		load();
	}
	
	/**
	 * Loads the hunt pass data from the configuration files.<br>
	 * This method checks if {@code HuntPassConfig.ENABLE_HUNT_PASS} is true.<br>
	 * It populates the reward lists and logs the results to the console.
	 */
	@Override
	public void load()
	{
		if (HuntPassConfig.ENABLE_HUNT_PASS)
		{
			_rewards.clear();
			parseDatapackFile("data/HuntPass.xml");
			_rewardCount = _rewards.size();
			_premiumRewardCount = _premiumRewards.size();
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _rewardCount + " HuntPass rewards.");
		}
		else
		{
			LOGGER.info(getClass().getSimpleName() + ": Disabled.");
		}
	}
	
	/**
	 * Parses the provided {@code Document} to populate reward lists.<br>
	 * This method reads item data from the XML file structure.<br>
	 * It validates each item against the global {@code ItemData} registry.
	 * @param document The {@code Document} object containing the parsed XML content.
	 * @param file The {@code File} source used for logging and identification.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		// Traverse "list" elements and process each "item" node.
		forEach(document, "list", listNode -> forEach(listNode, "item", rewardNode ->
		{
			// Parse attributes of the "item" element into a StatSet.
			final StatSet set = new StatSet(parseAttributes(rewardNode));
			final int itemId = set.getInt("id");
			final int itemCount = set.getInt("count");
			final int premiumItemId = set.getInt("premiumId");
			final int premiumItemCount = set.getInt("premiumCount");
			
			// Check if item exists, log if not found, and add to rewards lists if found.
			if (ItemData.getInstance().getTemplate(itemId) == null)
			{
				LOGGER.info(getClass().getSimpleName() + ": Item with id " + itemId + " does not exist.");
			}
			else
			{
				_rewards.add(new ItemHolder(itemId, itemCount));
				_premiumRewards.add(new ItemHolder(premiumItemId, premiumItemCount));
			}
		}));
	}
	
	/**
	 * Retrieves the list of standard rewards.<br>
	 * This method returns all items associated with the hunt pass.
	 * @return a {@code List} of {@code ItemHolder} objects.
	 */
	public List<ItemHolder> getRewards()
	{
		return _rewards;
	}
	
	/**
	 * Retrieves the total number of rewards.<br>
	 * This count represents all standard rewards associated with this {@code HuntPassData}.
	 * @return The integer count of rewards.
	 */
	public int getRewardsCount()
	{
		return _rewardCount;
	}
	
	/**
	 * Retrieves the list of premium rewards.<br>
	 * This method returns all items associated with the premium tier.
	 * @return a {@code List} of {@code ItemHolder} objects.
	 */
	public List<ItemHolder> getPremiumRewards()
	{
		return _premiumRewards;
	}
	
	/**
	 * Returns the total number of premium rewards.<br>
	 * This count is retrieved from the internal {@code _premiumRewardCount} variable.
	 * @return The number of premium rewards available.
	 */
	public int getPremiumRewardsCount()
	{
		return _premiumRewardCount;
	}
	
	/**
	 * Retrieves the global instance of {@code HuntPassData}.<br>
	 * This method ensures that only one instance of the data is used throughout the application.
	 * @return The singleton instance of {@code HuntPassData}.
	 */
	public static HuntPassData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final HuntPassData INSTANCE = new HuntPassData();
	}
}
