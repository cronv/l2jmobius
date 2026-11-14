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
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the distance between the caster and the target is within the allowed range.<br>
 * This condition ensures that a {@code Skill} can only be executed if it meets specific spatial requirements.
 * @author Mobius
 */
public class OpCheckCastRangeSkillCondition implements ISkillCondition
{
	private final int _distance;
	
	/**
	 * Creates a new condition to check if a skill is within a specific range.<br>
	 * It extracts the required distance from the provided {@code StatSet}.
	 * @param params The {@code StatSet} containing the "distance" value.
	 */
	public OpCheckCastRangeSkillCondition(StatSet params)
	{
		_distance = params.getInt("distance");
	}
	
	/**
	 * Checks if the caster can use a specific skill on a target.<br>
	 * This method verifies that the target is not {@code null}.<br>
	 * It also checks if the distance between the caster and target meets the required range.<br>
	 * Finally, it ensures the caster has a clear line of sight to the target.
	 * @param caster The creature attempting to use the skill.
	 * @param skill The skill being cast.
	 * @param target The object that is the target of the skill.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		return (target != null) //
			&& (caster.calculateDistance3D(target) >= _distance) //
			&& GeoEngine.getInstance().canSeeTarget(caster, target);
	}
}
