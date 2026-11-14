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
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.buylist.Product;
import org.l2jmobius.gameserver.mechanics.buylist.ProductList;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet sends a list of preview items to the client.<br>
 * It is used to display available products in a shop interface.
 * @author Mobius
 */
public class ShopPreviewList extends ServerPacket
{
	private final int _listId;
	private final Collection<Product> _list;
	private final long _money;
	
	/**
	 * Creates a new {@code ShopPreviewList} packet.<br>
	 * This constructor initializes the shop preview with a specific product list and amount of money.
	 * @param list The {@code ProductList} containing the items to display.
	 * @param currentMoney The amount of money the player currently has.
	 */
	public ShopPreviewList(ProductList list, long currentMoney)
	{
		_listId = list.getListId();
		_list = list.getProducts();
		_money = currentMoney;
	}
	
	/**
	 * Creates a new {@code ShopPreviewList} packet.<br>
	 * This packet sends a preview of products to the client.<br>
	 * It includes the list of items and the player's available funds.
	 * @param lst The collection of {@code Product} objects to display.
	 * @param listId The unique identifier for the shop list.
	 * @param currentMoney The amount of money the player currently has.
	 */
	public ShopPreviewList(Collection<Product> lst, int listId, long currentMoney)
	{
		_listId = listId;
		_list = lst;
		_money = currentMoney;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.BUY_PREVIEW_LIST.writeId(this, buffer);
		buffer.writeInt(5056);
		buffer.writeLong(_money); // current money
		buffer.writeInt(_listId);
		int newlength = 0;
		for (Product product : _list)
		{
			if (product.getItem().isEquipable())
			{
				newlength++;
			}
		}
		
		buffer.writeShort(newlength);
		for (Product product : _list)
		{
			if (product.getItem().isEquipable())
			{
				buffer.writeInt(product.getItemId());
				buffer.writeShort(product.getItem().getType2()); // item type2
				if (product.getItem().getType1() != ItemTemplate.TYPE1_ITEM_QUESTITEM_ADENA)
				{
					buffer.writeLong(product.getItem().getBodyPart().getMask()); // rev 415 slot 0006-lr.ear 0008-neck 0030-lr.finger 0040-head 0080-?? 0100-l.hand 0200-gloves 0400-chest 0800-pants 1000-feet 2000-?? 4000-r.hand 8000-r.hand
				}
				else
				{
					buffer.writeLong(0); // rev 415 slot 0006-lr.ear 0008-neck 0030-lr.finger 0040-head 0080-?? 0100-l.hand 0200-gloves 0400-chest 0800-pants 1000-feet 2000-?? 4000-r.hand 8000-r.hand
				}
				
				buffer.writeLong(GeneralConfig.WEAR_PRICE);
			}
		}
	}
}
