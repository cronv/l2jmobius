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
import org.l2jmobius.gameserver.entity.zone.ZoneType;

/**
 * Represents a zone that uses scripts to define its behavior.<br>
 * It requires a script reference to handle {@code onEnter()} and {@code onExit()} events.
 * @author durgus
 */
public class ScriptZone extends ZoneType
{
	/**
	 * Creates a new instance of a {@code ScriptZone}.<br>
	 * This constructor initializes the zone with a specific identifier.
	 * @param id The unique identification number for this zone.
	 */
	public ScriptZone(int id)
	{
		super(id);
	}
	
	/**
	 * This method is called when a {@code Creature} enters the zone.<br>
	 * It updates the creature's status to indicate it is inside a script zone.
	 * @param creature The {@code Creature} that entered the zone.
	 */
	@Override
	protected void onEnter(Creature creature)
	{
		creature.setInsideZone(ZoneId.SCRIPT, true);
	}
	
	/**
	 * This method is called when a {@code Creature} leaves the zone.<br>
	 * It updates the creature's status to indicate it is no longer inside the script zone.
	 * @param creature The {@code Creature} that is exiting the zone.
	 */
	@Override
	protected void onExit(Creature creature)
	{
		creature.setInsideZone(ZoneId.SCRIPT, false);
	}
}
