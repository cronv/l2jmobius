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
package org.l2jmobius.gameserver.entity.groups.matching;

/**
 * This class stores the historical data for {@code MatchingRoom} instances.<br>
 * It allows the system to track and retrieve past matching sessions.
 * @author Mobius
 */
public class MatchingRoomHistory
{
	private final String _title;
	private final String _leader;
	
	/**
	 * Creates a new instance of {@code MatchingRoomHistory}.<br>
	 * This constructor initializes the room details.
	 * @param title The name of the matching room.
	 * @param leader The name of the room leader.
	 */
	public MatchingRoomHistory(String title, String leader)
	{
		_title = title;
		_leader = leader;
	}
	
	/**
	 * Retrieves the title of the matching room.<br>
	 * This method returns the name assigned to the history record.
	 * @return The {@code String} title of the room.
	 */
	public String getTitle()
	{
		return _title;
	}
	
	/**
	 * Retrieves the name of the room leader.<br>
	 * This method returns the {@code String} value stored in the private field.
	 * @return The name of the leader as a {@code String}.
	 */
	public String getLeader()
	{
		return _leader;
	}
}
