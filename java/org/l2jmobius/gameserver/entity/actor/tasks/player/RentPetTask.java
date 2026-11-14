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
 * This task handles the logic for dismounting a {@code Player} from a rented pet.<br>
 * It ensures that the player is properly detached when their rental period ends or is cancelled.
 * @author UnAfraid
 */
public class RentPetTask implements Runnable
{
	private final Player _player;
	
	/**
	 * Creates a new task to handle dismounting a player from a rented pet.<br>
	 * This constructor initializes the task with the specific {@code Player} involved.
	 * @param player The {@code Player} who will be dismounted from the pet.
	 */
	public RentPetTask(Player player)
	{
		_player = player;
	}
	
	@Override
	public void run()
	{
		if (_player != null)
		{
			_player.stopRentPet();
		}
	}
}
