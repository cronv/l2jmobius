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
import org.l2jmobius.gameserver.entity.actor.holders.player.HennaList;
import org.l2jmobius.gameserver.entity.item.Henna;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet sends a player's henna information to the client.<br>
 * It is specifically used for displaying data within the Game Master's UI.
 * @author Mobius
 */
public class GMHennaInfo extends ServerPacket
{
	private final Player _player;
	private final List<Henna> _hennas = new ArrayList<>();
	
	/**
	 * This constructor initializes the packet with a specific player's henna data.<br>
	 * It collects all non-null {@code Henna} items from the {@code Player}.
	 * @param player The {@code Player} whose henna information will be sent.
	 */
	public GMHennaInfo(Player player)
	{
		_player = player;
		final HennaList hennas = player.getHennaList();
		for (int i = 1; i < 4; i++)
		{
			final Henna henna = hennas.get(i);
			if (henna != null)
			{
				_hennas.add(henna);
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.GM_HENNA_INFO.writeId(this, buffer);
		final HennaList hennas = _player.getHennaList();
		buffer.writeShort(hennas.getValue(BaseStat.INT)); // equip INT
		buffer.writeShort(hennas.getValue(BaseStat.STR)); // equip STR
		buffer.writeShort(hennas.getValue(BaseStat.CON)); // equip CON
		buffer.writeShort(hennas.getValue(BaseStat.MEN)); // equip MEN
		buffer.writeShort(hennas.getValue(BaseStat.DEX)); // equip DEX
		buffer.writeShort(hennas.getValue(BaseStat.WIT)); // equip WIT
		buffer.writeShort(hennas.getValue(BaseStat.LUC)); // equip LUC
		buffer.writeShort(hennas.getValue(BaseStat.CHA)); // equip CHA
		buffer.writeInt(3); // Slots
		buffer.writeInt(_hennas.size()); // Size
		for (Henna henna : _hennas)
		{
			buffer.writeInt(henna.getDyeId());
			buffer.writeInt(1);
		}
		
		final Henna premium = hennas.get(4);
		if (premium != null)
		{
			buffer.writeInt(premium.getDyeId());
			buffer.writeInt(premium.getDuration()); // Premium Slot Dye Time Left
			buffer.writeInt(premium.isAllowedClass(_player.getPlayerClass()));
		}
		else
		{
			buffer.writeInt(0); // Premium Slot Dye ID
			buffer.writeInt(0); // Premium Slot Dye Time Left
			buffer.writeInt(0); // Premium Slot Dye ID isValid
		}
	}
}
