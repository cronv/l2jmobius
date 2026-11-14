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
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Handles the {@code others} target type for skills.<br>
 * Currently, skills using this target type appear to be disabled.
 * @author Nik
 */
public class Others implements ITargetTypeHandler
{
	/**
	 * Retrieves the target type associated with this handler.<br>
	 * This method returns the {@code TargetType.OTHERS} value.
	 * @return The {@code TargetType} enum constant for other targets.
	 */
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.OTHERS;
	}
	
	/**
	 * Determines the valid target for a skill based on the provided parameters.<br>
	 * It prevents a {@code Creature} from targeting itself.<br>
	 * If the target is invalid, it sends a system message and returns {@code null}.
	 * @param creature The {@code Creature} attempting to use the skill.
	 * @param selectedTarget The {@code WorldObject} currently targeted by the user.
	 * @param skill The {@code Skill} being executed.
	 * @param forceUse Whether to bypass normal usage requirements.
	 * @param dontMove Whether to prevent the character from moving during the action.
	 * @param sendMessage Whether to send a confirmation message to the player.
	 * @return The validated {@code WorldObject} target, or {@code null} if invalid.
	 */
	@Override
	public WorldObject getTarget(Creature creature, WorldObject selectedTarget, Skill skill, boolean forceUse, boolean dontMove, boolean sendMessage)
	{
		if (selectedTarget == creature)
		{
			creature.sendPacket(SystemMessageId.YOU_CANNOT_USE_THIS_ON_YOURSELF);
			return null;
		}
		
		return selectedTarget;
	}
}
