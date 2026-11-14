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

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.network.serverpackets.ensoul.ExShowEnsoulExtractionWindow;
import org.l2jmobius.gameserver.network.serverpackets.ensoul.ExShowEnsoulWindow;

/**
 * Handles the logic for displaying the ensoul extraction window to players.<br>
 * This class processes bypass requests related to {@code ExShowEnsoulWindow}.
 * @author St3eT, Mobius
 */
public class EnsoulWindow implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"show_ensoul_window",
		"show_extract_ensoul_window"
	};
	
	/**
	 * Processes a command sent by a {@code Player}.<br>
	 * It checks if the {@code target} is an NPC.<br>
	 * If valid, it sends a specific packet to the player.
	 * @param command The name of the command being executed.
	 * @param player The player who sent the command.
	 * @param target The creature entity targeted by the command.
	 * @return {@code true} if the command was successful, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player player, Creature target)
	{
		if (!target.isNpc())
		{
			return false;
		}
		
		final String lowerCommand = command.toLowerCase();
		if (lowerCommand.startsWith(COMMANDS[0])) // show_ensoul_window
		{
			player.sendPacket(ExShowEnsoulWindow.STATIC_PACKET);
			return true;
		}
		else if (lowerCommand.startsWith(COMMANDS[1])) // show_extract_ensoul_window
		{
			player.sendPacket(ExShowEnsoulExtractionWindow.STATIC_PACKET);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Retrieves the list of valid commands for this handler.<br>
	 * These commands are used to identify supported actions.
	 * @return a {@code String[]} containing all available command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}
