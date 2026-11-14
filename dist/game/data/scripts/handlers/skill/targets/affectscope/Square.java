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
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * This class provides an implementation of the {@code AffectScope} for square-shaped areas.<br>
 * It defines a rectangular region to determine which entities are affected by a skill.<br>
 * Although named {@code Square}, it functions as a rectangle.
 * @author Nik
 */
public class Square implements IAffectScopeHandler
{
	/**
	 * Iterates through all creatures affected by the square scope.<br>
	 * It applies the provided action to each valid creature found within the area.<br>
	 * The method checks for visibility, distance, and skill-specific limits.
	 * @param creature The source creature casting the skill.
	 * @param target The primary target of the skill.
	 * @param skill The skill being executed.
	 * @param action The operation to perform on each affected {@code WorldObject}.
	 */
	@Override
	public void forEachAffected(Creature creature, WorldObject target, Skill skill, Consumer<? super WorldObject> action)
	{
		final IAffectObjectHandler affectObject = AffectObjectHandler.getInstance().getHandler(skill.getAffectObject());
		final int squareStartAngle = skill.getFanRange()[1];
		final int squareLength = skill.getFanRange()[2];
		final int squareWidth = skill.getFanRange()[3];
		final int radius = (int) Math.sqrt((squareLength * squareLength) + (squareWidth * squareWidth));
		final int affectLimit = skill.getAffectLimit();
		final int rectX = creature.getX();
		final int rectY = creature.getY() - (squareWidth / 2);
		final double heading = Math.toRadians(squareStartAngle + LocationUtil.convertHeadingToDegree(creature.getHeading()));
		final double cos = Math.cos(-heading);
		final double sin = Math.sin(-heading);
		
		// Target checks.
		final TargetType targetType = skill.getTargetType();
		final AtomicInteger affected = new AtomicInteger(0);
		final Predicate<Creature> filter = c ->
		{
			if ((affectLimit > 0) && (affected.get() >= affectLimit))
			{
				return false;
			}
			
			if (c.isDead() && (targetType != TargetType.NPC_BODY) && (targetType != TargetType.PC_BODY))
			{
				return false;
			}
			
			// Check if inside square.
			final int creatureX = creature.getX();
			final int creatureY = creature.getY();
			final int xp = c.getX() - creatureX;
			final int yp = c.getY() - creatureY;
			final int xr = (int) ((creatureX + (xp * cos)) - (yp * sin));
			final int yr = (int) (creatureY + (xp * sin) + (yp * cos));
			if ((xr > rectX) && (xr < (rectX + squareLength)) && (yr > rectY) && (yr < (rectY + squareWidth)))
			{
				if ((c != target) && (affectObject != null) && !affectObject.checkAffectedObject(creature, c))
				{
					return false;
				}
				
				if (!GeoEngine.getInstance().canSeeTarget(creature, c))
				{
					return false;
				}
				
				affected.incrementAndGet();
				return true;
			}
			
			return false;
		};
		
		// Add object of origin since it is skipped in the forEachVisibleObjectInRange method.
		if (filter.test(creature))
		{
			action.accept(creature);
		}
		
		// Check and add targets.
		World.forEachVisibleObjectInRange(creature, Creature.class, radius, c ->
		{
			if (filter.test(c))
			{
				action.accept(c);
			}
		});
	}
	
	/**
	 * Retrieves the type of affect scope for this handler.<br>
	 * It returns the {@code AffectScope} constant associated with this class.
	 * @return The {@code AffectScope} enum value representing a square area.
	 */
	@Override
	public Enum<AffectScope> getAffectScopeType()
	{
		return AffectScope.SQUARE;
	}
}
