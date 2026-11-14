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
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.instance.Doppelganger;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

/**
 * Handles the artificial intelligence logic for {@code Doppelganger} entities.<br>
 * This class defines how these creatures behave and interact with the world.<br>
 * It extends {@code CreatureAI} to provide specific behaviors for this mob type.
 */
public class DoppelgangerAI extends CreatureAI
{
	private volatile boolean _thinking; // To prevent recursive thinking.
	private volatile boolean _startFollow;
	private Creature _lastAttack = null;
	
	/**
	 * Creates a new {@code DoppelgangerAI} instance.<br>
	 * This constructor initializes the AI using an existing {@code Doppelganger} object.
	 * @param clone The {@code Doppelganger} entity to associate with this AI.
	 */
	public DoppelgangerAI(Doppelganger clone)
	{
		super(clone);
	}
	
	/**
	 * Sets the AI state to idle.<br>
	 * This method stops any current following behavior.<br>
	 * It resets the {@code _startFollow} flag to {@code false}.<br>
	 * It then switches the intention to active.
	 */
	@Override
	public void setIntentionIdle()
	{
		stopFollow();
		_startFollow = false;
		setIntentionActive();
	}
	
	/**
	 * Sets the AI intention to an active state.<br>
	 * This method determines if the actor should follow its summoner or perform a default action.<br>
	 * It checks the {@code _startFollow} flag before deciding which behavior to trigger.
	 */
	@Override
	public void setIntentionActive()
	{
		if (_startFollow)
		{
			setIntentionFollow(getActor().getSummoner());
		}
		else
		{
			super.setIntentionActive();
		}
	}
	
	/**
	 * Handles the logic for initiating an attack on a target.<br>
	 * It checks if the current target is valid and alive.<br>
	 * The method moves the actor into range or triggers {@code doAutoAttack}.
	 */
	private void thinkAttack()
	{
		final WorldObject target = getTarget();
		final Creature attackTarget = target == null ? null : target.asCreature();
		if (checkTargetLostOrDead(attackTarget))
		{
			setTarget(null);
			return;
		}
		
		if (maybeMoveToPawn(target, _actor.getPhysicalAttackRange()))
		{
			return;
		}
		
		clientStopMoving(null);
		_actor.doAutoAttack(attackTarget);
	}
	
	/**
	 * Handles the logic for casting a skill.<br>
	 * It checks if the actor is already casting.<br>
	 * It validates the current target and moves the actor to the correct range.<br>
	 * Finally, it executes the cast action.
	 */
	private void thinkCast()
	{
		if (_actor.isCastingNow(SkillCaster::isAnyNormalType))
		{
			return;
		}
		
		final WorldObject target = getCastTarget();
		if (checkTargetLost(target))
		{
			setCastTarget(null);
			setTarget(null);
			return;
		}
		
		final boolean val = _startFollow;
		if (maybeMoveToPawn(target, _actor.getMagicalAttackRange(_skill)))
		{
			return;
		}
		
		getActor().followSummoner(false);
		setIntentionIdle();
		_startFollow = val;
		_actor.doCast(_skill, _item, _forceUse, _dontMove);
	}
	
	/**
	 * Handles the logic for interacting with a target.<br>
	 * It checks if the current target is still valid.<br>
	 * It determines if the actor should move toward the target.<br>
	 * If no action is taken, it sets the intention to idle.
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
	 * This method triggers the AI thinking process for the {@code Doppelganger}.<br>
	 * It checks if the actor is currently busy before executing a new action.<br>
	 * The logic selects an action based on the current intention.<br>
	 * It handles {@code ATTACK}, {@code CAST}, and {@code INTERACT} behaviors.
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
	 * It determines the next behavior for the {@code Doppelganger}.<br>
	 * If no previous attack exists, it follows the summoner.<br>
	 * Otherwise, it switches back to attacking the last target.
	 */
	@Override
	public void notifyActionFinishCasting()
	{
		if (_lastAttack == null)
		{
			getActor().followSummoner(_startFollow);
		}
		else
		{
			setIntentionAttack(_lastAttack);
			_lastAttack = null;
		}
	}
	
	/**
	 * Toggles the follow status of the actor.<br>
	 * This method flips the value of {@code _startFollow}.<br>
	 * It then updates the summoner's behavior based on the current intention.
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
				getActor().followSummoner(_startFollow);
				break;
			}
		}
	}
	
	/**
	 * Sets whether the AI should start following a target.<br>
	 * This updates the {@code _startFollow} flag.
	 * @param value The new follow status to set.
	 */
	public void setStartFollowController(boolean value)
	{
		_startFollow = value;
	}
	
	/**
	 * Sets the current intention of the AI to casting a specific skill.<br>
	 * This method updates the internal state for casting actions.<br>
	 * It also handles tracking the last attacked creature if the previous intention was {@code ATTACK}.
	 * @param skill The {@code Skill} to be cast.
	 * @param target The {@code WorldObject} that is being targeted by the skill.
	 * @param item The {@code Item} involved in the casting action, or {@code null} if none.
	 * @param forceUse Whether to force the use of the skill.
	 * @param dontMove Whether the AI should remain stationary while casting.
	 */
	@Override
	public void setIntentionCast(Skill skill, WorldObject target, Item item, boolean forceUse, boolean dontMove)
	{
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
	 * Moves the actor toward a specific {@code WorldObject}.<br>
	 * This method calculates a random position near the target based on an offset.<br>
	 * It ensures the actor is capable of movement before updating its path.
	 * @param pawn The {@code WorldObject} to move towards.
	 * @param offsetValue The maximum distance from the target to allow for randomization.
	 */
	@Override
	public void moveToPawn(WorldObject pawn, int offsetValue)
	{
		// Check if actor can move.
		if (!_actor.isMovementDisabled() && (_actor.getMoveSpeed() > 0))
		{
			int offset = offsetValue;
			if (offset < 10)
			{
				offset = 10;
			}
			
			// Prevent possible extra calls to this function (there is none?), also don't send movetopawn packets too often.
			boolean sendPacket = true;
			final boolean actorMoving = _actor.isMoving();
			if (actorMoving && (getTarget() == pawn))
			{
				if (_clientMovingToPawnOffset == offset)
				{
					if (GameTimeTaskManager.getInstance().getGameTicks() < _moveToPawnTimeout)
					{
						return;
					}
					
					sendPacket = false;
				}
				else if (_actor.isOnGeodataPath())
				{
					// Minimum time to calculate new route is 2 seconds.
					if (GameTimeTaskManager.getInstance().getGameTicks() < (_moveToPawnTimeout + 10))
					{
						return;
					}
				}
			}
			
			// Set AI movement data.
			_clientMovingToPawnOffset = offset;
			setTarget(pawn);
			_moveToPawnTimeout = GameTimeTaskManager.getInstance().getGameTicks();
			_moveToPawnTimeout += 1000 / GameTimeTaskManager.MILLIS_IN_TICK;
			if (pawn == null)
			{
				return;
			}
			
			// Calculate movement data for a move to location action and add the actor to movingObjects of GameTimeTaskManager.
			// _actor.moveToLocation(pawn.getX(), pawn.getY(), pawn.getZ(), offset);
			final Location loc = new Location(pawn.getX() + Rnd.get(-offset, offset), pawn.getY() + Rnd.get(-offset, offset), pawn.getZ());
			_actor.moveToLocation(loc.getX(), loc.getY(), loc.getZ(), 0);
			if (!actorMoving)
			{
				clientActionFailed();
				return;
			}
			
			// Doppelgangers always send MoveToLocation packet.
			if (sendPacket)
			{
				_actor.broadcastMoveToLocation();
			}
		}
		else
		{
			clientActionFailed();
		}
	}
	
	/**
	 * Retrieves the {@code Doppelganger} associated with this AI.<br>
	 * This method casts the base actor to a {@code Doppelganger} type.
	 * @return The {@code Doppelganger} instance.
	 */
	@Override
	public Doppelganger getActor()
	{
		return (Doppelganger) super.getActor();
	}
}
