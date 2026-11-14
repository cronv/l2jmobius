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

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.zone.ZoneRegion;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.handler.ITargetTypeHandler;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Handles targets based on a specific ground location.<br>
 * It returns {@code null} or the player if the current skill's ground location meets the required conditions.
 * @author Nik
 */
public class Ground implements ITargetTypeHandler
{
	/**
	 * Retrieves the target type associated with this handler.<br>
	 * This method returns the {@code TargetType.GROUND} value.
	 * @return The {@code TargetType} enum constant for ground targets.
	 */
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.GROUND;
	}
	
	/**
	 * Determines the valid target for a ground-based skill.<br>
	 * This method checks if the player's current skill world position is valid based on distance, visibility, and zone restrictions.<br>
	 * It returns the {@code Creature} if the location is valid or {@code null} otherwise.
	 * @param creature The {@code Creature} attempting to use the skill.
	 * @param selectedTarget The {@code WorldObject} currently selected by the user.
	 * @param skill The {@code Skill} being cast.
	 * @param forceUse Whether to ignore certain restrictions.
	 * @param dontMove If {@code true}, returns {@code null} if the player is too far from the target position.
	 * @param sendMessage Whether to send a system message to the player on failure.
	 * @return The {@code Creature} if the ground location is valid, or {@code null}.
	 */
	@Override
	public WorldObject getTarget(Creature creature, WorldObject selectedTarget, Skill skill, boolean forceUse, boolean dontMove, boolean sendMessage)
	{
		if (creature.isPlayer())
		{
			final Location worldPosition = creature.asPlayer().getCurrentSkillWorldPosition();
			if (worldPosition != null)
			{
				final int x = worldPosition.getX();
				final int y = worldPosition.getY();
				final int z = worldPosition.getZ();
				if (dontMove && !creature.isInsideRadius2D(x, y, z, creature.getMagicalAttackRange(skill) + creature.getTemplate().getCollisionRadius()))
				{
					return null;
				}
				
				if (!GeoEngine.getInstance().canSeeTarget(creature, worldPosition) || (skill.isFlyType() && !GeoEngine.getInstance().canMoveToTarget(creature, worldPosition)))
				{
					if (sendMessage)
					{
						creature.sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
					}
					
					return null;
				}
				
				final ZoneRegion zoneRegion = ZoneManager.getInstance().getRegion(creature);
				if (skill.hasNegativeEffect() && !creature.isInInstance() && !zoneRegion.checkEffectRangeInsidePeaceZone(skill, x, y, z))
				{
					if (sendMessage)
					{
						creature.sendPacket(SystemMessageId.YOU_CANNOT_USE_SKILLS_THAT_MAY_HARM_OTHER_PLAYERS_IN_THIS_AREA);
					}
					
					return null;
				}
				
				return creature; // Return yourself to know that your ground location is legit.
			}
		}
		
		return null;
	}
}
