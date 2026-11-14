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
import org.l2jmobius.gameserver.entity.actor.instance.ControllableAirShip;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This class checks if a {@code Player} is allowed to refuel an airship.<br>
 * It validates the requirements for performing the refueling action.
 * @author Adry_85
 */
public class ConditionPlayerCanRefuelAirship extends Condition
{
	private final int _value;
	
	/**
	 * Creates a new condition to check if a player can refuel an airship.<br>
	 * This constructor sets the required threshold for the refueling action.
	 * @param value The numeric value used to validate the refueling condition.
	 */
	public ConditionPlayerCanRefuelAirship(int value)
	{
		_value = value;
	}
	
	/**
	 * Checks if a player can refuel their airship.<br>
	 * This method verifies the player owns a {@code ControllableAirShip}.<br>
	 * It also ensures that adding fuel will not exceed the maximum capacity.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code ItemTemplate} involved in the action.
	 * @return {@code true} if the refueling is valid, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		boolean canRefuelAirship = true;
		final Player player = effector.asPlayer();
		if ((player == null) || (player.getAirShip() == null) || !(player.getAirShip() instanceof ControllableAirShip) || ((player.getAirShip().getFuel() + _value) > player.getAirShip().getMaxFuel()))
		{
			canRefuelAirship = false;
		}
		
		return canRefuelAirship;
	}
}
