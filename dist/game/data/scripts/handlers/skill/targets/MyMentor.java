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
import org.l2jmobius.gameserver.entity.actor.holders.player.Mentee;
import org.l2jmobius.gameserver.handler.ITargetTypeHandler;
import org.l2jmobius.gameserver.managers.MentorManager;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;

/**
 * This class handles the logic for targeting a player's mentor.<br>
 * It allows skills to be cast specifically on the {@code Mentee}'s assigned mentor.
 * @author Nik
 */
public class MyMentor implements ITargetTypeHandler
{
	/**
	 * Retrieves the target type associated with this handler.<br>
	 * This method returns the {@code TargetType.MY_MENTOR} value.
	 * @return The {@code TargetType} enum constant for this handler.
	 */
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.MY_MENTOR;
	}
	
	/**
	 * Determines the target object for a skill based on the player's mentor.<br>
	 * It checks if the {@code creature} is a player and retrieves their mentor.<br>
	 * If no mentor exists, it returns {@code null}.
	 * @param creature The {@code Creature} attempting to use the skill.
	 * @param selectedTarget The {@code WorldObject} currently selected by the user.
	 * @param skill The {@code Skill} being executed.
	 * @param forceUse Whether to bypass normal usage requirements.
	 * @param dontMove Whether to prevent the character from moving during the action.
	 * @param sendMessage Whether to send a message to the player.
	 * @return The {@code WorldObject} of the mentor, or {@code null} if none is found.
	 */
	@Override
	public WorldObject getTarget(Creature creature, WorldObject selectedTarget, Skill skill, boolean forceUse, boolean dontMove, boolean sendMessage)
	{
		if (creature.isPlayer())
		{
			final Mentee mentor = MentorManager.getInstance().getMentor(creature.getObjectId());
			if (mentor != null)
			{
				return mentor.getPlayer();
			}
		}
		
		return null;
	}
}
