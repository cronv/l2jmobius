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
 * Handles the logic for skills that target a dead NPC body.<br>
 * This class ensures the effect is applied correctly to the corpse object.
 * @author Nik
 */
public class ObjectDeadNpcBody implements IAffectObjectHandler
{
	/**
	 * Checks if the {@code target} is a valid object for this effect.<br>
	 * It returns {@code false} if the {@code creature} and {@code target} are the same.<br>
	 * The {@code target} must be an NPC and it must be dead.
	 * @param creature The creature performing the action.
	 * @param target The creature being checked as a target.
	 * @return {@code true} if the target is a dead NPC, otherwise {@code false}.
	 */
	@Override
	public boolean checkAffectedObject(Creature creature, Creature target)
	{
		if (creature == target)
		{
			return false;
		}
		
		return target.isNpc() && target.isDead();
	}
	
	/**
	 * Retrieves the type of affect object associated with this handler.<br>
	 * This method identifies the specific {@code AffectObject} category.
	 * @return The {@code Enum<AffectObject>} representing the target type.
	 */
	@Override
	public Enum<AffectObject> getAffectObjectType()
	{
		return AffectObject.OBJECT_DEAD_NPC_BODY;
	}
}
