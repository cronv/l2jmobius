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
package handlers.skill.conditions;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.ControllableAirShip;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the player is allowed to refuel an airship.<br>
 * This condition validates the requirements for using a refueling skill on a {@code ControllableAirShip}.
 * @author Sdw
 */
public class CanRefuelAirshipSkillCondition implements ISkillCondition
{
	private final int _amount;
	
	/**
	 * Creates a new condition to check if an airship can be refueled.<br>
	 * It initializes the required amount from the provided stats.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public CanRefuelAirshipSkillCondition(StatSet params)
	{
		_amount = params.getInt("amount");
	}
	
	/**
	 * Checks if the {@code caster} is allowed to use the specified {@code skill} on a {@code target}.<br>
	 * This method verifies that the player owns a {@code ControllableAirShip}.<br>
	 * It also ensures that adding the fuel amount will not exceed the ship's maximum capacity.
	 * @param caster The creature attempting to use the skill.
	 * @param skill The skill being cast.
	 * @param target The object being targeted by the skill.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		boolean canRefuelAirship = true;
		final Player player = caster.asPlayer();
		if ((player == null) || (player.getAirShip() == null) || !(player.getAirShip() instanceof ControllableAirShip) || ((player.getAirShip().getFuel() + _amount) > player.getAirShip().getMaxFuel()))
		{
			canRefuelAirship = false;
		}
		
		return canRefuelAirship;
	}
}
