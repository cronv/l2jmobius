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

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.zone.ZoneType;

/**
 * Represents a specific zone type that triggers a teleportation effect.<br>
 * It handles moving a {@code Creature} to a new {@code Location} when they enter the area.
 * @author Sdw
 */
public class TeleportZone extends ZoneType
{
	private int _x = -1;
	private int _y = -1;
	private int _z = -1;
	
	/**
	 * Creates a new {@code TeleportZone} instance.<br>
	 * This constructor initializes the zone with a specific identifier.<br>
	 * It sets the default target type to {@code InstanceType.Player}.
	 * @param id The unique identification number for this zone.
	 */
	public TeleportZone(int id)
	{
		super(id);
		setTargetType(InstanceType.Player); // Default only player.
	}
	
	/**
	 * Sets a specific configuration parameter for this zone.<br>
	 * This method updates coordinates like {@code oustX}, {@code oustY}, or {@code oustZ}.<br>
	 * If the name is not recognized, it calls the parent {@code setParameter} method.
	 * @param name The name of the parameter to update.
	 * @param value The string value associated with the parameter.
	 */
	@Override
	public void setParameter(String name, String value)
	{
		switch (name)
		{
			case "oustX":
			{
				_x = Integer.parseInt(value);
				break;
			}
			case "oustY":
			{
				_y = Integer.parseInt(value);
				break;
			}
			case "oustZ":
			{
				_z = Integer.parseInt(value);
				break;
			}
			default:
			{
				super.setParameter(name, value);
				break;
			}
		}
	}
	
	/**
	 * This method is called when a {@code Creature} enters the zone.<br>
	 * It teleports the creature to the predefined coordinates.
	 * @param creature The {@code Creature} that entered the zone.
	 */
	@Override
	protected void onEnter(Creature creature)
	{
		creature.teleToLocation(new Location(_x, _y, _z));
	}
	
	/**
	 * This method is called when a {@code Creature} leaves the zone.<br>
	 * It handles any logic required for exiting the area.
	 * @param creature The {@code Creature} that is leaving the zone.
	 */
	@Override
	protected void onExit(Creature creature)
	{
	}
}
