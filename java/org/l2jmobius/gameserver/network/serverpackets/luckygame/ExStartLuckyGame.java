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
package org.l2jmobius.gameserver.network.serverpackets.luckygame;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.enums.LuckyGameBettingType;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet is sent to the client to initiate a new lucky game.<br>
 * It handles the starting sequence for the {@code LuckyGame} system.
 * @author Sdw
 */
public class ExStartLuckyGame extends ServerPacket
{
	private final LuckyGameBettingType _type;
	private final int _ticketCount;
	
	/**
	 * This constructor initializes a new {@code ExStartLuckyGame} packet.<br>
	 * It sets the game type and the number of tickets used.
	 * @param type The {@code LuckyGameBettingType} for this game instance.
	 * @param ticketCount The total number of tickets to be processed.
	 */
	public ExStartLuckyGame(LuckyGameBettingType type, long ticketCount)
	{
		_type = type;
		_ticketCount = (int) ticketCount;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_START_LUCKY_GAME.writeId(this, buffer);
		buffer.writeInt(_type.getClientId());
		buffer.writeInt(_ticketCount);
	}
}
