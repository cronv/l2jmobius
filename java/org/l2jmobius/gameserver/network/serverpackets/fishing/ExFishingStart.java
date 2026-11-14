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
package org.l2jmobius.gameserver.network.serverpackets.fishing;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet is sent to the client to initiate an extended fishing action.<br>
 * It informs the {@code GameClient} that a player has started fishing.
 * @author -Wooden-
 */
public class ExFishingStart extends ServerPacket
{
	private final Player _player;
	private final int _fishType;
	private final ILocational _baitLocation;
	
	/**
	 * Initializes a new {@code ExFishingStart} packet.<br>
	 * This packet is sent to the client when a player starts fishing.
	 * @param player The {@code Player} who started the fishing action.
	 * @param fishType The unique identifier for the type of fish being caught.
	 * @param baitLocation The {@code ILocational} where the bait is placed.
	 */
	public ExFishingStart(Player player, int fishType, ILocational baitLocation)
	{
		_player = player;
		_fishType = fishType;
		_baitLocation = baitLocation;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_FISHING_START.writeId(this, buffer);
		buffer.writeInt(_player.getObjectId());
		buffer.writeByte(_fishType);
		buffer.writeInt(_baitLocation.getX());
		buffer.writeInt(_baitLocation.getY());
		buffer.writeInt(_baitLocation.getZ());
		buffer.writeByte(1); // 0 = newbie, 1 = normal, 2 = night
	}
}
