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
import org.l2jmobius.gameserver.managers.MentorManager;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the target of a skill is a mentee of the caster.<br>
 * This condition ensures that the skill can only be used on valid mentees.
 * @author UnAfraid
 */
public class TargetMyMenteeSkillCondition implements ISkillCondition
{
	/**
	 * Creates a new condition to check if the target is a mentee.<br>
	 * It uses the provided {@code StatSet} to initialize the requirements.
	 * @param params The {@code StatSet} containing the necessary configuration data.
	 */
	public TargetMyMenteeSkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks if the {@code caster} can use a specific {@code skill} on a {@code target}.<br>
	 * This method verifies that the {@code target} is a player and is currently a mentee of the {@code caster}.
	 * @param caster The creature attempting to use the skill.
	 * @param skill The skill being cast.
	 * @param target The object being targeted by the skill.
	 * @return {@code true} if the conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		if ((target == null) || !target.isPlayer())
		{
			return false;
		}
		
		return MentorManager.getInstance().getMentee(caster.getObjectId(), target.getObjectId()) != null;
	}
}
