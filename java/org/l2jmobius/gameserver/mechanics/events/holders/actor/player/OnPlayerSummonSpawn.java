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

import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class represents an event triggered when a {@code Summon} is spawned by a player.<br>
 * It holds the necessary data for handling logic related to new summon appearances.
 * @author UnAfraid
 */
public class OnPlayerSummonSpawn implements IBaseEvent
{
	private final Summon _summon;
	
	/**
	 * Creates a new event for when a player's summon spawns.<br>
	 * This constructor initializes the event with the specific {@code Summon} object.
	 * @param summon The {@code Summon} instance that was spawned.
	 */
	public OnPlayerSummonSpawn(Summon summon)
	{
		_summon = summon;
	}
	
	/**
	 * Retrieves the {@code Summon} associated with this event.<br>
	 * This method returns the summon object that was spawned.
	 * @return the {@code Summon} instance.
	 */
	public Summon getSummon()
	{
		return _summon;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with the summon spawn.
	 * @return The {@code EventType} for this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_SUMMON_SPAWN;
	}
}
