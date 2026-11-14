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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.custom.MerchantZeroSellPriceConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.buylist.Product;
import org.l2jmobius.gameserver.mechanics.buylist.ProductList;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is used to send a list of items that can be bought or sold from an NPC.<br>
 * It contains the necessary data for the client to display the merchant's inventory.
 * @author Index
 */
public class ExBuySellList extends AbstractItemPacket
{
	public static final int BUY_SELL_LIST_BUY = 0;
	public static final int BUY_SELL_LIST_SELL = 1;
	public static final int BUY_SELL_LIST_UNK = 2;
	public static final int BUY_SELL_LIST_TAX = 3;
	
	public static final int UNK_SELECT_FIRST_TAB = 0;
	public static final int UNK_SHOW_PURCHASE_LIST = 1;
	public static final int UNK_SEND_NOT_ENOUGH_ADENA_MESSAGE = 2;
	public static final int UNK_SEND_INCORRECT_ITEM_MESSAGE = 3;
	
	private final int _inventorySlots;
	private final int _type;
	
	// buy type - BUY
	private long _money;
	private double _castleTaxRate;
	private Collection<Product> _list;
	private int _listId;
	
	// buy type - SELL
	private final List<Item> _sellList = new ArrayList<>();
	private final Collection<Item> _refundList = new ArrayList<>();
	private boolean _done;
	
	// buy type = unk
	private int _unkType;
	
	// buy type - send tax
	private int _nearestCastle;
	private boolean _applyTax;
	
	/**
	 * Creates a new {@code ExBuySellList} packet for buying items.<br>
	 * This constructor initializes the list data and calculates the player's available money.<br>
	 * It also sets the inventory size and the applicable castle tax rate.
	 * @param list The {@code ProductList} containing the items to be displayed.
	 * @param player The {@code Player} who is viewing the buy list.
	 * @param castleTaxRate The tax rate applied by the nearest castle.
	 */
	public ExBuySellList(ProductList list, Player player, double castleTaxRate)
	{
		_type = BUY_SELL_LIST_BUY;
		_listId = list.getListId();
		_list = list.getProducts();
		_money = player.isGM() && (player.getAdena() == 0) && (list.getNpcsAllowed() == null) ? 1000000000 : player.getAdena();
		_inventorySlots = player.getInventory().getNonQuestSize();
		_castleTaxRate = castleTaxRate;
	}
	
	/**
	 * Creates a new {@code ExBuySellList} for a specific player.<br>
	 * This constructor populates the list of sellable items from the player's inventory.<br>
	 * It also checks if the player has any refund items available.
	 * @param player The {@code Player} whose inventory is being processed.
	 * @param done A boolean flag indicating whether the transaction process is finished.
	 */
	public ExBuySellList(Player player, boolean done)
	{
		_type = BUY_SELL_LIST_SELL;
		final Summon pet = player.getPet();
		for (Item item : player.getInventory().getItems())
		{
			if (!item.isEquipped() && item.isSellable() && ((pet == null) || (item.getObjectId() != pet.getControlObjectId())))
			{
				_sellList.add(item);
			}
		}
		
		_inventorySlots = player.getInventory().getNonQuestSize();
		if (player.hasRefund())
		{
			_refundList.addAll(player.getRefund().getItems());
		}
		
		_done = done;
	}
	
	/**
	 * Creates a new {@code ExBuySellList} packet with an unknown type.<br>
	 * This constructor initializes the internal state for generic list updates.
	 * @param type The specific sub-type of the unknown action to perform.
	 */
	public ExBuySellList(int type)
	{
		_type = BUY_SELL_LIST_UNK;
		_unkType = type;
		_inventorySlots = 0;
	}
	
	/**
	 * Creates a packet to display the current tax rate.<br>
	 * This packet is used when a player interacts with a castle.
	 * @param nearestCastle The {@code Castle} object closest to the player.
	 * @param applyTax A boolean indicating whether the tax should be applied.
	 */
	public ExBuySellList(Castle nearestCastle, boolean applyTax)
	{
		_type = BUY_SELL_LIST_TAX;
		_inventorySlots = 0;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_BUY_SELL_LIST.writeId(this, buffer);
		buffer.writeInt(_type);
		switch (_type)
		{
			case BUY_SELL_LIST_BUY:
			{
				sendBuyList(buffer);
				break;
			}
			case BUY_SELL_LIST_SELL:
			{
				sendSellList(buffer);
				break;
			}
			case BUY_SELL_LIST_UNK:
			{
				sendUnk(buffer);
				break;
			}
			case BUY_SELL_LIST_TAX:
			{
				sendCurrentTax(buffer);
				break;
			}
			default:
			{
				PacketLogger.warning(getClass().getSimpleName() + ": unknown type " + _type);
				break;
			}
		}
	}
	
	/**
	 * Writes the buy list data into the provided {@code WriteBuffer}.<br>
	 * This method includes money, inventory slots, and all available products.<br>
	 * It calculates the final price for each product based on tax rates.
	 * @param buffer The {@code WriteBuffer} used to store the packet data.
	 */
	private void sendBuyList(WriteBuffer buffer)
	{
		buffer.writeLong(_money); // current money
		buffer.writeInt(_listId);
		buffer.writeInt(_inventorySlots);
		buffer.writeShort(_list.size());
		for (Product product : _list)
		{
			if ((product.getCount() > 0) || !product.hasLimitedStock())
			{
				writeItem(product, buffer);
				buffer.writeLong((long) (product.getPrice() * (1.0 + _castleTaxRate + product.getBaseTaxRate())));
			}
		}
	}
	
	/**
	 * Writes the sell list data into the provided buffer.<br>
	 * This method handles both the sell list and refund list items.<br>
	 * It also writes the completion status of the transaction.
	 * @param buffer The {@code WriteBuffer} used to store the packet data.
	 */
	private void sendSellList(WriteBuffer buffer)
	{
		buffer.writeInt(_inventorySlots);
		if (!_sellList.isEmpty())
		{
			buffer.writeShort(_sellList.size());
			for (Item item : _sellList)
			{
				writeItem(item, buffer);
				buffer.writeLong(MerchantZeroSellPriceConfig.MERCHANT_ZERO_SELL_PRICE ? 0 : item.getTemplate().getReferencePrice() / 2);
			}
		}
		else
		{
			buffer.writeShort(0);
		}
		
		if (!_refundList.isEmpty())
		{
			buffer.writeShort(_refundList.size());
			int i = 0;
			for (Item item : _refundList)
			{
				writeItem(item, buffer);
				buffer.writeInt(i++);
				buffer.writeLong(MerchantZeroSellPriceConfig.MERCHANT_ZERO_SELL_PRICE ? 0 : (item.getTemplate().getReferencePrice() / 2) * item.getCount());
			}
		}
		else
		{
			buffer.writeShort(0);
		}
		
		buffer.writeByte(_done ? 1 : 0);
	}
	
	/**
	 * Sends an unknown type of data to the client.<br>
	 * This method writes the {@code _unkType} value into the provided buffer.
	 * @param buffer The {@code WriteBuffer} used to store the packet data.
	 */
	private void sendUnk(WriteBuffer buffer)
	{
		buffer.writeByte(_unkType);
	}
	
	/**
	 * Writes the current castle tax information to the buffer.<br>
	 * It checks if a nearest castle exists before writing the data.<br>
	 * If a castle is found, it writes the ID and the tax rate.
	 * @param buffer The {@code WriteBuffer} used to store the packet data.
	 */
	private void sendCurrentTax(WriteBuffer buffer)
	{
		buffer.writeInt(_nearestCastle);
		if (_nearestCastle != 0)
		{
			buffer.writeInt(_nearestCastle);
			buffer.writeInt(_applyTax ? (int) _castleTaxRate : 0);
		}
	}
}
