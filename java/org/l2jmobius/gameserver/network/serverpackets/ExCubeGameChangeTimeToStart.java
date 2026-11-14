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
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet informs the client about a change in the time remaining until an {@code ExCube} game starts.<br>
 * It is used to synchronize the countdown timer between the server and the client.
 * @author mrTJO
 */
public class ExCubeGameChangeTimeToStart extends ServerPacket
{
	private final int _seconds;
	
	/**
	 * Updates the time remaining until a game starts.<br>
	 * This method sets the countdown value for the minigame.
	 * @param seconds The number of seconds left before the game begins.
	 */
	public ExCubeGameChangeTimeToStart(int seconds)
	{
		_seconds = seconds;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_BLOCK_UPSET_LIST.writeId(this, buffer);
		buffer.writeInt(3);
		buffer.writeInt(_seconds);
	}
}
