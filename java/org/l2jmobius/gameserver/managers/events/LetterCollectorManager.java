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
package org.l2jmobius.gameserver.managers.events;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;

/**
 * Manages the logic for collecting letters within the game world.<br>
 * It handles the processing of letter-related events and interactions.<br>
 * This class ensures that letter collection mechanics function correctly for players.
 * @author Index
 */
public class LetterCollectorManager
{
	protected static final Logger LOGGER = Logger.getLogger(LetterCollectorManager.class.getName());
	
	private final Map<Integer, LetterCollectorRewardHolder> _rewards = new HashMap<>();
	private final Map<Integer, List<ItemHolder>> _words = new HashMap<>();
	private final Map<String, Integer> _letter = new HashMap<>();
	private final Map<Integer, Boolean> _needToSumAllChance = new HashMap<>();
	
	private int _minLevel = 1;
	private int _maxLevel = PlayerConfig.PLAYER_MAXIMUM_LEVEL;
	
	/**
	 * Constructs a new instance of the {@code LetterCollectorManager}.<br>
	 * This constructor is protected to restrict instantiation.
	 */
	protected LetterCollectorManager()
	{
	}
	
	/**
	 * Initializes the manager and logs the loaded data.<br>
	 * It prints the count of rewards and letters to the logger.
	 */
	public void init()
	{
		final String simpleName = getClass().getSimpleName();
		LOGGER.info(simpleName + ": Loaded " + _rewards.size() + " words.");
		LOGGER.info(simpleName + ": Loaded " + _letter.size() + " letters.");
	}
	
	/**
	 * Retrieves the minimum level required for the letter collector.<br>
	 * This value is used to filter eligible players.
	 * @return The current {@code int} value of the minimum level.
	 */
	public int getMinLevel()
	{
		return _minLevel;
	}
	
	/**
	 * Sets the minimum level required for letter collection.<br>
	 * This updates the {@code _minLevel} field.
	 * @param minLevel The new minimum level value.
	 */
	public void setMinLevel(int minLevel)
	{
		_minLevel = minLevel;
	}
	
	/**
	 * Retrieves the maximum level allowed for a letter collector.<br>
	 * This value is used to determine the upper limit of progression.
	 * @return The current {@code int} value of the maximum level.
	 */
	public int getMaxLevel()
	{
		return _maxLevel;
	}
	
	/**
	 * Sets the maximum level for the letter collector.<br>
	 * If the provided {@code maxLevel} is less than {@code 1}, it defaults to {@code PlayerConfig.PLAYER_MAXIMUM_LEVEL}.
	 * @param maxLevel The maximum level to set.
	 */
	public void setMaxLevel(int maxLevel)
	{
		if (maxLevel < 1)
		{
			_maxLevel = PlayerConfig.PLAYER_MAXIMUM_LEVEL;
		}
		else
		{
			_maxLevel = maxLevel;
		}
	}
	
	/**
	 * Retrieves the reward holder for a specific letter collector ID.<br>
	 * This method looks up the rewards in the internal map.
	 * @param id The unique identifier for the reward set.
	 * @return The {@code LetterCollectorRewardHolder} associated with the given {@code id}, or {@code null} if not found.
	 */
	public LetterCollectorRewardHolder getRewards(int id)
	{
		return _rewards.get(id);
	}
	
	/**
	 * Retrieves the list of items associated with a specific word ID.<br>
	 * This method looks up the data in the internal words map.
	 * @param id The unique identifier for the word.
	 * @return A {@code List} of {@code ItemHolder} objects, or {@code null} if not found.
	 */
	public List<ItemHolder> getWord(int id)
	{
		return _words.get(id);
	}
	
	/**
	 * Sets the rewards for the letter collector.<br>
	 * This method updates the internal reward map with the provided data.
	 * @param rewards A {@code Map} containing the IDs and their corresponding {@code LetterCollectorRewardHolder}.
	 */
	public void setRewards(Map<Integer, LetterCollectorRewardHolder> rewards)
	{
		_rewards.putAll(rewards);
	}
	
	/**
	 * Updates the internal word collection with new data.<br>
	 * This method adds all entries from the provided map to the existing words list.
	 * @param words A {@code Map<Integer, List<ItemHolder>>} containing the word IDs and their associated items.
	 */
	public void setWords(Map<Integer, List<ItemHolder>> words)
	{
		_words.putAll(words);
	}
	
	/**
	 * Adds a new reward to the collection.<br>
	 * This method maps a specific ID to its corresponding {@code LetterCollectorRewardHolder}.<br>
	 * If the ID already exists, it will be overwritten.
	 * @param id The unique identifier for the reward.
	 * @param rewards The {@code LetterCollectorRewardHolder} object containing the reward data.
	 */
	public void addRewards(int id, LetterCollectorRewardHolder rewards)
	{
		_rewards.put(id, rewards);
	}
	
	/**
	 * Adds a list of items to a specific identifier.<br>
	 * This method maps the {@code List<ItemHolder>} to the provided {@code id}.
	 * @param id The unique identifier for the word collection.
	 * @param words The list of {@code ItemHolder} objects to associate with the ID.
	 */
	public void addWords(int id, List<ItemHolder> words)
	{
		_words.put(id, words);
	}
	
	/**
	 * Resets the internal fields of the {@code LetterCollectorManager}.<br>
	 * It sets {@code _minLevel} back to 1.<br>
	 * It clears the {@code _rewards}, {@code _words}, and {@code _needToSumAllChance} maps.
	 */
	public void resetField()
	{
		_minLevel = 1;
		_rewards.clear();
		_words.clear();
		_needToSumAllChance.clear();
	}
	
	/**
	 * Updates the internal collection of letters.<br>
	 * This method adds all entries from the provided {@code Map<String, Integer>} to the current letter map.
	 * @param letters The map containing the letters and their corresponding values.
	 */
	public void setLetters(Map<String, Integer> letters)
	{
		_letter.putAll(letters);
	}
	
	/**
	 * Retrieves the current map of letters and their counts.<br>
	 * This method returns the internal {@code _letter} collection.
	 * @return A {@code Map<String, Integer>} containing all letters.
	 */
	public Map<String, Integer> getLetters()
	{
		return _letter;
	}
	
	/**
	 * Sets whether a specific reward requires all letters to be summed.<br>
	 * This updates the configuration for the given {@code id}.
	 * @param id The unique identifier for the reward.
	 * @param needToSumAllChance The boolean value to set for this requirement.
	 */
	public void setNeedToSumAllChance(int id, boolean needToSumAllChance)
	{
		_needToSumAllChance.put(id, needToSumAllChance);
	}
	
	/**
	 * Checks if a specific reward requires all letters to be summed.<br>
	 * This method retrieves the boolean value associated with the given {@code id}.
	 * @param id The unique identifier for the reward.
	 * @return {@code true} if all letters must be summed, {@code false} otherwise.
	 */
	public boolean getNeedToSumAllChance(int id)
	{
		return _needToSumAllChance.get(id);
	}
	
	public static class LetterCollectorRewardHolder
	{
		final List<ItemChanceHolder> _rewards;
		final double _chance;
		
		public LetterCollectorRewardHolder(List<ItemChanceHolder> rewards, double chance)
		{
			_rewards = rewards;
			_chance = chance;
		}
		
		public List<ItemChanceHolder> getRewards()
		{
			return _rewards;
		}
		
		public double getChance()
		{
			return _chance;
		}
	}
	
	/**
	 * Provides the global instance of the {@code LetterCollectorManager}.<br>
	 * This method follows the singleton pattern.<br>
	 * Use this to access the manager from anywhere in the code.
	 * @return The single instance of {@code LetterCollectorManager}.
	 */
	public static LetterCollectorManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final LetterCollectorManager INSTANCE = new LetterCollectorManager();
	}
}
