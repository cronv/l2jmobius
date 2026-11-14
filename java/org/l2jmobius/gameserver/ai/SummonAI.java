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

import java.util.List;
import java.util.concurrent.Future;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.enums.npc.AISkillScope;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;

/**
 * Handles the artificial intelligence logic for {@code Summon} entities.<br>
 * It manages behaviors such as attacking targets and following their owners.<br>
 * This class implements {@code Runnable} to allow AI processing on separate threads.
 */
public class SummonAI extends PlayableAI implements Runnable
{
	private static final int AVOID_RADIUS = 70;
	
	private volatile boolean _thinking; // To prevent recursive thinking.
	private volatile boolean _startFollow = _actor.asSummon().getFollowStatus();
	private Creature _lastAttack = null;
	
	private volatile boolean _startAvoid;
	private volatile boolean _isDefending;
	private Future<?> _avoidTask = null;
	
	// Fix: Infinite Atk. Spd. exploit.
	private Creature _savedAttackTarget = null;
	
	/**
	 * Determines the next action for the summon.<br>
	 * It checks if there is a saved attack target.<br>
	 * If a target exists, it returns {@code Intention.ATTACK}.<br>
	 * Otherwise, it returns {@code null}.
	 * @return The next {@code Intention} or {@code null} if no action is pending.
	 */
	@Override
	public Intention getNextIntention()
	{
		return _savedAttackTarget != null ? Intention.ATTACK : null;
	}
	
	/**
	 * Creates a new {@code SummonAI} instance for a specific summon.<br>
	 * This constructor initializes the AI behavior for the provided {@code Summon}.
	 * @param summon The {@code Summon} entity that will use this AI.
	 */
	public SummonAI(Summon summon)
	{
		super(summon);
	}
	
	/**
	 * Sets the AI intention to an idle state.<br>
	 * This method stops any active avoidance or following tasks.<br>
	 * It resets the follow status and switches the current intention to {@code active}.
	 */
	@Override
	public void setIntentionIdle()
	{
		stopAvoidTask();
		stopFollow();
		_startFollow = false;
		setIntentionActive();
	}
	
	/**
	 * Sets the AI intention to an active state.<br>
	 * This method starts the avoidance task.<br>
	 * It checks if the summon should follow its owner.<br>
	 * If it follows, it sets the follow intention; otherwise, it calls the superclass method.
	 */
	@Override
	public synchronized void setIntentionActive()
	{
		startAvoidTask();
		final Summon summon = _actor.asSummon();
		if (_startFollow)
		{
			setIntentionFollow(summon.getOwner());
		}
		else
		{
			super.setIntentionActive();
		}
	}
	
	/**
	 * Sets the current intention to follow a specific target.<br>
	 * This method starts the avoidance task and updates the parent state.
	 * @param target The {@code WorldObject} that the actor should follow.
	 */
	@Override
	public synchronized void setIntentionFollow(WorldObject target)
	{
		startAvoidTask();
		super.setIntentionFollow(target);
	}
	
	/**
	 * Sets the current intention of the AI to attack a specific target.<br>
	 * This method stops any active avoidance tasks before updating the state.
	 * @param target The {@code WorldObject} that the AI will focus on attacking.
	 */
	@Override
	public synchronized void setIntentionAttack(WorldObject target)
	{
		stopAvoidTask();
		super.setIntentionAttack(target);
	}
	
	/**
	 * Sets the current intention of the AI to move to a specific location.<br>
	 * This method stops any active avoidance tasks before updating the destination.
	 * @param destination The {@code ILocational} target where the AI should move.
	 */
	@Override
	public synchronized void setIntentionMoveTo(ILocational destination)
	{
		stopAvoidTask();
		super.setIntentionMoveTo(destination);
	}
	
	/**
	 * Sets the current intention of the summon to a resting state.<br>
	 * This method stops any active avoidance tasks.<br>
	 * It also calls the {@code setIntentionRest()} method of the parent class.
	 */
	@Override
	public synchronized void setIntentionRest()
	{
		stopAvoidTask();
		super.setIntentionRest();
	}
	
	/**
	 * Sets the current intention to pick up a specific item.<br>
	 * This method stops any active avoidance tasks before updating the state.
	 * @param item The {@code WorldObject} that represents the item to be picked up.
	 */
	@Override
	public synchronized void setIntentionPickUp(WorldObject item)
	{
		stopAvoidTask();
		super.setIntentionPickUp(item);
	}
	
	/**
	 * Sets the current intention of the AI to interact with a specific object.<br>
	 * This method stops any active avoidance tasks before updating the state.
	 * @param object The {@code WorldObject} that the AI will interact with.
	 */
	@Override
	public synchronized void setIntentionInteract(WorldObject object)
	{
		stopAvoidTask();
		super.setIntentionInteract(object);
	}
	
	/**
	 * Handles the logic for performing an attack on a target.<br>
	 * It checks if the target is valid and reachable.<br>
	 * If the actor is already attacking, it saves the current target.<br>
	 * Otherwise, it triggers the auto-attack behavior.
	 */
	private void thinkAttack()
	{
		final WorldObject target = getTarget();
		final Creature attackTarget = target == null ? null : target.asCreature();
		if (checkTargetLostOrDead(attackTarget))
		{
			setTarget(null);
			if (_startFollow)
			{
				_actor.asSummon().setFollowStatus(true);
			}
			return;
		}
		
		if (maybeMoveToPawn(attackTarget, _actor.getPhysicalAttackRange()))
		{
			return;
		}
		
		clientStopMoving(null);
		
		// Fix: Infinite Atk. Spd. exploit.
		if (_actor.isAttackingNow())
		{
			_savedAttackTarget = attackTarget;
			return;
		}
		
		_actor.doAutoAttack(attackTarget);
	}
	
	/**
	 * Handles the logic for casting a skill.<br>
	 * It checks if the summon is already casting or has lost its target.<br>
	 * It determines if the summon needs to move closer to the target before casting.<br>
	 * If all conditions are met, it triggers the cast action.
	 */
	private void thinkCast()
	{
		final Summon summon = _actor.asSummon();
		if (summon.isCastingNow(SkillCaster::isAnyNormalType))
		{
			return;
		}
		
		final WorldObject target = getCastTarget();
		if (checkTargetLost(target))
		{
			setTarget(null);
			setCastTarget(null);
			summon.setFollowStatus(true);
			return;
		}
		
		final boolean val = _startFollow;
		if (maybeMoveToPawn(target, _actor.getMagicalAttackRange(_skill)))
		{
			return;
		}
		
		summon.setFollowStatus(false);
		setIntentionIdle();
		_startFollow = val;
		_actor.doCast(_skill, _item, _skill.hasNegativeEffect(), _dontMove);
	}
	
	/**
	 * Handles the logic for picking up an item.<br>
	 * It checks if the target is still valid.<br>
	 * It attempts to move toward the target before performing the action.<br>
	 * If successful, it sets the intention to idle and triggers the pickup.
	 */
	private void thinkPickUp()
	{
		final WorldObject target = getTarget();
		if (checkTargetLost(target))
		{
			return;
		}
		
		if (maybeMoveToPawn(target, 36))
		{
			return;
		}
		
		setIntentionIdle();
		getActor().doPickupItem(target);
	}
	
	/**
	 * Processes the logic for interacting with a target object.<br>
	 * It checks if the current target is still valid.<br>
	 * If the summon moves toward the target, it stops further processing.<br>
	 * Otherwise, it sets the intention to idle.
	 */
	private void thinkInteract()
	{
		final WorldObject target = getTarget();
		if (checkTargetLost(target))
		{
			return;
		}
		
		if (maybeMoveToPawn(target, 36))
		{
			return;
		}
		
		setIntentionIdle();
	}
	
	/**
	 * Triggers the AI logic to determine the next action.<br>
	 * This method checks if the {@code _actor} is available for thinking.<br>
	 * It executes the appropriate internal think method based on the current intention.
	 */
	@Override
	public void notifyActionThink()
	{
		if (_thinking || _actor.isCastingNow() || _actor.isAllSkillsDisabled())
		{
			return;
		}
		
		_thinking = true;
		try
		{
			switch (getIntention())
			{
				case ATTACK:
				{
					thinkAttack();
					break;
				}
				case CAST:
				{
					thinkCast();
					break;
				}
				case PICK_UP:
				{
					thinkPickUp();
					break;
				}
				case INTERACT:
				{
					thinkInteract();
					break;
				}
			}
		}
		finally
		{
			_thinking = false;
		}
	}
	
	/**
	 * This method is called when a casting action finishes.<br>
	 * It updates the summon's behavior based on its previous state.<br>
	 * If no target was attacked, it restores the initial follow status.<br>
	 * Otherwise, it sets the intention to attack the last target.
	 */
	@Override
	public void notifyActionFinishCasting()
	{
		if (_lastAttack == null)
		{
			_actor.asSummon().setFollowStatus(_startFollow);
		}
		else
		{
			setIntentionAttack(_lastAttack);
			_lastAttack = null;
		}
	}
	
	/**
	 * Notifies the AI that it has been attacked by a specific object.<br>
	 * This method triggers defensive behaviors based on the current state.<br>
	 * It calls {@code super.notifyActionAttacked(attacker)} if the attacker is not {@code null}.
	 * @param attacker The {@code WorldObject} that performed the attack.
	 */
	@Override
	public void notifyActionAttacked(WorldObject attacker)
	{
		if (attacker == null)
		{
			return;
		}
		
		super.notifyActionAttacked(attacker);
		
		if (_isDefending)
		{
			allServitorsDefend(attacker.asCreature());
		}
		else
		{
			avoidAttack(attacker.asCreature());
		}
	}
	
	/**
	 * This method is called when an attack against the summon is successfully evaded.<br>
	 * It triggers defensive behaviors based on the current state of the AI.<br>
	 * If the summon is defending, all servitors will also defend.<br>
	 * Otherwise, the summon will attempt to avoid the attack.
	 * @param attacker The {@code WorldObject} that attempted the attack.
	 */
	@Override
	public void notifyActionEvaded(WorldObject attacker)
	{
		if (attacker == null)
		{
			return;
		}
		
		super.notifyActionEvaded(attacker);
		
		if (_isDefending)
		{
			allServitorsDefend(attacker.asCreature());
		}
		else
		{
			avoidAttack(attacker.asCreature());
		}
	}
	
	/**
	 * Coordinates the defense actions for all relevant summons.<br>
	 * It checks if the owner has any servitors to defend.<br>
	 * If servitors exist, it triggers their defense logic.<br>
	 * Otherwise, it triggers the defense logic for this summon.
	 * @param attacker The {@code Creature} that initiated the attack.
	 */
	private void allServitorsDefend(Creature attacker)
	{
		final Creature owner = getActor().getOwner();
		final Player ownerPlayer = owner == null ? null : owner.asPlayer();
		if ((ownerPlayer != null) && ownerPlayer.hasServitors())
		{
			for (Summon summon : ownerPlayer.getServitors().values())
			{
				final SummonAI ai = (SummonAI) summon.getAI();
				if (ai.isDefending())
				{
					ai.defendAttack(attacker);
				}
			}
		}
		else
		{
			defendAttack(attacker);
		}
	}
	
	/**
	 * Handles the logic for avoiding an attack from a specific creature.<br>
	 * This method checks if the summon should start its avoidance behavior.<br>
	 * It prevents movement while the actor is currently casting a skill.<br>
	 * It sets {@code _startAvoid} to {@code true} if the owner is nearby.
	 * @param attacker The {@code Creature} that is performing the attack.
	 */
	private void avoidAttack(Creature attacker)
	{
		// Don't move while casting. It breaks casting animation, but still casts the skill... looks so bugged.
		if (_actor.isCastingNow())
		{
			return;
		}
		
		final Creature owner = getActor().getOwner();
		
		// Trying to avoid if summon near owner.
		if ((owner != null) && (owner != attacker) && owner.isInsideRadius3D(_actor, 2 * AVOID_RADIUS))
		{
			_startAvoid = true;
		}
	}
	
	/**
	 * This method handles the defense logic when a {@code Creature} attacks.<br>
	 * It checks if the actor is currently busy attacking or casting.<br>
	 * If free, it determines whether to follow the owner or attack the threat.
	 * @param attacker The {@code Creature} that initiated the attack.
	 */
	public void defendAttack(Creature attacker)
	{
		// Cannot defend while attacking or casting.
		if (_actor.isAttackingNow() || _actor.isCastingNow())
		{
			return;
		}
		
		final Summon summon = getActor();
		final Player owner = summon.getOwner();
		if (owner != null)
		{
			if (summon.calculateDistance3D(owner) > 3000)
			{
				summon.getAI().setIntentionFollow(owner);
			}
			else if ((owner != attacker) && !summon.isMoving() && summon.canAttack(attacker, false))
			{
				if (summon.isPet())
				{
					summon.doAttack(attacker);
				}
				else // Fix for attacking with skills when servitors are in defensive mode.
				{
					final List<Skill> skills = summon.getTemplate().getAISkills(AISkillScope.ATTACK);
					if (skills.isEmpty())
					{
						summon.doAttack(attacker);
					}
					else
					{
						summon.setTarget(attacker);
						summon.useMagic(skills.get(0), null, true, false);
					}
				}
			}
		}
	}
	
	@Override
	public void run()
	{
		if (_startAvoid)
		{
			_startAvoid = false;
			if (!_actor.isMoving() && !_actor.isDead() && !_actor.isMovementDisabled() && (_actor.getMoveSpeed() > 0))
			{
				final Player owner = _actor.asSummon().getOwner();
				final int ownerX = owner.getX();
				final int ownerY = owner.getY();
				final int actorX = _actor.getX();
				final int actorY = _actor.getY();
				final int actorZ = _actor.getZ();
				final double angle = Math.toRadians(Rnd.get(-90, 90)) + Math.atan2(ownerY - actorY, ownerX - actorX);
				final int targetX = ownerX + (int) (AVOID_RADIUS * Math.cos(angle));
				final int targetY = ownerY + (int) (AVOID_RADIUS * Math.sin(angle));
				if (GeoEngine.getInstance().canMoveToTarget(actorX, actorY, actorZ, targetX, targetY, actorZ, _actor.getInstanceWorld()))
				{
					moveTo(targetX, targetY, actorZ);
				}
			}
		}
	}
	
	/**
	 * Updates the internal follow status of the summon.<br>
	 * This method toggles the {@code _startFollow} boolean value.<br>
	 * It then updates the actual follow status on the {@code Summon} object based on the current intention.
	 */
	public void notifyFollowStatusChange()
	{
		_startFollow = !_startFollow;
		switch (getIntention())
		{
			case ACTIVE:
			case FOLLOW:
			case IDLE:
			case MOVE_TO:
			case PICK_UP:
			{
				_actor.asSummon().setFollowStatus(_startFollow);
				break;
			}
		}
	}
	
	/**
	 * Sets whether the summon should start following its owner.<br>
	 * This updates the internal {@code _startFollow} flag.
	 * @param value The new follow status to set.
	 */
	public void setStartFollowController(boolean value)
	{
		_startFollow = value;
	}
	
	/**
	 * Sets the current intention of the AI to cast a specific skill.<br>
	 * This method updates the internal state and handles necessary task cancellations.
	 * @param skill The {@code Skill} object to be cast.
	 * @param target The {@code WorldObject} that is the target of the skill.
	 * @param item The {@code Item} used for the casting action, if any.
	 * @param forceUse A boolean indicating if the use should be forced.
	 * @param dontMove A boolean indicating if the actor should remain stationary during the cast.
	 */
	@Override
	public synchronized void setIntentionCast(Skill skill, WorldObject target, Item item, boolean forceUse, boolean dontMove)
	{
		stopAvoidTask();
		if (getIntention() == Intention.ATTACK)
		{
			final WorldObject currentTarget = getTarget();
			_lastAttack = (currentTarget != null) && currentTarget.isCreature() ? currentTarget.asCreature() : null;
		}
		else
		{
			_lastAttack = null;
		}
		
		super.setIntentionCast(skill, target, item, forceUse, dontMove);
	}
	
	/**
	 * Starts a background task to handle avoidance logic.<br>
	 * This method schedules the {@code Runnable} at a fixed rate of 100ms.<br>
	 * It ensures that only one {@code Future} is created for this purpose.
	 */
	private void startAvoidTask()
	{
		if (_avoidTask == null)
		{
			_avoidTask = ThreadPool.scheduleAtFixedRate(this, 100, 100);
		}
	}
	
	/**
	 * Stops the current avoidance task if one is running.<br>
	 * It cancels the {@code Future} and sets the reference to {@code null}.
	 */
	private void stopAvoidTask()
	{
		if (_avoidTask != null)
		{
			_avoidTask.cancel(false);
			_avoidTask = null;
		}
	}
	
	/**
	 * Stops the current AI task for this entity.<br>
	 * This method also cancels any active avoidance tasks.
	 */
	@Override
	public void stopAITask()
	{
		stopAvoidTask();
		super.stopAITask();
	}
	
	/**
	 * Retrieves the {@code Summon} associated with this AI.<br>
	 * It casts the underlying actor to a {@code Summon} object.
	 * @return The {@code Summon} instance.
	 */
	@Override
	public Summon getActor()
	{
		return super.getActor().asSummon();
	}
	
	/**
	 * Checks if the summon is currently in a defending state.
	 * @return {@code true} if the summon is defending, {@code false} otherwise.
	 */
	public boolean isDefending()
	{
		return _isDefending;
	}
	
	/**
	 * Updates the defense status of the summon.<br>
	 * This method sets the {@code _isDefending} flag to the provided value.
	 * @param isDefending The new defense state to set.
	 */
	public void setDefending(boolean isDefending)
	{
		_isDefending = isDefending;
	}
}
