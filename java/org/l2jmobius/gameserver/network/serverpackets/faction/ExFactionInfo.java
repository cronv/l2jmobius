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
package org.l2jmobius.gameserver.network.serverpackets.faction;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Faction;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet sends information about a {@code Faction} to the client.<br>
 * It is used to update the player's view of faction data.
 * @author Mathael, Mobius
 */
public class ExFactionInfo extends ServerPacket
{
	private final Player _player;
	private final boolean _openDialog;
	
	/**
	 * Creates a new {@code ExFactionInfo} packet.<br>
	 * This constructor initializes the data for a faction information message.
	 * @param player The {@code Player} who will receive this packet.
	 * @param openDialog Set to {@code true} if a dialog should be opened, or {@code false} otherwise.
	 */
	public ExFactionInfo(Player player, boolean openDialog)
	{
		_player = player;
		_openDialog = openDialog;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_FACTION_INFO.writeId(this, buffer);
		buffer.writeInt(_player.getObjectId());
		buffer.writeByte(_openDialog);
		buffer.writeInt(Faction.values().length);
		for (Faction faction : Faction.values())
		{
			buffer.writeByte(faction.getId());
			buffer.writeShort(_player.getFactionLevel(faction));
			buffer.writeFloat(_player.getFactionProgress(faction));
		}
	}
}
