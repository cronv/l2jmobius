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
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Handles the {@code ChannelDelete} user command.<br>
 * This class allows a player to delete a specific channel from their list.<br>
 * It processes the request and updates the player's accessible channels.
 * @author Chris
 */
public class ChannelDelete implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		93
	};
	
	/**
	 * Handles the command to delete a channel.<br>
	 * This method checks if the {@code player} is the leader of a party in a command channel.<br>
	 * If valid, it disbands the channel and sends a system message.
	 * @param id The unique identifier for the command.
	 * @param player The {@code Player} who executed the command.
	 * @return {@code true} if the command was successfully processed, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(int id, Player player)
	{
		if (id != COMMAND_IDS[0])
		{
			return false;
		}
		
		final Party party = player.getParty();
		if ((party != null) && party.isLeader(player) && party.isInCommandChannel() && party.getCommandChannel().getLeader().equals(player))
		{
			final CommandChannel channel = party.getCommandChannel();
			channel.broadcastPacket(new SystemMessage(SystemMessageId.THE_COMMAND_CHANNEL_IS_DISBANDED));
			channel.disbandChannel();
			return true;
		}
		
		return false;
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
