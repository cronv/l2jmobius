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

import java.util.List;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * Checks if a player belongs to a clan that owns a clan hall.<br>
 * This condition is used to verify the existence of a specific clan property.
 * @author MrPoke
 */
public class ConditionPlayerHasClanHall extends Condition
{
	private final List<Integer> _clanHall;
	
	/**
	 * Creates a new instance of {@code ConditionPlayerHasClanHall}.<br>
	 * This condition checks if a player owns specific clan halls.
	 * @param clanHall A {@code List} of integers representing the required clan hall IDs.
	 */
	public ConditionPlayerHasClanHall(List<Integer> clanHall)
	{
		_clanHall = clanHall;
	}
	
	/**
	 * Checks if the {@code effector} meets the requirements for a clan hall.<br>
	 * This method verifies if the player belongs to a valid hideout.<br>
	 * It returns {@code true} if the condition is met and {@code false} otherwise.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item involved in the action.
	 * @return {@code true} if the player has a valid clan hall; {@code false} otherwise.
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
			return (_clanHall.size() == 1) && (_clanHall.get(0) == 0);
		}
		
		// All Clan Hall
		if ((_clanHall.size() == 1) && (_clanHall.get(0) == -1))
		{
			return clan.getHideoutId() > 0;
		}
		
		return _clanHall.contains(clan.getHideoutId());
	}
}
