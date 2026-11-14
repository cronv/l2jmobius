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
package org.l2jmobius.gameserver.taskmanagers;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.custom.AutoPotionsConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;

/**
 * Manages the background execution of automatic potion usage for players.<br>
 * It periodically checks player health and mana to trigger item consumption based on {@code AutoPotionsConfig}.
 * @author Mobius, Gigi
 */
public class AutoPotionTaskManager implements Runnable
{
	private static final Set<Player> PLAYERS = ConcurrentHashMap.newKeySet();
	private static final AtomicBoolean WORKING = new AtomicBoolean();
	
	/**
	 * Initializes a new instance of the {@code AutoPotionTaskManager}.<br>
	 * This constructor schedules the task to run every {@code 1000} milliseconds.<br>
	 * It sets up the background thread for managing player potions.
	 */
	protected AutoPotionTaskManager()
	{
		ThreadPool.schedulePriorityTaskAtFixedRate(this, 0, 1000);
	}
	
	@Override
	public void run()
	{
		if (!WORKING.compareAndSet(false, true))
		{
			return;
		}
		
		try
		{
			if (!PLAYERS.isEmpty())
			{
				PLAYER: for (Player player : PLAYERS)
				{
					if ((player == null) || player.isAlikeDead() || (player.isOnlineInt() == 0) || (!AutoPotionsConfig.AUTO_POTIONS_IN_OLYMPIAD && player.isInOlympiadMode()))
					{
						remove(player);
						continue PLAYER;
					}
					
					boolean success = false;
					if (AutoPotionsConfig.AUTO_HP_ENABLED)
					{
						final boolean restoreHP = ((player.getStatus().getCurrentHp() / player.getMaxHp()) * 100) < AutoPotionsConfig.AUTO_HP_PERCENTAGE;
						final boolean hpRecoveryActive = player.hasAbnormalType(AbnormalType.HP_RECOVER);
						HP: for (int itemId : AutoPotionsConfig.AUTO_HP_ITEM_IDS)
						{
							final Item hpPotion = player.getInventory().getItemByItemId(itemId);
							if ((hpPotion != null) && (hpPotion.getCount() > 0))
							{
								success = true;
								// Do not consume another HP potion while a healing-over-time potion is still active.
								if (restoreHP && !hpRecoveryActive)
								{
									final IItemHandler handler = ItemHandler.getInstance().getHandler(hpPotion.getEtcItem());
									if ((handler != null) && handler.onItemUse(player, hpPotion, false))
									{
										player.sendMessage("Auto potion: Restored HP.");
									}
									break HP;
								}
							}
						}
					}
					
					if (AutoPotionsConfig.AUTO_CP_ENABLED)
					{
						final boolean restoreCP = ((player.getStatus().getCurrentCp() / player.getMaxCp()) * 100) < AutoPotionsConfig.AUTO_CP_PERCENTAGE;
						CP: for (int itemId : AutoPotionsConfig.AUTO_CP_ITEM_IDS)
						{
							final Item cpPotion = player.getInventory().getItemByItemId(itemId);
							if ((cpPotion != null) && (cpPotion.getCount() > 0))
							{
								success = true;
								if (restoreCP)
								{
									ItemHandler.getInstance().getHandler(cpPotion.getEtcItem()).onItemUse(player, cpPotion, false);
									player.sendMessage("Auto potion: Restored CP.");
									break CP;
								}
							}
						}
					}
					
					if (AutoPotionsConfig.AUTO_MP_ENABLED)
					{
						final boolean restoreMP = ((player.getStatus().getCurrentMp() / player.getMaxMp()) * 100) < AutoPotionsConfig.AUTO_MP_PERCENTAGE;
						MP: for (int itemId : AutoPotionsConfig.AUTO_MP_ITEM_IDS)
						{
							final Item mpPotion = player.getInventory().getItemByItemId(itemId);
							if ((mpPotion != null) && (mpPotion.getCount() > 0))
							{
								success = true;
								if (restoreMP)
								{
									ItemHandler.getInstance().getHandler(mpPotion.getEtcItem()).onItemUse(player, mpPotion, false);
									player.sendMessage("Auto potion: Restored MP.");
									break MP;
								}
							}
						}
					}
					
					if (!success)
					{
						player.sendMessage("Auto potion: You are out of potions!");
					}
				}
			}
		}
		finally
		{
			WORKING.set(false);
		}
	}
	
	/**
	 * Adds a {@code Player} to the task manager.<br>
	 * This method ensures the player is only added if they are not already present.
	 * @param player The {@code Player} object to add.
	 */
	public void add(Player player)
	{
		if (!PLAYERS.contains(player))
		{
			PLAYERS.add(player);
		}
	}
	
	/**
	 * Removes a specific {@code Player} from the active task list.<br>
	 * This stops the auto-potion logic for that player.
	 * @param player The {@code Player} to be removed.
	 */
	public void remove(Player player)
	{
		PLAYERS.remove(player);
	}
	
	/**
	 * Checks if a specific {@code Player} is currently being managed.<br>
	 * This method verifies the presence of the player in the internal set.
	 * @param player The {@code Player} object to check.
	 * @return {@code true} if the player exists, {@code false} otherwise.
	 */
	public boolean hasPlayer(Player player)
	{
		return PLAYERS.contains(player);
	}
	
	/**
	 * Retrieves the singleton instance of the {@code AutoPotionTaskManager}.<br>
	 * This method provides a global access point to the manager.
	 * @return The active {@code AutoPotionTaskManager} instance.
	 */
	public static AutoPotionTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AutoPotionTaskManager INSTANCE = new AutoPotionTaskManager();
	}
}
