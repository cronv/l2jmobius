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
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.handler.AffectObjectHandler;
import org.l2jmobius.gameserver.handler.IAffectObjectHandler;
import org.l2jmobius.gameserver.handler.IAffectScopeHandler;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.targets.AffectScope;

/**
 * Handles skill effects applied to a party where all members are dead.<br>
 * This class ensures that the effect is processed correctly for deceased group members.
 * @author Nik
 */
public class DeadParty implements IAffectScopeHandler
{
	/**
	 * Iterates through all affected objects based on the dead party logic.<br>
	 * This method identifies members of the same party as the target who are currently dead.<br>
	 * It applies a specific action to each valid object found within the skill range.
	 * @param creature The {@code Creature} casting the skill.
	 * @param target The primary {@code WorldObject} being targeted.
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
			final Player player = target.asPlayer();
			final Party party = player.getParty();
			
			// Create the target filter.
			final AtomicInteger affected = new AtomicInteger(0);
			final Predicate<Playable> filter = plbl ->
			{
				if ((affectLimit > 0) && (affected.get() >= affectLimit))
				{
					return false;
				}
				
				final Player p = plbl.asPlayer();
				if ((p == null) || !p.isDead())
				{
					return false;
				}
				
				if (p != player)
				{
					final Party targetParty = p.getParty();
					if ((party == null) || (targetParty == null) || (party.getLeaderObjectId() != targetParty.getLeaderObjectId()))
					{
						return false;
					}
				}
				
				if ((affectObject != null) && !affectObject.checkAffectedObject(creature, p))
				{
					return false;
				}
				
				affected.incrementAndGet();
				return true;
			};
			
			// Affect object of origin since it is skipped in the forEachVisibleObjectInRange method.
			if (filter.test(target.asPlayable()))
			{
				action.accept(target);
			}
			
			// Check and add targets.
			World.forEachVisibleObjectInRange(target, Playable.class, affectRange, c ->
			{
				if (filter.test(c))
				{
					action.accept(c);
				}
			});
		}
	}
	
	/**
	 * Retrieves the specific type of affect scope for this handler.<br>
	 * This method identifies which {@code AffectScope} category is being used.
	 * @return The {@code AffectScope} enum value associated with this handler.
	 */
	@Override
	public Enum<AffectScope> getAffectScopeType()
	{
		return AffectScope.DEAD_PARTY;
	}
}
