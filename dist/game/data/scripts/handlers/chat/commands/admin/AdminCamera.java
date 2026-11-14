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
package handlers.chat.commands.admin;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Handles administrative commands related to the game camera.<br>
 * This class allows admins to manipulate camera perspectives and positions.
 * @author Zoey76
 */
public class AdminCamera implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_cam",
		"admin_camex",
		"admin_cam3"
	};
	
	/**
	 * Processes admin camera commands for a specific player.<br>
	 * This method validates the target and executes the corresponding camera effect.
	 * @param command The full command string entered by the user.
	 * @param activeChar The {@code Player} who is executing the command.
	 * @return {@code true} if the command was executed successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if ((activeChar.getTarget() == null) || !activeChar.getTarget().isCreature())
		{
			activeChar.sendPacket(SystemMessageId.YOUR_TARGET_CANNOT_BE_FOUND);
			return false;
		}
		
		final Creature target = activeChar.getTarget().asCreature();
		final String[] com = command.split(" ");
		switch (com[0])
		{
			case "admin_cam":
			{
				if (com.length != 12)
				{
					activeChar.sendSysMessage("Usage: //cam force angle1 angle2 time range duration relYaw relPitch isWide relAngle");
					return false;
				}
				
				Quest.specialCamera(activeChar, target, Integer.parseInt(com[1]), Integer.parseInt(com[2]), Integer.parseInt(com[3]), Integer.parseInt(com[4]), Integer.parseInt(com[5]), Integer.parseInt(com[6]), Integer.parseInt(com[7]), Integer.parseInt(com[8]), Integer.parseInt(com[9]), Integer.parseInt(com[10]));
				break;
			}
			case "admin_camex":
			{
				if (com.length != 10)
				{
					activeChar.sendSysMessage("Usage: //camex force angle1 angle2 time duration relYaw relPitch isWide relAngle");
					return false;
				}
				
				Quest.specialCameraEx(activeChar, target, Integer.parseInt(com[1]), Integer.parseInt(com[2]), Integer.parseInt(com[3]), Integer.parseInt(com[4]), Integer.parseInt(com[5]), Integer.parseInt(com[6]), Integer.parseInt(com[7]), Integer.parseInt(com[8]), Integer.parseInt(com[9]));
				break;
			}
			case "admin_cam3":
			{
				if (com.length != 12)
				{
					activeChar.sendSysMessage("Usage: //cam3 force angle1 angle2 time range duration relYaw relPitch isWide relAngle unk");
					return false;
				}
				
				Quest.specialCamera3(activeChar, target, Integer.parseInt(com[1]), Integer.parseInt(com[2]), Integer.parseInt(com[3]), Integer.parseInt(com[4]), Integer.parseInt(com[5]), Integer.parseInt(com[6]), Integer.parseInt(com[7]), Integer.parseInt(com[8]), Integer.parseInt(com[9]), Integer.parseInt(com[10]), Integer.parseInt(com[11]));
				break;
			}
		}
		
		return true;
	}
	
	/**
	 * Retrieves the list of available admin camera commands.<br>
	 * This method returns all registered command strings for this handler.
	 * @return An array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
