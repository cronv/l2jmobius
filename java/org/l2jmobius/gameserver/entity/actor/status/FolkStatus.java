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
import org.l2jmobius.gameserver.entity.actor.instance.Folk;

/**
 * Represents the status effects and conditions specific to {@link org.l2jmobius.gameserver.entity.actor.instance.Folk} entities.<br>
 * This class extends {@code NpcStatus} to provide specialized behavior for folk-type NPCs.
 */
public class FolkStatus extends NpcStatus
{
	/**
	 * Creates a new {@code FolkStatus} instance.<br>
	 * This constructor initializes the status for a specific NPC.
	 * @param activeChar The {@code Npc} character associated with this status.
	 */
	public FolkStatus(Npc activeChar)
	{
		super(activeChar);
	}
	
	/**
	 * Reduces the health points of this entity.<br>
	 * It takes a specific amount to subtract from the current health.<br>
	 * The method also records which creature caused the damage.
	 * @param value The amount of health to remove.
	 * @param attacker The {@code Creature} that performed the attack.
	 */
	@Override
	public void reduceHp(double value, Creature attacker)
	{
		reduceHp(value, attacker, true, false, false);
	}
	
	/**
	 * Reduces the health points of a {@code FolkStatus} instance.<br>
	 * This method handles various types of damage and status effects.
	 * @param value The amount of health to subtract.
	 * @param attacker The {@code Creature} that caused the reduction.
	 * @param awake Whether the target is currently awake.
	 * @param isDOT Whether this damage is from a Damage Over Time effect.
	 * @param isHpConsumption Whether this reduction is due to an HP consumption mechanic.
	 */
	@Override
	public void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHpConsumption)
	{
	}
	
	/**
	 * Retrieves the current active {@code Folk} character.<br>
	 * This method casts the result of the parent class call to a {@code Folk} type.
	 * @return The active {@code Folk} instance.
	 */
	@Override
	public Folk getActiveChar()
	{
		return (Folk) super.getActiveChar();
	}
}
