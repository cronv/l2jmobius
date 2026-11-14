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

import java.util.Collection;

import org.l2jmobius.gameserver.data.holders.ActionDataHolder;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.handler.IActionUserHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Handles the player action for unsummoning a servitor.<br>
 * This class manages the logic required to remove a {@code Summon} entity from the world.
 * @author St3eT
 */
public class UnsummonServitor implements IActionUserHandler
{
	/**
	 * Handles the logic for unsummoning a player's servitor.<br>
	 * It checks if the servitor is available to be deactivated.<br>
	 * If valid, it unsummons either a specific target or all current servitors.
	 * @param player The {@code Player} who initiated the action.
	 * @param data The {@code ActionDataHolder} containing action details.
	 * @param ctrlPressed Whether the {@code ctrl} key is pressed.
	 * @param shiftPressed Whether the {@code shift} key is pressed.
	 */
	@Override
	public void onAction(Player player, ActionDataHolder data, boolean ctrlPressed, boolean shiftPressed)
	{
		boolean canUnsummon = true;
		if (player.hasServitors())
		{
			final Collection<Summon> servitors = player.getServitors().values();
			for (Summon s : servitors)
			{
				if (s.isBetrayed())
				{
					player.sendPacket(SystemMessageId.YOUR_PET_SERVITOR_IS_UNRESPONSIVE_AND_WILL_NOT_OBEY_ANY_ORDERS);
					canUnsummon = false;
					break;
				}
				else if (s.isAttackingNow() || s.isInCombat() || s.isMovementDisabled())
				{
					player.sendPacket(SystemMessageId.A_SERVITOR_WHOM_IS_ENGAGED_IN_BATTLE_CANNOT_BE_DE_ACTIVATED);
					canUnsummon = false;
					break;
				}
			}
			
			if (canUnsummon)
			{
				servitors.forEach(s -> s.unSummon(player));
			}
		}
		else
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_A_SERVITOR);
		}
	}
	
	/**
	 * Checks if this action is related to a pet.<br>
	 * This method returns {@code true} for the unsummon servitor action.
	 * @return {@code true} if the action involves a pet, otherwise {@code false}.
	 */
	@Override
	public boolean isPetAction()
	{
		return true;
	}
}
