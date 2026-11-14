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
package org.l2jmobius.gameserver.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.FakePlayerData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.npc.FakePlayerChatHolder;
import org.l2jmobius.gameserver.entity.actor.holders.npc.FakePlayerHolder;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Manages the behavior and chat messages of fake players in the game world.<br>
 * It handles loading data from {@code FakePlayerData} and simulates social interaction for NPCs.
 * @author Mobius
 */
public class FakePlayerChatManager implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(FakePlayerChatManager.class.getName());
	
	private static final List<FakePlayerChatHolder> MESSAGES = new ArrayList<>();
	private static final int MIN_DELAY = 5000;
	private static final int MAX_DELAY = 15000;
	
	// Proactive random chat: every fake player says a random general or shout line.
	private static final List<String> RANDOM_GENERAL_MESSAGES = new ArrayList<>();
	private static final List<String> RANDOM_SHOUT_MESSAGES = new ArrayList<>();
	private static final int MIN_RANDOM_CHAT_DELAY = 120000;
	private static final int MAX_RANDOM_CHAT_DELAY = 240000;
	// A said line cannot be repeated by any fake player for 15 minutes.
	private static final Map<String, Long> RECENT_MESSAGES = new ConcurrentHashMap<>();
	private static final long MESSAGE_COOLDOWN = 900000;
	
	/**
	 * Initializes the {@code FakePlayerChatManager}.<br>
	 * This constructor calls the {@code load()} method to prepare the manager.
	 */
	protected FakePlayerChatManager()
	{
		load();
	}
	
	/**
	 * Loads the fake player chat data from the configuration files.<br>
	 * This method checks if fake players are enabled before processing.<br>
	 * It clears existing messages and parses the required XML file.
	 */
	@Override
	public void load()
	{
		if (FakePlayersConfig.FAKE_PLAYERS_ENABLED)
		{
			FakePlayerData.getInstance().report();
		}
		
		// Chat data loads whenever fake player chat is enabled, independent of the built-in fake player system.
		if (FakePlayersConfig.FAKE_PLAYER_CHAT)
		{
			MESSAGES.clear();
			RANDOM_GENERAL_MESSAGES.clear();
			RANDOM_SHOUT_MESSAGES.clear();
			parseDatapackFile("data/FakePlayerChatData.xml");
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + MESSAGES.size() + " chat templates and " + (RANDOM_GENERAL_MESSAGES.size() + RANDOM_SHOUT_MESSAGES.size()) + " random chat lines.");
		}
	}
	
	/**
	 * Parses the XML content from a file to populate the chat messages.<br>
	 * It reads all {@code fakePlayerChat} elements within the {@code list} node.<br>
	 * Each entry is added to the internal message collection.
	 * @param document The {@code Document} object containing the parsed XML data.
	 * @param file The {@code File} source of the configuration data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "fakePlayerChat", fakePlayerChatNode ->
		{
			final StatSet set = new StatSet(parseAttributes(fakePlayerChatNode));
			final String method = set.getString("method");
			final String text = set.getString("text");
			if ("RANDOM_GENERAL".equals(method))
			{
				addRandomLines(text, RANDOM_GENERAL_MESSAGES);
			}
			else if ("RANDOM_SHOUT".equals(method))
			{
				addRandomLines(text, RANDOM_SHOUT_MESSAGES);
			}
			else
			{
				// Reactive keyword answers.
				MESSAGES.add(new FakePlayerChatHolder(set.getString("fpcName"), method, text, set.getString("answers", "")));
			}
		}));
	}
	
	/**
	 * Adds semicolon separated lines to a random chat pool.
	 * @param text the raw text holding one or more lines separated by ';'
	 * @param pool the pool to add the lines to
	 */
	private static void addRandomLines(String text, List<String> pool)
	{
		for (String line : text.split(";"))
		{
			final String trimmed = line.trim();
			if (!trimmed.isEmpty())
			{
				pool.add(trimmed);
			}
		}
	}
	
	/**
	 * Schedules a delayed chat response from a fake player.<br>
	 * This method uses {@code ThreadPool} to send the message after a random delay.
	 * @param player The {@code Player} who will receive the message.
	 * @param fpcName The name of the fake player sending the message.
	 * @param message The content of the chat message.
	 */
	public void manageChat(Player player, String fpcName, String message)
	{
		ThreadPool.schedule(() -> manageResponce(player, fpcName, message), Rnd.get(MIN_DELAY, MAX_DELAY));
	}
	
	/**
	 * Schedules a chat response from a fake player.<br>
	 * The message will be sent after a random delay.
	 * @param player The {@code Player} who initiated the interaction.
	 * @param fpcName The name of the fake player responding.
	 * @param message The text content to be displayed in chat.
	 * @param minDelay The minimum time in milliseconds for the delay.
	 * @param maxDelay The maximum time in milliseconds for the delay.
	 */
	public void manageChat(Player player, String fpcName, String message, int minDelay, int maxDelay)
	{
		ThreadPool.schedule(() -> manageResponce(player, fpcName, message), Rnd.get(minDelay, maxDelay));
	}
	
	/**
	 * Starts the random chat loop for a fake player: it says a random line after a delay and reschedules itself.<br>
	 * Does nothing when fake player chat is disabled or no random lines are loaded.
	 * @param npc The fake player {@code Npc}.
	 */
	public void scheduleRandomChat(Npc npc)
	{
		if (!FakePlayersConfig.FAKE_PLAYER_CHAT || (RANDOM_GENERAL_MESSAGES.isEmpty() && RANDOM_SHOUT_MESSAGES.isEmpty()))
		{
			return;
		}
		
		// Shops sitting in a private store stay silent.
		final FakePlayerHolder holder = npc.getTemplate().getFakePlayerInfo();
		if ((holder != null) && (holder.getPrivateStoreType() > 0))
		{
			return;
		}
		
		ThreadPool.schedule(() -> sayRandom(npc), Rnd.get(MIN_RANDOM_CHAT_DELAY, MAX_RANDOM_CHAT_DELAY));
	}
	
	/**
	 * Makes the fake player say a random line to nearby players, then reschedules the next one.<br>
	 * The loop stops when the fake player is no longer spawned or is dead.
	 * @param npc The fake player {@code Npc}.
	 */
	private void sayRandom(Npc npc)
	{
		if ((npc == null) || !npc.isSpawned() || npc.isDead())
		{
			return;
		}
		
		final boolean shout = !RANDOM_SHOUT_MESSAGES.isEmpty() && (RANDOM_GENERAL_MESSAGES.isEmpty() || (Rnd.get(4) == 0));
		final List<String> pool = shout ? RANDOM_SHOUT_MESSAGES : RANDOM_GENERAL_MESSAGES;
		final String message = pickFresh(pool);
		if (message != null)
		{
			npc.broadcastPacket(new CreatureSay(npc, shout ? ChatType.SHOUT : ChatType.GENERAL, npc.getName(), message));
		}
		
		scheduleRandomChat(npc);
	}
	
	/**
	 * Picks a random line from the pool that has not been said in the last {@link #MESSAGE_COOLDOWN} milliseconds, and marks it as recently said.
	 * @param pool the message pool
	 * @return a fresh line, or {@code null} when the pool is empty
	 */
	private static String pickFresh(List<String> pool)
	{
		if (pool.isEmpty())
		{
			return null;
		}
		
		final long now = System.currentTimeMillis();
		String candidate = null;
		// A few random tries avoiding lines said within the cooldown window.
		for (int i = 0; i < 10; i++)
		{
			candidate = pool.get(Rnd.get(pool.size()));
			final Long until = RECENT_MESSAGES.get(candidate);
			if ((until == null) || (until.longValue() < now))
			{
				break;
			}
		}
		
		RECENT_MESSAGES.put(candidate, now + MESSAGE_COOLDOWN);
		return candidate;
	}
	
	/**
	 * Handles the logic for generating a response from a fake player.<br>
	 * It checks if the message contains specific keywords or matches configured patterns.<br>
	 * If no match is found, it may provide a default response based on proximity.
	 * @param player The {@code Player} who sent the message.
	 * @param fpcName The name of the fake player responding.
	 * @param message The text content of the received message.
	 */
	private void manageResponce(Player player, String fpcName, String message)
	{
		if (player == null)
		{
			return;
		}
		
		final String text = message.toLowerCase();
		
		// tricky question
		if (text.contains("can you see me"))
		{
			final Spawn spawn = SpawnTable.getInstance().getAnySpawn(FakePlayerData.getInstance().getNpcIdByName(fpcName));
			if (spawn != null)
			{
				final Npc npc = spawn.getLastSpawn();
				if (npc != null)
				{
					if (npc.calculateDistance2D(player) < 3000)
					{
						if (GeoEngine.getInstance().canSeeTarget(npc, player) && !player.isInvisible())
						{
							sendChat(player, fpcName, Rnd.nextBoolean() ? "i am not blind" : Rnd.nextBoolean() ? "of course i can" : "yes");
						}
						else
						{
							sendChat(player, fpcName, Rnd.nextBoolean() ? "i know you are around" : Rnd.nextBoolean() ? "not at the moment :P" : "no, where are you?");
						}
					}
					else
					{
						sendChat(player, fpcName, Rnd.nextBoolean() ? "nope, can't see you" : Rnd.nextBoolean() ? "nope" : "no");
					}
					return;
				}
			}
		}
		
		for (FakePlayerChatHolder chatHolder : MESSAGES)
		{
			final String fpcNameLocal = chatHolder.getFpcName();
			if (!fpcNameLocal.equals(fpcName) && !fpcNameLocal.equals("ALL"))
			{
				continue;
			}
			
			switch (chatHolder.getSearchMethod())
			{
				case "EQUALS":
				{
					if (text.equals(chatHolder.getSearchText().get(0)))
					{
						sendChat(player, fpcName, chatHolder.getAnswers().get(Rnd.get(chatHolder.getAnswers().size())));
					}
					break;
				}
				case "STARTS_WITH":
				{
					if (text.startsWith(chatHolder.getSearchText().get(0)))
					{
						sendChat(player, fpcName, chatHolder.getAnswers().get(Rnd.get(chatHolder.getAnswers().size())));
					}
					break;
				}
				case "CONTAINS":
				{
					boolean allFound = true;
					for (String word : chatHolder.getSearchText())
					{
						if (!text.contains(word))
						{
							allFound = false;
							break;
						}
					}
					
					if (allFound)
					{
						sendChat(player, fpcName, chatHolder.getAnswers().get(Rnd.get(chatHolder.getAnswers().size())));
					}
					break;
				}
			}
		}
	}
	
	/**
	 * Sends a whisper chat packet to the specified {@code Player}.<br>
	 * This method finds an {@code Npc} based on the provided {@code fpcName}.<br>
	 * It then sends a {@code CreatureSay} packet containing the message.
	 * @param player The {@code Player} who will receive the chat.
	 * @param fpcName The name of the fake player used for the sender name.
	 * @param message The text content to be sent in the chat.
	 */
	public void sendChat(Player player, String fpcName, String message)
	{
		final Spawn spawn = SpawnTable.getInstance().getAnySpawn(FakePlayerData.getInstance().getNpcIdByName(fpcName));
		if (spawn != null)
		{
			final Npc npc = spawn.getLastSpawn();
			if (npc != null)
			{
				player.sendPacket(new CreatureSay(npc, ChatType.WHISPER, fpcName, message));
			}
		}
	}
	
	/**
	 * Retrieves the singleton instance of {@code FakePlayerChatManager}.<br>
	 * This method provides a global access point to the manager.
	 * @return The active {@code FakePlayerChatManager} instance.
	 */
	public static FakePlayerChatManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final FakePlayerChatManager INSTANCE = new FakePlayerChatManager();
	}
}
