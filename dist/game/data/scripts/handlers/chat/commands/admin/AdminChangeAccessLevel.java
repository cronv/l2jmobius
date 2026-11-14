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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.holders.AccessLevel;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;

/**
 * Handles the admin command to change a user's access level.<br>
 * This class updates the {@code AccessLevel} for specific accounts in the database.
 */
public class AdminChangeAccessLevel implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_changelvl"
	};
	
	/**
	 * Processes the command to change a player's access level.<br>
	 * It supports targeting a selected object or a specific name.
	 * @param command The full command string provided by the user.
	 * @param activeChar The {@code Player} who is executing the command.
	 * @return {@code true} if the command was processed successfully.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final String[] parts = command.split(" ");
		if (parts.length == 2)
		{
			try
			{
				final int lvl = Integer.parseInt(parts[1]);
				final WorldObject target = activeChar.getTarget();
				if ((target == null) || !target.isPlayer())
				{
					activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
				}
				else
				{
					onlineChange(activeChar, target.asPlayer(), lvl);
				}
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Usage: //changelvl <target_new_level> | <player_name> <new_level>");
			}
		}
		else if (parts.length == 3)
		{
			final String name = parts[1];
			final int lvl = Integer.parseInt(parts[2]);
			final Player player = World.getPlayer(name);
			if (player != null)
			{
				onlineChange(activeChar, player, lvl);
			}
			else
			{
				try (Connection con = DatabaseFactory.getConnection())
				{
					final PreparedStatement statement = con.prepareStatement("UPDATE characters SET accesslevel=? WHERE char_name=?");
					statement.setInt(1, lvl);
					statement.setString(2, name);
					statement.execute();
					final int count = statement.getUpdateCount();
					statement.close();
					if (count == 0)
					{
						activeChar.sendSysMessage("Character not found or access level unaltered.");
					}
					else
					{
						activeChar.sendSysMessage("Character's access level is now set to " + lvl);
					}
				}
				catch (SQLException se)
				{
					activeChar.sendSysMessage("SQLException while changing character's access level");
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Retrieves the list of commands associated with this handler.<br>
	 * This method returns the {@code ADMIN_COMMANDS} array.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	/**
	 * Updates the access level of a player in real-time.<br>
	 * This method handles both valid level assignments and banning.<br>
	 * It sends confirmation messages to both the administrator and the target player.
	 * @param activeChar The {@code Player} who is executing the command.
	 * @param player The {@code Player} whose access level will be modified.
	 * @param lvl The new access level or a negative value to ban the player.
	 */
	private void onlineChange(Player activeChar, Player player, int lvl)
	{
		if (lvl >= 0)
		{
			final AccessLevel acccessLevel = AdminData.getInstance().getAccessLevel(lvl);
			if (acccessLevel != null)
			{
				player.setAccessLevel(lvl, true, true);
				final String accessLevelName = acccessLevel.getName();
				final int accessLevelValue = acccessLevel.getLevel();
				player.sendMessage("Your access level has been changed to " + accessLevelName + " (" + accessLevelValue + ").");
				activeChar.sendMessage(player.getName() + "'s access level has been changed to " + accessLevelName + " (" + accessLevelValue + ").");
			}
			else
			{
				activeChar.sendSysMessage("You are trying to set unexisting access level: " + lvl + " please try again with a valid one!");
			}
		}
		else
		{
			player.setAccessLevel(lvl, false, true);
			player.sendMessage("Your character has been banned. Bye.");
			Disconnection.of(player).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
		}
	}
}
