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
 * This class holds the data related to a player's challenge points.<br>
 * It serves as a container for tracking and managing these specific values within the {@code Player} entity.
 * @author Serenitty, MacuK
 */
public class ChallengePointInfoHolder
{
	private final int _pointGroupId;
	private int _challengePoint;
	private final int _ticketPointOpt1;
	private final int _ticketPointOpt2;
	private final int _ticketPointOpt3;
	private final int _ticketPointOpt4;
	private final int _ticketPointOpt5;
	private final int _ticketPointOpt6;
	
	/**
	 * Creates a new {@code ChallengePointInfoHolder} with specific point values.<br>
	 * This constructor initializes all point group and ticket options.
	 * @param pointGroupId The unique identifier for the point group.
	 * @param challengePoint The current amount of challenge points.
	 * @param ticketPointOpt1 The first optional ticket point value.
	 * @param ticketPointOpt2 The second optional ticket point value.
	 * @param ticketPointOpt3 The third optional ticket point value.
	 * @param ticketPointOpt4 The fourth optional ticket point value.
	 * @param ticketPointOpt5 The fifth optional ticket point value.
	 * @param ticketPointOpt6 The sixth optional ticket point value.
	 */
	public ChallengePointInfoHolder(int pointGroupId, int challengePoint, int ticketPointOpt1, int ticketPointOpt2, int ticketPointOpt3, int ticketPointOpt4, int ticketPointOpt5, int ticketPointOpt6)
	{
		_pointGroupId = pointGroupId;
		_challengePoint = challengePoint;
		_ticketPointOpt1 = ticketPointOpt1;
		_ticketPointOpt2 = ticketPointOpt2;
		_ticketPointOpt3 = ticketPointOpt3;
		_ticketPointOpt4 = ticketPointOpt4;
		_ticketPointOpt5 = ticketPointOpt5;
		_ticketPointOpt6 = ticketPointOpt6;
	}
	
	/**
	 * Retrieves the unique identifier for the point group.<br>
	 * This value identifies which specific group of points this holder belongs to.
	 * @return The {@code int} ID of the point group.
	 */
	public int getPointGroupId()
	{
		return _pointGroupId;
	}
	
	/**
	 * Retrieves the current number of challenge points.<br>
	 * This value is stored in the {@code _challengePoint} field.
	 * @return The total amount of challenge points as an {@code int}.
	 */
	public int getChallengePoint()
	{
		return _challengePoint;
	}
	
	/**
	 * Retrieves the first ticket point option value.<br>
	 * This value is stored in the {@code _ticketPointOpt1} field.
	 * @return The integer value of the first ticket point option.
	 */
	public int getTicketPointOpt1()
	{
		return _ticketPointOpt1;
	}
	
	/**
	 * Retrieves the second ticket point option.<br>
	 * This value is stored in the {@code _ticketPointOpt2} field.
	 * @return The integer value of the second ticket point option.
	 */
	public int getTicketPointOpt2()
	{
		return _ticketPointOpt2;
	}
	
	/**
	 * Retrieves the third ticket point option.<br>
	 * This value is stored in the {@code _ticketPointOpt3} field.
	 * @return The integer value of the third ticket point option.
	 */
	public int getTicketPointOpt3()
	{
		return _ticketPointOpt3;
	}
	
	/**
	 * Retrieves the value for ticket point option 4.<br>
	 * This method returns the fourth optional ticket point associated with the holder.
	 * @return The integer value of {@code _ticketPointOpt4}.
	 */
	public int getTicketPointOpt4()
	{
		return _ticketPointOpt4;
	}
	
	/**
	 * Retrieves the value for ticket point option 5.<br>
	 * This method returns the fifth optional ticket point associated with the holder.
	 * @return The integer value of {@code _ticketPointOpt5}.
	 */
	public int getTicketPointOpt5()
	{
		return _ticketPointOpt5;
	}
	
	/**
	 * Retrieves the value for ticket point option 6.<br>
	 * This method returns the sixth optional ticket point associated with the holder.
	 * @return The integer value of {@code _ticketPointOpt6}.
	 */
	public int getTicketPointOpt6()
	{
		return _ticketPointOpt6;
	}
	
	/**
	 * Adds a specific amount of points to the current total.<br>
	 * This method updates the {@code _challengePoint} field.
	 * @param points The number of points to add.
	 */
	public void addPoints(int points)
	{
		_challengePoint += points;
	}
}
