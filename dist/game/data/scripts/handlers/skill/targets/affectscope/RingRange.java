/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.skill.targets.affectscope;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.handler.AffectObjectHandler;
import org.l2jmobius.gameserver.handler.IAffectObjectHandler;
import org.l2jmobius.gameserver.handler.IAffectScopeHandler;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.targets.AffectScope;

/**
 * This class provides a {@code RingRange} affect scope implementation.<br>
 * It identifies objects located within a donut-shaped area defined by a start and end range.
 * @author Nik
 */
public class RingRange implements IAffectScopeHandler
{
	/**
	 * Iterates through all creatures affected by a skill within a ring range.<br>
	 * It filters targets based on distance, visibility, and the skill's limit.<br>
	 * The provided {@code action} is performed on each valid creature found.
	 * @param creature The creature casting the skill.
	 * @param target The center point of the effect area.
	 * @param skill The {@code Skill} being executed.
	 * @param action A {@code Consumer} to execute on each affected {@code WorldObject}.
	 */
	@Override
	public void forEachAffected(Creature creature, WorldObject target, Skill skill, Consumer<? super WorldObject> action)
	{
		final IAffectObjectHandler affectObject = AffectObjectHandler.getInstance().getHandler(skill.getAffectObject());
		final int affectRange = skill.getAffectRange();
		final int affectLimit = skill.getAffectLimit();
		final int startRange = skill.getFanRange()[2];
		
		// Target checks.
		final AtomicInteger affected = new AtomicInteger(0);
		final Predicate<Creature> filter = c ->
		{
			if ((affectLimit > 0) && (affected.get() >= affectLimit))
			{
				return false;
			}
			
			if (c.isDead())
			{
				return false;
			}
			
			// Targets before the start range are unaffected.
			if (c.isInsideRadius2D(target, startRange))
			{
				return false;
			}
			
			if ((affectObject != null) && !affectObject.checkAffectedObject(creature, c))
			{
				return false;
			}
			
			if (!GeoEngine.getInstance().canSeeTarget(target, c))
			{
				return false;
			}
			
			affected.incrementAndGet();
			return true;
		};
		
		// Check and add targets.
		World.forEachVisibleObjectInRange(target, Creature.class, affectRange, c ->
		{
			if (filter.test(c))
			{
				action.accept(c);
			}
		});
	}
	
	/**
	 * Retrieves the type of affect scope for this handler.<br>
	 * This method identifies the specific area shape used by the effect.
	 * @return The {@code AffectScope} enum value representing the ring range.
	 */
	@Override
	public Enum<AffectScope> getAffectScopeType()
	{
		return AffectScope.RING_RANGE;
	}
}
