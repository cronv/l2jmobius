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
package events.LetterCollector;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.managers.events.LetterCollectorManager;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.mechanics.script.LongTimeEvent;
import org.l2jmobius.gameserver.network.serverpackets.ExLetterCollectorUI;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Manages the logic for the {@code LetterCollector} event.<br>
 * This class handles player interactions and data loading for collecting letters.<br>
 * It extends {@code LongTimeEvent} to manage the lifecycle of the event.
 * @author Mobius
 */
public class LetterCollector extends LongTimeEvent implements IXmlReader
{
	/**
	 * Private constructor for the {@code LetterCollector} class.<br>
	 * This constructor initializes the event by registering the required NPCs and talk IDs.
	 */
	private LetterCollector()
	{
		if (isEventPeriod())
		{
			load();
			LetterCollectorManager.getInstance().init();
		}
	}
	
	/**
	 * Refreshes the reward data from the configuration files.<br>
	 * This method resets the current fields and calls {@code load()}.
	 */
	public void reloadRewards()
	{
		LetterCollectorManager.getInstance().resetField();
		load();
	}
	
	/**
	 * Loads the reward data from the configuration file.<br>
	 * This method reads the {@code rewards.xml} datapack.<br>
	 * It ensures that only one thread can perform this action at a time.
	 */
	@Override
	public synchronized void load()
	{
		parseDatapackFile("data/scripts/events/LetterCollector/rewards.xml");
	}
	
	/**
	 * Parses the configuration data from an XML document.<br>
	 * This method populates the {@code LetterCollectorManager} with rewards, letters, and level requirements.
	 * @param document The {@code Document} object containing the parsed XML content.
	 * @param file The {@code File} object representing the source of the configuration.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		final AtomicInteger minimumLevel = new AtomicInteger();
		final AtomicInteger maximumLevel = new AtomicInteger();
		final Map<String, Integer> letters = new HashMap<>();
		forEach(document, "list", listNode ->
		{
			forEach(listNode, "params", paramNode ->
			{
				forEach(paramNode, "minimum", minimumLevelNode -> minimumLevel.set(new StatSet(parseAttributes(minimumLevelNode)).getInt("level")));
				forEach(paramNode, "maximum", maximumLevelNode -> maximumLevel.set(new StatSet(parseAttributes(maximumLevelNode)).getInt("level")));
			});
			forEach(listNode, "letters", letterNode -> forEach(letterNode, "item", itemNode ->
			{
				StatSet letterSet = new StatSet(parseAttributes(itemNode));
				letters.put(letterSet.getString("name"), letterSet.getInt("id"));
			}));
			forEach(listNode, "reward", rewardNode ->
			{
				final int id = new StatSet(parseAttributes(rewardNode)).getInt("id");
				final List<ItemHolder> word = new ArrayList<>();
				final List<ItemChanceHolder> rewards = new ArrayList<>();
				final AtomicBoolean needToSumAllChance = new AtomicBoolean(false);
				final AtomicReference<Double> chanceSum = new AtomicReference<>(0d);
				forEach(rewardNode, "word", wordNode ->
				{
					String[] letter = wordNode.getTextContent().trim().split(";");
					for (String token : letter)
					{
						int count = 1;
						final int tokenId = letters.get(token);
						for (ItemHolder check : word)
						{
							if (check.getId() == tokenId)
							{
								count = Math.toIntExact(check.getCount() + 1);
								word.remove(check);
								break;
							}
						}
						
						word.add(new ItemHolder(tokenId, count));
					}
				});
				forEach(rewardNode, "rewards", rewardsNode ->
				{
					needToSumAllChance.set(new StatSet(parseAttributes(rewardsNode)).getBoolean("sumChances"));
					forEach(rewardsNode, "item", itemNode ->
					{
						StatSet itemSet = new StatSet(parseAttributes(itemNode));
						final double chance = itemSet.getDouble("chance");
						if (needToSumAllChance.get())
						{
							chanceSum.set(chanceSum.get() + chance);
						}
						
						rewards.add(new ItemChanceHolder(itemSet.getInt("id"), chance, itemSet.getLong("count"), (byte) itemSet.getInt("enchantLevel", 0)));
					});
				});
				final LetterCollectorManager lcm = LetterCollectorManager.getInstance();
				lcm.addWords(id, word);
				final double currentChanceSum = chanceSum.get();
				lcm.addRewards(id, new LetterCollectorManager.LetterCollectorRewardHolder(rewards, currentChanceSum == 0d ? 100d : currentChanceSum));
				lcm.setLetters(letters);
				lcm.setMinLevel(minimumLevel.get());
				lcm.setMaxLevel(maximumLevel.get());
			});
		});
	}
	
	/**
	 * Handles the logic when a player logs into the game.<br>
	 * This method checks if the event is currently active.<br>
	 * If active, it sends the {@code ExLetterCollectorUI} packet to the player.
	 * @param event The {@code OnPlayerLogin} event containing the player data.
	 */
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogin(OnPlayerLogin event)
	{
		if (!isEventPeriod())
		{
			return;
		}
		
		final Player player = event.getPlayer();
		if (player != null)
		{
			player.sendPacket(new ExLetterCollectorUI(player));
		}
	}
	
	/**
	 * This is the entry point for the {@code LetterCollector} class.<br>
	 * It initializes a new instance of the event handler.
	 * @param args The command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new LetterCollector();
	}
}
