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
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerSummonTalk;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.PetStatusShow;

/**
 * Handles the interaction when a player clicks on a pet.<br>
 * This class manages the logic for displaying pet status and other related actions.
 */
public class PetClick implements IActionClickHandler
{
	/**
	 * Handles the logic when a {@code Player} interacts with a pet object.<br>
	 * This method manages targeting, ownership updates, and interaction states.
	 * @param player The {@code Player} performing the action.
	 * @param target The {@code WorldObject} being interacted with.
	 * @param interact A boolean indicating if the player is interacting with the target.
	 * @return {@code true} if the action was processed successfully, otherwise {@code false}.
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
		
		final boolean isOwner = player.getObjectId() == target.asPet().getOwner().getObjectId();
		if (isOwner && (player != target.asPet().getOwner()))
		{
			target.asPet().updateRefOwner(player);
		}
		
		if (player.getTarget() != target)
		{
			// Set the target of the Player player.
			player.setTarget(target);
		}
		else if (interact)
		{
			// Check if the pet is attackable (without a forced attack) and isn't dead.
			if (target.isAutoAttackable(player) && !isOwner)
			{
				player.getAI().setIntentionAttack(target);
				player.onActionRequest();
			}
			else if (!target.asCreature().isInsideRadius2D(player, 150))
			{
				player.getAI().setIntentionInteract(target);
				player.onActionRequest();
			}
			else
			{
				if (isOwner)
				{
					player.sendPacket(new PetStatusShow(target.asPet()));
					
					// Notify to scripts
					if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_SUMMON_TALK, target))
					{
						EventDispatcher.getInstance().notifyEventAsync(new OnPlayerSummonTalk(target.asSummon()), target);
					}
				}
				
				player.updateNotMoveUntil();
			}
		}
		
		return true;
	}
	
	/**
	 * Retrieves the type of instance associated with this action.<br>
	 * This method identifies the specific category for the click handler.
	 * @return the {@code InstanceType} value returned by this method.
	 */
	@Override
	public InstanceType getInstanceType()
	{
		return InstanceType.Pet;
	}
}
