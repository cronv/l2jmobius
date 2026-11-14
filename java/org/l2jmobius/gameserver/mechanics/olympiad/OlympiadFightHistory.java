/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.mechanics.olympiad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.sql.PlayerInfoTable;
import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * This class manages the historical data of fights occurring during {@code Olympiad} events.<br>
 * It handles the retrieval and storage of combat records from the database for tracking player performance.
 * @author dontknowdontcare
 */
public class OlympiadFightHistory
{
	private static final Logger LOGGER = Logger.getLogger(OlympiadFightHistory.class.getName());
	
	private static final int MAX_FIGHT_HISTORY_COUNT = 3;
	private final Player _player;
	
	private List<OlympiadFight> _fights = new ArrayList<>();
	
	/**
	 * Creates a new history object for a specific player.<br>
	 * This constructor initializes the {@code _player} field.<br>
	 * It also loads recent fights from the database if the player is not {@code null}.
	 * @param player The {@code Player} whose fight history is being loaded.
	 */
	public OlympiadFightHistory(Player player)
	{
		_player = player;
		if ((MAX_FIGHT_HISTORY_COUNT > 0) && (_player != null))
		{
			loadRecentFights(_player.getObjectId());
		}
	}
	
	/**
	 * Retrieves the list of recent olympiad fights.<br>
	 * This method returns all {@code OlympiadFight} objects stored for the player.
	 * @return a {@code List} containing the history of {@code OlympiadFight} records.
	 */
	public List<OlympiadFight> getFights()
	{
		return _fights;
	}
	
	/**
	 * Determines the correct winner format for a fight.<br>
	 * This method compares the participants and returns an integer code.<br>
	 * It handles cases where {@code parOne} is {@code null}.
	 * @param parOne The first participant in the fight.
	 * @param parTwo The second participant in the fight.
	 * @param winner The ID of the winning participant.
	 * @return An integer representing the formatted winner result.
	 */
	public int getWinnerFormat(Participant parOne, Participant parTwo, int winner)
	{
		if (parOne == null)
		{
			LOGGER.warning("OlympiadFightHistory - getWinnerFormat error parOne is null, owner = " + _player.getName());
			return 2;
		}
		
		if (_player.getObjectId() == parOne.getObjectId())
		{
			switch (winner)
			{
				case 1:
				{
					return 1;
				}
				case 2:
				{
					return 0;
				}
				default:
				{
					return 2;
				}
			}
		}
		
		switch (winner)
		{
			case 2:
			{
				return 1;
			}
			case 1:
			{
				return 0;
			}
			default:
			{
				return 2;
			}
		}
	}
	
	/**
	 * Records a new fight in the history.<br>
	 * This method adds an {@code OlympiadFight} to the list.<br>
	 * It maintains only the most recent fights based on {@code MAX_FIGHT_HISTORY_COUNT}.
	 * @param opponent The {@code Participant} who was fought.
	 * @param winner The ID of the winner from the fight.
	 */
	public void addOlympiadFight(Participant opponent, int winner)
	{
		if (opponent != null)
		{
			final OlympiadFight fight = new OlympiadFight(opponent.getName(), opponent.getLevel(), opponent.getBaseClass(), winner);
			final List<OlympiadFight> newFights = new ArrayList<>();
			newFights.add(fight);
			if ((MAX_FIGHT_HISTORY_COUNT > 1) && !_fights.isEmpty())
			{
				for (int i = 0; i < (MAX_FIGHT_HISTORY_COUNT - 1); i++)
				{
					if (_fights.size() < (i + 1))
					{
						break;
					}
					
					OlympiadFight curFight = _fights.get(i);
					newFights.add(curFight);
				}
			}
			
			_fights = newFights;
		}
	}
	
	/**
	 * Fetches the most recent olympiad fight records from the database.<br>
	 * It populates the internal list of fights based on a specific character ID.<br>
	 * The method handles result processing and logs any errors encountered during execution.
	 * @param charId The unique identifier of the character to load history for.
	 */
	private void loadRecentFights(int charId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM olympiad_fights WHERE (charOneId=? OR charTwoId=?) ORDER BY start DESC LIMIT " + MAX_FIGHT_HISTORY_COUNT))
		{
			ps.setInt(1, charId);
			ps.setInt(2, charId);
			try (ResultSet rset = ps.executeQuery())
			{
				int charOneId;
				int charOneClass;
				int charTwoId;
				int charTwoClass;
				int winner;
				int opponentCharId;
				int opponentLevel;
				int opponentClassId;
				int winInfo;
				while (rset.next())
				{
					charOneId = rset.getInt("charOneId");
					charOneClass = rset.getInt("charOneClass");
					charTwoId = rset.getInt("charTwoId");
					charTwoClass = rset.getInt("charTwoClass");
					winner = rset.getInt("winner");
					opponentLevel = charOneId == charId ? rset.getInt("charTwoLevel") : rset.getInt("charOneLevel");
					opponentCharId = charOneId == charId ? charTwoId : charOneId;
					opponentClassId = charOneId == charId ? charTwoClass : charOneClass;
					
					final String name = PlayerInfoTable.getInstance().getNameById(opponentCharId);
					if (name != null)
					{
						// WinInfo: 0 = win, 1 = loss, 2 = draw.
						if (charOneId == charId)
						{
							switch (winner)
							{
								case 1:
								{
									winInfo = 1;
									break;
								}
								case 2:
								{
									winInfo = 0;
									break;
								}
								default:
								{
									winInfo = 2;
									break;
								}
							}
						}
						else
						{
							switch (winner)
							{
								case 2:
								{
									winInfo = 1;
									break;
								}
								case 1:
								{
									winInfo = 0;
									break;
								}
								default:
								{
									winInfo = 2;
									break;
								}
							}
						}
						
						_fights.add(new OlympiadFight(name, opponentLevel, opponentClassId, winInfo));
					}
					else
					{
						LOGGER.severe("OlympiadFightHistory could not load opponent name for char id = " + opponentCharId);
					}
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning("OlympiadFightHistory: could not load fights history for CharId: " + charId + ": " + e);
		}
	}
}
