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

import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class represents an event triggered immediately when an {@code Attackable} entity is killed by a {@code Player}.<br>
 * It serves as a data holder for processing logic related to kill events.
 * @author UnAfraid
 */
public class OnAttackableKill implements IBaseEvent
{
	private final Player _attacker;
	private final Attackable _target;
	private final boolean _isSummon;
	
	/**
	 * This constructor initializes a new {@code OnAttackableKill} event.<br>
	 * It stores the details of the kill action.
	 * @param attacker The {@code Player} who performed the kill.
	 * @param target The {@code Attackable} entity that was killed.
	 * @param isSummon A boolean indicating if the target was a summon.
	 */
	public OnAttackableKill(Player attacker, Attackable target, boolean isSummon)
	{
		_attacker = attacker;
		_target = target;
		_isSummon = isSummon;
	}
	
	/**
	 * Retrieves the {@code Player} who performed the attack.<br>
	 * This method returns the attacker associated with this kill event.
	 * @return the {@code Player} who killed the target
	 */
	public Player getAttacker()
	{
		return _attacker;
	}
	
	/**
	 * Retrieves the {@code Attackable} entity that was killed.<br>
	 * This method returns the target of the kill event.
	 * @return The {@code Attackable} object representing the target.
	 */
	public Attackable getTarget()
	{
		return _target;
	}
	
	/**
	 * Checks if the killed target was a summon.<br>
	 * Returns {@code true} if it was a summon.<br>
	 * Returns {@code false} otherwise.
	 * @return The sumon status of the target.
	 */
	public boolean isSummon()
	{
		return _isSummon;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with the kill.
	 * @return The {@code EventType} for an attackable kill.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_ATTACKABLE_KILL;
	}
}
