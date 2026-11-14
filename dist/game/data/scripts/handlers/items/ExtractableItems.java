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
package handlers.items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.request.AutoPeelRequest;
import org.l2jmobius.gameserver.entity.item.EtcItem;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.enums.SpecialItemType;
import org.l2jmobius.gameserver.entity.item.holders.ExtractableProduct;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.managers.DailyResetManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExPCCafePointInfo;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.autopeel.ExResultItemAutoPeel;
import org.l2jmobius.gameserver.network.serverpackets.autopeel.ExStopItemAutoPeel;

/**
 * This class handles the logic for extracting items from other objects.<br>
 * It manages how {@code Item} instances are processed to produce {@code ExtractableProduct} results.
 * @author HorridoJoho, Mobius
 */
public class ExtractableItems implements IItemHandler
{
	private static final int MAX_EXTRACTION_ATTEMPTS = 100;
	
	/**
	 * Handles the logic for extracting items from an {@code EtcItem}.<br>
	 * It checks if the user is a {@code Player} and has enough inventory space.<br>
	 * The original item is destroyed and replaced with new items based on defined chances.
	 * @param playable The entity attempting to use the item.
	 * @param item The specific item instance being used.
	 * @param forceUse Whether to force the usage of the item.
	 * @return {@code true} if the extraction was successful, {@code false} otherwise.
	 */
	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_TRANSFERRED_TO_A_PET);
			return false;
		}
		
		final EtcItem etcitem = (EtcItem) item.getTemplate();
		final List<ExtractableProduct> exitems = etcitem.getExtractableItems();
		if (exitems == null)
		{
			LOGGER.info("No extractable data defined for " + etcitem);
			return false;
		}
		
		final Player player = playable.asPlayer();
		if (!player.isInventoryUnder80(false))
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_ITEM_OWNERSHIP_LIMIT_AND_YOU_CANNOT_TAKE_THE_ITEM_CHECK_ITEM_OWNERSHIP_TIME_LIMITS_FOR_THE_INVENTORY_PLEASE);
			return false;
		}
		
		// destroy item
		if (!DailyResetManager.RESET_ITEMS.contains(item.getId()) && !player.destroyItem(ItemProcessType.FEE, item.getObjectId(), 1, player, true))
		{
			return false;
		}
		
		boolean specialReward = false;
		final Map<Item, Long> extractedItems = new HashMap<>();
		final List<Item> enchantedItems = new ArrayList<>();
		final int extractableCountMax = etcitem.getExtractableCountMax();
		if (etcitem.getExtractableCountMin() > 0)
		{
			int attempts = 0;
			while ((extractedItems.size() < etcitem.getExtractableCountMin()) && (++attempts <= MAX_EXTRACTION_ATTEMPTS))
			{
				int roll = Rnd.get(100000);
				int total = 0;
				for (ExtractableProduct expi : exitems)
				{
					if ((extractableCountMax > 0) && (extractedItems.size() == extractableCountMax))
					{
						break;
					}
					
					total += expi.getChance();
					if (total <= roll)
					{
						continue;
					}
					
					final long min = (long) (expi.getMin() * RatesConfig.RATE_EXTRACTABLE);
					final long max = (long) (expi.getMax() * RatesConfig.RATE_EXTRACTABLE);
					long createItemAmount = (max == min) ? min : (Rnd.get((max - min) + 1) + min);
					if (createItemAmount == 0)
					{
						continue;
					}
					
					// Do not extract the same item.
					boolean alreadyExtracted = false;
					for (Item i : extractedItems.keySet())
					{
						if (i.getTemplate().getId() == expi.getId())
						{
							alreadyExtracted = true;
							break;
						}
					}
					
					if (alreadyExtracted && (exitems.size() >= extractableCountMax))
					{
						continue;
					}
					
					if (expi.getId() == -1) // Prime points
					{
						player.setPrimePoints(player.getPrimePoints() + (int) createItemAmount);
						player.sendMessage("You have obtained " + (createItemAmount / 100) + " Euro!");
						specialReward = true;
						
						// Reset the accumulated chance for the remaining products.
						total = 0;
						roll = Rnd.get(100000);
						continue;
					}
					else if (expi.getId() == SpecialItemType.PC_CAFE_POINTS.getClientId())
					{
						final int currentPoints = player.getPcCafePoints();
						final int upgradePoints = player.getPcCafePoints() + (int) createItemAmount;
						player.setPcCafePoints(upgradePoints);
						final SystemMessage message = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_PA_POINTS_X_S1).addInt((int) createItemAmount);
						player.sendPacket(message);
						player.sendPacket(new ExPCCafePointInfo(currentPoints, upgradePoints, 1));
						specialReward = true;
						
						// Reset the accumulated chance for the remaining products.
						total = 0;
						roll = Rnd.get(100000);
						continue;
					}
					else if (expi.getId() == SpecialItemType.HONOR_COINS.getClientId())
					{
						player.setHonorCoins(player.getHonorCoins() + (int) createItemAmount);
						player.sendMessage("You have obtained " + (createItemAmount) + " Honor Coin.");
						specialReward = true;
						
						// Reset the accumulated chance for the remaining products.
						total = 0;
						roll = Rnd.get(100000);
						continue;
					}
					
					final ItemTemplate template = ItemData.getInstance().getTemplate(expi.getId());
					if (template == null)
					{
						LOGGER.warning("ExtractableItems: Could not find " + item + " product template with id " + expi.getId() + "!");
						continue;
					}
					
					if (template.isStackable() || (createItemAmount == 1))
					{
						final Item newItem = player.addItem(ItemProcessType.REWARD, expi.getId(), createItemAmount, player, false);
						if (expi.getMaxEnchant() > 0)
						{
							newItem.setEnchantLevel(Rnd.get(expi.getMinEnchant(), expi.getMaxEnchant()));
							enchantedItems.add(newItem);
						}
						
						addItem(extractedItems, newItem, createItemAmount);
					}
					else
					{
						while (createItemAmount > 0)
						{
							final Item newItem = player.addItem(ItemProcessType.REWARD, expi.getId(), 1, player, false);
							if (expi.getMaxEnchant() > 0)
							{
								newItem.setEnchantLevel(Rnd.get(expi.getMinEnchant(), expi.getMaxEnchant()));
								enchantedItems.add(newItem);
							}
							
							addItem(extractedItems, newItem, 1);
							createItemAmount--;
						}
					}
					
					// Reset the accumulated chance for the remaining products.
					total = 0;
					roll = Rnd.get(100000);
				}
			}
		}
		else
		{
			for (ExtractableProduct expi : exitems)
			{
				if ((extractableCountMax > 0) && (extractedItems.size() == extractableCountMax))
				{
					break;
				}
				
				if (Rnd.get(100000) <= expi.getChance())
				{
					final long min = (long) (expi.getMin() * RatesConfig.RATE_EXTRACTABLE);
					final long max = (long) (expi.getMax() * RatesConfig.RATE_EXTRACTABLE);
					long createItemAmount = (max == min) ? min : (Rnd.get((max - min) + 1) + min);
					if (createItemAmount == 0)
					{
						continue;
					}
					
					if (expi.getId() == -1) // Prime points
					{
						player.setPrimePoints(player.getPrimePoints() + (int) createItemAmount);
						player.sendMessage("You have obtained " + (createItemAmount / 100) + " Euro!");
						specialReward = true;
						continue;
					}
					else if (expi.getId() == SpecialItemType.PC_CAFE_POINTS.getClientId())
					{
						final int currentPoints = player.getPcCafePoints();
						final int upgradePoints = player.getPcCafePoints() + (int) createItemAmount;
						player.setPcCafePoints(upgradePoints);
						final SystemMessage message = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_PA_POINTS_X_S1).addInt((int) createItemAmount);
						player.sendPacket(message);
						player.sendPacket(new ExPCCafePointInfo(currentPoints, upgradePoints, 1));
						specialReward = true;
						continue;
					}
					else if (expi.getId() == SpecialItemType.HONOR_COINS.getClientId())
					{
						player.setHonorCoins(player.getHonorCoins() + (int) createItemAmount);
						player.sendMessage("You have obtained " + (createItemAmount) + " Honor Coin.");
						specialReward = true;
						continue;
					}
					
					final ItemTemplate template = ItemData.getInstance().getTemplate(expi.getId());
					if (template == null)
					{
						LOGGER.warning("ExtractableItems: Could not find " + item + " product template with id " + expi.getId() + "!");
						continue;
					}
					
					if (template.isStackable() || (createItemAmount == 1))
					{
						final Item newItem = player.addItem(ItemProcessType.REWARD, expi.getId(), createItemAmount, player, false);
						if (expi.getMaxEnchant() > 0)
						{
							newItem.setEnchantLevel(Rnd.get(expi.getMinEnchant(), expi.getMaxEnchant()));
							enchantedItems.add(newItem);
						}
						
						addItem(extractedItems, newItem, createItemAmount);
					}
					else
					{
						while (createItemAmount > 0)
						{
							final Item newItem = player.addItem(ItemProcessType.REWARD, expi.getId(), 1, player, false);
							if (expi.getMaxEnchant() > 0)
							{
								newItem.setEnchantLevel(Rnd.get(expi.getMinEnchant(), expi.getMaxEnchant()));
								enchantedItems.add(newItem);
							}
							
							addItem(extractedItems, newItem, 1);
							createItemAmount--;
						}
					}
				}
			}
		}
		
		if (extractedItems.isEmpty() && !specialReward)
		{
			player.sendPacket(SystemMessageId.FAILED_TO_CHANGE_THE_ITEM);
		}
		
		if (!enchantedItems.isEmpty())
		{
			final InventoryUpdate playerIU = new InventoryUpdate();
			for (Item i : enchantedItems)
			{
				playerIU.addModifiedItem(i);
			}
			
			player.sendInventoryUpdate(playerIU);
		}
		
		for (Entry<Item, Long> entry : extractedItems.entrySet())
		{
			sendMessage(player, entry.getKey(), entry.getValue());
		}
		
		final AutoPeelRequest request = player.getRequest(AutoPeelRequest.class);
		if (request != null)
		{
			if (request.isProcessing())
			{
				request.setProcessing(false);
				final List<ItemHolder> rewards = new ArrayList<>();
				for (Entry<Item, Long> entry : extractedItems.entrySet())
				{
					rewards.add(new ItemHolder(entry.getKey().getId(), entry.getValue()));
				}
				
				player.sendPacket(new ExResultItemAutoPeel(true, request.getTotalPeelCount(), request.getRemainingPeelCount() - 1, rewards));
			}
			else
			{
				player.sendPacket(new ExStopItemAutoPeel(false));
			}
		}
		else // Fixes last auto peel not shown. No effect if auto peel window is closed.
		{
			final List<ItemHolder> rewards = new ArrayList<>();
			for (Entry<Item, Long> entry : extractedItems.entrySet())
			{
				rewards.add(new ItemHolder(entry.getKey().getId(), entry.getValue()));
			}
			
			player.sendPacket(new ExResultItemAutoPeel(true, 0, 0, rewards));
		}
		
		return true;
	}
	
	/**
	 * Adds a new item to the collection of extracted items.<br>
	 * It updates the total count if the item already exists in the {@code Map}.
	 * @param extractedItems The map containing current items and their counts.
	 * @param newItem The {@code Item} to be added or updated.
	 * @param count The amount of the {@code newItem} to add.
	 */
	private void addItem(Map<Item, Long> extractedItems, Item newItem, long count)
	{
		extractedItems.merge(newItem, count, Long::sum);
	}
	
	/**
	 * Sends a notification message to the player about an obtained item.<br>
	 * The message content changes based on the item count and enchantment status.
	 * @param player The {@code Player} who will receive the message.
	 * @param item The {@code Item} that was obtained.
	 * @param count The number of items received.
	 */
	private void sendMessage(Player player, Item item, long count)
	{
		final SystemMessage sm;
		if (count > 1)
		{
			sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1_X_S2);
			sm.addItemName(item);
			sm.addLong(count);
		}
		else if (item.isEnchanted())
		{
			sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1_S2);
			sm.addInt(item.getEnchantLevel());
			sm.addItemName(item);
		}
		else
		{
			sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1);
			sm.addItemName(item);
		}
		
		player.sendPacket(sm);
	}
}
