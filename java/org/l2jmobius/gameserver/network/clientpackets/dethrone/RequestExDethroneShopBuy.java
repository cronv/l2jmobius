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
package org.l2jmobius.gameserver.network.clientpackets.dethrone;

import java.util.Collection;

import org.l2jmobius.gameserver.data.holders.DethroneShopHolder;
import org.l2jmobius.gameserver.data.xml.DethroneShopData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.request.PrimeShopRequest;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.enums.ExBrProductReplyType;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.dethrone.ExDethronePointInfo;
import org.l2jmobius.gameserver.network.serverpackets.dethrone.ExDethroneShopBuy;
import org.l2jmobius.gameserver.network.serverpackets.primeshop.ExBRBuyProduct;

/**
 * Handles the client request to purchase an item from the Ex Dethrone Shop.<br>
 * This packet processes the transaction and updates the player's inventory accordingly.
 * @author Liamxroy
 */
public class RequestExDethroneShopBuy extends ClientPacket
{
	private int _productId;
	private int _amount;
	private DethroneShopHolder _product;
	
	@Override
	protected void readImpl()
	{
		_productId = readInt();
		_amount = readInt();
		_product = DethroneShopData.getInstance().getProduct(_productId);
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (_product == null)
		{
			return;
		}
		
		if ((_amount < 1) || (_amount > 10000))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVENTORY_OVERFLOW));
			player.sendPacket(new ExDethroneShopBuy(false));
			return;
		}
		
		if (!player.isInventoryUnder80(false))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVENTORY_OVERFLOW));
			player.sendPacket(new ExDethroneShopBuy(false));
			return;
		}
		
		final long personalPoints = player.getVariables().getLong(PlayerVariables.CONQUEST_PERSONAL_POINTS, 0);
		
		// Atomically claim the purchase request to prevent concurrent purchases from racing the deduction.
		if (player.hasItemRequest() || !player.addRequest(new PrimeShopRequest(player)))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVALID_USER_STATE));
			player.sendPacket(new ExDethroneShopBuy(false));
			return;
		}
		
		// Check existing items.
		final int ingredientIdsLength = _product.getIngredientIds().length;
		for (int i = 0; i < ingredientIdsLength; i++)
		{
			if (_product.getIngredientIds()[i] == 0)
			{
				continue;
			}
			
			if (_product.getIngredientIds()[i] == Inventory.ADENA_ID)
			{
				if (player.getAdena() < (_product.getIngredientQuantities()[i] * _amount))
				{
					player.removeRequest(PrimeShopRequest.class);
					player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
					player.sendPacket(new ExDethroneShopBuy(false));
					return;
				}
			}
			else if (_product.getIngredientIds()[i] == Inventory.CONQUEST_PERSONAL_POINTS)
			{
				if (personalPoints < (_product.getIngredientQuantities()[i] * _amount))
				{
					player.removeRequest(PrimeShopRequest.class);
					player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
					player.sendPacket(new ExDethroneShopBuy(false));
					return;
				}
			}
			
			else if (player.getInventory().getInventoryItemCount(_product.getIngredientIds()[i], _product.getIngredientEnchants()[i] == 0 ? -1 : _product.getIngredientEnchants()[i], true) < (_product.getIngredientQuantities()[i] * _amount))
			{
				player.removeRequest(PrimeShopRequest.class);
				player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
				player.sendPacket(new ExDethroneShopBuy(false));
				return;
			}
		}
		
		// Remove items.
		for (int i = 0; i < ingredientIdsLength; i++)
		{
			if (_product.getIngredientIds()[i] == 0)
			{
				continue;
			}
			
			final long toRemove = _product.getIngredientQuantities()[i] * _amount;
			if (_product.getIngredientIds()[i] == Inventory.ADENA_ID)
			{
				player.reduceAdena(ItemProcessType.FEE, toRemove, player, true);
			}
			else if (_product.getIngredientIds()[i] == Inventory.CONQUEST_PERSONAL_POINTS)
			{
				player.getVariables().set(PlayerVariables.CONQUEST_PERSONAL_POINTS, (personalPoints - toRemove));
				
				// Message
				SystemMessage sm = new SystemMessage(SystemMessageId.PERSONAL_CONQUEST_POINTS_S1);
				sm.addLong(toRemove);
				player.sendPacket(sm);
			}
			else
			{
				if (_product.getIngredientEnchants()[i] > 0)
				{
					long count = 0;
					final Collection<Item> items = player.getInventory().getAllItemsByItemId(_product.getIngredientIds()[i], _product.getIngredientEnchants()[i]);
					for (Item item : items)
					{
						if (count == toRemove)
						{
							break;
						}
						
						count++;
						player.destroyItem(ItemProcessType.FEE, item, player, true);
					}
				}
				else
				{
					player.destroyItemByItemId(ItemProcessType.FEE, _product.getIngredientIds()[i], toRemove, player, true);
				}
			}
		}
		
		// Reward.
		if (_product.getProductionId() > 0)
		{
			player.addItem(ItemProcessType.BUY, _product.getProductionId(), _product.getCount() * _amount, player, true);
			
			if (_product.getProductionId2() > 0)
			{
				player.addItem(ItemProcessType.BUY, _product.getProductionId2(), _product.getCount2() * _amount, player, true);
			}
			else if (_product.getProductionId3() > 0)
			{
				player.addItem(ItemProcessType.BUY, _product.getProductionId3(), _product.getCount3() * _amount, player, true);
			}
			else if (_product.getProductionId4() > 0)
			{
				player.addItem(ItemProcessType.BUY, _product.getProductionId4(), _product.getCount4() * _amount, player, true);
			}
			else if (_product.getProductionId5() > 0)
			{
				player.addItem(ItemProcessType.BUY, _product.getProductionId5(), _product.getCount5() * _amount, player, true);
			}
		}
		
		player.sendPacket(new ExDethroneShopBuy(true));
		player.sendPacket(new ExDethronePointInfo(personalPoints));
		player.sendItemList();
		player.removeRequest(PrimeShopRequest.class);
	}
}
