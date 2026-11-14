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
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.EffectScope;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

import handlers.skill.effects.SummonCubic;

/**
 * Checks if the player meets the requirements to use a skill that summons a Cubic.<br>
 * This condition validates whether the {@code Skill} can be executed based on specific game rules.
 * @author UnAfraid
 */
public class CanSummonCubicSkillCondition implements ISkillCondition
{
	/**
	 * Creates a new instance of {@code CanSummonCubicSkillCondition}.<br>
	 * This constructor initializes the condition using specific statistics.
	 * @param params The {@code StatSet} containing the required values.
	 */
	public CanSummonCubicSkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks if the {@code caster} is allowed to use a specific {@code skill}.<br>
	 * It verifies that the user is a living player and not in a restricted state.<br>
	 * The check fails if the player is mounted, protected, or in observer mode.
	 * @param caster The {@code Creature} attempting to cast the skill.
	 * @param skill The {@code Skill} being used.
	 * @param target The {@code WorldObject} targeted by the skill.
	 * @return {@code true} if all conditions are met; {@code false} otherwise.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		if (!caster.isPlayer() || caster.isAlikeDead())
		{
			return false;
		}
		
		final Player player = caster.asPlayer();
		final boolean inObserverMode = player.getObserverMode().isActive();
		if (inObserverMode)
		{
			return false;
		}
		
		if (player.getAutoUseSettings().isAutoSkill(skill.getId()))
		{
			for (AbstractEffect effect : skill.getEffects(EffectScope.GENERAL))
			{
				if ((effect instanceof SummonCubic) && (player.getCubicList().getById(((SummonCubic) effect).getCubicId()) != null))
				{
					return false;
				}
			}
		}
		
		return !inObserverMode && !player.isMounted() && !player.isSpawnProtected() && !player.isTeleportProtected();
	}
}
