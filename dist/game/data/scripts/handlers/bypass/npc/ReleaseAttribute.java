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
import org.l2jmobius.gameserver.network.serverpackets.ExShowBaseAttributeCancelWindow;

/**
 * Handles the logic for releasing an attribute from a {@code Creature}.<br>
 * This handler is triggered when a player interacts with a specific NPC bypass.<br>
 * It manages the removal of attributes and closes the associated window.
 */
public class ReleaseAttribute implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"ReleaseAttribute"
	};
	
	/**
	 * Handles the {@code ReleaseAttribute} command.<br>
	 * Checks if the target is an NPC.<br>
	 * Sends a cancellation window to the player if successful.
	 * @param command The name of the command being executed.
	 * @param player The player who sent the command.
	 * @param target The creature targeted by the player.
	 * @return {@code true} if the command was processed, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player player, Creature target)
	{
		if (!target.isNpc())
		{
			return false;
		}
		
		player.sendPacket(new ExShowBaseAttributeCancelWindow(player));
		return true;
	}
	
	/**
	 * Retrieves the list of valid commands for this handler.<br>
	 * This method returns the {@code COMMANDS} array.
	 * @return an array of {@code String} containing all supported commands.
	 */
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}
