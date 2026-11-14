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
package org.l2jmobius.gameserver.mechanics.events.holders.olympiad;

import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.olympiad.CompetitionType;
import org.l2jmobius.gameserver.mechanics.olympiad.Participant;

/**
 * This class holds the data for an olympiad match result event.<br>
 * It is triggered when a competition concludes to provide information about the winners and participants.
 * @author UnAfraid
 */
public class OnOlympiadMatchResult implements IBaseEvent
{
	private final Participant _winner;
	private final Participant _loser;
	private final CompetitionType _type;
	
	/**
	 * Creates a new {@code OnOlympiadMatchResult} object.<br>
	 * This constructor stores the results of an olympiad match.<br>
	 * It records who won, who lost, and the type of competition.
	 * @param winner The {@code Participant} who won the match.
	 * @param looser The {@code Participant} who lost the match.
	 * @param type The {@code CompetitionType} of the olympiad.
	 */
	public OnOlympiadMatchResult(Participant winner, Participant looser, CompetitionType type)
	{
		_winner = winner;
		_loser = looser;
		_type = type;
	}
	
	/**
	 * Retrieves the winner of the olympiad match.<br>
	 * This method returns the {@code Participant} who won the competition.
	 * @return the winning {@code Participant} object.
	 */
	public Participant getWinner()
	{
		return _winner;
	}
	
	/**
	 * Retrieves the participant who lost the olympiad match.<br>
	 * This method returns the {@code Participant} object associated with the losing side.
	 * @return The {@code Participant} who lost the match.
	 */
	public Participant getLoser()
	{
		return _loser;
	}
	
	/**
	 * Retrieves the type of competition for this match result.<br>
	 * This method returns the {@code CompetitionType} associated with the event.
	 * @return the {@code CompetitionType} of the match.
	 */
	public CompetitionType getCompetitionType()
	{
		return _type;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant from the {@code EventType} enum.
	 * @return the {@code EventType} associated with this result.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_OLYMPIAD_MATCH_RESULT;
	}
}
