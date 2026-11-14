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
package org.l2jmobius.gameserver.network.serverpackets.virtualItem;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.IllusoryEquipmentConfig;
import org.l2jmobius.gameserver.data.holders.VirtualItemHolder;
import org.l2jmobius.gameserver.data.xml.VirtualItemData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * Answers a {@code RequestExVirtualItemSystem} with the state of the system for a player.<br>
 * It carries the points, the selection the client asked about and every virtual item the player currently carries.
 * @author CostyKiller
 */
public class ExVirtualItemSystem extends ServerPacket
{
	private static final Logger LOGGER = Logger.getLogger(ExVirtualItemSystem.class.getName());
	
	private final int _type;
	private final boolean _result;
	private final int _remainingTime;
	private final int _acquiredPoints;
	private final int _usedPoints;
	private final int _bonusPoints;
	private final int _maxPoints;
	private final int _selectIndexMain;
	private final int _selectIndexSub;
	private final int _selectSlot;
	private final Collection<VirtualItemHolder> _virtualItems;
	private final Map<Integer, Integer> _slotIndexes;
	
	/**
	 * Constructs a new {@code ExVirtualItemSystem} packet.
	 * @param player The {@code Player} who will receive this packet.
	 * @param type The type of the request being answered.
	 * @param result The result of the request, shown by the client.
	 * @param selectIndexMain The main selection index the client sent.
	 * @param selectIndexSub The sub selection index the client sent.
	 * @param selectSlot The window slot the client sent.
	 * @param virtualItems The virtual items the player currently carries.
	 * @param slotIndexes The window slot each of those virtual items sits in, mapped by category.
	 */
	public ExVirtualItemSystem(Player player, int type, boolean result, int selectIndexMain, int selectIndexSub, int selectSlot, Collection<VirtualItemHolder> virtualItems, Map<Integer, Integer> slotIndexes)
	{
		final PlayerVariables variables = player.getVariables();
		_type = type;
		_result = result;
		_remainingTime = VirtualItemData.getInstance().getEventRemainingTime();
		_acquiredPoints = variables.getInt(PlayerVariables.ILLUSORY_POINTS_ACQUIRED, 0);
		_usedPoints = variables.getInt(PlayerVariables.ILLUSORY_POINTS_USED, 0);
		_bonusPoints = variables.getInt(PlayerVariables.ILLUSORY_POINTS_BONUS, 0);
		_maxPoints = IllusoryEquipmentConfig.getPointsLimit(player.getLevel());
		_selectIndexMain = selectIndexMain;
		_selectIndexSub = selectIndexSub;
		_selectSlot = selectSlot;
		_virtualItems = virtualItems;
		_slotIndexes = slotIndexes;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		final int size = _virtualItems.size();
		final boolean debug = IllusoryEquipmentConfig.ILLUSORY_EQUIPMENT_EVENT_DEBUG_ENABLED;
		ServerPackets.EX_VIRTUALITEM_SYSTEM.writeId(this, buffer);
		buffer.writeByte(_type); // var int cType;
		buffer.writeByte(_result); // var int cResult;
		buffer.writeInt(_remainingTime); // var int nEndTime; seconds the window counts down from.
		buffer.writeInt(_acquiredPoints); // var int nTotalGetVISPoint;
		buffer.writeInt(_usedPoints); // var int nTotalUsedVISPoint;
		buffer.writeInt(_bonusPoints); // var int nVISMaxPoint; the bonus the window shows beside the cap.
		buffer.writeInt(_maxPoints); // var int nTotalVISMaxPoint; the cap of the character level.
		buffer.writeInt(_selectIndexMain); // var int nSelectIndexMain;
		buffer.writeInt(_selectIndexSub); // var int nSelectIndexSub;
		buffer.writeInt(_selectSlot); // var int nSelectSlot;
		buffer.writeInt(size); // var int lstVISItemInfoList size;
		if (debug)
		{
			LOGGER.info("IllusoryEquipment: sending cType=" + _type + " cResult=" + _result + " nEndTime=" + _remainingTime + " nTotalGetVISPoint=" + _acquiredPoints + " nTotalUsedVISPoint=" + _usedPoints + " nVISMaxPoint=" + _bonusPoints + " nTotalVISMaxPoint=" + _maxPoints + " nSelectIndexMain=" + _selectIndexMain + " nSelectIndexSub=" + _selectIndexSub + " nSelectSlot=" + _selectSlot + " list size=" + size);
		}
		
		for (VirtualItemHolder virtualItem : _virtualItems)
		{
			final int indexMain = virtualItem.getIndexMain();
			final int indexSub = virtualItem.getIndexSub();
			final int cost = VirtualItemData.getInstance().getTotalCost(virtualItem);
			final int itemId = virtualItem.getItemId();
			final int enchant = virtualItem.getEnchant();
			final Integer slotIndex = _slotIndexes.get(Integer.valueOf(indexMain));
			final int slot = slotIndex == null ? 0 : slotIndex.intValue();
			buffer.writeInt(indexMain); // var int nIndexMain;
			buffer.writeInt(indexSub); // var int nIndexSub;
			buffer.writeInt(slot); // var int nSlot; the window slot, not the body part.
			buffer.writeInt(cost); // var int nCostVISPoint;
			buffer.writeInt(itemId); // var int nItemClass;
			buffer.writeInt(enchant); // var int nEnchant;
			
			if (debug)
			{
				LOGGER.info("IllusoryEquipment: entry nIndexMain=" + indexMain + " nIndexSub=" + indexSub + " nSlot=" + slot + " nCostVISPoint=" + cost + " nItemClass=" + itemId + " nEnchant=" + enchant);
			}
		}
	}
}
