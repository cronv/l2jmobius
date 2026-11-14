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
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the player meets the requirements to perform a fishing reeling skill.<br>
 * This condition validates specific criteria for {@code Skill} execution during fishing activities.
 * @author Mobius
 */
public class OpFishingReelingSkillCondition implements ISkillCondition
{
	/**
	 * Creates a new instance of {@code OpFishingReelingSkillCondition}.<br>
	 * This constructor initializes the condition using provided statistics.
	 * @param params The {@code StatSet} containing the required parameters.
	 */
	public OpFishingReelingSkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks if the player is allowed to use a specific skill.<br>
	 * This method verifies if the {@code caster} is currently in the fishing zone.
	 * @param caster The creature attempting to use the skill.
	 * @param skill The skill being used.
	 * @param target The object targeted by the skill.
	 * @return {@code true} if the player is in the correct zone, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		return caster.isInsideZone(ZoneId.FISHING);
	}
}
