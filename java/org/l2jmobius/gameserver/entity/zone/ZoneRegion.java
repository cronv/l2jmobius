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
package org.l2jmobius.gameserver.entity.zone;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.zone.type.PeaceZone;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * Represents a specific geographical area within a zone that has unique properties.<br>
 * It defines rules for entities, such as {@code PeaceZone} restrictions or skill limitations.<br>
 * This class is used to manage spatial logic and environmental effects.
 * @author NosBit
 */
public class ZoneRegion
{
	private final int _regionX;
	private final int _regionY;
	private final Map<Integer, ZoneType> _zones = new ConcurrentHashMap<>();
	
	/**
	 * Creates a new {@code ZoneRegion} instance.<br>
	 * This constructor sets the initial coordinates for the region.
	 * @param regionX The x-coordinate of the region.
	 * @param regionY The y-coordinate of the region.
	 */
	public ZoneRegion(int regionX, int regionY)
	{
		_regionX = regionX;
		_regionY = regionY;
	}
	
	/**
	 * Retrieves the map of zones for this region.<br>
	 * The keys represent zone IDs.
	 * @return a {@code Map<Integer, ZoneType>} containing all zones.
	 */
	public Map<Integer, ZoneType> getZones()
	{
		return _zones;
	}
	
	/**
	 * Retrieves the X coordinate of the region.<br>
	 * This value represents the horizontal position in the grid.
	 * @return The {@code int} value of the X coordinate.
	 */
	public int getRegionX()
	{
		return _regionX;
	}
	
	/**
	 * Retrieves the vertical coordinate of the region.<br>
	 * This value represents the {@code y} axis position.
	 * @return The current {@code y} coordinate as an {@code int}.
	 */
	public int getRegionY()
	{
		return _regionY;
	}
	
	/**
	 * Updates the zone status for a specific creature.<br>
	 * This method checks if the {@code Creature} is currently teleporting.<br>
	 * If not teleporting, it revalidates the entity against all active zones.
	 * @param creature The {@code Creature} to update.
	 */
	public void revalidateZones(Creature creature)
	{
		// Do NOT update the world region while the character is still in the process of teleporting.
		// Once the teleport is COMPLETED, revalidation occurs safely, at that time.
		if (creature.isTeleporting())
		{
			return;
		}
		
		for (ZoneType z : _zones.values())
		{
			z.revalidateInZone(creature);
		}
	}
	
	/**
	 * Removes the specified {@code Creature} from all active zones.<br>
	 * This method iterates through every {@code ZoneType} in this region.<br>
	 * It calls the removal logic for each zone individually.
	 * @param creature The {@code Creature} to be removed from the zones.
	 */
	public void removeFromZones(Creature creature)
	{
		for (ZoneType z : _zones.values())
		{
			z.removeCharacter(creature);
		}
	}
	
	/**
	 * Checks if a skill effect enters any {@code PeaceZone}.<br>
	 * It validates the boundaries of the skill based on its range.<br>
	 * Returns {@code false} if any part of the effect is inside a peace zone.
	 * @param skill The {@code Skill} being checked.
	 * @param x The x-coordinate of the center point.
	 * @param y The y-coordinate of the center point.
	 * @param z The z-coordinate of the center point.
	 * @return {@code true} if the effect is safe to use, or {@code false} otherwise.
	 */
	public boolean checkEffectRangeInsidePeaceZone(Skill skill, int x, int y, int z)
	{
		final int range = skill.getEffectRange();
		final int up = y + range;
		final int down = y - range;
		final int left = x + range;
		final int right = x - range;
		for (ZoneType e : _zones.values())
		{
			if (e instanceof PeaceZone)
			{
				if (e.isInsideZone(x, up, z))
				{
					return false;
				}
				
				if (e.isInsideZone(x, down, z))
				{
					return false;
				}
				
				if (e.isInsideZone(left, y, z))
				{
					return false;
				}
				
				if (e.isInsideZone(right, y, z))
				{
					return false;
				}
				
				if (e.isInsideZone(x, y, z))
				{
					return false;
				}
			}
		}
		
		return true;
	}
	
	/**
	 * This method is called when a {@code Creature} dies.<br>
	 * It checks if the creature is inside any active zones.<br>
	 * If it is, the specific zone logic for death is triggered.
	 * @param creature The {@code Creature} that has died.
	 */
	public void onDeath(Creature creature)
	{
		for (ZoneType z : _zones.values())
		{
			if (z.isInsideZone(creature))
			{
				z.onDieInside(creature);
			}
		}
	}
	
	/**
	 * This method is called when a {@code Creature} is revived.<br>
	 * It checks if the creature is inside any active zones.<br>
	 * If it is, the specific zone logic for reviving is triggered.
	 * @param creature The {@code Creature} that has been revived.
	 */
	public void onRevive(Creature creature)
	{
		for (ZoneType z : _zones.values())
		{
			if (z.isInsideZone(creature))
			{
				z.onReviveInside(creature);
			}
		}
	}
}
