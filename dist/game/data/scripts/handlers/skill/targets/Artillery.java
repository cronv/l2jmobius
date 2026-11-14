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
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.handler.ITargetTypeHandler;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * This class handles target selection for artillery skills.<br>
 * It allows players to target specific structures such as siege doors and walls.
 * @author Nik
 */
public class Artillery implements ITargetTypeHandler
{
	/**
	 * Retrieves the target type associated with this handler.<br>
	 * This method identifies which {@code TargetType} is handled by this class.
	 * @return The {@code TargetType} enum value for artillery.
	 */
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.ARTILLERY;
	}
	
	/**
	 * Determines the valid target for a specific skill.<br>
	 * This method checks if the current target is an enemy door that can be attacked.<br>
	 * It returns {@code null} if the target is invalid.
	 * @param creature The {@code Creature} performing the action.
	 * @param selectedTarget The {@code WorldObject} currently targeted by the user.
	 * @param skill The {@code Skill} being used.
	 * @param forceUse Whether to force the use of the skill.
	 * @param dontMove Whether to prevent the creature from moving.
	 * @param sendMessage Whether to send a system message if the target is invalid.
	 * @return The valid {@code WorldObject} target or {@code null}.
	 */
	@Override
	public WorldObject getTarget(Creature creature, WorldObject selectedTarget, Skill skill, boolean forceUse, boolean dontMove, boolean sendMessage)
	{
		final WorldObject target = creature.getTarget();
		if ((target != null) && target.isDoor())
		{
			final Door targetDoor = target.asDoor();
			if (!targetDoor.isDead() && targetDoor.isAutoAttackable(creature) && targetDoor.isEnemy())
			{
				return targetDoor;
			}
		}
		
		if (sendMessage)
		{
			creature.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
		}
		
		return null;
	}
}
