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
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillConditionAffectType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if a {@code Player} meets the minimum level requirement to use a specific skill.<br>
 * This condition validates the character's current level against the required value defined in the {@code Skill} data.
 * @author UnAfraid
 */
public class CheckLevelSkillCondition implements ISkillCondition
{
	private final int _minLevel;
	private final int _maxLevel;
	private final SkillConditionAffectType _affectType;
	
	/**
	 * Creates a new skill condition based on level requirements.<br>
	 * This constructor extracts values from the provided {@code StatSet}.<br>
	 * It sets the minimum and maximum levels for the skill check.
	 * @param params The {@code StatSet} containing configuration data.
	 */
	public CheckLevelSkillCondition(StatSet params)
	{
		_minLevel = params.getInt("minLevel", 1);
		_maxLevel = params.getInt("maxLevel", Integer.MAX_VALUE);
		_affectType = params.getEnum("affectType", SkillConditionAffectType.class, SkillConditionAffectType.CASTER);
	}
	
	/**
	 * Checks if the level requirements for a skill are met.<br>
	 * It validates the level of either the {@code caster} or the {@code target}.<br>
	 * The check depends on the defined {@code SkillConditionAffectType}.
	 * @param caster The creature casting the skill.
	 * @param skill The skill being attempted.
	 * @param target The object being targeted by the skill.
	 * @return {@code true} if the level requirements are satisfied, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		switch (_affectType)
		{
			case CASTER:
			{
				return (caster.getLevel() >= _minLevel) && (caster.getLevel() <= _maxLevel);
			}
			case TARGET:
			{
				if ((target != null) && target.isPlayer())
				{
					final Player player = target.asPlayer();
					return (player.getLevel() >= _minLevel) && (player.getLevel() <= _maxLevel);
				}
				break;
			}
		}
		
		return false;
	}
}
