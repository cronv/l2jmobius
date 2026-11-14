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
 * Checks if a skill's effect is triggered by a critical hit.<br>
 * This condition validates whether the {@code Skill} execution meets specific critical hit requirements.
 * @author Sdw
 */
public class OpCheckCrtEffectSkillCondition implements ISkillCondition
{
	/**
	 * Creates a new {@code OpCheckCrtEffectSkillCondition} object.<br>
	 * This constructor initializes the condition using the provided statistics.
	 * @param params The {@code StatSet} containing the required values.
	 */
	public OpCheckCrtEffectSkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks if the skill can be used on a specific target.<br>
	 * The target must be an NPC.<br>
	 * The NPC must also be capable of being hit by critical strikes.
	 * @param caster The {@code Creature} casting the skill.
	 * @param skill The {@code Skill} being attempted.
	 * @param target The {@code WorldObject} targeted by the skill.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		return (target != null) && target.isNpc() && target.asNpc().getTemplate().canBeCrt();
	}
}
