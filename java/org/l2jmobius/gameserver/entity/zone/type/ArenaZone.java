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
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Represents a specific type of zone designated for Player vs Player (PVP) combat.<br>
 * This class defines the behavior and properties unique to arena environments.
 * @author durgus
 */
public class ArenaZone extends ZoneType
{
	/**
	 * Creates a new instance of an {@code ArenaZone}.<br>
	 * This constructor initializes the zone with a specific identifier.
	 * @param id The unique identification number for this zone.
	 */
	public ArenaZone(int id)
	{
		super(id);
	}
	
	/**
	 * This method is called when a {@code Creature} enters the zone.<br>
	 * It checks if the creature is a player and not already in a PVP zone.<br>
	 * If so, it sends a system message to the player.<br>
	 * Finally, it sets the PVP zone status for the creature to {@code true}.
	 * @param creature The {@code Creature} entering the zone.
	 */
	@Override
	protected void onEnter(Creature creature)
	{
		if (creature.isPlayer() && !creature.isInsideZone(ZoneId.PVP))
		{
			creature.sendPacket(SystemMessageId.YOU_HAVE_ENTERED_A_COMBAT_ZONE);
		}
		
		creature.setInsideZone(ZoneId.PVP, true);
	}
	
	/**
	 * This method is called when a {@code Creature} leaves the zone.<br>
	 * It updates the creature's status to indicate they are no longer in a PVP area.<br>
	 * If the creature is a player, it sends a system message confirming their exit.
	 * @param creature The {@code Creature} that is leaving the zone.
	 */
	@Override
	protected void onExit(Creature creature)
	{
		creature.setInsideZone(ZoneId.PVP, false);
		if (creature.isPlayer() && !creature.isInsideZone(ZoneId.PVP))
		{
			creature.sendPacket(SystemMessageId.YOU_HAVE_LEFT_A_COMBAT_ZONE);
		}
	}
}
