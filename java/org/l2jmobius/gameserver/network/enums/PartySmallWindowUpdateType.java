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
package org.l2jmobius.gameserver.network.enums;

import org.l2jmobius.gameserver.interfaces.MaskComponent;

/**
 * Defines the types of updates sent for the small party window.<br>
 * This enum is used to categorize different network synchronization events for parties.
 * @author UnAfraid
 */
public enum PartySmallWindowUpdateType implements MaskComponent
{
	CURRENT_CP(1),
	MAX_CP(2),
	CURRENT_HP(4),
	MAX_HP(8),
	CURRENT_MP(16),
	MAX_MP(32),
	LEVEL(64),
	CLASS_ID(128),
	PARTY_SUBSTITUTE(256),
	VITALITY_POINTS(512);
	
	private final int _mask;
	
	/**
	 * Creates a new instance of {@code PartySmallWindowUpdateType}.<br>
	 * This constructor assigns the bitmask value to the internal field.
	 * @param mask The integer value representing the specific update type.
	 */
	private PartySmallWindowUpdateType(int mask)
	{
		_mask = mask;
	}
	
	/**
	 * Returns the bitmask associated with this update type.<br>
	 * This value is used to identify specific data fields in a network packet.
	 * @return The integer mask value for this {@code PartySmallWindowUpdateType}.
	 */
	@Override
	public int getMask()
	{
		return _mask;
	}
}
