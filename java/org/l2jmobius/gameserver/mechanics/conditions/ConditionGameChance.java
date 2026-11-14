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

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This class represents a condition that is checked based on a random probability.<br>
 * It determines if an action should occur by evaluating a specific chance percentage.<br>
 * Use {@code ConditionGameChance} when you need to trigger mechanics randomly.
 * @author Advi
 */
public class ConditionGameChance extends Condition
{
	private final int _chance;
	
	/**
	 * Creates a new instance of {@code ConditionGameChance}.<br>
	 * This condition determines if an action succeeds based on a probability.
	 * @param chance The percentage chance for the condition to succeed.
	 */
	public ConditionGameChance(int chance)
	{
		_chance = chance;
	}
	
	/**
	 * Checks if a random chance succeeds.<br>
	 * This method compares a random number against the internal chance value.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code ItemTemplate} involved in the action.
	 * @return {@code true} if the random roll is successful, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		return Rnd.get(100) < _chance;
	}
}
