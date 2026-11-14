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

import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.xml.CategoryData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.handler.IAffectObjectHandler;
import org.l2jmobius.gameserver.mechanics.skill.targets.AffectObject;

/**
 * Handles the specific logic for effects applied to {@code Wyvern} objects.<br>
 * This class manages how skills interact with these entities within the game world.
 * @author Nik
 */
public class WyvernObject implements IAffectObjectHandler
{
	/**
	 * Checks if the {@code target} belongs to a specific category.<br>
	 * This method verifies if the object is part of the {@code WYVERN_GROUP}.
	 * @param creature The {@code Creature} performing the action.
	 * @param target The {@code Creature} being checked.
	 * @return {@code true} if the target is in the correct category, {@code false} otherwise.
	 */
	@Override
	public boolean checkAffectedObject(Creature creature, Creature target)
	{
		// TODO Check if this is proper. Not sure if this is the object we are looking for.
		return CategoryData.getInstance().isInCategory(CategoryType.WYVERN_GROUP, target.getId());
	}
	
	/**
	 * Retrieves the specific type of {@code AffectObject} for this handler.<br>
	 * This identifies which object type is being affected by the skill.
	 * @return The {@code Enum<AffectObject>} representing the wyvern object type.
	 */
	@Override
	public Enum<AffectObject> getAffectObjectType()
	{
		return AffectObject.WYVERN_OBJECT;
	}
}
