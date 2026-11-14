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
 * Handles the logic for when an {@code Npc} bypasses a manor. <br>
 * This event is triggered to manage specific actions related to manor interactions.
 * @author malyelfik
 */
public class OnNpcManorBypass implements IBaseEvent
{
	private final Player _player;
	private final Npc _target;
	private final int _request;
	private final int _manorId;
	private final boolean _nextPeriod;
	
	/**
	 * Creates a new instance of the {@code OnNpcManorBypass} event.<br>
	 * This constructor initializes all necessary data for an NPC manor bypass action.
	 * @param player The {@code Player} who initiated the request.
	 * @param target The {@code Npc} being interacted with.
	 * @param request The specific request ID sent by the player.
	 * @param manorId The unique identifier for the manor.
	 * @param nextPeriod Indicates if the action should occur in the next period.
	 */
	public OnNpcManorBypass(Player player, Npc target, int request, int manorId, boolean nextPeriod)
	{
		_player = player;
		_target = target;
		_request = request;
		_manorId = manorId;
		_nextPeriod = nextPeriod;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.<br>
	 * This method returns the player who triggered the action.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the {@code Npc} that was targeted by this event.
	 * @return the {@code Npc} object associated with this bypass request.
	 */
	public Npc getTarget()
	{
		return _target;
	}
	
	/**
	 * Retrieves the request value associated with this event.<br>
	 * This value was provided during the creation of the {@code OnNpcManorBypass} instance.
	 * @return The integer value of the request.
	 */
	public int getRequest()
	{
		return _request;
	}
	
	/**
	 * Retrieves the unique identifier for the manor.<br>
	 * This value was provided during the creation of this event.
	 * @return the {@code int} ID of the manor.
	 */
	public int getManorId()
	{
		return _manorId;
	}
	
	/**
	 * Checks if the bypass request is for the next period.<br>
	 * This method returns the value of the {@code _nextPeriod} flag.
	 * @return {@code true} if it is the next period, {@code false} otherwise.
	 */
	public boolean isNextPeriod()
	{
		return _nextPeriod;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with the bypass action.
	 * @return The {@code EventType} for this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_NPC_MANOR_BYPASS;
	}
}
