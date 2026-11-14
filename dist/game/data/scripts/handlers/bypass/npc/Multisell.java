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
package handlers.bypass.npc;

import java.util.logging.Level;

import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IBypassHandler;

/**
 * Handles the logic for {@code Multisell} NPC bypasses.<br>
 * This class processes requests to sell multiple items at once from a specific vendor.<br>
 * It interacts with {@code MultisellData} to validate and execute bulk sales.
 */
public class Multisell implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"multisell",
		"exc_multisell"
	};
	
	/**
	 * Processes a command sent by a {@code Player}.<br>
	 * This method handles multisell and exclusive multisell actions.<br>
	 * It checks if the {@code target} is an NPC before executing.
	 * @param command The string command entered by the player.
	 * @param player The {@code Player} who sent the command.
	 * @param target The {@code Creature} being interacted with.
	 * @return {@code true} if the command was processed successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player player, Creature target)
	{
		if (!target.isNpc())
		{
			return false;
		}
		
		try
		{
			int listId;
			final String lowerCommand = command.toLowerCase();
			if (lowerCommand.startsWith(COMMANDS[0])) // multisell
			{
				listId = Integer.parseInt(command.substring(9).trim());
				MultisellData.getInstance().separateAndSend(listId, player, target.asNpc(), false);
				return true;
			}
			else if (lowerCommand.startsWith(COMMANDS[1])) // exc_multisell
			{
				listId = Integer.parseInt(command.substring(13).trim());
				MultisellData.getInstance().separateAndSend(listId, player, target.asNpc(), true);
				return true;
			}
			
			return false;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception in " + getClass().getSimpleName(), e);
		}
		
		return false;
	}
	
	/**
	 * Retrieves the list of valid commands for this handler.<br>
	 * These are the strings that trigger the {@code onCommand} method.
	 * @return An array of {@code String} containing all supported commands.
	 */
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}
