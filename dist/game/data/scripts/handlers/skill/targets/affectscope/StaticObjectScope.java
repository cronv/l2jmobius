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
import org.l2jmobius.gameserver.entity.actor.instance.StaticObject;
import org.l2jmobius.gameserver.handler.AffectObjectHandler;
import org.l2jmobius.gameserver.handler.IAffectObjectHandler;
import org.l2jmobius.gameserver.handler.IAffectScopeHandler;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.targets.AffectScope;

/**
 * This class provides an implementation of {@code IAffectScopeHandler} for static objects.<br>
 * It is primarily used to detect specific entities like hidden doors.
 * @author Nik
 */
public class StaticObjectScope implements IAffectScopeHandler
{
	/**
	 * Iterates through all creatures affected by a skill within a specific range.<br>
	 * This method filters targets based on the skill's limits and object types.<br>
	 * It executes a provided action for every valid target found.
	 * @param creature The creature who cast the skill.
	 * @param target The primary target of the skill.
	 * @param skill The {@code Skill} being executed.
	 * @param action A {@code Consumer} to perform on each affected {@code WorldObject}.
	 */
	@Override
	public void forEachAffected(Creature creature, WorldObject target, Skill skill, Consumer<? super WorldObject> action)
	{
		final IAffectObjectHandler affectObject = AffectObjectHandler.getInstance().getHandler(skill.getAffectObject());
		final int affectRange = skill.getAffectRange();
		final int affectLimit = skill.getAffectLimit();
		
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
			
			if (!c.isDoor() && !(c instanceof StaticObject))
			{
				return false;
			}
			
			if ((affectObject != null) && !affectObject.checkAffectedObject(creature, c))
			{
				return false;
			}
			
			affected.incrementAndGet();
			return true;
		};
		
		// Add object of origin since it is skipped in the forEachVisibleObjectInRange method.
		if (target.isCreature() && filter.test(target.asCreature()))
		{
			action.accept(target);
		}
		
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
	 * Retrieves the scope type for this handler.<br>
	 * It returns the {@code AffectScope} constant associated with static objects.
	 * @return The {@code AffectScope} enum value representing the scope type.
	 */
	@Override
	public Enum<AffectScope> getAffectScopeType()
	{
		return AffectScope.STATIC_OBJECT_SCOPE;
	}
}
