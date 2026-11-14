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
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.PetStatusShow;

/**
 * Handles the interaction when a player clicks on a summon object.<br>
 * This class manages the logic for summoning pets and displaying their status.<br>
 * It ensures that the correct {@code PetStatusShow} packets are sent to the player.
 */
public class SummonClick implements IActionClickHandler
{
	/**
	 * Handles the logic when a {@code Player} clicks on a {@code WorldObject}.<br>
	 * This method manages target locking, pet interactions, and movement intentions.
	 * @param player The {@code Player} performing the action.
	 * @param target The {@code WorldObject} being clicked.
	 * @param interact Whether the interaction key was pressed.
	 * @return {@code true} if the action was processed successfully, or {@code false} otherwise.
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
		
		final WorldObject playerTarget = player.getTarget();
		if ((player == target.asSummon().getOwner()) && (playerTarget == target))
		{
			player.sendPacket(new PetStatusShow(target.asSummon()));
			player.updateNotMoveUntil();
			player.sendPacket(ActionFailed.STATIC_PACKET);
			
			// Notify to scripts
			if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_SUMMON_TALK, target))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnPlayerSummonTalk(target.asSummon()), target);
			}
		}
		else if (playerTarget != target)
		{
			player.setTarget(target);
		}
		else if (interact)
		{
			if (target.isAutoAttackable(player))
			{
				player.getAI().setIntentionAttack(target);
				player.onActionRequest();
			}
			else
			{
				// This Action Failed packet avoids player getting stuck when clicking three or more times.
				player.sendPacket(ActionFailed.STATIC_PACKET);
				if (target.asSummon().isInsideRadius2D(player, 150))
				{
					player.updateNotMoveUntil();
				}
				
				// else if (GeoEngine.getInstance().canMoveToTarget(player, target))
				// {
				player.getAI().setIntentionFollow(target);
				// }
			}
		}
		
		return true;
	}
	
	/**
	 * Retrieves the type of instance associated with this action.<br>
	 * This method returns a constant value from {@code InstanceType}.
	 * @return the {@code InstanceType} for this click action.
	 */
	@Override
	public InstanceType getInstanceType()
	{
		return InstanceType.Summon;
	}
}
