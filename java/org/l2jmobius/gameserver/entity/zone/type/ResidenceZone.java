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

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.ZoneRespawn;

/**
 * Represents a specialized zone for player residences.<br>
 * This class extends {@code ZoneRespawn} to provide specific behaviors for residential areas.
 * @author xban1x
 */
public abstract class ResidenceZone extends ZoneRespawn
{
	private int _residenceId;
	
	/**
	 * Creates a new instance of a {@code ResidenceZone}.<br>
	 * This constructor initializes the zone with a specific identifier.
	 * @param id The unique identification number for the zone.
	 */
	protected ResidenceZone(int id)
	{
		super(id);
	}
	
	/**
	 * Removes all players from the zone who are not members of the specified clan.<br>
	 * Players belonging to {@code owningClanId} will remain in the zone.<br>
	 * All other players are teleported to the banish spawn location.
	 * @param owningClanId The unique ID of the clan allowed to stay in the residence.
	 */
	public void banishForeigners(int owningClanId)
	{
		for (Player temp : getPlayersInside())
		{
			if ((owningClanId != 0) && (temp.getClanId() == owningClanId))
			{
				continue;
			}
			
			temp.teleToLocation(getBanishSpawnLoc(), true);
		}
	}
	
	/**
	 * Sets the unique identifier for this residence.<br>
	 * This value is used to identify specific residential zones.
	 * @param residenceId The {@code int} ID of the residence.
	 */
	protected void setResidenceId(int residenceId)
	{
		_residenceId = residenceId;
	}
	
	/**
	 * Retrieves the unique identifier for this residence.<br>
	 * This ID is used to identify specific residential zones.
	 * @return The {@code int} value of the residence ID.
	 */
	public int getResidenceId()
	{
		return _residenceId;
	}
}
