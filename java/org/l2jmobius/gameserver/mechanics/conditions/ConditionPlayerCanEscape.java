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
 * This class implements the {@code ConditionPlayerCanEscape} logic.<br>
 * It checks whether a player is currently able to escape from their current situation.
 * @author Adry_85
 */
public class ConditionPlayerCanEscape extends Condition
{
	private final boolean _value;
	
	/**
	 * Creates a new {@code ConditionPlayerCanEscape} instance.<br>
	 * This sets the escape status for the condition.
	 * @param value The boolean state to determine if the player can escape.
	 */
	public ConditionPlayerCanEscape(boolean value)
	{
		_value = value;
	}
	
	/**
	 * Checks if the {@code effector} is allowed to escape based on specific game rules.<br>
	 * This method verifies conditions like duel status, flight, and event restrictions.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} receiving the action.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code ItemTemplate} involved in the action.
	 * @return {@code true} if all escape conditions are met, {@code false} otherwise.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		boolean canTeleport = true;
		final Player player = effector.asPlayer();
		if (player == null)
		{
			canTeleport = false;
		}
		else if (player.isInDuel())
		{
			canTeleport = false;
		}
		else if (player.isControlBlocked())
		{
			canTeleport = false;
		}
		else if (player.isCombatFlagEquipped())
		{
			canTeleport = false;
		}
		else if (player.isFlying() || player.isFlyingMounted())
		{
			canTeleport = false;
		}
		else if (player.isInOlympiadMode())
		{
			canTeleport = false;
		}
		else if (player.isOnEvent())
		{
			canTeleport = false;
		}
		
		return _value == canTeleport;
	}
}
