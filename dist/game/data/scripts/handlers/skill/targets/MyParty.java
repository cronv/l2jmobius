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
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.handler.ITargetTypeHandler;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;

/**
 * This class handles skill targets for the entire party.<br>
 * It functions similarly to a self-target but applies effects to all members of the {@code Party}.<br>
 * It is primarily used for aura-type skills.
 * @author Nik
 */
public class MyParty implements ITargetTypeHandler
{
	/**
	 * Retrieves the target type associated with this handler.<br>
	 * This method returns the {@code TargetType.MY_PARTY} value.
	 * @return The {@code TargetType} enum value for this handler.
	 */
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.MY_PARTY;
	}
	
	/**
	 * Determines the valid {@code WorldObject} for a skill based on party membership.<br>
	 * It checks if the {@code selectedTarget} belongs to the same party as the {@code creature}.<br>
	 * If they are in the same party, it returns the {@code selectedTarget}.<br>
	 * Otherwise, it defaults to returning the {@code creature}.
	 * @param creature The {@code Creature} performing the action.
	 * @param selectedTarget The {@code WorldObject} currently targeted by the user.
	 * @param skill The {@code Skill} being executed.
	 * @param forceUse Whether to force the use of the skill.
	 * @param dontMove Whether to prevent the creature from moving.
	 * @param sendMessage Whether to send a message to the player.
	 * @return The validated {@code WorldObject} target.
	 */
	@Override
	public WorldObject getTarget(Creature creature, WorldObject selectedTarget, Skill skill, boolean forceUse, boolean dontMove, boolean sendMessage)
	{
		if ((selectedTarget != null) && selectedTarget.isPlayer() && (selectedTarget != creature))
		{
			final Party party = creature.getParty();
			final Party targetParty = selectedTarget.asPlayer().getParty();
			if ((party != null) && (targetParty != null) && (party.getLeaderObjectId() == targetParty.getLeaderObjectId()))
			{
				return selectedTarget;
			}
		}
		
		return creature;
	}
}
