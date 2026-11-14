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
package handlers.bypass.npc;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.network.enums.HtmlActionScope;
import org.l2jmobius.gameserver.network.serverpackets.TutorialCloseHtml;

/**
 * Handles the bypass action for closing a tutorial window.<br>
 * It processes requests to close specific HTML content and sends the {@code TutorialCloseHtml} packet.
 * @author UnAfraid
 */
public class TutorialClose implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"tutorial_close",
	};
	
	/**
	 * This method handles the execution of a specific command.<br>
	 * It closes the tutorial HTML for the player.<br>
	 * It returns {@code false} to indicate the command was handled.
	 * @param command The name of the command being executed.
	 * @param player The {@code Player} who sent the command.
	 * @param target The {@code Creature} that was targeted by the command.
	 * @return Always returns {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player player, Creature target)
	{
		player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
		player.clearHtmlActions(HtmlActionScope.TUTORIAL_HTML);
		return false;
	}
	
	/**
	 * Retrieves the list of commands for this handler.<br>
	 * This method returns all valid command strings.
	 * @return a {@code String[]} containing the allowed commands.
	 */
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}
