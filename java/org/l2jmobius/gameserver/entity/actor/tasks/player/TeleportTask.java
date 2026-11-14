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

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * Handles the logic for teleporting a {@code Player} to a new location.<br>
 * This task ensures the movement is processed correctly within the game world.
 * @author UnAfraid
 */
public class TeleportTask implements Runnable
{
	private final Player _player;
	private final Location _loc;
	
	/**
	 * Creates a new task to teleport a player.<br>
	 * This task stores the target destination for the movement.
	 * @param player The {@code Player} who will be moved.
	 * @param loc The {@code Location} where the player should land.
	 */
	public TeleportTask(Player player, Location loc)
	{
		_player = player;
		_loc = loc;
	}
	
	@Override
	public void run()
	{
		if ((_player != null) && _player.isOnline())
		{
			_player.teleToLocation(_loc, true);
		}
	}
}
