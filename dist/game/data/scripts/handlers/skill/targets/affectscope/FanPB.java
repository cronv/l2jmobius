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
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Implements a fan-shaped point-blank effect scope.<br>
 * It gathers objects within a specific angle of a circular area around the caster.<br>
 * This handler does not take a specific target into account.
 * @author Nik
 */
public class FanPB implements IAffectScopeHandler
{
	/**
	 * Iterates through all creatures affected by the skill's fan area.<br>
	 * It filters targets based on distance, angle, visibility, and life status.<br>
	 * The action is performed on each valid creature found.
	 * @param creature The source {@code Creature} casting the skill.
	 * @param target The primary {@code WorldObject} targeted by the skill.
	 * @param skill The {@code Skill} being executed.
	 * @param action The {@code Consumer} to execute for each affected {@code WorldObject}.
	 */
	@Override
	public void forEachAffected(Creature creature, WorldObject target, Skill skill, Consumer<? super WorldObject> action)
	{
		final IAffectObjectHandler affectObject = AffectObjectHandler.getInstance().getHandler(skill.getAffectObject());
		final double headingAngle = LocationUtil.convertHeadingToDegree(creature.getHeading());
		final int fanStartAngle = skill.getFanRange()[1];
		final int fanRadius = skill.getFanRange()[2];
		final int fanAngle = skill.getFanRange()[3];
		final double fanHalfAngle = fanAngle / 2; // Half left and half right.
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
			
			if (Math.abs(LocationUtil.calculateAngleFrom(creature, c) - (headingAngle + fanStartAngle)) > fanHalfAngle)
			{
				return false;
			}
			
			if ((affectObject != null) && !affectObject.checkAffectedObject(creature, c))
			{
				return false;
			}
			
			if (!GeoEngine.getInstance().canSeeTarget(creature, c))
			{
				return false;
			}
			
			affected.incrementAndGet();
			return true;
		};
		
		// Check and add targets.
		World.forEachVisibleObjectInRange(creature, Creature.class, fanRadius, c ->
		{
			if (filter.test(c))
			{
				action.accept(c);
			}
		});
	}
	
	/**
	 * Retrieves the specific type of affect scope for this handler.<br>
	 * This method identifies the area of effect as {@code FAN_PB}.
	 * @return The {@code AffectScope} enum value representing the fan point blank type.
	 */
	@Override
	public Enum<AffectScope> getAffectScopeType()
	{
		return AffectScope.FAN_PB;
	}
}
