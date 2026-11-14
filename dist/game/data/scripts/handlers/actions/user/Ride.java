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

/**
 * Handles the logic for players mounting and dismounting creatures.<br>
 * This {@code Ride} class manages the interaction between a {@code Player} and their mount.
 * @author Nik
 */
public class Ride implements IActionUserHandler
{
	/**
	 * Handles the logic when a player performs an action.<br>
	 * This method mounts the {@code Player} onto their pet.
	 * @param player The {@code Player} who is performing the action.
	 * @param data The {@code ActionDataHolder} containing information about the action.
	 * @param ctrlPressed True if the {@code Ctrl} key is held down, false otherwise.
	 * @param shiftPressed True if the {@code Shift} key is held down, false otherwise.
	 */
	@Override
	public void onAction(Player player, ActionDataHolder data, boolean ctrlPressed, boolean shiftPressed)
	{
		player.mountPlayer(player.getPet());
	}
}
