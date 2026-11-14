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
import org.l2jmobius.gameserver.entity.zone.ZoneId;

/**
 * Represents a specific type of residence area designated as a castle.<br>
 * This class handles the unique properties and behaviors associated with castle zones.
 * @author durgus
 */
public class CastleZone extends ResidenceZone
{
	/**
	 * Creates a new instance of {@code CastleZone}.<br>
	 * This constructor initializes the zone with a specific identifier.
	 * @param id The unique identification number for the zone.
	 */
	public CastleZone(int id)
	{
		super(id);
	}
	
	/**
	 * Sets a specific configuration parameter for this zone.<br>
	 * It handles the {@code castleId} key specially by converting it to an integer.<br>
	 * Other keys are passed to the parent class.
	 * @param name The name of the parameter to set.
	 * @param value The string value associated with the parameter.
	 */
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("castleId"))
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
	 * It updates the creature's status to indicate it is inside the castle.
	 * @param creature The {@code Creature} entering the zone.
	 */
	@Override
	protected void onEnter(Creature creature)
	{
		creature.setInsideZone(ZoneId.CASTLE, true);
	}
	
	/**
	 * This method is called when a {@code Creature} leaves the castle zone.<br>
	 * It updates the creature's status to indicate it is no longer inside the {@code ZoneId.CASTLE}.
	 * @param creature The {@code Creature} that is exiting the zone.
	 */
	@Override
	protected void onExit(Creature creature)
	{
		creature.setInsideZone(ZoneId.CASTLE, false);
	}
}
