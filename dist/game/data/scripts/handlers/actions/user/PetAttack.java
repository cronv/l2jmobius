/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package handlers.actions.user;

import org.l2jmobius.gameserver.data.holders.ActionDataHolder;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.handler.IActionUserHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Handles the user action for commanding a {@code Pet} to attack.<br>
 * This class processes the logic when a {@code Player} initiates an attack command.
 * @author Mobius
 */
public class PetAttack implements IActionUserHandler
{
	/**
	 * Handles the logic for a player commanding their pet to attack.<br>
	 * It checks if the pet is valid and reachable before initiating an attack.<br>
	 * If the target is too far, the pet will follow the player instead.
	 * @param player The {@code Player} who is performing the action.
	 * @param data The {@code ActionDataHolder} containing action details.
	 * @param ctrlPressed The state of the {@code true} or {@code false} control key.
	 * @param shiftPressed The state of the {@code true} or {@code false} shift key.
	 */
	@Override
	public void onAction(Player player, ActionDataHolder data, boolean ctrlPressed, boolean shiftPressed)
	{
		final Pet pet = player.getPet();
		if ((pet == null) || !pet.isPet())
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_A_PET);
			return;
		}
		
		if (pet.isUncontrollable())
		{
			player.sendPacket(SystemMessageId.WHEN_YOUR_PET_S_SATIETY_REACHES_0_YOU_CANNOT_CONTROL_IT);
			return;
		}
		
		if (pet.isBetrayed())
		{
			player.sendPacket(SystemMessageId.YOUR_PET_SERVITOR_IS_UNRESPONSIVE_AND_WILL_NOT_OBEY_ANY_ORDERS);
			return;
		}
		
		final WorldObject target = player.getTarget();
		if (target == null)
		{
			return;
		}
		
		if ((player.calculateDistance3D(target) > 3000) || !GeoEngine.getInstance().canMoveToTarget(pet, target))
		{
			pet.getAI().setIntentionFollow(player);
		}
		else if (pet.canAttack(target, ctrlPressed))
		{
			pet.doAttack(target);
		}
	}
	
	/**
	 * Checks if the current action is related to a pet.<br>
	 * This method identifies actions performed by or on pets.
	 * @return {@code true} if the action involves a pet, otherwise {@code false}.
	 */
	@Override
	public boolean isPetAction()
	{
		return true;
	}
}
