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
package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.TrapAction;
import org.l2jmobius.gameserver.entity.actor.instance.Trap;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class represents an event triggered when a player interacts with a {@code Trap}.<br>
 * It holds the data necessary to process specific {@code TrapAction} effects.<br>
 * Use this class to handle logic for traps such as damage, teleportation, or status effects.
 * @author UnAfraid
 */
public class OnTrapAction implements IBaseEvent
{
	private final Trap _trap;
	private final Creature _trigger;
	private final TrapAction _action;
	
	/**
	 * Creates a new {@code OnTrapAction} event.<br>
	 * This constructor initializes the trap, the creature that triggered it, and the resulting action.
	 * @param trap The {@code Trap} instance involved in the event.
	 * @param trigger The {@code Creature} that activated the trap.
	 * @param action The specific {@code TrapAction} to be performed.
	 */
	public OnTrapAction(Trap trap, Creature trigger, TrapAction action)
	{
		_trap = trap;
		_trigger = trigger;
		_action = action;
	}
	
	/**
	 * Retrieves the {@code Trap} associated with this event.
	 * @return the {@code Trap} object.
	 */
	public Trap getTrap()
	{
		return _trap;
	}
	
	/**
	 * Returns the {@code Creature} that triggered this event.<br>
	 * This is the entity that interacted with the trap.
	 * @return the {@code Creature} that caused the trigger
	 */
	public Creature getTrigger()
	{
		return _trigger;
	}
	
	/**
	 * Retrieves the specific action triggered by a trap.<br>
	 * This returns the {@code TrapAction} associated with this event.
	 * @return the {@code TrapAction} performed by the trap.
	 */
	public TrapAction getAction()
	{
		return _action;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with the trap action.
	 * @return The {@code EventType} for this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_TRAP_ACTION;
	}
}
