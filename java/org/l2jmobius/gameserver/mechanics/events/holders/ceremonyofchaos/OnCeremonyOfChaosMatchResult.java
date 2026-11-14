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
package org.l2jmobius.gameserver.mechanics.events.holders.ceremonyofchaos;

import java.util.List;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class holds the data for a {@code CeremonyOfChaos} match result.<br>
 * It is triggered once a match concludes to process rewards and final outcomes.
 * @author UnAfraid
 */
public class OnCeremonyOfChaosMatchResult implements IBaseEvent
{
	private final List<Player> _winners;
	private final List<Player> _members;
	
	/**
	 * Creates a new {@code OnCeremonyOfChaosMatchResult} object.<br>
	 * This constructor initializes the match results with specific players.
	 * @param winners A list of {@code Player} objects who won the match.
	 * @param members A list of {@code Player} objects who participated in the match.
	 */
	public OnCeremonyOfChaosMatchResult(List<Player> winners, List<Player> members)
	{
		_winners = winners;
		_members = members;
	}
	
	/**
	 * Retrieves the list of players who won the match.<br>
	 * This method returns all winners from the {@code OnCeremonyOfChaosMatchResult}.
	 * @return a {@code List} containing all {@code Player} objects that won.
	 */
	public List<Player> getWinners()
	{
		return _winners;
	}
	
	/**
	 * Retrieves the list of all players involved in the match.<br>
	 * This includes both winners and participants.
	 * @return a {@code List} containing all {@code Player} objects.
	 */
	public List<Player> getMembers()
	{
		return _members;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant from the {@code EventType} enum.
	 * @return the {@code EventType} associated with this result.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_CEREMONY_OF_CHAOS_MATCH_RESULT;
	}
}
