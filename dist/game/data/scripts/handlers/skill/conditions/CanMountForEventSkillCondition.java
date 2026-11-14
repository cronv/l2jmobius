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
 * Checks if a player is allowed to mount their pet or mount for an event-specific skill.<br>
 * This condition ensures that the {@code Skill} can only be executed while the user is in a valid mounting state.
 */
public class CanMountForEventSkillCondition implements ISkillCondition
{
	/**
	 * Creates a new condition to check if an event skill can be mounted.<br>
	 * This constructor initializes the requirements using a {@code StatSet}.
	 * @param params The set of statistics used to evaluate the condition.
	 */
	public CanMountForEventSkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks if the {@code caster} is allowed to use a specific {@code skill} on a {@code target}.<br>
	 * This method validates the requirements for performing an action.
	 * @param caster The {@code Creature} attempting to perform the action.
	 * @param skill The {@code Skill} being used.
	 * @param target The {@code WorldObject} that is receiving the action.
	 * @return {@code true} if the action is allowed, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		return true; // TODO
	}
}
