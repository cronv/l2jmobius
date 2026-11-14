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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.WorldRegion;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.actor.transform.Transform;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcMoveFinished;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.AutoAttackStop;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;
import org.l2jmobius.gameserver.taskmanagers.WalkingTaskManager;

/**
 * This class manages the artificial intelligence for {@code Creature} entities.<br>
 * It serves as a base for various behaviors such as {@code AttackableAI}, {@code DoorAI}, {@code PlayerAI}, and {@code SummonAI}.
 */
public class CreatureAI extends AbstractAI
{
	private OnNpcMoveFinished _onNpcMoveFinished = null;
	
	/**
	 * Creates a new {@code CreatureAI} instance for a specific creature.<br>
	 * This constructor links the AI logic to the provided {@code Creature}.
	 * @param creature The {@code Creature} that will use this AI.
	 */
	public CreatureAI(Creature creature)
	{
		super(creature);
	}
	
	/**
	 * Notifies the AI that it has been attacked by another object.<br>
	 * This method triggers the auto-attack logic for the actor.<br>
	 * It also checks if a specific action should be performed as a result of being attacked.
	 * @param attacker The {@code WorldObject} that initiated the attack.
	 */
	@Override
	public void notifyActionAttacked(WorldObject attacker)
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		clientStartAutoAttack();
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.ATTACKED))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * Sets the AI intention to {@code IDLE}.<br>
	 * This method stops following, casting, and attacking behaviors.<br>
	 * It also clears any pending actions and stops movement on both the server and client sides.
	 */
	@Override
	public void setIntentionIdle()
	{
		// Stop the follow mode (IDLE is neither FOLLOW nor ATTACK).
		stopFollow();
		
		// Set the AI Intention to IDLE.
		_intention = Intention.IDLE;
		
		// Init cast target.
		setCastTarget(null);
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast).
		clientStopMoving(null);
		
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast).
		clientStopAutoAttack();
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isRemovedBy(Intention.IDLE))
		{
			setNextAction(null);
		}
	}
	
	/**
	 * Sets the AI intention to {@code ACTIVE}.<br>
	 * This method stops any current follow behavior.<br>
	 * It also clears the cast target and stops movement on both the server and client sides.<br>
	 * Finally, it triggers the think action for the actor.
	 */
	@Override
	public void setIntentionActive()
	{
		try
		{
			// Stop the follow mode (ACTIVE is neither FOLLOW nor ATTACK).
			stopFollow();
			
			// Check if the Intention is not already Active.
			if (getIntention() == Intention.ACTIVE)
			{
				return;
			}
			
			// Set the AI Intention to ACTIVE.
			_intention = Intention.ACTIVE;
			
			// Check if region and its neighbors are active.
			final WorldRegion region = _actor.getWorldRegion();
			if ((region == null) || !region.areNeighborsActive())
			{
				return;
			}
			
			// Init cast target.
			setCastTarget(null);
			
			// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast).
			clientStopMoving(null);
			
			// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast).
			clientStopAutoAttack();
			
			// Launch the Think Action.
			notifyActionThink();
		}
		finally
		{
			final NextAction nextAction = getNextAction();
			if ((nextAction != null) && nextAction.isRemovedBy(Intention.ACTIVE))
			{
				setNextAction(null);
			}
		}
	}
	
	/**
	 * Sets the AI intention to a resting state.<br>
	 * This method calls {@code setIntentionIdle()} internally.<br>
	 * It also removes any pending actions that are incompatible with the {@code REST} intention.
	 */
	@Override
	public void setIntentionRest()
	{
		// Set the AI Intention to IDLE.
		setIntentionIdle();
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isRemovedBy(Intention.REST))
		{
			setNextAction(null);
		}
	}
	
	/**
	 * Sets the AI intention to attack a specific target.<br>
	 * This method validates if the {@code target} is valid and if the actor can perform actions.<br>
	 * It updates the current intention to {@code Intention.ATTACK} and triggers the think action.
	 * @param target The {@code WorldObject} that the AI will attack.
	 */
	@Override
	public void setIntentionAttack(WorldObject target)
	{
		try
		{
			if ((target == null) || !target.isTargetable())
			{
				clientActionFailed();
				return;
			}
			
			final Intention intention = getIntention();
			if (intention == Intention.REST)
			{
				// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor.
				clientActionFailed();
				return;
			}
			
			if (_actor.isAllSkillsDisabled() || _actor.isCastingNow() || _actor.isControlBlocked())
			{
				// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor.
				clientActionFailed();
				return;
			}
			
			// Check if the Intention is already ATTACK.
			if (intention == Intention.ATTACK)
			{
				// Check if the AI already targets the Creature.
				if (getTarget() != target)
				{
					// Set the AI attack target (change target).
					setTarget(target);
					
					// Launch the Think Action.
					notifyActionThink();
				}
				else if (_actor.isAttackingNow())
				{
					// A repeated attack during a swing must not restart it, the pending hit would be replaced and never land.
					// The running attack notifies the AI when it finishes.
					clientActionFailed();
				}
				else
				{
					// A repeated attack on the same target revalidates a move that ended without arriving.
					// Sending only ActionFailed leaves the intention waiting on an event that never comes.
					stopFollow();
					notifyActionThink();
				}
			}
			else
			{
				// Set the Intention of this AbstractAI to ATTACK.
				_intention = Intention.ATTACK;
				
				// Set the AI attack target.
				setTarget(target);
				
				// Launch the Think Action.
				notifyActionThink();
			}
		}
		finally
		{
			final NextAction nextAction = getNextAction();
			if ((nextAction != null) && nextAction.isRemovedBy(Intention.ATTACK))
			{
				setNextAction(null);
			}
		}
	}
	
	/**
	 * Sets the current intention of the actor to cast a specific skill.<br>
	 * This method handles logic for interrupting attacks and checking magic requirements.<br>
	 * It updates the internal state to reflect that the actor is now casting.
	 * @param skill The {@code Skill} that the actor intends to cast.
	 * @param target The {@code WorldObject} that the skill will be directed towards.
	 * @param item The {@code Item} required for the skill, if any.
	 * @param forceUse A boolean indicating whether to force the use of the skill.
	 * @param dontMove A boolean indicating whether the actor should remain stationary while casting.
	 */
	@Override
	public void setIntentionCast(Skill skill, WorldObject target, Item item, boolean forceUse, boolean dontMove)
	{
		try
		{
			// Stop the follow mode (CAST is neither FOLLOW nor ATTACK).
			stopFollow();
			
			if ((getIntention() == Intention.REST) && skill.isMagic())
			{
				clientActionFailed();
				return;
			}
			
			final long currentTime = System.nanoTime();
			final long attackEndTime = _actor.getAttackEndTime();
			if (attackEndTime > currentTime)
			{
				ThreadPool.schedule(() ->
				{
					if (_actor.isAttackingNow())
					{
						_actor.abortAttack();
					}
					
					changeIntentionToCast(skill, target, item, forceUse, dontMove);
				}, TimeUnit.NANOSECONDS.toMillis(attackEndTime - currentTime));
			}
			else
			{
				changeIntentionToCast(skill, target, item, forceUse, dontMove);
			}
		}
		finally
		{
			final NextAction nextAction = getNextAction();
			if ((nextAction != null) && nextAction.isRemovedBy(Intention.CAST))
			{
				setNextAction(null);
			}
		}
	}
	
	/**
	 * Updates the AI state to a casting intention.<br>
	 * This method sets all necessary parameters for the cast action.<br>
	 * It then triggers the next think action for the AI.
	 * @param skill The {@code Skill} being used for the cast.
	 * @param target The {@code WorldObject} that is being targeted.
	 * @param item The {@code Item} associated with this skill, if any.
	 * @param forceUse Whether the {@code true} value indicates a forced use of the skill.
	 * @param dontMove Whether the {@code true} value prevents the AI from moving during the cast.
	 */
	protected void changeIntentionToCast(Skill skill, WorldObject target, Item item, boolean forceUse, boolean dontMove)
	{
		// Set the AI cast target.
		setCastTarget(target);
		
		// Set the AI skill used by INTENTION_CAST.
		_skill = skill;
		
		// Set the AI item that triggered this skill.
		_item = item;
		
		// Set the ctrl/shift pressed parameters.
		_forceUse = forceUse;
		_dontMove = dontMove;
		
		// Change the Intention of this AbstractAI to CAST.
		_intention = Intention.CAST;
		
		// Launch the Think Action.
		notifyActionThink();
	}
	
	/**
	 * Sets the AI intention to move to a specific location.<br>
	 * This method stops any current following behavior and cancels active attacks.<br>
	 * It moves the actor to the coordinates provided by the {@code ILocational} object.
	 * @param loc The destination location for the movement.
	 */
	@Override
	public void setIntentionMoveTo(ILocational loc)
	{
		try
		{
			// Stop the follow mode (MOVE_TO is neither FOLLOW nor ATTACK).
			stopFollow();
			
			if (getIntention() == Intention.REST)
			{
				// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor.
				clientActionFailed();
				return;
			}
			
			if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
			{
				// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor.
				clientActionFailed();
				return;
			}
			
			// Set the Intention of this AbstractAI to MOVE_TO.
			_intention = Intention.MOVE_TO;
			
			// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast).
			clientStopAutoAttack();
			
			// Abort the attack of the Creature and send Server->Client ActionFailed packet.
			_actor.abortAttack();
			
			// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation (broadcast).
			moveTo(loc.getX(), loc.getY(), loc.getZ());
		}
		finally
		{
			final NextAction nextAction = getNextAction();
			if ((nextAction != null) && nextAction.isRemovedBy(Intention.MOVE_TO))
			{
				setNextAction(null);
			}
		}
	}
	
	/**
	 * Sets the AI intention to follow a specific target.<br>
	 * This method checks if the actor is capable of moving and not currently busy.<br>
	 * It stops auto-attack and starts a periodic follow task for the {@code target}.
	 * @param target The {@code WorldObject} that the actor should follow.
	 */
	@Override
	public void setIntentionFollow(WorldObject target)
	{
		try
		{
			if (getIntention() == Intention.REST)
			{
				// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor.
				clientActionFailed();
				return;
			}
			
			if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
			{
				// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor.
				clientActionFailed();
				return;
			}
			
			if (_actor.isMovementDisabled() || (_actor.getMoveSpeed() <= 0))
			{
				// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor.
				clientActionFailed();
				return;
			}
			
			// Dead actors can't follow.
			if (_actor.isDead())
			{
				clientActionFailed();
				return;
			}
			
			// Do not follow yourself.
			if (_actor == target)
			{
				clientActionFailed();
				return;
			}
			
			// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast).
			clientStopAutoAttack();
			
			// Set the Intention of this AbstractAI to FOLLOW.
			_intention = Intention.FOLLOW;
			
			// Create and Launch an AI Follow Task to execute every 1s.
			startFollow(target.asCreature());
		}
		finally
		{
			final NextAction nextAction = getNextAction();
			if ((nextAction != null) && nextAction.isRemovedBy(Intention.FOLLOW))
			{
				setNextAction(null);
			}
		}
	}
	
	/**
	 * Sets the AI intention to pick up a specific object.<br>
	 * This method stops current following behavior and cancels auto-attacks.<br>
	 * It moves the actor toward the target object if it is an item.
	 * @param object The {@code WorldObject} that the actor should move to pick up.
	 */
	@Override
	public void setIntentionPickUp(WorldObject object)
	{
		try
		{
			// Stop the follow mode (PICK_UP is neither FOLLOW nor ATTACK).
			stopFollow();
			
			if (getIntention() == Intention.REST)
			{
				// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor.
				clientActionFailed();
				return;
			}
			
			if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
			{
				// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor.
				clientActionFailed();
				return;
			}
			
			// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast).
			clientStopAutoAttack();
			
			if (object.isItem() && (((Item) object).getItemLocation() != ItemLocation.VOID))
			{
				return;
			}
			
			// Set the Intention of this AbstractAI to PICK_UP.
			_intention = Intention.PICK_UP;
			
			// Set the AI pick up target.
			setTarget(object);
			
			if ((object.getX() == 0) && (object.getY() == 0))
			{
				// LOGGER.warning("Object in coords 0,0 - using a temporary fix");
				final Creature actor = getActor();
				object.setXYZ(actor.getX(), actor.getY(), actor.getZ() + 5);
			}
			
			// Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast).
			moveToPawn(object, 20);
		}
		finally
		{
			final NextAction nextAction = getNextAction();
			if ((nextAction != null) && nextAction.isRemovedBy(Intention.PICK_UP))
			{
				setNextAction(null);
			}
		}
	}
	
	/**
	 * Sets the AI intention to interact with a specific object.<br>
	 * This method stops any current following behavior and moves the actor toward the target.<br>
	 * It cancels the action if the actor is resting or has skills disabled.
	 * @param object The {@code WorldObject} that the actor will interact with.
	 */
	@Override
	public void setIntentionInteract(WorldObject object)
	{
		try
		{
			// Stop the follow mode (INTERACT is neither FOLLOW nor ATTACK).
			stopFollow();
			
			final Intention intention = getIntention();
			if (intention == Intention.REST)
			{
				// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor.
				clientActionFailed();
				return;
			}
			
			if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
			{
				// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor.
				clientActionFailed();
				return;
			}
			
			// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast).
			clientStopAutoAttack();
			
			if (intention != Intention.INTERACT)
			{
				// Set the Intention of this AbstractAI to INTERACT.
				_intention = Intention.INTERACT;
				
				// Set the AI interact target.
				setTarget(object);
				
				// Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast).
				moveToPawn(object, 60);
			}
		}
		finally
		{
			final NextAction nextAction = getNextAction();
			if ((nextAction != null) && nextAction.isRemovedBy(Intention.INTERACT))
			{
				setNextAction(null);
			}
		}
	}
	
	/**
	 * This method triggers the AI thinking process for the actor.<br>
	 * It checks if the actor is spawned and has an active AI.<br>
	 * If a new action triggered by {@code Action.THINK} exists, it executes that action.
	 */
	@Override
	public void notifyActionThink()
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.THINK))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * Notifies the AI that a target has triggered an aggression action.<br>
	 * This method checks if the next scheduled action is an aggression type.<br>
	 * If it is, the action will be executed immediately.
	 * @param target The {@code WorldObject} that caused the aggression.
	 * @param aggro The amount of aggression value added by this action.
	 */
	@Override
	public void notifyActionAggression(WorldObject target, int aggro)
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.AGGRESSION))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * Notifies the system that the actor's action has been blocked.<br>
	 * This method stops auto-attacks and movement for the {@code Creature}.<br>
	 * It also removes any active attack stance tasks from the manager.<br>
	 * If a blocked action is queued, it will be executed immediately.
	 */
	@Override
	public void notifyActionBlocked()
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast).
		_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(_actor))
		{
			AttackStanceTaskManager.getInstance().removeAttackStanceTask(_actor);
		}
		
		// Stop Server AutoAttack also.
		setAutoAttacking(false);
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast).
		clientStopMoving(null);
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.BLOCKED))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * Notifies the AI that the actor has been rooted.<br>
	 * This method stops movement on both the server and client sides.<br>
	 * It also triggers any actions specifically tied to the {@code ROOTED} state.
	 */
	@Override
	public void notifyActionRooted()
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast).
		clientStopMoving(null);
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.ROOTED))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * Notifies the AI that the actor has been confused.<br>
	 * This method stops all movement for the actor on both the server and client sides.<br>
	 * It also triggers any specific actions associated with the {@code CONFUSED} state.
	 */
	@Override
	public void notifyActionConfused()
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast).
		clientStopMoving(null);
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.CONFUSED))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * This method is called when the actor becomes muted.<br>
	 * It checks if the current action was triggered by a {@code MUTED} status.<br>
	 * If so, it executes that specific action.
	 */
	@Override
	public void notifyActionMuted()
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.MUTED))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * This method is called when an action performed by the actor was evaded.<br>
	 * It checks if the next scheduled action was triggered by an evasion event.<br>
	 * If so, it executes that specific action immediately.
	 * @param attacker The {@code WorldObject} that attempted to perform the action.
	 */
	@Override
	public void notifyActionEvaded(WorldObject attacker)
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.EVADED))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * Signals that the actor is ready to perform its next action.<br>
	 * This method checks if the actor can think and execute a new task.<br>
	 * It triggers {@code notifyActionThink()} if the actor is not currently casting.<br>
	 * If a valid action is found, it executes that action immediately.
	 */
	@Override
	public void notifyActionReadyToAct()
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		// Skip if the actor is casting (matches the prior dispatcher gate).
		if (!_actor.isCastingNow())
		{
			// Launch actions corresponding to the Action Think.
			notifyActionThink();
		}
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.READY_TO_ACT))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * This method is called when an actor reaches its destination.<br>
	 * It updates the actor's zone and checks if it should move to the next point.<br>
	 * If the movement is finished, it notifies the {@code WalkingManager} and triggers any relevant scripts.<br>
	 * Finally, it executes the next action if one was scheduled for arrival.
	 */
	@Override
	public void notifyActionArrived()
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		try
		{
			// Skip if the actor is casting (matches the prior dispatcher gate, e.g. from stopmove during cast).
			if (_actor.isCastingNow())
			{
				return;
			}
			
			getActor().revalidateZone(true);
			
			if (getActor().moveToNextRoutePoint())
			{
				return;
			}
			
			clientStoppedMoving();
			
			// If the Intention was MOVE_TO, set the Intention to ACTIVE.
			if (getIntention() == Intention.MOVE_TO)
			{
				setIntentionActive();
			}
			
			if (_actor.isNpc())
			{
				final Npc npc = _actor.asNpc();
				WalkingTaskManager.getInstance().onArrived(npc); // Walking Manager support.
				
				// Notify to scripts
				if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_MOVE_FINISHED, npc))
				{
					if (_onNpcMoveFinished == null)
					{
						_onNpcMoveFinished = new OnNpcMoveFinished(npc);
					}
					
					EventDispatcher.getInstance().notifyEventAsync(_onNpcMoveFinished, npc);
				}
			}
			
			// Launch actions corresponding to the Action Think.
			notifyActionThink();
		}
		finally
		{
			final NextAction nextAction = getNextAction();
			if ((nextAction != null) && nextAction.isTriggeredBy(Action.ARRIVED))
			{
				setNextAction(null);
				nextAction.doAction();
			}
		}
	}
	
	/**
	 * This method is called when an actor arrives at a destination.<br>
	 * It checks if the actor should perform a new action based on its current state.<br>
	 * If the actor is moving, it triggers {@code notifyActionThink()}.<br>
	 * It also executes any next action triggered by {@code Action.ARRIVED_REVALIDATE}.
	 */
	@Override
	public void notifyActionArrivedRevalidate()
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		// Skip if the actor is not moving any more (matches the prior dispatcher gate).
		if (_actor.isMoving())
		{
			// Launch actions corresponding to the Action Think.
			notifyActionThink();
		}
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.ARRIVED_REVALIDATE))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * Notifies the actor that it has arrived at a location but is blocked.<br>
	 * This method stops movement on both the server and client sides.<br>
	 * It then triggers the next available action if one exists for this state.
	 * @param location The {@code Location} where the actor was blocked.
	 */
	@Override
	public void notifyActionArrivedBlocked(Location location)
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		// If the Intention was MOVE_TO, set the Intention to ACTIVE.
		final Intention intention = getIntention();
		if ((intention == Intention.MOVE_TO) || (intention == Intention.CAST))
		{
			setIntentionActive();
		}
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast).
		clientStopMoving(location);
		
		// Launch actions corresponding to the Action Think.
		notifyActionThink();
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.ARRIVED_BLOCKED))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * This method handles the logic for when an actor forgets a specific object.<br>
	 * It removes the object from seen creatures and cancels any active casts or follow tasks.<br>
	 * If the forgotten object was the current target, it resets the intention to {@code ACTIVE} or {@code IDLE}.<br>
	 * Finally, it executes any pending actions triggered by a {@code FORGET_OBJECT} event.
	 * @param object The {@code WorldObject} that the actor is forgetting.
	 */
	@Override
	public void notifyActionForgetObject(WorldObject object)
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		// Remove the object from the seen creatures list (matches the prior dispatcher side effect).
		_actor.removeSeenCreature(object);
		
		final WorldObject target = getTarget();
		
		// Stop any casting pointing to this object.
		getActor().abortCast(sc -> sc.getTarget() == object);
		
		// If the object was targeted and the Intention was INTERACT or PICK_UP, set the Intention to ACTIVE.
		if (target == object)
		{
			setTarget(null);
			
			if (isFollowing())
			{
				// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast).
				clientStopMoving(null);
				
				// Stop an AI Follow Task.
				stopFollow();
			}
			
			// Stop any intention that has target we want to forget.
			if (getIntention() != Intention.MOVE_TO)
			{
				setIntentionActive();
			}
		}
		
		// Check if the targeted object was the actor.
		if (_actor == object)
		{
			// Cancel AI target.
			setTarget(null);
			
			// Init cast target.
			setCastTarget(null);
			
			// Stop an AI Follow Task.
			stopFollow();
			
			// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast).
			clientStopMoving(null);
			
			// Set the Intention of this AbstractAI to IDLE.
			_intention = Intention.IDLE;
		}
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.FORGET_OBJECT))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * Notifies the AI that its current action has been cancelled.<br>
	 * This method aborts any active casts and stops follow tasks.<br>
	 * It then triggers a new thought process to determine the next behavior.
	 */
	@Override
	public void notifyActionCancel()
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		_actor.abortCast();
		
		// Stop an AI Follow Task.
		stopFollow();
		
		if (!AttackStanceTaskManager.getInstance().hasAttackStanceTask(_actor))
		{
			_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
		}
		
		// Launch actions corresponding to the Action Think.
		notifyActionThink();
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.CANCEL))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * Notifies the system that the actor has died.<br>
	 * This method stops all current AI tasks and clears display effects.<br>
	 * It sends a death notification to the client and handles any actions triggered by death.
	 */
	@Override
	public void notifyActionDeath()
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		// Stop an AI Tasks.
		stopAITask();
		
		if (_actor.isNpc())
		{
			_actor.asNpc().setDisplayEffect(0);
		}
		
		// Kill the actor client side by sending Server->Client packet AutoAttackStop, StopMove/StopRotation, Die (broadcast).
		clientNotifyDead();
		
		if (!_actor.isPlayable() && !_actor.isFakePlayer())
		{
			_actor.setWalking();
		}
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.DEATH))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * This method handles the logic for when an actor performs a fake death action.<br>
	 * It stops any active follow tasks and movement.<br>
	 * The AI intention is reset to {@code IDLE}.<br>
	 * Any specific targets are cleared from the current state.
	 */
	@Override
	public void notifyActionFakeDeath()
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		// Stop an AI Follow Task.
		stopFollow();
		
		// Stop the actor movement and send Server->Client packet StopMove/StopRotation (broadcast).
		clientStopMoving(null);
		
		// Init AI
		_intention = Intention.IDLE;
		setTarget(null);
		setCastTarget(null);
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.FAKE_DEATH))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * This method is called when a casting action finishes.<br>
	 * It checks if the actor has an AI and is currently spawned.<br>
	 * If the next action is triggered by {@code Action.FINISH_CASTING}, it executes that action.
	 */
	@Override
	public void notifyActionFinishCasting()
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.FINISH_CASTING))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * Checks if the actor should move toward a specific position.<br>
	 * It calculates the distance and updates movement if necessary.<br>
	 * This method handles pathing logic for NPCs and other entities.
	 * @param worldPosition The target {@code ILocational} to move towards.
	 * @param offset The radius or distance used for calculation.
	 * @return {@code true} if the actor starts moving, otherwise {@code false}.
	 */
	protected boolean maybeMoveToPosition(ILocational worldPosition, int offset)
	{
		if (worldPosition == null)
		{
			// LOGGER.warning("maybeMoveToPosition: worldPosition == NULL!");
			return false;
		}
		
		if (offset < 0)
		{
			return false; // skill radius -1
		}
		
		if (!_actor.isInsideRadius2D(worldPosition, offset + _actor.getTemplate().getCollisionRadius()))
		{
			if (_actor.isMovementDisabled() || (_actor.getMoveSpeed() <= 0))
			{
				return true;
			}
			
			if (!_actor.isRunning() && !(this instanceof PlayerAI) && !(this instanceof SummonAI))
			{
				_actor.setRunning();
			}
			
			stopFollow();
			
			int x = _actor.getX();
			int y = _actor.getY();
			
			final double dx = worldPosition.getX() - x;
			final double dy = worldPosition.getY() - y;
			double dist = Math.sqrt((dx * dx) + (dy * dy));
			
			final double sin = dy / dist;
			final double cos = dx / dist;
			dist -= offset - 5;
			x += (int) (dist * cos);
			y += (int) (dist * sin);
			moveTo(x, y, worldPosition.getZ());
			return true;
		}
		
		if (isFollowing())
		{
			stopFollow();
		}
		
		return false;
	}
	
	/**
	 * Determines if the actor should move toward a specific target based on distance.<br>
	 * This method handles collision offsets and movement logic for different intentions.<br>
	 * It may trigger following behavior or update the actor's current intention.
	 * @param target The {@code WorldObject} to move towards.
	 * @param offsetValue The base distance offset to use for the check.
	 * @return {@code true} if the actor should proceed with moving or acting, {@code false} otherwise.
	 */
	protected boolean maybeMoveToPawn(WorldObject target, int offsetValue)
	{
		// Get the distance between the current position of the Creature and the target (x,y).
		if (target == null)
		{
			// LOGGER.warning("maybeMoveToPawn: target == NULL!");
			return false;
		}
		
		if (offsetValue < 0)
		{
			return false; // skill radius -1
		}
		
		int offsetWithCollision = offsetValue + _actor.getTemplate().getCollisionRadius();
		if (target.isCreature())
		{
			offsetWithCollision += target.asCreature().getTemplate().getCollisionRadius();
		}
		
		if (!_actor.isInsideRadius2D(target, offsetWithCollision))
		{
			// Caller should be Playable and thinkAttack/thinkCast/thinkInteract/thinkPickUp.
			if (isFollowing())
			{
				// Allow larger hit range when the target is moving (check is run only once per second).
				if (!_actor.isInsideRadius2D(target, offsetWithCollision + 100))
				{
					return true;
				}
				
				stopFollow();
				return false;
			}
			
			if (_actor.isMovementDisabled() || (_actor.getMoveSpeed() <= 0))
			{
				// If player is trying attack target but he cannot move to attack target.
				// Change his intention to idle.
				if (_actor.getAI().getIntention() == Intention.ATTACK)
				{
					_actor.getAI().setIntentionIdle();
				}
				
				return true;
			}
			
			// While flying there is no move to cast.
			if (_actor.isPlayer() && (_actor.getAI().getIntention() == Intention.CAST))
			{
				final Transform transform = _actor.getTransformation();
				if ((transform != null) && !transform.canUseWeaponStats())
				{
					_actor.sendPacket(SystemMessageId.THE_DISTANCE_IS_TOO_FAR_AND_SO_THE_CASTING_HAS_BEEN_CANCELLED);
					_actor.sendPacket(ActionFailed.STATIC_PACKET);
					return true;
				}
			}
			
			// If not running, set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others Player.
			if (!_actor.isRunning() && !(this instanceof PlayerAI) && !(this instanceof SummonAI))
			{
				_actor.setRunning();
			}
			
			stopFollow();
			int offset = offsetValue;
			if (target.isCreature() && !target.isDoor())
			{
				final Creature targetCreature = target.asCreature();
				
				// The follow task loses a summon's quarry the moment it moves, so a summon heads for the target on every think instead.
				if (_actor.isSummon())
				{
					if (targetCreature.isMoving())
					{
						moveTo(targetCreature.getX(), targetCreature.getY(), targetCreature.getZ());
					}
					else
					{
						moveToPawn(targetCreature, 0);
					}
				}
				else
				{
					if (targetCreature.isMoving())
					{
						offset -= 100;
					}
					
					if (offset < 5)
					{
						offset = 5;
					}
					
					startFollow(targetCreature, offset);
				}
			}
			else
			{
				// Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast).
				moveToPawn(target, offset);
			}
			
			return true;
		}
		
		if (isFollowing())
		{
			stopFollow();
		}
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast).
		// clientStopMoving(null);
		return false;
	}
	
	/**
	 * Checks if the specified {@code Creature} is no longer available.<br>
	 * This happens if the target is {@code null} or is dead.<br>
	 * If true, it resets the intention to active.
	 * @param target The {@code Creature} to check.
	 * @return {@code true} if the target is lost or dead, otherwise {@code false}.
	 */
	protected boolean checkTargetLostOrDead(Creature target)
	{
		if ((target == null) || target.isDead())
		{
			setIntentionActive();
			return true;
		}
		
		return false;
	}
	
	/**
	 * Checks if the specified {@code WorldObject} is no longer reachable or visible.<br>
	 * This method updates the actor's intention to active if the target is lost.
	 * @param target The {@code WorldObject} to check for visibility and reachability.
	 * @return {@code true} if the target is lost, otherwise {@code false}.
	 */
	protected boolean checkTargetLost(WorldObject target)
	{
		if (target == null)
		{
			setIntentionActive();
			return true;
		}
		
		if (_actor != null)
		{
			if ((_skill != null) && _skill.hasNegativeEffect() && (_skill.getAffectRange() > 0))
			{
				if (_actor.isPlayer() && _actor.isMoving())
				{
					if (!GeoEngine.getInstance().canMoveToTarget(_actor, target))
					{
						setIntentionActive();
						return true;
					}
				}
				else
				{
					if (!GeoEngine.getInstance().canSeeTarget(_actor, target))
					{
						setIntentionActive();
						return true;
					}
				}
			}
			
			if (_actor.isSummon())
			{
				if (GeoEngine.getInstance().canMoveToTarget(_actor, target))
				{
					return false;
				}
				
				setIntentionActive();
				return true;
			}
		}
		
		return false;
	}
	
	protected class SelfAnalysis
	{
		public boolean isMage = false;
		public boolean isBalanced;
		public boolean isArcher = false;
		public boolean isHealer = false;
		public boolean isFighter = false;
		public boolean cannotMoveOnLand = false;
		public Set<Skill> generalSkills = ConcurrentHashMap.newKeySet();
		public Set<Skill> buffSkills = ConcurrentHashMap.newKeySet();
		public int lastBuffTick = 0;
		public Set<Skill> debuffSkills = ConcurrentHashMap.newKeySet();
		public int lastDebuffTick = 0;
		public Set<Skill> cancelSkills = ConcurrentHashMap.newKeySet();
		public Set<Skill> healSkills = ConcurrentHashMap.newKeySet();
		public Set<Skill> generalDisablers = ConcurrentHashMap.newKeySet();
		public Set<Skill> sleepSkills = ConcurrentHashMap.newKeySet();
		public Set<Skill> rootSkills = ConcurrentHashMap.newKeySet();
		public Set<Skill> muteSkills = ConcurrentHashMap.newKeySet();
		public Set<Skill> resurrectSkills = ConcurrentHashMap.newKeySet();
		public boolean hasHealOrResurrect = false;
		public boolean hasLongRangeSkills = false;
		public boolean hasLongRangeDamageSkills = false;
		public int maxCastRange = 0;
		
		public SelfAnalysis()
		{
		}
		
		public void init()
		{
			switch (((NpcTemplate) _actor.getTemplate()).getAIType())
			{
				case FIGHTER:
				{
					isFighter = true;
					break;
				}
				case MAGE:
				{
					isMage = true;
					break;
				}
				case CORPSE:
				case BALANCED:
				{
					isBalanced = true;
					break;
				}
				case ARCHER:
				{
					isArcher = true;
					break;
				}
				case HEALER:
				{
					isHealer = true;
					break;
				}
				default:
				{
					isFighter = true;
					break;
				}
			}
			
			// Water movement analysis.
			if (_actor.isNpc())
			{
				switch (_actor.getId())
				{
					case 20314: // Great White Shark
					case 20849: // Light Worm
					{
						cannotMoveOnLand = true;
						break;
					}
					default:
					{
						cannotMoveOnLand = false;
						break;
					}
				}
			}
			
			// Skill analysis.
			for (Skill sk : _actor.getAllSkills())
			{
				if (sk.isPassive())
				{
					continue;
				}
				
				final int castRange = sk.getCastRange();
				boolean hasLongRangeDamageSkill = false;
				if (sk.isContinuous())
				{
					if (!sk.isDebuff())
					{
						buffSkills.add(sk);
					}
					else
					{
						debuffSkills.add(sk);
					}
					continue;
				}
				
				if (sk.hasEffectType(EffectType.DISPEL, EffectType.DISPEL_BY_SLOT))
				{
					cancelSkills.add(sk);
				}
				else if (sk.hasEffectType(EffectType.HEAL))
				{
					healSkills.add(sk);
					hasHealOrResurrect = true;
				}
				else if (sk.hasEffectType(EffectType.SLEEP))
				{
					sleepSkills.add(sk);
				}
				else if (sk.hasEffectType(EffectType.BLOCK_ACTIONS))
				{
					// Hardcoding petrification until improvements are made to EffectTemplate... petrification is totally different for AI than paralyze.
					switch (sk.getId())
					{
						case 367:
						case 4111:
						case 4383:
						case 4616:
						case 4578:
						{
							sleepSkills.add(sk);
							break;
						}
						default:
						{
							generalDisablers.add(sk);
							break;
						}
					}
				}
				else if (sk.hasEffectType(EffectType.ROOT))
				{
					rootSkills.add(sk);
				}
				else if (sk.hasEffectType(EffectType.BLOCK_CONTROL))
				{
					debuffSkills.add(sk);
				}
				else if (sk.hasEffectType(EffectType.MUTE))
				{
					muteSkills.add(sk);
				}
				else if (sk.hasEffectType(EffectType.RESURRECTION))
				{
					resurrectSkills.add(sk);
					hasHealOrResurrect = true;
				}
				else
				{
					generalSkills.add(sk);
					hasLongRangeDamageSkill = true;
				}
				
				if (castRange > 150)
				{
					hasLongRangeSkills = true;
					if (hasLongRangeDamageSkill)
					{
						hasLongRangeDamageSkills = true;
					}
				}
				
				if (castRange > maxCastRange)
				{
					maxCastRange = castRange;
				}
			}
			
			// Because of missing skills, some mages/balanced cannot play like mages.
			if (!hasLongRangeDamageSkills && isMage)
			{
				isBalanced = true;
				isMage = false;
				isFighter = false;
			}
			
			if (!hasLongRangeSkills && (isMage || isBalanced))
			{
				isBalanced = false;
				isMage = false;
				isFighter = true;
			}
			
			if (generalSkills.isEmpty() && isMage)
			{
				isBalanced = true;
				isMage = false;
			}
		}
	}
}
