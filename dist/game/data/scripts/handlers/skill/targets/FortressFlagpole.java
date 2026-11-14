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
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.handler.ITargetTypeHandler;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Handles the logic for targeting a fortress flagpole.<br>
 * This class allows skills to interact with specific flagpole objects in the game world.
 * @author Nik
 */
public class FortressFlagpole implements ITargetTypeHandler
{
	/**
	 * Retrieves the specific {@code TargetType} for this handler.<br>
	 * This identifies the target as a fortress flagpole.
	 * @return The {@code TargetType} associated with this class.
	 */
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.FORTRESS_FLAGPOLE;
	}
	
	/**
	 * Determines the valid target for a specific skill action.<br>
	 * This method checks if the selected object is a flagpole within a fortress zone.<br>
	 * It returns {@code null} if the target is invalid.
	 * @param creature The {@code Creature} performing the action.
	 * @param selectedTarget The {@code WorldObject} currently being targeted by the player.
	 * @param skill The {@code Skill} being used to determine targeting logic.
	 * @param forceUse Whether to ignore certain restrictions when using the skill.
	 * @param dontMove Whether the creature should remain stationary during the action.
	 * @param sendMessage Whether to notify the player if the target is invalid.
	 * @return The validated {@code WorldObject} or {@code null} if no valid target exists.
	 */
	@Override
	public WorldObject getTarget(Creature creature, WorldObject selectedTarget, Skill skill, boolean forceUse, boolean dontMove, boolean sendMessage)
	{
		final WorldObject target = creature.getTarget();
		if ((target != null) && creature.isInsideZone(ZoneId.HQ) && creature.isInsideZone(ZoneId.FORT) && !target.isPlayable() && target.getName().toLowerCase().contains("flagpole"))
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
