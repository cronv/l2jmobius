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
package handlers.chat.commands.admin;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;

/**
 * This class handles the {@code character_disconnect} admin command.<br>
 * It allows administrators to disconnect a target player from the game world.
 */
public class AdminDisconnect implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_character_disconnect"
	};
	
	/**
	 * Processes an admin command sent by a player.<br>
	 * This method checks if the {@code command} matches the allowed list.<br>
	 * It executes the corresponding action on the {@code activeChar}.
	 * @param command The name of the command to execute.
	 * @param activeChar The {@code Player} who is sending the command.
	 * @return {@code true} if the command was handled successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.equals("admin_character_disconnect"))
		{
			disconnectCharacter(activeChar);
		}
		
		return true;
	}
	
	/**
	 * Retrieves the list of available admin commands.<br>
	 * This method returns all command strings handled by this class.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	/**
	 * Disconnects a target player from the server.<br>
	 * This method checks if the target is a {@code Player}.<br>
	 * It prevents an admin from disconnecting their own character.
	 * @param activeChar The {@code Player} who is executing the command.
	 */
	private void disconnectCharacter(Player activeChar)
	{
		final WorldObject target = activeChar.getTarget();
		Player player = null;
		if ((target != null) && target.isPlayer())
		{
			player = target.asPlayer();
		}
		else
		{
			return;
		}
		
		if (player == activeChar)
		{
			activeChar.sendSysMessage("You cannot logout your own character.");
		}
		else
		{
			activeChar.sendSysMessage("Character " + player.getName() + " disconnected from server.");
			Disconnection.of(player).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
		}
	}
}
