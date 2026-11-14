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

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;

/**
 * This class handles the {@code //gmspeed} admin command.<br>
 * It provides a custom implementation to modify player movement speed.<br>
 * It is based on the classical L2J style for handling super haste.
 * @author lord_rex
 */
public class AdminSuperHaste implements IAdminCommandHandler
{
	static final String[] ADMIN_COMMANDS =
	{
		"admin_superhaste",
		"admin_superhaste_menu",
		"admin_speed",
		"admin_speed_menu",
	};
	
	private static final int SUPER_HASTE_ID = 7029;
	
	/**
	 * Processes the admin command sent by a player.<br>
	 * It handles speed and super haste modifications.
	 * @param command The raw string input from the player.
	 * @param player The {@code Player} object who executed the command.
	 * @return {@code true} if the command was handled successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player player)
	{
		final StringTokenizer st = new StringTokenizer(command);
		final String cmd = st.nextToken();
		switch (cmd)
		{
			case "admin_superhaste":
			case "admin_speed":
			{
				try
				{
					final int val = Integer.parseInt(st.nextToken());
					final boolean sendMessage = player.isAffectedBySkill(SUPER_HASTE_ID);
					player.stopSkillEffects((val == 0) && sendMessage ? SkillFinishType.REMOVED : SkillFinishType.NORMAL, SUPER_HASTE_ID);
					if ((val >= 1) && (val <= 4))
					{
						int time = 0;
						if (st.hasMoreTokens())
						{
							time = Integer.parseInt(st.nextToken());
						}
						
						final Skill superHasteSkill = SkillData.getInstance().getSkill(SUPER_HASTE_ID, val);
						superHasteSkill.applyEffects(player, player, true, time);
					}
				}
				catch (Exception e)
				{
					player.sendMessage("Usage: //superhaste <Effect level (0-4)> <Time in seconds>");
				}
				break;
			}
			case "admin_superhaste_menu":
			case "admin_speed_menu":
			{
				AdminHtml.showAdminHtml(player, "gm_menu.htm");
				break;
			}
		}
		
		return true;
	}
	
	/**
	 * Retrieves the list of valid commands for this handler.<br>
	 * This method returns all registered command strings.
	 * @return an array of {@code String} containing the allowed commands.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
