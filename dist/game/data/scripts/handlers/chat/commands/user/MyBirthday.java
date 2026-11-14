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

import java.util.Calendar;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IUserCommandHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Handles the {@code /mybirthday} user command.<br>
 * This class allows a player to display their birthday information in the game chat.
 * @author JIV
 */
public class MyBirthday implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		126
	};
	
	/**
	 * Handles the execution of a user command.<br>
	 * This method checks if the provided {@code id} matches the registered command.<br>
	 * It sends a birthday message to the {@code Player}.
	 * @param id The unique identifier for the command.
	 * @param player The {@code Player} who executed the command.
	 * @return {@code true} if the command was handled successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(int id, Player player)
	{
		if (id != COMMAND_IDS[0])
		{
			return false;
		}
		
		final Calendar date = player.getCreateDate();
		
		final SystemMessage sm = new SystemMessage(SystemMessageId.C1_S_BIRTHDAY_IS_S3_S4_S2);
		sm.addPcName(player);
		sm.addString(Integer.toString(date.get(Calendar.YEAR)));
		sm.addString(Integer.toString(date.get(Calendar.MONTH) + 1));
		sm.addString(Integer.toString(date.get(Calendar.DATE)));
		
		player.sendPacket(sm);
		return true;
	}
	
	/**
	 * Retrieves the list of command IDs for this handler.<br>
	 * These IDs are used to identify which commands this class handles.
	 * @return an {@code int[]} containing the valid command IDs.
	 */
	@Override
	public int[] getCommandList()
	{
		return COMMAND_IDS;
	}
}
