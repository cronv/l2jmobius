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
package handlers.conditions;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.conditions.ICondition;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if a player meets a specific level requirement.<br>
 * This {@code PlayerLevelCondition} is used to validate the character's current level against a target value.
 * @author Sdw
 */
public class PlayerLevelCondition implements ICondition
{
	private final int _minLevel;
	private final int _maxLevel;
	
	/**
	 * Creates a new {@code PlayerLevelCondition} using the provided statistics.<br>
	 * This constructor extracts level requirements from a {@code StatSet}.
	 * @param params The {@code StatSet} containing the required min and max levels.
	 */
	public PlayerLevelCondition(StatSet params)
	{
		_minLevel = params.getInt("minLevel");
		_maxLevel = params.getInt("maxLevel");
	}
	
	/**
	 * Checks if a {@code Creature} meets the required level range.<br>
	 * This method verifies that the entity is a player.<br>
	 * It also checks if the level is between {@code _minLevel} and {@code _maxLevel}.
	 * @param creature The {@code Creature} to check.
	 * @param object The {@code WorldObject} associated with the action.
	 * @return {@code true} if the player's level is within the valid range, otherwise {@code false}.
	 */
	@Override
	public boolean test(Creature creature, WorldObject object)
	{
		final boolean isPlayer = creature.isPlayer();
		if (isPlayer)
		{
			return (creature.getLevel() >= _minLevel) && (creature.getLevel() < _maxLevel);
		}
		
		return false;
	}
}
