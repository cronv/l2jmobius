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
 * This packet handles the extended change points for the {@code ExCube} game system.<br>
 * It synchronizes specific point updates between the server and the client.
 * @author mrTJO
 */
public class ExCubeGameExtendedChangePoints extends ServerPacket
{
	private final int _timeLeft;
	private final int _bluePoints;
	private final int _redPoints;
	private final boolean _isRedTeam;
	private final Player _player;
	private final int _playerPoints;
	
	/**
	 * Updates the Secret Point Counter for a specific player.<br>
	 * This packet provides current game statistics during an ExCube game.
	 * @param timeLeft The time remaining before the minigame ends.
	 * @param bluePoints The current total points for the blue team.
	 * @param redPoints The current total points for the red team.
	 * @param isRedTeam Indicates if the player belongs to the red team.
	 * @param player The {@code Player} instance receiving the update.
	 * @param playerPoints The current points held by the specific player.
	 */
	public ExCubeGameExtendedChangePoints(int timeLeft, int bluePoints, int redPoints, boolean isRedTeam, Player player, int playerPoints)
	{
		_timeLeft = timeLeft;
		_bluePoints = bluePoints;
		_redPoints = redPoints;
		_isRedTeam = isRedTeam;
		_player = player;
		_playerPoints = playerPoints;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_BLOCK_UPSET_STATE.writeId(this, buffer);
		buffer.writeInt(0);
		buffer.writeInt(_timeLeft);
		buffer.writeInt(_bluePoints);
		buffer.writeInt(_redPoints);
		buffer.writeInt(_isRedTeam);
		buffer.writeInt(_player.getObjectId());
		buffer.writeInt(_playerPoints);
	}
}
