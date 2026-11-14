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
import org.l2jmobius.gameserver.entity.actor.holders.player.HennaList;
import org.l2jmobius.gameserver.entity.item.Henna;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client to remove a list of {@code Henna} items.<br>
 * It informs the game client that specific henna effects should be cleared from the player.
 * @author Mobius
 */
public class HennaRemoveList extends ServerPacket
{
	private final Player _player;
	
	/**
	 * This packet removes the list of {@code Henna} items from a player.<br>
	 * It is sent to the client to update the UI.
	 * @param player The {@code Player} instance for whom the henna list is being removed.
	 */
	public HennaRemoveList(Player player)
	{
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.HENNA_UNEQUIP_LIST.writeId(this, buffer);
		buffer.writeLong(_player.getAdena());
		final HennaList hennas = _player.getHennaList();
		final boolean premiumSlotEnabled = hennas.get(4) != null;
		buffer.writeInt(premiumSlotEnabled ? 4 : 3); // seems to be max size
		buffer.writeInt((premiumSlotEnabled ? 4 : 3) - hennas.getEmptySlots()); // slots used
		for (Henna henna : hennas.getAll())
		{
			if (henna != null)
			{
				buffer.writeInt(henna.getDyeId());
				buffer.writeInt(henna.getDyeItemId());
				buffer.writeLong(henna.getCancelCount());
				buffer.writeLong(henna.getCancelFee());
				buffer.writeInt(henna.isAllowedClass(_player.getPlayerClass()) ? 1 : 0);
				buffer.writeInt(henna.getDuration() * 60000);
			}
		}
	}
}
