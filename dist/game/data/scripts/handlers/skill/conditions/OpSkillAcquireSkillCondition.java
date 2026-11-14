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
 * Checks if a character has acquired a specific skill.<br>
 * This condition is used to verify skill ownership before executing an {@code Skill}.<br>
 * It returns {@code true} if the required skill is present.
 * @author Sdw
 */
public class OpSkillAcquireSkillCondition implements ISkillCondition
{
	private final int _skillId;
	private final boolean _hasLearned;
	
	/**
	 * Creates a new condition to check if a skill is learned.<br>
	 * It uses the provided {@code StatSet} to initialize the required values.
	 * @param params The {@code StatSet} containing {@code skillId} and {@code hasLearned}.
	 */
	public OpSkillAcquireSkillCondition(StatSet params)
	{
		_skillId = params.getInt("skillId");
		_hasLearned = params.getBoolean("hasLearned");
	}
	
	/**
	 * Checks if the caster can use a specific skill based on the target's skills.<br>
	 * It verifies that the target is a valid {@code Creature}.<br>
	 * The result depends on whether the required skill has been learned by the target.
	 * @param caster The {@code Creature} attempting to use the skill.
	 * @param skill The {@code Skill} being cast.
	 * @param target The {@code WorldObject} being targeted.
	 * @return {@code true} if the conditions are met, {@code false} otherwise.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		if ((target == null) || !target.isCreature())
		{
			return false;
		}
		
		final int skillLevel = target.asCreature().getSkillLevel(_skillId);
		return _hasLearned ? skillLevel != 0 : skillLevel == 0;
	}
}
