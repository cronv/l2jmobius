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

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This condition checks if a target creature belongs to a specific {@code Race}.<br>
 * It is used to filter targets based on their species in the game world.
 * @author mkizub
 */
public class ConditionTargetRace extends Condition
{
	private final Race _race;
	
	/**
	 * Creates a new {@code ConditionTargetRace} instance.<br>
	 * This condition checks if the target belongs to a specific race.
	 * @param race The {@code Race} type required for this condition.
	 */
	public ConditionTargetRace(Race race)
	{
		_race = race;
	}
	
	/**
	 * Checks if the target creature belongs to a specific race.<br>
	 * This method compares the race of the {@code effected} creature against the required race.
	 * @param effector The creature performing the action.
	 * @param effected The creature being targeted by the action.
	 * @param skill The skill being used.
	 * @param item The item template involved in the action.
	 * @return {@code true} if the races match, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		return _race == effected.getRace();
	}
}
