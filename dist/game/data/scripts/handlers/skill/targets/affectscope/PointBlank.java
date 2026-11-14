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

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.handler.AffectObjectHandler;
import org.l2jmobius.gameserver.handler.IAffectObjectHandler;
import org.l2jmobius.gameserver.handler.IAffectScopeHandler;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.targets.AffectObject;
import org.l2jmobius.gameserver.mechanics.skill.targets.AffectScope;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;

/**
 * This class handles the {@code PointBlank} affect scope.<br>
 * It gathers all targets within a specific radius while excluding the initial target.
 * @author Nik
 */
public class PointBlank implements IAffectScopeHandler
{
	/**
	 * Iterates through all creatures affected by the skill and performs an action on each.<br>
	 * This method handles range, visibility, and target limits based on the {@code Skill} configuration.
	 * @param creature The {@code Creature} casting the skill.
	 * @param target The primary {@code WorldObject} target of the skill.
	 * @param skill The {@code Skill} being executed.
	 * @param action A {@code Consumer} to execute on each affected {@code WorldObject}.
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
			
			if (affectObject != null)
			{
				if (c.isDead() && (skill.getAffectObject() != AffectObject.OBJECT_DEAD_NPC_BODY))
				{
					return false;
				}
				
				if (!affectObject.checkAffectedObject(creature, c))
				{
					return false;
				}
			}
			
			if (!GeoEngine.getInstance().canSeeTarget(target, c))
			{
				return false;
			}
			
			affected.incrementAndGet();
			return true;
		};
		
		// Check and add targets.
		if (skill.getTargetType() == TargetType.GROUND)
		{
			if (creature.isPlayable())
			{
				final Location worldPosition = creature.asPlayer().getCurrentSkillWorldPosition();
				if (worldPosition != null)
				{
					World.forEachVisibleObjectInRange(creature, Creature.class, (int) (affectRange + creature.calculateDistance2D(worldPosition)), c ->
					{
						if (!c.isInsideRadius3D(worldPosition, affectRange))
						{
							return;
						}
						
						if (filter.test(c))
						{
							action.accept(c);
						}
					});
				}
			}
		}
		else
		{
			World.forEachVisibleObjectInRange(target, Creature.class, affectRange, c ->
			{
				if (filter.test(c))
				{
					action.accept(c);
				}
			});
		}
	}
	
	/**
	 * Retrieves the specific scope type for this handler.<br>
	 * This method identifies the effect as {@code POINT_BLANK}.
	 * @return The {@code AffectScope} enum value representing the point blank range.
	 */
	@Override
	public Enum<AffectScope> getAffectScopeType()
	{
		return AffectScope.POINT_BLANK;
	}
}
