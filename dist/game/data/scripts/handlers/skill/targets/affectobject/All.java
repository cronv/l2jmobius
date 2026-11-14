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
package handlers.skill.targets.affectobject;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.handler.IAffectObjectHandler;
import org.l2jmobius.gameserver.mechanics.skill.targets.AffectObject;

/**
 * This handler applies an effect to all valid targets within the specified range.<br>
 * It is used when a skill needs to affect every {@code Creature} in its vicinity.
 * @author Nik
 */
public class All implements IAffectObjectHandler
{
	/**
	 * Checks if the {@code target} is a valid object for the skill effect.<br>
	 * This method currently returns {@code true} for all inputs.
	 * @param creature The {@code Creature} casting the skill.
	 * @param target The {@code Creature} being targeted by the skill.
	 * @return {@code true} if the target is valid, otherwise {@code false}.
	 */
	@Override
	public boolean checkAffectedObject(Creature creature, Creature target)
	{
		return true;
	}
	
	/**
	 * Retrieves the type of affected object for this handler.<br>
	 * This method returns a constant representing all possible types.
	 * @return The {@code Enum<AffectObject>} value returned by this method.
	 */
	@Override
	public Enum<AffectObject> getAffectObjectType()
	{
		return AffectObject.ALL;
	}
}
