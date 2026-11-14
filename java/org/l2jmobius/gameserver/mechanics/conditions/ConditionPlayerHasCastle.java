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
 * Condition checking whether the player owns the required castle.
 * @author MrPoke
 */
public class ConditionPlayerHasCastle extends Condition
{
	private final int _castle;
	
	/**
	 * Creates a condition matching the given castle id.
	 * @param castle The castle id, {@code -1} for any, or {@code 0} for none.
	 */
	public ConditionPlayerHasCastle(int castle)
	{
		_castle = castle;
	}
	
	/**
	 * Checks whether the effector is a player whose clan owns the required castle.
	 * @param effector The acting {@code Creature}.
	 * @param effected The target {@code Creature}.
	 * @param skill The {@code Skill} used.
	 * @param item The {@code ItemTemplate} involved.
	 * @return {@code true} if the condition is met, {@code false} otherwise.
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
			return _castle == 0;
		}
		
		// Any castle
		if (_castle == -1)
		{
			return clan.getCastleId() > 0;
		}
		
		return clan.getCastleId() == _castle;
	}
}
