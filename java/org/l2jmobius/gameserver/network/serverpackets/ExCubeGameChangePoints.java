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
 * This packet handles the synchronization of game change points for the {@code ExCube} system.<br>
 * It informs the client about updates to specific game state values.
 * @author mrTJO
 */
public class ExCubeGameChangePoints extends ServerPacket
{
	private final int _timeLeft;
	private final int _bluePoints;
	private final int _redPoints;
	
	/**
	 * Updates the point counters for the game client.<br>
	 * This method initializes the remaining time and scores for both teams.
	 * @param timeLeft The amount of time remaining before the minigame ends.
	 * @param bluePoints The current score for the blue team.
	 * @param redPoints The current score for the red team.
	 */
	public ExCubeGameChangePoints(int timeLeft, int bluePoints, int redPoints)
	{
		_timeLeft = timeLeft;
		_bluePoints = bluePoints;
		_redPoints = redPoints;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_BLOCK_UPSET_STATE.writeId(this, buffer);
		buffer.writeInt(2);
		buffer.writeInt(_timeLeft);
		buffer.writeInt(_bluePoints);
		buffer.writeInt(_redPoints);
	}
}
