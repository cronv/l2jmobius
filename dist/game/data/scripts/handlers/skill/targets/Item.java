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
 * This class handles skill targets that are specific items.<br>
 * It allows skills to interact with {@code WorldObject} instances that represent items.
 * @author Nik
 */
public class Item implements ITargetTypeHandler
{
	/**
	 * Returns the target type associated with this handler.<br>
	 * This method identifies that the current target is an {@code ITEM}.
	 * @return The {@code TargetType} enum value for items.
	 */
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.ITEM;
	}
	
	/**
	 * Determines the valid target for a skill based on the selected object.<br>
	 * It checks if the {@code selectedTarget} is an item.<br>
	 * Returns the object if it is an item, otherwise returns {@code null}.
	 * @param creature The {@code Creature} performing the action.
	 * @param selectedTarget The {@code WorldObject} currently targeted by the user.
	 * @param skill The {@code Skill} being executed.
	 * @param forceUse Whether to force the use of the target.
	 * @param dontMove Whether to prevent movement during the action.
	 * @param sendMessage Whether to send a confirmation message to the player.
	 * @return The {@code WorldObject} if it is an item, or {@code null} otherwise.
	 */
	@Override
	public WorldObject getTarget(Creature creature, WorldObject selectedTarget, Skill skill, boolean forceUse, boolean dontMove, boolean sendMessage)
	{
		return (selectedTarget != null) && selectedTarget.isItem() ? selectedTarget : null;
	}
}
