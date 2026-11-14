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
 * This event is triggered when a {@code Player} interacts with an {@code Npc} for the first time.<br>
 * It allows for specific logic to execute during this initial interaction.
 * @author UnAfraid
 */
public class OnNpcFirstTalk implements IBaseEvent
{
	private final Npc _npc;
	private final Player _player;
	
	/**
	 * This constructor initializes a new {@code OnNpcFirstTalk} event.<br>
	 * It stores the specific {@code Npc} and {@code Player} involved in the interaction.
	 * @param npc The {@code Npc} that is being spoken to.
	 * @param player The {@code Player} who initiated the conversation.
	 */
	public OnNpcFirstTalk(Npc npc, Player player)
	{
		_npc = npc;
		_player = player;
	}
	
	/**
	 * Retrieves the {@code Npc} associated with this event.<br>
	 * This method returns the specific non-player character involved in the interaction.
	 * @return The {@code Npc} object.
	 */
	public Npc getNpc()
	{
		return _npc;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.<br>
	 * This method returns the player who first spoke to the {@code Npc}.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with the first interaction between a player and an {@code Npc}.
	 * @return The {@code EventType} for this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_NPC_FIRST_TALK;
	}
}
