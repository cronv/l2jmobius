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
package org.l2jmobius.gameserver.mechanics.events.holders.actor.npc;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class represents an event triggered when an {@code Npc} is attacked by a member of a specific faction.<br>
 * It holds the necessary data to handle faction-based combat logic during attack events.
 * @author UnAfraid
 */
public class OnAttackableFactionCall implements IBaseEvent
{
	private final Npc _npc;
	private final Npc _caller;
	private final Player _attacker;
	private final boolean _isSummon;
	
	/**
	 * Creates a new event for when an {@code Npc} is attacked by a {@code Player}.<br>
	 * This constructor initializes the target, the caller, and the attacker.<br>
	 * It also records whether the attack involves a summon.
	 * @param npc The {@code Npc} being attacked.
	 * @param caller The {@code Npc} that triggered the event.
	 * @param attacker The {@code Player} performing the attack.
	 * @param isSummon A boolean indicating if the action is a summon.
	 */
	public OnAttackableFactionCall(Npc npc, Npc caller, Player attacker, boolean isSummon)
	{
		_npc = npc;
		_caller = caller;
		_attacker = attacker;
		_isSummon = isSummon;
	}
	
	/**
	 * Retrieves the {@code Npc} associated with this event.<br>
	 * This method returns the target NPC that was involved in the call.
	 * @return the {@code Npc} object.
	 */
	public Npc getNpc()
	{
		return _npc;
	}
	
	/**
	 * Retrieves the {@code Npc} that initiated this event.<br>
	 * This method returns the caller associated with the attackable faction call.
	 * @return The {@code Npc} who called the event.
	 */
	public Npc getCaller()
	{
		return _caller;
	}
	
	/**
	 * Retrieves the {@code Player} who initiated the attack.<br>
	 * This method returns the attacker associated with this event.
	 * @return the {@code Player} who performed the attack.
	 */
	public Player getAttacker()
	{
		return _attacker;
	}
	
	/**
	 * Checks if the event was triggered by a summon.<br>
	 * Returns {@code true} if it is a summon.<br>
	 * Returns {@code false} otherwise.
	 * @return The sumon status of the event.
	 */
	public boolean isSummon()
	{
		return _isSummon;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with this call.
	 * @return The {@code EventType} for this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_ATTACKABLE_FACTION_CALL;
	}
}
