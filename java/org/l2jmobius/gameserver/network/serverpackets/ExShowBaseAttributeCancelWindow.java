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
import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is used to close the base attribute display window for a player.<br>
 * It informs the client to hide the UI associated with viewing character attributes.
 */
public class ExShowBaseAttributeCancelWindow extends ServerPacket
{
	private final List<Item> _items = new ArrayList<>();
	private long _price;
	
	/**
	 * Creates a new window to cancel base attributes.<br>
	 * This method scans the {@code Player} inventory for items with attributes.<br>
	 * It adds all found items to the internal list.
	 * @param player The {@code Player} whose inventory will be scanned.
	 */
	public ExShowBaseAttributeCancelWindow(Player player)
	{
		for (Item item : player.getInventory().getItems())
		{
			if (item.hasAttributes())
			{
				_items.add(item);
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_SHOW_BASE_ATTRIBUTE_CANCEL_WINDOW.writeId(this, buffer);
		buffer.writeInt(_items.size());
		for (Item item : _items)
		{
			buffer.writeInt(item.getObjectId());
			buffer.writeLong(getPrice(item));
		}
	}
	
	/**
	 * Calculates the price for a specific {@code Item}.<br>
	 * The price depends on the item's crystal type and whether it is a weapon.
	 * @param item The {@code Item} to check for pricing.
	 * @return The calculated price as a {@code long}.
	 */
	private long getPrice(Item item)
	{
		switch (item.getTemplate().getCrystalType())
		{
			case S:
			{
				if (item.isWeapon())
				{
					_price = 50000;
				}
				else
				{
					_price = 40000;
				}
				break;
			}
			case S80:
			{
				if (item.isWeapon())
				{
					_price = 100000;
				}
				else
				{
					_price = 80000;
				}
				break;
			}
			case S84:
			{
				if (item.isWeapon())
				{
					_price = 200000;
				}
				else
				{
					_price = 160000;
				}
				break;
			}
		}
		
		return _price;
	}
}
