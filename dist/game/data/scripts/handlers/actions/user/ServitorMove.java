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
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.handler.IActionUserHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Handles the action where a {@code Summon} moves toward a target {@code Player}.<br>
 * This class processes the movement logic for servitors commanded by users.
 * @author Nik
 */
public class ServitorMove implements IActionUserHandler
{
	/**
	 * Handles the action to move servitors toward a target.<br>
	 * Checks if the {@code player} has any active servitors.<br>
	 * Updates the movement intention of each valid summon.
	 * @param player The {@code Player} who initiated the action.
	 * @param data The {@code ActionDataHolder} containing action details.
	 * @param ctrlPressed Whether the {@code ctrl} key is currently pressed.
	 * @param shiftPressed Whether the {@code shift} key is currently pressed.
	 */
	@Override
	public void onAction(Player player, ActionDataHolder data, boolean ctrlPressed, boolean shiftPressed)
	{
		if (!player.hasServitors())
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_A_SERVITOR);
			return;
		}
		
		final WorldObject playerTarget = player.getTarget();
		if (playerTarget != null)
		{
			for (Summon summon : player.getServitors().values())
			{
				if ((summon != playerTarget) && !summon.isMovementDisabled())
				{
					if (summon.isBetrayed())
					{
						player.sendPacket(SystemMessageId.YOUR_PET_SERVITOR_IS_UNRESPONSIVE_AND_WILL_NOT_OBEY_ANY_ORDERS);
						return;
					}
					
					summon.setFollowStatus(false);
					summon.getAI().setIntentionMoveTo(playerTarget.getLocation());
				}
			}
		}
	}
	
	/**
	 * Checks if the current action is performed by a pet.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if this is a pet action.
	 */
	@Override
	public boolean isPetAction()
	{
		return true;
	}
}
