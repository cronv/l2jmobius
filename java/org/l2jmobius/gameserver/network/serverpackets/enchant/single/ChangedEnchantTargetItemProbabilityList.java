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
package org.l2jmobius.gameserver.network.serverpackets.enchant.single;

import static org.l2jmobius.gameserver.mechanics.stats.Stat.ENCHANT_RATE;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.xml.EnchantItemData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.request.EnchantItemRequest;
import org.l2jmobius.gameserver.entity.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.entity.item.enchant.EnchantScroll;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.CrystalType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet informs the client about changes to the target item probabilities for enchanting.<br>
 * It is used to update the visual or functional list of items that can be affected by an enchantment.
 * @author Index
 */
public class ChangedEnchantTargetItemProbabilityList extends ServerPacket
{
	private final Player _player;
	private final boolean _isMulti;
	
	/**
	 * Creates a new packet to update the enchantment target item probabilities.<br>
	 * This constructor initializes the data for both single and multi-item enchantments.
	 * @param player The {@code Player} who triggered the action.
	 * @param isMulti A {@code Boolean} indicating if the request involves multiple items.
	 */
	public ChangedEnchantTargetItemProbabilityList(Player player, Boolean isMulti)
	{
		_player = player;
		_isMulti = isMulti;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		final EnchantItemRequest request = _player.getRequest(EnchantItemRequest.class);
		if (request == null)
		{
			return;
		}
		
		if ((!_isMulti && (request.getEnchantingItem() == null)) || request.isProcessing() || (request.getEnchantingScroll() == null))
		{
			return;
		}
		
		int count = 1;
		if (_isMulti)
		{
			count = request.getMultiEnchantingItemsCount();
		}
		
		ServerPackets.EX_CHANGED_ENCHANT_TARGET_ITEM_PROB_LIST.writeId(this, buffer);
		buffer.writeInt(count);
		for (int i = 1; i <= count; i++)
		{
			// 100,00 % = 10000, because last 2 numbers going after float comma.
			double baseRate;
			double passiveRate;
			if (!_isMulti || (request.getMultiEnchantingItemsBySlot(i) != 0))
			{
				baseRate = getBaseRate(request, i);
				passiveRate = getPassiveRate(request, i);
			}
			else
			{
				baseRate = 0;
				passiveRate = 0;
			}
			
			double passiveBaseRate = 0;
			final double supportRate = getSupportRate(request);
			if (passiveRate != 0)
			{
				passiveBaseRate = (baseRate * passiveRate) / 10000;
			}
			
			double totalRate = baseRate + supportRate + passiveBaseRate;
			if (totalRate >= 10000)
			{
				totalRate = 10000;
			}
			
			if (!_isMulti)
			{
				buffer.writeInt(request.getEnchantingItem().getObjectId());
			}
			else
			{
				buffer.writeInt(request.getMultiEnchantingItemsBySlot(i));
			}
			
			buffer.writeInt((int) totalRate); // Total success.
			buffer.writeInt((int) baseRate); // Base success.
			buffer.writeInt((int) supportRate); // Support success.
			buffer.writeInt((int) passiveBaseRate); // Passive success (items, skills).
			buffer.writeInt(0); // VariationProbPermyriad (493)
		}
	}
	
	/**
	 * Calculates the base success rate for an enchantment.<br>
	 * This method retrieves data from {@code EnchantItemData}.<br>
	 * It returns a value scaled by {@code 100}.
	 * @param request The {@code EnchantItemRequest} containing item details.
	 * @param iteration The current index used for multi-enchanting items.
	 * @return The calculated base rate as an {@code int}.
	 */
	private int getBaseRate(EnchantItemRequest request, int iteration)
	{
		final EnchantScroll enchantScroll = EnchantItemData.getInstance().getEnchantScroll(request.getEnchantingScroll());
		if (enchantScroll == null)
		{
			return 0;
		}
		
		return (int) Math.min(100, enchantScroll.getChance(_player, _isMulti ? _player.getInventory().getItemByObjectId(request.getMultiEnchantingItemsBySlot(iteration)) : request.getEnchantingItem()) + enchantScroll.getBonusRate()) * 100;
	}
	
	/**
	 * Calculates the bonus rate provided by a support item.<br>
	 * This value is only applied if the request is not for multiple items.<br>
	 * The result is converted from a percentage to an integer.
	 * @param request The {@code EnchantItemRequest} containing the support item information.
	 * @return The calculated support rate as an {@code int}.
	 */
	private int getSupportRate(EnchantItemRequest request)
	{
		double supportRate = 0;
		if (!_isMulti && (request.getSupportItem() != null))
		{
			supportRate = EnchantItemData.getInstance().getSupportItem(request.getSupportItem()).getBonusRate();
			supportRate = supportRate * 100;
		}
		
		return (int) supportRate;
	}
	
	/**
	 * Calculates the passive enchantment rate for a specific item.<br>
	 * This method checks the player's {@code ENCHANT_RATE} stat.<br>
	 * It returns 0 if the crystal type is {@code NONE} or {@code EVENT}.<br>
	 * Otherwise, it returns the stat value multiplied by 100.
	 * @param request The {@code EnchantItemRequest} containing item details.
	 * @param iteration The index used for multi-enchanting items.
	 * @return The calculated passive rate as an {@code int}.
	 */
	private int getPassiveRate(EnchantItemRequest request, int iteration)
	{
		double passiveRate = 0;
		final PlayerStat stat = _player.getStat();
		if (stat.getValue(ENCHANT_RATE) != 0)
		{
			if (!_isMulti)
			{
				final int crystalLevel = request.getEnchantingItem().getTemplate().getCrystalType().getLevel();
				if ((crystalLevel == CrystalType.NONE.getLevel()) || (crystalLevel == CrystalType.EVENT.getLevel()))
				{
					passiveRate = 0;
				}
				else
				{
					passiveRate = stat.getValue(ENCHANT_RATE);
					passiveRate = passiveRate * 100;
				}
			}
			else
			{
				final Item item = _player.getInventory().getItemByObjectId(request.getMultiEnchantingItemsBySlot(iteration));
				if (item == null)
				{
					return 0;
				}
				
				final int crystalLevel = item.getTemplate().getCrystalType().getLevel();
				if ((crystalLevel == CrystalType.NONE.getLevel()) || (crystalLevel == CrystalType.EVENT.getLevel()))
				{
					passiveRate = 0;
				}
				else
				{
					passiveRate = stat.getValue(ENCHANT_RATE);
					passiveRate = passiveRate * 100;
				}
			}
		}
		
		return (int) passiveRate;
	}
}
