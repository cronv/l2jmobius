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
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the target of a skill is a member of the player's Pledge Academy.<br>
 * This condition ensures that the {@code Skill} can only be cast on valid academy targets.
 * @author UnAfraid
 */
public class OpTargetMyPledgeAcademySkillCondition implements ISkillCondition
{
	/**
	 * Creates a new condition for the Pledge Academy skill.<br>
	 * This constructor initializes the requirements using a {@code StatSet}.
	 * @param params The set of statistics used to validate the condition.
	 */
	public OpTargetMyPledgeAcademySkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks if the {@code caster} can use a specific {@code skill} on a {@code target}.<br>
	 * The {@code target} must be a {@code Player} who is an academy member.<br>
	 * Both the {@code caster} and the {@code target} must belong to the same clan.
	 * @param caster The creature attempting to use the skill.
	 * @param skill The skill being used.
	 * @param target The object being targeted by the skill.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		if ((caster.getClan() == null) || (target == null) || !target.isPlayer())
		{
			return false;
		}
		
		final Player targetPlayer = target.asPlayer();
		return targetPlayer.isAcademyMember() && (targetPlayer.getClan() == caster.getClan());
	}
}
