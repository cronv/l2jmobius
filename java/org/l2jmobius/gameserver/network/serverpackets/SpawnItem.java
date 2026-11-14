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
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is used to notify the client to spawn an {@code Item} instance.<br>
 * It synchronizes the appearance of items in the game world.
 * @author Mobius
 */
public class SpawnItem extends ServerPacket
{
	private final Item _item;
	
	/**
	 * Creates a new {@code SpawnItem} packet.<br>
	 * This method initializes the packet with a specific {@code Item}.
	 * @param item The {@code Item} to be spawned in the game world.
	 */
	public SpawnItem(Item item)
	{
		_item = item;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.SPAWN_ITEM.writeId(this, buffer);
		buffer.writeInt(_item.getObjectId());
		buffer.writeInt(_item.getDisplayId());
		buffer.writeInt(_item.getX());
		buffer.writeInt(_item.getY());
		buffer.writeInt(_item.getZ());
		
		// Only show item count if it is a stackable item.
		buffer.writeInt(_item.isStackable());
		buffer.writeLong(_item.getCount());
		buffer.writeInt(0); // c2
		buffer.writeByte(_item.getEnchantLevel()); // Grand Crusade
		buffer.writeByte(_item.getAugmentation() != null); // Grand Crusade
		buffer.writeByte(_item.getSpecialAbilities().size()); // Grand Crusade
		buffer.writeByte(0);
	}
}
