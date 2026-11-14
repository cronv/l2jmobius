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
 * Checks if a specific skill is not currently being used in an event.<br>
 * This condition returns {@code true} if the skill is absent from the active event list.
 * @author Mobius
 */
public class OpNotInEventSkillCondition implements ISkillCondition
{
	/**
	 * Creates a new condition to check if a skill is not part of an event.<br>
	 * This constructor initializes the condition using the provided statistics.
	 * @param params The {@code StatSet} containing the required parameters.
	 */
	public OpNotInEventSkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks if the {@code caster} is allowed to use a specific {@code skill}.<br>
	 * This condition returns {@code true} only if the {@code caster} is a player.<br>
	 * It also ensures that the player is not currently participating in an event.
	 * @param caster The creature attempting to use the skill.
	 * @param skill The skill being used.
	 * @param target The object being targeted by the skill.
	 * @return {@code true} if the player is not on an event, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		return (caster.isPlayer() && !caster.asPlayer().isOnEvent());
	}
}
