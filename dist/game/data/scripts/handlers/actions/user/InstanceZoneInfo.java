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

import org.l2jmobius.gameserver.data.holders.ActionDataHolder;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IActionUserHandler;
import org.l2jmobius.gameserver.network.serverpackets.ExInzoneWaiting;

/**
 * This class handles the user action for retrieving information about an instance zone.<br>
 * It processes requests to display details regarding specific instance areas.
 * @author St3eT
 */
public class InstanceZoneInfo implements IActionUserHandler
{
	/**
	 * Handles the action when a {@code Player} interacts with an instance zone.<br>
	 * This method sends a waiting packet to the player.
	 * @param player The {@code Player} who performed the action.
	 * @param data The {@code ActionDataHolder} containing information about the action.
	 * @param ctrlPressed True if the {@code Ctrl} key was held down, false otherwise.
	 * @param shiftPressed True if the {@code Shift} key was held down, false otherwise.
	 */
	@Override
	public void onAction(Player player, ActionDataHolder data, boolean ctrlPressed, boolean shiftPressed)
	{
		player.sendPacket(new ExInzoneWaiting(player, false));
	}
}
