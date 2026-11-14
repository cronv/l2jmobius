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
import org.l2jmobius.gameserver.handler.IParseBoardHandler;

/**
 * Handles the logic for the friends community board.<br>
 * This class manages how players interact with the friends-specific board content.
 * @author Zoey76
 */
public class FriendsBoard implements IParseBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_friendlist",
		"_friendblocklist"
	};
	
	/**
	 * Retrieves the list of valid commands for this board.<br>
	 * These are the strings that {@code onCommand} will check against.
	 * @return a {@code String[]} containing all supported command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
	
	/**
	 * Processes a command sent by a {@code Player}.<br>
	 * It handles the {@code _friendlist} and {@code _friendblocklist} commands.<br>
	 * The method sends the corresponding HTML content to the player.
	 * @param command The string name of the command being executed.
	 * @param player The {@code Player} who sent the command.
	 * @return {@code true} if the command was handled successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player player)
	{
		if (command.equals("_friendlist"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Friends List", command);
			final String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/friends_list.html");
			CommunityBoardHandler.separateAndSend(html, player);
		}
		else if (command.equals("_friendblocklist"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Ignore list", command);
			final String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/friends_block_list.html");
			CommunityBoardHandler.separateAndSend(html, player);
		}
		
		return true;
	}
}
