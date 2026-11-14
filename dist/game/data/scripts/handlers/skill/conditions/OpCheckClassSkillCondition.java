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
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillConditionAffectType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the player belongs to a specific {@code PlayerClass} before executing a skill.<br>
 * This condition ensures that only certain classes can trigger the associated {@code Skill}.
 * @author UnAfraid
 */
public class OpCheckClassSkillCondition implements ISkillCondition
{
	private final PlayerClass _classId;
	private final SkillConditionAffectType _affectType;
	private final boolean _isWithin;
	
	/**
	 * Creates a new {@code OpCheckClassSkillCondition} using the provided data.<br>
	 * This constructor extracts values from a {@code StatSet} to initialize the condition.<br>
	 * It sets the required class, affect type, and range check status.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public OpCheckClassSkillCondition(StatSet params)
	{
		_classId = params.getEnum("classId", PlayerClass.class);
		_affectType = params.getEnum("affectType", SkillConditionAffectType.class);
		_isWithin = params.getBoolean("isWithin");
	}
	
	/**
	 * Checks if the skill can be used based on class requirements.<br>
	 * It validates the {@code PlayerClass} of either the caster or the target.<br>
	 * The result depends on whether the required class matches the specified condition.
	 * @param caster The creature casting the skill.
	 * @param skill The skill being attempted.
	 * @param target The object being targeted by the skill.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		switch (_affectType)
		{
			case CASTER:
			{
				return caster.isPlayer() && (_isWithin == (_classId == caster.asPlayer().getPlayerClass()));
			}
			case TARGET:
			{
				if ((target != null) && !target.isPlayer())
				{
					return _isWithin == (_classId == target.asPlayer().getPlayerClass());
				}
				break;
			}
		}
		
		return false;
	}
}
