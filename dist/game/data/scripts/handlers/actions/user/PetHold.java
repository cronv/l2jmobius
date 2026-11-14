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
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.handler.IActionUserHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Handles the player action for holding a pet's position.<br>
 * This class manages the logic when a {@code Player} commands a {@code Pet} to stay in its current location.
 * @author Nik
 */
public class PetHold implements IActionUserHandler
{
	/**
	 * Handles the action for a player to command their pet.<br>
	 * It checks if the {@code Player} has a valid {@code Pet}.<br>
	 * It also verifies if the pet is controllable before updating its status.
	 * @param player The {@code Player} who initiated the action.
	 * @param data The {@code ActionDataHolder} containing action details.
	 * @param ctrlPressed True if the {@code Ctrl} key is held down, false otherwise.
	 * @param shiftPressed True if the {@code Shift} key is held down, false otherwise.
	 */
	@Override
	public void onAction(Player player, ActionDataHolder data, boolean ctrlPressed, boolean shiftPressed)
	{
		if ((player.getPet() == null) || !player.getPet().isPet())
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_A_PET);
			return;
		}
		
		final Pet pet = player.getPet();
		if (pet.isUncontrollable())
		{
			player.sendPacket(SystemMessageId.WHEN_YOUR_PET_S_SATIETY_REACHES_0_YOU_CANNOT_CONTROL_IT);
		}
		else if (pet.isBetrayed())
		{
			player.sendPacket(SystemMessageId.YOUR_PET_SERVITOR_IS_UNRESPONSIVE_AND_WILL_NOT_OBEY_ANY_ORDERS);
		}
		else
		{
			((SummonAI) pet.getAI()).notifyFollowStatusChange();
		}
	}
	
	/**
	 * Checks if the current action belongs to a pet.<br>
	 * This method identifies actions performed by {@code Pet} entities.
	 * @return {@code true} if the action is related to a pet, otherwise {@code false}.
	 */
	@Override
	public boolean isPetAction()
	{
		return true;
	}
}
