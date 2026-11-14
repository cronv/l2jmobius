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
import java.util.Set;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the target of a {@code Skill} is an open door.<br>
 * This condition ensures that the skill can only be executed on doors with a state of {@code true}.
 * @author Mobius
 */
public class OpTargetDoorSkillCondition implements ISkillCondition
{
	private final Set<Integer> _doorIds = new HashSet<>();
	
	/**
	 * Creates a new condition to check if a target is a specific door.<br>
	 * It initializes the allowed doors from the provided parameters.
	 * @param params The {@code StatSet} containing the list of valid door IDs.
	 */
	public OpTargetDoorSkillCondition(StatSet params)
	{
		_doorIds.addAll(params.getList("doorIds", Integer.class));
	}
	
	/**
	 * Checks if a {@code Skill} can be used on a specific {@code WorldObject}.<br>
	 * The target must not be {@code null}.<br>
	 * The target must be a door.<br>
	 * The door's ID must exist in the allowed list.
	 * @param caster The {@code Creature} attempting to use the skill.
	 * @param skill The {@code Skill} being cast.
	 * @param target The {@code WorldObject} that is the target of the skill.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		return (target != null) && target.isDoor() && _doorIds.contains(target.getId());
	}
}
