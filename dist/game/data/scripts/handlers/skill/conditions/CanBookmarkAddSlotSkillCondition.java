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
package handlers.skill.conditions;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if a player is allowed to add a skill to their bookmark slots.<br>
 * This condition validates the availability of space within the {@code Player}'s bookmark system.
 * @author
 */
public class CanBookmarkAddSlotSkillCondition implements ISkillCondition
{
	private final int _teleportBookmarkSlots;
	
	/**
	 * Creates a new condition to check for bookmark slot availability.<br>
	 * It initializes the required slots from the provided configuration.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public CanBookmarkAddSlotSkillCondition(StatSet params)
	{
		_teleportBookmarkSlots = params.getInt("teleportBookmarkSlots");
	}
	
	/**
	 * Checks if the caster can use a skill to add a bookmark slot.<br>
	 * It verifies that the player has not reached the maximum limit of slots.
	 * @param caster The {@code Creature} attempting to use the skill.
	 * @param skill The {@code Skill} being cast.
	 * @param target The {@code WorldObject} targeted by the skill.
	 * @return {@code true} if the slot can be added, {@code false} otherwise.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		final Player player = caster.asPlayer();
		if (player == null)
		{
			return false;
		}
		
		if ((player.getBookMarkSlot() + _teleportBookmarkSlots) > 30)
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_REACHED_THE_MAXIMUM_NUMBER_OF_MY_TELEPORT_SLOTS_OR_USE_CONDITIONS_ARE_NOT_OBSERVED);
			return false;
		}
		
		return true;
	}
}
