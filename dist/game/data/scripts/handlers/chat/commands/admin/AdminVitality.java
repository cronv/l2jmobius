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

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;

/**
 * This class handles the admin command for modifying player vitality.<br>
 * It allows administrators to set or adjust {@code PlayerStat} values for a specific {@code Player}.
 * @author Psychokiller1888
 */
public class AdminVitality implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_set_vitality",
		"admin_full_vitality",
		"admin_empty_vitality",
		"admin_get_vitality"
	};
	
	/**
	 * Processes admin commands related to player vitality.<br>
	 * Checks if the command is valid and if the target is a {@code Player}.<br>
	 * Executes actions like setting, filling, or checking vitality points.
	 * @param command The full command string entered by the administrator.
	 * @param activeChar The {@code Player} who is executing the command.
	 * @return {@code true} if the command was executed successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (activeChar == null)
		{
			return false;
		}
		
		if (!PlayerConfig.ENABLE_VITALITY)
		{
			activeChar.sendSysMessage("Vitality is not enabled on the server!");
			return false;
		}
		
		int vitality = 0;
		
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String cmd = st.nextToken();
		if ((activeChar.getTarget() != null) && activeChar.getTarget().isPlayer())
		{
			final Player target = activeChar.getTarget().asPlayer();
			if (cmd.equals("admin_set_vitality"))
			{
				try
				{
					vitality = Integer.parseInt(st.nextToken());
				}
				catch (Exception e)
				{
					activeChar.sendSysMessage("Incorrect vitality");
				}
				
				target.setVitalityPoints(vitality, true);
				target.sendMessage("Admin set your Vitality points to " + vitality);
			}
			else if (cmd.equals("admin_full_vitality"))
			{
				target.setVitalityPoints(PlayerStat.MAX_VITALITY_POINTS, true);
				target.sendMessage("Admin completly recharged your Vitality");
			}
			else if (cmd.equals("admin_empty_vitality"))
			{
				target.setVitalityPoints(PlayerStat.MIN_VITALITY_POINTS, true);
				target.sendMessage("Admin completly emptied your Vitality");
			}
			else if (cmd.equals("admin_get_vitality"))
			{
				vitality = target.getVitalityPoints();
				activeChar.sendSysMessage("Player vitality points: " + vitality);
			}
			
			return true;
		}
		
		activeChar.sendSysMessage("Target not found or not a player");
		return false;
	}
	
	/**
	 * Retrieves the list of available admin commands.<br>
	 * This method returns all valid command strings for this handler.
	 * @return An array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
