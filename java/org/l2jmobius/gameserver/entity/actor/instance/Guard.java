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
package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcFirstTalk;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

/**
 * Represents a guard entity within the game world.<br>
 * This class extends {@code Attackable} to provide specific behaviors for guards.<br>
 * It includes additional logic for tracking {@code Player} PK status and aggressive monsters.
 */
public class Guard extends Attackable
{
	/**
	 * Creates a new {@code Guard} instance based on the provided template.<br>
	 * This constructor initializes the guard with its specific type.
	 * @param template The {@code NpcTemplate} used to define the guard's properties.
	 */
	public Guard(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Guard);
	}
	
	/**
	 * Checks if the guard can be automatically attacked by a specific creature.<br>
	 * This method evaluates monster types and faction rules to determine hostility.
	 * @param attacker The {@code Creature} attempting to attack.
	 * @return {@code true} if the attacker is allowed to auto-attack, {@code false} otherwise.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		if (attacker.isMonster() && !attacker.isFakePlayer())
		{
			return true;
		}
		
		if (FactionSystemConfig.FACTION_SYSTEM_ENABLED && FactionSystemConfig.FACTION_GUARDS_ENABLED && attacker.isPlayable())
		{
			final Player player = attacker.asPlayer();
			final boolean isGood = player.isGood();
			final boolean isEvil = player.isEvil();
			if ((isGood && getTemplate().isClan(FactionSystemConfig.FACTION_EVIL_TEAM_NAME)) || (isEvil && getTemplate().isClan(FactionSystemConfig.FACTION_GOOD_TEAM_NAME)))
			{
				return true;
			}
		}
		
		return super.isAutoAttackable(attacker);
	}
	
	/**
	 * Applies damage to this entity from a specific attacker.<br>
	 * This method also triggers AI behavior for nearby guards.
	 * @param attacker The {@code Creature} performing the attack.
	 * @param damage The amount of damage to apply.
	 * @param skill The {@code Skill} used to perform the attack.
	 */
	@Override
	public void addDamage(Creature attacker, int damage, Skill skill)
	{
		super.addDamage(attacker, damage, skill);
		getAI().startFollow(attacker);
		addDamageHate(attacker, 0, 10);
		World.forEachVisibleObjectInRange(this, Guard.class, 500, guard ->
		{
			guard.getAI().startFollow(attacker);
			guard.addDamageHate(attacker, 0, 10);
		});
	}
	
	/**
	 * This method is called when the entity is spawned into the world.<br>
	 * It initializes the random walking behavior based on the template settings.<br>
	 * It also activates the AI intention for the creature.
	 */
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		setRandomWalking(getTemplate().isRandomWalkEnabled());
		getAI().setIntentionActive();
		
		// Check the region where this mob is, do not activate the AI if region is inactive.
		// final WorldRegion region = World.getRegion(this);
		// if ((region != null) && (!region.isActive()))
		// {
		// getAI().stopAITask();
		// }
	}
	
	/**
	 * Retrieves the file path for a specific guard HTML template.<br>
	 * The path is constructed based on the provided NPC ID and value.
	 * @param npcId The unique identifier of the NPC.
	 * @param value An additional value used to differentiate templates.
	 * @param player The {@code Player} object interacting with the guard.
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
		
		return "data/html/guard/" + pom + ".htm";
	}
	
	/**
	 * Handles the interaction logic between a {@code Player} and this guard.<br>
	 * It determines if the player should attack or interact based on faction rules and aggro status.<br>
	 * The method updates the player's target and sets their AI intention accordingly.
	 * @param player The {@code Player} who performed the action.
	 * @param interactValue A boolean indicating the initial interaction state.
	 */
	@Override
	public void onAction(Player player, boolean interactValue)
	{
		if (!canTarget(player))
		{
			return;
		}
		
		boolean interact = interactValue;
		if (FactionSystemConfig.FACTION_SYSTEM_ENABLED && FactionSystemConfig.FACTION_GUARDS_ENABLED && ((player.isGood() && getTemplate().isClan(FactionSystemConfig.FACTION_EVIL_TEAM_NAME)) || (player.isEvil() && getTemplate().isClan(FactionSystemConfig.FACTION_GOOD_TEAM_NAME))))
		{
			interact = false;
			
			// TODO: Fix normal targeting
			player.getAI().setIntentionAttack(this);
		}
		
		if (isFakePlayer() && isInCombat())
		{
			interact = false;
			
			// TODO: Fix normal targeting
			player.getAI().setIntentionAttack(this);
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
			// Check if the Player is in the _aggroList of the GuardInstance.
			if (isInAggroList(player))
			{
				// Set the Player Intention to ATTACK.
				player.getAI().setIntentionAttack(this);
			}
			else
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
						if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_FIRST_TALK, this))
						{
							EventDispatcher.getInstance().notifyEventAsync(new OnNpcFirstTalk(this, player), this);
						}
					}
					else
					{
						showChatWindow(player, 0);
					}
				}
			}
		}
		
		// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet.
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}
