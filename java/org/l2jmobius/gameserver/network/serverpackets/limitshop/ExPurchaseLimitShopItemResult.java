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
package org.l2jmobius.gameserver.network.serverpackets.limitshop;

import java.util.Collection;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.holders.LimitShopRandomCraftReward;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet is sent to the client to provide the result of an {@code ExPurchaseLimitShopItem} action.<br>
 * It informs the player whether their purchase was successful or failed.
 * @author Norvox
 */
public class ExPurchaseLimitShopItemResult extends ServerPacket
{
	private final int _category, _productId;
	private final boolean _isSuccess;
	private final int _remainingInfo;
	private final Collection<LimitShopRandomCraftReward> _rewards;
	
	/**
	 * Creates a new {@code ExPurchaseLimitShopItemResult} packet.<br>
	 * This constructor initializes the result of an item purchase in the limit shop.
	 * @param isSuccess The status of the purchase operation.
	 * @param category The category ID of the purchased item.
	 * @param productId The unique identifier for the product.
	 * @param remainingInfo Additional information regarding the remaining stock or limits.
	 * @param rewards A collection of {@code LimitShopRandomCraftReward} objects granted to the player.
	 */
	public ExPurchaseLimitShopItemResult(boolean isSuccess, int category, int productId, int remainingInfo, Collection<LimitShopRandomCraftReward> rewards)
	{
		_isSuccess = isSuccess;
		_category = category;
		_productId = productId;
		_remainingInfo = remainingInfo;
		_rewards = rewards;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_PURCHASE_LIMIT_SHOP_ITEM_BUY.writeId(this, buffer);
		buffer.writeByte(_isSuccess ? 0 : 1);
		buffer.writeByte(_category);
		buffer.writeInt(_productId);
		buffer.writeInt(_rewards.size());
		for (LimitShopRandomCraftReward entry : _rewards)
		{
			buffer.writeByte(entry.getRewardIndex());
			buffer.writeInt(entry.getItemId());
			buffer.writeInt(entry.getCount().get());
		}
		
		buffer.writeInt(_remainingInfo);
	}
}
