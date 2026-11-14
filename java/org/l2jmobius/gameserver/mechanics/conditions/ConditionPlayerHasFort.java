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
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * Checks if a player belongs to a clan that owns a fort.<br>
 * This condition is used to verify ownership of specific territory.<br>
 * It returns {@code true} if the requirements are met.
 * @author MrPoke
 */
public class ConditionPlayerHasFort extends Condition
{
	private final int _fort;
	
	/**
	 * Creates a new {@code ConditionPlayerHasFort} instance.<br>
	 * This condition checks if a player possesses a specific fort.
	 * @param fort The unique identifier for the fort.
	 */
	public ConditionPlayerHasFort(int fort)
	{
		_fort = fort;
	}
	
	/**
	 * Checks if the {@code effector} meets the fortress requirements.<br>
	 * The {@code effector} must be a player to pass this check.<br>
	 * If the player has no clan, it checks against the required fort ID.<br>
	 * It returns {@code true} if the player belongs to the correct fortress.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item involved in the action.
	 * @return {@code true} if the condition is met, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		if (!effector.isPlayer())
		{
			return false;
		}
		
		final Clan clan = effector.asPlayer().getClan();
		if (clan == null)
		{
			return _fort == 0;
		}
		
		// Any fortress
		if (_fort == -1)
		{
			return clan.getFortId() > 0;
		}
		
		return clan.getFortId() == _fort;
	}
}
