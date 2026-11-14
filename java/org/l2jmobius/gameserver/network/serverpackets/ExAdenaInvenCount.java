/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client to provide the count of {@code Adena} in a player's inventory.<br>
 * It helps the client display the correct amount of currency for the user.
 * @author Sdw
 */
public class ExAdenaInvenCount extends ServerPacket
{
	private final Player _player;
	
	/**
	 * Creates a new {@code ExAdenaInvenCount} packet for a specific player.<br>
	 * This constructor initializes the internal player reference.
	 * @param player The {@code Player} object associated with this packet.
	 */
	public ExAdenaInvenCount(Player player)
	{
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_ADENA_INVEN_COUNT.writeId(this, buffer);
		buffer.writeLong(_player.getAdena());
		buffer.writeShort(_player.getInventory().getSize());
	}
}
