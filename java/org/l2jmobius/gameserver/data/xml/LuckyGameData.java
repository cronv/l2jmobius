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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.LuckyGameDataHolder;
import org.l2jmobius.gameserver.entity.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.entity.item.holders.ItemPointHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading and management of lucky game data from XML files.<br>
 * It provides configuration for various game mechanics related to luck and item chances.<br>
 * The games are published as an immutable snapshot only after the whole file parses, so a failed reload keeps the active games.
 * @author BazookaRpm
 */
public class LuckyGameData implements IXmlReader
{
	private volatile Map<Integer, LuckyGameDataHolder> _luckyGame = Collections.emptyMap();
	private final AtomicInteger _serverPlay = new AtomicInteger();
	
	/**
	 * Initializes a new instance of the {@code LuckyGameData} class.<br>
	 * This constructor automatically calls the {@code load} method to populate data.
	 */
	protected LuckyGameData()
	{
		load();
	}
	
	/**
	 * Loads the lucky game data from the XML file.<br>
	 * The active games are replaced only when the file parses successfully, so a failed reload leaves the play counter and the games untouched.
	 */
	@Override
	public synchronized void load()
	{
		final Map<Integer, LuckyGameDataHolder> previous = _luckyGame;
		parseDatapackFile("data/LuckyGameData.xml");
		if (_luckyGame == previous)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Could not load lucky game data. Keeping the active games.");
			return;
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _luckyGame.size() + " lucky game data.");
	}
	
	/**
	 * Parses the XML content from a {@code Document} and publishes it as the active games.<br>
	 * This method processes common, unique, and modify rewards for lucky games.
	 * @param document The {@code Document} object containing the XML data to be parsed.
	 * @param file The {@code File} associated with the loaded XML document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		if (document == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Cannot parse a null XML document.");
			return;
		}
		
		final Map<Integer, LuckyGameDataHolder> luckyGame = new HashMap<>();
		forEach(document, "list", listNode -> forEach(listNode, "luckygame", rewardNode ->
		{
			final LuckyGameDataHolder holder = new LuckyGameDataHolder(new StatSet(parseAttributes(rewardNode)));
			
			// Parse common_reward items.
			forEach(rewardNode, "common_reward", commonRewardNode -> forEach(commonRewardNode, "item", itemNode ->
			{
				final StatSet stats = new StatSet(parseAttributes(itemNode));
				holder.addCommonReward(new ItemChanceHolder(stats.getInt("id"), stats.getDouble("chance"), stats.getLong("count")));
			}));
			
			// Parse unique_reward items.
			forEach(rewardNode, "unique_reward", uniqueRewardNode -> forEach(uniqueRewardNode, "item", itemNode ->
			{
				holder.addUniqueReward(new ItemPointHolder(new StatSet(parseAttributes(itemNode))));
			}));
			
			// Parse modify_reward items.
			forEach(rewardNode, "modify_reward", uniqueRewardNode ->
			{
				final NamedNodeMap uniqueAttrs = uniqueRewardNode.getAttributes();
				holder.setMinModifyRewardGame(parseInteger(uniqueAttrs, "min_game"));
				holder.setMaxModifyRewardGame(parseInteger(uniqueAttrs, "max_game"));
				forEach(uniqueRewardNode, "item", itemNode ->
				{
					final StatSet stats = new StatSet(parseAttributes(itemNode));
					holder.addModifyReward(new ItemChanceHolder(stats.getInt("id"), stats.getDouble("chance"), stats.getLong("count")));
				});
			});
			
			// Store holder in the candidate map.
			luckyGame.put(parseInteger(rewardNode.getAttributes(), "index"), holder);
		}));
		
		if (luckyGame.isEmpty())
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + ": No lucky games were found in " + (file != null ? file.getAbsolutePath() : "<unknown>") + ".");
		}
		
		_luckyGame = Collections.unmodifiableMap(luckyGame);
	}
	
	/**
	 * Returns the total number of lucky games.<br>
	 * This count is based on the size of the internal map.
	 * @return The total count of lucky games as an {@code int}.
	 */
	public int getLuckyGameCount()
	{
		return _luckyGame.size();
	}
	
	/**
	 * Retrieves the lucky game data for a specific position.<br>
	 * This method looks up the holder using the provided index.
	 * @param index The unique identifier of the lucky game to retrieve.
	 * @return The {@code LuckyGameDataHolder} associated with the index, or {@code null} if not found.
	 */
	public LuckyGameDataHolder getLuckyGameDataByIndex(int index)
	{
		return _luckyGame.get(index);
	}
	
	/**
	 * Increments the total number of games played on the server.<br>
	 * This method updates the internal counter and returns the new value.
	 * @return The updated count of games played as an {@code int}.
	 */
	public int increaseGame()
	{
		return _serverPlay.incrementAndGet();
	}
	
	/**
	 * Retrieves the total number of plays recorded on the server.<br>
	 * This value is stored in an {@code AtomicInteger}.
	 * @return The current count of server plays as an {@code int}.
	 */
	public int getServerPlay()
	{
		return _serverPlay.get();
	}
	
	/**
	 * Retrieves the global instance of {@code LuckyGameData}.<br>
	 * This method follows the singleton pattern.<br>
	 * Use this to access game data from anywhere in the code.
	 * @return The single instance of {@code LuckyGameData}.
	 */
	public static LuckyGameData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final LuckyGameData INSTANCE = new LuckyGameData();
	}
}
