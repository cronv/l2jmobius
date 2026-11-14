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
 * This event is triggered when a {@code Player} reaches a new clan level.<br>
 * It handles the logic associated with a {@code Clan} leveling up.
 * @author UnAfraid
 */
public class OnPlayerClanLvlUp implements IBaseEvent
{
	private final Player _player;
	private final Clan _clan;
	
	/**
	 * This constructor initializes a new {@code OnPlayerClanLvlUp} event.<br>
	 * It sets the specific {@code Player} and {@code Clan} involved in the level up.
	 * @param player The {@code Player} who triggered the clan level increase.
	 * @param clan The {@code Clan} that reached a new level.
	 */
	public OnPlayerClanLvlUp(Player player, Clan clan)
	{
		_player = player;
		_clan = clan;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.<br>
	 * This method returns the player who triggered the clan level up.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the {@code Clan} associated with this event.<br>
	 * This method returns the clan that leveled up.
	 * @return The {@code Clan} object.
	 */
	public Clan getClan()
	{
		return _clan;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant from {@code EventType}.
	 * @return The {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_CLAN_LEVELUP;
	}
}
