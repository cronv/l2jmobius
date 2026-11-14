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
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.handler.IActionUserHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Handles the user action for unsummoning a pet.<br>
 * This class manages the logic when a {@code Player} chooses to dismiss their active {@code Summon}.
 * @author St3eT
 */
public class UnsummonPet implements IActionUserHandler
{
	/**
	 * Handles the logic for unsummoning a player's pet.<br>
	 * It checks if the pet is available and valid to be returned.<br>
	 * If requirements are met, it calls {@code pet.unSummon(player)}.<br>
	 * Otherwise, it sends an appropriate error message to the player.
	 * @param player The {@code Player} attempting to perform the action.
	 * @param data The {@code ActionDataHolder} containing action details.
	 * @param ctrlPressed True if the {@code Ctrl} key is held down.
	 * @param shiftPressed True if the {@code Shift} key is held down.
	 */
	@Override
	public void onAction(Player player, ActionDataHolder data, boolean ctrlPressed, boolean shiftPressed)
	{
		final Summon pet = player.getPet();
		if (pet == null)
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_A_PET);
		}
		else if (pet.asPet().isUncontrollable())
		{
			player.sendPacket(SystemMessageId.WHEN_YOUR_PET_S_SATIETY_REACHES_0_YOU_CANNOT_CONTROL_IT);
		}
		else if (pet.isBetrayed())
		{
			player.sendPacket(SystemMessageId.WHEN_YOUR_PET_S_SATIETY_REACHES_0_YOU_CANNOT_CONTROL_IT);
		}
		else if (pet.isDead())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_SUMMON_A_DEAD_PET);
		}
		else if (pet.isAttackingNow() || pet.isInCombat() || pet.isMovementDisabled())
		{
			player.sendPacket(SystemMessageId.A_PET_CANNOT_BE_UNSUMMONED_WHILE_IN_COMBAT);
		}
		else if (pet.isHungry())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_RETURN_A_HUNGRY_PET);
		}
		else
		{
			pet.unSummon(player);
		}
	}
	
	/**
	 * Checks if the current action is related to a pet.<br>
	 * This method identifies actions performed on {@code Summon} entities.
	 * @return {@code true} if the action involves a pet, otherwise {@code false}.
	 */
	@Override
	public boolean isPetAction()
	{
		return true;
	}
}
