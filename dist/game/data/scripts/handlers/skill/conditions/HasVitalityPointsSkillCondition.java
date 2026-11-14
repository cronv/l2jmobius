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
 * Checks if a creature possesses the required amount of vitality points.<br>
 * This condition is used to validate skill requirements based on {@code StatSet} values.
 * @author Mode
 */
public class HasVitalityPointsSkillCondition implements ISkillCondition
{
	private final int _amount;
	
	/**
	 * Creates a new condition to check for vitality points.<br>
	 * This constructor initializes the required amount from a {@code StatSet}.<br>
	 * It defaults to {@code 1} if no value is provided.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public HasVitalityPointsSkillCondition(StatSet params)
	{
		_amount = params.getInt("amount", 1);
	}
	
	/**
	 * Checks if the {@code caster} meets the requirements to use a specific {@code skill}.<br>
	 * This method verifies that the {@code caster} is a player.<br>
	 * It also checks if the player has enough vitality points.
	 * @param caster The {@code Creature} attempting to perform the action.
	 * @param skill The {@code Skill} being used.
	 * @param target The {@code WorldObject} targeted by the skill.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		return caster.isPlayer() && (caster.asPlayer().getVitalityPoints() >= _amount);
	}
}
