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
package handlers.chat.commands.voiced;

import org.l2jmobius.gameserver.config.custom.OnlineInfoConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;

/**
 * Handles the voiced chat command to display information about currently online players.<br>
 * It retrieves and broadcasts the list of active users in the game world.
 * @author Mobius
 */
public class Online implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"online"
	};
	
	/**
	 * Handles the execution of a specific voice command.<br>
	 * This method checks if the {@code command} matches the allowed list.<br>
	 * It sends a message to the {@code player} regarding the current online count.
	 * @param command The name of the command being executed.
	 * @param player The {@code Player} who sent the command.
	 * @param target The target of the command, if any.
	 * @return {@code true} if the command was handled successfully.
	 */
	@Override
	public boolean onCommand(String command, Player player, String target)
	{
		if (command.equals("online") && OnlineInfoConfig.ENABLE_ONLINE_COMMAND)
		{
			final int count = World.getPlayers().size();
			if (count > 1)
			{
				player.sendMessage("There are " + count + " players online!");
			}
			else
			{
				player.sendMessage("There is 1 player online!");
			}
		}
		
		return true;
	}
	
	/**
	 * Retrieves the list of available voiced commands.<br>
	 * This method returns all valid command strings for this handler.
	 * @return an array of {@code String} containing the supported commands.
	 */
	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}
