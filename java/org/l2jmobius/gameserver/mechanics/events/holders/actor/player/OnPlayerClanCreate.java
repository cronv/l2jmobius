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
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class holds the event data triggered when a {@code Player} creates a new {@code Clan}.<br>
 * It provides access to the relevant actors and information involved in the clan creation process.
 * @author UnAfraid
 */
public class OnPlayerClanCreate implements IBaseEvent
{
	private final Player _player;
	private final Clan _clan;
	
	/**
	 * This event is triggered when a {@code Player} creates a new {@code Clan}.<br>
	 * It stores the details of both the actor and the newly created group.
	 * @param player The {@code Player} who performed the action.
	 * @param clan The {@code Clan} that was just created.
	 */
	public OnPlayerClanCreate(Player player, Clan clan)
	{
		_player = player;
		_clan = clan;
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
	 * Retrieves the {@code Clan} associated with this event.<br>
	 * This method returns the clan that was created by the player.
	 * @return the {@code Clan} object.
	 */
	public Clan getClan()
	{
		return _clan;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant representing the clan creation action.
	 * @return the {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_CLAN_CREATE;
	}
}
