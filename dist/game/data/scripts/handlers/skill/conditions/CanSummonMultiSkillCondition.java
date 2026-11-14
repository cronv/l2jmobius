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
 * Checks if a player is allowed to use a skill that summons multiple entities.<br>
 * This condition ensures the requirements for multi-summoning skills are met.
 * @author Sdw
 */
public class CanSummonMultiSkillCondition implements ISkillCondition
{
	private final int _summonPoints;
	
	/**
	 * Creates a new condition to check for multi-skill summoning.<br>
	 * It initializes the required points from the provided {@code StatSet}.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public CanSummonMultiSkillCondition(StatSet params)
	{
		_summonPoints = params.getInt("summonPoints");
	}
	
	/**
	 * Checks if the {@code caster} is allowed to use a specific {@code skill}.<br>
	 * This method validates various conditions like summon points and player state.<br>
	 * It returns {@code false} if any requirements are not met.
	 * @param caster The {@code Creature} attempting to cast the skill.
	 * @param skill The {@code Skill} being used.
	 * @param target The {@code WorldObject} targeted by the skill.
	 * @return {@code true} if the skill can be used, otherwise {@code false}.
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
		
		final int servitorSize = player.getServitors().size();
		if ((servitorSize == 1) && (player.getSummonPoints() == 0))
		{
			canSummon = false;
		}
		else if (servitorSize > 4)
		{
			canSummon = false;
		}
		else if (player.isFlyingMounted() || player.isMounted() || player.getObserverMode().isActive() || player.isTeleporting())
		{
			canSummon = false;
		}
		else if (player.isInAirShip())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_SUMMON_A_SERVITOR_WHILE_MOUNTED);
			canSummon = false;
		}
		else if ((player.getSummonPoints() + _summonPoints) > player.getMaxSummonPoints())
		{
			canSummon = false;
		}
		
		return canSummon;
	}
}
