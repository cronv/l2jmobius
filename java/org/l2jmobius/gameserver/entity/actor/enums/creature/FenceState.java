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
package org.l2jmobius.gameserver.entity.actor.enums.creature;

/**
 * Represents the various states of a creature's fence.<br>
 * This enum is used to track whether a creature is currently fenced or free.
 * @author Nik64
 */
public enum FenceState
{
	HIDDEN(0),
	OPENED(1),
	CLOSED(2),
	CLOSED_HIDDEN(0);
	
	private final int _clientId;
	
	/**
	 * Creates a new instance of {@code FenceState}.<br>
	 * This constructor initializes the internal ID for the state.
	 * @param clientId The unique identifier for this specific fence state.
	 */
	private FenceState(int clientId)
	{
		_clientId = clientId;
	}
	
	/**
	 * Retrieves the unique identifier for this {@code FenceState}.<br>
	 * This value is used to identify the state in the game data.
	 * @return The integer ID of the current state.
	 */
	public int getClientId()
	{
		return _clientId;
	}
	
	/**
	 * Checks if the current {@code FenceState} allows geodata to be active.<br>
	 * This is true if the fence is either {@code CLOSED} or {@code CLOSED_HIDDEN}.
	 * @return {@code true} if geodata is enabled, {@code false} otherwise.
	 */
	public boolean isGeodataEnabled()
	{
		return (this == CLOSED_HIDDEN) || (this == CLOSED);
	}
}
