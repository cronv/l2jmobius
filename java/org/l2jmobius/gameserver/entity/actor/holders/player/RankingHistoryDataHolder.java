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
package org.l2jmobius.gameserver.entity.actor.holders.player;

/**
 * This class holds the historical ranking data for a player.<br>
 * It stores information used to track and display progress over time.
 * @author Serenitty
 */
public class RankingHistoryDataHolder
{
	private final int _rank;
	private final long _exp;
	private final long _day;
	
	/**
	 * Creates a new {@code RankingHistoryDataHolder} object.<br>
	 * This constructor initializes the history data for a specific date.
	 * @param day The day value to store.
	 * @param rank The ranking position of the player.
	 * @param exp The experience points earned by the player.
	 */
	public RankingHistoryDataHolder(long day, int rank, long exp)
	{
		_day = day;
		_rank = rank;
		_exp = exp;
	}
	
	/**
	 * Retrieves the day associated with this ranking history.<br>
	 * This value represents the specific day the data was recorded.
	 * @return The day as a {@code long}.
	 */
	public long getDay()
	{
		return _day;
	}
	
	/**
	 * Retrieves the ranking position of the player.<br>
	 * This value represents the numerical rank stored in the {@code RankingHistoryDataHolder}.
	 * @return The integer rank of the player.
	 */
	public int getRank()
	{
		return _rank;
	}
	
	/**
	 * Retrieves the experience points for this ranking record.<br>
	 * This value is stored as a {@code long}.
	 * @return The total experience points.
	 */
	public long getExp()
	{
		return _exp;
	}
}
