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
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;

/**
 * This class handles administrative commands for repairing characters.<br>
 * It allows administrators to restore character data or perform related maintenance tasks.
 */
public class AdminRepairChar implements IAdminCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(AdminRepairChar.class.getName());
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_restore",
		"admin_repair"
	};
	
	/**
	 * Processes an admin command for repairing a character.<br>
	 * This method calls the internal repair logic.
	 * @param command The name of the command to execute.
	 * @param activeChar The {@code Player} who is executing the command.
	 * @return {@code true} if the command was processed successfully.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		handleRepair(command);
		return true;
	}
	
	/**
	 * Retrieves the list of available admin commands.<br>
	 * This method returns all strings defined in {@code ADMIN_COMMANDS}.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	/**
	 * Processes the command to repair a character's position and items.<br>
	 * It updates coordinates in the database for the specified name.<br>
	 * It also clears shortcuts and moves all items to the inventory.
	 * @param command The full command string containing the character name.
	 */
	private void handleRepair(String command)
	{
		final String[] parts = command.split(" ");
		if (parts.length != 2)
		{
			return;
		}
		
		final String cmd = "UPDATE characters SET x=-84318, y=244579, z=-3730 WHERE char_name=?";
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement(cmd);
			statement.setString(1, parts[1]);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("SELECT charId FROM characters where char_name=?");
			statement.setString(1, parts[1]);
			final ResultSet rset = statement.executeQuery();
			int objId = 0;
			if (rset.next())
			{
				objId = rset.getInt(1);
			}
			
			rset.close();
			statement.close();
			
			if (objId == 0)
			{
				// con.close();
				return;
			}
			
			// connection = DatabaseFactory.getConnection();
			statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE charId=?");
			statement.setInt(1, objId);
			statement.execute();
			statement.close();
			
			// connection = DatabaseFactory.getConnection();
			statement = con.prepareStatement("UPDATE items SET loc=\"INVENTORY\" WHERE owner_id=?");
			statement.setInt(1, objId);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "could not repair char:", e);
		}
	}
}
