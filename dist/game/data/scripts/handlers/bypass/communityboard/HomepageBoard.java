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
 * Handles the logic for the homepage community board.<br>
 * This class manages how players interact with the main board content.
 * @author Zoey76
 */
public class HomepageBoard implements IParseBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_bbslink"
	};
	
	/**
	 * Retrieves the list of valid commands for this board.<br>
	 * This method returns the internal {@code COMMANDS} array.
	 * @return an array of {@code String} objects representing the allowed commands.
	 */
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
	
	/**
	 * Handles the execution of a specific command.<br>
	 * This method processes the input from a {@code Player}.<br>
	 * It sends the corresponding HTML content to the user.
	 * @param command The string name of the command being executed.
	 * @param player The {@code Player} who sent the command.
	 * @return {@code true} if the command was handled successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player player)
	{
		CommunityBoardHandler.separateAndSend(HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/homepage.html"), player);
		return true;
	}
}
