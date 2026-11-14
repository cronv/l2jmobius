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
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This class checks the current health points of a player.<br>
 * It is used to evaluate if a creature meets specific HP requirements for mechanics or skills.
 * @author mr
 */
public class ConditionPlayerHp extends Condition
{
	private final int _hp;
	
	/**
	 * Creates a new instance of {@code ConditionPlayerHp}.<br>
	 * This constructor sets the required health value.
	 * @param hp The amount of health to check.
	 */
	public ConditionPlayerHp(int hp)
	{
		_hp = hp;
	}
	
	/**
	 * Checks if the {@code effector} has a low enough health percentage.<br>
	 * The current health of the {@code effector} is compared against the required threshold.<br>
	 * This method returns {@code true} if the condition is met.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item involved in the action.
	 * @return {@code true} if the health requirement is satisfied, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		return (effector != null) && (((effector.getCurrentHp() * 100) / effector.getMaxHp()) <= _hp);
	}
}
