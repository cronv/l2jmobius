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
package org.l2jmobius.gameserver.data.holders;

/**
 * This class holds the reward data associated with a monster book.<br>
 * It stores information used to determine what items or rewards are granted upon completion.
 * @author Mobius
 */
public class MonsterBookRewardHolder
{
	private final int _kills;
	private final long _exp;
	private final int _sp;
	private final int _points;
	
	/**
	 * Creates a new {@code MonsterBookRewardHolder} with specific rewards.<br>
	 * This constructor initializes the kill count, experience, skill points, and reward points.
	 * @param kills The number of monsters killed.
	 * @param exp The amount of experience gained.
	 * @param sp The amount of skill points awarded.
	 * @param points The amount of reward points granted.
	 */
	public MonsterBookRewardHolder(int kills, long exp, int sp, int points)
	{
		_kills = kills;
		_exp = exp;
		_sp = sp;
		_points = points;
	}
	
	/**
	 * Retrieves the total number of kills.<br>
	 * This value is stored in the {@code MonsterBookRewardHolder}.
	 * @return The number of kills as an {@code int}.
	 */
	public int getKills()
	{
		return _kills;
	}
	
	/**
	 * Retrieves the experience points awarded.<br>
	 * This value is stored in the {@code MonsterBookRewardHolder}.
	 * @return The total amount of experience as a {@code long}.
	 */
	public long getExp()
	{
		return _exp;
	}
	
	/**
	 * Retrieves the amount of SP awarded.<br>
	 * This value is stored in the {@code MonsterBookRewardHolder}.
	 * @return The current SP value as an {@code int}.
	 */
	public int getSp()
	{
		return _sp;
	}
	
	/**
	 * Retrieves the total number of points awarded.<br>
	 * This value is stored in the {@code MonsterBookRewardHolder}.
	 * @return The current point count as an {@code int}.
	 */
	public int getPoints()
	{
		return _points;
	}
}
