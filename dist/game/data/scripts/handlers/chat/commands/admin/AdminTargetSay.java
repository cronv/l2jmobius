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
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.StaticObject;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;

/**
 * Handles the {@code targetsay} admin command.<br>
 * This command allows an administrator to make a {@code Creature} speak a specific message.
 * @author nonom
 */
public class AdminTargetSay implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_targetsay"
	};
	
	/**
	 * Processes an admin command sent by a player.<br>
	 * This method checks if the command is {@code admin_targetsay}.<br>
	 * It sends a message to the target creature if valid.
	 * @param command The raw command string received from the client.
	 * @param activeChar The {@code Player} who sent the command.
	 * @return {@code true} if the command was handled successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_targetsay"))
		{
			try
			{
				final WorldObject obj = activeChar.getTarget();
				if ((obj instanceof StaticObject) || !obj.isCreature())
				{
					activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
					return false;
				}
				
				final String message = command.substring(16);
				final Creature target = obj.asCreature();
				target.broadcastPacket(new CreatureSay(target, target.isPlayer() ? ChatType.GENERAL : ChatType.NPC_GENERAL, target.getName(), message));
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendSysMessage("Usage: //targetsay <text>");
				return false;
			}
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
}
