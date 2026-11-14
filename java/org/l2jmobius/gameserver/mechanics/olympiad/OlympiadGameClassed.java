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

import java.util.List;
import java.util.Set;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.OlympiadConfig;

/**
 * This class handles the logic for olympiad games that are restricted to specific classes.<br>
 * It extends {@code OlympiadGameNormal} to provide specialized behavior for these events.
 * @author DS
 */
public class OlympiadGameClassed extends OlympiadGameNormal
{
	/**
	 * Creates a new instance of {@code OlympiadGameClassed}.<br>
	 * This constructor initializes the game with specific participants.
	 * @param id The unique identifier for the game.
	 * @param opponents An array of {@code Participant} objects competing in the game.
	 */
	private OlympiadGameClassed(int id, Participant[] opponents)
	{
		super(id, opponents);
	}
	
	/**
	 * Returns the type of competition for this game.<br>
	 * This method identifies the specific category of the olympiad.
	 * @return the {@code CompetitionType} associated with this game.
	 */
	@Override
	public CompetitionType getType()
	{
		return CompetitionType.CLASSED;
	}
	
	/**
	 * Returns the divider value for classed olympiads.<br>
	 * This value is retrieved from {@code OlympiadConfig}.
	 * @return the integer divider used for calculations.
	 */
	@Override
	protected int getDivider()
	{
		return OlympiadConfig.OLYMPIAD_DIVIDER_CLASSED;
	}
	
	/**
	 * Creates a new {@code OlympiadGameClassed} instance based on the provided data.<br>
	 * This method selects a valid set of participants from the list to form a game.<br>
	 * It returns {@code null} if no valid matches can be formed.
	 * @param id The unique identifier for the game.
	 * @param classList A list containing sets of participant IDs.
	 * @return A new {@code OlympiadGameClassed} object or {@code null}.
	 */
	protected static OlympiadGameClassed createGame(int id, List<Set<Integer>> classList)
	{
		if ((classList == null) || classList.isEmpty())
		{
			return null;
		}
		
		Set<Integer> list;
		Participant[] opponents;
		while (!classList.isEmpty())
		{
			list = classList.get(Rnd.get(classList.size()));
			if ((list == null) || (list.size() < 2))
			{
				classList.remove(list);
				continue;
			}
			
			opponents = OlympiadGameNormal.createListOfParticipants(list);
			if (opponents == null)
			{
				classList.remove(list);
				continue;
			}
			
			return new OlympiadGameClassed(id, opponents);
		}
		
		return null;
	}
	
	/**
	 * Validates the winner for the third round of the olympiad.<br>
	 * This method checks the {@code OlympiadStadium} to ensure the results are correct.
	 * @param stadium The {@code OlympiadStadium} object containing the current game state.
	 */
	@Override
	protected void validateRound3Winner(OlympiadStadium stadium)
	{
	}
}
