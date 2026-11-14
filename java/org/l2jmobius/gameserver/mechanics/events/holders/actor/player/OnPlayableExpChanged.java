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

import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class holds the event data for when a {@code Playable} entity's experience changes.<br>
 * It is used to notify systems that an update to the experience value has occurred.
 * @author UnAfraid
 */
public class OnPlayableExpChanged implements IBaseEvent
{
	private final Playable _playable;
	private final long _oldExp;
	private final long _newExp;
	
	/**
	 * Creates a new {@code OnPlayableExpChanged} event.<br>
	 * This event tracks changes to an actor's experience points.
	 * @param playable The {@code Playable} entity that experienced the change.
	 * @param oldExp The previous experience value.
	 * @param newExp The current experience value.
	 */
	public OnPlayableExpChanged(Playable playable, long oldExp, long newExp)
	{
		_playable = playable;
		_oldExp = oldExp;
		_newExp = newExp;
	}
	
	/**
	 * Retrieves the {@code Playable} entity associated with this event.
	 * @return The {@code Playable} object.
	 */
	public Playable getPlayable()
	{
		return _playable;
	}
	
	/**
	 * Retrieves the experience value before it was changed.<br>
	 * This value is stored when the {@code OnPlayableExpChanged} event is created.
	 * @return The previous experience amount as a {@code long}.
	 */
	public long getOldExp()
	{
		return _oldExp;
	}
	
	/**
	 * Retrieves the new experience value.<br>
	 * This value is used when a {@code Playable} entity's experience changes.
	 * @return The new experience as a {@code long}.
	 */
	public long getNewExp()
	{
		return _newExp;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * It returns the specific {@code EventType} associated with experience changes.
	 * @return The {@code EventType} for this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYABLE_EXP_CHANGED;
	}
}
