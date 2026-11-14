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
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if a {@code Skill} requires the consumption of a body to be executed.<br>
 * This condition validates whether the target entity is a valid source for the required resource.
 * @author UnAfraid
 */
public class ConsumeBodySkillCondition implements ISkillCondition
{
	/**
	 * Creates a new {@code ConsumeBodySkillCondition} instance.<br>
	 * This constructor initializes the condition using the provided statistics.
	 * @param params The {@code StatSet} containing the required values.
	 */
	public ConsumeBodySkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks if the skill can be used on a specific target.<br>
	 * The target must be a dead monster or summon that is still spawned in the world.<br>
	 * Returns {@code true} if all conditions are met.
	 * @param caster The creature attempting to use the skill.
	 * @param skill The skill being cast.
	 * @param target The object targeted by the skill.
	 * @return {@code true} if the skill can be used, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		if ((target != null) && (target.isMonster() || target.isSummon()))
		{
			final Creature creature = target.asCreature();
			if (creature.isDead() && creature.isSpawned())
			{
				return true;
			}
		}
		
		if (caster.isPlayer())
		{
			caster.sendPacket(SystemMessageId.INVALID_TARGET);
		}
		
		return false;
	}
}
