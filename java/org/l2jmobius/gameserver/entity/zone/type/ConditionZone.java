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
 * Represents a zone that applies specific conditions to entities within its boundaries.<br>
 * This {@code ConditionZone} type is used to trigger effects or state changes based on location.
 * @author UnAfraid
 */
public class ConditionZone extends ZoneType
{
	private boolean NO_ITEM_DROP = false;
	private boolean NO_BOOKMARK = false;
	
	/**
	 * Creates a new {@code ConditionZone} instance.<br>
	 * This constructor initializes the zone with a specific identifier.
	 * @param id The unique identification number for this zone.
	 */
	public ConditionZone(int id)
	{
		super(id);
	}
	
	/**
	 * Sets a specific configuration parameter for this zone.<br>
	 * This method handles special keys like {@code NoBookmark} and {@code NoItemDrop}.<br>
	 * If the name is not recognized, it passes the value to the parent class.
	 * @param name The name of the parameter to set.
	 * @param value The value associated with the parameter.
	 */
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equalsIgnoreCase("NoBookmark"))
		{
			NO_BOOKMARK = Boolean.parseBoolean(value);
		}
		else if (name.equalsIgnoreCase("NoItemDrop"))
		{
			NO_ITEM_DROP = Boolean.parseBoolean(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	/**
	 * This method is called when a {@code Creature} enters the zone.<br>
	 * It checks if the creature is a player.<br>
	 * It updates specific zone flags for players based on the current settings.
	 * @param creature The {@code Creature} that entered the zone.
	 */
	@Override
	protected void onEnter(Creature creature)
	{
		if (creature.isPlayer())
		{
			if (NO_BOOKMARK)
			{
				creature.setInsideZone(ZoneId.NO_BOOKMARK, true);
			}
			
			if (NO_ITEM_DROP)
			{
				creature.setInsideZone(ZoneId.NO_ITEM_DROP, true);
			}
		}
	}
	
	/**
	 * This method is called when a {@code Creature} leaves the zone.<br>
	 * It updates specific status flags for players.<br>
	 * It checks if the player should be removed from the {@code NO_BOOKMARK} or {@code NO_ITEM_DROP} zones.
	 * @param creature The {@code Creature} that is leaving the zone.
	 */
	@Override
	protected void onExit(Creature creature)
	{
		if (creature.isPlayer())
		{
			if (NO_BOOKMARK)
			{
				creature.setInsideZone(ZoneId.NO_BOOKMARK, false);
			}
			
			if (NO_ITEM_DROP)
			{
				creature.setInsideZone(ZoneId.NO_ITEM_DROP, false);
			}
		}
	}
}
