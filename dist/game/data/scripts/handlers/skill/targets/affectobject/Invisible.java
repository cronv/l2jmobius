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
 * This class handles the {@code Invisible} effect applied to an object.<br>
 * It manages how the invisibility status is processed for target objects.
 * @author Nik
 */
public class Invisible implements IAffectObjectHandler
{
	/**
	 * Checks if the target object is currently invisible.<br>
	 * This method validates the visibility state of a {@code Creature}.
	 * @param creature The creature performing the action.
	 * @param target The creature being checked for invisibility.
	 * @return {@code true} if the target is invisible, {@code false} otherwise.
	 */
	@Override
	public boolean checkAffectedObject(Creature creature, Creature target)
	{
		return target.isInvisible();
	}
	
	/**
	 * Retrieves the type of affect object associated with this handler.<br>
	 * This method returns the {@code AffectObject} enum value.
	 * @return The {@code Enum<AffectObject>} representing the specific effect type.
	 */
	@Override
	public Enum<AffectObject> getAffectObjectType()
	{
		return AffectObject.INVISIBLE;
	}
}
