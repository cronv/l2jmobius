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
package handlers.actions.shiftclick;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.handler.AdminCommandHandler;
import org.l2jmobius.gameserver.handler.IActionShiftHandler;

/**
 * Handles the logic for when a {@code Player} performs a shift-click action.<br>
 * This class processes interactions between players and other world objects.
 */
public class PlayerShiftClick implements IActionShiftHandler
{
	/**
	 * Handles the action when a player shift-clicks on an object.<br>
	 * If the player is a GM, it sets the target and runs a command.<br>
	 * This method always returns {@code true}.
	 * @param player The {@code Player} who performed the action.
	 * @param target The {@code WorldObject} that was clicked.
	 * @param interact A boolean indicating if the interaction is active.
	 * @return Always returns {@code true}.
	 */
	@Override
	public boolean onAction(Player player, WorldObject target, boolean interact)
	{
		if (player.isGM())
		{
			// Check if the GM already target this l2pcinstance.
			if (player.getTarget() != target)
			{
				// Set the target of the Player player.
				player.setTarget(target);
			}
			
			AdminCommandHandler.getInstance().onCommand(player, "admin_character_info " + target.getName(), true);
		}
		
		return true;
	}
	
	/**
	 * Retrieves the type of instance for this action.<br>
	 * This method returns a constant value from {@code InstanceType}.
	 * @return the {@code InstanceType} associated with this handler.
	 */
	@Override
	public InstanceType getInstanceType()
	{
		return InstanceType.Player;
	}
}
