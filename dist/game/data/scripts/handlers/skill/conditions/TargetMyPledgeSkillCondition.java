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
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the target of a skill is a member of the player's pledge.<br>
 * This condition ensures that the skill can only be cast on valid pledge members.
 * @author UnAfraid
 */
public class TargetMyPledgeSkillCondition implements ISkillCondition
{
	/**
	 * Creates a new instance of {@code TargetMyPledgeSkillCondition}.<br>
	 * This constructor initializes the condition using the provided statistics.
	 * @param params The {@code StatSet} containing the required parameters.
	 */
	public TargetMyPledgeSkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks if the skill can be used on a specific target.<br>
	 * The target must be a player and belong to the same clan as the {@code caster}.
	 * @param caster The creature casting the skill.
	 * @param skill The skill being attempted.
	 * @param target The object targeted by the skill.
	 * @return {@code true} if the conditions are met, {@code false} otherwise.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		if ((target == null) || !target.isPlayer())
		{
			return false;
		}
		
		final Clan clan = caster.getClan();
		return (clan != null) && (clan == target.asPlayer().getClan());
	}
}
