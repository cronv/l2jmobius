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
 * This class handles the effect scope for party, clan and alliance members.<br>
 * It determines which entities are affected based on group membership.
 * @author Nik, Mobius
 */
public class PartyPledgeAlly implements IAffectScopeHandler
{
	/**
	 * Executes an action for all affected objects within a specific scope.<br>
	 * This method filters targets based on party, clan, alliance and skill limits.<br>
	 * It identifies valid {@code Playable} entities near the target.
	 * @param creature The source of the effect.
	 * @param target The primary object being targeted by the skill.
	 * @param skill The {@code Skill} instance defining the range and limits.
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
			final Playable playable = target.asPlayable();
			final Player player = playable.asPlayer();
			final Party party = player.getParty();
			final int clanId = player.getClanId();
			final int allyId = player.getAllyId();
			
			// Create the target filter.
			final AtomicInteger affected = new AtomicInteger(0);
			final Predicate<Playable> filter = c ->
			{
				if ((affectLimit > 0) && (affected.get() >= affectLimit))
				{
					return false;
				}
				
				final Player p = c.asPlayer();
				if ((p == null) || p.isDead())
				{
					return false;
				}
				
				if ((p != player) && ((clanId == 0) || (p.getClanId() != clanId)) && ((allyId == 0) || (p.getAllyId() != allyId)) && ((party == null) || (party != p.getParty())))
				{
					return false;
				}
				
				if ((affectObject != null) && !affectObject.checkAffectedObject(creature, p))
				{
					return false;
				}
				
				affected.incrementAndGet();
				return true;
			};
			
			// Add object of origin since it is skipped in the forEachVisibleObjectInRange method.
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
	 * This identifies how the effect is distributed among targets.
	 * @return The {@code AffectScope} enum value representing the party pledge ally scope.
	 */
	@Override
	public Enum<AffectScope> getAffectScopeType()
	{
		return AffectScope.PARTY_PLEDGE_ALLY;
	}
}
