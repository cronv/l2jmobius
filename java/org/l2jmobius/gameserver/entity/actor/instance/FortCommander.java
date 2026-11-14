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

import java.util.List;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.managers.FortSiegeManager;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.siege.FortSiege;
import org.l2jmobius.gameserver.mechanics.siege.FortSpawnHolder;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * Represents the commander of a fort during siege events.<br>
 * This class handles specific behaviors and logic for the fort leader entity.<br>
 * It extends {@code Defender} to provide specialized functionality for fort defense.
 */
public class FortCommander extends Defender
{
	private boolean _canTalk;
	
	/**
	 * Creates a new {@code FortCommander} instance.<br>
	 * This constructor initializes the commander using a specific template.<br>
	 * It sets the instance type to {@code InstanceType.FortCommander}.<br>
	 * It also enables the ability for the commander to talk.
	 * @param template The {@code NpcTemplate} used to define the commander's properties.
	 */
	public FortCommander(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.FortCommander);
		_canTalk = true;
	}
	
	/**
	 * Checks if the commander can be attacked by a specific creature.<br>
	 * The attacker must be a player and not a defender during an active siege.<br>
	 * This method returns {@code false} if the attacker is {@code null}.
	 * @param attacker The {@code Creature} attempting to perform the attack.
	 * @return {@code true} if the commander can be attacked, otherwise {@code false}.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		if ((attacker == null) || !attacker.isPlayer())
		{
			return false;
		}
		
		// Attackable during siege by all except defenders.
		final Fort fort = getFort();
		if (fort != null)
		{
			final FortSiege siege = fort.getSiege();
			final boolean isInProgress = siege.isInProgress();
			return ((fort.getResidenceId() > 0) && isInProgress && !siege.checkIsDefender(attacker.getClan()));
		}
		
		return false;
	}
	
	/**
	 * Updates the hate level of a target based on an attack.<br>
	 * This method checks if the {@code attacker} is a {@code FortCommander}.<br>
	 * If it is not, the damage and aggro are passed to the parent class.
	 * @param attacker The {@code Creature} performing the action.
	 * @param damage The amount of damage dealt.
	 * @param aggro The amount of hate to add.
	 */
	@Override
	public void addDamageHate(Creature attacker, long damage, long aggro)
	{
		if (attacker == null)
		{
			return;
		}
		
		if (!(attacker instanceof FortCommander))
		{
			super.addDamageHate(attacker, damage, aggro);
		}
	}
	
	/**
	 * Handles the logic for when this commander dies.<br>
	 * It checks if the death is successful before updating the siege status.<br>
	 * If a siege is active, it notifies the system that the commander was killed.
	 * @param killer The {@code Creature} that caused the death.
	 * @return {@code true} if the death was processed successfully, otherwise {@code false}.
	 */
	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		if (getFort().getSiege().isInProgress())
		{
			getFort().getSiege().killedCommander(this);
		}
		
		return true;
	}
	
	/**
	 * Moves the commander back to its original spawn location.<br>
	 * This method clears all current aggro targets.<br>
	 * It updates the AI intention if the entity has one.
	 */
	@Override
	public void returnHome()
	{
		final Spawn spawn = getSpawn();
		if (!isInsideRadius2D(spawn, 200))
		{
			clearAggroList();
			
			if (hasAI())
			{
				getAI().setIntentionMoveTo(spawn.getLocation());
			}
		}
	}
	
	/**
	 * Applies damage to a {@code Creature}.<br>
	 * This method also triggers specific NPC shouts based on the current spawn.<br>
	 * It calls the superclass {@code addDamage} method to process the actual health reduction.
	 * @param creature The {@code Creature} receiving the damage.
	 * @param damage The amount of damage to apply.
	 * @param skill The {@code Skill} used to perform the attack.
	 */
	@Override
	public void addDamage(Creature creature, int damage, Skill skill)
	{
		Creature attacker = creature;
		final Spawn spawn = getSpawn();
		if ((spawn != null) && canTalk())
		{
			final List<FortSpawnHolder> commanders = FortSiegeManager.getInstance().getCommanderSpawnList(getFort().getResidenceId());
			for (FortSpawnHolder spawn2 : commanders)
			{
				if (spawn2.getId() == spawn.getId())
				{
					NpcStringId npcString = null;
					switch (spawn2.getMessageId())
					{
						case 1:
						{
							npcString = NpcStringId.ATTACKING_THE_ENEMY_S_REINFORCEMENTS_IS_NECESSARY_TIME_TO_DIE;
							break;
						}
						case 2:
						{
							if (attacker.isSummon())
							{
								attacker = attacker.asSummon().getOwner();
							}
							
							npcString = NpcStringId.EVERYONE_CONCENTRATE_YOUR_ATTACKS_ON_S1_SHOW_THE_ENEMY_YOUR_RESOLVE;
							break;
						}
						case 3:
						{
							npcString = NpcStringId.FIRE_SPIRIT_UNLEASH_YOUR_POWER_BURN_THE_ENEMY;
							break;
						}
					}
					
					if (npcString != null)
					{
						broadcastSay(ChatType.NPC_SHOUT, npcString, npcString.getParamCount() == 1 ? attacker.getName() : null);
						setCanTalk(false);
						ThreadPool.schedule(new ScheduleTalkTask(), 10000);
					}
				}
			}
		}
		
		super.addDamage(attacker, damage, skill);
	}
	
	private class ScheduleTalkTask implements Runnable
	{
		public ScheduleTalkTask()
		{
		}
		
		@Override
		public void run()
		{
			setCanTalk(true);
		}
	}
	
	/**
	 * Sets whether the {@code FortCommander} is allowed to speak.<br>
	 * Use {@code true} to enable speech and {@code false} to disable it.
	 * @param value The new status for speaking.
	 */
	void setCanTalk(boolean value)
	{
		_canTalk = value;
	}
	
	/**
	 * Checks if the commander is allowed to speak.<br>
	 * This method returns the current state of the {@code _canTalk} flag.
	 * @return {@code true} if the commander can talk, {@code false} otherwise.
	 */
	private boolean canTalk()
	{
		return _canTalk;
	}
	
	/**
	 * Checks if the commander plays random animations.<br>
	 * This method currently always returns {@code false}.
	 * @return {@code true} if random animations are enabled, otherwise {@code false}.
	 */
	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
}
