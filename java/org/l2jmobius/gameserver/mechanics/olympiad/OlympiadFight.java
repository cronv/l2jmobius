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

/**
 * Manages the logic and state for individual fights within an {@code Olympiad} event.<br>
 * It handles combat interactions between participants during these specific competitions.
 * @author dontknowdontcare
 */
public class OlympiadFight
{
	private final String _opponentName;
	private final int _opponentClassId;
	private final int _winner; // 0 = win, 1 = loss, 2 = draw
	private final int _opponentLevel;
	
	/**
	 * Creates a new {@code OlympiadFight} instance.<br>
	 * This constructor initializes the fight details with specific opponent data.
	 * @param opponentName The name of the opponent.
	 * @param opponentLevel The level of the opponent.
	 * @param opponentClassId The class ID of the opponent.
	 * @param winner The result of the fight where {@code 0} is a win, {@code 1} is a loss, and {@code 2} is a draw.
	 */
	public OlympiadFight(String opponentName, int opponentLevel, int opponentClassId, int winner)
	{
		_opponentName = opponentName;
		_opponentClassId = opponentClassId;
		_winner = winner;
		_opponentLevel = opponentLevel;
	}
	
	/**
	 * Retrieves the name of the opponent.<br>
	 * This value is stored in the {@code _opponentName} field.
	 * @return The name of the opponent as a {@code String}.
	 */
	public String getOpponentName()
	{
		return _opponentName;
	}
	
	/**
	 * Retrieves the unique identifier for the opponent's class.<br>
	 * This value is used to identify which character type was fought.
	 * @return the {@code int} ID of the opponent's class.
	 */
	public int getOpponentClassId()
	{
		return _opponentClassId;
	}
	
	/**
	 * Retrieves the result of the olympiad fight.<br>
	 * The value represents who won the match.
	 * @return The winner status where {@code 0} is a win, {@code 1} is a loss, and {@code 2} is a draw.
	 */
	public int getWinner()
	{
		return _winner;
	}
	
	/**
	 * Retrieves the level of the opponent.<br>
	 * This value is stored during the creation of the {@code OlympiadFight}.
	 * @return The integer level of the opponent.
	 */
	public int getOpponentLevel()
	{
		return _opponentLevel;
	}
}
