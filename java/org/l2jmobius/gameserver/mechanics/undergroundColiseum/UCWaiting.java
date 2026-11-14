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

import org.l2jmobius.gameserver.entity.groups.Party;

/**
 * Manages the waiting state for players in the Underground Coliseum.<br>
 * This class handles logic related to participants who are queued before a match begins.
 */
public class UCWaiting
{
	private final Party _party;
	private long _registerMillis;
	private final UCArena _baseArena;
	
	/**
	 * Creates a new instance of {@code UCWaiting}.<br>
	 * This constructor initializes the waiting state for a specific group.
	 * @param party The {@code Party} that is currently waiting.
	 * @param baseArena The {@code UCArena} associated with this wait.
	 */
	public UCWaiting(Party party, UCArena baseArena)
	{
		_party = party;
		_baseArena = baseArena;
	}
	
	/**
	 * Resets the registration timer to {@code 0L}.<br>
	 * This method clears the current progress for the waiting process.
	 */
	public void clean()
	{
		_registerMillis = 0L;
	}
	
	/**
	 * Retrieves the base arena associated with this waiting instance.<br>
	 * This method returns the {@code UCArena} object used for the current session.
	 * @return The {@code UCArena} instance.
	 */
	public UCArena getBaseArena()
	{
		return _baseArena;
	}
	
	/**
	 * Retrieves the {@code Party} associated with this waiting instance.<br>
	 * This method checks if the party has a leader.<br>
	 * If no leader is found, it sets the active status to {@code false}.
	 * @return the {@code Party} object or {@code null} if none exists.
	 */
	public Party getParty()
	{
		if ((_party != null) && (_party.getLeader() == null))
		{
			setParty(false);
		}
		
		return _party;
	}
	
	/**
	 * Updates the state of the associated {@code Party}.<br>
	 * If {@code isActive} is {@code true}, it sets the party's state to this instance.<br>
	 * If {@code isActive} is {@code false}, it clears the party's state.
	 * @param isActive The status determining if the party should be linked to this waiting instance.
	 */
	public void setParty(boolean isActive)
	{
		if (isActive)
		{
			_party.setUCState(this);
		}
		else
		{
			_party.setUCState(null);
		}
	}
	
	/**
	 * Updates the registration timestamp to the current system time.<br>
	 * This method records when the party successfully registered for the arena.
	 */
	public void hasRegisterdNow()
	{
		_registerMillis = System.currentTimeMillis();
	}
	
	/**
	 * Returns the registration time in milliseconds.<br>
	 * This value represents how long ago the registration occurred.
	 * @return The registration time as a {@code long}.
	 */
	public long getRegisterMillis()
	{
		return _registerMillis;
	}
}
