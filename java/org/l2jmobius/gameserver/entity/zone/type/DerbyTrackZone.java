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
 * Represents the specific zone type for the Monster Derby track.<br>
 * This class defines the behavior and properties unique to this racing area.
 * @author durgus
 */
public class DerbyTrackZone extends ZoneType
{
	/**
	 * Creates a new instance of {@code DerbyTrackZone}.<br>
	 * This constructor initializes the zone with a specific identifier.
	 * @param id The unique identification number for this zone.
	 */
	public DerbyTrackZone(int id)
	{
		super(id);
	}
	
	/**
	 * This method is called when a {@code Creature} enters the zone.<br>
	 * It checks if the creature is playable.<br>
	 * If it is, the creature's inside zone status is updated to {@code true}.
	 * @param creature The {@code Creature} entering the zone.
	 */
	@Override
	protected void onEnter(Creature creature)
	{
		if (creature.isPlayable())
		{
			creature.setInsideZone(ZoneId.MONSTER_TRACK, true);
		}
	}
	
	/**
	 * This method is called when a {@code Creature} leaves the zone.<br>
	 * It updates the zone status for playable characters.
	 * @param creature The {@code Creature} that is leaving the zone.
	 */
	@Override
	protected void onExit(Creature creature)
	{
		if (creature.isPlayable())
		{
			creature.setInsideZone(ZoneId.MONSTER_TRACK, false);
		}
	}
}
