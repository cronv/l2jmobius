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

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.handler.IActionClickHandler;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcFirstTalk;
import org.l2jmobius.gameserver.network.serverpackets.MoveToPawn;

/**
 * Handles the logic triggered when a {@code Player} clicks on an {@code Npc}.<br>
 * It manages interactions such as opening dialogues or triggering specific NPC behaviors.
 */
public class NpcClick implements IActionClickHandler
{
	/**
	 * Handles the logic when a {@code Player} clicks on a {@code WorldObject}.<br>
	 * It manages targeting, attacking, and interacting with NPCs.
	 * @param player The {@code Player} who performed the action.
	 * @param target The {@code WorldObject} that was clicked.
	 * @param interact A boolean indicating if the interaction key was pressed.
	 * @return {@code true} if the action was processed successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onAction(Player player, WorldObject target, boolean interact)
	{
		final Npc npc = target.asNpc();
		if (!npc.canTarget(player))
		{
			return false;
		}
		
		player.setLastFolkNPC(npc);
		
		// Check if the Player already target the Npc.
		if (target != player.getTarget())
		{
			// Set the target of the Player player.
			player.setTarget(target);
			
			// Check if the player is attackable (without a forced attack).
			if (target.isAutoAttackable(player))
			{
				npc.getAI(); // wake up ai
			}
		}
		else if (interact)
		{
			// Check if the player is attackable (without a forced attack) and isn't dead.
			final boolean autoAttackable = target.isAutoAttackable(player);
			if (autoAttackable && !target.asCreature().isAlikeDead())
			{
				player.getAI().setIntentionAttack(target);
			}
			else if (!autoAttackable)
			{
				// Calculate the distance between the Player and the Npc.
				if (!npc.canInteract(player))
				{
					player.getAI().setIntentionInteract(target);
				}
				else
				{
					if ((npc.getId() != 34200 /* Mystic Tavern Globe */) && !player.isSitting())
					{
						// Turn NPC to the player.
						player.sendPacket(new MoveToPawn(player, npc, 100));
						if (npc.hasRandomAnimation())
						{
							npc.onRandomAnimation(Rnd.get(8));
						}
					}
					
					// If player is moving and NPC is in interaction distance, change the intention to stop moving.
					if (player.isMoving() && npc.isInsideRadius3D(player, Npc.INTERACTION_DISTANCE))
					{
						player.getAI().setIntentionActive();
					}
					
					// Stop movement when trying to talk to a moving NPC.
					if (npc.isMoving())
					{
						player.stopMove(null);
					}
					
					// Open a chat window on client with the text of the Npc.
					if (npc.hasListener(EventType.ON_NPC_QUEST_START))
					{
						player.setLastQuestNpcObject(target.getObjectId());
					}
					
					if (npc.hasListener(EventType.ON_NPC_FIRST_TALK))
					{
						EventDispatcher.getInstance().notifyEventAsync(new OnNpcFirstTalk(npc, player), npc);
					}
					else
					{
						npc.showChatWindow(player);
					}
					
					if (PlayerConfig.PLAYER_MOVEMENT_BLOCK_TIME > 0)
					{
						player.updateNotMoveUntil();
					}
					
					if (npc.isFakePlayer() && GeoEngine.getInstance().canSeeTarget(player, npc))
					{
						player.getAI().setIntentionFollow(npc);
					}
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Retrieves the type of instance for this action.<br>
	 * This method returns a constant value representing an {@code Npc}.
	 * @return the {@code InstanceType} associated with this click handler.
	 */
	@Override
	public InstanceType getInstanceType()
	{
		return InstanceType.Npc;
	}
}
