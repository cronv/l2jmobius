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
package org.l2jmobius.gameserver.entity.teleporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Represents a specific destination point for teleportation.<br>
 * This class extends {@code Location} to provide data for teleporters in the game world.
 * @author UnAfraid
 */
public class TeleportLocation extends Location
{
	private final int _id;
	private final String _name;
	private final NpcStringId _npcStringId;
	private final int _questZoneId;
	private final int _feeId;
	private final long _feeCount;
	private final List<Integer> _castleId;
	
	/**
	 * Creates a new {@code TeleportLocation} instance.<br>
	 * This constructor initializes the location using data from a {@code StatSet}.<br>
	 * It sets properties like name, quest zone, and fees based on the provided set.
	 * @param id The unique identifier for this teleport location.
	 * @param set The {@code StatSet} containing all configuration data.
	 */
	public TeleportLocation(int id, StatSet set)
	{
		super(set);
		_id = id;
		_name = set.getString("name", null);
		_npcStringId = NpcStringId.getNpcStringIdOrDefault(set.getInt("npcStringId", -1), null);
		_questZoneId = set.getInt("questZoneId", 0);
		_feeId = set.getInt("feeId", Inventory.ADENA_ID);
		_feeCount = set.getLong("feeCount", 0);
		
		final String castleIds = set.getString("castleId", "");
		if (castleIds.isEmpty())
		{
			_castleId = Collections.emptyList();
		}
		else if (!castleIds.contains(";"))
		{
			_castleId = Collections.singletonList(Integer.parseInt(castleIds));
		}
		else
		{
			_castleId = new ArrayList<>();
			for (String castleId : castleIds.split(";"))
			{
				_castleId.add(Integer.parseInt(castleId));
			}
		}
	}
	
	/**
	 * Retrieves the unique identifier for this {@code TeleportLocation}.<br>
	 * This ID is used to distinguish between different teleport points.
	 * @return The unique integer ID of the teleport location.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the display name of the teleport location.<br>
	 * This value is used to identify the destination in the game UI.
	 * @return The {@code String} name of the location.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Retrieves the unique identifier for the NPC string.<br>
	 * This ID is used to identify specific text or data associated with the NPC.
	 * @return the {@code NpcStringId} of this teleport location.
	 */
	public NpcStringId getNpcStringId()
	{
		return _npcStringId;
	}
	
	/**
	 * Retrieves the unique identifier for the quest zone.<br>
	 * This value is used to determine if a location belongs to a specific quest area.
	 * @return The {@code int} ID of the quest zone.
	 */
	public int getQuestZoneId()
	{
		return _questZoneId;
	}
	
	/**
	 * Retrieves the unique identifier for the teleport fee.<br>
	 * This value is used to determine which item or currency is required.
	 * @return The {@code int} ID of the fee.
	 */
	public int getFeeId()
	{
		return _feeId;
	}
	
	/**
	 * Retrieves the total number of fees required for this teleport.<br>
	 * This value is stored as a {@code long}.
	 * @return The count of fees.
	 */
	public long getFeeCount()
	{
		return _feeCount;
	}
	
	/**
	 * Retrieves the list of castle identifiers associated with this location.<br>
	 * This method returns a {@code List<Integer>} containing all relevant IDs.
	 * @return A {@code List<Integer>} of castle IDs.
	 */
	public List<Integer> getCastleId()
	{
		return _castleId;
	}
}
