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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.holders.TradeItem;
import org.l2jmobius.gameserver.network.holders.TradeList;

/**
 * This packet handles the management of items listed for sale in a private store.<br>
 * It is used to update or synchronize the list of sellable items with the client.
 * @author Mobius
 */
public class PrivateStoreManageListSell extends AbstractItemPacket
{
	private final int _sendType;
	private final int _objId;
	private final long _playerAdena;
	private final boolean _packageSale;
	private final Collection<TradeItem> _itemList;
	private final Collection<TradeItem> _sellList;
	
	/**
	 * This method initializes a packet to manage the sell list in a private store.<br>
	 * It updates the items available for sale based on the {@code Player} inventory.<br>
	 * The packet includes information about the current adena and item collection.
	 * @param sendType The type of message being sent to the client.
	 * @param player The {@code Player} object who owns the private store.
	 * @param isPackageSale A boolean indicating if the sale is a package sale.
	 */
	public PrivateStoreManageListSell(int sendType, Player player, boolean isPackageSale)
	{
		_sendType = sendType;
		_objId = player.getObjectId();
		_playerAdena = player.getAdena();
		player.getSellList().updateItems();
		_packageSale = isPackageSale;
		final TradeList sellList = player.getSellList();
		_itemList = player.getInventory().getAvailableItems(sellList);
		_sellList = sellList.getItems();
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.PRIVATE_STORE_MANAGE_LIST.writeId(this, buffer);
		buffer.writeByte(_sendType);
		if (_sendType == 2)
		{
			final int itemListSize = _itemList.size();
			buffer.writeInt(itemListSize);
			buffer.writeInt(itemListSize);
			for (TradeItem item : _itemList)
			{
				writeItem(item, buffer);
				buffer.writeLong(item.getItem().getReferencePrice() * 2);
			}
		}
		else
		{
			buffer.writeInt(_objId);
			buffer.writeInt(_packageSale);
			buffer.writeLong(_playerAdena);
			buffer.writeInt(0);
			for (TradeItem item : _itemList)
			{
				writeItem(item, buffer);
				buffer.writeLong(item.getItem().getReferencePrice() * 2);
			}
			
			buffer.writeInt(0);
			for (TradeItem item : _sellList)
			{
				writeItem(item, buffer);
				buffer.writeLong(item.getPrice());
				buffer.writeLong(item.getItem().getReferencePrice() * 2);
			}
		}
	}
}
