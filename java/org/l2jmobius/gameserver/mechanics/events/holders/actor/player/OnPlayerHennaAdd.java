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
 * This event is triggered when a {@code Player} adds a {@code Henna} item.<br>
 * It handles the logic associated with applying henna effects to an actor.
 * @author UnAfraid
 */
public class OnPlayerHennaAdd implements IBaseEvent
{
	private final Player _player;
	private final Henna _henna;
	
	/**
	 * This constructor initializes a new {@code OnPlayerHennaAdd} event.<br>
	 * It stores the {@code Player} who received the item.<br>
	 * It also stores the specific {@code Henna} item involved.
	 * @param player The {@code Player} instance associated with this event.
	 * @param henna The {@code Henna} item that was added to the player.
	 */
	public OnPlayerHennaAdd(Player player, Henna henna)
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
	 * This method returns the specific henna item added to the player.
	 * @return the {@code Henna} instance.
	 */
	public Henna getHenna()
	{
		return _henna;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant from the {@code EventType} enum.
	 * @return The {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_HENNA_ADD;
	}
}
