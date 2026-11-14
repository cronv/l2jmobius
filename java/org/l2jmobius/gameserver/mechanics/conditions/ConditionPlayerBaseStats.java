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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;

/**
 * This class checks if a {@code Player} meets specific requirements based on their base statistics.<br>
 * It evaluates whether the player's current stats satisfy the conditions defined in {@code BaseStat}.
 * @author mkizub
 */
public class ConditionPlayerBaseStats extends Condition
{
	private final BaseStat _stat;
	private final int _value;
	
	/**
	 * Creates a new {@code ConditionPlayerBaseStats} instance.<br>
	 * This condition checks for specific base statistics on a creature.
	 * @param creature The {@code Creature} to which this condition applies.
	 * @param stat The {@code BaseStat} type to check.
	 * @param value The required integer {@code value} for the statistic.
	 */
	public ConditionPlayerBaseStats(Creature creature, BaseStat stat, int value)
	{
		super();
		_stat = stat;
		_value = value;
	}
	
	/**
	 * Checks if the {@code effector} meets specific base stat requirements.<br>
	 * This method verifies that a {@code Player} has enough of a required stat.<br>
	 * It returns {@code false} if the {@code effector} is not a {@code Player}.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item template involved in the action.
	 * @return {@code true} if the player meets the stat requirement, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		final Player player = effector.asPlayer();
		if (player == null)
		{
			return false;
		}
		
		switch (_stat)
		{
			case INT:
			{
				return player.getINT() >= _value;
			}
			case STR:
			{
				return player.getSTR() >= _value;
			}
			case CON:
			{
				return player.getCON() >= _value;
			}
			case DEX:
			{
				return player.getDEX() >= _value;
			}
			case MEN:
			{
				return player.getMEN() >= _value;
			}
			case WIT:
			{
				return player.getWIT() >= _value;
			}
		}
		
		return false;
	}
}
