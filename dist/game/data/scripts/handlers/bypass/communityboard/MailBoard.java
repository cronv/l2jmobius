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
package handlers.bypass.communityboard;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IWriteBoardHandler;

/**
 * Handles the logic for the mail board system.<br>
 * This class manages how players interact with and write to the community mail board.
 * @author Zoey76
 */
public class MailBoard implements IWriteBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_maillist"
	};
	
	/**
	 * Retrieves the list of valid commands for this board.<br>
	 * This method returns all available command strings.
	 * @return An array of {@code String} containing the allowed commands.
	 */
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
	
	/**
	 * Handles the execution of a specific command.<br>
	 * This method processes the input from a {@code Player}.<br>
	 * It sends an HTML message to the user.
	 * @param command The string representing the command entered by the player.
	 * @param player The {@code Player} object who executed the command.
	 * @return {@code true} if the command was handled successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player player)
	{
		CommunityBoardHandler.getInstance().addBypass(player, "Mail Command", command);
		
		final String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/mail.html");
		CommunityBoardHandler.separateAndSend(html, player);
		return true;
	}
	
	/**
	 * Processes a command to write content to the community board.<br>
	 * This method handles the input provided by a {@code Player}.<br>
	 * It currently returns {@code false} as it is not yet implemented.
	 * @param player The {@code Player} who sent the command.
	 * @param arg1 The first argument of the command.
	 * @param arg2 The second argument of the command.
	 * @param arg3 The third argument of the command.
	 * @param arg4 The fourth argument of the command.
	 * @param arg5 The fifth argument of the command.
	 * @return {@code true} if the command was successful, otherwise {@code false}.
	 */
	@Override
	public boolean writeCommunityBoardCommand(Player player, String arg1, String arg2, String arg3, String arg4, String arg5)
	{
		// TODO: Implement.
		return false;
	}
}
