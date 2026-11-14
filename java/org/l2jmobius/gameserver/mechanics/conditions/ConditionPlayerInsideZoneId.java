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
package org.l2jmobius.gameserver.mechanics.conditions;

import java.util.Set;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * Condition checking whether the player is inside one of the given zone ids.
 * @author UnAfraid
 */
public class ConditionPlayerInsideZoneId extends Condition
{
	private final Set<Integer> _zones;
	
	/**
	 * Creates a condition matching the given zone ids.
	 * @param zones The allowed zone ids.
	 */
	public ConditionPlayerInsideZoneId(Set<Integer> zones)
	{
		_zones = zones;
	}
	
	/**
	 * Checks whether the effector is a player inside one of the allowed zones.
	 * @param effector The acting {@code Creature}.
	 * @param effected The target {@code Creature}.
	 * @param skill The {@code Skill} used.
	 * @param item The {@code ItemTemplate} involved.
	 * @return {@code true} if the condition is met, {@code false} otherwise.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		if (effector.asPlayer() == null)
		{
			return false;
		}
		
		for (ZoneType zone : ZoneManager.getInstance().getZones(effector))
		{
			if (_zones.contains(zone.getId()))
			{
				return true;
			}
		}
		
		return false;
	}
}
