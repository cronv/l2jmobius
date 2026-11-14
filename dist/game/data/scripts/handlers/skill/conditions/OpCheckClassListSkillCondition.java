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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillConditionAffectType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the target creature belongs to a specific list of allowed classes.<br>
 * This condition is used to restrict skill usage based on {@code PlayerClass} types.
 * @author UnAfraid, Mobius
 */
public class OpCheckClassListSkillCondition implements ISkillCondition
{
	private final Set<PlayerClass> _classIds = new HashSet<>();
	private final SkillConditionAffectType _affectType;
	private final boolean _isWithin;
	
	/**
	 * Creates a new {@code OpCheckClassListSkillCondition} using the provided parameters.<br>
	 * This constructor initializes the required class list and affect type from a {@code StatSet}.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public OpCheckClassListSkillCondition(StatSet params)
	{
		final List<PlayerClass> classIds = params.getEnumList("classIds", PlayerClass.class);
		if (classIds != null)
		{
			_classIds.addAll(classIds);
		}
		
		_affectType = params.getEnum("affectType", SkillConditionAffectType.class);
		_isWithin = params.getBoolean("isWithin");
	}
	
	/**
	 * Checks if the skill can be used based on specific player classes.<br>
	 * It verifies if the {@code caster} or {@code target} belongs to a permitted class list.
	 * @param caster The creature attempting to use the skill.
	 * @param skill The skill being cast.
	 * @param target The object being targeted by the skill.
	 * @return {@code true} if the class requirements are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		switch (_affectType)
		{
			case CASTER:
			{
				return caster.isPlayer() && (_classIds.contains(caster.asPlayer().getPlayerClass()) == _isWithin);
			}
			case TARGET:
			{
				return (target != null) && target.isPlayer() && (_classIds.contains(target.asPlayer().getPlayerClass()) == _isWithin);
			}
			default:
			{
				return false;
			}
		}
	}
}
