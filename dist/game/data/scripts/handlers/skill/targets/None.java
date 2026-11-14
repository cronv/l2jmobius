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
package handlers.skill.targets;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.handler.ITargetTypeHandler;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;

/**
 * This class handles skills that do not require a specific target.<br>
 * It is used when the {@code TargetType} is set to {@code NONE}.
 * @author Sdw
 */
public class None implements ITargetTypeHandler
{
	/**
	 * Determines the target object for a specific skill.<br>
	 * This method returns the {@code Creature} that initiated the action.
	 * @param creature The {@code Creature} performing the action.
	 * @param selectedTarget The {@code WorldObject} currently selected by the user.
	 * @param skill The {@code Skill} being executed.
	 * @param forceUse Whether to bypass normal usage requirements.
	 * @param dontMove Whether to prevent the character from moving during the action.
	 * @param sendMessage Whether to send a confirmation message to the user.
	 * @return The {@code WorldObject} representing the target.
	 */
	@Override
	public WorldObject getTarget(Creature creature, WorldObject selectedTarget, Skill skill, boolean forceUse, boolean dontMove, boolean sendMessage)
	{
		return creature;
	}
	
	/**
	 * Retrieves the target type associated with this handler.<br>
	 * This method returns a constant value from the {@code TargetType} enum.
	 * @return The {@code TargetType} for this specific handler.
	 */
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.NONE;
	}
}
