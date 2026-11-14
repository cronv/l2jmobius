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
 * This task is responsible for resetting the current souls of a {@code Player}.<br>
 * It handles the logic required to clear the player's soul count.
 * @author UnAfraid
 */
public class ResetSoulsTask implements Runnable
{
	private final Player _player;
	
	/**
	 * Creates a new task to reset the souls of a specific player.<br>
	 * This constructor initializes the task with the target {@code Player}.
	 * @param player The {@code Player} whose souls will be reset.
	 */
	public ResetSoulsTask(Player player)
	{
		_player = player;
	}
	
	@Override
	public void run()
	{
		if (_player != null)
		{
			_player.clearSouls();
		}
	}
}
