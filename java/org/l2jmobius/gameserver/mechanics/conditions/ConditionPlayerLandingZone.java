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
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * Checks if a player is currently located within a specific landing zone.<br>
 * This condition is used to verify the spatial position of an actor relative to a {@code ZoneId}.
 * @author Kerberos
 */
public class ConditionPlayerLandingZone extends Condition
{
	private final boolean _value;
	
	/**
	 * Creates a new instance of {@code ConditionPlayerLandingZone}.<br>
	 * This constructor sets the internal state for the landing zone condition.
	 * @param value The boolean value to assign to this condition.
	 */
	public ConditionPlayerLandingZone(boolean value)
	{
		_value = value;
	}
	
	/**
	 * Checks if the {@code effector} is in the landing zone.<br>
	 * The result depends on the internal value of this condition.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item involved in the action.
	 * @return {@code true} if the condition is met, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		return effector.isInsideZone(ZoneId.LANDING) == _value;
	}
}
