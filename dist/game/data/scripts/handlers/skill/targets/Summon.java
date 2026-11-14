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

/**
 * This handler identifies and targets one of the player's active summons.<br>
 * It is used to automate target selection for skills that require a summon as a valid target.
 * @author Nik
 */
public class Summon implements ITargetTypeHandler
{
	/**
	 * Retrieves the target type associated with this handler.<br>
	 * This method identifies which {@code TargetType} is used for summons.
	 * @return The {@code TargetType} enum value representing a summon.
	 */
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.SUMMON;
	}
	
	/**
	 * Determines the target object for a skill based on the creature's summons.<br>
	 * It prioritizes player servitors if the creature is a player.<br>
	 * Otherwise, it returns the creature's pet.
	 * @param creature The {@code Creature} performing the action.
	 * @param selectedTarget The {@code WorldObject} currently selected by the user.
	 * @param skill The {@code Skill} being executed.
	 * @param forceUse Whether to force the use of the skill.
	 * @param dontMove Whether to prevent movement during the action.
	 * @param sendMessage Whether to send a message to the player.
	 * @return The resulting {@code WorldObject} target or {@code null}.
	 */
	@Override
	public WorldObject getTarget(Creature creature, WorldObject selectedTarget, Skill skill, boolean forceUse, boolean dontMove, boolean sendMessage)
	{
		if (creature.isPlayer() && creature.hasSummon())
		{
			return creature.asPlayer().getAnyServitor();
		}
		
		return creature.getPet();
	}
}
