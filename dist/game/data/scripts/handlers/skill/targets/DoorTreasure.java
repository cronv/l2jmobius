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
import org.l2jmobius.gameserver.entity.actor.instance.Chest;
import org.l2jmobius.gameserver.handler.ITargetTypeHandler;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Handles skill targets for doors and treasure chests.<br>
 * This class allows skills to interact with {@code WorldObject} types like {@code Chest}.
 * @author UnAfraid
 */
public class DoorTreasure implements ITargetTypeHandler
{
	/**
	 * Retrieves the target type associated with this handler.<br>
	 * This method returns the {@code TargetType.DOOR_TREASURE} value.
	 * @return The {@code TargetType} enum constant for doors and treasure chests.
	 */
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.DOOR_TREASURE;
	}
	
	/**
	 * Determines if the current target is a valid door or chest.<br>
	 * It checks the {@code creature}'s current target object.<br>
	 * If the target is invalid, it may send an error message to the player.
	 * @param creature The {@code Creature} performing the action.
	 * @param selectedTarget The {@code WorldObject} currently being interacted with.
	 * @param skill The {@code Skill} being used.
	 * @param forceUse Whether to force the use of the target.
	 * @param dontMove Whether to prevent the creature from moving.
	 * @param sendMessage Whether to send a system message if the target is invalid.
	 * @return The valid {@code WorldObject} if it is a door or chest, otherwise {@code null}.
	 */
	@Override
	public WorldObject getTarget(Creature creature, WorldObject selectedTarget, Skill skill, boolean forceUse, boolean dontMove, boolean sendMessage)
	{
		final WorldObject target = creature.getTarget();
		if ((target != null) && (target.isDoor() || (target instanceof Chest)))
		{
			return target;
		}
		
		if (sendMessage)
		{
			creature.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
		}
		
		return null;
	}
}
