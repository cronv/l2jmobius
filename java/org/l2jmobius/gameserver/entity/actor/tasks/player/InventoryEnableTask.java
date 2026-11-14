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

/**
 * This task is responsible for enabling the inventory of a {@code Player}.<br>
 * It ensures that the player can interact with their items normally.
 * @author UnAfraid
 */
public class InventoryEnableTask implements Runnable
{
	private final Player _player;
	
	/**
	 * This constructor initializes a new {@code InventoryEnableTask}.<br>
	 * It assigns the provided {@code Player} to the task.
	 * @param player The {@code Player} instance whose inventory will be enabled.
	 */
	public InventoryEnableTask(Player player)
	{
		_player = player;
	}
	
	@Override
	public void run()
	{
		if (_player != null)
		{
			_player.setInventoryBlockingStatus(false);
		}
	}
}
