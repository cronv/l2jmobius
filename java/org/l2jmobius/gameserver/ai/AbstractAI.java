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

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.WorldRegion;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.AutoAttackStart;
import org.l2jmobius.gameserver.network.serverpackets.AutoAttackStop;
import org.l2jmobius.gameserver.network.serverpackets.Die;
import org.l2jmobius.gameserver.network.serverpackets.MoveToLocation;
import org.l2jmobius.gameserver.network.serverpackets.MoveToPawn;
import org.l2jmobius.gameserver.network.serverpackets.StopMove;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;
import org.l2jmobius.gameserver.taskmanagers.CreatureFollowTaskManager;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

/**
 * This is the base class for all artificial intelligence behaviors in the game world.<br>
 * It serves as the foundation for {@code CreatureAI} and other entity logic.
 */
public abstract class AbstractAI
{
	/** The creature that this AI manages */
	protected final Creature _actor;
	
	/** Current long-term intention. */
	protected Intention _intention = Intention.IDLE;
	
	/** Flags about client's state, in order to know which messages to send. */
	private volatile boolean _clientAutoAttacking;
	/** Flags about client's state, in order to know which messages to send. */
	protected int _clientMovingToPawnOffset;
	
	/** Different targets this AI maintains. */
	private WorldObject _target;
	private WorldObject _castTarget;
	
	/** The skill we are currently casting by INTENTION_CAST. */
	protected Skill _skill;
	protected Item _item;
	protected boolean _forceUse;
	protected boolean _dontMove;
	
	/** Different internal state flags. */
	protected int _moveToPawnTimeout;
	
	private NextAction _nextAction;
	
	/**
	 * Retrieves the next action to be performed by this AI.<br>
	 * This method returns the current value of {@code _nextAction}.
	 * @return the {@code NextAction} object.
	 */
	public NextAction getNextAction()
	{
		return _nextAction;
	}
	
	/**
	 * Sets the next action to be performed by this AI.<br>
	 * This updates the {@code _nextAction} field with the provided value.
	 * @param nextAction The {@code NextAction} to set.
	 */
	public void setNextAction(NextAction nextAction)
	{
		_nextAction = nextAction;
	}
	
	/**
	 * Initializes the AI with a specific actor.<br>
	 * This constructor sets the {@code _actor} field to the provided {@code Creature}.
	 * @param creature The {@code Creature} that this AI will manage.
	 */
	protected AbstractAI(Creature creature)
	{
		_actor = creature;
	}
	
	/**
	 * Retrieves the {@code Creature} associated with this AI.<br>
	 * This is the entity that the AI currently manages.
	 * @return The {@code Creature} instance.
	 */
	public Creature getActor()
	{
		return _actor;
	}
	
	/**
	 * Retrieves the current intention of this AI.<br>
	 * This represents what the AI is currently trying to do.
	 * @return the current {@code Intention} object.
	 */
	public Intention getIntention()
	{
		return _intention;
	}
	
	/**
	 * Retrieves the next intended action for this AI.<br>
	 * This method currently returns {@code null}.
	 * @return the next {@code Intention} or {@code null} if none exists.
	 */
	public Intention getNextIntention()
	{
		return null;
	}
	
	public abstract void setIntentionIdle();
	
	public abstract void setIntentionActive();
	
	public abstract void setIntentionRest();
	
	public abstract void setIntentionAttack(WorldObject target);
	
	public abstract void setIntentionCast(Skill skill, WorldObject target, Item item, boolean forceUse, boolean dontMove);
	
	/**
	 * Sets the current intention of the AI to cast a specific skill.<br>
	 * This method updates the internal state to reflect that a {@code Skill} is being used on a {@code WorldObject}.
	 * @param skill The {@code Skill} that the AI intends to cast.
	 * @param target The {@code WorldObject} that is the target of the skill.
	 */
	public void setIntentionCast(Skill skill, WorldObject target)
	{
		setIntentionCast(skill, target, null, false, false);
	}
	
	public abstract void setIntentionMoveTo(ILocational destination);
	
	public abstract void setIntentionFollow(WorldObject target);
	
	public abstract void setIntentionPickUp(WorldObject item);
	
	public abstract void setIntentionInteract(WorldObject object);
	
	public abstract void notifyActionThink();
	
	public abstract void notifyActionAttacked(WorldObject attacker);
	
	public abstract void notifyActionAggression(WorldObject target, int aggro);
	
	public abstract void notifyActionBlocked();
	
	public abstract void notifyActionRooted();
	
	public abstract void notifyActionConfused();
	
	public abstract void notifyActionMuted();
	
	public abstract void notifyActionEvaded(WorldObject attacker);
	
	public abstract void notifyActionReadyToAct();
	
	public abstract void notifyActionArrived();
	
	public abstract void notifyActionArrivedRevalidate();
	
	public abstract void notifyActionArrivedBlocked(Location location);
	
	public abstract void notifyActionForgetObject(WorldObject object);
	
	public abstract void notifyActionCancel();
	
	public abstract void notifyActionDeath();
	
	public abstract void notifyActionFakeDeath();
	
	public abstract void notifyActionFinishCasting();
	
	/**
	 * Handles the logic when a client action fails.<br>
	 * This method checks if the {@code _actor} is a {@code Player}.<br>
	 * If it is a player, it sends the {@code ActionFailed.STATIC_PACKET} to them.
	 */
	protected void clientActionFailed()
	{
		if (_actor.isPlayer())
		{
			_actor.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	/**
	 * Moves the actor toward a specific {@code WorldObject}.<br>
	 * This method updates the movement target and calculates the path.<br>
	 * It handles packet broadcasting to clients based on whether the target is a creature.
	 * @param pawn The {@code WorldObject} that the actor should move towards.
	 * @param offsetValue The distance offset from the target location.
	 */
	public void moveToPawn(WorldObject pawn, int offsetValue)
	{
		// Check if actor can move.
		if (!_actor.isMovementDisabled() && !_actor.isAttackingNow() && !_actor.isCastingNow())
		{
			int offset = offsetValue;
			if (offset < 10)
			{
				offset = 10;
			}
			
			// Prevent possible extra calls to this function (there is none?), also don't send movetopawn packets too often.
			final int gameTime = GameTimeTaskManager.getInstance().getGameTicks();
			if (_actor.isMoving() && (_target == pawn))
			{
				if (_clientMovingToPawnOffset == offset)
				{
					if (gameTime < _moveToPawnTimeout)
					{
						return;
					}
				}
				// Minimum time to calculate new route is 2 seconds.
				else if (_actor.isOnGeodataPath() && (gameTime < (_moveToPawnTimeout + 10)))
				{
					return;
				}
			}
			
			// Set AI movement data.
			_clientMovingToPawnOffset = offset;
			_target = pawn;
			_moveToPawnTimeout = gameTime;
			_moveToPawnTimeout += 1000 / GameTimeTaskManager.MILLIS_IN_TICK;
			
			if (pawn == null)
			{
				return;
			}
			
			// Calculate movement data for a move to location action and add the actor to movingObjects of GameTimeTaskManager.
			_actor.moveToLocation(pawn.getX(), pawn.getY(), pawn.getZ(), offset);
			
			// May result to make monsters stop moving.
			// if (!_actor.isMoving())
			// {
			// clientActionFailed();
			// return;
			// }
			
			// Send a Server->Client packet MoveToPawn/MoveToLocation to the actor and all Player in its known players.
			if (pawn.isCreature())
			{
				if (_actor.isOnGeodataPath())
				{
					_actor.broadcastMoveToLocation();
					_clientMovingToPawnOffset = 0;
				}
				else
				{
					final WorldRegion region = _actor.getWorldRegion();
					if ((region != null) && region.isActive())
					{
						_actor.broadcastPacket(new MoveToPawn(_actor, pawn, offset));
					}
				}
			}
			else
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
	 * Moves the actor to a specific location.<br>
	 * This method updates the position using the coordinates from the provided {@code ILocational} object.
	 * @param loc The destination location to move to.
	 */
	public void moveTo(ILocational loc)
	{
		moveTo(loc.getX(), loc.getY(), loc.getZ());
	}
	
	/**
	 * Moves the actor to a specific set of coordinates.<br>
	 * This method checks if movement is enabled before updating the position.<br>
	 * If movement is disabled, it triggers a failed action notification.
	 * @param x The target x coordinate.
	 * @param y The target y coordinate.
	 * @param z The target z coordinate.
	 */
	protected void moveTo(int x, int y, int z)
	{
		// Check if actor can move.
		if (!_actor.isMovementDisabled())
		{
			// Set AI movement data.
			_clientMovingToPawnOffset = 0;
			
			// Calculate movement data for a move to location action and add the actor to movingObjects of GameTimeTaskManager.
			_actor.moveToLocation(x, y, z, 0);
			
			// Send a Server->Client packet MoveToLocation to the actor and all Player in its known players.
			_actor.broadcastMoveToLocation();
		}
		else
		{
			clientActionFailed();
		}
	}
	
	/**
	 * Stops the movement of the {@code Creature}.<br>
	 * This method checks if the actor is currently moving before stopping it.<br>
	 * It also resets the {@code clientMovingToPawnOffset} to {@code 0}.
	 * @param loc The {@code Location} where the movement should stop.
	 */
	public void clientStopMoving(Location loc)
	{
		// Stop movement of the Creature.
		if (_actor.isMoving())
		{
			_actor.stopMove(loc);
		}
		
		_clientMovingToPawnOffset = 0;
	}
	
	/**
	 * This method is called when the client stops moving.<br>
	 * It resets the {@code _clientMovingToPawnOffset} value to {@code 0}.<br>
	 * It sends a {@code StopMove} packet to the {@code _actor} if an offset was active.
	 */
	protected void clientStoppedMoving()
	{
		if (_clientMovingToPawnOffset > 0) // movetoPawn needs to be stopped.
		{
			_clientMovingToPawnOffset = 0;
			_actor.broadcastPacket(new StopMove(_actor));
		}
	}
	
	/**
	 * Checks if the entity is currently in an auto-attack state.<br>
	 * This returns {@code true} if auto-attacking is active.<br>
	 * It returns {@code false} otherwise.
	 * @return The current auto-attack status as a {@code boolean}.
	 */
	public boolean isAutoAttacking()
	{
		return _clientAutoAttacking;
	}
	
	/**
	 * Sets whether the actor is currently in an auto-attacking state.<br>
	 * If the actor is a {@code Summon}, this value is also applied to its owner.
	 * @param isAutoAttacking The new status for auto-attacking. Use {@code true} to enable and {@code false} to disable.
	 */
	public void setAutoAttacking(boolean isAutoAttacking)
	{
		if (_actor.isSummon())
		{
			final Summon summon = _actor.asSummon();
			final Player owner = summon.getOwner();
			if (owner != null)
			{
				owner.getAI().setAutoAttacking(isAutoAttacking);
			}
			return;
		}
		
		// Warg.
		if (_actor.isPlayer() && _actor.asPlayer().isWarg())
		{
			if (isAutoAttacking)
			{
				switch (_actor.getAffectedSkillLevel(31454 /* Werewolf's Awakening */))
				{
					case 1:
					{
						_actor.transform(182, false);
						break;
					}
					case 2:
					{
						_actor.transform(183, false);
						break;
					}
					case 3:
					{
						_actor.transform(184, false);
						break;
					}
				}
			}
			else
			{
				_actor.stopTransformation(false);
			}
		}
		
		_clientAutoAttacking = isAutoAttacking;
	}
	
	/**
	 * Starts the auto-attack state for the actor.<br>
	 * This method checks if the actor is allowed to enter combat.<br>
	 * It handles special logic for summons and players with pets.<br>
	 * Finally, it broadcasts the {@code AutoAttackStart} packet and updates the internal state.
	 */
	public void clientStartAutoAttack()
	{
		// Non attackable NPCs should not get in combat.
		if (_actor.isNpc() && (!_actor.isAttackable() || _actor.isCoreAIDisabled()))
		{
			return;
		}
		
		if (_actor.isSummon())
		{
			final Summon summon = _actor.asSummon();
			final Player owner = summon.getOwner();
			if (owner != null)
			{
				owner.getAI().clientStartAutoAttack();
			}
			return;
		}
		
		if (!_clientAutoAttacking)
		{
			if (_actor.isPlayer() && _actor.hasSummon())
			{
				final Summon pet = _actor.getPet();
				if (pet != null)
				{
					pet.broadcastPacket(new AutoAttackStart(pet.getObjectId()));
				}
				
				_actor.getServitors().values().forEach(s -> s.broadcastPacket(new AutoAttackStart(s.getObjectId())));
			}
			
			// Send a Server->Client packet AutoAttackStart to the actor and all Player in its known players.
			_actor.broadcastPacket(new AutoAttackStart(_actor.getObjectId()));
			setAutoAttacking(true);
		}
		
		AttackStanceTaskManager.getInstance().addAttackStanceTask(_actor);
	}
	
	/**
	 * Stops the auto-attack state for the actor.<br>
	 * If the actor is a {@code Summon}, it stops the auto-attack for its owner.<br>
	 * For players, it ensures an attack stance task is active if they are currently attacking.<br>
	 * For other creatures, it broadcasts an {@code AutoAttackStop} packet and sets {@code _clientAutoAttacking} to {@code false}.
	 */
	public void clientStopAutoAttack()
	{
		if (_actor.isSummon())
		{
			final Summon summon = _actor.asSummon();
			final Player owner = summon.getOwner();
			if (owner != null)
			{
				owner.getAI().clientStopAutoAttack();
			}
			return;
		}
		
		if (_actor.isPlayer())
		{
			if (!AttackStanceTaskManager.getInstance().hasAttackStanceTask(_actor) && isAutoAttacking())
			{
				AttackStanceTaskManager.getInstance().addAttackStanceTask(_actor);
			}
		}
		else if (_clientAutoAttacking)
		{
			_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
			setAutoAttacking(false);
		}
	}
	
	/**
	 * Retrieves the current offset value for moving to a pawn.<br>
	 * This value is used when the client moves toward a {@code WorldObject}.
	 * @return the integer offset value.
	 */
	public int getClientMovingToPawnOffset()
	{
		return _clientMovingToPawnOffset;
	}
	
	/**
	 * Notifies the client that the actor has died.<br>
	 * This method sends a {@code Die} packet to the actor and nearby players.<br>
	 * It also resets the AI state by clearing targets and intentions.<br>
	 * Finally, it cancels any active follow tasks.
	 */
	protected void clientNotifyDead()
	{
		// Send a Server->Client packet Die to the actor and all Player in its known players.
		_actor.broadcastPacket(new Die(_actor));
		
		// Init AI
		_intention = Intention.IDLE;
		_target = null;
		_castTarget = null;
		
		// Cancel the follow task if necessary.
		stopFollow();
	}
	
	/**
	 * Sends the current movement state of this AI to a specific player.<br>
	 * This method checks if the actor is visible and moving.<br>
	 * It sends either a {@code MoveToPawn} or {@code MoveToLocation} packet based on the follow status.
	 * @param player The {@code Player} who will receive the state update.
	 */
	public void describeStateToPlayer(Player player)
	{
		if (_actor.isVisibleFor(player) && _actor.isMoving())
		{
			if ((_clientMovingToPawnOffset != 0) && isFollowing())
			{
				// Send a Server->Client packet MoveToPawn to the actor and all Player in its known players.
				player.sendPacket(new MoveToPawn(_actor, _target, _clientMovingToPawnOffset));
			}
			else
			{
				// Send a Server->Client packet MoveToLocation to the actor and all Player in its known players.
				player.sendPacket(new MoveToLocation(_actor));
			}
		}
	}
	
	/**
	 * Checks if the AI is currently in a following state.<br>
	 * It returns {@code true} if there is a valid target and the intention is set to follow.<br>
	 * It also checks the status of the {@code CreatureFollowTaskManager}.
	 * @return {@code true} if the actor is following, {@code false} otherwise.
	 */
	public boolean isFollowing()
	{
		return (_target != null) && _target.isCreature() && ((_intention == Intention.FOLLOW) || CreatureFollowTaskManager.getInstance().isFollowing(_actor));
	}
	
	/**
	 * Starts the AI to follow a specific creature.<br>
	 * This method initiates the following behavior for the {@code Creature}.<br>
	 * It uses a default range of -1.
	 * @param target The {@code Creature} that should be followed.
	 */
	public void startFollow(Creature target)
	{
		startFollow(target, -1);
	}
	
	/**
	 * Starts the AI to follow a specific target.<br>
	 * This method stops any current following behavior before starting the new one.<br>
	 * It determines whether to use normal or attack follow based on the provided range.
	 * @param target The {@code Creature} that the actor should follow.
	 * @param range The distance for following. Use -1 for normal follow mode.
	 */
	public void startFollow(Creature target, int range)
	{
		stopFollow();
		setTarget(target);
		if (range == -1)
		{
			CreatureFollowTaskManager.getInstance().addNormalFollow(_actor, range);
		}
		else
		{
			CreatureFollowTaskManager.getInstance().addAttackFollow(_actor, range);
		}
	}
	
	/**
	 * Stops the current following behavior.<br>
	 * This method removes the {@code _actor} from the {@code CreatureFollowTaskManager}.
	 */
	public void stopFollow()
	{
		CreatureFollowTaskManager.getInstance().remove(_actor);
	}
	
	/**
	 * Sets the current target for this AI.<br>
	 * This updates the {@code _target} field with the provided object.
	 * @param target The {@code WorldObject} to be set as the new target.
	 */
	public void setTarget(WorldObject target)
	{
		_target = target;
	}
	
	/**
	 * Retrieves the current target of this AI.<br>
	 * Returns {@code null} if there is no active target.
	 * @return The {@code WorldObject} currently being targeted.
	 */
	public WorldObject getTarget()
	{
		return _target;
	}
	
	/**
	 * Sets the specific object that this AI will cast a skill at.<br>
	 * This updates the internal {@code _castTarget} variable.
	 * @param target The {@code WorldObject} to be used as the casting goal.
	 */
	protected void setCastTarget(WorldObject target)
	{
		_castTarget = target;
	}
	
	/**
	 * Retrieves the current object being targeted for a cast.<br>
	 * This is used to identify what the AI is currently aiming at.
	 * @return the {@code WorldObject} that is currently targeted, or {@code null} if no target exists.
	 */
	public WorldObject getCastTarget()
	{
		return _castTarget;
	}
	
	/**
	 * Stops the current AI task for the actor.<br>
	 * This method calls {@code stopFollow()} to cancel any active following behavior.
	 */
	public void stopAITask()
	{
		stopFollow();
	}
	
	/**
	 * Returns a string representation of the AI.<br>
	 * This includes the name of the associated {@code Creature}.
	 * @return A string starting with {@code Actor:} followed by the actor's name.
	 */
	@Override
	public String toString()
	{
		return "Actor: " + _actor;
	}
}
