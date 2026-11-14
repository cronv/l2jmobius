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
package handlers.skill.targets.affectscope;

import java.util.function.Consumer;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.handler.AffectObjectHandler;
import org.l2jmobius.gameserver.handler.IAffectObjectHandler;
import org.l2jmobius.gameserver.handler.IAffectScopeHandler;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.targets.AffectScope;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Handles skill effects applied to a scope of targets.<br>
 * This handler ensures that the master of a summon is excluded from receiving the effect.<br>
 * It filters out the specific {@code Creature} that owns the summoned entity.
 * @author Nik, Mobius
 */
public class SummonExceptMaster implements IAffectScopeHandler
{
	/**
	 * Iterates through all valid targets affected by a skill.<br>
	 * This method specifically checks the servitors and pets of a playable target.<br>
	 * It applies the provided {@code action} to each valid creature found.
	 * @param creature The source creature casting the skill.
	 * @param target The primary world object being targeted.
	 * @param skill The {@code Skill} being executed.
	 * @param action The {@code Consumer} to execute on each affected {@code WorldObject}.
	 */
	@Override
	public void forEachAffected(Creature creature, WorldObject target, Skill skill, Consumer<? super WorldObject> action)
	{
		final IAffectObjectHandler affectObject = AffectObjectHandler.getInstance().getHandler(skill.getAffectObject());
		final int affectRange = skill.getAffectRange();
		final int affectLimit = skill.getAffectLimit();
		if (target.isPlayable())
		{
			int count = 0;
			final int limit = (affectLimit > 0) ? affectLimit : Integer.MAX_VALUE;
			for (Creature c : target.asPlayer().getServitorsAndPets())
			{
				if (c.isDead())
				{
					continue;
				}
				
				if ((affectRange > 0) && !LocationUtil.checkIfInRange(affectRange, c, target, true))
				{
					continue;
				}
				
				if ((affectObject != null) && !affectObject.checkAffectedObject(creature, c))
				{
					continue;
				}
				
				count++;
				action.accept(c);
				
				if (count >= limit)
				{
					break;
				}
			}
		}
	}
	
	/**
	 * Retrieves the specific scope type for this handler.<br>
	 * This identifies how the effect is applied to targets.
	 * @return The {@code AffectScope} enum value associated with this handler.
	 */
	@Override
	public Enum<AffectScope> getAffectScopeType()
	{
		return AffectScope.SUMMON_EXCEPT_MASTER;
	}
}
