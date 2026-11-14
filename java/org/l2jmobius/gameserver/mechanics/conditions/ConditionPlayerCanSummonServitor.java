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
 * Condition checking whether the player may summon a servitor.
 * @author Sdw
 */
public class ConditionPlayerCanSummonServitor extends Condition
{
	private final boolean _value;
	
	/**
	 * Creates a condition matching the given expected state.
	 * @param value The required summon-allowed state.
	 */
	public ConditionPlayerCanSummonServitor(boolean value)
	{
		_value = value;
	}
	
	/**
	 * Checks whether the player may summon a servitor, requiring no mount, flight, observer mode or teleport and fewer than four servitors.
	 * @param effector The acting {@code Creature}.
	 * @param effected The target {@code Creature}.
	 * @param skill The {@code Skill} used.
	 * @param item The {@code ItemTemplate} involved.
	 * @return {@code true} if the condition is met, {@code false} otherwise.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		final Player player = effector.asPlayer();
		if (player == null)
		{
			return false;
		}
		
		boolean canSummon = true;
		if (player.isFlyingMounted() || player.isMounted() || player.getObserverMode().isActive() || player.isTeleporting())
		{
			canSummon = false;
		}
		else if (player.getServitors().size() >= 4)
		{
			canSummon = false;
		}
		
		return canSummon == _value;
	}
}
