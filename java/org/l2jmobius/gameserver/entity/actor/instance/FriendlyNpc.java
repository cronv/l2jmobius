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
package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.FriendlyNpcAI;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnAttackableAttack;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnAttackableKill;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcFirstTalk;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

/**
 * Represents a non-hostile NPC that can be attacked by players.<br>
 * This class handles the behavior of friendly entities that are still targetable in the game world.
 * @author GKR, Sdw
 */
public class FriendlyNpc extends Attackable
{
	private boolean _isAutoAttackable = true;
	
	/**
	 * Creates a new {@code FriendlyNpc} instance.<br>
	 * This constructor initializes the NPC with specific settings.<br>
	 * It sets the instance type to {@code InstanceType.FriendlyNpc}.<br>
	 * It disables the ability for the NPC to return to its spawn point.
	 * @param template The {@code NpcTemplate} used to create this NPC.
	 */
	public FriendlyNpc(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.FriendlyNpc);
		setCanReturnToSpawnPoint(false);
	}
	
	/**
	 * Checks if this NPC can be attacked by other entities.<br>
	 * This method always returns {@code false} for friendly NPCs.
	 * @return {@code false} because friendly NPCs are not targetable for attack.
	 */
	@Override
	public boolean isAttackable()
	{
		return false;
	}
	
	/**
	 * Checks if the NPC can be attacked automatically by a specific creature.<br>
	 * This returns {@code true} only if auto-attack is enabled and the attacker is not a player or another friendly NPC.
	 * @param attacker The {@code Creature} attempting to initiate an attack.
	 * @return {@code true} if the NPC can be auto-attacked, {@code false} otherwise.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return _isAutoAttackable && !attacker.isPlayable() && !(attacker instanceof FriendlyNpc);
	}
	
	/**
	 * Sets whether the NPC can be automatically attacked.<br>
	 * Use {@code true} to allow automatic attacks.<br>
	 * Use {@code false} to prevent automatic attacks.
	 * @param value The new auto-attackable status.
	 */
	@Override
	public void setAutoAttackable(boolean value)
	{
		_isAutoAttackable = value;
	}
	
	/**
	 * Applies damage to the {@code FriendlyNpc}.<br>
	 * It checks if the attacker is a valid source of damage.<br>
	 * It also notifies any listeners for attack events.
	 * @param attacker The {@code Creature} performing the attack.
	 * @param damage The amount of damage to apply.
	 * @param skill The {@code Skill} used to perform the attack.
	 */
	@Override
	public void addDamage(Creature attacker, int damage, Skill skill)
	{
		if (!attacker.isPlayable() && !(attacker instanceof FriendlyNpc))
		{
			super.addDamage(attacker, damage, skill);
		}
		
		if (attacker.isAttackable() && EventDispatcher.getInstance().hasListener(EventType.ON_ATTACKABLE_ATTACK, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnAttackableAttack(null, this, damage, skill, false), this);
		}
	}
	
	/**
	 * Updates the hate level of a target based on an attack.<br>
	 * This method calculates how much aggression is gained from a specific source.<br>
	 * It ensures that friendly NPCs do not gain hate from each other.
	 * @param attacker The {@code Creature} performing the action.
	 * @param damage The amount of damage dealt to the target.
	 * @param aggro The amount of hate to add to the target's aggression level.
	 */
	@Override
	public void addDamageHate(Creature attacker, long damage, long aggro)
	{
		if (!attacker.isPlayable() && !(attacker instanceof FriendlyNpc))
		{
			super.addDamageHate(attacker, damage, aggro);
		}
	}
	
	/**
	 * Handles the logic for when a {@code FriendlyNpc} dies.<br>
	 * It processes the death of the entity and notifies relevant scripts.
	 * @param killer The {@code Creature} that killed this entity.
	 * @return {@code true} if the death was successful, otherwise {@code false}.
	 */
	@Override
	public boolean doDie(Creature killer)
	{
		// Kill the Npc (the corpse disappeared after 7 seconds).
		if (!super.doDie(killer))
		{
			return false;
		}
		
		// Notify to scripts.
		if ((killer != null) && killer.isAttackable() && EventDispatcher.getInstance().hasListener(EventType.ON_ATTACKABLE_KILL, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnAttackableKill(null, this, false), this);
		}
		
		return true;
	}
	
	/**
	 * Handles actions performed by a {@code Player} on this NPC.<br>
	 * It updates the player's target and manages interaction logic.<br>
	 * This includes opening chat windows or notifying quest events.
	 * @param player The {@code Player} who is performing the action.
	 * @param interact A boolean indicating if the player is trying to interact with the NPC.
	 */
	@Override
	public void onAction(Player player, boolean interact)
	{
		if (!canTarget(player))
		{
			return;
		}
		
		// Check if the Player already target the GuardInstance.
		final int objectId = getObjectId();
		if (objectId != player.getTargetId())
		{
			// Set the target of the Player player.
			player.setTarget(this);
		}
		else if (interact)
		{
			// Calculate the distance between the Player and the Npc.
			if (!canInteract(player))
			{
				// Set the Player Intention to INTERACT.
				player.getAI().setIntentionInteract(this);
			}
			else
			{
				player.setLastFolkNPC(this);
				
				// Open a chat window on client with the text of the GuardInstance.
				if (hasListener(EventType.ON_NPC_QUEST_START))
				{
					player.setLastQuestNpcObject(objectId);
				}
				
				if (hasListener(EventType.ON_NPC_FIRST_TALK))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnNpcFirstTalk(this, player), this);
				}
				else
				{
					showChatWindow(player, 0);
				}
			}
		}
		
		// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet.
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Retrieves the file path for a specific NPC HTML template.<br>
	 * It constructs a path based on the provided ID and value.
	 * @param npcId The unique identifier of the NPC.
	 * @param value An additional value used to differentiate the HTML file.
	 * @param player The {@code Player} interacting with the NPC.
	 * @return A {@code String} representing the full path to the .htm file.
	 */
	@Override
	public String getHtmlPath(int npcId, int value, Player player)
	{
		String pom = "";
		if (value == 0)
		{
			pom = Integer.toString(npcId);
		}
		else
		{
			pom = npcId + "-" + value;
		}
		
		return "data/html/default/" + pom + ".htm";
	}
	
	/**
	 * Initializes the artificial intelligence for this NPC.<br>
	 * This method creates and returns a new {@code FriendlyNpcAI} instance.
	 * @return The initialized {@code CreatureAI} object.
	 */
	@Override
	protected CreatureAI initAI()
	{
		return new FriendlyNpcAI(this);
	}
}
