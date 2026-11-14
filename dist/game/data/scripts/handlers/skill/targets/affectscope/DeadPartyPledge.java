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
 * This class handles the affect scope for Dead Party and Clan effects.<br>
 * It determines which entities are affected based on group membership.
 * @author Nik
 */
public class DeadPartyPledge implements IAffectScopeHandler
{
	/**
	 * Iterates through all affected objects based on the skill scope.<br>
	 * This method identifies dead players within a party or clan and applies an action to them.<br>
	 * It respects the range and limit defined by the {@code Skill}.
	 * @param creature The source of the effect.
	 * @param target The primary object being targeted.
	 * @param skill The skill being executed.
	 * @param action The operation to perform on each affected {@code WorldObject}.
	 */
	@Override
	public void forEachAffected(Creature creature, WorldObject target, Skill skill, Consumer<? super WorldObject> action)
	{
		final IAffectObjectHandler affectObject = AffectObjectHandler.getInstance().getHandler(skill.getAffectObject());
		final int affectRange = skill.getAffectRange();
		final int affectLimit = skill.getAffectLimit();
		if (target.isPlayable())
		{
			final Playable playable = target.asPlayable();
			final Player player = playable.asPlayer();
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
				
				final int clanId = p.getClanId();
				if ((p != player) && ((clanId == 0) || (clanId != player.getClanId())))
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
			if (filter.test(playable))
			{
				action.accept(playable);
			}
			
			// Check and add targets.
			World.forEachVisibleObjectInRange(playable, Playable.class, affectRange, c ->
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
	 * This identifies how the effect is applied to targets.
	 * @return The {@code AffectScope} enum value representing the scope type.
	 */
	@Override
	public Enum<AffectScope> getAffectScopeType()
	{
		return AffectScope.DEAD_PARTY_PLEDGE;
	}
}
