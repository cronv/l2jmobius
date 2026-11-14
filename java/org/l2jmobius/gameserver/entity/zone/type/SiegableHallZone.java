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
package org.l2jmobius.gameserver.entity.zone.type;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * Represents a specific type of clan hall that can be conquered by other clans.<br>
 * This class extends {@code ClanHallZone} to provide unique logic for siege mechanics.
 * @author BiggBoss
 */
public class SiegableHallZone extends ClanHallZone
{
	private List<Location> _challengerLocations;
	
	/**
	 * Creates a new instance of {@code SiegableHallZone}.<br>
	 * This constructor initializes the zone with a specific identifier.
	 * @param id The unique identification number for this zone.
	 */
	public SiegableHallZone(int id)
	{
		super(id);
	}
	
	/**
	 * Parses a location and adds it to the appropriate list.<br>
	 * This method handles specific coordinates for different zone types.<br>
	 * If the {@code type} is {@code challenger}, it saves the point as a spawn.<br>
	 * Otherwise, it passes the data to the parent class.
	 * @param x The X coordinate of the location.
	 * @param y The Y coordinate of the location.
	 * @param z The Z coordinate of the location.
	 * @param type The category of the location to be parsed.
	 */
	@Override
	public void parseLoc(int x, int y, int z, String type)
	{
		if ((type != null) && type.equals("challenger"))
		{
			if (_challengerLocations == null)
			{
				_challengerLocations = new ArrayList<>();
			}
			
			_challengerLocations.add(new Location(x, y, z));
		}
		else
		{
			super.parseLoc(x, y, z, type);
		}
	}
	
	/**
	 * Retrieves the list of spawn points for challengers.<br>
	 * These locations are used to place challenger entities within the zone.
	 * @return a {@code List} of {@code Location} objects.
	 */
	public List<Location> getChallengerSpawns()
	{
		return _challengerLocations;
	}
	
	/**
	 * Removes players from the zone who are not part of the siege.<br>
	 * It checks every {@code Player} inside the current area.<br>
	 * Players who are not in a hideout siege are moved to the banish location.
	 */
	public void banishNonSiegeParticipants()
	{
		for (Player player : getPlayersInside())
		{
			if ((player != null) && player.isInHideoutSiege())
			{
				player.teleToLocation(getBanishSpawnLoc(), true);
			}
		}
	}
}
