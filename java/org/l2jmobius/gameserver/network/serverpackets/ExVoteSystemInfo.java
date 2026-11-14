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
import org.l2jmobius.gameserver.entity.actor.holders.player.Recommendations;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This class handles the {@code ExVoteSystemInfo} server packet.<br>
 * It provides information regarding the current state of the voting system to the client.
 * @author Gnacik
 */
public class ExVoteSystemInfo extends ServerPacket
{
	private final int _recomLeft;
	private final int _recomHave;
	private final int _bonusTime;
	private final int _bonusVal;
	private final int _bonusType;
	
	/**
	 * Creates a new {@code ExVoteSystemInfo} packet for a specific player.<br>
	 * This constructor initializes the recommendation counts from the {@code Player} object.<br>
	 * It sets all bonus related values to {@code 0}.
	 * @param player The {@code Player} instance used to retrieve current data.
	 */
	public ExVoteSystemInfo(Player player)
	{
		final Recommendations recommendations = player.getRecommendations();
		_recomLeft = recommendations.getLeft();
		_recomHave = recommendations.getHave();
		_bonusTime = 0;
		_bonusVal = 0;
		_bonusType = 0;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_VOTE_SYSTEM_INFO.writeId(this, buffer);
		buffer.writeInt(_recomLeft);
		buffer.writeInt(_recomHave);
		buffer.writeInt(_bonusTime);
		buffer.writeInt(_bonusVal);
		buffer.writeInt(_bonusType);
	}
}
