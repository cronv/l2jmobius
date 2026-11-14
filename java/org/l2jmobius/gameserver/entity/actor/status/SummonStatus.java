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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.holders.player.Duel;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Represents the status effects and properties specific to {@code Summon} entities.<br>
 * This class handles how summons interact with game mechanics and player actions.
 */
public class SummonStatus extends PlayableStatus
{
	/**
	 * Creates a new {@code SummonStatus} instance.<br>
	 * This constructor links the status to a specific {@code Summon}.
	 * @param activeChar The {@code Summon} character associated with this status.
	 */
	public SummonStatus(Summon activeChar)
	{
		super(activeChar);
	}
	
	/**
	 * Reduces the health points of the summon.<br>
	 * This method updates the current HP based on a specific amount.<br>
	 * It also records which creature caused the damage.
	 * @param value The amount of health to subtract.
	 * @param attacker The {@code Creature} that performed the attack.
	 */
	@Override
	public void reduceHp(double value, Creature attacker)
	{
		reduceHp(value, attacker, true, false, false);
	}
	
	/**
	 * Reduces the health of the summon and handles damage transfer to the owner or party.<br>
	 * This method checks for duel status and calculates shared damage based on stats.<br>
	 * It also manages CP reduction if the transferred damage exceeds available CP.
	 * @param amount The amount of damage to apply.
	 * @param attacker The creature performing the attack.
	 * @param awake Whether the summon is currently awake.
	 * @param isDOT Whether this damage is from a Damage Over Time effect.
	 * @param isHPConsumption Whether this damage is caused by HP consumption.
	 */
	@Override
	public void reduceHp(double amount, Creature attacker, boolean awake, boolean isDOT, boolean isHPConsumption)
	{
		final Summon summon = getActiveChar();
		if ((attacker == null) || summon.isDead())
		{
			return;
		}
		
		final Player attackerPlayer = attacker.asPlayer();
		final Player summonOwner = summon.getOwner();
		if ((attackerPlayer != null) && ((summonOwner == null) || (summonOwner.getDuelId() != attackerPlayer.getDuelId())))
		{
			attackerPlayer.setDuelState(Duel.DUELSTATE_INTERRUPTED);
		}
		
		double value = amount;
		final Player caster = summon.getTransferingDamageTo();
		if ((summonOwner != null) && (summonOwner.getParty() != null))
		{
			if ((caster != null) && LocationUtil.checkIfInRange(1000, summon, caster, true) && !caster.isDead() && summon.getParty().getMembers().contains(caster))
			{
				int transferDmg = ((int) value * (int) summon.getStat().getValue(Stat.TRANSFER_DAMAGE_TO_PLAYER, 0)) / 100;
				transferDmg = Math.min((int) caster.getCurrentHp() - 1, transferDmg);
				if (transferDmg > 0)
				{
					int membersInRange = 0;
					for (Player member : caster.getParty().getMembers())
					{
						if (LocationUtil.checkIfInRange(1000, member, caster, false) && (member != caster))
						{
							membersInRange++;
						}
					}
					
					int casterHpDamage = transferDmg;
					if (attacker.isPlayable() && (caster.getCurrentCp() > 0))
					{
						final int cpDamage = (int) Math.min(caster.getCurrentCp(), transferDmg);
						caster.getStatus().reduceCp(cpDamage);
						casterHpDamage -= cpDamage;
					}
					
					if (membersInRange > 0)
					{
						// CP was taken above and the share is capped, so the rest goes straight to HP without the damage taken modifiers.
						caster.getStatus().reduceHp(casterHpDamage / membersInRange, attacker, null, true, false, false, true);
						value -= transferDmg;
					}
				}
			}
		}
		else if ((caster != null) && (caster == summonOwner) && LocationUtil.checkIfInRange(1000, summon, caster, true) && !caster.isDead()) // When no party, transfer only to owner (caster).
		{
			int transferDmg = ((int) value * (int) summon.getStat().getValue(Stat.TRANSFER_DAMAGE_TO_PLAYER, 0)) / 100;
			transferDmg = Math.min((int) caster.getCurrentHp() - 1, transferDmg);
			if (transferDmg > 0)
			{
				int casterHpDamage = transferDmg;
				if (attacker.isPlayable() && (caster.getCurrentCp() > 0))
				{
					final int cpDamage = (int) Math.min(caster.getCurrentCp(), transferDmg);
					caster.getStatus().reduceCp(cpDamage);
					casterHpDamage -= cpDamage;
				}
				
				// CP was taken above and the share is capped, so the rest goes straight to HP without the damage taken modifiers.
				caster.getStatus().reduceHp(casterHpDamage, attacker, null, true, false, false, true);
				value -= transferDmg;
			}
		}
		
		super.reduceHp(value, attacker, awake, isDOT, isHPConsumption);
	}
	
	/**
	 * Retrieves the active character associated with this status.<br>
	 * This method returns the {@code Summon} object.
	 * @return the current {@code Summon} entity.
	 */
	@Override
	public Summon getActiveChar()
	{
		return super.getActiveChar().asSummon();
	}
}
