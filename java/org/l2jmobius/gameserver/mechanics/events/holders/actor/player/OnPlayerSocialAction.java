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

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class represents an event triggered when a {@code Player} performs a social action.<br>
 * It holds the necessary data for processing interactions between players in the game world.
 * @author St3eT
 */
public class OnPlayerSocialAction implements IBaseEvent
{
	private final Player _player;
	private final int _socialActionId;
	
	/**
	 * Creates a new {@code OnPlayerSocialAction} event.<br>
	 * This object stores the player who performed an action.<br>
	 * It also records the specific ID of that social action.
	 * @param player The {@code Player} who triggered the action.
	 * @param socialActionId The unique identifier for the social action.
	 */
	public OnPlayerSocialAction(Player player, int socialActionId)
	{
		_player = player;
		_socialActionId = socialActionId;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this social action.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the unique identifier for the social action.<br>
	 * This ID identifies which specific social interaction occurred.
	 * @return The {@code int} value of the social action ID.
	 */
	public int getSocialActionId()
	{
		return _socialActionId;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant value from {@code EventType}.
	 * @return The {@code EventType} associated with this action.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_SOCIAL_ACTION;
	}
}
