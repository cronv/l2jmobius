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

import static org.l2jmobius.gameserver.data.xml.MultisellData.PAGE_SIZE;

import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.holders.MultisellEntryHolder;
import org.l2jmobius.gameserver.data.holders.PreparedMultisellListHolder;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.entity.item.holders.ItemInfo;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is used to send a list of items from a multisell NPC to the client.<br>
 * It contains information about the available items and their respective quantities.
 * @author Mobius
 */
public class MultiSellList extends AbstractItemPacket
{
	private final Player _player;
	private int _size;
	private int _index;
	private final PreparedMultisellListHolder _list;
	private final boolean _finished;
	private final int _type;
	
	/**
	 * Creates a new {@code MultiSellList} packet for a specific player.<br>
	 * This constructor initializes the list data and calculates the remaining size.<br>
	 * It determines if the current page is the final one based on the {@code PAGE_SIZE}.
	 * @param player The {@code Player} receiving the packet.
	 * @param list The {@code PreparedMultisellListHolder} containing the items.
	 * @param index The starting position in the list.
	 * @param type The specific type of the multi-sell list.
	 */
	public MultiSellList(Player player, PreparedMultisellListHolder list, int index, int type)
	{
		_player = player;
		_list = list;
		_index = index;
		_size = list.getEntries().size() - index;
		if (_size > PAGE_SIZE)
		{
			_finished = false;
			_size = PAGE_SIZE;
		}
		else
		{
			_finished = true;
		}
		
		_type = type;
	}
	
	/**
	 * Creates a new {@code MultiSellList} packet for a specific player.<br>
	 * This constructor initializes the list data and calculates the remaining items to display.<br>
	 * It determines if there are more pages left based on the {@code PAGE_SIZE}.
	 * @param player The {@code Player} who will receive this packet.
	 * @param list The {@code PreparedMultisellListHolder} containing the sell entries.
	 * @param index The current position in the list to start from.
	 */
	public MultiSellList(Player player, PreparedMultisellListHolder list, int index)
	{
		this(player, list, index, 0);
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.MULTI_SELL_LIST.writeId(this, buffer);
		buffer.writeByte(0); // Helios
		buffer.writeInt(_list.getId()); // list id
		buffer.writeByte(_type); // 196?
		buffer.writeInt(1 + (_index / PAGE_SIZE)); // page started from 1
		buffer.writeInt(_finished); // finished
		buffer.writeInt(PAGE_SIZE); // size of pages
		buffer.writeInt(_size); // list length
		buffer.writeByte(0); // Grand Crusade
		buffer.writeByte(_list.isChanceMultisell()); // new multisell window
		buffer.writeInt(32); // Helios - Always 32
		while (_size-- > 0)
		{
			ItemInfo itemEnchantment = _list.getItemEnchantment(_index);
			final MultisellEntryHolder entry = _list.getEntries().get(_index++);
			if ((itemEnchantment == null) && _list.isMaintainEnchantment())
			{
				SEARCH: for (ItemChanceHolder holder : entry.getIngredients())
				{
					final Item item = _player.getInventory().getItemByItemId(holder.getId());
					if ((item != null) && item.isEquipable() && !item.isEquipped())
					{
						itemEnchantment = new ItemInfo(item);
						break SEARCH;
					}
				}
			}
			
			buffer.writeInt(_index); // Entry ID. Start from 1.
			buffer.writeByte(entry.isStackable());
			
			// Those values will be passed down to MultiSellChoose packet.
			buffer.writeShort(itemEnchantment != null ? itemEnchantment.getEnchantLevel() : 0); // enchant level
			writeItemAugment(itemEnchantment, buffer);
			writeItemElemental(itemEnchantment, buffer);
			writeItemEnsoulOptions(itemEnchantment, buffer);
			buffer.writeByte(0); // 286
			final List<ItemChanceHolder> products = entry.getProducts();
			buffer.writeShort(products.size());
			buffer.writeShort(entry.getIngredients().size());
			for (ItemChanceHolder product : products)
			{
				final int productId = product.getId();
				final ItemTemplate template = ItemData.getInstance().getTemplate(productId);
				final ItemInfo displayItemEnchantment = _list.isMaintainEnchantment() && (itemEnchantment != null) && (template != null) && template.getClass().equals(itemEnchantment.getItem().getClass()) ? itemEnchantment : null;
				if (template != null)
				{
					buffer.writeInt(template.getDisplayId());
					buffer.writeLong(template.getBodyPart().getMask());
					buffer.writeShort(template.getType2());
				}
				else
				{
					buffer.writeInt(productId);
					buffer.writeLong(0);
					buffer.writeShort(65535);
				}
				
				buffer.writeLong(_list.getProductCount(product));
				buffer.writeShort(product.getEnchantmentLevel() > 0 ? product.getEnchantmentLevel() : displayItemEnchantment != null ? displayItemEnchantment.getEnchantLevel() : 0); // enchant level
				buffer.writeInt((int) (product.getChance() * 1000000)); // chance
				writeItemAugment(displayItemEnchantment, buffer);
				writeItemElemental(displayItemEnchantment, buffer);
				writeItemEnsoulOptions(displayItemEnchantment, buffer);
				buffer.writeByte(0); // 286
			}
			
			for (ItemChanceHolder ingredient : entry.getIngredients())
			{
				final int ingredientId = ingredient.getId();
				final ItemTemplate template = ItemData.getInstance().getTemplate(ingredientId);
				final ItemInfo displayItemEnchantment = (itemEnchantment != null) && (template != null) && template.getClass().equals(itemEnchantment.getItem().getClass()) ? itemEnchantment : null;
				if (template != null)
				{
					buffer.writeInt(template.getDisplayId());
					buffer.writeShort(template.getType2());
				}
				else
				{
					buffer.writeInt(ingredientId);
					buffer.writeShort(65535);
				}
				
				buffer.writeLong(_list.getIngredientCount(ingredient));
				buffer.writeShort(ingredient.getEnchantmentLevel() > 0 ? ingredient.getEnchantmentLevel() : displayItemEnchantment != null ? displayItemEnchantment.getEnchantLevel() : 0); // enchant level
				writeItemAugment(displayItemEnchantment, buffer);
				writeItemElemental(displayItemEnchantment, buffer);
				writeItemEnsoulOptions(displayItemEnchantment, buffer);
				buffer.writeByte(0); // 286
			}
		}
	}
}
