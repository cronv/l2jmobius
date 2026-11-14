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
 * Checks if the player meets the specific requirements for an Oath Pledge skill.<br>
 * This condition validates whether the {@code Creature} is eligible to use the associated {@code Skill}.
 * @author UnAfraid
 */
public class OpPledgeSkillCondition implements ISkillCondition
{
	private final int _level;
	
	/**
	 * Creates a new {@code OpPledgeSkillCondition} using the provided statistics.<br>
	 * This constructor extracts the required level from the {@code StatSet}.
	 * @param params The {@code StatSet} containing the skill requirements.
	 */
	public OpPledgeSkillCondition(StatSet params)
	{
		_level = params.getInt("level");
	}
	
	/**
	 * Checks if the {@code caster} meets the requirements to use a specific {@code skill}.<br>
	 * This method verifies that the {@code caster} belongs to a {@code Clan}.<br>
	 * It also checks if the {@code Clan} level is high enough.
	 * @param caster The creature attempting to cast the skill.
	 * @param skill The skill being used.
	 * @param target The object targeted by the skill.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		final Clan clan = caster.getClan();
		return (clan != null) && (clan.getLevel() >= _level);
	}
}
