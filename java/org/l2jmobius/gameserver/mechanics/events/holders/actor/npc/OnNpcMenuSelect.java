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
 * This class represents an event triggered when a {@code Player} selects an option from an {@code Npc} menu.<br>
 * It holds the necessary data for handling interactions between players and non-player characters.
 * @author St3eT
 */
public class OnNpcMenuSelect implements IBaseEvent
{
	private final Player _player;
	private final Npc _npc;
	private final int _ask;
	private final int _reply;
	
	/**
	 * Creates a new {@code OnNpcMenuSelect} event.<br>
	 * This represents an interaction between a {@code Player} and an {@code Npc}.<br>
	 * It stores the specific menu options selected by the player.
	 * @param player The {@code Player} who initiated the interaction.
	 * @param npc The {@code Npc} being interacted with.
	 * @param ask The ID of the question or option chosen from the menu.
	 * @param reply The ID of the response provided to that question.
	 */
	public OnNpcMenuSelect(Player player, Npc npc, int ask, int reply)
	{
		_player = player;
		_npc = npc;
		_ask = ask;
		_reply = reply;
	}
	
	/**
	 * Retrieves the {@code Player} who initiated the interaction.<br>
	 * This method returns the player associated with this event.
	 * @return the {@code Player} object.
	 */
	public Player getTalker()
	{
		return _player;
	}
	
	/**
	 * Retrieves the {@code Npc} associated with this event.
	 * @return the {@code Npc} object.
	 */
	public Npc getNpc()
	{
		return _npc;
	}
	
	/**
	 * Retrieves the {@code ask} value from this event.<br>
	 * This value represents the specific request identifier.
	 * @return The integer value of the {@code ask} field.
	 */
	public int getAsk()
	{
		return _ask;
	}
	
	/**
	 * Retrieves the reply value from this event.<br>
	 * This value is used to identify the specific response selected by a player.
	 * @return The {@code int} value of the reply.
	 */
	public int getReply()
	{
		return _reply;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with the selection.
	 * @return The {@code EventType} for this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_NPC_MENU_SELECT;
	}
}
