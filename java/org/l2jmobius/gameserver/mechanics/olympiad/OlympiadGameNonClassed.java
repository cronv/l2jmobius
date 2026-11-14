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
package org.l2jmobius.gameserver.mechanics.olympiad;

import java.util.Set;

import org.l2jmobius.gameserver.config.OlympiadConfig;

/**
 * Handles the logic for {@code Olympiad} games where players are not restricted by classes.<br>
 * This class extends {@code OlympiadGameNormal} to provide specific behavior for non-classed competition.
 * @author DS
 */
public class OlympiadGameNonClassed extends OlympiadGameNormal
{
	/**
	 * Creates a new instance of an unclassed olympiad game.<br>
	 * This constructor initializes the game with a specific ID and a set of participants.
	 * @param id The unique identifier for the game.
	 * @param opponents An array of {@code Participant} objects competing in the game.
	 */
	public OlympiadGameNonClassed(int id, Participant[] opponents)
	{
		super(id, opponents);
	}
	
	/**
	 * Retrieves the type of competition for this game.<br>
	 * This method returns a constant value representing non-classed games.
	 * @return the {@code CompetitionType} of the current game.
	 */
	@Override
	public CompetitionType getType()
	{
		return CompetitionType.NON_CLASSED;
	}
	
	/**
	 * Retrieves the divider value for non-classed olympiads.<br>
	 * This value is fetched from {@code OlympiadConfig}.
	 * @return The integer divider used for calculations.
	 */
	@Override
	protected int getDivider()
	{
		return OlympiadConfig.OLYMPIAD_DIVIDER_NON_CLASSED;
	}
	
	/**
	 * Creates a new {@code OlympiadGameNonClassed} instance.<br>
	 * It converts the provided set of IDs into a list of participants.<br>
	 * Returns {@code null} if the participant list cannot be created.
	 * @param id The unique identifier for the game.
	 * @param list A set of integers representing the participant IDs.
	 * @return A new {@code OlympiadGameNonClassed} object or {@code null}.
	 */
	protected static OlympiadGameNonClassed createGame(int id, Set<Integer> list)
	{
		final Participant[] opponents = OlympiadGameNormal.createListOfParticipants(list);
		if (opponents == null)
		{
			return null;
		}
		
		return new OlympiadGameNonClassed(id, opponents);
	}
}
