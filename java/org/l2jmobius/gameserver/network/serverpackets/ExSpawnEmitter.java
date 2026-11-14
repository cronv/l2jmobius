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
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This class handles the transmission of {@code ExSpawnEmitter} packets to clients.<br>
 * It is used to notify players about specific entity spawning events in the game world.
 * @author KenM
 */
public class ExSpawnEmitter extends ServerPacket
{
	private final int _playerObjectId;
	private final int _npcObjectId;
	
	/**
	 * Creates a new {@code ExSpawnEmitter} packet.<br>
	 * This constructor uses unique identifiers for the entities.
	 * @param playerObjectId The unique ID of the {@code Player}.
	 * @param npcObjectId The unique ID of the {@code Npc}.
	 */
	public ExSpawnEmitter(int playerObjectId, int npcObjectId)
	{
		_playerObjectId = playerObjectId;
		_npcObjectId = npcObjectId;
	}
	
	/**
	 * Creates a new {@code ExSpawnEmitter} packet using entity objects.<br>
	 * This constructor simplifies the creation of a spawn event for a specific player and NPC.
	 * @param player The {@code Player} who triggers the spawn.
	 * @param npc The {@code Npc} that is being spawned.
	 */
	public ExSpawnEmitter(Player player, Npc npc)
	{
		this(player.getObjectId(), npc.getObjectId());
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_SPAWN_EMITTER.writeId(this, buffer);
		buffer.writeInt(_npcObjectId);
		buffer.writeInt(_playerObjectId);
		buffer.writeInt(0); // ?
	}
}
