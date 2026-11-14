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
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if a player has successfully executed a specific skill.<br>
 * This condition validates the execution of a {@code Skill} by a {@code Player}.<br>
 * It is used to verify that a required action was performed before allowing a skill to trigger.
 * @author Sdw
 */
public class OpCallPcSkillCondition implements ISkillCondition
{
	/**
	 * Creates a new {@code OpCallPcSkillCondition} instance.<br>
	 * This constructor initializes the condition using the provided statistics.
	 * @param params The {@code StatSet} containing the required parameters.
	 */
	public OpCallPcSkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks if the {@code caster} is allowed to use a specific {@code skill} on a {@code target}.<br>
	 * This method validates various restrictions such as player status, zone limitations, and game modes.
	 * @param caster The {@code Creature} attempting to cast the skill.
	 * @param skill The {@code Skill} being used.
	 * @param target The {@code WorldObject} that is the target of the skill.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		boolean canCallPlayer = true;
		final Player player = caster.asPlayer();
		if (player == null)
		{
			canCallPlayer = false;
		}
		else if ((target != null) && target.isPlayer() && target.asPlayer().isDead())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_DEAD_AT_THE_MOMENT_AND_CANNOT_BE_SUMMONED_OR_TELEPORTED);
			sm.addPcName(target.asPlayer());
			player.sendPacket(sm);
			canCallPlayer = false;
		}
		else if (player.isInOlympiadMode())
		{
			player.sendPacket(SystemMessageId.A_USER_PARTICIPATING_IN_THE_OLYMPIAD_CANNOT_USE_SUMMONING_OR_TELEPORTING);
			canCallPlayer = false;
		}
		else if (player.getObserverMode().isActive())
		{
			canCallPlayer = false;
		}
		else if (player.isOnEvent())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_SUMMONING_OR_TELEPORTING_IN_THIS_AREA);
			canCallPlayer = false;
		}
		else if (player.isInsideZone(ZoneId.NO_SUMMON_FRIEND) || player.isInsideZone(ZoneId.JAIL) || player.isFlyingMounted())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_SUMMONING_OR_TELEPORTING_IN_THIS_AREA);
			canCallPlayer = false;
		}
		
		return canCallPlayer;
	}
}
