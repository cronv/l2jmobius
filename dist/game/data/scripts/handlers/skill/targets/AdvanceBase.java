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
 * Handles the targeting logic for the Outpost NPC.<br>
 * This class specifically targets the NPC with ID {@code 36590}.
 * @author Nik
 */
public class AdvanceBase implements ITargetTypeHandler
{
	/**
	 * Retrieves the specific {@code TargetType} associated with this handler.<br>
	 * This method identifies which target type this class is responsible for handling.
	 * @return The {@code TargetType} enum value for this handler.
	 */
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.ADVANCE_BASE;
	}
	
	/**
	 * Determines the valid target for a specific skill action.<br>
	 * It checks if the current target is the correct NPC and is alive.<br>
	 * Returns {@code null} if the target is invalid.
	 * @param creature The {@code Creature} performing the action.
	 * @param selectedTarget The {@code WorldObject} currently targeted by the player.
	 * @param skill The {@code Skill} being used.
	 * @param forceUse Whether to ignore certain restrictions when using the skill.
	 * @param dontMove Whether to prevent the creature from moving during the action.
	 * @param sendMessage Whether to send a system message if the target is invalid.
	 * @return The validated {@code WorldObject} or {@code null}.
	 */
	@Override
	public WorldObject getTarget(Creature creature, WorldObject selectedTarget, Skill skill, boolean forceUse, boolean dontMove, boolean sendMessage)
	{
		final WorldObject target = creature.getTarget();
		if ((target != null) && target.isNpc() && (target.getId() == 36590) && !target.asNpc().isDead())
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
