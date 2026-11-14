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
package handlers.actions.click;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.handler.IActionClickHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Handles the logic for when a player clicks on a trap object.<br>
 * It triggers the corresponding trap effect or damage based on the {@code WorldObject} clicked.
 */
public class TrapClick implements IActionClickHandler
{
	/**
	 * Handles the logic when a {@code Player} interacts with a {@code WorldObject}.<br>
	 * This method checks if the player is currently locked onto an enemy.<br>
	 * It prevents changing targets if a lock is active and the target is different.<br>
	 * Otherwise, it sets the new target for the player.
	 * @param player The {@code Player} performing the action.
	 * @param target The {@code WorldObject} being interacted with.
	 * @param interact A boolean indicating if the interaction was successful.
	 * @return {@code true} if the target was successfully set, or {@code false} otherwise.
	 */
	@Override
	public boolean onAction(Player player, WorldObject target, boolean interact)
	{
		// Aggression target lock effect.
		if (player.isLockedTarget() && (player.getLockedTarget() != target))
		{
			player.sendPacket(SystemMessageId.FAILED_TO_CHANGE_ENMITY);
			return false;
		}
		
		player.setTarget(target);
		return true;
	}
	
	/**
	 * Retrieves the type of instance associated with this action.<br>
	 * This method returns a constant value from the {@code InstanceType} enum.
	 * @return the {@code InstanceType} for this trap.
	 */
	@Override
	public InstanceType getInstanceType()
	{
		return InstanceType.Trap;
	}
}
