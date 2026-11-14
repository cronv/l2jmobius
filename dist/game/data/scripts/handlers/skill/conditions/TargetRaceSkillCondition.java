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

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the target of a {@code Skill} belongs to a specific {@code Race}.<br>
 * This condition ensures that a skill can only be cast on creatures of the correct type.
 * @author UnAfraid, Mobius
 */
public class TargetRaceSkillCondition implements ISkillCondition
{
	private final Set<Race> _races = EnumSet.noneOf(Race.class);
	
	/**
	 * Creates a new condition to check the race of a target.<br>
	 * This constructor extracts the required race from a {@code StatSet}.
	 * @param params The {@code StatSet} containing the "race" enum value.
	 */
	public TargetRaceSkillCondition(StatSet params)
	{
		final List<Race> races = params.getEnumList("race", Race.class);
		if (races != null)
		{
			_races.addAll(races);
		}
		else
		{
			_races.add(params.getEnum("race", Race.class));
		}
	}
	
	/**
	 * Checks if the skill can be used on a specific target.<br>
	 * It verifies that the target is a {@code Creature}.<br>
	 * It also checks if the target belongs to the required race.
	 * @param caster The creature casting the skill.
	 * @param skill The skill being attempted.
	 * @param target The object being targeted.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		if ((target == null) || !target.isCreature())
		{
			return false;
		}
		
		return _races.contains(target.asCreature().getRace());
	}
}
