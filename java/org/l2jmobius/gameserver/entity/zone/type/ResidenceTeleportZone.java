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

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneRespawn;

/**
 * This class handles teleportation logic specifically for residence areas.<br>
 * It is based on the {@code CastleTeleportZone} implementation used in Kerberos.<br>
 * It extends {@code ZoneRespawn} to manage zone-specific behavior.
 * @author Nyaran
 */
public class ResidenceTeleportZone extends ZoneRespawn
{
	private int _residenceId;
	
	/**
	 * Creates a new {@code ResidenceTeleportZone} instance.<br>
	 * This constructor initializes the zone using a specific identifier.
	 * @param id The unique identification number for the zone.
	 */
	public ResidenceTeleportZone(int id)
	{
		super(id);
	}
	
	/**
	 * Sets a specific configuration parameter for this zone.<br>
	 * It handles the {@code residenceId} property specifically.<br>
	 * Other parameters are passed to the parent class.
	 * @param name The name of the parameter to set.
	 * @param value The string value associated with the parameter.
	 */
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("residenceId"))
		{
			_residenceId = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	/**
	 * This method is called when a {@code Creature} enters the zone.<br>
	 * It updates the creature's internal state regarding its location.<br>
	 * The {@code creature} is marked as being inside a specific zone type.
	 * @param creature The {@code Creature} that entered the zone.
	 */
	@Override
	protected void onEnter(Creature creature)
	{
		creature.setInsideZone(ZoneId.NO_SUMMON_FRIEND, true); // FIXME: Custom ?
	}
	
	/**
	 * This method is called when a {@code Creature} leaves the zone.<br>
	 * It updates the creature's status to indicate it is no longer inside the zone.
	 * @param creature The {@code Creature} that is exiting the zone.
	 */
	@Override
	protected void onExit(Creature creature)
	{
		creature.setInsideZone(ZoneId.NO_SUMMON_FRIEND, false); // FIXME: Custom ?
	}
	
	/**
	 * Removes all players from the current zone.<br>
	 * It teleports every online {@code Player} to the spawn location.<br>
	 * The teleportation speed is set to {@code 200}.
	 */
	@Override
	public void oustAllPlayers()
	{
		for (Player player : getPlayersInside())
		{
			if ((player != null) && player.isOnline())
			{
				player.teleToLocation(getSpawnLoc(), 200);
			}
		}
	}
	
	/**
	 * Retrieves the unique identifier for the residence.<br>
	 * This value is used to identify which specific residence this zone belongs to.
	 * @return The {@code int} ID of the residence.
	 */
	public int getResidenceId()
	{
		return _residenceId;
	}
}
