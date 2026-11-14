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
package handlers.chat.commands.user;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IUserCommandHandler;

/**
 * Handles the {@code dismount} chat command for players.<br>
 * This allows a player to get off their current mount.<br>
 * It checks if the user is currently mounted before executing the action.
 * @author Micht
 */
public class Dismount implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		62
	};
	
	/**
	 * Handles the execution of a specific user command.<br>
	 * This method checks the {@code id} and performs actions on the {@code Player}.<br>
	 * It handles dismounting or stopping rented pets based on the player's state.
	 * @param id The unique identifier for the command.
	 * @param player The {@code Player} instance who sent the command.
	 * @return {@code true} if the command was processed successfully, otherwise {@code false}.
	 */
	@Override
	public synchronized boolean onCommand(int id, Player player)
	{
		if (id != COMMAND_IDS[0])
		{
			return false;
		}
		
		if (player.isRentedPet())
		{
			player.stopRentPet();
		}
		else if (player.isMounted())
		{
			player.dismount();
		}
		
		return true;
	}
	
	/**
	 * Retrieves the list of command IDs for this handler.<br>
	 * This method returns the internal {@code COMMAND_IDS} array.
	 * @return an {@code int[]} containing the valid command IDs.
	 */
	@Override
	public int[] getCommandList()
	{
		return COMMAND_IDS;
	}
}
