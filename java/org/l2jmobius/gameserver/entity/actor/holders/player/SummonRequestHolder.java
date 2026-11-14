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
package org.l2jmobius.gameserver.entity.actor.holders.player;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.mechanics.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This class holds the data required to process a summon request from a {@code Player}.<br>
 * It stores temporary information needed to validate and execute the summoning action.
 * @author Mobius
 */
public class SummonRequestHolder
{
	private final Player _summoner;
	private final Location _location;
	
	/**
	 * Creates a new {@code SummonRequestHolder} for a specific player.<br>
	 * This constructor also captures the current location of the {@code Player}.<br>
	 * If the {@code Player} is {@code null}, the location will be set to {@code null}.
	 * @param summoner The {@code Player} who is requesting the summon.
	 */
	public SummonRequestHolder(Player summoner)
	{
		_summoner = summoner;
		_location = summoner == null ? null : new Location(summoner.getX(), summoner.getY(), summoner.getZ(), summoner.getHeading());
	}
	
	/**
	 * Retrieves the {@code Player} who initiated the summon request.<br>
	 * This method returns the owner of the current request.
	 * @return The {@code Player} object representing the summoner.
	 */
	public Player getSummoner()
	{
		return _summoner;
	}
	
	/**
	 * Retrieves the location associated with this request.<br>
	 * This method returns the {@code Location} where the summon was requested.
	 * @return The {@code Location} of the request.
	 */
	public Location getLocation()
	{
		return _location;
	}
	
	/**
	 * Checks if a target player can be summoned by an effector.<br>
	 * This method validates various conditions such as death, combat status, and zone restrictions.<br>
	 * It sends a system message to the {@code effector} if the summon fails.
	 * @param target The {@code Player} who is being targeted for summoning.
	 * @param effector The {@code Player} attempting to perform the summon.
	 * @return {@code true} if the summon is valid, or {@code false} otherwise.
	 */
	public static boolean checkSummonTargetStatus(Player target, Player effector)
	{
		if (target == effector)
		{
			return false;
		}
		
		if (target.isAlikeDead())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_DEAD_AT_THE_MOMENT_AND_CANNOT_BE_SUMMONED_OR_TELEPORTED);
			sm.addPcName(target);
			effector.sendPacket(sm);
			return false;
		}
		
		if (target.isInStoreMode())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_CURRENTLY_TRADING_OR_OPERATING_A_PRIVATE_STORE_AND_CANNOT_BE_SUMMONED_OR_TELEPORTED);
			sm.addPcName(target);
			effector.sendPacket(sm);
			return false;
		}
		
		if (target.isRooted() || target.isInCombat())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_ENGAGED_IN_COMBAT_AND_CANNOT_BE_SUMMONED_OR_TELEPORTED);
			sm.addPcName(target);
			effector.sendPacket(sm);
			return false;
		}
		
		if (target.isInOlympiadMode())
		{
			effector.sendPacket(SystemMessageId.A_USER_PARTICIPATING_IN_THE_OLYMPIAD_CANNOT_USE_SUMMONING_OR_TELEPORTING);
			return false;
		}
		
		if (target.isOnEvent() || target.isFlyingMounted() || target.isCombatFlagEquipped() || target.isInTraingCamp() || target.isInTimedHuntingZone() || effector.isInTimedHuntingZone())
		{
			effector.sendPacket(SystemMessageId.YOU_CANNOT_USE_SUMMONING_OR_TELEPORTING_IN_THIS_AREA);
			return false;
		}
		
		if (target.getObserverMode().isActive() || OlympiadManager.getInstance().isRegisteredInComp(target))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_IN_AN_AREA_WHERE_SUMMONING_OR_TELEPORT_IS_BLOCKED_2);
			sm.addString(target.getName());
			effector.sendPacket(sm);
			return false;
		}
		
		if (target.isInsideZone(ZoneId.NO_SUMMON_FRIEND) || target.isInsideZone(ZoneId.JAIL))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_IN_AN_AREA_WHERE_SUMMONING_OR_TELEPORT_IS_BLOCKED);
			sm.addString(target.getName());
			effector.sendPacket(sm);
			return false;
		}
		
		final Instance instance = effector.getInstanceWorld();
		if ((instance != null) && !instance.isPlayerSummonAllowed())
		{
			effector.sendPacket(SystemMessageId.CANNOT_BE_SUMMONED_IN_THIS_LOCATION);
			return false;
		}
		
		return true;
	}
}
