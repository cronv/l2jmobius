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
 * This class handles the logic for targeting a siege artefact.<br>
 * It allows skills to interact specifically with {@code WorldObject} instances that represent holy items.
 * @author Nik
 */
public class HolyThing implements ITargetTypeHandler
{
	/**
	 * Returns the specific {@code TargetType} for this handler.<br>
	 * This identifies the target category used by the skill.
	 * @return The {@code TargetType} enum value associated with this class.
	 */
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.HOLYTHING;
	}
	
	/**
	 * Determines the valid target for a specific skill.<br>
	 * It checks if the current target of the {@code creature} is an artefact.<br>
	 * If the target is invalid, it may send a system message to the player.
	 * @param creature The {@code Creature} attempting to use the skill.
	 * @param selectedTarget The {@code WorldObject} currently selected by the user.
	 * @param skill The {@code Skill} being executed.
	 * @param forceUse Whether to force the use of the skill.
	 * @param dontMove Whether to prevent the creature from moving.
	 * @param sendMessage Whether to send a message if the target is invalid.
	 * @return The valid {@code WorldObject} target, or {@code null} if no valid target exists.
	 */
	@Override
	public WorldObject getTarget(Creature creature, WorldObject selectedTarget, Skill skill, boolean forceUse, boolean dontMove, boolean sendMessage)
	{
		final WorldObject target = creature.getTarget();
		if ((target != null) && target.isArtefact())
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
