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
package org.l2jmobius.gameserver.entity.actor.status;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.Duel;

/**
 * Represents the status effects and conditions specific to {@code Npc} entities.<br>
 * This class handles how non-player characters interact with various game statuses.
 */
public class NpcStatus extends CreatureStatus
{
	/**
	 * Creates a new {@code NpcStatus} instance for a specific NPC.<br>
	 * This constructor initializes the status using the provided {@code activeChar}.
	 * @param activeChar The {@code Npc} character associated with this status.
	 */
	public NpcStatus(Npc activeChar)
	{
		super(activeChar);
	}
	
	/**
	 * Reduces the health points of this {@code Creature}.<br>
	 * It takes a specific amount to subtract from the current health.<br>
	 * The method also records which entity performed the action.
	 * @param value The amount of health to remove.
	 * @param attacker The {@code Creature} that caused the damage.
	 */
	@Override
	public void reduceHp(double value, Creature attacker)
	{
		reduceHp(value, attacker, true, false, false);
	}
	
	/**
	 * Reduces the health points of the associated {@code Npc}.<br>
	 * This method handles damage logic and updates the attacker list.<br>
	 * It also checks for duel status if the attacker is a player.
	 * @param value The amount of health to subtract.
	 * @param attacker The {@code Creature} performing the action.
	 * @param awake Whether the target is currently awake.
	 * @param isDOT Whether this damage is from a Damage Over Time effect.
	 * @param isHpConsumption Whether this reduction is due to HP consumption.
	 */
	@Override
	public void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHpConsumption)
	{
		final Npc npc = getActiveChar();
		if (npc.isDead())
		{
			return;
		}
		
		if (attacker != null)
		{
			final Player attackerPlayer = attacker.asPlayer();
			if ((attackerPlayer != null) && attackerPlayer.isInDuel())
			{
				attackerPlayer.setDuelState(Duel.DUELSTATE_INTERRUPTED);
			}
			
			// Add attackers to npc's attacker list.
			npc.addAttackerToAttackByList(attacker);
		}
		
		super.reduceHp(value, attacker, awake, isDOT, isHpConsumption);
	}
	
	/**
	 * Retrieves the current active {@code Npc} associated with this status.<br>
	 * This method converts the internal character to an {@code Npc} type.
	 * @return The active {@code Npc} object.
	 */
	@Override
	public Npc getActiveChar()
	{
		return super.getActiveChar().asNpc();
	}
}
