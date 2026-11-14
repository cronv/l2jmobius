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

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the target of a skill is an {@code NPC}.<br>
 * This condition ensures that the skill can only be cast on non-player entities.
 * @author Sdw
 */
public class OpTargetNpcSkillCondition implements ISkillCondition
{
	private final Set<Integer> _npcIds = new HashSet<>();
	
	/**
	 * Creates a new condition for skills that can only be used on specific NPCs.<br>
	 * It populates the allowed NPC IDs from the provided configuration.
	 * @param params The {@code StatSet} containing the list of valid NPC IDs.
	 */
	public OpTargetNpcSkillCondition(StatSet params)
	{
		_npcIds.addAll(params.getList("npcIds", Integer.class));
	}
	
	/**
	 * Checks if the skill can be used on the specified target.<br>
	 * It verifies that the target is a valid NPC or door.<br>
	 * The target ID must also exist in the allowed list.
	 * @param caster The {@code Creature} casting the skill.
	 * @param skill The {@code Skill} being attempted.
	 * @param target The {@code WorldObject} targeted by the skill.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		final WorldObject actualTarget = (caster == null) || !caster.isPlayer() ? target : caster.getTarget();
		return (actualTarget != null) && (actualTarget.isNpc() || actualTarget.isDoor()) && _npcIds.contains(actualTarget.getId());
	}
}
