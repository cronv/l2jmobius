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
package org.l2jmobius.gameserver.mechanics.undergroundColiseum;

/**
 * This class manages the logic for determining and tracking the best team in the Underground Coliseum.<br>
 * It provides helper methods to identify top-performing teams based on specific game criteria.
 */
public class UCBestTeam
{
	private final int _arenaId;
	private String _leaderName;
	private int _wins;
	
	/**
	 * Creates a new {@code UCBestTeam} object.<br>
	 * Initializes the team with an arena ID, a leader name, and a win count.
	 * @param arenaId The unique identifier for the arena.
	 * @param leaderName The name of the team leader.
	 * @param wins The total number of wins achieved by the team.
	 */
	public UCBestTeam(int arenaId, String leaderName, int wins)
	{
		_arenaId = arenaId;
		_leaderName = leaderName;
		_wins = wins;
	}
	
	/**
	 * Retrieves the unique identifier for the arena.<br>
	 * This value is used to identify which specific underground coliseum this team belongs to.
	 * @return The {@code int} ID of the arena.
	 */
	public int getArenaId()
	{
		return _arenaId;
	}
	
	/**
	 * Retrieves the name of the team leader.<br>
	 * This value is stored in the {@code _leaderName} field.
	 * @return The name of the leader as a {@code String}.
	 */
	public String getLeaderName()
	{
		return _leaderName;
	}
	
	/**
	 * Sets the name of the team leader.<br>
	 * This updates the {@code _leaderName} field.
	 * @param leader The new name for the team leader.
	 */
	public void setLeader(String leader)
	{
		_leaderName = leader;
	}
	
	/**
	 * Retrieves the total number of wins for this team.<br>
	 * This value is stored in the {@code _wins} field.
	 * @return The current win count as an {@code int}.
	 */
	public int getWins()
	{
		return _wins;
	}
	
	/**
	 * Updates the total number of wins for this team.<br>
	 * This method sets the {@code _wins} field to a new value.
	 * @param wins The new number of wins to assign.
	 */
	public void setWins(int wins)
	{
		_wins = wins;
	}
}
