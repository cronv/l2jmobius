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
 * Checks if an NPC meets a specific level requirement.<br>
 * This {@code NpcLevelCondition} evaluates the level of a {@code Creature} against a target value.
 * @author Sdw
 */
public class NpcLevelCondition implements ICondition
{
	private final int _minLevel;
	private final int _maxLevel;
	
	/**
	 * Creates a new {@code NpcLevelCondition} using the provided statistics.<br>
	 * This constructor extracts level requirements from a {@code StatSet}.
	 * @param params The {@code StatSet} containing {@code minLevel} and {@code maxLevel} values.
	 */
	public NpcLevelCondition(StatSet params)
	{
		_minLevel = params.getInt("minLevel");
		_maxLevel = params.getInt("maxLevel");
	}
	
	/**
	 * Checks if a {@code WorldObject} is an NPC within a specific level range.<br>
	 * This method verifies the level of the creature associated with the object.
	 * @param creature The {@code Creature} performing the check.
	 * @param object The {@code WorldObject} to be validated.
	 * @return {@code true} if the object is an NPC and its level is between {@code _minLevel} and {@code _maxLevel}, otherwise {@code false}.
	 */
	@Override
	public boolean test(Creature creature, WorldObject object)
	{
		if (object.isNpc())
		{
			return (object.asCreature().getLevel() >= _minLevel) && (object.asCreature().getLevel() < _maxLevel);
		}
		
		return false;
	}
}
