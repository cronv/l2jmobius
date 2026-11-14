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
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.MentorManager;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client to initiate a trade between two players.<br>
 * It contains the necessary data to open the trade window and display items.
 * @author Mobius
 */
public class TradeStart extends AbstractItemPacket
{
	private final int _sendType;
	private final Player _player;
	private final Player _partner;
	private final Collection<Item> _itemList;
	private int _mask = 0;
	
	/**
	 * Initializes a new trade start packet.<br>
	 * This constructor sets the sender type and identifies the player.<br>
	 * It also determines the trade partner and available items.
	 * @param sendType The type of message being sent.
	 * @param player The {@code Player} who is initiating the trade.
	 */
	public TradeStart(int sendType, Player player)
	{
		_sendType = sendType;
		_player = player;
		_partner = player.getActiveTradeList().getPartner();
		_itemList = _player.getInventory().getAvailableItems(true, (_player.isGM() && GeneralConfig.GM_TRADE_RESTRICTED_ITEMS), false);
		if (_partner != null)
		{
			final int objectId = _partner.getObjectId();
			if (player.getFriends().contains(objectId))
			{
				_mask |= 0x01;
			}
			
			final int clanId = player.getClanId();
			if ((clanId > 0) && (clanId == _partner.getClanId()))
			{
				_mask |= 0x02;
			}
			
			final int playerObjectId = player.getObjectId();
			if ((MentorManager.getInstance().getMentee(playerObjectId, objectId) != null) || (MentorManager.getInstance().getMentee(objectId, playerObjectId) != null))
			{
				_mask |= 0x04;
			}
			
			final int playerAllyId = player.getAllyId();
			if ((playerAllyId > 0) && (playerAllyId == _partner.getAllyId()))
			{
				_mask |= 0x08;
			}
			
			// Does not shows level.
			if (_partner.isGM())
			{
				_mask |= 0x10;
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		if ((_player.getActiveTradeList() == null) || (_partner == null))
		{
			return;
		}
		
		ServerPackets.TRADE_START.writeId(this, buffer);
		buffer.writeByte(_sendType);
		if (_sendType == 2)
		{
			final int itemListSize = _itemList.size();
			buffer.writeInt(itemListSize);
			buffer.writeInt(itemListSize);
			for (Item item : _itemList)
			{
				writeItem(item, buffer);
			}
		}
		else
		{
			buffer.writeInt(_partner.getObjectId());
			buffer.writeByte(_mask); // some kind of mask
			if ((_mask & 0x10) == 0)
			{
				buffer.writeByte(_partner.getLevel());
			}
		}
	}
}
