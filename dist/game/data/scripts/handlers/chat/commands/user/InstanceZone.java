/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package handlers.chat.commands.user;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IUserCommandHandler;
import org.l2jmobius.gameserver.network.serverpackets.ExInzoneWaiting;

/**
 * Handles the {@code InstanceZone} user chat command.<br>
 * This class manages logic for players interacting with instance zone features.
 * @author nille02, UnAfraid
 */
public class InstanceZone implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		90
	};
	
	/**
	 * Retrieves the list of command IDs for this handler.<br>
	 * These IDs are used to identify valid commands.
	 * @return an {@code int[]} array containing the command IDs.
	 */
	@Override
	public int[] getCommandList()
	{
		return COMMAND_IDS;
	}
	
	/**
	 * Handles the execution of a specific user command.<br>
	 * This method checks if the provided {@code id} is valid.<br>
	 * It sends an {@code ExInzoneWaiting} packet to the {@code Player}.
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
		
		player.sendPacket(new ExInzoneWaiting(player, false));
		return true;
	}
}
