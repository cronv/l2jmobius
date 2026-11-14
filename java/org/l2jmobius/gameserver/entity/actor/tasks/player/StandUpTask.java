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
 * This task is responsible for making a {@code Player} character stand up.<br>
 * It handles the transition from a crouching or lying state to a standing position.
 * @author UnAfraid
 */
public class StandUpTask implements Runnable
{
	private final Player _player;
	
	/**
	 * Creates a new task to make a player stand up.<br>
	 * This constructor initializes the task with a specific {@code Player}.
	 * @param player The {@code Player} who will perform the stand up action.
	 */
	public StandUpTask(Player player)
	{
		_player = player;
	}
	
	@Override
	public void run()
	{
		if (_player == null)
		{
			return;
		}
		
		_player.setBlockActions(false);
		_player.setSitting(false);
		_player.setSittingProgress(false);
		_player.getAI().setIntentionIdle();
	}
}
