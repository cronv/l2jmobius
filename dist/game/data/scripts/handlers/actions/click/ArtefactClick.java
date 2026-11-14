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

/**
 * Handles the interaction when a player clicks on an artefact object.<br>
 * This class processes the logic for interacting with specific world objects.<br>
 * It ensures that the correct actions are triggered based on the {@code WorldObject} clicked.
 */
public class ArtefactClick implements IActionClickHandler
{
	/**
	 * Handles the logic when a {@code Player} interacts with a {@code WorldObject}.<br>
	 * It checks if the target is valid for the player.<br>
	 * If the target is not already selected, it sets the target for the {@code Player}.<br>
	 * If the player is interacting and cannot do so directly, it sets an interaction intention.
	 * @param player The {@code Player} performing the action.
	 * @param target The {@code WorldObject} being clicked.
	 * @param interact A boolean indicating if the player is trying to interact.
	 * @return {@code true} if the action was processed successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onAction(Player player, WorldObject target, boolean interact)
	{
		if (!target.asNpc().canTarget(player))
		{
			return false;
		}
		
		if (player.getTarget() != target)
		{
			player.setTarget(target);
		}
		// Calculate the distance between the Player and the Npc.
		else if (interact && !target.asNpc().canInteract(player))
		{
			// Notify the Player AI with INTERACT.
			player.getAI().setIntentionInteract(target);
		}
		
		return true;
	}
	
	/**
	 * Retrieves the type of instance associated with this action.<br>
	 * This method returns a constant value from {@code InstanceType}.
	 * @return The {@code InstanceType} for this specific click handler.
	 */
	@Override
	public InstanceType getInstanceType()
	{
		return InstanceType.Artefact;
	}
}
