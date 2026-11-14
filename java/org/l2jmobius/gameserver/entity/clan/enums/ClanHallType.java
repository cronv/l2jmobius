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
package org.l2jmobius.gameserver.entity.clan.enums;

/**
 * Defines the different types of halls available for clans.<br>
 * This enumeration is used to categorize and identify specific clan hall variations.
 * @author St3eT
 */
public enum ClanHallType
{
	AUCTIONABLE(0),
	SIEGEABLE(1),
	OTHER(2);
	
	private final int _clientVal;
	
	/**
	 * This is a private constructor for the {@code ClanHallType} enum.<br>
	 * It initializes the internal value used by the game client.
	 * @param clientVal The integer value assigned to this specific hall type.
	 */
	private ClanHallType(int clientVal)
	{
		_clientVal = clientVal;
	}
	
	/**
	 * Retrieves the internal value used by the game client.<br>
	 * This value corresponds to the {@code _clientVal} field.
	 * @return The integer value associated with this {@code ClanHallType}.
	 */
	public int getClientVal()
	{
		return _clientVal;
	}
}
