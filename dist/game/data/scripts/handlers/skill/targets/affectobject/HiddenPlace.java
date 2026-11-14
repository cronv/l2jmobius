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
 * Handles skill effects applied to hidden locations.<br>
 * This class allows skills to interact with specific {@code AffectObject} instances that represent concealed areas.
 * @author Nik
 */
public class HiddenPlace implements IAffectObjectHandler
{
	/**
	 * Checks if the {@code target} is a valid object for the current effect.<br>
	 * This method determines if an action can be performed on a specific creature.
	 * @param creature The creature performing the action.
	 * @param target The creature being targeted by the action.
	 * @return {@code true} if the target is valid, otherwise {@code false}.
	 */
	@Override
	public boolean checkAffectedObject(Creature creature, Creature target)
	{
		// TODO: What is this?
		return false;
	}
	
	/**
	 * Retrieves the type of {@code AffectObject} associated with this handler.<br>
	 * This method identifies which specific effect type is being processed.
	 * @return The {@code AffectObject} enum value for this hidden place.
	 */
	@Override
	public Enum<AffectObject> getAffectObjectType()
	{
		return AffectObject.HIDDEN_PLACE;
	}
}
