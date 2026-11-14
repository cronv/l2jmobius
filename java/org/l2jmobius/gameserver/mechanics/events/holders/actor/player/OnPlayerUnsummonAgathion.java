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
package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This event is triggered when a {@code Player} unsummons their Agathion.<br>
 * It handles the logic associated with removing an active Agathion from the game world.
 * @author Mobius
 */
public class OnPlayerUnsummonAgathion implements IBaseEvent
{
	private final Player _player;
	private final int _agathionId;
	
	/**
	 * This event is triggered when a {@code Player} unsummons an Agathion.<br>
	 * It stores the specific {@code Player} and the unique {@code agathionId}.
	 * @param player The {@code Player} who performed the action.
	 * @param agathionId The unique identifier of the Agathion being dismissed.
	 */
	public OnPlayerUnsummonAgathion(Player player, int agathionId)
	{
		_player = player;
		_agathionId = agathionId;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.<br>
	 * This method returns the player who performed the action.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the unique identifier for the unsummoned Agathion.<br>
	 * This value was provided during the creation of the {@code OnPlayerUnsummonAgathion} event.
	 * @return The {@code int} ID of the Agathion.
	 */
	public int getAgathionId()
	{
		return _agathionId;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * It returns a specific {@code EventType}.
	 * @return the {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_UNSUMMON_AGATHION;
	}
}
