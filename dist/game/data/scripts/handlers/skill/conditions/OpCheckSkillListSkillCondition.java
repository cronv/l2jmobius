/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package handlers.skill.conditions;

import java.util.List;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillConditionAffectType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if a skill is present within a specific list of allowed skills.<br>
 * This condition validates whether the executed {@code Skill} matches any entry in a provided {@code List}.
 * @author Mobius
 */
public class OpCheckSkillListSkillCondition implements ISkillCondition
{
	private final List<Integer> _skillIds;
	private final SkillConditionAffectType _affectType;
	
	/**
	 * Creates a new condition that checks for specific skills.<br>
	 * This constructor initializes the required skill IDs and affect type.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public OpCheckSkillListSkillCondition(StatSet params)
	{
		_skillIds = params.getList("skillIds", Integer.class);
		_affectType = params.getEnum("affectType", SkillConditionAffectType.class);
	}
	
	/**
	 * Checks if the skill can be used based on specific skill requirements.<br>
	 * It verifies if either the {@code caster} or the {@code target} possesses a required skill.<br>
	 * The check depends on the defined {@code SkillConditionAffectType}.
	 * @param caster The creature attempting to use the skill.
	 * @param skill The skill being cast.
	 * @param target The object being targeted by the skill.
	 * @return {@code true} if the requirements are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		switch (_affectType)
		{
			case CASTER:
			{
				for (int id : _skillIds)
				{
					if (caster.getSkillLevel(id) > 0)
					{
						return true;
					}
				}
				break;
			}
			case TARGET:
			{
				if ((target != null) && !target.isPlayer())
				{
					final Player player = target.asPlayer();
					for (int id : _skillIds)
					{
						if (player.getSkillLevel(id) > 0)
						{
							return true;
						}
					}
				}
				break;
			}
		}
		
		return false;
	}
}
