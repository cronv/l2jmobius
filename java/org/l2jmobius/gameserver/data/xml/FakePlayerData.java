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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;

/**
 * This class manages data for fake players used in the game server.<br>
 * It stores and retrieves information related to {@code FakePlayersConfig}.
 * @author Mobius
 */
public class FakePlayerData
{
	private static final Logger LOGGER = Logger.getLogger(FakePlayerData.class.getName());
	
	private final Map<String, String> _fakePlayerNames = new ConcurrentHashMap<>();
	private final Map<String, Integer> _fakePlayerIds = new ConcurrentHashMap<>();
	private final Set<String> _talkableFakePlayerNames = ConcurrentHashMap.newKeySet();
	
	/**
	 * Creates a new instance of the {@code FakePlayerData} class.<br>
	 * This constructor initializes the internal data structures for fake players.
	 */
	protected FakePlayerData()
	{
	}
	
	/**
	 * Logs the status of fake players to the server log.<br>
	 * It checks if {@code FakePlayersConfig.FAKE_PLAYERS_ENABLED} is true.<br>
	 * If enabled, it prints the count of loaded templates.<br>
	 * Otherwise, it logs that the feature is disabled.
	 */
	public void report()
	{
		if (FakePlayersConfig.FAKE_PLAYERS_ENABLED)
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _fakePlayerIds.size() + " templates.");
		}
		else
		{
			LOGGER.info(getClass().getSimpleName() + ": Disabled.");
		}
	}
	
	/**
	 * Adds a new fake player to the system.<br>
	 * This maps a specific name to an {@code npcId}.
	 * @param name The display name of the fake player.
	 * @param npcId The unique identifier for the NPC.
	 */
	public void addFakePlayerId(String name, int npcId)
	{
		_fakePlayerIds.put(name, npcId);
	}
	
	/**
	 * Retrieves the unique ID for a fake player based on their name.<br>
	 * This method looks up the {@code String} name in the internal data map.<br>
	 * It returns the corresponding integer ID.
	 * @param name The name of the fake player to search for.
	 * @return The unique NPC ID associated with the name, or null if not found.
	 */
	public int getNpcIdByName(String name)
	{
		return _fakePlayerIds.get(name);
	}
	
	/**
	 * Adds a new fake player name to the system.<br>
	 * This method maps a lowercase version of a name to its display name.
	 * @param lowercaseName The unique identifier for the fake player in lowercase.
	 * @param name The actual name that will be displayed to other players.
	 */
	public void addFakePlayerName(String lowercaseName, String name)
	{
		_fakePlayerNames.put(lowercaseName, name);
	}
	
	/**
	 * Retrieves the correct display name for a given input.<br>
	 * This method converts the input to lowercase to find the matching entry.<br>
	 * It returns the original name stored in the internal map.
	 * @param name The name provided by the user to look up.
	 * @return The proper name associated with the input or {@code null} if not found.
	 */
	public String getProperName(String name)
	{
		return _fakePlayerNames.get(name.toLowerCase());
	}
	
	/**
	 * Adds a fake player name to the list of talkable entities.<br>
	 * This allows players to interact with specific fake characters.
	 * @param lowercaseName The name of the fake player in lowercase.
	 */
	public void addTalkableFakePlayerName(String lowercaseName)
	{
		_talkableFakePlayerNames.add(lowercaseName);
	}
	
	/**
	 * Checks if a fake player can be talked to.<br>
	 * This method looks up the name in the internal set of talkable names.
	 * @param name The name of the fake player to check.
	 * @return {@code true} if the player is talkable, otherwise {@code false}.
	 */
	public boolean isTalkable(String name)
	{
		return _talkableFakePlayerNames.contains(name.toLowerCase());
	}
	
	/**
	 * Provides access to the singleton instance of {@code FakePlayerData}.<br>
	 * Use this method to get the global data manager for fake players.
	 * @return The single instance of {@code FakePlayerData}.
	 */
	public static FakePlayerData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final FakePlayerData INSTANCE = new FakePlayerData();
	}
}
