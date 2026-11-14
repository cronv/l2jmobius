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

import java.util.Objects;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.request.SayuneRequest;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.network.serverpackets.sayune.ExNotifyFlyMoveStart;

/**
 * Handles the initial logic when a player begins moving while in flight.<br>
 * This task ensures that {@code ExNotifyFlyMoveStart} is sent to the client.<br>
 * It manages the transition of movement states for the {@code Player}.
 * @author UnAfraid
 */
public class FlyMoveStartTask implements Runnable
{
	private final Player _player;
	private final ZoneType _zone;
	
	/**
	 * Initializes a new task to handle the start of flight movement.<br>
	 * This constructor sets up the required {@code Player} and {@code ZoneType}.<br>
	 * It ensures that neither parameter is {@code null}.
	 * @param zone The type of zone where the action occurs.
	 * @param player The player who is starting the flight move.
	 */
	public FlyMoveStartTask(ZoneType zone, Player player)
	{
		Objects.requireNonNull(zone);
		Objects.requireNonNull(player);
		_player = player;
		_zone = zone;
	}
	
	@Override
	public void run()
	{
		if (!_zone.isCharacterInZone(_player))
		{
			return;
		}
		
		if (!_player.hasRequest(SayuneRequest.class))
		{
			_player.sendPacket(ExNotifyFlyMoveStart.STATIC_PACKET);
			ThreadPool.schedule(this, 1000);
		}
	}
}
