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

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.SellBuffsManager;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.holders.TradeItem;

/**
 * This packet is sent to the client to display the list of items available for sale in a private store.<br>
 * It contains the data required to populate the selling interface for the player.
 * @author Mobius
 */
public class PrivateStoreListSell extends AbstractItemPacket
{
	private final Player _player;
	private final Player _seller;
	
	/**
	 * Creates a new {@code PrivateStoreListSell} packet.<br>
	 * This packet is used to send the list of items for sale from a private store.
	 * @param player The player who will receive the packet.
	 * @param seller The player who owns the private store.
	 */
	public PrivateStoreListSell(Player player, Player seller)
	{
		_player = player;
		_seller = seller;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		if (_seller.isSellingBuffs())
		{
			SellBuffsManager.getInstance().sendBuffMenu(_player, _seller, 0);
		}
		else
		{
			ServerPackets.PRIVATE_STORE_LIST.writeId(this, buffer);
			buffer.writeInt(_seller.getObjectId());
			buffer.writeInt(_seller.getSellList().isPackaged());
			buffer.writeLong(_player.getAdena());
			buffer.writeInt(0);
			buffer.writeInt(_seller.getSellList().getItems().size());
			for (TradeItem item : _seller.getSellList().getItems())
			{
				writeItem(item, buffer);
				buffer.writeLong(item.getPrice());
				buffer.writeLong(item.getItem().getReferencePrice() * 2);
			}
		}
	}
}
