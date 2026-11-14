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
package handlers.actions.user;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.BotReportTable;
import org.l2jmobius.gameserver.data.holders.ActionDataHolder;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IActionUserHandler;

/**
 * Handles the player action for clicking the bot report button.<br>
 * This class processes reports submitted by users regarding automated behavior.
 * @author Nik
 */
public class BotReport implements IActionUserHandler
{
	/**
	 * Handles the action when a player reports a bot.<br>
	 * Checks if the feature is enabled in {@code GeneralConfig}.<br>
	 * Sends a message to the {@code Player} if it is disabled.
	 * @param player The {@code Player} who triggered the action.
	 * @param data The {@code ActionDataHolder} containing action details.
	 * @param ctrlPressed True if the {@code Ctrl} key was held down.
	 * @param shiftPressed True if the {@code Shift} key was held down.
	 */
	@Override
	public void onAction(Player player, ActionDataHolder data, boolean ctrlPressed, boolean shiftPressed)
	{
		if (GeneralConfig.BOTREPORT_ENABLE)
		{
			BotReportTable.getInstance().reportBot(player);
		}
		else
		{
			player.sendMessage("This feature is disabled.");
		}
	}
}
