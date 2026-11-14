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

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if a creature possesses the {@code Recall} skill.<br>
 * This condition is used to validate requirements for specific skills.
 * @author Liamxroy
 */
public class OpHasRecallCreatureSkillCondition implements ISkillCondition
{
	/**
	 * Creates a new condition to check if a creature has the recall skill.<br>
	 * This constructor initializes the condition using the provided statistics.
	 * @param params The {@code StatSet} containing the required data.
	 */
	public OpHasRecallCreatureSkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks if the {@code caster} is allowed to use a specific skill.<br>
	 * This condition verifies that the {@code caster} is a player and has a recall creature assigned.
	 * @param caster The entity attempting to cast the skill.
	 * @param skill The skill being attempted.
	 * @param target The object targeted by the skill.
	 * @return {@code true} if the requirements are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		return caster.isPlayer() && (caster.asPlayer().getRecallCreature() != null);
	}
}
