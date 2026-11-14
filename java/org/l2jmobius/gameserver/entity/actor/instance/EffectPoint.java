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

import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Team;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

/**
 * Represents a visual point in the game world where an effect is triggered.<br>
 * This {@code Npc} acts as a container for spawning effects or handling specific interactions at a fixed location.
 */
public class EffectPoint extends Npc
{
	private final Player _owner;
	private ScheduledFuture<?> _skillTask;
	
	/**
	 * Creates a new {@code EffectPoint} instance.<br>
	 * This object handles periodic skills based on an {@code NpcTemplate}.<br>
	 * It automatically links to the world of the provided {@code Creature}.
	 * @param template The {@code NpcTemplate} containing the skill and timing data.
	 * @param owner The {@code Creature} that owns this effect point.
	 */
	public EffectPoint(NpcTemplate template, Creature owner)
	{
		super(template);
		setInstanceType(InstanceType.EffectPoint);
		setInvul(false);
		
		_owner = owner == null ? null : owner.asPlayer();
		if (owner != null)
		{
			setInstance(owner.getInstanceWorld());
		}
		
		final SkillHolder skill = template.getParameters().getSkillHolder("union_skill");
		if (skill != null)
		{
			final long castTime = (long) (template.getParameters().getFloat("cast_time", 0.1f) * 1000);
			final long skillDelay = (long) (template.getParameters().getFloat("skill_delay", 2) * 1000);
			_skillTask = ThreadPool.scheduleAtFixedRate(() ->
			{
				if ((isDead() || !isSpawned()) && (_skillTask != null))
				{
					_skillTask.cancel(false);
					_skillTask = null;
					return;
				}
				
				doCast(skill.getSkill());
			}, castTime, skillDelay);
		}
	}
	
	/**
	 * Removes this {@code EffectPoint} from the game world.<br>
	 * It cancels any active {@code ScheduledFuture} tasks before deletion.
	 * @return {@code true} if the deletion was successful, otherwise {@code false}.
	 */
	@Override
	public boolean deleteMe()
	{
		if (_skillTask != null)
		{
			_skillTask.cancel(false);
			_skillTask = null;
		}
		
		return super.deleteMe();
	}
	
	/**
	 * Retrieves the {@code Player} who owns this effect point.<br>
	 * This method returns the internal owner of the object.
	 * @return The {@code Player} associated with this instance.
	 */
	@Override
	public Player asPlayer()
	{
		return _owner;
	}
	
	/**
	 * Handles an action performed by a {@code Player}.<br>
	 * This method sends a failure packet to the player.
	 * @param player The {@code Player} who triggered the action.
	 * @param interact A boolean indicating if the interaction was successful.
	 */
	@Override
	public void onAction(Player player, boolean interact)
	{
		// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet.
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Handles the action shift logic for a specific player.<br>
	 * This method checks if the {@code player} is null.<br>
	 * If the player exists, it sends an {@code ActionFailed.STATIC_PACKET} to them.
	 * @param player The {@code Player} who triggered the action shift.
	 */
	@Override
	public void onActionShift(Player player)
	{
		if (player == null)
		{
			return;
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Retrieves the {@code Party} of the owner.<br>
	 * This method returns {@code null} if the owner is {@code null}.
	 * @return the {@code Party} associated with the owner.
	 */
	@Override
	public Party getParty()
	{
		if (_owner == null)
		{
			return null;
		}
		
		return _owner.getParty();
	}
	
	/**
	 * Checks if the owner of this {@code EffectPoint} is currently in a party.<br>
	 * This method returns {@code true} only if the owner exists and belongs to a party.
	 * @return {@code true} if the owner is in a party, {@code false} otherwise.
	 */
	@Override
	public boolean isInParty()
	{
		return (_owner != null) && _owner.isInParty();
	}
	
	/**
	 * Retrieves the unique identifier of the clan belonging to the owner.<br>
	 * If the owner is {@code null}, it returns {@code 0}.
	 * @return The {@code int} ID of the clan.
	 */
	@Override
	public int getClanId()
	{
		return (_owner != null) ? _owner.getClanId() : 0;
	}
	
	/**
	 * Retrieves the unique identifier of the owner's ally.<br>
	 * This method checks if the {@code _owner} is not {@code null}.<br>
	 * If the owner exists, it returns their ally ID.<br>
	 * Otherwise, it returns {@code 0}.
	 * @return The ally ID of the owner or {@code 0} if no owner exists.
	 */
	@Override
	public int getAllyId()
	{
		return (_owner != null) ? _owner.getAllyId() : 0;
	}
	
	/**
	 * Retrieves the PvP flag of the owner.<br>
	 * If the owner is {@code null}, it returns {@code 0}.
	 * @return The PvP flag as a {@code byte}.
	 */
	@Override
	public byte getPvpFlag()
	{
		return _owner != null ? _owner.getPvpFlag() : 0;
	}
	
	/**
	 * Retrieves the {@code Team} of the owner.<br>
	 * If the owner is {@code null}, it returns {@code Team.NONE}.
	 * @return the {@code Team} of the owner or {@code Team.NONE}
	 */
	@Override
	public Team getTeam()
	{
		return _owner != null ? _owner.getTeam() : Team.NONE;
	}
}
