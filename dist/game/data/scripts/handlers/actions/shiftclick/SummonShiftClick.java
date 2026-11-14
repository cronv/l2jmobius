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
 * Handles the logic for summoning entities via a shift-click action.<br>
 * This class processes requests to spawn objects when a player interacts with them using {@code Shift} + Click.
 */
public class SummonShiftClick implements IActionShiftHandler
{
	/**
	 * Handles the action when a player shift-clicks a {@code WorldObject}.<br>
	 * If the {@code Player} is an administrator, it sets their target and sends a command.<br>
	 * This method always returns {@code true}.
	 * @param player The {@code Player} who performed the action.
	 * @param target The {@code WorldObject} that was clicked.
	 * @param interact A boolean indicating if the interaction was successful.
	 * @return Always returns {@code true}.
	 */
	@Override
	public boolean onAction(Player player, WorldObject target, boolean interact)
	{
		if (player.isGM())
		{
			if (player.getTarget() != target)
			{
				// Set the target of the Player player.
				player.setTarget(target);
			}
			
			AdminCommandHandler.getInstance().onCommand(player, "admin_summon_info", true);
		}
		
		return true;
	}
	
	/**
	 * Retrieves the type of instance associated with this action.<br>
	 * This method returns a constant value from the {@code InstanceType} enum.
	 * @return the {@code InstanceType} for this handler.
	 */
	@Override
	public InstanceType getInstanceType()
	{
		return InstanceType.Summon;
	}
}
