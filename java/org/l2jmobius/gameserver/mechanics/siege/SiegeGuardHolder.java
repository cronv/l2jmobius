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
package org.l2jmobius.gameserver.mechanics.siege;

/**
 * This class acts as a container for data related to siege guards.<br>
 * It helps manage the state and properties of guards during siege events.
 * @author St3eT
 */
public class SiegeGuardHolder
{
	private final int _castleId;
	private final int _itemId;
	private final SiegeGuardType _type;
	private final boolean _stationary;
	private final int _npcId;
	private final int _maxNpcAmount;
	
	/**
	 * Creates a new instance of {@code SiegeGuardHolder}.<br>
	 * This constructor initializes all required properties for the siege guard.
	 * @param castleId The unique identifier for the castle.
	 * @param itemId The item ID associated with the guard.
	 * @param type The {@code SiegeGuardType} of the guard.
	 * @param stationary Set to {@code true} if the guard is stationary, or {@code false} otherwise.
	 * @param npcId The unique identifier for the NPC.
	 * @param maxNpcAmount The maximum number of NPCs allowed.
	 */
	public SiegeGuardHolder(int castleId, int itemId, SiegeGuardType type, boolean stationary, int npcId, int maxNpcAmount)
	{
		_castleId = castleId;
		_itemId = itemId;
		_type = type;
		_stationary = stationary;
		_npcId = npcId;
		_maxNpcAmount = maxNpcAmount;
	}
	
	/**
	 * Retrieves the unique identifier for the castle.<br>
	 * This value is used to identify which castle a {@code SiegeGuardHolder} belongs to.
	 * @return The {@code int} ID of the castle.
	 */
	public int getCastleId()
	{
		return _castleId;
	}
	
	/**
	 * Retrieves the unique identifier for the item.<br>
	 * This value is used to identify specific siege items.
	 * @return The {@code int} ID of the item.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Retrieves the type of the siege guard.<br>
	 * This method returns the {@code SiegeGuardType} associated with this holder.
	 * @return the {@code SiegeGuardType} of the guard.
	 */
	public SiegeGuardType getType()
	{
		return _type;
	}
	
	/**
	 * Checks if the siege guard is in a stationary position.<br>
	 * Returns {@code true} if it does not move.<br>
	 * Returns {@code false} if it is mobile.
	 * @return The stationary status of the holder.
	 */
	public boolean isStationary()
	{
		return _stationary;
	}
	
	/**
	 * Retrieves the unique identifier for the NPC.<br>
	 * This value is used to identify specific NPCs in the game world.
	 * @return The {@code int} ID of the NPC.
	 */
	public int getNpcId()
	{
		return _npcId;
	}
	
	/**
	 * Retrieves the maximum number of NPCs allowed.<br>
	 * This value is used to limit the population size.
	 * @return The maximum amount as an {@code int}.
	 */
	public int getMaxNpcAmout()
	{
		return _maxNpcAmount;
	}
}
