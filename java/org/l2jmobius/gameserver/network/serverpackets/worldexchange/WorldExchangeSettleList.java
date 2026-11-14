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
package org.l2jmobius.gameserver.network.serverpackets.worldexchange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.WorldExchangeConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.entity.item.enchant.attribute.AttributeHolder;
import org.l2jmobius.gameserver.entity.item.enums.WorldExchangeItemStatusType;
import org.l2jmobius.gameserver.entity.item.holders.WorldExchangeHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.WorldExchangeManager;
import org.l2jmobius.gameserver.mechanics.ensoul.EnsoulOption;
import org.l2jmobius.gameserver.mechanics.options.AugmentationInstance;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet is sent to the client to provide a list of items that have been settled in the {@code WorldExchange}.<br>
 * It contains the final details for each item processed during the exchange transaction.
 * @author Index, Mobius
 */
public class WorldExchangeSettleList extends ServerPacket
{
	private final Player _player;
	private final int _weType;
	
	/**
	 * Creates a new {@code WorldExchangeSettleList} packet.<br>
	 * This packet is sent to the client to settle world exchange items.
	 * @param player The {@code Player} who initiated the action.
	 * @param weType The type of world exchange being processed.
	 */
	public WorldExchangeSettleList(Player player, int weType)
	{
		_player = player;
		_weType = weType;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_WORLD_EXCHANGE_SETTLE_LIST.writeId(this, buffer);
		
		buffer.writeByte(_weType); // 557
		
		final Map<WorldExchangeItemStatusType, List<WorldExchangeHolder>> holders = WorldExchangeManager.getInstance().getPlayerBids(_player.getObjectId());
		if (holders.isEmpty())
		{
			buffer.writeInt(0); // RegiItemDataList
			buffer.writeInt(0); // RecvItemDataList
			buffer.writeInt(0); // TimeOutItemDataList
			buffer.writeInt(0); // 563 WaitItemDataList
			buffer.writeInt(0); // 563 PetToolTipDataList
			return;
		}
		
		// Freshly registered items sit in a short wait period before the client shows them as counting down.
		final long waitThreshold = System.currentTimeMillis() - (WorldExchangeConfig.WORLD_EXCHANGE_REGISTRATION_WAIT_TIME * 1000L);
		final List<WorldExchangeHolder> activeHolders = new ArrayList<>();
		final List<WorldExchangeHolder> waitHolders = new ArrayList<>();
		for (WorldExchangeHolder holder : holders.get(WorldExchangeItemStatusType.WORLD_EXCHANGE_REGISTERED))
		{
			if (holder.getStartTime() > waitThreshold)
			{
				waitHolders.add(holder);
			}
			else
			{
				activeHolders.add(holder);
			}
		}
		
		buffer.writeInt(activeHolders.size());
		for (WorldExchangeHolder holder : activeHolders)
		{
			getItemInfo(buffer, holder);
		}
		
		final List<WorldExchangeHolder> soldHolders = holders.get(WorldExchangeItemStatusType.WORLD_EXCHANGE_SOLD);
		buffer.writeInt(soldHolders.size());
		for (WorldExchangeHolder holder : soldHolders)
		{
			getItemInfo(buffer, holder);
		}
		
		final List<WorldExchangeHolder> outTimeHolders = holders.get(WorldExchangeItemStatusType.WORLD_EXCHANGE_OUT_TIME);
		buffer.writeInt(outTimeHolders.size());
		for (WorldExchangeHolder holder : outTimeHolders)
		{
			getItemInfo(buffer, holder);
		}
		
		buffer.writeInt(waitHolders.size());
		for (WorldExchangeHolder holder : waitHolders)
		{
			getItemInfo(buffer, holder);
		}
		
		buffer.writeInt(0); // 563 PetToolTipDataList
	}
	
	/**
	 * Writes the details of a specific item to the provided buffer.<br>
	 * This method extracts data from the {@code WorldExchangeHolder} and populates it into the {@code WriteBuffer}.<br>
	 * It includes information such as IDs, counts, enchantments, attributes, and soul options.
	 * @param buffer The {@code WriteBuffer} used to store the serialized item data.
	 * @param holder The {@code WorldExchangeHolder} containing the source item and exchange details.
	 */
	private void getItemInfo(WriteBuffer buffer, WorldExchangeHolder holder)
	{
		buffer.writeLong(holder.getWorldExchangeId());
		buffer.writeLong(holder.getPrice());
		buffer.writeInt((int) (holder.getEndTime() / 1000L));
		
		final Item item = holder.getItemInstance();
		buffer.writeInt(item.getId());
		buffer.writeLong(item.getCount());
		buffer.writeInt(item.getEnchantLevel() < 1 ? 0 : item.getEnchantLevel());
		
		final AugmentationInstance augment = item.getAugmentation();
		if (augment != null)
		{
			buffer.writeInt(augment.getOption1Id());
			buffer.writeInt(augment.getOption2Id());
			buffer.writeInt(augment.getOption3Id());
		}
		else
		{
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
		}
		
		buffer.writeInt(-1); // IntensiveItemClassID
		final AttributeHolder attackAttribute = item.getAttackAttribute();
		buffer.writeShort(attackAttribute != null ? attackAttribute.getType().getClientId() : 0);
		buffer.writeShort(attackAttribute != null ? attackAttribute.getValue() : 0);
		buffer.writeShort(item.getDefenceAttribute(AttributeType.FIRE));
		buffer.writeShort(item.getDefenceAttribute(AttributeType.WATER));
		buffer.writeShort(item.getDefenceAttribute(AttributeType.WIND));
		buffer.writeShort(item.getDefenceAttribute(AttributeType.EARTH));
		buffer.writeShort(item.getDefenceAttribute(AttributeType.HOLY));
		buffer.writeShort(item.getDefenceAttribute(AttributeType.DARK));
		buffer.writeInt(item.getVisualId());
		
		final List<EnsoulOption> soul = (List<EnsoulOption>) holder.getItemInfo().getSoulCrystalOptions();
		try
		{
			buffer.writeInt(soul.get(0).getId());
		}
		catch (IndexOutOfBoundsException ignored)
		{
			buffer.writeInt(0);
		}
		
		try
		{
			buffer.writeInt(soul.get(1).getId());
		}
		catch (IndexOutOfBoundsException ignored)
		{
			buffer.writeInt(0);
		}
		
		final List<EnsoulOption> specialSoul = (List<EnsoulOption>) holder.getItemInfo().getSoulCrystalSpecialOptions();
		try
		{
			buffer.writeInt(specialSoul.get(0).getId());
		}
		catch (IndexOutOfBoundsException ignored)
		{
			buffer.writeInt(0);
		}
		
		buffer.writeShort(0); // isBlessed
	}
}
