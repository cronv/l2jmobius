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
 * This class handles the {@code AffectObject} logic for undead enemy NPCs.<br>
 * It defines how these specific entities interact with target effects.
 * @author Nik
 */
public class UndeadRealEnemy implements IAffectObjectHandler
{
	/**
	 * Checks if the {@code target} is a valid object for this effect.<br>
	 * It ensures that the {@code creature} is not attacking itself.<br>
	 * The {@code target} must be an undead creature and attackable by the {@code creature}.
	 * @param creature The creature performing the action.
	 * @param target The creature being checked as a potential target.
	 * @return {@code true} if the target is valid, otherwise {@code false}.
	 */
	@Override
	public boolean checkAffectedObject(Creature creature, Creature target)
	{
		// You are not an enemy of yourself.
		if (creature == target)
		{
			return false;
		}
		
		return target.isUndead() && target.isAutoAttackable(creature);
	}
	
	/**
	 * Returns the type of affect object associated with this handler.<br>
	 * This identifies which specific {@code AffectObject} category is being used.
	 * @return The {@code Enum<AffectObject>} representing the target type.
	 */
	@Override
	public Enum<AffectObject> getAffectObjectType()
	{
		return AffectObject.UNDEAD_REAL_ENEMY;
	}
}
