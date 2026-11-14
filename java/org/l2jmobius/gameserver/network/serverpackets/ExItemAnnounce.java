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
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is used to announce the creation of a new item instance.<br>
 * It informs clients about an {@code Item} that has been added to the world.
 * @author NviX, Mobius
 */
public class ExItemAnnounce extends ServerPacket
{
	public static final int ENCHANT = 0;
	public static final int LOOT_BOX = 1;
	public static final int RANDOM_CRAFT = 2;
	public static final int SPECIAL_CREATION = 3;
	public static final int WORKSHOP = 4;
	public static final int EVENT_PARTICIPATE = 5;
	public static final int LIMITED_CRAFT = 6;
	public static final int CONTAINER = 7;
	public static final int COMPOUND = 8;
	public static final int CRAFT_SYSTEM_FANCY = 9;
	public static final int UPGRADE = 10;
	
	private final Item _item;
	private final int _type;
	private final String _announceName;
	
	/**
	 * Sends a special item announcement to a specific player.<br>
	 * This packet is used for events like enchanting or random crafting.
	 * @param player The {@code Player} who will receive the announcement.
	 * @param item The {@code Item} being announced.
	 * @param type The category of the announcement, such as {@code 0} for enchantments.
	 */
	public ExItemAnnounce(Player player, Item item, int type)
	{
		_item = item;
		_type = type;
		if (!player.getClientSettings().isAnnounceDisabled())
		{
			_announceName = player.getName();
		}
		else if ("ru".equals(player.getLang()))
		{
			_announceName = "Некто";
		}
		else
		{
			_announceName = "Someone";
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_ITEM_ANNOUNCE.writeId(this, buffer);
		// Item types are defined as: 0 for enchant, 1 for container, 2 for random creation, 3 for special creation, 4 for private workshop, 5 for secret shop, 6 for limited craft, 7 for fire and container, 8 for compound, 9 for fancy craft system, 10 for upgrade, and others use null item names by item_id with icons from the chest.
		buffer.writeByte(_type); // announce type
		buffer.writeSizedString(_announceName); // name of player
		buffer.writeInt(_item.getId()); // item id
		buffer.writeByte(_item.getEnchantLevel()); // enchant level
		buffer.writeInt(0); // chest item id
	}
}
