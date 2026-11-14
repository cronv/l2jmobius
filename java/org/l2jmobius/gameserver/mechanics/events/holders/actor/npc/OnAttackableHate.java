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

import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class represents an event triggered when an {@code Attackable} entity gains hate.<br>
 * It is used to handle mechanics related to aggression and target selection for NPCs.
 * @author UnAfraid
 */
public class OnAttackableHate implements IBaseEvent
{
	private final Attackable _npc;
	private final Player _player;
	private final boolean _isSummon;
	
	/**
	 * Creates a new {@code OnAttackableHate} event instance.<br>
	 * This constructor initializes the hate event with specific actors.
	 * @param npc The {@code Attackable} entity that is being targeted.
	 * @param player The {@code Player} who is initiating the attack.
	 * @param isSummon A boolean indicating if the target is a summon. Use {@code true} for summons and {@code false} otherwise.
	 */
	public OnAttackableHate(Attackable npc, Player player, boolean isSummon)
	{
		_npc = npc;
		_player = player;
		_isSummon = isSummon;
	}
	
	/**
	 * Retrieves the {@code Attackable} entity associated with this event.<br>
	 * This method returns the NPC that triggered the hate mechanic.
	 * @return The {@code Attackable} object representing the NPC.
	 */
	public Attackable getNpc()
	{
		return _npc;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.<br>
	 * This method returns the player who triggered the hate mechanic.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Checks if the current event involves a summon.<br>
	 * This method returns whether the {@code _isSummon} flag is set to {@code true}.
	 * @return {@code true} if the entity is a summon, {@code false} otherwise.
	 */
	public boolean isSummon()
	{
		return _isSummon;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with this instance.
	 * @return The {@code EventType} for this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_NPC_HATE;
	}
}
