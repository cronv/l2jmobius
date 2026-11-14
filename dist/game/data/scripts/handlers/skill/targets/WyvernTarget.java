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
 * Handles target selection logic for skills used while riding a wyvern.<br>
 * This class ensures that the {@code TargetType} is correctly processed during wyvern mounts.
 * @author Nik
 */
public class WyvernTarget implements ITargetTypeHandler
{
	/**
	 * Retrieves the specific target type for this handler.<br>
	 * This method identifies which {@code TargetType} is associated with wyvern targeting.
	 * @return The {@code TargetType} enum value representing the wyvern target.
	 */
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.WYVERN_TARGET;
	}
	
	/**
	 * Determines the target for a specific skill action.<br>
	 * This method evaluates the interaction between a {@code Creature} and a {@code WorldObject}.<br>
	 * It considers various flags to decide if the action should be forced or moved.
	 * @param creature The actor performing the action.
	 * @param selectedTarget The object currently targeted by the user.
	 * @param skill The {@code Skill} being executed.
	 * @param forceUse Whether to bypass normal requirements for using the skill.
	 * @param dontMove Whether to prevent the actor from moving during the action.
	 * @param sendMessage Whether to broadcast a message to other players.
	 * @return The resulting {@code WorldObject} target or {@code null}.
	 */
	@Override
	public WorldObject getTarget(Creature creature, WorldObject selectedTarget, Skill skill, boolean forceUse, boolean dontMove, boolean sendMessage)
	{
		return null;
	}
}
