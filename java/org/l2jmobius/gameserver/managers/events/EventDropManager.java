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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.npc.EventDropHolder;
import org.l2jmobius.gameserver.mechanics.script.LongTimeEvent;

/**
 * Manages the logic for special loot drops during specific game events.<br>
 * It handles the distribution of items from {@code EventDropHolder} objects.<br>
 * This class ensures that event-specific rewards are correctly processed and awarded.
 * @author Mobius
 */
public class EventDropManager
{
	private static final Map<LongTimeEvent, List<EventDropHolder>> EVENT_DROPS = new ConcurrentHashMap<>(1);
	
	/**
	 * Registers a list of drops for a specific long-term event.<br>
	 * This method maps the {@code LongTimeEvent} to its corresponding {@code EventDropHolder} list.<br>
	 * Use this to define what items can drop during an active event.
	 * @param longTimeEvent The {@code LongTimeEvent} instance to associate with the drops.
	 * @param dropList The {@code List} of {@code EventDropHolder} objects to be added.
	 */
	public void addDrops(LongTimeEvent longTimeEvent, List<EventDropHolder> dropList)
	{
		EVENT_DROPS.put(longTimeEvent, dropList);
	}
	
	/**
	 * Removes the drop list associated with a specific event.<br>
	 * This method clears the data for the provided {@code LongTimeEvent}.
	 * @param longTimeEvent The {@code LongTimeEvent} to remove from the manager.
	 */
	public void removeDrops(LongTimeEvent longTimeEvent)
	{
		EVENT_DROPS.remove(longTimeEvent);
	}
	
	/**
	 * Handles the logic for dropping special event items.<br>
	 * This method checks if the {@code attacker} is a valid player and meets level requirements.<br>
	 * It then determines if an item should drop based on configured chances and monster IDs.
	 * @param attacker The {@code Creature} performing the action.
	 * @param attackable The {@code Attackable} target being interacted with.
	 */
	public void doEventDrop(Creature attacker, Attackable attackable)
	{
		if (EVENT_DROPS.isEmpty())
		{
			return;
		}
		
		// Event items drop only for players.
		if ((attacker == null) || !attacker.isPlayable() || attackable.isFakePlayer())
		{
			return;
		}
		
		// Event items drop only within a default 9 level difference.
		final Player player = attacker.asPlayer();
		final int attackableLevel = attackable.getLevel();
		if ((player.getLevel() - attackableLevel) > RatesConfig.EVENT_ITEM_MAX_LEVEL_DIFFERENCE)
		{
			return;
		}
		
		for (List<EventDropHolder> eventDrops : EVENT_DROPS.values())
		{
			DROPS: for (EventDropHolder drop : eventDrops)
			{
				if (!drop.getMonsterIds().isEmpty() && !drop.getMonsterIds().contains(attackable.getId()))
				{
					continue DROPS;
				}
				
				final int monsterLevel = attackableLevel;
				if ((monsterLevel >= drop.getMinLevel()) && (monsterLevel <= drop.getMaxLevel()) && (Rnd.get(100d) < drop.getChance()))
				{
					final int itemId = drop.getItemId();
					final long itemCount = Rnd.get(drop.getMin(), drop.getMax());
					if (PlayerConfig.AUTO_LOOT_ITEM_IDS.contains(itemId) || PlayerConfig.AUTO_LOOT || attackable.isFlying())
					{
						player.doAutoLoot(attackable, itemId, itemCount); // Give the item to the player that has killed the attackable.
					}
					else
					{
						attackable.dropItem(player, itemId, itemCount); // Drop the item on the ground.
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves the singleton instance of the {@code EventDropManager}.<br>
	 * Use this method to access the manager from anywhere in the code.
	 * @return The global {@code EventDropManager} instance.
	 */
	public static EventDropManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EventDropManager INSTANCE = new EventDropManager();
	}
}
