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
 * Checks if a player belongs to a specific social class before allowing a skill to be used.<br>
 * This condition validates the {@code Player}'s current status against required social class requirements.
 * @author UnAfraid
 */
public class OpSocialClassSkillCondition implements ISkillCondition
{
	private final int _socialClass;
	
	/**
	 * Creates a new condition based on a social class.<br>
	 * This constructor extracts the required value from the provided {@code StatSet}.
	 * @param params The {@code StatSet} containing the "socialClass" integer.
	 */
	public OpSocialClassSkillCondition(StatSet params)
	{
		_socialClass = params.getInt("socialClass");
	}
	
	/**
	 * Checks if the {@code caster} meets the social class requirements to use a skill.<br>
	 * This method verifies the player's clan status and pledge level.
	 * @param caster The {@code Creature} attempting to cast the skill.
	 * @param skill The {@code Skill} being used.
	 * @param target The {@code WorldObject} targeted by the skill.
	 * @return {@code true} if the requirements are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		final Player player = caster.asPlayer();
		if ((player == null) || (player.getClan() == null))
		{
			return false;
		}
		
		final boolean isClanLeader = player.isClanLeader();
		if ((_socialClass == -1) && !isClanLeader)
		{
			return false;
		}
		
		return isClanLeader || (player.getPledgeType() >= _socialClass);
	}
}
