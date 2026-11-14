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

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.mechanics.olympiad.OlympiadMode;
import org.l2jmobius.gameserver.network.serverpackets.CameraMode;
import org.l2jmobius.gameserver.network.serverpackets.olympiad.ExOlympiadMode;

/**
 * Handles the free look camera command.<br>
 * Enables a mode where the player can fly the camera freely, which is useful for recording footage.<br>
 * This command toggles the effect on and off.
 * @author Mobius
 */
public class AdminFreeCam implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_freecam",
		"admin_freelook",
	};
	
	/**
	 * Processes the admin freecam command.<br>
	 * This method toggles the free look mode for a {@code Player}.<br>
	 * It handles both "on" and "off" arguments or simple toggling.
	 * @param command The command string sent by the player.
	 * @param activeChar The {@code Player} instance executing the command.
	 * @return {@code true} if the command was successfully processed, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final String[] parts = command.split(" ");
		final String arg = parts.length > 1 ? parts[1].toLowerCase() : "";
		
		// Explicit "off" or toggle while already active: leave.
		if (arg.equals("off") || (arg.isEmpty() && activeChar.isFreeLook()))
		{
			if (!activeChar.isFreeLook())
			{
				activeChar.sendSysMessage("Freelook camera is already disabled.");
				return false;
			}
			
			activeChar.setFreeLook(false);
			activeChar.setFlying(false);
			activeChar.sendPacket(new ExOlympiadMode(OlympiadMode.NONE));
			activeChar.sendPacket(new CameraMode(0));
			activeChar.broadcastCharInfo();
			activeChar.sendSysMessage("Freelook camera disabled.");
			return true;
		}
		
		// Explicit "on" or toggle while inactive: enter.
		if (activeChar.isFreeLook())
		{
			activeChar.sendSysMessage("Freelook camera is already enabled.");
			return false;
		}
		
		activeChar.setInvul(true);
		activeChar.setInvisible(true);
		activeChar.setFreeLook(true);
		activeChar.setFlying(true);
		activeChar.sendPacket(new CameraMode(1));
		activeChar.sendPacket(new ExOlympiadMode(OlympiadMode.SPECTATOR));
		activeChar.sendSysMessage("Freelook camera enabled.");
		return true;
	}
	
	/**
	 * Retrieves the list of valid commands for this handler.<br>
	 * These are the strings used to trigger the free camera functionality.
	 * @return an array of {@code String} containing all supported command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
