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
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

/**
 * Handles the logic for when a {@code Player} clicks on an object in the game world.<br>
 * This class processes interactions between players and {@code WorldObject} entities.
 */
public class PlayerClick implements IActionClickHandler
{
	private static final int CURSED_WEAPON_VICTIM_MIN_LEVEL = 21;
	
	/**
	 * Handles the logic when a {@code Player} interacts with another object.<br>
	 * This method checks for control blocks and target locks before processing actions.<br>
	 * It determines whether to set a new target, initiate an attack, or follow the target.
	 * @param player The {@code Player} who is performing the action.
	 * @param target The {@code WorldObject} being interacted with.
	 * @param interact A boolean indicating if the interaction is a secondary click.
	 * @return {@code true} if the action was processed successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onAction(Player player, WorldObject target, boolean interact)
	{
		// Check if the Player is confused.
		if (player.isControlBlocked())
		{
			return false;
		}
		
		// Aggression target lock effect.
		if (player.isLockedTarget() && (player.getLockedTarget() != target))
		{
			player.sendPacket(SystemMessageId.FAILED_TO_CHANGE_ENMITY);
			return false;
		}
		
		// Check if the player already target this Player.
		if (player.getTarget() != target)
		{
			// Set the target of the player.
			player.setTarget(target);
		}
		else if (interact)
		{
			// Check if this Player has a Private Store.
			final Player targetPlayer = target.asPlayer();
			if (targetPlayer.isInStoreMode())
			{
				player.getAI().setIntentionInteract(target);
			}
			else
			{
				// Check if this Player is autoAttackable.
				if (target.isAutoAttackable(player))
				{
					// Player with level < 21 can't attack a cursed weapon holder.
					// And a cursed weapon holder can't attack players with level < 21.
					if ((targetPlayer.isCursedWeaponEquipped() && (player.getLevel() < CURSED_WEAPON_VICTIM_MIN_LEVEL)) //
						|| (player.isCursedWeaponEquipped() && (targetPlayer.getLevel() < CURSED_WEAPON_VICTIM_MIN_LEVEL)))
					{
						player.sendPacket(ActionFailed.STATIC_PACKET);
					}
					else
					{
						player.getAI().setIntentionAttack(target);
						player.onActionRequest();
					}
				}
				else
				{
					// This Action Failed packet avoids player getting stuck when clicking three or more times.
					player.sendPacket(ActionFailed.STATIC_PACKET);
					
					// if (GeoEngine.getInstance().canMoveToTarget(player, target))
					// {
					player.getAI().setIntentionFollow(target);
					// }
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Returns the type of instance for this action.<br>
	 * This method identifies the entity as a {@code Player}.
	 * @return the {@code InstanceType} of the current object.
	 */
	@Override
	public InstanceType getInstanceType()
	{
		return InstanceType.Player;
	}
}
