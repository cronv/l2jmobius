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

/**
 * Checks if a {@code Player} currently has an active summon.<br>
 * This condition is used to verify the presence of summoned entities.
 * @author Sdw
 */
public class ConditionPlayerHasSummon extends Condition
{
	private final boolean _hasSummon;
	
	/**
	 * Creates a new condition to check if a player has an active summon.<br>
	 * This constructor sets the required state for the {@code ConditionPlayerHasSummon} object.
	 * @param hasSummon The boolean value representing whether the player must have a summon.
	 */
	public ConditionPlayerHasSummon(boolean hasSummon)
	{
		_hasSummon = hasSummon;
	}
	
	/**
	 * Checks if the {@code effector} has a summon based on the current state.<br>
	 * This method verifies if the {@code Player} status matches the required condition.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} receiving the action.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code ItemTemplate} involved in the action.
	 * @return {@code true} if the summon status matches, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		final Player player = effector.asPlayer();
		if (player == null)
		{
			return false;
		}
		
		return _hasSummon == player.hasSummon();
	}
}
