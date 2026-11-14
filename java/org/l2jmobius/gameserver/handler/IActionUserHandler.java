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
package org.l2jmobius.gameserver.handler;

import org.l2jmobius.gameserver.data.holders.ActionDataHolder;
import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * This interface defines the contract for handling actions performed by a {@code Player}.<br>
 * It allows the server to process specific user interactions and update the corresponding {@code ActionDataHolder}.
 * @author UnAfraid
 */
public interface IActionUserHandler
{
	void onAction(Player player, ActionDataHolder data, boolean ctrlPressed, boolean shiftPressed);
	
	/**
	 * Checks if the current action is related to a pet.<br>
	 * This method returns {@code false} by default.
	 * @return {@code true} if the action involves a pet, otherwise {@code false}.
	 */
	default boolean isPetAction()
	{
		return false;
	}
}
