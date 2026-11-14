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
 * This packet is sent to the client when an {@code ExCube} game session has finished.<br>
 * It informs the player that the match is over.
 * @author mrTJO
 */
public class ExCubeGameEnd extends ServerPacket
{
	private final boolean _isRedTeamWin;
	
	/**
	 * This method initializes the game end packet.<br>
	 * It sets whether the red team won the match.
	 * @param isRedTeamWin Set to {@code true} if the red team won, or {@code false} otherwise.
	 */
	public ExCubeGameEnd(boolean isRedTeamWin)
	{
		_isRedTeamWin = isRedTeamWin;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_BLOCK_UPSET_STATE.writeId(this, buffer);
		buffer.writeInt(1);
		buffer.writeInt(_isRedTeamWin);
		buffer.writeInt(0); // TODO: Find me!
	}
}
