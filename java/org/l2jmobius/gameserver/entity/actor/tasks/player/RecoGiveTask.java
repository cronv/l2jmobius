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
package org.l2jmobius.gameserver.entity.actor.tasks.player;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.Recommendations;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This task is responsible for increasing the recommendation bonus of a {@code Player}.<br>
 * It handles the logic required to update the player's reward status.
 * @author UnAfraid
 */
public class RecoGiveTask implements Runnable
{
	private final Player _player;
	
	/**
	 * Creates a new task to increase the recommendation bonus.<br>
	 * This constructor initializes the task for a specific {@code Player}.
	 * @param player The {@code Player} who will receive the bonus.
	 */
	public RecoGiveTask(Player player)
	{
		_player = player;
	}
	
	@Override
	public void run()
	{
		if (_player != null)
		{
			// 10 recommendations to give out after 2 hours of being logged in.
			// 1 more recommendation to give out every hour after that.
			int recoToGive = 1;
			final Recommendations recommendations = _player.getRecommendations();
			if (!recommendations.isTwoHoursGiven())
			{
				recoToGive = 10;
				recommendations.setTwoHoursGiven(true);
			}
			
			recommendations.setLeft(recommendations.getLeft() + recoToGive);
			
			final SystemMessage sm = new SystemMessage(SystemMessageId.RECOMMENDATIONS_OBTAINED_S1);
			sm.addInt(recoToGive);
			_player.sendPacket(sm);
			_player.updateUserInfo();
		}
	}
}
