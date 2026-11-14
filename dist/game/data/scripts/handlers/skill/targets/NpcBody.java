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
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.handler.ITargetTypeHandler;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * This handler is used to target a dead monster.<br>
 * It identifies and processes {@code Creature} entities that are currently in a dead state.
 * @author Nik
 */
public class NpcBody implements ITargetTypeHandler
{
	/**
	 * Retrieves the target type associated with this handler.<br>
	 * This method returns the {@code TargetType} constant for NPC bodies.
	 * @return The {@code TargetType} enum value.
	 */
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.NPC_BODY;
	}
	
	/**
	 * Determines the valid target for a specific skill based on the selected object.<br>
	 * This method checks if the target is a dead NPC or summon and validates distance requirements.
	 * @param creature The {@code Creature} attempting to use the skill.
	 * @param selectedTarget The {@code WorldObject} currently targeted by the user.
	 * @param skill The {@code Skill} being cast.
	 * @param forceUse Whether to force the use of the skill.
	 * @param dontMove If {@code true}, prevents the character from moving toward the target.
	 * @param sendMessage If {@code true}, sends system messages for invalid targets or distance issues.
	 * @return The valid {@code WorldObject} if successful, otherwise {@code null}.
	 */
	@Override
	public WorldObject getTarget(Creature creature, WorldObject selectedTarget, Skill skill, boolean forceUse, boolean dontMove, boolean sendMessage)
	{
		if (selectedTarget == null)
		{
			return null;
		}
		
		if (!selectedTarget.isCreature())
		{
			return null;
		}
		
		if (!selectedTarget.isNpc() && !selectedTarget.isSummon())
		{
			if (sendMessage)
			{
				creature.sendPacket(SystemMessageId.INVALID_TARGET);
			}
			
			return null;
		}
		
		final Creature target = selectedTarget.asCreature();
		if (target.isDead())
		{
			// Check for cast range if character cannot move. TODO: char will start follow until within castrange, but if his moving is blocked by geodata, this msg will be sent.
			if (dontMove)
			{
				if (creature.calculateDistance2D(target) > creature.getMagicalAttackRange(skill))
				{
					if (sendMessage)
					{
						creature.sendPacket(SystemMessageId.THE_DISTANCE_IS_TOO_FAR_AND_SO_THE_CASTING_HAS_BEEN_CANCELLED);
					}
					
					return null;
				}
			}
			
			// Geodata check when character is within range.
			if (!GeoEngine.getInstance().canSeeTarget(creature, target) || (skill.isFlyType() && !GeoEngine.getInstance().canMoveToTarget(creature, target)))
			{
				if (sendMessage)
				{
					creature.sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
				}
				
				return null;
			}
			
			return target;
		}
		
		// If target is not dead or not player/pet it will not even bother to walk within range, unlike Enemy target type.
		if (sendMessage)
		{
			creature.sendPacket(SystemMessageId.INVALID_TARGET);
		}
		
		return null;
	}
}
