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
package org.l2jmobius.gameserver.network.serverpackets.ceremonyofchaos;

import java.util.Collection;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.enums.CeremonyOfChaosResult;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet is sent to the client to communicate the result of a {@code CeremonyOfChaos} event.<br>
 * It contains information regarding whether the player succeeded or failed in their attempt.
 * @author Sdw
 */
public class ExCuriousHouseResult extends ServerPacket
{
	private final CeremonyOfChaosResult _result;
	private final Collection<Player> _players;
	private final int _time;
	
	/**
	 * Creates a new {@code ExCuriousHouseResult} packet.<br>
	 * This constructor initializes the result of the ceremony.<br>
	 * It also sets the list of involved players and the elapsed time.
	 * @param result The outcome of the ceremony.
	 * @param players The collection of players who participated.
	 * @param time The total time taken for the event.
	 */
	public ExCuriousHouseResult(CeremonyOfChaosResult result, Collection<Player> players, int time)
	{
		_result = result;
		_players = players;
		_time = time;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_CURIOUS_HOUSE_RESULT.writeId(this, buffer);
		buffer.writeInt(0); // _event.getId()
		buffer.writeShort(_result.ordinal());
		buffer.writeInt(18); // max players
		buffer.writeInt(_players.size());
		int pos = 0;
		for (Player player : _players)
		{
			buffer.writeInt(player.getObjectId());
			buffer.writeInt(pos++); // position
			buffer.writeInt(player.getPlayerClass().getId());
			buffer.writeInt(_time); // getLifeTime
			buffer.writeInt(player.getVariables().getInt(PlayerVariables.CEREMONY_OF_CHAOS_SCORE, 0));
		}
	}
}
