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
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.mechanics.options.AugmentationInstance;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.enums.InventorySlot;

/**
 * This packet is used to send information about a specific equipment slot to the client.<br>
 * It allows the game server to update the visual state of an item in a player's inventory.<br>
 * The packet contains data related to the {@code InventorySlot} being updated.
 * @author Mobius
 */
public class ExUserInfoEquipSlot extends AbstractComponentPacket<InventorySlot>
{
	private final Player _player;
	private final byte[] _masks =
	{
		(byte) 0x00,
		(byte) 0x00,
		(byte) 0x00,
		(byte) 0x00,
		(byte) 0x00,
		(byte) 0x00, // 152
		(byte) 0x00, // 152
		(byte) 0x00, // 152
	};
	
	/**
	 * Creates a new {@code ExUserInfoEquipSlot} packet for a specific player.<br>
	 * This constructor automatically includes all equipment slots in the update.
	 * @param player The {@code Player} object associated with this packet.
	 */
	public ExUserInfoEquipSlot(Player player)
	{
		this(player, true);
	}
	
	/**
	 * Creates a new {@code ExUserInfoEquipSlot} packet for a specific player.<br>
	 * This constructor determines which inventory slots are included in the packet.<br>
	 * If {@code addAll} is {@code true}, it includes all available {@code InventorySlot} types.
	 * @param player The {@code Player} object associated with this packet.
	 * @param addAll Set to {@code true} to include every inventory slot, or {@code false} for default behavior.
	 */
	public ExUserInfoEquipSlot(Player player, boolean addAll)
	{
		_player = player;
		if (addAll)
		{
			addComponentType(InventorySlot.values());
		}
	}
	
	/**
	 * Retrieves the bitmask array for this packet.<br>
	 * This array defines which fields are included in the data.
	 * @return a {@code byte[]} containing the masks.
	 */
	@Override
	protected byte[] getMasks()
	{
		return _masks;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_USER_INFO_EQUIPSLOT.writeId(this, buffer);
		buffer.writeInt(_player.getObjectId());
		buffer.writeShort(InventorySlot.values().length); // 152
		buffer.writeBytes(_masks);
		final PlayerInventory inventory = _player.getInventory();
		for (InventorySlot slot : InventorySlot.values())
		{
			if (containsMask(slot))
			{
				final AugmentationInstance augment = inventory.getPaperdollAugmentation(slot.getSlot());
				buffer.writeShort(26); // 10 + 4 * 3
				buffer.writeInt(inventory.getPaperdollObjectId(slot.getSlot()));
				buffer.writeInt(inventory.getPaperdollItemId(slot.getSlot()));
				
				if (augment != null)
				{
					buffer.writeInt(augment.getOption1Id());
					buffer.writeInt(augment.getOption2Id());
					buffer.writeInt(augment.getOption3Id());
				}
				else
				{
					buffer.writeInt(0);
					buffer.writeInt(0);
					buffer.writeInt(0);
				}
				
				buffer.writeInt(inventory.getPaperdollItemVisualId(slot.getSlot()));
			}
		}
	}
}
