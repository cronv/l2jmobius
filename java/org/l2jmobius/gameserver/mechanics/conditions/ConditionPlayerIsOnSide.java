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
import org.l2jmobius.gameserver.mechanics.siege.CastleSide;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * Checks if a player is currently on a specific side.<br>
 * This condition is typically used for siege mechanics involving {@code CastleSide}.
 * @author St3eT
 */
public class ConditionPlayerIsOnSide extends Condition
{
	private final CastleSide _side;
	
	/**
	 * Creates a new condition to check if a player is on a specific side.<br>
	 * This condition uses the {@code CastleSide} provided during construction.
	 * @param side The {@code CastleSide} that the player must be on.
	 */
	public ConditionPlayerIsOnSide(CastleSide side)
	{
		_side = side;
	}
	
	/**
	 * Checks if the {@code effector} is on the correct side.<br>
	 * This method returns {@code false} if the {@code effector} is {@code null} or not a player.<br>
	 * It verifies that the player's side matches the required {@code CastleSide}.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item involved in the action.
	 * @return {@code true} if the player is on the correct side, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		if ((effector == null) || !effector.isPlayer())
		{
			return false;
		}
		
		return effector.asPlayer().getPlayerSide() == _side;
	}
}
