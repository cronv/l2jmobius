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
import org.l2jmobius.gameserver.entity.item.Henna;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This event is triggered when a {@code Henna} item is removed from a {@code Player}.<br>
 * It handles the logic associated with the removal of henna effects or items.
 * @author UnAfraid
 */
public class OnPlayerHennaRemove implements IBaseEvent
{
	private final Player _player;
	private final Henna _henna;
	
	/**
	 * This constructor initializes a new {@code OnPlayerHennaRemove} event.<br>
	 * It stores the {@code Player} who lost the item and the specific {@code Henna} removed.
	 * @param player The {@code Player} instance involved in the event.
	 * @param henna The {@code Henna} item that was removed from the player.
	 */
	public OnPlayerHennaRemove(Player player, Henna henna)
	{
		_player = player;
		_henna = henna;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the {@code Henna} object associated with this event.<br>
	 * This method returns the specific henna being removed from a player.
	 * @return the {@code Henna} instance.
	 */
	public Henna getHenna()
	{
		return _henna;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with this instance.
	 * @return The {@code EventType} for this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_HENNA_REMOVE;
	}
}
