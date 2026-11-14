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
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.handler.ITargetTypeHandler;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.siege.Siege;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * This class handles targets for skills that affect a dead {@code Playable} entity.<br>
 * It specifically allows targeting the bodies of deceased players or pets.
 * @author Nik
 */
public class PcBody implements ITargetTypeHandler
{
	/**
	 * Retrieves the target type associated with this handler.<br>
	 * This method returns the {@code TargetType.PC_BODY} constant.
	 * @return The {@code TargetType} enum value for this specific handler.
	 */
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.PC_BODY;
	}
	
	/**
	 * Validates and retrieves a target for a specific skill.<br>
	 * This method checks if the selected target is a dead player or pet.<br>
	 * It also verifies resurrection requirements, distance, and line of sight.
	 * @param creature The {@code Creature} attempting to use the skill.
	 * @param selectedTarget The {@code WorldObject} currently targeted by the user.
	 * @param skill The {@code Skill} being cast.
	 * @param forceUse Whether to force the use of the skill.
	 * @param dontMove Whether the creature is prevented from moving toward the target.
	 * @param sendMessage Whether to send feedback messages to the player.
	 * @return The validated {@code WorldObject} as a target, or {@code null} if invalid.
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
		
		if (!selectedTarget.isPlayer() && !selectedTarget.isPet())
		{
			if (sendMessage)
			{
				creature.sendPacket(SystemMessageId.INVALID_TARGET);
			}
			
			return null;
		}
		
		final Playable target = selectedTarget.asPlayable();
		if (target.isDead())
		{
			if (skill.hasEffectType(EffectType.RESURRECTION))
			{
				if (creature.isResurrectionBlocked() || target.isResurrectionBlocked())
				{
					if (sendMessage)
					{
						creature.sendPacket(SystemMessageId.REJECT_RESURRECTION); // Reject resurrection
						target.sendPacket(SystemMessageId.REJECT_RESURRECTION); // Reject resurrection
					}
					
					return null;
				}
				
				// Check the siege rules when the target lies on an active siege battlefield.
				final Siege siege = target.isPlayer() && (skill.getId() != 2393) ? SiegeManager.getInstance().getSiege(target) : null; // Blessed Scroll of Battlefield Resurrection
				final SystemMessageId refusal = (siege != null) && siege.isInProgress() ? siege.getResurrectionRefusal(target.asPlayer()) : null;
				if (refusal != null)
				{
					if (sendMessage)
					{
						creature.sendPacket(refusal);
					}
					
					return null;
				}
			}
			
			// Check for cast range if character cannot move. TODO: char will start follow until within castrange, but if his moving is blocked by geodata, this msg will be sent.
			if (dontMove && (creature.calculateDistance2D(target) > creature.getMagicalAttackRange(skill)))
			{
				if (sendMessage)
				{
					creature.sendPacket(SystemMessageId.THE_DISTANCE_IS_TOO_FAR_AND_SO_THE_CASTING_HAS_BEEN_CANCELLED);
				}
				
				return null;
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
