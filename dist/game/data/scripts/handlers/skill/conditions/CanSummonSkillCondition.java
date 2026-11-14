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
 * Checks if the player meets the requirements to use a summoning skill.<br>
 * This condition validates whether the {@code Skill} can be successfully cast based on specific summon rules.
 * @author Sdw
 */
public class CanSummonSkillCondition implements ISkillCondition
{
	/**
	 * Creates a new instance of {@code CanSummonSkillCondition}.<br>
	 * It initializes the condition using the provided statistics.
	 * @param params The {@code StatSet} containing the required values.
	 */
	public CanSummonSkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks if the {@code caster} is allowed to use a specific {@code skill}.<br>
	 * This method verifies that the player is not in a restricted state.<br>
	 * It returns {@code false} if the player is protected, mounted, or in an airship.
	 * @param caster The {@code Creature} attempting to cast the skill.
	 * @param skill The {@code Skill} being used.
	 * @param target The {@code WorldObject} that is the target of the skill.
	 * @return {@code true} if the conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		final Player player = caster.asPlayer();
		if ((player == null) || player.isSpawnProtected() || player.isTeleportProtected())
		{
			return false;
		}
		
		boolean canSummon = true;
		if (player.isFlyingMounted() || player.isMounted() || player.getObserverMode().isActive() || player.isTeleporting())
		{
			canSummon = false;
		}
		else if (player.isInAirShip())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_SUMMON_A_SERVITOR_WHILE_MOUNTED);
			canSummon = false;
		}
		
		return canSummon;
	}
}
