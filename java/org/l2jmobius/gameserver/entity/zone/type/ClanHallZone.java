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

import org.l2jmobius.gameserver.data.xml.ClanHallData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.residences.ClanHall;
import org.l2jmobius.gameserver.entity.zone.ZoneId;

/**
 * Represents a specific zone dedicated to a {@code ClanHall}.<br>
 * This class handles the unique logic and properties for clan residence areas.
 * @author durgus
 */
public class ClanHallZone extends ResidenceZone
{
	/**
	 * Creates a new instance of {@code ClanHallZone}.<br>
	 * This constructor initializes the zone with a specific identifier.
	 * @param id The unique identification number for the zone.
	 */
	public ClanHallZone(int id)
	{
		super(id);
	}
	
	/**
	 * Sets a specific configuration parameter for this zone.<br>
	 * This method updates the internal state based on the provided name and value.<br>
	 * If the {@code name} is {@code "clanHallId"}, it parses the {@code value} as an integer.<br>
	 * Otherwise, it passes the data to the parent class.
	 * @param name The unique identifier for the parameter.
	 * @param value The string representation of the value to assign.
	 */
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("clanHallId"))
		{
			setResidenceId(Integer.parseInt(value));
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	/**
	 * This method is called when a {@code Creature} enters the zone.<br>
	 * It checks if the {@code creature} is a player.<br>
	 * If it is a player, it sets their inside zone status to {@code true}.
	 * @param creature The {@code Creature} entering the zone.
	 */
	@Override
	protected void onEnter(Creature creature)
	{
		if (creature.isPlayer())
		{
			creature.setInsideZone(ZoneId.CLAN_HALL, true);
		}
	}
	
	/**
	 * This method is called when a {@code Creature} leaves the zone.<br>
	 * It updates the internal state of the creature if it is a player.<br>
	 * The {@code ZoneId.CLAN_HALL} flag is set to {@code false}.
	 * @param creature The {@code Creature} that is exiting the zone.
	 */
	@Override
	protected void onExit(Creature creature)
	{
		if (creature.isPlayer())
		{
			creature.setInsideZone(ZoneId.CLAN_HALL, false);
		}
	}
	
	/**
	 * Retrieves the spawn location for banished players.<br>
	 * This method looks up the {@code ClanHall} data using the current residence ID.<br>
	 * It returns the specific banish location defined in the data.
	 * @return the {@code Location} of the banish point, or {@code null} if no clan hall is found.
	 */
	@Override
	public Location getBanishSpawnLoc()
	{
		final ClanHall clanHall = ClanHallData.getInstance().getClanHallById(getResidenceId());
		if (clanHall == null)
		{
			return null;
		}
		
		return clanHall.getBanishLocation();
	}
}
