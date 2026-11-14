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
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class checks if a specific skill is currently active or available for use.<br>
 * It implements the {@code ISkillCondition} interface to validate skill requirements.<br>
 * Use this condition when a skill's execution depends on another skill's state.
 * @author UnAfraid
 */
public class OpSkillSkillCondition implements ISkillCondition
{
	private final int _skillId;
	private final int _skillLevel;
	private final boolean _hasLearned;
	
	/**
	 * Creates a new {@code OpSkillSkillCondition} using the provided parameters.<br>
	 * This constructor extracts required values from a {@code StatSet}.<br>
	 * It initializes the skill ID, level, and learning status.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public OpSkillSkillCondition(StatSet params)
	{
		_skillId = params.getInt("skillId");
		_skillLevel = params.getInt("skillLevel");
		_hasLearned = params.getBoolean("hasLearned");
	}
	
	/**
	 * Checks if the {@code caster} meets the requirements to use a specific skill.<br>
	 * This method validates the skill level and ownership based on pre-defined conditions.
	 * @param caster The {@code Creature} attempting to perform the action.
	 * @param skill The {@code Skill} being used by the caster.
	 * @param target The {@code WorldObject} that is being targeted.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		final Skill requestedSkill = caster.getKnownSkill(_skillId);
		if (_hasLearned)
		{
			return (requestedSkill != null) && (requestedSkill.getLevel() == _skillLevel);
		}
		
		return (requestedSkill == null) || (requestedSkill.getLevel() != _skillLevel);
	}
}
