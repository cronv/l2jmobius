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

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.custom.SayuneForAllConfig;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.tasks.player.FlyMoveStartTask;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;

/**
 * Represents a specific zone type for the Sayune area.<br>
 * It handles unique behaviors and restrictions specific to this region.
 * @author UnAfraid
 */
public class SayuneZone extends ZoneType
{
	private int _mapId = -1;
	
	/**
	 * Creates a new {@code SayuneZone} instance.<br>
	 * This constructor initializes the zone with a specific identifier.
	 * @param id The unique identifier for the zone.
	 */
	public SayuneZone(int id)
	{
		super(id);
	}
	
	/**
	 * Sets a specific configuration parameter for this zone.<br>
	 * This method updates the internal state based on the provided name and value.<br>
	 * It handles special cases like {@code mapId} before passing others to the parent class.
	 * @param name The name of the parameter to set.
	 * @param value The string value associated with the parameter.
	 */
	@Override
	public void setParameter(String name, String value)
	{
		switch (name)
		{
			case "mapId":
			{
				_mapId = Integer.parseInt(value);
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
	 * It checks if the creature is a {@code Player} who meets specific requirements.<br>
	 * If valid, it enables free jumping and starts a flight task for the player.
	 * @param creature The {@code Creature} entering the zone.
	 */
	@Override
	protected void onEnter(Creature creature)
	{
		if (creature.isPlayer() && (creature.isInCategory(CategoryType.SIXTH_CLASS_GROUP) || SayuneForAllConfig.FREE_JUMPS_FOR_ALL) && !creature.isTransformed())
		{
			final Player player = creature.asPlayer();
			if (!player.isMounted())
			{
				creature.setInsideZone(ZoneId.SAYUNE, true);
				ThreadPool.execute(new FlyMoveStartTask(this, player));
			}
		}
	}
	
	/**
	 * This method is called when a {@code Creature} leaves the zone.<br>
	 * It checks if the creature is a {@code Player}.<br>
	 * If it is a player, it updates their zone status to {@code false}.
	 * @param creature The {@code Creature} that is leaving the zone.
	 */
	@Override
	protected void onExit(Creature creature)
	{
		if (creature.isPlayer())
		{
			creature.setInsideZone(ZoneId.SAYUNE, false);
		}
	}
	
	/**
	 * Retrieves the unique identifier for the map associated with this zone.<br>
	 * This value is used to identify specific locations in the game world.
	 * @return The {@code int} ID of the map.
	 */
	public int getMapId()
	{
		return _mapId;
	}
}
