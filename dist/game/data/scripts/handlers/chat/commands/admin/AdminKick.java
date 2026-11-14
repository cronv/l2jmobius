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

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;

/**
 * Handles the admin command to disconnect a player from the game world.<br>
 * It processes the target player's name and sends a {@code LeaveWorld} packet.<br>
 * This allows administrators to remove players for moderation purposes.
 */
public class AdminKick implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_kick",
		"admin_kick_non_gm"
	};
	
	/**
	 * Processes administrative kick commands.<br>
	 * This method handles both single and mass player kicks.<br>
	 * It returns {@code true} if the command was handled successfully.
	 * @param command The raw string input from the admin.
	 * @param activeChar The {@code Player} who sent the command.
	 * @return {@code true} if the command was processed, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_kick"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			if (st.countTokens() > 1)
			{
				st.nextToken();
				final String player = st.nextToken();
				final Player plyr = World.getPlayer(player);
				if (plyr != null)
				{
					Disconnection.of(plyr).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
					activeChar.sendSysMessage("You kicked " + plyr.getName() + " from the game.");
				}
			}
		}
		
		if (command.startsWith("admin_kick_non_gm"))
		{
			int counter = 0;
			for (Player player : World.getPlayers())
			{
				if (!player.isGM())
				{
					counter++;
					Disconnection.of(player).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
				}
			}
			
			activeChar.sendSysMessage("Kicked " + counter + " players.");
		}
		
		return true;
	}
	
	/**
	 * Retrieves the list of valid commands for this handler.<br>
	 * This method returns all strings that trigger an {@code AdminKick} action.
	 * @return a {@code String[]} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
