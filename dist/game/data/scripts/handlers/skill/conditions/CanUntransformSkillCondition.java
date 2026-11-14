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
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if a player is currently in a transformed state.<br>
 * This condition ensures that the skill can only be executed when the {@code Player} is not transformed.<br>
 * It returns {@code true} if the player is in their original form.
 * @author Sdw
 */
public class CanUntransformSkillCondition implements ISkillCondition
{
	/**
	 * Creates a new {@code CanUntransformSkillCondition} instance.<br>
	 * It uses the provided {@code StatSet} to define the requirements.
	 * @param params The set of statistics used for this condition.
	 */
	public CanUntransformSkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks if the {@code caster} is allowed to use a specific {@code skill}.<br>
	 * It verifies that the player is alive and not restricted by certain conditions.<br>
	 * The method also checks for altitude restrictions when flying.
	 * @param caster The {@code Creature} attempting to use the skill.
	 * @param skill The {@code Skill} being cast.
	 * @param target The {@code WorldObject} that is the target of the skill.
	 * @return {@code true} if the skill can be used, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		boolean canUntransform = true;
		final Player player = caster.asPlayer();
		if (player == null)
		{
			canUntransform = false;
		}
		else if (player.isAlikeDead() || player.isCursedWeaponEquipped())
		{
			canUntransform = false;
		}
		else if (player.isFlyingMounted() && !player.isInsideZone(ZoneId.LANDING))
		{
			player.sendPacket(SystemMessageId.YOU_ARE_TOO_HIGH_TO_PERFORM_THIS_ACTION_PLEASE_LOWER_YOUR_ALTITUDE_AND_TRY_AGAIN); // TODO: check if message is retail like.
			canUntransform = false;
		}
		
		return canUntransform;
	}
}
