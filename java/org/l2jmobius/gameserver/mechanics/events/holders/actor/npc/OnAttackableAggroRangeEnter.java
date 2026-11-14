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
 * This event is triggered when an {@code Npc} enters the attackable aggro range of another actor.<br>
 * It handles the initial detection logic for hostile interactions between entities.
 * @author UnAfraid
 */
public class OnAttackableAggroRangeEnter implements IBaseEvent
{
	private final Npc _npc;
	private final Player _player;
	private final boolean _isSummon;
	
	/**
	 * This constructor initializes a new {@code OnAttackableAggroRangeEnter} event.<br>
	 * It sets the target {@code Npc}, the {@code Player} who triggered it, and the summon status.
	 * @param npc The {@code Npc} that entered the aggro range.
	 * @param attacker The {@code Player} who caused the aggro.
	 * @param isSummon Whether the triggering action was performed by a summon.
	 */
	public OnAttackableAggroRangeEnter(Npc npc, Player attacker, boolean isSummon)
	{
		_npc = npc;
		_player = attacker;
		_isSummon = isSummon;
	}
	
	/**
	 * Retrieves the {@code Npc} associated with this event.<br>
	 * This method returns the specific non-player character that was entered.
	 * @return the {@code Npc} object.
	 */
	public Npc getNpc()
	{
		return _npc;
	}
	
	/**
	 * Retrieves the {@code Player} who triggered this event.<br>
	 * This method returns the attacker associated with the aggro range entry.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Checks if the event involves a summon.<br>
	 * This method returns {@code true} if the actor is a summon.<br>
	 * It returns {@code false} otherwise.
	 * @return The sumon status of the actor as a {@code boolean}.
	 */
	public boolean isSummon()
	{
		return _isSummon;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant from the {@code EventType} enum.
	 * @return The {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_ATTACKABLE_AGGRO_RANGE_ENTER;
	}
}
