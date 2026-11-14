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

import org.l2jmobius.gameserver.ai.SummonAI;
import org.l2jmobius.gameserver.data.holders.ActionDataHolder;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IActionUserHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Handles the player action for commanding a servitor to hold its current position.<br>
 * This class processes the logic when a {@code Player} interacts with a servitor to stay put.
 * @author Nik
 */
public class ServitorHold implements IActionUserHandler
{
	/**
	 * Handles the action for a player to hold their servitors.<br>
	 * It checks if the player owns any servitors.<br>
	 * It notifies all non-betrayed servitors to change their follow status.
	 * @param player The {@code Player} who is performing the action.
	 * @param data The {@code ActionDataHolder} containing information about the action.
	 * @param ctrlPressed A boolean indicating if the {@code Ctrl} key is pressed.
	 * @param shiftPressed A boolean indicating if the {@code Shift} key is pressed.
	 */
	@Override
	public void onAction(Player player, ActionDataHolder data, boolean ctrlPressed, boolean shiftPressed)
	{
		if (!player.hasServitors())
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_A_SERVITOR);
			return;
		}
		
		player.getServitors().values().forEach(s ->
		{
			if (s.isBetrayed())
			{
				player.sendPacket(SystemMessageId.YOUR_PET_SERVITOR_IS_UNRESPONSIVE_AND_WILL_NOT_OBEY_ANY_ORDERS);
				return;
			}
			
			((SummonAI) s.getAI()).notifyFollowStatusChange();
		});
	}
	
	/**
	 * Checks if the current action is related to a pet.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if this is a pet action.
	 */
	@Override
	public boolean isPetAction()
	{
		return true;
	}
}
