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
import org.l2jmobius.gameserver.entity.groups.CommandChannel;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.handler.IUserCommandHandler;
import org.l2jmobius.gameserver.network.serverpackets.ExMultiPartyCommandChannelInfo;

/**
 * Handles the {@code ChannelInfo} user command.<br>
 * This class provides information about a specific {@code CommandChannel}.
 * @author chris_00
 */
public class ChannelInfo implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		97
	};
	
	/**
	 * Handles the execution of a user command.<br>
	 * It checks if the {@code id} matches the expected command ID.<br>
	 * It sends channel information to the {@code Player}.
	 * @param id The unique identifier for the command.
	 * @param player The {@code Player} who sent the command.
	 * @return {@code true} if the command was processed successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(int id, Player player)
	{
		if (id != COMMAND_IDS[0])
		{
			return false;
		}
		
		final Party party = player.getParty();
		if ((party == null) || (party.getCommandChannel() == null))
		{
			return false;
		}
		
		final CommandChannel channel = party.getCommandChannel();
		player.sendPacket(new ExMultiPartyCommandChannelInfo(channel));
		return true;
	}
	
	/**
	 * Retrieves the list of command IDs for this handler.<br>
	 * These IDs are used to identify which commands this class processes.
	 * @return an {@code int[]} array containing the valid command IDs.
	 */
	@Override
	public int[] getCommandList()
	{
		return COMMAND_IDS;
	}
}
