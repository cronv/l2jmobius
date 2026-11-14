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
package org.l2jmobius.gameserver.ai;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.npc.AIType;
import org.l2jmobius.gameserver.geoengine.GeoEngine;

/**
 * Handles the artificial intelligence for friendly non-player characters.<br>
 * This class manages behaviors for {@code NPC} entities that do not attack players.<br>
 * It extends {@code AttackableAI} to provide basic interaction logic.
 * @author Sdw, Mobius
 */
public class FriendlyNpcAI extends AttackableAI
{
	/**
	 * Creates a new instance of {@code FriendlyNpcAI}.<br>
	 * This constructor initializes the AI for a specific entity.
	 * @param attackable The {@code Attackable} entity that will use this AI.
	 */
	public FriendlyNpcAI(Attackable attackable)
	{
		super(attackable);
	}
	
	/**
	 * This method is called when an entity attacks the NPC.<br>
	 * It allows the AI to react to the {@code attacker}.
	 * @param attacker The {@code WorldObject} that initiated the attack.
	 */
	@Override
	public void notifyActionAttacked(WorldObject attacker)
	{
	}
	
	/**
	 * Notifies the AI that a specific object has become aggressive.<br>
	 * This method updates the internal state of the NPC based on the target and its aggression level.
	 * @param target The {@code WorldObject} that is initiating the aggression.
	 * @param aggro The intensity level of the aggression as an {@code int}.
	 */
	@Override
	public void notifyActionAggression(WorldObject target, int aggro)
	{
	}
	
	/**
	 * Sets the AI's current intention to attack a specific target.<br>
	 * This method updates the {@code _intention} state and starts the attack task.<br>
	 * It ensures the actor is not currently casting or blocked before proceeding.
	 * @param target The {@code WorldObject} that the AI will focus on attacking.
	 */
	@Override
	public synchronized void setIntentionAttack(WorldObject target)
	{
		if (target == null)
		{
			clientActionFailed();
			return;
		}
		
		if (getIntention() == Intention.REST)
		{
			clientActionFailed();
			return;
		}
		
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow() || _actor.isControlBlocked())
		{
			clientActionFailed();
			return;
		}
		
		// Set the Intention of this AbstractAI to ATTACK.
		_intention = Intention.ATTACK;
		
		// Set the AI attack target.
		setTarget(target);
		
		stopFollow();
		
		// Schedule notifyActionThink repeatedly.
		startAITask();
		
		// Launch the Think Action.
		notifyActionThink();
	}
	
	/**
	 * Handles the logic for an NPC attacking a target.<br>
	 * It checks if the target is alive and within range.<br>
	 * The method decides whether to move toward the target or perform an attack.
	 */
	@Override
	protected void thinkAttack()
	{
		final Attackable npc = getActiveChar();
		if (npc.isCastingNow() || npc.isCoreAIDisabled())
		{
			return;
		}
		
		// Check if target is dead or if timeout is expired to stop this attack.
		final WorldObject target = getTarget();
		final Creature originalAttackTarget = target == null ? null : target.asCreature();
		if ((originalAttackTarget == null) || originalAttackTarget.isAlikeDead())
		{
			// Stop hating this target after the attack timeout or if target is dead.
			if (originalAttackTarget != null)
			{
				npc.stopHating(originalAttackTarget);
			}
			
			setIntentionActive();
			npc.setWalking();
			return;
		}
		
		final int collision = npc.getTemplate().getCollisionRadius();
		setTarget(originalAttackTarget);
		
		final int combinedCollision = collision + originalAttackTarget.getTemplate().getCollisionRadius();
		if (!npc.isMovementDisabled() && (Rnd.get(100) <= 3))
		{
			World.forFirstVisibleObject(npc, Attackable.class, nearby -> npc.isInsideRadius2D(nearby, collision) && (nearby != originalAttackTarget), nearby ->
			{
				int newX = combinedCollision + Rnd.get(40);
				if (Rnd.nextBoolean())
				{
					newX += originalAttackTarget.getX();
				}
				else
				{
					newX = originalAttackTarget.getX() - newX;
				}
				
				int newY = combinedCollision + Rnd.get(40);
				if (Rnd.nextBoolean())
				{
					newY += originalAttackTarget.getY();
				}
				else
				{
					newY = originalAttackTarget.getY() - newY;
				}
				
				if (!npc.isInsideRadius2D(newX, newY, 0, collision))
				{
					final int npcZ = npc.getZ();
					final int newZ = npcZ + 30;
					if (GeoEngine.getInstance().canMoveToTarget(npc.getX(), npc.getY(), npcZ, newX, newY, newZ, npc.getInstanceWorld()))
					{
						moveTo(newX, newY, newZ);
					}
				}
			});
		}
		
		// Calculate Archer movement.
		if ((!npc.isMovementDisabled()) && (npc.getAiType() == AIType.ARCHER) && (Rnd.get(100) < 15))
		{
			final double distance = npc.calculateDistance2D(originalAttackTarget);
			if (distance <= (60 + combinedCollision))
			{
				final int npcX = npc.getX();
				final int npcY = npc.getY();
				final int npcZ = npc.getZ();
				int posX = npcX;
				int posY = npcY;
				final int posZ = npcZ + 30;
				if (originalAttackTarget.getX() < posX)
				{
					posX += 300;
				}
				else
				{
					posX -= 300;
				}
				
				if (originalAttackTarget.getY() < posY)
				{
					posY += 300;
				}
				else
				{
					posY -= 300;
				}
				
				if (GeoEngine.getInstance().canMoveToTarget(npcX, npcY, npcZ, posX, posY, posZ, npc.getInstanceWorld()))
				{
					setIntentionMoveTo(new Location(posX, posY, posZ, 0));
				}
				return;
			}
		}
		
		final double dist = npc.calculateDistance2D(originalAttackTarget);
		final int dist2 = (int) dist - collision;
		int range = npc.getPhysicalAttackRange() + combinedCollision;
		final boolean targetIsMoving = originalAttackTarget.isMoving();
		
		if (targetIsMoving)
		{
			range += 50;
			if (npc.isMoving())
			{
				range += 50;
			}
		}
		
		if ((dist2 > range) || !GeoEngine.getInstance().canSeeTarget(npc, originalAttackTarget))
		{
			if (targetIsMoving)
			{
				range -= 100;
			}
			
			if (range < 5)
			{
				range = 5;
			}
			
			moveToPawn(originalAttackTarget, range);
			return;
		}
		
		_actor.doAutoAttack(originalAttackTarget);
	}
	
	/**
	 * Handles the logic for performing a skill cast.<br>
	 * It checks if the target is still valid.<br>
	 * It moves the actor to the target if necessary.<br>
	 * Finally, it executes the {@code doCast} action.
	 */
	@Override
	protected void thinkCast()
	{
		final WorldObject target = getCastTarget();
		if (checkTargetLost(target))
		{
			setCastTarget(null);
			setTarget(null);
			return;
		}
		
		if (maybeMoveToPawn(target, _actor.getMagicalAttackRange(_skill)))
		{
			return;
		}
		
		_actor.doCast(_skill, _item, _forceUse, _dontMove);
	}
}
