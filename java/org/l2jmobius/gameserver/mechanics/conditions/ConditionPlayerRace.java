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
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This class checks if a player belongs to a specific {@code Race}.<br>
 * It is used to verify race requirements for various game mechanics.
 * @author mkizub
 */
public class ConditionPlayerRace extends Condition
{
	private final Set<Race> _races;
	
	/**
	 * Creates a new {@code ConditionPlayerRace} instance.<br>
	 * This condition checks if an actor belongs to specific races.
	 * @param races A {@code Set} of allowed {@code Race} types.
	 */
	public ConditionPlayerRace(Set<Race> races)
	{
		_races = races;
	}
	
	/**
	 * Checks if the {@code effector} meets the race requirements.<br>
	 * This method returns {@code false} if the {@code effector} is not a player.<br>
	 * It verifies if the player's race is in the allowed set.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item involved in the action.
	 * @return {@code true} if the requirements are met, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		if ((effector == null) || !effector.isPlayer())
		{
			return false;
		}
		
		return _races.contains(effector.asPlayer().getRace());
	}
}
